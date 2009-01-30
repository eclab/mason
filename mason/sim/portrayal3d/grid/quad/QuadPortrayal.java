/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid.quad;

import sim.display.*;
import sim.portrayal.*;
import sim.util.gui.*;
import sim.portrayal3d.grid.*;
import sim.field.grid.*;
import sim.util.*;
import com.sun.j3d.utils.picking.*;

/**
 * A QuadPortrayal is the abstract superclass of objects which describe how rectangles in a
 * in a ValueGrid2DPortrayal3D are associated with the underlying ValueGrid2D.  
 * These objects are not true Portrayal3D objects: they can only be used with ValueGrid2DPortrayal3D.
 *
 * <p>There are two standard subclasses: TilePortrayal represents objects as squares on
 * the grid.  MeshPortrayal represents objects as the *intersections* of the grid lines. 
 *
 * <p>QuadPortrayals may be associated with a <i>zScale</i>, indicating the degree to which the value
 * effects the "height" of the rectangle.  In TilePortrayal, different "heights" result in stair-step
 * effects.  In MeshsPortrayal, different "heights" result in a mountainscape.
 *
 * <p>QuadPortrayals are also associated with a <i>colorDispenser</i> which specifies the color of the
 * ValueGrid2D point, just as in ValueGridPortrayal2D.
 *
 * <p><b><font color=red>Bug in MeshPortrayal.</font></b>  It appears that if values in MeshPortrayals
 * cause "bends" in the angle of the underlying squares that are too severe (we've seen over 45 degrees), 
 * then when Java3D tries to pick the square you've double-clicked on, the "bent" squares will insist on
 * being included in the pick collection. We believe this to be a bug in Sun's Java3D code.  You'll
 * see this happen when you double-click on a MeshPortrayal and the wrong-coordinate object pops up.
 *
 * @author Gabriel Balan
 */
public abstract class QuadPortrayal implements Portrayal 
    {
    /** How much we move the quad up or down for a given value. */
    public float zScale;
    /** Our color map for values */
    public ColorMap colorDispenser;

    public abstract void setData(ValueGridCellInfo gridCell, float[] coordinates, float[] colors, int quadIndex,
        int gridWidth, int gridHeight);
    
    public QuadPortrayal(ColorMap colorDispenser, float zScale)
        {
        this.colorDispenser = colorDispenser;
        this.zScale = zScale;
        }

    public String getStatus(LocationWrapper wrapper)
        {
        return getName(wrapper) + ": " + ((MutableDouble)(wrapper.getObject())).val;
        }

    public String getName(LocationWrapper wrapper)
        {
        ValueGrid2DPortrayal3D portrayal = (ValueGrid2DPortrayal3D)(wrapper.getFieldPortrayal());
        return portrayal.getValueName() + " at " + wrapper.getLocationName();
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        // by default, we don't want to be selected
        if (selected) return false;  // don't want to be selected
        else return true;            // we'll always be deselected -- doesn't matter
        }
    
    public static abstract class Filter
        {
        int x;
        int y;
        ValueGrid2DPortrayal3D fieldPortrayal;
        public Filter(LocationWrapper wrapper)
            {
            fieldPortrayal = (ValueGrid2DPortrayal3D)(wrapper.getFieldPortrayal());
            Int2D loc = (Int2D)(wrapper.getLocation());
            x = loc.x;
            y = loc.y;
            }
        }
    public static class DoubleFilter extends Filter
        {
        public DoubleFilter(LocationWrapper wrapper) { super(wrapper); }
        public double getValue() { return ((DoubleGrid2D)fieldPortrayal.field).field[x][y]; }
        public void setValue(double val) { ((DoubleGrid2D)fieldPortrayal.field).field[x][y] = fieldPortrayal.newValue(x,y,val); }
        }
        
    public static class IntFilter extends Filter
        {
        public IntFilter(LocationWrapper wrapper) { super(wrapper); }
        public int getValue() { return ((IntGrid2D)fieldPortrayal.field).field[x][y]; }
        public void setValue(int val) { ((IntGrid2D)fieldPortrayal.field).field[x][y] = (int)fieldPortrayal.newValue(x,y,val); }
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        if (wrapper == null) return null;
        Grid2D grid = (Grid2D)(((ValueGrid2DPortrayal3D)(wrapper.getFieldPortrayal())).getField());
        if (grid instanceof DoubleGrid2D)
            return new SimpleInspector(new DoubleFilter(wrapper), state, "Properties");
        else
            return new SimpleInspector(new IntFilter(wrapper) ,state, "Properties");
        }
        
    public Int2D getCellForIntersection(PickIntersection pi, Grid2D field)
        {
        int[] indices = pi.getPrimitiveVertexIndices();
        if(indices == null)
            return null;

        int height = field.getHeight();
        int x = indices[0]/4/height;
        int y = indices[0]/4%height;
        return new Int2D(x,y);
        }

    }
