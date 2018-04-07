/*
 * SaltWater.java
 *
 * $Id: SaltWater.java 1613 2013-02-12 20:34:08Z jharri $
 */

package riftland.parcel;

/**
 * Represents 1 km^2 of salt water
 *
 * I.e., over the Indian Ocean
 * 
 */
public class SaltWater extends Parcel
{
    /** Used for color table lookup
     *
     */
    public static final double COLOR_VALUE = 1.0;


    private static final long serialVersionUID = 1L;

    
    public SaltWater()
    {
        super();
    }

    public SaltWater(int x, int y, int country)
    {
        super(x,y, country);
    }
    

    /**
     * XXX What does this mean?  Should it return a constant that's used to
     * map into a color table for rendering?
     * 
     * @return ???
     */
    public double doubleValue()
    {
        return COLOR_VALUE;
    }

    /**
     *
     * @return false since agents cannot move on SaltWater
     */
    @Override
    public boolean isNavigable()
    {
        return false;
    }

}
