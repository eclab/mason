/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;
import sim.util.*;
import sim.field.*;
import sim.field.grid.*;
import sim.field.continuous.*;
import sim.portrayal.*;
import sim.display.*;

/**
   StableInt2D is a StableLocation for Int2D.  See StableLocation for more information.
*/

public class StableInt2D implements StableLocation
    {
    public int x = 0;
    public int y = 0;
    public boolean exists = false;
    public FieldPortrayal fieldPortrayal;
    public GUIState gui;
    public Object object;
        
    public String toString()
        {
        update();
        if (!exists) return "Gone";
        else return "(" + x + ", " + y + ")"; 
        }
        
    public StableInt2D(FieldPortrayal fieldPortrayal, Object object, GUIState gui)
        {
        this.gui = gui;
        this.fieldPortrayal = fieldPortrayal;
        this.object = object;
        }
        
    void update()
        {
        Int2D pos = null;
        if (fieldPortrayal == null) return;
        Object p = fieldPortrayal.getObjectLocation(object, gui);
        if (p == null)  { exists = false; }  // purposely don't update x and y and z so they stay the same
        else 
        	{
        	if (p instanceof Int3D)
				{
				pos = new Int2D(((Int3D)p).x, ((Int3D)p).y);
				}
			else if (p instanceof Int2D)
				{
				pos = (Int2D)p;
				}
			else 
				{
				throw new RuntimeException("StableInt2D expected an Int2D or Int3D position from underlying field portrayal " + fieldPortrayal);
				}
			x = pos.x; 
			y = pos.y; 
			exists = true; 
			}
        }
            
    public int getX() { update(); return x; }
    public int getY() { update(); return y; }
    public boolean getExists() { update(); return exists; }  // what an ugly name
            
    public void setX(int val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Int2D(val, getY()), gui);
        }

    public void setY(int val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Int2D(getX(), val), gui);
        }

    }
