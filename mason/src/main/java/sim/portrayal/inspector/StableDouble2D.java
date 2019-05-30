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
   StableDouble2D is a StableLocation for Double2D.  See StableLocation for more information.
*/

public class StableDouble2D implements StableLocation
    {
    public double x = 0;
    public double y = 0;
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
        
    public StableDouble2D(FieldPortrayal fieldPortrayal, Object object, GUIState gui)
        {
        this.gui = gui;
        this.fieldPortrayal = fieldPortrayal;
        this.object = object;
        }
        
    void update()
        {
        Double2D pos = null;
        if (fieldPortrayal == null) return;
        Object p = fieldPortrayal.getObjectLocation(object, gui);
        if (p == null)  { exists = false; }  // purposely don't update x and y and z so they stay the same
        else 
        	{
        	if (p instanceof Double3D)
				{
				pos = new Double2D(((Double3D)p).x, ((Double3D)p).y);
				}
			else if (p instanceof Double2D)
				{
				pos = (Double2D)p;
				}
			else 
				{
				throw new RuntimeException("StableDouble3D expected an Double2D or Double3D position from underlying field portrayal " + fieldPortrayal);
				}
			x = pos.x; 
			y = pos.y; 
			exists = true; 
			}
        }
            
    public double getX() { update(); return x; }
    public double getY() { update(); return y; }
    public boolean getExists() { update(); return exists; }  // what an ugly name
            
    public void setX(double val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Double2D(val, getY()), gui);
        }

    public void setY(double val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Double2D(getX(), val), gui);
        }

	}
