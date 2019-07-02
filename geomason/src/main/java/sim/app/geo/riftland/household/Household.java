/*
 * Household.java
 *
 * $Id: Household.java 2008 2013-08-19 18:21:25Z hkarbasi $
 */
package sim.app.geo.riftland.household;

import ec.util.MersenneTwisterFast;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.Population;
import sim.app.geo.riftland.WaterHoles;
import sim.app.geo.riftland.World;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.app.geo.riftland.parcel.Parcel;
import sim.app.geo.riftland.parcel.WaterHole;
import sim.app.geo.riftland.util.CachedDistance;
import sim.engine.SimState;
import sim.engine.Steppable;
import java.util.logging.Level;

/**
 * Represents a family unit.
 *
 * TODO: should have more overt support for removing Household?
 */
public class Household implements Steppable
{
    // <editor-fold defaultstate="collapsed" desc="Fields">
    private static final long serialVersionUID = 1L;
    
    private ActivityManager activityManager;
    private Parcel location = null;   
    private Parameters params;
    
    /** The culture ID of this household according to Murdoch map. */
    private int culture = 0;
    /** The country to which this Household belongs. */
    private int citizenship = 0;    
    
    /** The amount of food available in person-food-days, i.e., the amount of food one person eats in one day.
     * This includes food from all activities. */
    private double wealth = 0;
	public double getWealth() { return wealth; }
	public void setWealth(double val) { wealth = val; }

    /** Date on which the household rebalances its workers. Synced to harvest if it exists. */
    private int endOfPseudoAnnualWorkCycle = 365;

	/** The total food that has been provided by farming. Reset at endOfPseudoAnnualWorkCycle. */
    private double farmingContribution = 0;
	public double getFarmingContribution() { return farmingContribution; }
	public void setFarmingContribution(double val) { farmingContribution = val; }
    
    /** The total food that has been provided by herding. Reset at endOfPseudoAnnualWorkCycle. */
    private double herdingContribution = 0;
	public double getHerdingContribution() { return herdingContribution; }
	public void setHerdingContribution(double val) { herdingContribution = val; }

    /** The total food that has been provided by laboring. Reset at endOfPseudoAnnualWorkCycle. */
    private double laboringContribution = 0;
	public double getLaboringContribution() { return laboringContribution; }
	public void setLaboringContribution(double val) { laboringContribution = val; }
    
    private double farmAreaInHectares = 0.0;

    /** True if we can have a viable herding activity. Requires a nearby watering hole. */
    private boolean canHerd = true;
    /** True if we are permitted to farm. */
    private boolean canFarm = true; // XXX make this final once it's been tested.
    
    
    
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Accessors">
    /** @return Cash made from Laboring, or NaN if there is no Laboring activity */
    
    public boolean canFarm()
    {
        return canFarm;
    }

    
    public boolean canHerd()
    {
        return canHerd;
    }

    public int getEndOfPseudoAnnualWorkCycle() {
        return endOfPseudoAnnualWorkCycle;
    }
    
    public void setEndOfPseudoAnnualWorkCycle(int val) {
		endOfPseudoAnnualWorkCycle = val;
	}
    
    void depositGrain(double amount) {
    	wealth += amount;
    	farmingContribution += amount;
    }
    
    void depositFoodFromHerd(double amount) {
        wealth += amount;
    	herdingContribution += amount;
    }
    
    void depositCash(double amount) {
    	wealth += amount;
    	laboringContribution += amount;
    }
    
    /**
     * Spend the given amount of wealth if its available. If not, return
     * the amount available to spend.
     * @return the amount spent
     */
    double spendWealth(double amount) {
    	double amountSpent = Math.min(amount, wealth);
    	wealth -= amountSpent;
    	return amountSpent;
    }
    
    void setLocation(Parcel location)
    {
        this.location = location;
    }

    public Parcel getLocation()
    {
        return location;
    }

    public int getCitizenship()
    {
        return citizenship;
    }

    public int getCulture()
    {
        return culture;
    }

    public Farming getFarming()
    {
        return activityManager.getFarming();
    }

    void endFarming()
    {
        activityManager.endFarming();
        setFarmAreaInHectares(0);
    }
    
    public Herding getHerding()
    {
        return activityManager.getHerding();
    }

    void endHerding()
    {
        activityManager.endHerding();
    }

    public Laboring getLaboring()
    {
        return activityManager.getLaboring();
    }
    
    public void endLaboring()
    {
        activityManager.endLaboring();
    }
    
    public Displacing getDisplacing()
    {
        return activityManager.getDisplacing();
    }

    void endDisplacing()
    {
        activityManager.endDisplacing();
    }

    /** @return true iff we have actual farming */
    public boolean hasFarming()
    {
        return activityManager.hasFarming();
    }

    /** @return true iff we have actual herding*/
    public boolean hasHerding()
    {
        return activityManager.hasHerding();
    }

    /** @return true iff we have actual labor*/
    public boolean hasLaboring()
    {
        return activityManager.hasLaboring();
    }

    /** @return true iff we have actual displacing unit.*/
    public boolean isDisplaced()
    {
        return activityManager.isDisplaced();
    }
    
    public double getFarmAreaInHectares()
    {
        return this.farmAreaInHectares;
    }
    
    public double getFarmAreaInKm2()
    {
        return this.farmAreaInHectares/100.0;
    }
    
    public void setFarmAreaInHectares(double farmArea_)
    {
        double desiredFarmAreaChange = farmArea_ - farmAreaInHectares;
        double actualFarmAreaChange = ((GrazableArea)getLocation()).adjustLandClaim(desiredFarmAreaChange);
        farmAreaInHectares += actualFarmAreaChange;
    }
    
    public int getPopulation()
    {
        return activityManager.getHouseholdPopulation();
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
    // </editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Constructors">

    /** Bare-bones Household.  */
    private Household(Parameters params, GrazableArea location, int culture, int citizenship)
    {
        assert(params != null);
        assert(culture >= 0);
        this.params = params;
        this.location = location;
        this.culture = culture;
        this.citizenship = citizenship;
    }
    
    /** Displaced Household. */
    Household(Parameters params, GrazableArea location, int culture, int citizenship, int size)
    {
        assert(params != null);
        assert(culture >= 0);
        this.params = params;
        this.location = location;
        this.culture = culture;
        this.citizenship = citizenship;
        activityManager = new ActivityManager(params, this, size);
    }
    
    /** Build a Household from a HouseholdParameters object. */
    Household(HouseholdParameters householdParameters, Population population, WaterHoles waterHoles, MersenneTwisterFast random)
    {
        this(householdParameters.getParams(), householdParameters.getLocation(), householdParameters.getCulture(), householdParameters.getCitizenship());
        assert(householdParameters.allValuesSet());
        this.canHerd = householdParameters.canHerd();
        double farmArea = householdParameters.getFarmArea();
        int numFarmers = householdParameters.getNumFarmers();
        int herdAssets = householdParameters.getHerdSize();
        int numHerders = householdParameters.getNumHerders();
        int numLaborers = householdParameters.getNumLaborers();
        this.wealth = householdParameters.getWealth();
        
        setFarmingAndHerdingCapability(waterHoles, location);
        activityManager = new ActivityManager(params, this, population, waterHoles, (GrazableArea)location, farmArea, numFarmers, herdAssets, numHerders, numLaborers, random);
    }
    

    /**
     * Create a Household associated with a given parcel.
     *
     * This entails creating a Household and then assigning a Farming activity
     * for that parcel.
     *
     * @param params
     * @param parcel
     * @param culture ID of the household's ethnicity
     * @param citizenship to what country this household belongs
     *
     */
    public Household(Parameters params, WaterHoles waterHoles, Population population, GrazableArea parcel, double farmArea, int culture, int citizenship, int totalPeople, MersenneTwisterFast random)
    {
        this(params, parcel, culture, citizenship);
        assert(waterHoles != null);
        assert(population != null);
        assert(parcel != null);
        assert(farmArea >= 0);

        // initialize household assets
        
        // initialize end of PsuedoAnnual Work Cycle to a random time
        endOfPseudoAnnualWorkCycle = random.nextInt(365) + 90;
        
        // enough food for all farmers (i.e. half the household) for some
        // amount of time between 1 and 2 years
//        wealth = (endOfPseudoAnnualWorkCycle + 365) * totalPeople * params.households.getInitialEndowmentInYears();
        wealth = (365 * params.households.getInitialEndowmentInYears() + endOfPseudoAnnualWorkCycle) * totalPeople;
        
        synchronized(population.getHouseholdsGrid())
        {
            population.getHouseholdsGrid().setObjectLocation(this, parcel.getX(), parcel.getY());
        }

        setFarmingAndHerdingCapability(waterHoles, location);
        activityManager = new ActivityManager(params, this, population, waterHoles, parcel, farmArea, totalPeople, random);
    }
    
    /**
     * Determine whether or not this household can farm and herd. It first checks the model parameters,
     * then checks the viability of this particular location.
     * @param waterHoles
     * @param parcel
     */
    private void setFarmingAndHerdingCapability(WaterHoles waterHoles, Parcel parcel) {
    	if (!params.households.canHerd())
    		canHerd = false;
    	else {
            WaterHole nearestWaterHole = waterHoles.getNearestWaterHole(parcel.getX(), parcel.getY());
            if (nearestWaterHole == null || CachedDistance.distance(parcel.getCoordinate(), nearestWaterHole.getLocation()) > params.herding.getMigrationRange())
            	canHerd = false;
            else
            	canHerd = true;
    	}

        canFarm = params.households.canFarm();
    }

    /**
     * Create a displaced household of a given size
     *
     * @param params
     * @param parcel
     * @param culture ID of the household's ethnicity
     * @param citizenship to what country this household belongs
     * @param size number of people in the displaced household
     *
     * @return newly created Household 
     */
    public static Household createDisplacedHousehold(Parameters params, GrazableArea parcel, int culture, int citizenship, int size)
    {
        Household household_ = new Household(params, parcel, culture, citizenship, size);
        return household_;
    }

    /**
     * Create a displaced household of a given size
     *
     * @param params
     * @param culture ID of the household's ethnicity
     * @param citizenship to what country this household belongs
     * @param size number of people in the displaced household
     *
     * @return newly created Household 
     */
    public static Household createDisplacedHousehold(Parameters params, int culture, int citizenship, int size)
    {
    	return createDisplacedHousehold(params, null, culture, citizenship, size);
    }
    //</editor-fold>
    
    @Override
    public void step(SimState ss)
    {
        World world = (World)ss;
        int today = (int)world.schedule.getSteps();
        
        assert(getPopulation() > 0);

        // All people in all activities eat their daily meals.
        feedEveryone();

        // adjust population (all births/deaths combined at household level and assigned to herding or farming
//        activityManager.adjustPopulation(world, this);
        
        if (hasFarming())
            processFarmingActivity(world);
        if (hasHerding())
            processHerdingActivity(world);
        if (hasLaboring())
            processLaboringActivity(world);
        
        // implement daily tax
        setWealth( wealth * (1.0 - world.getParams().households.getAnnualTaxRate()*2.7397260274e-3) ); // 2.7397260274e-3 = 1/365
        
        // Check to see if the household needs to get displaced
        if (wealth < 0) {
        	displaceHousehold(world);
        	return;
        }

        if (today == endOfPseudoAnnualWorkCycle) {
        	activityManager.adjustPopulationAnnually(world, this);
        	if (today > 365)
        		activityManager.rebalanceWorkers(world);
    		farmingContribution = 0;
    		herdingContribution = 0;
    		laboringContribution = 0;
    		endOfPseudoAnnualWorkCycle += 365;	// this may get overriden by future harvest dates
    		
    		// Check to see if the household should split
    		if (getPopulation() > world.getParams().households.getHouseholdSplitPopulation())
    			split(world);
    	}
//        printHouseholdTrace(world,this);
    }

    // <editor-fold defaultstate="collapsed" desc="General Mutators">
    
    /** Split this Household into two, and register the child Household in the
     *  scheduler & householdsGrid.
     */
    private void split(World world)
    {
        Population population = world.getPopulation();
        int today = (int) world.schedule.getSteps();
        Household newHousehold = HouseholdSplitter.splitHousehold(today, this, params, world.getWaterHoles(), world.getLand(), population, world.getRandom());
        population.getHouseholdsGrid().setObjectLocation(newHousehold, newHousehold.getLocation().getX(), newHousehold.getLocation().getY());
        population.getHouseHoldRandomSequence().addSteppable(newHousehold);
    }
    
    /** Reset the history of what activities have been attempted by this
     * Household.  After this is called, activities will be attempted soon no
     * matter how often they have failed in the past. */
    public void resetActivityRestartHistory()
    {
        activityManager.resetActivityRestartHistory();
    }
    
    /** Reset the history of when herding has been attempted by this Household.
     *  After this is called, Herding will be attempted again soon no matter how
     *  often it has failed in the past.
     */
    public void resetHerdingRestartHistory()
    {
        activityManager.resetHerdingRestartHistory();
    }
    
    /** This function gets called when a harvest has been scheduled. It's really
     *  more of an event handler.
     */
    void harvestScheduled(int harvestDate) {
    	if (Math.abs(harvestDate - endOfPseudoAnnualWorkCycle) < 90)
    		endOfPseudoAnnualWorkCycle = harvestDate;
    }
    
    /**
     * Remove the entire household <p> This not only means removing the
     * household from the sparse grid of households, but it also has to be
     * removed from the MutableRandomSequence of households. Moreover, all
     * consitutuent parts/activites need to be shut down, if they haven't
     * already been shut down.
     */
    private void remove(World world)
    {
        // First remove this from the households grid
        world.getPopulation().getHouseholdsGrid().remove(this);

        // Now remove this from the HouseholdsRandomSequence
        world.getPopulation().getHouseHoldRandomSequence().removeSteppable(this);

        if (hasFarming())
            getFarming().remove();

        if (hasHerding())
            getHerding().remove();

        if (hasLaboring())
            getLaboring().remove();

        if (isDisplaced())
            // XXX But does it still exist/referred to elsewhere?
            getDisplacing().remove();
    }

    void setHerdSize(int deposit)
    {
        if (hasHerding())
            getHerding().setHerdSize(deposit);
        else
            System.out.println("Illegal transfer of TLUs to household that does not have a Herding Unit!");
    }
    
    void liquidateTLUs(int howMany)
    {
    	assert(howMany >= 0);
        // convert to cash
    	depositFoodFromHerd(howMany * params.herding.getPFDPerConsumedTLU());

        getHerding().setHerdSize(getHerding().getHerdSize() - howMany);
    }
    
    private void feedEveryone() {
    	int population = getPopulation();
    	// if we can't feed the whole household, liquidate TLUs until we can
    	while ((population > wealth) && hasHerding() && (getHerding().getHerdSize() > 0)) {
			World.getLogger().log(Level.FINE, String.format("Household unable to feed %d people with %.1f wealth, so liquidating a TLU",
					getPopulation(), getWealth()));
			liquidateTLUs(1);
    	}
    	
    	// eat
    	wealth -= population;
    }

    int getHerdSize()
    {
        if (hasHerding())
            return getHerding().getHerdSize();
        else
            return 0;
    }

    double getLaborFraction()
    {
        double fract = 0.0;
        if (this.getLaboring() != null)
            fract = ((double) activityManager.getLaboring().getPopulation() / (double) this.getPopulation());

        return fract;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Farming Activity">

    /**
     * Kill off farming if it is no longer viable or spawn off a new farming
     * activity in a new household if it is too large.
     */
    private void processFarmingActivity(World world)
    {
        getFarming().step(world);
        
        // check to see if the farming activity was shut down during the step
        if (!hasFarming())
        	return;
        
        assert(getFarmAreaInKm2() > 0);

        // If there are no more farmers, clear the activity
        if (getFarming().getPopulation() <= 0)
        {
            World.getLogger().log(Level.FINE, 
            	String.format("%.3f Farm removed because there are no more farmers. Wealth: %.2f", 
            	world.schedule.getTime(), getWealth()));
            getFarming().remove();
        }
    }

    public void harvestCompleted(int today, MersenneTwisterFast random, World world) {
    }
    
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Herding Activity">

    /** daily monitoring of herding activity at household level */
    private void processHerdingActivity(World world)
    {
        getHerding().step(world);
    }
    
    // </editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Labor Activity">
    
    private void processLaboringActivity(World world)
    {
        // Have labor do its daily routine
        getLaboring().step(world);

        if (hasLaboring() && this.getLaboring().getPopulation() == 0)
        {
            getLaboring().remove();
            World.getLogger().log(Level.FINE, "Household>processLaboring> no laborers, removed laboring");
        }
    }

    // </editor-fold>
    
    private void printHouseholdTrace(World world_, Household household_)
    {
        System.out.print("@" + world_.schedule.getTime() + " printHouseholdTrace> ");

        // farming status
        if (hasFarming())
        {
            System.out.print(household_.getFarming().getPopulation() + " farmers");
            if (household_.getFarming().hasPlanted())
            {
                System.out.print(" crops planted;");
            }
        } else
        {
            System.out.print("no farm;");
        }

        // herding status
        if (hasHerding())
        {
            System.out.print(" " + household_.getHerding().getPopulation() + " herders & "
                + household_.getHerding().getHerdSize() + " TLUs");
            if (household_.getHerding().isAtWateringHole())
            {
                System.out.print(" at watering hole");
            }
            if (household_.getHerding().isAtHousehold())
            {
                System.out.print(" at farm");
            }
        } else
        {
            System.out.print(" no herd");
        }

        // labor status
        if (hasLaboring())
        {
            System.out.print(" " + household_.getLaboring().getPopulation() + " laborers");
        } else
        {
//            System.out.print(" no laboring");
        }
        
        System.out.format(" %f in wealth", household_.getWealth());

        System.out.println(".");
    }
    
	public void displaceHousehold(World world) {
		activityManager.displaceHousehold(world);
		
		// remove from the scheduler
        world.getPopulation().getHouseHoldRandomSequence().removeSteppable(this);
	}

}
