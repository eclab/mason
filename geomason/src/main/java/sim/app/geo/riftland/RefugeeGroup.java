/*
 * RefugeeGroup.java
 *
 * $Id: RefugeeGroup.java 1733 2013-04-01 20:33:47Z escott8 $
 */

package sim.app.geo.riftland;

import sim.app.geo.riftland.household.Household;
import java.util.ArrayList;

import sim.app.geo.cityMigration.CityMigrationModel;

import sim.engine.SimState;
import sim.engine.Steppable;

/** Represents a wandering group of refugees.
 *
 * 
 */
public class RefugeeGroup implements Steppable
{    
	private static final long serialVersionUID = 1L;

	public ArrayList<Household> households = new ArrayList<Household>();

	public PopulationCenter currentCity;
    public PopulationCenter destination;
    private double travelEnergy = 0;


    public RefugeeGroup()
    {
    }

    public int getPopulation()
    {
    	int total = 0;
    	for (Household h : households)
    		total += h.getDisplacing().getPopulation();
    			
    	return total;
    }
    
    public void setDestination(PopulationCenter city) {
    	destination = city;
    }
    
    public void addHousehold(Household h) {
    	households.add(h);
    }
    
    /**
     * 
     * RefugeeGroup.step:
	travelEnergy = model.dailyTravelDistance	// note: might accumulate over multiple days
	while (travelEnergy > 0)
		cityDestination = currentCity.calcDestination(this)
		if (cityDestination is-better-than destination)
			destination = cityDestination
		nextStep = firstStepToward(destination)
		if (travelEnergy < pathDistance(currentCity, nextStep))
			return		// no more movement today
			
		travelEnergy -= pathDistance(currentCity, nextStep)
		currentCity.removeRefugees(this)
		currentCity = nextStep
		nextStep.addRefugees(this)
		if (nextStep == destination)
			// households will be absorbed into nextStep
			remove from scheduler
			return
	end while	
     */
	@Override
	public void step(SimState state) {
		CityMigrationModel model = (CityMigrationModel)state;
		
		travelEnergy = model.travelPerDay;
		while (currentCity != destination) {
			PopulationCenter potentialDestination = model.findDestinationForIDPs(currentCity);
			destination = model.chooseBetterDestination(currentCity, destination, potentialDestination);
			PopulationCenter nextStep = model.calcFirstStepOnPath(currentCity, destination);
			double dist = model.workingNetwork.getPathLength(currentCity, nextStep);
			if (travelEnergy < dist && !model.moveOneCityPerStep)
				break;		// no more movement today
			
			// locomote!
			travelEnergy -= dist;
			currentCity.removeRefugees(this);
			nextStep.addRefugees(this);
			currentCity = nextStep;
			
			if (model.moveOneCityPerStep)
				break;
		}
		
		if (currentCity != destination)
			state.schedule.scheduleOnce(this);
	}
    


}
