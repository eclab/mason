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
import sim.portrayal.inspector.*;
import sim.portrayal.*;
import sim.display.*;

/**
   StableDouble3D is a StableLocation for Double3D.  It can be used with either 2D or 3D fields.  See StableLocation for more information.
*/

public class StableDouble3D implements StableLocation
    {
    public double x = 0;
    public double y = 0;
    public double z = 0;
    public boolean exists = false;
    public FieldPortrayal fieldPortrayal;
    public GUIState gui;
    public Object object;

    public String toString()
        {
        update();
        if (!exists) return "Gone";
        else return "(" + x + ", " + y + ", " + z + ")"; 
        }
        
    public StableDouble3D(FieldPortrayal fieldPortrayal, Object object, GUIState gui)
        {
        this.gui = gui;
        this.fieldPortrayal = fieldPortrayal;
        this.object = object;
        }
        
    void update()
        {
        Double3D pos = null;
        if (fieldPortrayal == null) return;
        Object p = fieldPortrayal.getObjectLocation(object, gui);
        if (p == null)  { exists = false; }  // purposely don't update x and y and z so they stay the same
        else 
        	{
        	if (p instanceof Double3D)
				{
				pos = (Double3D)p;
				}
			else if (p instanceof Double2D)
				{
				pos = new Double3D((Double2D)p);
				}
			else 
				{
				throw new RuntimeException("StableDouble3D expected an Double2D or Double3D position from underlying field portrayal " + fieldPortrayal);
				}
			x = pos.x; 
			y = pos.y; 
			z = pos.z; 
			exists = true; 
			}
        }

    /* For some reason, the order of the parameters in the MASON windows will be Z, Exists, Y, X.  Oh well! */
    public double getX() { update(); return x; }
    public double getY() { update(); return y; }
    public double getZ() { update(); return z; }
    public boolean getExists() { update(); return exists; }  // what an ugly name

    public void setX(double val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Double3D(val, getY(), getZ()), gui);
        }

    public void setY(double val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Double3D(getX(), val, getZ()), gui);
        }

    public void setZ(double val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Double3D(getX(), getY(), val), gui);
        }
    }
