package sim.physics2D.util;

/** Angle handles adding angles and ensuring that the result never gets
 * above 2PI or below 0. 
 */
public class Angle
    {
    public final double radians;
        
    // Pre-compute some often used constants
    public static final double twoPI = 2 * Math.PI;
    public static final double halfPI = Math.PI / 2;

    public Angle(double radians)
        {
        this.radians = radians;
        }

    /** Adds two angles and makes sure the result doesn't
     * falls below 0 or above 2 P 
     */
    public Angle add(Angle other)
        {
        return add(other.radians);
        }

    /** Adds two angles and makes sure the result doesn't
     * falls below 0 or above 2 P 
     */
    public Angle add(double radians)
        {
        double newVal = radians + this.radians;
        while(newVal > twoPI)
            newVal -= twoPI;
                
        while(newVal < 0)
            newVal += twoPI;
                
        return new Angle(newVal);
        }
    }
