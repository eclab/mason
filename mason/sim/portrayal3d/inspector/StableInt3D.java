/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.inspector;
import sim.util.*;
import sim.field.*;
import sim.field.grid.*;
import sim.field.continuous.*;
import sim.portrayal.inspector.*;

/**
   StableInteD is a StableLocation for InteD, usable for SparseGrid2D and SparseGrid3D.  See StableLocation for more information.
*/

public class StableInt3D implements StableLocation
    {
    public int x = 0;
    public int y = 0;
    public int z = 0;
    public boolean exists = false;
    public SparseField field;
    public Object object;
        
    public String toString()
        {
        update();
        if (!exists) return "Gone";
        else return "(" + x + ", " + y + ", " + z + ")"; 
        }
        
    public StableInt3D(SparseGrid2D field, Object object)
        {
        this.field = field;
        this.object = object;
        }
        
    public StableInt3D(SparseGrid3D field, Object object)
        {
        this.field = field;
        this.object = object;
        }
        
    void update()
        {
        Int3D pos = null;
        if (field == null) return;
        if (field instanceof SparseGrid2D)
            pos = new Int3D(((SparseGrid2D)field).getObjectLocation(object));
        else
            pos = ((SparseGrid3D)field).getObjectLocation(object);
        if (pos == null) { exists = false; }  // purposely don't update x and y and z so they stay the same
        else { x = pos.x; y = pos.y; z = pos.z; exists = true; }
        }
            
    public int getX() { update(); return x; }
    public int getY() { update(); return y; }
    public int getZ() { update(); return z; }
    public boolean getExists() { update(); return exists; }  // what an ugly name
            
    public void setX(int val)
        {
        if (field == null) return;
        if (field instanceof SparseGrid2D)
            { ((SparseGrid2D)field).setObjectLocation(object, new Int2D(val,getY()));  z = 0; }
        else ((SparseGrid3D)field).setObjectLocation(object, new Int3D(val,getY(),getZ()));
        x = val;
        exists = true;
        }

    public void setY(int val)
        {
        if (field == null) return;
        if (field instanceof SparseGrid2D)
            { ((SparseGrid2D)field).setObjectLocation(object, new Int2D(getX(),val));  z = 0; }
        else ((SparseGrid3D)field).setObjectLocation(object, new Int3D(getX(),val,getZ()));
        y = val;
        exists = true;
        }

    public void setZ(int val)
        {
        if (field == null) return;
        if (field instanceof SparseGrid2D) { z = 0; return; }
        else ((SparseGrid3D)field).setObjectLocation(object, new Int3D(getX(),getY(),val));
        z = val;
        exists = true;
        }
    }

