/**
 * Freshwater.java
 *
 * $Id: FreshWater.java 1812 2013-05-13 20:20:58Z jharri $
 *
 */

package riftland.parcel;

/**
 *
 * Represents a 1 km^2 area of fresh water.  (E.g., over Lake Victoria)
 * 
 */
public class FreshWater extends Parcel
{
    /** Used for color table lookup
     *
     */
    public static final double COLOR_VALUE = 2.0;

    private static final long serialVersionUID = 1L;

    public FreshWater()
    {
        super();
    }

    public FreshWater(int x, int y, int country)
    {
        super(x,y,country);
    }
    

    /**
     * XXX What is this going to return?  Maybe a hardcoded constant to represent
     * fresh water for color mapping?
     * 
     * @return
     */
    public double doubleValue()
    {
        return COLOR_VALUE;
    }


    /**
     *
     * @return false since fresh water is not navigable by agents
     */
    @Override
    public boolean isNavigable()
    {
        return false;
    }

}
