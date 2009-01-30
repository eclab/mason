/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid.quad;
import sim.util.*;
import sim.util.gui.*;
import sim.field.grid.*;
import sim.portrayal3d.grid.*;
import com.sun.j3d.utils.picking.*;

/**
 * A QuadPortrayal which relates grid locations with <i>intersections</i> on a mesh (like positions on
 * a Go board, coloring and changing the Z location of the intersections.  
 *
 * <p><b><font color=red>Bug in MeshPortrayal.</font></b>  It appears that if values in MeshPortrayals
 * cause "bends" in the angle of the underlying squares that are too severe (we've seen over 45 degrees), 
 * then when Java3D tries to pick the square you've double-clicked on, the "bent" squares will insist on
 * being included in the pick collection. We believe this to be a bug in Sun's Java3D code.  You'll
 * see this happen when you double-click on a MeshPortrayal and the wrong-coordinate object pops up.
 **/
public class MeshPortrayal extends QuadPortrayal
    {
    public float[] tmpCoords;
    public float[] tmpColor;
    
    public MeshPortrayal(ColorMap colorDispenser)
        {
        this(colorDispenser,0);
        }

    public MeshPortrayal(ColorMap colorDispenser, float zScale)
        {
        super(colorDispenser,zScale);
        tmpCoords = new float[12];
        tmpColor = new float[4];
        }
        
    static final int[] dx = {0, -1, -1, 0};
    static final int[] dy = {0, 0, -1, -1};
        
    public void setData(ValueGridCellInfo gridCell, float[] coordinates, float[] colors, int quadIndex, 
        int gridWidth, int gridHeight)
        {
        // cast the width and height into a mesh form (1- the normal values)
        gridWidth -= 1;
        gridHeight -= 1;
        
        int x = gridCell.x;
        int y = gridCell.y;
        float value = (float)gridCell.value();
        colorDispenser.getColor(value).getColorComponents(tmpColor);
        value*=zScale;

        //              3-2
        //              | |  <- this is how I go through a quad.
        //              0-1
        //
        //              3-2 3-2
        //              | | | |
        //              0-1 0-1
        //              3-2 3-2
        //              | | | |
        //              0-1 0-1
        
        //look for the four quads this vertex is part of.
        for(int i=0; i<4; i++)
            {
            int cellx = x+dx[i];
            int celly = y+dy[i];
            if(cellx<0 || celly<0 || cellx>=gridWidth || celly>=gridHeight)
                continue;
            int iQuadIndex = cellx*gridHeight+celly;
            int offset = iQuadIndex * 12 + i * 3;
                        
            coordinates[offset+0] = x;
            coordinates[offset+1] = y;
            coordinates[offset+2] = value + 0.1f;
            System.arraycopy(tmpColor, 0, colors, (iQuadIndex*4+i)*3, 3);  // 3 color values -- alpha transparency doesn't work here :-(
            }
        }

    public Int2D getCellForIntersection(PickIntersection pi, Grid2D field)
        {
        int[] indices = pi.getPrimitiveVertexIndices();
        int closenessOffset = pi.getClosestVertexIndex();
        closenessOffset = indices[closenessOffset]%4;
        // 3 2
        // 0 1
        int xExtraOffset = (closenessOffset ==3 || closenessOffset ==0)? 0 :1;
        int yExtraOffset = (closenessOffset ==3 || closenessOffset ==2)? 1 :0;
                
        int height = field.getHeight();
        int x = (indices[0]/4)/(height-1)+xExtraOffset;
        int y = (indices[0]/4)%(height-1)+yExtraOffset;
        return new Int2D(x,y);
        }
    }
