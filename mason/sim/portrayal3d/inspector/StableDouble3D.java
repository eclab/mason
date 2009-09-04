package sim.portrayal3d.inspector;
import sim.util.*;
import sim.field.*;
import sim.field.grid.*;
import sim.field.continuous.*;
import sim.portrayal.inspector.*;

/**
   StableDouble3D is a StableLocation for Double3D.  It can be used with either 2D or 3D fields.  See StableLocation for more information.
*/

public class StableDouble3D implements StableLocation
    {
    public double x = 0;
    public double y = 0;
    public double z = 0;
    public boolean exists = false;
    public SparseField field;
    public Object object;

    public String toString()
        {
        if (!exists) return "Gone";
        else return "(" + x + ", " + y + ", " + z + ")"; 
        }
        
    public StableDouble3D(Continuous2D field, Object object)
        {
        this.field = field;
        this.object = object;
        update();
        }
        
    public StableDouble3D(Continuous3D field, Object object)
        {
        this.field = field;
        this.object = object;
        update();
        }
        
    public void update()
        {
        Double3D pos = null;
        if (field == null) return;
        if (field instanceof Continuous2D)
            pos = new Double3D(((Continuous2D)field).getObjectLocation(object));
        else
            pos = ((Continuous3D)field).getObjectLocation(object);
        
        if (pos == null) { exists = false; }  // purposely don't update x and y and z so they stay the same
        else { x = pos.x; y = pos.y; z = pos.z; exists = true; }
        }

    /* For some reason, the order of the parameters in the MASON windows will be Z, Exists, Y, X.  Oh well! */
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public boolean getExists() { return exists; }  // what an ugly name

    public void setX(double val)
        {
        x = val;
        if (field == null) return;
        if (field instanceof Continuous2D)
            { ((Continuous2D)field).setObjectLocation(object, new Double2D(x,y));  z = 0; }
        else ((Continuous3D)field).setObjectLocation(object, new Double3D(x,y,z));
        }
            
    public void setY(double val)
        {
        y = val;
        if (field == null) return;
        if (field instanceof Continuous2D)
            { ((Continuous2D)field).setObjectLocation(object, new Double2D(x,y));  z = 0; }
        else ((Continuous3D)field).setObjectLocation(object, new Double3D(x,y,z));
        }
            
    public void setZ(double val)
        {
        z = val;
        if (field == null) return;
        if (field instanceof Continuous2D)
            { ((Continuous2D)field).setObjectLocation(object, new Double2D(x,y));  z = 0; }
        else ((Continuous3D)field).setObjectLocation(object, new Double3D(x,y,z));
        }
    }
