/*
 * ActivityAtLocation.java
 *
 * $Id: ActivityAtLocation.java 1761 2013-04-16 20:05:28Z jharri $
 */

package sim.app.geo.riftland.household;

import sim.app.geo.riftland.World;
import sim.app.geo.riftland.parcel.Parcel;

/**
 *
 */
abstract public class ActivityAtLocation extends Activity
{

    /** Where this activity is taking place
     *
     * XXX should consider making this more specific to GrazableArea since
     * all the current activities take place only on those parcel types
     */
    private Parcel location = null;


    public ActivityAtLocation(Household household, Parcel location)
    {
        super(household);
        
        this.location = location;
    }

    
    public Parcel getLocation()
    {
        return location;
    }

    public void setLocation(Parcel location)
    {
        this.location = location;
    }

	@Override
	abstract public void remove();

}
