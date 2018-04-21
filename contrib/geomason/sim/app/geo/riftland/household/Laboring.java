/*
 * Laboring.java
 *
 * $Id: Laboring.java 1825 2013-05-20 04:23:17Z escott8 $
 */

package riftland.household;

import riftland.Parameters;
import sim.engine.SimState;

/** Represents part of a Household working in a city that sends money back
 *
 * May become deprecated.
 * 
 */
public class Laboring extends Activity
{
    /** ID of PopulationCenter in which this Activity is occurring */
    private int city = 0;

    /** How much they have earned */
    private float cash = 0;
    
    private Parameters params;

    public Laboring(Household household, int laborers, Parameters params)
    {
        super(household);
    	this.params = params;
        setPopulation(laborers);
    }

    @Override
    public void remove()
    {
        // Remove this from the household
        getHousehold().endLaboring();
    }


    @Override
    public void step(SimState ss)
    {
        super.step(ss);

        // add pay into household assets
        getHousehold().depositCash(getPopulation() * params.households.getLaborProductionRate());
    }

    public float getCash()
    {
        return cash;
    }

    public int getCity()
    {
        return city;
    }

    public void setCity(int city)
    {
        this.city = city;
    }

}
