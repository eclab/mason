/*
 * ParkLand.java
 *
 * $Id: ParkLand.java 1613 2013-02-12 20:34:08Z jharri $
 *
 */

package riftland.parcel;

/** Represents a parcel of state park land
 *
 */
public class ParkLand extends Parcel
{
    /** Used for color table lookup
     *
     */
    public static final double COLOR_VALUE = 3.0;


    private static final long serialVersionUID = 1L;
    

    public ParkLand()
    {
        super();
    }

    public ParkLand(int x, int y, int country)
    {
        super(x,y,country);
    }

    @Override
    public boolean isNavigable()
    {
        return false;
    }


    public double doubleValue()
    {
        return COLOR_VALUE;
    }

}
