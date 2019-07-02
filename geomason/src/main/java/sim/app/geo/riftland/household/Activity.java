/*
 * Activity.java
 *
 * $Id: Activity.java 1814 2013-05-14 15:12:24Z escott8 $
 */

package sim.app.geo.riftland.household;

import sim.engine.SimState;


/** Corresponds to a Household activity
 *
 * An activity has a location and a number of participants.
 *
 */
public abstract class Activity
{
    /** How many individuals are participating in this activity.*/
    private int population = 0;

    /** After eat(), how many were unfed */
    private int unfed = 0;

    /** All activities are associated with a household */
    private Household household;
    

    /**
     * @param household to which this activity belongs
     */
    public Activity(Household household)
    {
        super();
        this.household = household;
    }

    /** thou shalt use the ctor that specifies a household */
    private Activity() {}
    
    public void step(SimState ss)
    {
        // TODO insert code that would be common for all Activities, which may
        // turn out to be nothing

        //World world = (World) ss;
    }
    
    abstract public void remove();
    
    public final int getPopulation()
    {
        return population;
    }

    public final void setPopulation(int population)
    {
    	assert(population >= 0);
        this.population = population;
        if (population == 0)
        	remove();
    }

    public final Household getHousehold()
    {
        return household;
    }

    public final void setHousehold(Household household)
    {
        this.household = household;
    }

}
