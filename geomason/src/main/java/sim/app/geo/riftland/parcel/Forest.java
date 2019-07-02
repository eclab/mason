/*
 * Forest.java
 *
 * $Id: Forest.java 1613 2013-02-12 20:34:08Z jharri $
 *
 */

package sim.app.geo.riftland.parcel;

/** Represents a parcel of forest
 *
 */
public class Forest extends Parcel
{
    /** Used for color table lookup
     *
     */
    public static final double COLOR_VALUE = 4.0;


    private static final long serialVersionUID = 1L;
    

    public Forest()
    {
        super();
    }

    public Forest(int x, int y, int country)
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
