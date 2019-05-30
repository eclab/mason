/*
 * Displacing.java
 *
 * $Id: Displacing.java 1723 2013-03-26 21:08:37Z escott8 $
 */

package sim.app.geo.riftland.household;

import sim.engine.SimState;

/** This activity is activated when the Household has failed and becomes an IDP. */
public class Displacing extends Activity
{
    Displacing(Household household, int people)
    {
        super(household);
        setPopulation(people);
    }

    public void addPeople(int people)
    {
        this.setPopulation(this.getPopulation() + people);
    }

    public void remove()
    {
        getHousehold().endDisplacing();
    }

    @Override
    public void step(SimState ss)
    {
        super.step(ss);
    }
}
