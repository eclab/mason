/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.display.*;
import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal.grid.*;
import java.awt.*;
import sim.util.*;

/** 
    The ValuePortrayal2D is the default portrayal for ValueGridPortrayal2Ds.
    It requires a parent (the ValueGridPortrayal2D), which it uses to determine
    the correct colors for a given object.  The objects portrayed must be 
    instances of MutableDouble, where the value represents the level of the color.
*/

public class ValuePortrayal2D extends RectanglePortrayal2D
    {
    // public ValueGridPortrayal2D parent;  // no longer used
    
    public ValuePortrayal2D() { }
        
    /** @deprecated
     */
    public ValuePortrayal2D(ValueGridPortrayal2D parent)
        {
        super(null);  // no color  -- we'll determine the color during portrayal
        // setParent(parent);
        }
    
    /** @deprecated does nothing now
     */
    public void setParent(ValueGridPortrayal2D parent)
        {
        // this.parent = parent;
        }
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        ValueGridPortrayal2D parent = (ValueGridPortrayal2D) (info.fieldPortrayal);
        double levelHere = ((MutableDouble)object).val;
        Color c = parent.getMap().getColor(levelHere);
        if (c.getAlpha() != 0) 
            {
            paint = c;
            super.draw(object, graphics, info);
            }
        }
    
    public static abstract class Filter
        {
        int x;
        int y;
        ValueGridPortrayal2D fieldPortrayal;
        String name;
        public Filter(LocationWrapper wrapper)
            {
            fieldPortrayal = (ValueGridPortrayal2D)(wrapper.getFieldPortrayal());
            Int2D loc = (Int2D)(wrapper.getLocation());
            x = loc.x;
            y = loc.y;
            name = fieldPortrayal.getValueName() + " at " + wrapper.getLocationName();
            }
        public String toString() { return name; }
        }

    // the only reason for these two subclasses is that they differ in the data
    // type of their property (double vs int).  This allows us to guarantee that
    // ints are displayed or set as opposed to doubles in the Inspector.  No
    // big whoop -- it's more a formatting thing than anything else.
    
    public static class DoubleFilter extends Filter
        {
        public DoubleFilter(LocationWrapper wrapper) { super(wrapper); }
        public double getValue() { return ((DoubleGrid2D)fieldPortrayal.getField()).field[x][y]; }
        public void setValue(double val) { ((DoubleGrid2D)fieldPortrayal.getField()).field[x][y] = fieldPortrayal.newValue(x,y,val); }
        // static inner classes don't need serialVersionUIDs
        }
        
    public static class IntFilter extends Filter
        {
        public IntFilter(LocationWrapper wrapper) { super(wrapper); }
        public int getValue() { return ((IntGrid2D)fieldPortrayal.getField()).field[x][y]; }
        public void setValue(int val) { ((IntGrid2D)fieldPortrayal.getField()).field[x][y] = (int)fieldPortrayal.newValue(x,y,val); }
        // static inner classes don't need serialVersionUIDs
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        if (((ValueGridPortrayal2D)(wrapper.getFieldPortrayal())).getField() instanceof DoubleGrid2D)
            return sim.portrayal.Inspector.getInspector(new DoubleFilter(wrapper), state, "Properties");
        else
            return sim.portrayal.Inspector.getInspector(new IntFilter(wrapper) ,state, "Properties");
        // static inner classes don't need serialVersionUIDs
        }
    
    public String getStatus(LocationWrapper wrapper)
        {
        return getName(wrapper) + ": " + ((MutableDouble)(wrapper.getObject())).val;
        }

    public String getName(LocationWrapper wrapper)
        {
        ValueGridPortrayal2D portrayal = (ValueGridPortrayal2D)(wrapper.getFieldPortrayal());
        return portrayal.getValueName() + " at " + wrapper.getLocationName();
        }
    }
