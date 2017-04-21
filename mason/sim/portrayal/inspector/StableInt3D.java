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
   StableInt3D is a StableLocation for Int3D, usable for SparseGrid2D and SparseGrid3D.  See StableLocation for more information.
*/

public class StableInt3D implements StableLocation
    {
    public int x = 0;
    public int y = 0;
    public int z = 0;
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
        
    public StableInt3D(FieldPortrayal fieldPortrayal, Object object, GUIState gui)
        {
        this.gui = gui;
        this.fieldPortrayal = fieldPortrayal;
        this.object = object;
        }

    void update()
        {
        Int3D pos = null;
        if (fieldPortrayal == null) return;
        Object p = fieldPortrayal.getObjectLocation(object, gui);
        if (p == null)  { exists = false; }  // purposely don't update x and y and z so they stay the same
        else 
        	{
        	if (p instanceof Int3D)
				{
				pos = (Int3D)p;
				}
			else if (p instanceof Int2D)
				{
				pos = new Int3D((Int2D)p);
				}
			else 
				{
				throw new RuntimeException("StableInt3D expected an Int2D or Int3D position from underlying field portrayal " + fieldPortrayal);
				}
			x = pos.x; 
			y = pos.y; 
			z = pos.z; 
			exists = true; 
			}
        }
            
    public int getX() { update(); return x; }
    public int getY() { update(); return y; }
    public int getZ() { update(); return z; }
    public boolean getExists() { update(); return exists; }  // what an ugly name
            
    public void setX(int val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Int3D(val, getY(), getZ()), gui);
        }

    public void setY(int val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Int3D(getX(), val, getZ()), gui);
        }

    public void setZ(int val)
        {
        if (fieldPortrayal == null) return;
        fieldPortrayal.setObjectLocation(object, new Int3D(getX(), getY(), val), gui);
        }
    }

