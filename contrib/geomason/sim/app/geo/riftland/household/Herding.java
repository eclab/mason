/*
 * Herding.java
 *
 * $Id: Herding.java 2029 2013-09-04 19:49:57Z escott8 $
 */
package riftland.household;

import ec.util.MersenneTwisterFast;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import riftland.Parameters;
import riftland.WaterHoles;
import riftland.World;
import riftland.parcel.GrazableArea;
import riftland.parcel.WaterHole;
import riftland.util.CachedDistance;
import sim.engine.SimState;
import sim.field.SparseField;
import sim.field.grid.SparseGrid2D;
import sim.util.Valuable;

/**
 *
 */
public class Herding extends ActivityAtLocation implements Valuable
{   
    // <editor-fold defaultstate="collapsed" desc="Fields">
    final protected SparseGrid2D herdingGrid;
    final protected Parameters params;
    final private WaterHoles waterHoles;
    
    /** How many animals do we have? */
    private int herdSize = 0;
    /** the amount of food the herd has
     *
     * TODO need to initialize this to a sane value
     *
     * @see #eat()
     * @see #adjustHerdSize(sim.field.SparseField, double)
     */
    private double food;
    /** the amount of water the herd has
     * <p>
     * TODO need to initialize this to a sane value
     *
     * @see #adjustHerdSize(sim.field.SparseField, double)
     * @see #herdDrink()
     */
    private double water;
    /* flag to note when waterhole is dry */
    private boolean drinkFailed = false;
    private int waterHoleRemainsBad = 0;
    /**
     * History of where the herder has traveled.
     *
     * @see riftland.gui.SnailTrailPortrayal
     */
    private LinkedList<GrazableArea> trail = new LinkedList<GrazableArea>();
    /** The current watering hole. */
    private WaterHole waterHole;
    
    /** The location we will move to next.
     * @see HerdMover */
    private GrazableArea nextLocation = null;

    private boolean goingToNewWaterhole = false;
    
    /** Fixed location jitter for visualization. */
    private final double jitterX;
    private final double jitterY;
    
    
    double getNumHerdersSupportedByHerdFoodProduction() {
    	return herdSize / (double)params.herding.getTLUPerPFD();
    }
    
    /** Get the minimum number of herders required to manage this herd. */
    public int getMinimumHerdingStaff() {
    	return (int)Math.ceil((double)getHerdSize() / params.herding.getMaxTLUsPerHerder());
    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Accessors">
    GrazableArea getNextLocation()
    {
        return nextLocation;
    }

    void setNextLocation(GrazableArea nextLocation)
    {
        this.nextLocation = nextLocation;
    }
    
    /**
     * @param wh that we may remember being a dried out watering hole
     *
     * @return true iff the given parcel corresponds to a dried out watering hole
     */
    private void setWaterHoleToNearest()
    {
        waterHole = waterHoles.getNearestWaterHole(getLocation().getX(), getLocation().getY());
    }

    /** @return the ordinal of the herding State */
    @Override
    public double doubleValue()
    {
        return 0;  // TODO
    }

    public void consumeFood(int amount) {
    	
    }

    /** Over-rides parent since we know we're always on GrazableArea and not
     * on a generic parcel.
     *
     * @return grazable area herd is on
     */
    @Override
    public final GrazableArea getLocation()
    {
        return (GrazableArea) super.getLocation();
    }

    /*@return iterator to the movement history */
    public Iterator<GrazableArea> trailIterator()
    {
        return trail.iterator();
    }

    /** @return the watering hole that currently serves as "base camp" */
    final protected WaterHole getCurrentWaterHole()
    {
        return waterHole;
    }
    
    /** Nulls out the current water hole. */
    final void removeWaterHole()
    {
        this.waterHole = null;
    }

    /** Set the herd food to a new value
     * <p>
     * Presumes that food is > 0; if not, then food is set to zero
     *
     * @param food is new amount of food in kilograms
     */
    public final void setHerdFood(double food)
    {
        //if (food < 0) {food = 0;}
        this.food = Math.min(food, params.herding.getTLUFoodMax() * getHerdSize());

    }

    /** Set the herd water amount to a new value
     * <p>
     * Presumes that water is > 0; if not, then water is set to zero
     * 
     * @param water is new amount of water in liters
     */
    public final void setHerdWater(double water)
    {
        //TRG removed limit to keep water from going negative
        //if (water < 0) {water =0;}
        this.water = Math.min(water, params.herding.getTLUWaterMax() * getHerdSize());
    }

    /**
     * XXX what units?
     *
     * @return the amount of food the herd has
     */
    public double getHerdFood()
    {
        return this.food;
    }

    /**
     * XXX what units?
     * 
     * @return the amount of water the herd has
     */
    public final double getHerdWater()
    {
        return this.water;
    }

    /** @return the maximum length of history trail */
    public final int getMaxTrailLength()
    {
        return params.herding.getMaxTrailLength();
    }

    public final int getHerdSize()
    {
        return herdSize;
    }

    /** Set the amount of animals in the herd
     * <p>
     * Presumes that 'herd' is positive; is set to zero if not
     *
     * @param herd is new number of animals we have
     */
    public final void setHerdSize(int herd)
    {
        this.herdSize = Math.max(herd, 0);
    }
    
    public final void setGoingToNewWaterhole (boolean status){
        goingToNewWaterhole = status;

    }
    
    public final boolean getGoingToNewWaterhole (){
        return goingToNewWaterhole;
    }

    /**
     * @return true if we're at the current watering hole
     *
     * @see #step(sim.engine.SimState)
     */
    boolean isAtWateringHole()
    {
        if (waterHole == null)
            return false;
        return getLocation().getX() == waterHole.getX()
                && getLocation().getY() == waterHole.getY();
    }

    /** @return true if we're at the household location */
    boolean isAtHousehold()
    {
        return getLocation().getX() == getHousehold().getLocation().getX()
                && getLocation().getY() == getHousehold().getLocation().getY();
    }

    /** @return true if really hungry or really thirsty */
    boolean isStressed()
    {
        if (getScaledHunger() > 0.99 || getScaledThirst() > 0.99)
            return true;
        else
            return false;
    }

    public double getJitterX() {
        return jitterX;
    }

    public double getJitterY() {
        return jitterY;
    }
    
    //</editor-fold>
    
    /**
     * @param world contains needed simulation state
     * @param household to which this herding belongs
     * @param land that the herd is on
     * @param returnTime is absolute time when herd returns to homestead
     * @param numHerdAnimals is the number of animals this activity has
     */
    Herding(Parameters params, SparseGrid2D herdingGrid, Household household, WaterHoles waterHoles, MersenneTwisterFast random, GrazableArea land, int numHerders, int numTLUs)
    {
        super(household, land);
        assert(params != null);
        assert(herdingGrid != null);
        assert(household != null);
        assert(waterHoles != null);
        assert(random != null);
        assert(land != null);
        assert(numHerders > 0);
        assert(numTLUs > 0);
        this.params = params;
        this.waterHoles = waterHoles;
        this.herdingGrid = herdingGrid;
        setPopulation(numHerders);
        setHerdSize(numTLUs);
        this.jitterX = random.nextDouble()*0.8 - 0.4;
        this.jitterY = random.nextDouble()*0.8 - 0.4;
        
        synchronized(herdingGrid)
        {
            herdingGrid.setObjectLocation(this, land.getX(), land.getY());
        }
        setWaterHoleToNearest();
        land.addHerd(this);

        // initialize herd food & water at max for departure
        setHerdFood(params.herding.getTLUFoodMax() * getHerdSize());
        setHerdWater(params.herding.getTLUWaterMax() * getHerdSize());
    }

    // <editor-fold defaultstate="collapsed" desc="Computations">
    
    /**
     * Indicates the hunger of the herd, scaled based on max food and herd size.
     *
     * @return herd hunger
     */
    public final double getScaledHunger()
    {
        double val = 1.0 - (getHerdFood() / ((params.herding.getTLUFoodMax() * getHerdSize())));
        return val;
    }

    /**
     * Indicates the hydration of the herd, scaled based on max thirst and herd size.
     *
     * @return herd thirst
     */
    public final double getScaledThirst()
    {
        double val = 1.0 - (getHerdWater() / (params.herding.getTLUWaterMax() * getHerdSize()));
        return val;
    }
    
    /**
     * Decide to migrate if stressed or if it's time to go home for harvest.
     *
     * @return true if we need to start migrating
     */
    protected boolean shouldFindNewWaterHole(int time)
    {
        boolean shouldGo = false;
        // (any of these are true) AND not already going to a new water hole
        boolean needsNotMet = (drinkFailed || getScaledHunger() > 0.99 || shouldReturn(time)) && !getGoingToNewWaterhole();
        if (needsNotMet) {
            waterHoleRemainsBad++;
        }
        if (waterHoleRemainsBad > 1){
            setGoingToNewWaterhole(true);
            waterHoleRemainsBad = 0;
            shouldGo = true;
        }

        return shouldGo;
    }
    
    /** Number of days since we last checked if it was time to head toward home. */
    private int numDaysSinceLastReturnCheck = 0;

    /**
     * If the current time is greater than the preset return time
     * for the herd, then we need to start returning to the household.  This
     * only updates its result once every returnHomeCheckInterval days.
     *
     * @return true if the herder needs to return to household
     */
    protected boolean shouldReturn(int time)
    {   
        numDaysSinceLastReturnCheck++;
        if (numDaysSinceLastReturnCheck > params.herding.getReturnHomeCheckInterval())
        {
            numDaysSinceLastReturnCheck = 0;
            double distanceToHome = getLocation().getCoordinate().distance(getHousehold().getLocation().getCoordinate());
            if (daysTillReturn(time) <= distanceToHome)
                return true;
        }

        return false;
    }
    
    /** Get the number of days before the herd needs to arrive back home. */
    int daysTillReturn(int currentTime)
    {
        return getHousehold().getEndOfPseudoAnnualWorkCycle() - currentTime;
    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Mutators">

    /* Appends the given location to the travel history.
     *
     * This is a NOP if the maximum travel history length is zero.
     *
     * Will delete oldest location if adding new location will exceed
     * maxTrailLength.
     *
     */
    private void addToTrail(GrazableArea location)
    {
        if (params.herding.getMaxTrailLength() == 0)
        {
            return;
        }
        if (trail.size() == params.herding.getMaxTrailLength())
        {
            trail.removeLast();
        }
        trail.addFirst(location);
    }

    void setLocationAndUpdateTrail(GrazableArea newLocation)
    {
        addToTrail(getLocation());
        setLocation(newLocation);
        herdingGrid.setObjectLocation(this, newLocation.getX(), newLocation.getY());
    }
    
    

    /** Delete this herding activity
     *
     * This entails removing it from the household, MASON schedule, and MASON layers.
     *
     * XXX Not liking that it's possible to invoke with household that *doesn't*
     * contain this activity.  Might have activities know about their encompassing
     * households, and then use that information to remove themselves accordingly.
     *
     */
    @Override
    public void remove()
    {
        // Unschedule this task
//        stop();
    	
    	// Sell the herd
    	getHousehold().liquidateTLUs(getHerdSize());

        // Remove this from the household
        getHousehold().endHerding();

        // Remove this from the MASON layer
        herdingGrid.remove(this);

        // Remove the farmer from the list of farmers associated with the parcel
        getLocation().removeHerd(this);

        // XXX do we have all the references to this object?
        // TODO REMOVE TRAILS TOO!!!

    }
    
    /** Start the herding migrating -- ie choose a new WaterHole and enter the
     * 'migrating' state. */
    void selectNewWaterHole(MersenneTwisterFast random, int time)
    {
        if (waterHole == null)
        	setWaterHoleToNearest();
        waterHole = WaterHoleFinder.findNewWaterHole(waterHole, getLocation().getCoordinate(), time, daysTillReturn(time), params, random);
        assert (this.getCurrentWaterHole() != null) : "herder without current watering hole...";
    }

    /** Moving the herd burns food and uses water
     *
     * @param oldLocation
     */
    void metabolizeForMovement(GrazableArea oldLocation)
    {
        // assess cost of movement at 1/10th of daily metabolism per 1km moved
        double distMoved = CachedDistance.distance(getLocation().getCoordinate(), oldLocation.getCoordinate());

        assert(distMoved >= 0);
        if (distMoved == 0)
        {
            return;
        }

        double burnedFood = params.herding.getTLUFoodMetabolismRate() * params.herding.getFoodCostOfMovement() * distMoved;
        setHerdFood(getHerdFood() - burnedFood);

        double usedWater = params.herding.getTLUWaterMetabolismRate() * params.herding.getWaterCostOfMovement() * distMoved;
        setHerdWater(getHerdWater() - usedWater);
    }
    
    //TRG changed this so that it can go negative -- making hunger get very intense when needed
    /**  Burn the food it takes to maintain anmials before movement */
    private void herdMetabolize()
    {
//        double newherdfood = Math.max(getHerdFood() - (params.herding.getTLUFoodMetabolismRate() * getHerdSize()), 0);
//        assert (newherdfood >= 0) : newherdfood;
        double newherdfood = getHerdFood() - (params.herding.getTLUFoodMetabolismRate() * getHerdSize());
        setHerdFood(newherdfood);


        //TRG changed this so that it can go negative -- making thirst get very intense when needed
        // Use up the water it takes to maintain the animals before movement
//        double newherdwater = Math.max(getHerdWater() - (params.herding.getTLUWaterMetabolismRate() * getHerdSize()), 0);
//        assert (newherdwater >= 0) : newherdwater;
        double newherdwater = getHerdWater() - (params.herding.getTLUWaterMetabolismRate() * getHerdSize());
        setHerdWater(newherdwater);
    }

    /** Consume vegetation (XXX or from stores?) */
    private void herdEat()
    {
        // Metabolism moved back to its own method.

        // Get the shortfall of desired food for the entire herd
        double herdNetHunger = ((params.herding.getTLUFoodMax() * getHerdSize()) - getHerdFood());
        // Only eat as much as the animals can eat in a day.
        
        herdNetHunger = Math.min(herdNetHunger, getHerdSize() * params.herding.getTLUFoodConsumptionRate());

        // vegEaten will be the actual amount of vegetation consumed, which
        // may be less than 'totalHunger'; i.e., if there is less vegetation
        // available than requested through 'totalHunger', then that lesser
        // amount is what eatVegetation() returns.


        if (herdNetHunger > 0)
        {
            double vegEaten = getLocation().eatVegetation(herdNetHunger);
            setHerdFood(getHerdFood() + vegEaten);
            if (vegEaten > (herdNetHunger - 1) && getHerdFood() < 0){
                setHerdFood(0.0); // once the herd gets a good meal, it stops starving to death
//                System.out.println("reset herdFood");
            }
            assert (getHerdFood() <= (params.herding.getTLUFoodMax() * getHerdSize()) );
        }
    }

    /** Collect water from our current WaterHole if we are at it.  This
     * does not consume the water, only collect it -- it's consumed in the
     * metabolism functions. */
    private void herdDrink()
    {
        if(!isAtWateringHole())
            return;

        goingToNewWaterhole = false;
        // Get the amount of total thirst for the entire herd; essentially this
        // is what's left over after the herd drinks its store of water.
        double herdDailyNetWaterNeed = ((params.herding.getTLUWaterMax() * getHerdSize()) - getHerdWater());

        if (herdDailyNetWaterNeed > 0)
        {
            drinkFailed = false;
            double waterDrunk = waterHole.drinkWater(herdDailyNetWaterNeed);
            setHerdWater(getHerdWater() + waterDrunk);
            if (waterDrunk < herdDailyNetWaterNeed) {          
                 drinkFailed = true;
                }
            assert (getHerdWater() <= (params.herding.getTLUWaterMax() * getHerdSize()) );
        }
    } 
    
    /** Remove a given number of animals from the herd */
    private void killTLUs(int num)
    {
    	assert (num <= getHerdSize()) : String.format("killTLUs: num %d was greater than herd size %d.", num, getHerdSize());
    	
        // If we don't have any animals, then we obviously have none to kill
        if (getHerdSize() == 0)
        {
            return;
        }

        // Reduce food and water stored by the animal that died
        double waterPerAnimal = getHerdWater() / getHerdSize();
        setHerdWater(getHerdWater() - num*waterPerAnimal);

        double foodPerAnimal = getHerdFood() / getHerdSize();
        setHerdFood(getHerdFood() - num*foodPerAnimal);

        setHerdSize(getHerdSize() - num);
    }
    
    /**
     * Calculate the number of TLUs that should die from hunger and/or thirst
     * @param food
     * @param water
     * @param herdSize
     * @param random
     * @return
     */
    private int calcDeathsFromDeprivation(double food, double water, int herdSize, MersenneTwisterFast random) {
    	if ((food > 0) && (water > 0))
    		return 0;
    	
    	double starvation = Math.max(0, food / (-params.herding.getTLUFoodMax() * herdSize));
    	double dehydration = Math.max(0, water / (-params.herding.getTLUWaterMax() * herdSize));
            	
    	// this includes an exponent to control the shape of the dyoff curve
        double pDeath = Math.pow((1 - ((1-(starvation)) * (1-(dehydration)))),4);
    	//double pDeath = 1 - ((1-starvation) * (1-dehydration));
        
    	int deaths = (int)(pDeath * herdSize);
    	double remainder = (pDeath * herdSize) - deaths;
    	if (random.nextDouble() < remainder)
    		deaths++;
        
        //if (deaths > 0)
        //    System.out.format("Herd: %s, Size: %d, S: %.2f, D: %.2f, deaths: %d\n", this, herdSize, starvation, dehydration, deaths);
    	
    	return deaths;
        //return 0;
    }
    
    private int calcBirths(double idealHerdBirthProbability, double food, double water, int herdSize, MersenneTwisterFast random) {
    	double stress = Math.max(getScaledThirst(), getScaledHunger()); 
        
        double pBirth = idealHerdBirthProbability * (1 - stress);
        
    	int births = (int)(pBirth * herdSize);
    	double remainder = (pBirth * herdSize) - births;
    	if (random.nextDouble() < remainder)
            births++;
        
//        if (births > 0)
//            System.out.format("Size: %d, H: %.2f, T: %.2f, births: %d\n", herdSize, hunger, thirst, births);
        
        return births;
    }

    /**
     * Adjusts the herd size due to dehydration and starvation.  Also splits
     * the herd when it gets too big to manage.  Alternatively the herding activity
     * will be removed if all the animals die out.
     * <p>
     * TODO How does this affect the human population?
     *
     * @param herdingField is MASON field containing this activity
     * @param idealHerdBirthProbability is chance of spawning a new animal
     */
    private void adjustHerdSize(SparseField herdingField, MersenneTwisterFast random, double idealHerdBirthProbability)
    {
    	int deaths = calcDeathsFromDeprivation(getHerdFood(), getHerdWater(), getHerdSize(), random);
    	if (deaths > 0) {
            double prevFood = getHerdFood();
            double prevWater = getHerdWater();
            killTLUs(deaths);
    		
            if (getHerdSize() <= 0)
            {
                String cause = "unknown";
                if (prevFood <= 0.0)
                {
                	if (prevWater <= 0.0)
                		cause = "starvation and dehydration";
                	else
                		cause = "starvation";
                }
                else if (prevWater <= 0.0)
                	cause = "dehydration";

                World.getLogger().log(Level.FINE, String.format(
                        "Herding.adjustHerdSize: herd died from %s, food: %f, water: %f", 
                        cause, prevFood, prevWater));

                return;
            }
    	}

        int births = calcBirths(idealHerdBirthProbability, getHerdFood(), getHerdWater(), herdSize, random);
        setHerdSize(getHerdSize() + births);
        
        // If there are too many TLUs for the number of herders, liquidate some
        if (getHerdSize() > getPopulation() * params.herding.getMaxTLUsPerHerder())
        	getHousehold().liquidateTLUs(getHerdSize() - getPopulation() * params.herding.getMaxTLUsPerHerder());
        
        assert (getHerdSize() <= getPopulation() * params.herding.getMaxTLUsPerHerder()) : 
        	String.format("%d herders can't herd %d TLUs.", getPopulation(), getHerdSize());
    }

    //</editor-fold>
    
    @Override
    public void step(SimState ss)
    {
        super.step(ss);
        
        // If we have no herd or no herders, remove this activity.
        if ((getPopulation() == 0) || (getHerdSize() == 0))
        {
            remove();
            return;
        }
        
        assert(getPopulation() > 0);
        assert(getHerdSize() > 0);
        
        World world = (World) ss;

        if (this.getCurrentWaterHole() == null && this.getHousehold() != null)
        {
            this.waterHole = null;
            System.out.println("@" + world.schedule.getTime() + " herding>step>"
                    + " herder with no watering hole. Assigned to household at ("
                    + this.getLocation().getX() + ", "
                    + this.getLocation().getY() + ")");
        }

        assert (this.getCurrentWaterHole() != null) : "herder without current watering hole...";

        herdMetabolize(); // Use up the food an water that it takes just to live

        herdEat();

        herdDrink();
        
        getHousehold().depositFoodFromHerd(getNumHerdersSupportedByHerdFoodProduction());

        // add or subtract animals based on health and stress
        adjustHerdSize(world.getPopulation().getHerdingGrid(), world.getRandom(), world.getParams().herding.getHerdIdealBirthProbability());

        if (shouldFindNewWaterHole((int)world.schedule.getTime()))
                    selectNewWaterHole(world.getRandom(), (int)world.schedule.getTime());

        assert (this.getCurrentWaterHole() != null) : "herder without current watering hole...";
            
        if (nextLocation != null)
        {
            GrazableArea oldLocation = getLocation();
            setLocationAndUpdateTrail(nextLocation);
            oldLocation.removeHerd(this); //update location so that parcel knows what herders are on it
            this.getLocation().addHerd(this);
            metabolizeForMovement(oldLocation); // Moving the herd burns some food and uses water
            nextLocation = null;
        }
        else
            this.setLocationAndUpdateTrail((GrazableArea) this.getLocation());
           
// THIS IS THE CODE THAT CONNECTS TO THE CONFLICT STUBS
        // Now determine if we have conflict
        // Are there farmers here?
//        if ( ! this.getLocation().getFarmers().isEmpty())
//        {
//            // only conflict if not own farm
//            if (this.getHousehold().getLocation() != this.getLocation())
//            {
//                Conflict conflict = new Conflict(this,
//                                                 (GrazableArea) this.getLocation(),
//                                                 Conflict.CONFLICT_TYPE_HF );
////                getWorld().mediator.addConflict(conflict);     // This was commented out when surroundings were not.
//            }
//        }
//
//        // Are there other herders here?
//        if ( this.getLocation().getHerders().size() > 1)  // more than current herder
//        {
//             Conflict conflict = new Conflict(this,
//                                              (GrazableArea) this.getLocation(),
//                                              Conflict.CONFLICT_TYPE_HH_TBD );  // don't know culture yet
//             getWorld().mediator.addConflict(conflict);
//        }
    }
}
