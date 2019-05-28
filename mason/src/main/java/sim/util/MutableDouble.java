/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/**
   MutableDouble simply holds a double value, which can be changed at any time.
   Can't get much simpler than a class like this!  Note that this class is not synchronized,
   and shouldn't be used in a multithreaded environment without a lock.
   
   Note that MutableDouble hashes by ADDRESS and not by VALUE.  Thus
   (new MutableDouble(2)).equals(new MutableDouble(2)) is FALSE.
*/

public class MutableDouble extends Number implements Valuable, Cloneable /* automatically java.io.Serializable */
    {
    private static final long serialVersionUID = 1;

    public double val;

    public MutableDouble() { this.val = 0; }
    public MutableDouble(double val) { this.val = val; }
    public MutableDouble(MutableDouble md) { this.val = md.val; }

    public Double toDouble() { return new Double(val); }
        
    // written to implement the Number abstract class
    public double doubleValue() { return val; }
    public float floatValue() { return (float)val; }
    public int intValue() { return (int)val; }
    public long longValue() { return (long)val; }

    public Object clone()
        { 
        try 
            { 
            return super.clone(); 
            }
        catch(CloneNotSupportedException e)
            { 
            return null; // never happens
            } 
        }
                
    public String toString()
        {
        return "" + val;
        }

    public boolean isNaN()
        {
        return Double.isNaN(val);
        }
                
    public boolean isInfinite()
        {
        return Double.isInfinite(val);
        }
    }
