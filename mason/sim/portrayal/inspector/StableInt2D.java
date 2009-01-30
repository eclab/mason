package sim.portrayal.inspector;
import sim.util.*;
import sim.field.*;
import sim.field.grid.*;
import sim.field.continuous.*;

/**
   StableInt2D is a StableLocation for Int2D.  See StableLocation for more information.
*/

public class StableInt2D implements StableLocation
    {
    public int x = 0;
    public int y = 0;
    public boolean exists;
    public SparseGrid2D field;
    public Object object;
        
    public String toString()
        {
        if (!exists) return "Gone";
        else return "(" + x + ", " + y + ")"; 
        }
        
    public StableInt2D(SparseGrid2D field, Object object)
        {
        this.field = field;
        this.object = object;
        update();
        }
        
    public void update()
        {
        Int2D pos = null;
        if (field != null) pos = field.getObjectLocation(object);
        if (pos == null) { exists = false; }  // purposely don't update x and y so they stay the same
        else { x = pos.x; y = pos.y; exists = true; }
        }
            
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean getExists() { return exists; }  // what an ugly name
            
    public void setX(int val)
        {
        x = val;
        exists = true;
        if (field!=null) field.setObjectLocation(object, new Int2D(x,y));
        }

    public void setY(int val)
        {
        y = val;
        exists = true;
        if (field!=null) field.setObjectLocation(object, new Int2D(x,y));
        }

// playing with too much fire
/* 
   public void setExists(boolean val)
   {
   exists = val;
   if (exists)
   { if (field!=null) field.setObjectLocation(object, new Int2D(x,y)); }
   else
   { if (field!=null) field.remove(object); } // too powerful?
   }
*/
    }

