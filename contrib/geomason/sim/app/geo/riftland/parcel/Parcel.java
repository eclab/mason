/*
 * Parcel.java
 * 
 * $Id: Parcel.java 1707 2013-03-20 21:43:50Z escott8 $
 */

package riftland.parcel;

import riftland.Weather;
import sim.util.Int2D;
import sim.util.Valuable;


/** Denotes a 1 km^2 of surface area
 *
 * 
 * TODO: add test harness for this
 */
public abstract class Parcel implements Valuable, java.io.Serializable
{
    /** Where the parcel is located  */
    private Int2D coordinate;
    final public Int2D getCoordinate() { return coordinate; }
    final public void setCoordinate(int x, int y) { coordinate = new Int2D(x,y); }
    private Int2D rainfallPatch;
    final public Int2D getRainfallPatch() { return rainfallPatch; }
    final public void setRainfallPatch(int x, int y) { rainfallPatch = new Int2D(x,y); }
    
    /** Country of which this parcel is a part. */
    private int country;
    final public int getCountry() { return country; }
    final void setCountry(int val) { country = val; }
    

    Parcel()
    {
        super();
    }


    public Parcel(int x, int y, int country)
    {
        coordinate = new Int2D(x,y);
        this.country = country;        
    }


    public int getX()
    {
        return coordinate.x;
    }

    public int getY()
    {
        return coordinate.y;
    }


    /**
     * @return true if an agent can move through this parcel
     *
     * Generally GrazableAreas are navigable whereas all other types of parcels
     * are not.
     * 
     */
    public abstract boolean isNavigable();



    /** Returns the amount of vegetation change between the current and previous
     *  step
     * <p>
     * Note that this is over-ridden in GrazableArea, which is the only subclass
     * for which this value makes sense.  Behold the power of polymorphism!
     *
     * @return the amount of vegetation that has changed from previous day
     */
    public double getVegetationChange()
    {
        return 0.0;
    }



    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Parcel other = (Parcel) obj;
        if (this.coordinate != other.coordinate && (this.coordinate == null || !this.coordinate.equals(other.coordinate)))
        {
            return false;
        }
        return true;
    }



    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 23 * hash + (this.coordinate != null ? this.coordinate.hashCode() : 0);
        return hash;
    }



}

