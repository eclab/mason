package riftland.household;

import cityMigration.DisplacementEvent;
import ec.util.MersenneTwisterFast;
import java.util.logging.Level;
import riftland.Parameters;
import riftland.Population;
import riftland.WaterHoles;
import riftland.World;
import riftland.parcel.GrazableArea;
import sim.field.grid.SparseGrid2D;

/**
 *
 * @author Eric 'Siggy' Scott
 */
class ActivityManager
{
    private final Parameters params;
    private final Household household;
    
    private Farming farming = null;
    private Herding herding = null;
    private Laboring laboring = null;
    private Displacing displacing = null;
    private int yearsSinceFarmingViable = 0;  // years since rebalancing produced
                                              // more than 0 farmers
                                              // This may belong in activityRestarts
    private int yearsSinceHerdingViable = 0;  // years since rebalancing produced
                                              // more than 0 farmers
                                              // This may belong in activityRestarts

    // XXX We can probably get rid of ActivityRestartHistory
    private final ActivityRestartHistory activityRestarts = new ActivityRestartHistory();

    
    /** WorkerAllocation is a temporary class I use to return multiple
     *  values from a function.
     */
    public class WorkerAllocation
    {	
	public int farmers;
	public int herders;
	public int laborers;
	
	public WorkerAllocation(int farmers, int herders, int laborers)
        {
		this.farmers = farmers;
		this.herders = herders;
		this.laborers = laborers;
	}
        
        // I won't even bother with accessor functions.
    }

    
    
    // <editor-fold defaultstate="collapsed" desc="Accessors">
    public Farming getFarming() { return farming; }
    
    public boolean hasFarming() { return farming != null; }

    public void endFarming()
    {
        farming = null;
//        assert(repOK());	// this is causing faulty errors when rebalanceWorkers has no laborers and decides to move everyone to labor
    }
    
    public Herding getHerding() { return herding; }
    
    public boolean hasHerding() { return herding != null; }

    public void endHerding() {
    	// if there are people in herding, move them to laboring
    	if (getHerdingPopulation() > 0) {
	    	int newLaborers = herding.getPopulation() + getLaboringPopulation();
	    	if (hasLaboring())
	    		getLaboring().setPopulation(newLaborers);
	    	else
	    		laboring = new Laboring(household, newLaborers, params);   	
    	}
    	
    	herding = null; 
        assert(repOK());
    }

    public Laboring getLaboring() { return laboring; }
    
    public boolean hasLaboring() { return laboring != null; }
    
    public void endLaboring()
    {
        laboring = null;
        assert(repOK());
    }

    public Displacing getDisplacing() { return displacing; }
    
    public boolean isDisplaced() { return displacing != null; }

    public void endDisplacing() { displacing = null; }
    
    private int getDisplacedPopulation() {
    	if (displacing == null)
    		return 0;
    	return displacing.getPopulation();
    }

    public int getFarmingPopulation() {
    	if (hasFarming())
    		return getFarming().getPopulation();
    	
    	return 0;
    }
    
    public int getHerdingPopulation() {
    	if (hasHerding())
    		return getHerding().getPopulation();
    	
    	return 0;
    }
    
    public int getLaboringPopulation() {
    	if (hasLaboring())
    		return getLaboring().getPopulation();
    	
    	return 0;
    }
    
    final public int getHouseholdPopulation()
    {
    	return getFarmingPopulation() + getHerdingPopulation() + getLaboringPopulation() + getDisplacedPopulation();
    }
    
    public void resetActivityRestartHistory()
    {
        activityRestarts.reset();
        assert(repOK());
    }
    
    public void resetHerdingRestartHistory()
    {
        activityRestarts.resetHerding();
        assert(repOK());
    }
    //</editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    private ActivityManager(Parameters params, Household household)
    {
        assert(params != null);
        assert(household != null);
        this.params = params;
        this.household = household;
    }
    
    /** Create an activity manager for a displaced Household. */
    public ActivityManager(Parameters params, Household household, int numDisplaced)
    {
        assert(params != null);
        assert(household != null);
        assert(numDisplaced > 0);
        this.params = params;
        this.household = household;
        this.displacing = new Displacing(household, numDisplaced);
        assert(repOK());
    }
    
    /** Initialize the activities with a specific number of people assigned to
     *  each task.
     */
    public ActivityManager(Parameters params, Household household, Population population, WaterHoles waterHoles, GrazableArea grazableArea, double farmArea, int numFarmers, int herdAssets, int numHerders, int numLaborers, MersenneTwisterFast random)
    {
        this(params, household);
        assert(population != null);
        assert(waterHoles != null);
        assert(grazableArea != null);
        assert(farmArea >= 0);
        assert(numFarmers >= 0);
        assert(!(numFarmers > 0 && farmArea == 0));
        assert(numHerders >= 0);
        
        if (household.canHerd() && numHerders > 0)
            startHerding(population, waterHoles, random, numHerders, herdAssets);
        
        if (household.canFarm() && numFarmers > 0){
            //System.out.println("AM1: " + farmArea);
            startFarmingActivity(population.getFarmingGrid(), grazableArea, farmArea, numFarmers, random);
        }
        
        if (numLaborers > 0)
            laboring = new Laboring(household, numLaborers, params);
        assert(repOK());
    }
    
    /** Initialize the population and activities of the Household, automatically
     * allocating people to each activity. */
    public ActivityManager(Parameters params, Household household, Population population, WaterHoles waterHoles, GrazableArea location, double farmArea_, int totalPeople, MersenneTwisterFast random)
    {
        this(params, household);
        assert(population != null);
        assert(waterHoles != null);
        assert(location != null);
        assert(farmArea_ >= 0);
        assert(totalPeople > 0);
        assert(random != null);
        
        // initialize labor with 1 or 10% which ever is larger
        int potentialLaborers = totalPeople / 10;	// integer division OK
        int potentialHerders = (totalPeople - potentialLaborers) / 2;	// integer division OK
        int potentialFarmers = totalPeople - potentialHerders - potentialLaborers;
        
        boolean tooCrowdedtoHerd = false;
        if (household.canHerd() && location.getPopulation() > 100){
            tooCrowdedtoHerd = true;
        }
        
        
        if (!household.canHerd() || tooCrowdedtoHerd)
        {
        	if (household.canFarm())
        		potentialFarmers += potentialHerders;
        	else
        		potentialLaborers += potentialHerders;
            potentialHerders = 0;
        }
        
        if (!household.canFarm())
        {
        	if (household.canHerd() && !tooCrowdedtoHerd)
        		potentialHerders += potentialFarmers;
        	else
        		potentialLaborers += potentialFarmers;
            potentialFarmers = 0;
        }
        assert(potentialFarmers + potentialHerders + potentialLaborers == totalPeople);
        
        // Create a new herdingGrid activitiy with about half the population and all of the household's TLUs
        if (potentialHerders > 0)
        {
            // pick a random herd size between the minimum needed to support the herders and 
            // the maximum number of TLUs the herders can handle
            int minTLUsPerHerder = params.herding.getTLUPerPFD();
            int maxTLUsPerHerder = params.herding.getMaxTLUsPerHerder();
            int herdSize = potentialHerders * (minTLUsPerHerder + random.nextInt(maxTLUsPerHerder - minTLUsPerHerder + 1));
            startHerding(population, waterHoles, random, potentialHerders, herdSize);
        }
        
        // Create a new farmingGrid activity and add it to this household
        if (potentialFarmers>0) {
           startFarmingActivity(population.getFarmingGrid(), location, farmArea_, potentialFarmers, random);
        }
        
        if (potentialLaborers > 0)
                laboring = new Laboring(household, potentialLaborers, params);
        
        assert(getHouseholdPopulation() == totalPeople); // FIXME This is know to fail.
        assert(repOK());
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Activity Allocation"> 
    
    /** Restart farming with one farmer */
    private void restartFarming(World world, GrazableArea where) {

        final int today = (int) world.schedule.getSteps();
        
//        GrazableArea where = (GrazableArea) household.getLocation();
        double desiredFarmArea = 1; // one hectare
        
        //System.out.println("Restarting farming");
        // If there is not enough land to restart our farm here
//        if (!FarmSearch.enoughLand(desiredFarmArea, where))
//        {
//            // TODO Try downsizing?
//            System.out.print("Out of space -- moved farm from " + where.getCoordinate().toCoordinates());
//            where = FarmSearch.findNewFarmLoc(world.getLand(), world.getPopulation(), household, desiredFarmArea, where, world.random);
//            System.out.println(" to " + where.getCoordinate().toCoordinates());
//        }
        
        if (where == null) {
        	System.err.format("Error: restartFarming, where is null\n");
        }
                
        if (household.getLocation() != where) {
        	World.getLogger().log(Level.FINE, String.format("Restarting farming, moving farm from %s to %s", 
        			household.getLocation().getCoordinate().toCoordinates(),
        			where.getCoordinate().toCoordinates()));
        }
        else
        	World.getLogger().log(Level.FINE, String.format("Restarting farming at %s\n", household.getLocation().getCoordinate().toCoordinates()));
        
        startFarmingActivity(world.getPopulation().getFarmingGrid(), where, desiredFarmArea, 1, world.random);

        int time = (int) world.schedule.getTime();
        household.getFarming().restartFarming(time, world.getRandom(), 1, world);
        //System.out.println("restarted farm has this much land:" + household.getFarmAreaInHectares());
        
        // XXX ActivityRestartHistory info really isn't used anymore.  Delete this.
        activityRestarts.setFarmingRestartTries(activityRestarts.getFarmingTries() - 1, today);
    }
    
    /** Restart herding with one herder */
    private void restartHerding(World world) {

        final int today = (int) world.schedule.getSteps();

        int herdSize = world.getParams().herding.getTLUPerPFD();

        // buy the herd
        double price = herdSize * world.getParams().herding.getPFDPerConsumedTLU();
        household.spendWealth(price);
        
        startHerding(world.getPopulation(), world.getWaterHoles(), world.getRandom(), 1, herdSize);
        activityRestarts.setHerdingRestartTries(activityRestarts.getHerdingTries() - 1, today);
        World.getLogger().log(Level.FINE, "@{0} HouseholdStep>considerStartingHerdingActivity: have enough TLUs to restart herding", world.schedule.getTime());
    }
    

    /** Should the household start herding? */
    private boolean shouldRestartHerding(World world)
    {
    	if (hasHerding() || !household.canHerd()) 
    		return false;

        final int today = (int) world.schedule.getSteps();

        // Have we restarted herding too recently?
        if (!activityRestarts.isHerdingRestartDelayOk(today, world.getRandom()))
        	return false;

        // Do we have enough wealth to buy a herd
        int neededTLUs = world.getParams().herding.getTLUPerPFD();
        double priceofNeededTLUs = neededTLUs * world.getParams().herding.getPFDPerConsumedTLU();
        double neededFoodReserves = 0;
        // calculate the amount of food needed to feed the farmers until next harvest
        if (hasFarming()) {
        	long daysUntilHarvest = getFarming().getNextHarvestDate() - today; 
        	neededFoodReserves = getFarmingPopulation() * daysUntilHarvest;
        }
        if (household.getWealth() < (priceofNeededTLUs + neededFoodReserves))
        	return false;
        
        // All tests OK
        return true;
    }

    /** Should the household either continue or restart herding? */
    private boolean shouldCommitToHerding(World world, int yearsSinceHerdingViable)
    {
    	if (!household.canHerd() || ((GrazableArea) household.getLocation()).getAvailableAreaInHectares() < 5.0 )
    		return false;

          final int today = (int) world.schedule.getSteps();

          // Have we restarted herding too recently?
          double r = world.getRandom().nextDouble();
          if (r > (params.herding.getHerdingRestartProbabilityExponent() / (double)yearsSinceHerdingViable))
          {
              return false;
          }

        // Do we have enough wealth to buy a herd
        int neededTLUs = world.getParams().herding.getTLUPerPFD();
        double priceofNeededTLUs = neededTLUs * world.getParams().herding.getPFDPerConsumedTLU();
        double neededFoodReserves = 0;
        // calculate the amount of food needed to feed the farmers until next harvest
        if (hasFarming()) {
        	long daysUntilHarvest = getFarming().getNextHarvestDate() - today; 
        	neededFoodReserves = getFarmingPopulation() * daysUntilHarvest;
        }
        if (household.getWealth() < (priceofNeededTLUs + neededFoodReserves))
        	return false;
        
        // All tests OK
        return true;
    }

    /**
     * Spin up a herding activity
     *
     * @see #household.processHerdingActivity
     */
    private void startHerding(Population population, WaterHoles waterHoles, MersenneTwisterFast random, int numHerders, int numTLUs)
    {
        assert (numTLUs <= numHerders * params.herding.getMaxTLUsPerHerder()) : 
        	String.format("%d herder(s) can't herd %d TLUs.", numHerders, numTLUs);
        
        if (numHerders == 0)
            return;
        // create new herding agent
        SparseGrid2D herdingGrid = population.getHerdingGrid();
        herding = new Herding(params, herdingGrid, household, waterHoles, random, (GrazableArea) household.getLocation(), numHerders, numTLUs);
    }
    
    /** Should the household restart farming? */
    private boolean shouldRestartFarming(World world)
    {
    	if (hasFarming() || !household.canFarm()) {
            //System.out.println("Restart: have farming already, or it is not allowed ");
            return false;
        }
    	
    	// If we have no land, don't start farming
//    	if (household.getFarmAreaInHectares() <= 0){
//            System.out.println("Restart: have no land, not restarting farming");
//            return false;
//        }
    	
        // If we tried farming too recently, don't restart yet
        final int today = (int) world.schedule.getSteps();
        if (!activityRestarts.isFarmingRestartDelayOk(today, world.getRandom())){	    	
            System.out.println("Restart: delay is not right");
            return false;
        }

        // Are there enough workers in other activities that we could poach one?
        int herdersAvailable = 0;
        int laborersAvailable = 0;
        if (household.hasHerding())
            herdersAvailable = household.getHerding().getPopulation() - household.getHerding().getMinimumHerdingStaff();

        if (household.hasLaboring())
            laborersAvailable = household.getLaboring().getPopulation();
        
        if (herdersAvailable + laborersAvailable < 1){
            //System.out.println("Restart: not enough people");
            return false;
        }
        //System.out.println("Restart: ought to be restarting");
        return true;

    } // ok to start farming
    
    private GrazableArea findPlaceToRestartFarming(World world) {
    	return FarmSearch.findNewFarmLoc(world.getLand(), world.getPopulation(), household, 1.0,
                (GrazableArea)household.getLocation(), world.random, params.farming.getMaxMoveDistanceForNewHousehold());
    }

    
    /** Should the household commit to farming in spite of poor results? */
    private boolean shouldCommitToFarming(World world, int yearsSinceFarmingViable, GrazableArea newLoc)
    {
        //System.out.println(yearsSinceFarmingViable);
        if (!household.canFarm()) {
            //System.out.println("Restart: have farming already, or it is not allowed ");
            return false;
        }
    	
    	// If we have no land, don't start farming
//    	if (household.getFarmAreaInHectares() <= 0){
//            System.out.println("Restart: have no land, not restarting farming");
//            return false;
//        }

        double r = world.getRandom().nextDouble();
        if (r > (params.farming.getFarmingRestartProbabilityExponent() / (double)yearsSinceFarmingViable))
        {
            //System.out.println("quitting farming: " + yearsSinceFarmingViable);
            return false;
        }

        // Are there enough workers in other activities that we could poach one?
//        int herdersAvailable = 0;
//        int laborersAvailable = 0;
//        if (household.hasHerding())
//            herdersAvailable = household.getHerding().getPopulation() - household.getHerding().getMinimumHerdingStaff();
//
//        if (household.hasLaboring())
//            laborersAvailable = household.getLaboring().getPopulation();
//        
//        if (herdersAvailable + laborersAvailable < 1){
//            //System.out.println("Restart: not enough people");
//            return false;
//        }
        //System.out.println("Restart: ought to be restarting");
        //System.out.println("not quitting farming" + yearsSinceFarmingViable);
        return true;

    } // ok to start farming


    private void startFarmingActivity(SparseGrid2D farmingGrid, GrazableArea where, double farmArea_, int farmers, MersenneTwisterFast random)
    {
        assert(farmArea_ >= 0); // Ought only to be zero temporarily!
//        if (farmArea_ < 1) 
//                System.out.println(farmArea_);
        farming = new FarmingVariableIntensity(params, farmingGrid, household, where, farmArea_, farmers, random);
    }
    
    //</editor-fold>


    public WorkerAllocation calculateWorkerAllocation(Household house,
                                                      int availablePopulation)
    {
    	double farmCon = house.getFarmingContribution();
    	double herdCon = house.getHerdingContribution();
    	double laborCon = house.getLaboringContribution();
    	
    	double totalContributions = farmCon + herdCon + laborCon;
//    	assert(totalContributions > 0) : String.format("Total contribution: %.1f, wealth: %.1f, farmers: %d, herders: %d, laborers: %d", 
//    			totalContributions, household.getWealth(), getFarmingPopulation(), getHerdingPopulation(), getLaboringPopulation());
    	double farmRatio = (totalContributions > 0) ? farmCon / totalContributions : 0;
    	double herdRatio = (totalContributions > 0) ? herdCon / totalContributions : 0;
    	
    	int newFarmers = hasFarming() ? (int)Math.round(farmRatio * availablePopulation) : 0;
    	int newHerders = hasHerding() ? (int)Math.round(herdRatio * availablePopulation) : 0;
//        if (hasFarming() && (newFarmers == 0) && (availablePopulation > 1)) newFarmers = 1; 
        int newLaborers = availablePopulation - newFarmers - newHerders;
        
        return new WorkerAllocation(newFarmers, newHerders, newLaborers);
    }


    /**
     * Consider restarting farming and/or herding if they've been latent.
     * Then allocate people according to how productive the household's activities 
     * have been since last harvest. 
     * @author Joey Harrison
     * @modified Jeff Bassett - Changed the logic to allow "restarting"
     *                          immediately after a bad year
     */
    public void rebalanceWorkers(World world)
    {
    	boolean committingToFarming = false, committingToHerding = false;
    	int availablePopulation = getHouseholdPopulation();
        WorkerAllocation alloc;
        GrazableArea newLoc = null;

    	// Consider restarting latent activities	
//    	if (shouldRestartFarming(world) && (availablePopulation > 0)) {
//    		availablePopulation--;
//    		restartFarming = true;
//    	}
//    	if (shouldRestartHerding(world) && (availablePopulation > 0)) {
//    		availablePopulation--;
//    		restartHerding = true;
//    	}

        alloc = calculateWorkerAllocation(household, availablePopulation);

        if (alloc.farmers > 0)
            yearsSinceFarmingViable = 0;
        else
        {
            yearsSinceFarmingViable++;
            if (shouldCommitToFarming(world, yearsSinceFarmingViable, newLoc) && (availablePopulation > 0) &&
            		((newLoc = findPlaceToRestartFarming(world)) != null))
            {
                availablePopulation--;
                committingToFarming = true;

                if (newLoc == null) {
                	System.err.println("Error: rebalanceWorkers, newLoc is null but shouldCommitToFarming returned true");
                }
            }
        }
        
        if (alloc.herders > 0)
            yearsSinceHerdingViable = 0;
        else
        {
            yearsSinceHerdingViable++;
            if (shouldCommitToHerding(world, yearsSinceHerdingViable) && availablePopulation > 0)
            {
                availablePopulation--;
                committingToHerding = true;
            }
        }
        
        alloc = calculateWorkerAllocation(household, availablePopulation);
        
        if (committingToFarming) {
    		assert(alloc.farmers == 0);
    		alloc.farmers = 1;
    	}
    	if (committingToHerding) {
    		assert(alloc.herders == 0);
    		alloc.herders = 1;
    	}
    	
    	assert(alloc.laborers >= 0);
    	assert(alloc.farmers + alloc.herders + alloc.laborers == household.getPopulation());
    	    	
    	if (hasFarming())
    		getFarming().setPopulation(alloc.farmers);
    	else if (committingToFarming)
            restartFarming(world, newLoc);
    	else if (alloc.farmers > 0)
    		System.err.println("Error: ActivityManager.rebalanceWorkers: Don't have farming, not restarting farming, yet farmers = " + alloc.farmers);
    	
    	if (hasHerding())
    		getHerding().setPopulation(alloc.herders);
    	else if (committingToHerding)
    		restartHerding(world);
    	else if (alloc.herders > 0)
    		System.err.println("Error: ActivityManager.rebalanceWorkers: Don't have herding, not restarting herding, yet herders = " + alloc.herders);
    	
    	if (hasLaboring())
    		getLaboring().setPopulation(alloc.laborers);
    	else if (alloc.laborers > 0)
    		laboring = new Laboring(household, alloc.laborers, params);
    
//        World.getLogger().log(Level.INFO, String.format("ActivityManager.rebalanceWorkers: farming: %d (%+d), herding: %d (%+d), laboring: %d (%+d), contributions: %.1f, %.1f, %.1f",
//                        getFarmingPopulation(), diffFarmers, getHerdingPopulation(), diffHerders, getLaboringPopulation(), diffLaborers, 
//                        farmCon, herdCon, laborCon));
        assert(repOK());
    }
    
    private double getAnnualBirthRate(World world, Household household_) {
        double repRate = 0; 
        
        int countryID = world.getLand().getCountry(household_.getLocation().getX(), household_.getLocation().getY());
        
        switch (countryID) {
        case 0: return 3.0e-2;		// placeholder
        case 1: return 3.46e-2;	// Burundi
        case 2: return 3.19e-2;	// Ethiopia
        case 3: return 2.46e-2;	// Kenya
        case 4: return 2.79e-2;	// Rwanda
        case 5: return 1.60e-2;	// Somalia
        case 6: return 2.48e-2;	// Sudan
        case 7: return 2.00e-2;	// Tanzania
        case 8: return 3.58e-2;	// Uganda
        case 9: return 2.61e-2;	// DR Congo
        default:
        	World.getLogger().log(Level.WARNING, "ActivityManager.getAnnualBirthRate: unknown country ID: "
                    + countryID + " at x,y: " 
                    + household_.getLocation().getX() +", "+ household_.getLocation().getY());
        	return 2.5;
        }
    }
    
   
    /**
     * Expand a household's population based on the current 
     * population and growth rate of the household's nation (determined by 
     * location, i.e., (x,y).
     * 
     * The national population rate is net of births and deaths.  So, we should not
     * model births or deaths explicitly.
     * 
     * @param world
     * @param household_
     * Author: Bill
     */
    public void adjustPopulation (World world, Household household_)
    {
        double repRate = getAnnualBirthRate(world, household_);
        repRate = repRate * 2.739726027e-3; // convert annual rate to daily. 2.739726027e-3 = 1/365
        
        int currentPopulation = getHouseholdPopulation();
        
        // probabilistically increase population by 1         
        if (repRate*currentPopulation > world.random.nextDouble())
        {
            // have both
            if (hasFarming() && hasHerding())
            {
                // place with larger
                if (herding.getPopulation() > farming.getPopulation())
                    household_.getHerding().setPopulation(herding.getPopulation() + 1); 
                else
                    farming.setPopulation(farming.getPopulation() + 1); 
            }
            
            // have only farming
            if (household_.hasFarming() && !hasHerding())
            {
                household_.getFarming().setPopulation(household_.getFarming().getPopulation() + 1); 
            }
            
            // have only herding
            if (hasHerding() && !hasFarming())
            {
                herding.setPopulation(herding.getPopulation() + 1); 
            }

            // has neither herding nor farming, but does have laboring
            if (!hasHerding() && !hasFarming() && hasLaboring())
            {
            	assert(hasLaboring());
            	household_.getLaboring().setPopulation(household_.getLaboring().getPopulation() + 1);
            }   

            // has no activities. 
            if (!hasHerding() && !hasFarming() && !hasLaboring())
            {
                World.getLogger().log(Level.FINE, " birth but not placed with activity...");                
            }   
        }
        assert(repOK());
    }
    
   
    /**
     * Expand a household's population based on the current 
     * population and growth rate of the household's nation (determined by 
     * location, i.e., (x,y).
     * 
     * The national population rate is net of births and deaths.  So, we should not
     * model births or deaths explicitly.
     * 
     * @param world
     * @param household_
     * Author: Bill
     */
    public void adjustPopulationAnnually(World world, Household household_)
    {
        double repRate = getAnnualBirthRate(world, household_);
        
        int currentPopulation = getHouseholdPopulation();
    	int births = (int)(repRate * currentPopulation);
    	double remainder = (repRate * currentPopulation) - births;
    	if (world.random.nextDouble() < remainder)
    		births++;
    	
    	if (births == 0)
    		return;
    	
    	// doesn't matter where we put the people since this happens right before worker rebalancing
    	if (hasFarming())
    		farming.setPopulation(farming.getPopulation() + births);
    	else if (hasHerding())
    		herding.setPopulation(herding.getPopulation() + births);
    	else if (hasLaboring())
    		laboring.setPopulation(laboring.getPopulation() + births);
    	else
    		World.getLogger().log(Level.WARNING, " birth but not placed with activity..."); 
    	
        assert(repOK());
    }
    
    /** A flag set while displaceHousehold() is being run, so repOK knows it's
     * temporarily okay to have no activities (a Displacing activity will be
     * created.
     */
    private boolean preparingToDisplace = false;
    /**
     * Give up on the households, everyone now on the move. <p> Take everyone
     * from any activities and move them to a new Displacing activity.
     *
     * TODO Eventually have the displacement model take over management of this
     * activity/people.
     */
    public void displaceHousehold(World world)
    {
        int peopleToDisplace = household.getPopulation();
        preparingToDisplace = true;
        
        if (hasFarming())
            getFarming().remove();

        if (hasHerding())
            getHerding().remove();

        if (hasLaboring())
            getLaboring().remove();
        
        assert (peopleToDisplace > 0);
        displacing = new Displacing(household, peopleToDisplace);
        preparingToDisplace = false;

        world.getPopulation().setCurrentPopulationDisplaced(world.getPopulation().getCurrentPopulationDisplaced() + peopleToDisplace);
        
        world.getPopulation().addDisplacementEvent(new DisplacementEvent(
        		world.schedule.getTime(), household));
        
        assert(repOK());
        // XXX Because there are now no longer any individuals that are farming,
        // herding, or laboring during the next step() this household will
        // be removed, including the new Displacing activity.  Therefore we may
        // wish to reconsider going through the effort of creating an activity
        // that is just going to be thrown away in the next step.
    }
    
    final public boolean repOK()
    {
    	assert(params != null);
    	assert(household != null);
    	assert(preparingToDisplace || (hasFarming() || hasHerding() || hasLaboring() || isDisplaced()));
    	assert(!isDisplaced() || !(hasFarming() || hasHerding() || hasLaboring()));
    	
        return params != null
                && household != null
                && activityRestarts != null
                && (preparingToDisplace || (hasFarming() || hasHerding() || hasLaboring() || isDisplaced()))
                && (!isDisplaced() || !(hasFarming() || hasHerding() || hasLaboring()));
    }

    private boolean haveNoNonDisplacingActivities() {
        return farming == null || herding == null || laboring == null;
    }
}