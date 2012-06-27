package sim.util;

import sim.engine.*;

/** An experimental test object for performing assertions which log all location accesses in fields.  */

public class LocationLog
    {
    static ThreadLocal local = new ThreadLocal();
    public static boolean assertsEnabled = false;
    public static boolean propertyEnabled = (System.getProperty("LocationLog") != null);
    
    static {
        assert assertsEnabled = true; // Intentional side effect!!!
        } 

    public static boolean test()
        {
        return true;
        }
        
    public static boolean set(Steppable agent)
        {
        if (propertyEnabled)
            local.set(agent);
        return true;  // we use this in assert so we always return true
        }
    
    public static boolean clear()
        {
        if (propertyEnabled)             
            local.remove();
        return true;  // we use this in assert so we always return true
        }
    
    public static boolean it(Object field, Object location)
        {
        if (propertyEnabled)
            {
            Steppable agent = (Steppable)(local.get());
            System.err.println("" + agent + "\t" + field + "\t" + location + "\t");       
            } 
        return true;  // we use this in assert so we always return true
        }
    }