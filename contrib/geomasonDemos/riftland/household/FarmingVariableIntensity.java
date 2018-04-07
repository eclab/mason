package riftland.household;

import ec.util.MersenneTwisterFast;
import riftland.Parameters;
import riftland.World;
import riftland.parcel.GrazableArea;
import sim.field.grid.SparseGrid2D;

/**
 * @author Tim
 */
public class FarmingVariableIntensity extends Farming
{
    /** Multiplier applied to the vegetation on this farm at harvest time. This
     * is our way of implementing the idea that a farm is more productive than
     * naturally growing vegetation: if farmYieldRate = 2.0, then the amount
     * of crop actually harvested is twice the amount of vegetation that has
     * been grown on the farm.  The yield rate is updated at planting time via
     * a call to computeYieldRate().
     * 
     * @see FarmingVariableIntensity#computeYieldRate()
     */
    private double farmYieldRate = 0.0; // Reset at planting time.

    /**
     * This is the farm's yield rate when the number of workers is equal
     * to the number of hectares.
     */
    private static final double BASE_FARM_YIELD_FACTOR = 1.0;

    
    
    //<editor-fold defaultstate="collapsed" desc="Accessors">
    private void setFarmYieldRate(double yieldRate)
    {
        this.farmYieldRate = yieldRate;

    }

    public double getFarmYieldRate()
    {
        return farmYieldRate;

    }
    //</editor-fold>
    
     /**
     * Create the FarmingVariableIntensity activity and notify the household of it. Then populate
     * the activity, set the grain store, and the area within the parcel dedicated
     * to farming.  Finally, call adjustFarmSize() once.
     *
     * @param household is the household we need to tell about this farming activity
     * @param farmland is the parcel on which this farming is going to occur
     * @param population the initial population assigned to this activity
     * 
     * @return newly created VariableIntenistyFarming activity
     */
    public FarmingVariableIntensity(Parameters params, SparseGrid2D farmingGrid, Household household, GrazableArea farmland, double farmSize, int population, MersenneTwisterFast initRandom)
    {
        super(params, farmingGrid, household, farmland, initRandom);
        assert(farmSize >= 0); // Ought only to be zero temporarily!
        assert(population > 0);
        assert(initRandom != null);

        // Plug this activity into the corresponding MASON layer
        synchronized(farmingGrid)
        {
            farmingGrid.setObjectLocation(this, farmland.getX(), farmland.getY());
        }
        farmland.setIsaFarmland();
        farmland.addFarm(this);

        // farming activity population; see: Household.create() for this value
        setPopulation(population);
        //move the location of the household to the new parcel
        household.setLocation(farmland);

        getHousehold().setFarmAreaInHectares(farmSize); 
//        System.out.println("farmSize:" + farmSize + " new farm getFarmAreaInHectares():" + getHousehold().getFarmAreaInHectares() + 
//                "farmed availalbe on parcel:" + farmland.getAvailableAreaInHectares());
        

    }

    /**
     * Calculate the decreasing returns factor for farm efficiency.
     * 
     * @return Yield rate (efficiency) of this farm.
     * @see FarmingVariableIntensity#farmYieldRate
     */
    private double computeYieldRate()
    {

        // Farm produces twice wild vegetation with 1 farmer per hectare.
        // Higher intensity produces reduced yield per farmer following Boserup.
        double farmAreaInHectares = this.getHousehold().getFarmAreaInHectares();
        int farmWorkers = this.getPopulation();
        if (farmWorkers == 0)
        	return 0;
        
        assert (farmAreaInHectares > 0);

        double efficiency = BASE_FARM_YIELD_FACTOR * Math.pow(farmWorkers/(farmAreaInHectares * params.farming.getMaxWorkableLandPerFarmer()), 
        		params.farming.getFarmIntensificationExponent());
        return efficiency;
    }
    
    @Override
    void plantCrops(int today)
    {
        // presume all hectars of farm size are planted
        // plant hectars up to the lower of land owned & labor available

        // set farmland yield to min (i.e., prepare land... plow)
        setCropVegetationDensity(params.vegetation.getMinVegetationKgPerKm2());
                
        //adjust land holdings to be closer to what is needed (if possible)
        adjustFarmSize();
        
        // set farmed land to what they have labor for
        setFarmYieldRate(computeYieldRate()); //Update Farm Yield Rate based on the current efficiency

        // set plant date
        setHasPlanted(true);
        setPlantedDate(today);
        // if (hasRestarted) System.out.println("restarted farm planted" );
    }
    
    /**
     * @param today
     */
    @Override
    protected void harvestCrops(int today, MersenneTwisterFast random, World world)
    {
        // This is a conditional activity

        // harvest planted hectars up to the lower of planted & labor available
        // should harvesting take time?
        // This needs to be updated to move the growing inside the farming activity so that different farmers can harvest at different times.  TRG

        if (hasPlanted()) // make sure there's something planted
        {
            double vegDensity = getCropVegetationDensity();
            double harvestedVegDensity = (vegDensity - params.vegetation.getMinVegetationKgPerKm2());
            setCropVegetationDensity(params.vegetation.getMinVegetationKgPerKm2());
            
            // Increase the Grainstore with newly haarvested vegetation amounts.
            // Before adding harvested amounts of vegetation to Grainstore we multiply it with the yield rate of the FarmingParams unit that depends on the number of people the unit has.
            
            double harvestedCrops = getHousehold().getFarmAreaInKm2() * harvestedVegDensity * getFarmYieldRate();
            double harvestPFD = harvestedCrops / params.farming.getKgOfGrainPerPersonDay();
            getHousehold().depositGrain(harvestPFD);
            
            double altHarvestPFD = ((getVegetationBeforeHarvesting() - params.vegetation.getMinVegetationKgPerKm2()) 
                    * getHousehold().getFarmAreaInKm2() * getFarmYieldRate()) / params.farming.getKgOfGrainPerPersonDay();
            
    //public void updatePlantDate(double harvestDensity, double rainBeforeStart, double rainAtEnd, GrowingSeason other)
            growingSeasons[whichSeason].updateSeasonLength(harvestPFD, altHarvestPFD, getPopulation());
            growingSeasons[whichSeason].updatePlantDate(harvestedVegDensity, getRainBeforeGrowingSeason(), 
                    getRainAtEndofGrowingSeason(), getOtherGrowingSeason());
          
            setHasPlanted(false); // Resetting planted flag.

//            System.out.println("Switching Seasons: " + getWhichSeason());
            setWhichSeason(((getWhichSeason() + 1) % 2)); // switch to the other season
//            System.out.println("Switched Seasons: " + getWhichSeason());
            //if (hasRestarted) System.out.println("restarted farm harvested " + harvestPFD );
        }
    }
    
    /**
     * @param world supplies the MASON layer and random number generator
     * @param pop 
     * restartFarming() creates FarmingParams activity when the restarting criteria is met.
     * 
     */
    @Override
    public void restartFarming(int time, MersenneTwisterFast random, int pop, World world) 
    {

        hasRestarted = true;
        growingSeasons[getWhichSeason()].restartPlantDate(time);
        manageCrops(time, random, world);
        //System.out.println("Hit restartFarming -- need to figure out what should happen");


        
    }
}
