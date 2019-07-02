/*
 * UrbanArea.java
 *
 * $Id: UrbanArea.java 1613 2013-02-12 20:34:08Z jharri $
 *
 */

package sim.app.geo.riftland.parcel;

import sim.app.geo.riftland.PopulationCenter;

/**
 * Represents a 1 km^2 of developed area.
 */
public class UrbanArea extends Parcel
{
    /** Used for color table lookup
     *
     */
    public static final double COLOR_VALUE = 2.0;


    private static final long serialVersionUID = 1L;

    
    /** The population center that "owns" this UrbanArea
     * 
     */
    private PopulationCenter populationCenter = null;

    public UrbanArea()
    {
        super();
    }

    public UrbanArea(int x, int y, int country)
    {
        super(x,y,country);
    }

    public UrbanArea(PopulationCenter p)
    {
        this.populationCenter = p;
    }


    public PopulationCenter getPopulationCenter()
    {
        return populationCenter;
    }

    public void setPopulationCenter(PopulationCenter populationCenter)
    {
        this.populationCenter = populationCenter;
    }


    /**
     * @return color value used to lookup in color table in GUI version
     */
    public double doubleValue()
    {
        return COLOR_VALUE;
    }

    
    /**
     * 
     * @return false since agents cannot freely move through urban areas
     */
    @Override
    public boolean isNavigable()
    {
        return false;
    }
}
