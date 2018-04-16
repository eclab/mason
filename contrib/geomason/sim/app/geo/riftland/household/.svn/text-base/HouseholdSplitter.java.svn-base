package riftland.household;

import ec.util.MersenneTwisterFast;
import riftland.*;
import riftland.parcel.GrazableArea;
import riftland.parcel.WaterHole;
import riftland.util.CachedDistance;
import sim.util.Int2D;

import java.util.logging.Level;

/**
 * Static methods for splitting one Household into two.
 * 
 * @author Eric 'Siggy' Scott
 */
class HouseholdSplitter
{
    /**
     * Splitting a Household into two smaller households.
     *
     * @author CRC
     * @author Eric 'Siggy' Scott
     */
    static Household splitHousehold(int today, Household household, Parameters params, WaterHoles waterHoles, Land land, Population population, MersenneTwisterFast random)
    {
        assert(household.getLocation() != null);
        logHouseholdStats(household, "before");

        // create new household of same culture and citizenship
        HouseholdParameters newHouseholdData = new HouseholdParameters(params, (GrazableArea)household.getLocation(), household.getCulture(), household.getCitizenship());

        // initialize household assets
        
        double halfWealth = household.getWealth() / 2;
        newHouseholdData.setWealth(halfWealth);
        household.spendWealth(halfWealth);
        
        // Get a new location for the child Household
        GrazableArea newLocation;
        if (household.hasFarming())
        {
            double halfCurrentFarmArea = household.getFarmAreaInHectares() / 2.0;
            //double desiredFarmArea = Math.max(halfCurrentFarmArea, 1.0); // look for at least one hectare
            newLocation = FarmSearch.findNewFarmLoc(land, population, household, halfCurrentFarmArea,
                    (GrazableArea)household.getLocation(), random, params.farming.getMaxMoveDistanceForNewHousehold());
            
            // if a suitable location was found, it means we got the land
            if (newLocation != null)
            	newHouseholdData.setFarmArea(halfCurrentFarmArea);
           
            else { 	// if new household can't find a suitable location, take half of the parent household's land
            	newLocation = (GrazableArea)household.getLocation();
                // Knock the parent Household's land in half to make room for us.
                household.setFarmAreaInHectares(halfCurrentFarmArea);
                newHouseholdData.setFarmArea(halfCurrentFarmArea);
            }
        //    System.out.println("halfCurrentFarmArea:" + halfCurrentFarmArea + " desiredFarmArea: "+ desiredFarmArea + " newHouseholdData.getFarmArea: " + newHouseholdData.getFarmArea() );
        }
        else
            newLocation = (GrazableArea)household.getLocation();
        newHouseholdData.setLocation(newLocation);
        
        // Check whether the new household will be able to herd
        newHouseholdData.setCanHerd(canNewHouseholdHerd(params, newHouseholdData, waterHoles)); 
        
        int totalPopulation = household.getPopulation();
        
        // Move half the population of the parent into the child Household
        int spareWorkers = splitHerding(params, waterHoles, today, household, newHouseholdData);
        spareWorkers = splitFarming(spareWorkers, household, newHouseholdData);
        splitLaboring(spareWorkers, household, newHouseholdData);
        household.resetActivityRestartHistory();
        
        
        Household newHousehold = new Household(newHouseholdData, population, waterHoles, random);
        newHousehold.setEndOfPseudoAnnualWorkCycle(household.getEndOfPseudoAnnualWorkCycle());
        if (household.hasFarming() && newHousehold.hasFarming())
        	newHousehold.getFarming().copyStateFrom(household.getFarming());
        
        logSplit(household, newHousehold);
        assert(household.getPopulation() + newHousehold.getPopulation() == totalPopulation);

        logHouseholdStats(household, "after-orig");
        logHouseholdStats(newHousehold, "after-new");
        
        return newHousehold;
    }
    
    static private boolean canNewHouseholdHerd(Parameters params, HouseholdParameters newHouseholdData, WaterHoles waterHoles) {
    	if (!params.households.canHerd())
    		return false;

        // If there are no watering holes close enough, then herding is not a
        // viable activity for this household.
    	Int2D newLoc = newHouseholdData.getLocation().getCoordinate();
		WaterHole nearestWaterHole = waterHoles.getNearestWaterHole(newLoc.getX(), newLoc.getY());
		double distanceToNearestWaterHole = CachedDistance.distance(newLoc, nearestWaterHole.getLocation());
		if (distanceToNearestWaterHole > params.herding.getMigrationRange())
		{
		    World.getLogger().log(Level.FINE, "Herding not viable, nearest watering hole is too far away.");
		    return false;
		}
		
		return true;
    }
    
    // <editor-fold defaultstate="collapsed" desc="Split Activities">
    /**
     * Move half of this Household's farmers to a new Household if it has a
     * Farming activity.  If the new Household can't herd, you may specify a
     * number of "spare Herders" that will be converted into farmers.
     * 
     * @param spareWorkers Extra hands to be added onto the child Household's
     * farming population.
     * @param newHouseholdData Household to move them to.
     * @return Number of workers left over
     */
    private static int splitFarming(int spareWorkers, Household household, HouseholdParameters newHouseholdData)
    {
    	if (!household.hasFarming())
    		return spareWorkers;

        int newFarmingPopulation = household.getFarming().getPopulation() / 2;	// integer division OK
        if (newFarmingPopulation > 0) {
        	household.getFarming().setPopulation(household.getFarming().getPopulation() - newFarmingPopulation);
            newHouseholdData.setNumFarmers(newFarmingPopulation + spareWorkers);
            return 0;	// everybody got a job
        }
        
        return spareWorkers;
    }

    /**
     * Split the herd if it's large enough to split. If the herd was large enough
     * to split, the new household will get half the herders. Otherwise, those 
     * workers will be made available for other work by the new household.
     * 
     * @return Number of workers left over
     */
    private static int splitHerding(Parameters params, WaterHoles waterHoles, int today, Household household, HouseholdParameters newHouseholdData)
    {
        if (household.hasHerding() && (household.getHerding().getPopulation() >= 2)) {
	        int newHerdingPopulation = household.getHerding().getPopulation() / 2;	// integer division OK
            household.getHerding().setPopulation(household.getHerding().getPopulation() - newHerdingPopulation);
            household.resetHerdingRestartHistory();
            
            // if we can't herd, use the workers elsewhere
        	if (!newHouseholdData.canHerd())
        		return newHerdingPopulation; // these workers will be used elsewhere by the new household
            
            // is herd large enough to split?
        	int newHerdSize = household.getHerdSize() / 2; // Integer division OK
        	if (newHerdSize == 0)
        		return newHerdingPopulation; // these workers will be used elsewhere by the new household
        	
    		// split the herd and put the new workers in herding
    		newHouseholdData.setNumHerders(newHerdingPopulation);
            newHouseholdData.setHerdSize(newHerdSize); 
    		household.setHerdSize(household.getHerdSize() - newHerdSize);
        }
        
        return 0;	// all the workers were used
    }

    /**
     * Take half the household's laborers and give them to the new household. 
     * The new household will also put the spareWorkers in laboring.
     */
    private static void splitLaboring(int spareWorkers, Household household, HouseholdParameters newHouseholdData)
    {
        int newLaboringPopulation = 0;
        if (household.hasLaboring())
        {
            newLaboringPopulation = household.getLaboring().getPopulation() / 2;	// integer division OK
            household.getLaboring().setPopulation(household.getLaboring().getPopulation() - newLaboringPopulation);
        }

        newHouseholdData.setNumLaborers(newLaboringPopulation + spareWorkers);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Logging/Tracing">
    
    /** Print a trace of a household split operation. */
    private static void logSplit(Household household, Household newHousehold)
    {
        // if full household splitting, trace, if not just comment
        if (household.getFarming() != null && household.getHerding() != null
                && newHousehold.getFarming() != null && newHousehold.getHerding() != null)
        {
            World.getLogger().log(Level.FINE, "Household splitting: original now has " + household.getPopulation()
                    + "(" + household.getFarming().getPopulation() + " farmers, " + household.getHerding().getPopulation() + " herders)"
                    + " and new has " + newHousehold.getPopulation()
                    + "(" + newHousehold.getFarming().getPopulation() + " farmers, " + newHousehold.getHerding().getPopulation() + " herders)");
        }
        else
        {
            World.getLogger().log(Level.FINE, "Household splitting: original now has " + household.getPopulation()
                    + " and new household has " + newHousehold.getPopulation() + " total people, but both don't have farms and herds");
        }
        //printFullSplitTrace(today, newFarmLoc, newHousehold);
    }

    private void printFullSplitTrace(int today, GrazableArea newFarmLoc, Household household, Household newHousehold)
    {
        // show ithe original household
        System.out.print("@" + today + " Household>split>"
               + " original household (after the splitting into two) at ("
               + newFarmLoc.getX() + ", "
               + newFarmLoc.getY() + ")" + " with "
               + newHousehold.getPopulation() + " people total: ");

       if (newHousehold.hasFarming())
       {
           System.out.print(newHousehold.getFarming().getPopulation() + " farmers");
       }

       if (newHousehold.hasHerding())
       {
           System.out.print(", " + newHousehold.getHerding().getPopulation() + " herders & "
                   + newHousehold.getHerding().getHerdSize() + " TLUs");
       } else
       {
           System.out.print(", no herders");
       }


       if (newHousehold.hasLaboring())
       {
           System.out.print(", " + newHousehold.getLaboring().getPopulation() + " laborers");
       }

       System.out.println(".");


       // show ithe original household
       System.out.print("@" + today + " Household>split>"
           + " original household (after the splitting into two) at ("
           + newFarmLoc.getX() + ", "
           + newFarmLoc.getY() + ")" + " with "
           + household.getPopulation() + " people total: ");

       if (household.hasFarming())
       {
           System.out.print(household.getFarming().getPopulation() + " farmers");
       }

       if (household.hasHerding())
       {
           System.out.print(", " + household.getHerding().getPopulation() + " herders & "
               + household.getHerding().getHerdSize() + " TLUs");
       } else
       {
           System.out.print(", no herders");
       }

       if (household.hasLaboring())
       {
           System.out.print(", " + household.getLaboring().getPopulation() + " laborers");
       }

       // end print line
       System.out.println(".");
    }
    
	static void logHouseholdStats(Household h, String prefix) {
        World.getLogger().log(Level.FINE, String.format("Split (%s): loc: (%d, %d), f: %d, h: %d, l: %d, wealth: %.1f" , 
        		prefix,
        		h.getLocation().getX(), h.getLocation().getY(),
        		h.getFarmingPopulation(), h.getHerdingPopulation(), h.getLaboringPopulation(),
        		h.getWealth()));

	}
    // </editor-fold>
}
