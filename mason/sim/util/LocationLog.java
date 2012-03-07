package sim.util;

import sim.engine.*;

/** An experimental test object for performing assertions which log all location accesses in fields.  */

class LocationLog
    {
    static ThreadLocal local = new ThreadLocal();
    public static boolean assertsEnabled = false;
    
    static {
        assert assertsEnabled = true; // Intentional side effect!!!
    } 

    public static boolean test()
        {
        return true;
        }
        
    public static boolean set(Steppable agent)
        {
        local.set(agent);
        return true;  // we use this in assert so we always return true
        }
    
    public static boolean clear()
        {
        local.remove();
        return true;  // we use this in assert so we always return true
        }
    
    public static boolean it(Object field, Object location)
        {
        Steppable agent = (Steppable)(local.get());
        System.err.println("" + agent + "\t" + field + "\t" + location + "\t");        
        return true;  // we use this in assert so we always return true
        }
    }