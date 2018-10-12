/*
 * World.java
 *
 * $Id: World.java 2028 2013-09-04 18:44:30Z escott8 $
 *
 */
package sim.app.geo.riftland;

import ec.util.MersenneTwisterFast;
import sim.app.geo.riftland.conflict.ConflictMediatorRural;
import sim.app.geo.riftland.conflict.Mediator;
import sim.app.geo.riftland.dataCollectors.DataCollector;
import sim.app.geo.riftland.dataCollectors.DisplacementEventCollector;
import sim.app.geo.riftland.dataCollectors.DisplacementEventDetailsCollector;
import sim.app.geo.riftland.dataCollectors.EthnicityCollector;
import sim.app.geo.riftland.household.HerdMover;
import sim.app.geo.riftland.util.CachedDistance;
import sim.app.geo.riftland.util.Misc;
import sim.engine.MakesSimState;
import sim.engine.SimState;
import sim.field.grid.DoubleGrid2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Implements state for the RiftLand simulation. */
public class World extends SimState
{
    private Parameters params;
    private static final Logger logger = Logger.getAnonymousLogger();
    private Land land;
    private Population population;
    private HerdMover herdMover;
    private WaterHoles waterHoles;
    private ThreadedGardener threadedGardener;
    private Mediator mediator;
    private Weather weather;
    private int upperLeftX = 0, upperLeftY = 0, lowerRightX = 1694, lowerRightY = 1630;

    DataCollector dataCollector;
    EthnicityCollector ethnicityCollector;
    DisplacementEventCollector displacementEventCollector;
    DisplacementEventDetailsCollector displacementEventDetailsCollector;

    /** Shows changes in vegetation from day to day */
    public volatile DoubleGrid2D vegetationChanges;
    /* Shows current daily rainfall */
    public DoubleGrid2D dailyRainfallGrid;
    
    //<editor-fold defaultstate="collapsed" desc="State Variables">
    
    /** The directory where data files are located */
    private String datapath;
    
    /** Tracked counter for number of watering holes cached. */
    public volatile int numWateringHolesCaching;
    /** Number of GrazableAreas that have built caches of nearby GrazableAreas */
    public volatile int numGrazableAreaCaching;

    // <editor-fold defaultstate="collapsed" desc="Accessors">public WaterHoles getWaterHoles()
    public WaterHoles getWaterHoles()
    {
        return waterHoles;
    }

    public Weather getWeather()
    {
        return weather;
    }
    
    public MersenneTwisterFast getRandom()
    {
        return random;
    }

    public Land getLand()
    {
        return land;
    }

    public Population getPopulation()
    {
        return population;
    }

    public DoubleGrid2D getVegetationChanges()
    {
        return vegetationChanges;
    }

    public DoubleGrid2D getDailyRainfallGrid()
    {
        return dailyRainfallGrid;
    }
    
    public String getDatapath()
    {
        return datapath;
    }

    public Parameters getParams()
    {
        return params;
    }

    public Mediator getMediator()
    {
        return mediator;
    }

    public static void setLoggerLevel(Level level)
    {
        logger.setLevel(level);
    }

    public static Level getLoggerLevel()
    {
        return logger.getLevel();
    }

    public static Logger getLogger()
    {
        return logger;
    }
    //</editor-fold>

    public World(long seed, String[] args)
    {
        this(seed, args, true);
    }

    public World(long seed, String[] args, boolean scheduleMediator)
    {
        super(seed);
        Logger.getLogger(World.class.getName()).info("World constructor begins.");
        params = new Parameters(args);
        
        setLoggerLevel(params.system.getLoggerLevel());

        datapath = Misc.argumentForKey("-filepath", args);
        datapath = Misc.normalizeDataPath(datapath);

        params.world.setScheduleMediator(scheduleMediator);

        threadedGardener = new ThreadedGardener(params);

//      initialization code for finding an almost exact number of waterholes for a given subarea
        land = new Land(params);

        if (params.world.getWaterHoleCounterFlag() && land.getSubAreaLowerRight() != null)
        {
            upperLeftX = (int)land.getSubAreaUpperLeft().getX();
            upperLeftY = (int)land.getSubAreaUpperLeft().getY();
            lowerRightX = (int)land.getSubAreaLowerRight().getX();
            lowerRightY = (int)land.getSubAreaLowerRight().getY();
            params.world.setSubArea(null);
            params.world.setNumInitialWateringHoles(20000);
            land = new Land(params);
        }
        land.loadLandData(datapath, threadedGardener);

        waterHoles = new WaterHoles(params, land);
        threadedGardener.setWaterHoles(waterHoles);
        
        population = new Population(params, datapath, land);
        herdMover = new HerdMover(params, population.getHerdingGrid());
        mediator = new ConflictMediatorRural(land.getWidth(), land.getHeight());
        createGrids(land.getWidth(), land.getHeight());

        weather = new Weather(params, land.getWidth(), land.getHeight(), datapath, random, land);
        
        int h = land.getSubAreaLowerRight().y - land.getSubAreaUpperLeft().y;
        int w = land.getSubAreaLowerRight().x - land.getSubAreaUpperLeft().x;
        assert(h > 0);
        assert(w > 0);
        CachedDistance.buildLUT((int)Math.sqrt(h*h + w*w) + 1);

        this.dataCollector = new DataCollector(this);
        this.ethnicityCollector = new EthnicityCollector(this);
        this.displacementEventCollector = new DisplacementEventCollector(this);
        this.displacementEventDetailsCollector= new DisplacementEventDetailsCollector(this);

        Logger.getLogger(World.class.getName()).info("End World.constructor");
    }
    
    // <editor-fold defaultstate="collapsed" desc="Constructor Helpers">
    

    /** create the underlying grids to store Parcels, Herders, Farmers, and Conflicts
     *
     * @param width grid width
     * @param height grid height
     *
     */
    private void createGrids(int width, int height)
    {
        vegetationChanges = new DoubleGrid2D(width, height);
        dailyRainfallGrid = new DoubleGrid2D(width, height);
    }

    //</editor-fold>


    @Override
    public void start()
    {
        final int WEATHER_ORDERING = 0;
        final int GARDENER_ORDERING = 1;
        final int HERDMOVER_ORDERING = 3;
        final int HOUSEHOLD_ORDERING = 4;
        final int MEDIATOR_ORDERING = 6;
        final int OUTPUT_TOTAL = 100;
        final int OUTPUT_ETHNICITY = 101;
        final int OUTPUT_DISPLACED = 102;
        final int OUTPUT_DISPLACEDDETAILS = 103;

        // FIXME Clear out any values from previous runs.
        // Actually finish() should have cleared out households and other
        // artifacts left over from previous runs.

        super.start();


        // reset all GrazableAreas to initial vegetation and water levels.
        // XXX Does this reset properly? -- Siggy
        threadedGardener.reset(this);


        // TODO reset weather

        waterHoles.placeWaterHoles(land, random, datapath);
        
        if (params.world.getWaterHoleCounterFlag()) {
            System.out.println("\n(" + upperLeftX +", " + upperLeftY + ") (" + lowerRightX + ", " + lowerRightY + ")");
            System.out.format("WaterHoleCounts: %d\n", waterHoles.countWaterHoles(upperLeftX, upperLeftY, lowerRightX, lowerRightY));
        	System.exit(0);
        }
        
        population.populate(land, waterHoles, random);
        System.out.println("Placed " + population.getCurrentNumHouseholds() + " households with "
                + population.getCurrentPopulationFarmers() +" farmers, "
                + population.getCurrentPopulationHerders() + " herders, "
                + population.getCurrentPopulationLaborers() + " laborers for a total of "
                + ( population.getCurrentPopulationFarmers()
                  + population.getCurrentPopulationHerders()
                  + population.getCurrentPopulationLaborers() ) + " people."
                );

        schedule.scheduleRepeating(weather, WEATHER_ORDERING, 1);
        schedule.scheduleRepeating(threadedGardener, GARDENER_ORDERING, 1);
        schedule.scheduleRepeating(herdMover, HERDMOVER_ORDERING, 1);
        schedule.scheduleRepeating(population.getHouseHoldRandomSequence(), HOUSEHOLD_ORDERING, 1);

        if (params.system.getOutputFilename()  != null)
        {
            schedule.scheduleRepeating(dataCollector, OUTPUT_TOTAL, 1);
            dataCollector.start();
        }

        if (params.system.getEthnicityOutputFilename()  != null)
        {
            schedule.scheduleRepeating(ethnicityCollector, OUTPUT_ETHNICITY, 1);
            ethnicityCollector.start();
        }

        // We need to wake up the observer if we want a running data log of the simulation.
//        if (params.system.isWriteStats())
        if (params.system.getDisplacedOutputFilename() != null)
        {
            // Schedule the observer to run after everything else. (Is that right?)
            schedule.scheduleRepeating(displacementEventCollector, OUTPUT_DISPLACED, 1);
            displacementEventCollector.start();
        }

        if (params.system.getDisplacedDetailsOutputFilename() != null)
        {
            // Schedule the observer to run after everything else. (Is that right?)
            schedule.scheduleRepeating(displacementEventDetailsCollector, OUTPUT_DISPLACEDDETAILS, 1);
            displacementEventDetailsCollector.start();
        }

        // Schedule Mediator to run last.
        // WorldGUI sets scheduleMediator=false
        // (and does it after the graphics update to see conflicts before they're
        // cleaned in the mediator.step())
        if (params.world.isScheduleMediator())
        {
            schedule.scheduleRepeating(getMediator(), MEDIATOR_ORDERING, 1);
            getLogger().log(Level.INFO, "World>start>scheduling mediator {0}", getMediator());
        }
        System.out.println("End of World.start()");
    }

    @Override
    public void finish()
    {
        super.finish();

        //Buffer should be written to the file.
        if (params.system.getOutputFilename() != null)
        {
            dataCollector.clearAll();
        }

        //Ethnicity Data Collection
        if (params.system.getEthnicityOutputFilename()  != null)
        {
            ethnicityCollector.clearAll();
        }

        //Displacement Event Data Collection
        if (params.system.getDisplacedOutputFilename()  != null)
        {
            displacementEventCollector.clearAll();
        }

        //Details of Displacement Event Data Collection
        if (params.system.getDisplacedDetailsOutputFilename()  != null)
        {
            displacementEventDetailsCollector.clearAll();
        }

        // Cleanup all the threads
        this.threadedGardener.finish();
    }

    public static void main(String[] args)
    {
        doLoop(new MakesSimState()
        {
            @Override
            public SimState newInstance(long seed, String[] args)
            {
                return new World(seed, args);
            }

            @Override
            public Class simulationClass()
            {
                return World.class;
            }
        }, args);
            System.exit(0);
    }
}
