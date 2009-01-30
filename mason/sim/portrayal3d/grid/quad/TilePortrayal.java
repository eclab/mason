/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid.quad;
import sim.util.gui.*;
import sim.portrayal3d.grid.*;

/**
 * A QuadPortrayal which describes locations as the center of a square in a grid (like tiles on the floor,
 * each tile corresponding to a location on the grid).  
 */
public class TilePortrayal extends QuadPortrayal
    {
    public float[] tmpCoords;
    public float[] tmpColor;
        
    public TilePortrayal(ColorMap colorDispenser)
        {
        this(colorDispenser,0);
        }

    public TilePortrayal(ColorMap colorDispenser, float zScale)
        {
        super(colorDispenser, zScale);
        tmpCoords = new float[12];
        tmpColor = new float[4];
        }
        

    public void setData(ValueGridCellInfo gridCell, float[] coordinates, float[] colors, int quadIndex,
        int gridWidth, int gridHeight)
        {
        int x = gridCell.x;
        int y = gridCell.y;
        float value = (float)gridCell.value();
        colorDispenser.getColor(value).getComponents(tmpColor);
        value*=zScale;
        
        for(int i=0;i <4;i++) 
            System.arraycopy(tmpColor, 0, colors, (quadIndex*4+i)*3, 3);  // 3 color values -- alpha transparency doesn't work here :-(

        int offset = quadIndex*12; 
        coordinates[offset+0] = x - 0.5f;
        coordinates[offset+1] = y- 0.5f;
        coordinates[offset+2] = value;
        coordinates[offset+3] = x + 0.5f;
        coordinates[offset+4] = y - 0.5f;
        coordinates[offset+5] = value;
        coordinates[offset+6] = x + 0.5f;
        coordinates[offset+7] = y + 0.5f;
        coordinates[offset+8] = value;
        coordinates[offset+9] = x - 0.5f;
        coordinates[offset+10]= y + 0.5f;
        coordinates[offset+11]= value;

        }

    }
