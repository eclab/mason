/*
 * FarmingParams.java
 *
 * $Id: Farming.java 1943 2013-07-16 01:53:34Z hkarbasi $
 */
package sim.app.geo.riftland.household;


import ec.util.MersenneTwisterFast;
import sim.app.geo.riftland.Gardener;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.World;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.app.geo.riftland.parcel.Parcel;
import sim.engine.SimState;
import sim.field.grid.SparseGrid2D;

/**
 *
 */
public abstract class Farming extends ActivityAtLocation
{

    // <editor-fold defaultstate="collapsed" desc="State Variables">
    
    /** Flag to indicate there is no second planting season.
     *
     * @see #secondPlantingDate
     */
    public static final int NO_SECOND_PLANTING = -1;
    
    final protected SparseGrid2D farmingGrid;
    
    /** When to first plant */
    private int firstPlantingDate = -1;
    /** Similar to firstPlantingDate*/
    private int secondPlantingDate = NO_SECOND_PLANTING;
    /** How long the plants stay in the ground.
     *
     * Harvest will be {first,second}PlantingDate + {first,second}GrowingSeason
     */
    private int lengthOfFirstGrowingSeason;
    private int lengthOfSecondGrowingSeason;
    /** flag (0or1) for which planting season & date is the current focus 
     * this has changed from being 1 or 2.*/
    int whichSeason = 0;
    /** When the current crop was planted */
    private int plantedDate = 0;

    /** Last time step this activity harvested
     *
     * this private variable is used by and set by calculateFarmFraction
     */
    private int lastHarvestDate = 0;
    
    /** Do we have any plants in the ground?*/
    public boolean hasPlanted = false;
    
    /*
     * counters to keep track of rain at both ends of growing season to make adjustments
     */
    private double rainBeforeGrowingSeason = 0;
    private double rainAtEndOfGrowingSeason = 0;
    
        
    private GrowingSeason growingSeason0;
    private GrowingSeason growingSeason1;
    
    GrowingSeason[] growingSeasons;
    
    public boolean hasRestarted = false;
    
    /** The amount of vegetation before we harvest
     *
     * Used to adjust the next growing season.
     */
    private double vegetationBeforeHarvesting = 0.0;

    /** Consecutive harvests below needs
     *
     * Used by household to decide resources for farming.
     */
    private int unsatYields = 0;
    
    private boolean hasHarvested = false;
    
    /** Amount of crop per km^2 on this farm. */
    private double cropVegetationDensity;
    
    final protected Parameters params;
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Accessors">

    public double getCropVegetationDensity()
    {
        return cropVegetationDensity;
    }

    public void setCropVegetationDensity(double cropVegetationDensity)
    {
        assert(cropVegetationDensity >= params.vegetation.getMinVegetationKgPerKm2());
        assert(cropVegetationDensity <= params.vegetation.getMaxVegetationKgPerKm2());
        this.cropVegetationDensity = cropVegetationDensity;
    }
    
    public void setHasHarvested(boolean val) {
        hasHarvested = val;
    }

    public boolean getHasHarvested() {
        return hasHarvested;
    }
    
    public void setHasPlanted(boolean val) {
        hasPlanted = val;
    }

    public boolean getHasPlanted() {
        return hasPlanted;
    }
    
    /**
     * planting focus keeps track of which of two seasons
     * the planting date and season are associated with
     *
     * @return
     */
    public int getWhichSeason()
    {
        return whichSeason;
    }

    public void setWhichSeason(int flag)
    {
        whichSeason = flag;
    }
    
    public GrowingSeason getGrowingSeason(){
        return growingSeasons[whichSeason];
    }
    
    public GrowingSeason getOtherGrowingSeason(){
        return growingSeasons[(whichSeason + 1) % 2];
    }


    public int getLengthOfGrowingSeason()
    {
        return growingSeasons[whichSeason].getSeasonLength();
    }

    public double getRainAtEndofGrowingSeason (){
        return rainAtEndOfGrowingSeason;
    }
    
    public double getRainBeforeGrowingSeason() {
        return rainBeforeGrowingSeason;
    }

    public int getNextHarvestDate() {
        return growingSeasons[whichSeason].getHarvestDate();
    }

    protected int getPlantedDate() {
        return plantedDate;
    }

    protected void setPlantedDate(int plantedDate) {
        this.plantedDate = plantedDate;
    }

    protected boolean hasPlanted() {
        return hasPlanted;
    }

    public double getVegetationBeforeHarvesting()
    {
        return vegetationBeforeHarvesting;
    }

    private void setVegetationBeforeHarvesting(double veg)
    {
        this.vegetationBeforeHarvesting = veg;
    }

    public int getUnsatYields() {
        return unsatYields;
    }

    public void setUnsatYields(int newcount) {
        this.unsatYields = newcount;
    }

    // </editor-fold>

    /**
     * @param household to which this farm belongs
     * @param parcel on which the farm is located
     */
    Farming(Parameters params, SparseGrid2D farmingGrid, Household household, Parcel parcel, MersenneTwisterFast random)
    {
        super(household, parcel);
        growingSeason0 = new GrowingSeason(this, getHousehold().getEndOfPseudoAnnualWorkCycle(), params);
        growingSeason1 = new GrowingSeason(this, (getHousehold().getEndOfPseudoAnnualWorkCycle() + 180), params);
        growingSeasons = new GrowingSeason[2];
        growingSeasons[0] = growingSeason0;
        growingSeasons[1] = growingSeason1;
        assert(params != null);
        assert(farmingGrid != null);
        this.params = params;
        this.farmingGrid = farmingGrid;
//        household.depositGrain(360 * 10000);
        
//        if (growingSeason0.getPlantDate() < 0)
//            System.err.print("--- Error: negative plant date:" + growingSeason0.getPlantDate());
//        
//        if (growingSeason1.getPlantDate() < 0)
//            System.err.print("--- Error: negative plant date:" + growingSeason1.getPlantDate());

    }
    
    /**
     * Copy all the state parameters from another instance of Farming. This is
     * used during household splitting to make sure the new household is in a 
     * coherent state.
     * @param other the Farming to copy from
     */
    public void copyStateFrom(Farming other) {

//    	firstPlantingDate = other.firstPlantingDate;
//    	nextPlantDate = other.nextPlantDate;
//    	nextHarvestDate = other.nextHarvestDate;
    	whichSeason = other.whichSeason;
    	lastHarvestDate = other.lastHarvestDate;
//    	lengthOfFirstGrowingSeason = other.lengthOfFirstGrowingSeason;
//    	lengthOfSecondGrowingSeason = other.lengthOfSecondGrowingSeason;
//    	secondPlantingDate = other.secondPlantingDate;
    	plantedDate = other.plantedDate;
    	hasPlanted = other.hasPlanted;
    	hasHarvested = other.hasHarvested;
        growingSeasons[0] = new GrowingSeason(other.growingSeasons[0]);
        growingSeasons[1] = new GrowingSeason(other.growingSeasons[1]);
    }
    
    // <editor-fold defaultstate="collapsed" desc="Abstract Methods">
    /**
     *  harvest crops and update stores
     *  @noreturn
     */
    abstract void harvestCrops(int today, MersenneTwisterFast random, World world);
    
    /**
     * restartFarming
     * re-initializes a farm to try farming again
     */
    abstract void restartFarming(int time, MersenneTwisterFast random, int pop, World world);
    
    /**
     * @param today is current time step
     */
    abstract void plantCrops(int today);
    // </editor-fold>

    @Override
    public void step(SimState ss)
    {
        super.step(ss);
        
        World world = (World) ss;

        if (getHasPlanted()){
             growCrops(world);
        }

        manageCrops((int) world.schedule.getTime(), world.getRandom(), world);
    }


    void manageCrops(int today, MersenneTwisterFast random, World world)
    {

        //System.out.println(whichSeason);//(growingSeasons[whichSeason]);
        int nextPlantDate = growingSeasons[whichSeason].getPlantDate();
        int nextHarvestDate = nextPlantDate + growingSeasons[whichSeason].getSeasonLength();
        
        final int patchX = getLocation().getRainfallPatch().x;
        final int patchY = getLocation().getRainfallPatch().y;
        double rainfall = world.getWeather().getDailyRainfall(patchX,patchY);
        
        if (today > nextPlantDate - 14 && today < nextPlantDate){
            rainBeforeGrowingSeason = rainBeforeGrowingSeason + rainfall;
        }
        if (today > nextHarvestDate - 14 && today < nextHarvestDate){
            rainAtEndOfGrowingSeason = rainAtEndOfGrowingSeason + rainfall;
        }

        // if haven't planted and today is planting day, plant!
        if (!hasPlanted() && today == nextPlantDate && getLengthOfGrowingSeason() > 0)
        {
            plantCrops(today);
        }

        if (hasPlanted())
        {
            // Note crop level prior to harvesting
            if (today == (nextHarvestDate - 7))
            {
                setVegetationBeforeHarvesting(getCropVegetationDensity());
            } else if (today == nextHarvestDate) // if harvesting season, harvestCrops & calc farming fraction
            {
                harvestCrops(today, random, world);

                rainBeforeGrowingSeason = 0;
                rainAtEndOfGrowingSeason = 0;
            }
        }
    }
    
    /**
     * Increase the vegetation level on this farm as a function of rainfall.
     * @param world Used to access rainfall information for this farm's location.
     */
    private void growCrops(World world)
    {
        final int patchX = getLocation().getRainfallPatch().x;
        final int patchY = getLocation().getRainfallPatch().y;
        final double rainfall = world.getWeather().getDailyRainfall(patchX,patchY);
        setCropVegetationDensity(Gardener.getUpdatedVegetation(rainfall, (GrazableArea)this.getLocation(), cropVegetationDensity, world.getParams()));
    }
    
    /** Delete this farming activity and all references to it. */
    @Override
    public void remove()
    {
        getHousehold().setFarmAreaInHectares(0.0); // give land back to GrazableArea
        getHousehold().endFarming();
        farmingGrid.remove(this);
        ((GrazableArea) getLocation()).removeFarm(this);
    }

    void adjustFarmSize()
    {
        double farmArea_ = this.getHousehold().getFarmAreaInHectares();
        int farmWorkers_ = this.getPopulation();
        GrazableArea myGrazableArea = (GrazableArea) (getHousehold().getLocation());
        double desiredLandChange = farmWorkers_ - farmArea_; // what it would take to get to 1 ha per worker.
//        double realLandChange = myGrazableArea.adjustLandClaim(desiredLandChange);
        this.getHousehold().setFarmAreaInHectares(farmArea_ + desiredLandChange);

        //myGrazableArea.debugFarmedLandArea();
    }
    
    //</editor-fold>
}
