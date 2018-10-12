package sim.app.geo.riftland;

import sim.app.geo.cityMigration.DisplacementEvent;
import com.vividsolutions.jts.geom.Point;
import ec.util.MersenneTwisterFast;
import sim.app.geo.riftland.household.Farming;
import sim.app.geo.riftland.household.Herding;
import sim.app.geo.riftland.household.Household;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.app.geo.riftland.parcel.Parcel;
import sim.app.geo.riftland.util.Misc;
import sim.engine.RandomSequence;
import sim.engine.Steppable;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.field.grid.IntGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.io.geo.ArcInfoASCGridImporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Eric 'Siggy' Scott
 */
final public class Population {

    // <editor-fold defaultstate="collapsed" desc="Fields">
    /** LandScan population data */
    private GeomGridField populationGrid = new GeomGridField();
    /** Contains all the household "points of origin" */
    private SparseGrid2D householdsGrid;
    /** Contains herding activities */
    private SparseGrid2D herdingGrid;
    /** Contains farming activities */
    private SparseGrid2D farmingGrid;
    /** Polygons describing the Murdoch ethnic regions */
    private GeomVectorField ethnicRegions = new GeomVectorField();
    // A RandomSchedule of Households
    final public RandomSequence houseHoldRandomSequence;

    /** Dictionary of ethnic IDs to culture names */
    private Map<Integer, String> ethnicIDToName = new HashMap<Integer, String>();

    final Parameters params;
    private int lastTotalPop = 0;
    private double currentPopulationChangeRate = 0.0;
    private HashMap<Integer, Integer> displacedPeopleByEthnicity = new HashMap<Integer, Integer>();
    
    /**
     * This list contains all households that are displaced in the current time
     * step.
     */
    private LinkedList<DisplacementEvent> displacementEvents = new LinkedList<DisplacementEvent>();
    /** This is the count of displaced people, not the households or events */
    private int displacedPopulation = 0;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Accessors">
    public HashMap<Integer, Integer> getDisplacedPeopleByEthnicity()
    {
        return displacedPeopleByEthnicity;
    }

    public void setDisplacedPeopleByEthnicity(HashMap<Integer, Integer> displacedPeopleByEthnicity)
    {
        this.displacedPeopleByEthnicity = displacedPeopleByEthnicity;
    }

    public LinkedList<DisplacementEvent> getDisplacementEvents()
    {
        return displacementEvents;
    }
    
    public GeomGridField getPopulationGrid()
    {
        return populationGrid;
    }

    public GeomVectorField getEthnicRegionsGrid()
    {
        return ethnicRegions;
    }
    
    public void addDisplacementEvent(DisplacementEvent event)
    {
    	World.getLogger().log(Level.FINE, String.format("Household displaced: %s", event.toString()));
        displacementEvents.addLast(event);
        
        if (!displacedPeopleByEthnicity.containsKey(event.culture))
        	displacedPeopleByEthnicity.put(event.culture, event.groupSize);
        else
        	displacedPeopleByEthnicity.put(event.culture, event.groupSize + displacedPeopleByEthnicity.get(event.culture));
    }
    /** @return Get the current population of displaced people */
    public int getCurrentPopulationDisplaced()
    {
        return displacedPopulation;
    }

    public void setCurrentPopulationDisplaced(int count)
    {
        displacedPopulation = count;
    }
    
    public void setLastTotalPop(int pop)
    {
        lastTotalPop = pop;
    }

    public int getLastTotalPop()
    {
        return lastTotalPop;
    }

    public double getCurrentPopulationChangeRate()
    {
        return currentPopulationChangeRate;
    }

    public void setCurrentPopulationChangeRate(double rate)
    {
        currentPopulationChangeRate = rate;
    }
    
    public SparseGrid2D getHouseholdsGrid()
    {
        return householdsGrid;
    }

    public SparseGrid2D getHerdingGrid()
    {
        return herdingGrid;
    }

    public SparseGrid2D getFarmingGrid()
    {
        return farmingGrid;
    }

    public RandomSequence getHouseHoldRandomSequence()
    {
        return houseHoldRandomSequence;
    }
    
    /**
     * Return the name of the ethnic group given the culture ID
     *
     * @param id is Murdock culture ID
     * @return name of that culture; will be empty string if ID not found
     */
    public String getEthnicName(Integer id)
    {
        return ethnicIDToName.get(id);
    }
    
    /**
     * @return The households; note bag can be empty if no households were
     * specified.
     */
    public Bag getHouseholds()
    {
        if (householdsGrid == null)
            return null;
        return householdsGrid.getAllObjects();
    }

    private Bag getFarmers()
    {
        if (farmingGrid == null)
            return null;

        Bag farmerbag = farmingGrid.getAllObjects();

        if (farmerbag.numObjs == 0)
            return null;

        return farmerbag;
    }

    /** @return Get the current number of farming activities in 'farming' layer */
    public int getCurrentNumFarmers()
    {
        Bag farmerbag = getFarmers();
        if (farmerbag == null) // might be empty
            return 0;

        return farmerbag.size();
    }
    
    /**
     * @return Bag of herding, or null if no herding
     *
     * If you want to keep the bag around for any length of time, you should
     * make a copy of it.
     */
    private Bag getHerders()
    {
        if (herdingGrid == null)
            return null;

        Bag herderbag = herdingGrid.getAllObjects();

        if (herderbag.isEmpty())
            return null;

        return herderbag;
    }

    /** @return the current number of herding activities in the simulation */
    public int getCurrentNumHerders()
    {
        Bag herderbag = getHerders();
        if (herderbag == null) // might be empty
            return 0;

        return herderbag.size();
    }

    /** @return the current population of herding activities in the simulation */
    public int getCurrentNumberTLUs()
    {
        Bag householdsbag = getHouseholds();
        if (householdsbag == null || householdsbag.isEmpty()) // might be empty
        {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < householdsbag.size(); i++)
        {
            Household thisagent = (Household) householdsbag.objs[i];
            if (thisagent.getHerding() != null)
            {
                total = total + thisagent.getHerding().getHerdSize();
            }
        }
        return total;
    }

    
    /** @return the current population of herding activities in the simulation */
    public double getCurrentSizeOfFarmsInHectares()
    {
        Bag householdsbag = getHouseholds();
        if (householdsbag == null || householdsbag.isEmpty()) // might be empty
        {
            return 0;
        }

        double total = 0;
        for (int i = 0; i < householdsbag.size(); i++)
        {
            Household thisagent = (Household) householdsbag.objs[i];
            if (thisagent.getFarming() != null)
            {
                total = total + thisagent.getFarmAreaInHectares();
            }
        }
        return total;
    }

    /** @return The total wealth of all Households. */
    public double getTotalWealth()
    {
        Bag householdsbag = getHouseholds();
        if (householdsbag == null || householdsbag.isEmpty()) // might be empty
        {
            return 0;
        }

        double total = 0;
        for (int i = 0; i < householdsbag.size(); i++)
        {
            Household h = (Household) householdsbag.objs[i];
            total = total + h.getWealth();
        }
        return total;
    }

    /** @return the current number of herding activities in the simulation */
    public int getCurrentNumHerds()
    {
        Bag herderbag = getHerders();
        if (herderbag == null) // might be empty
            return 0;

        return herderbag.size();
    }    

    /** @return the current population of farming activities in the simulation */
    public int getCurrentPopulationFarmers()
    {
        Bag householdsbag = getHouseholds();
        if (householdsbag == null || householdsbag.isEmpty()) // might be empty
        {
            return 0;
        }

        //return householdbag.size();   // this won't work because we don't remove households
        int total = 0;
        for (int i = 0; i < householdsbag.size(); i++)
        {
            Household thisagent = (Household) householdsbag.objs[i];
            if (thisagent.getFarming() != null)
            {
                total += thisagent.getFarming().getPopulation();
            }
        }
        return total;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Computations">
    private int getNewHouseholdSize(MersenneTwisterFast random)
    {
        // Get a random household size with specified mean (m), sampled from [2/3m, 4/3m].
        double twoThirdsMean = (2.0/3.0)*params.households.getMeanHouseholdSize();
        int householdPop;
        householdPop = (int)Math.round(twoThirdsMean + random.nextDouble()*twoThirdsMean);
        assert(householdPop > 0) : "Assertion failed: Household.populateParcel(): householdPop <= 0";
        return householdPop;
    }
    
    /** @return Get the current number of farming activities in 'farming' layer */
    public int getCurrentNumHouseholds()
    {
        Bag householdsbag = getHouseholds();
        if (householdsbag == null || householdsbag.isEmpty()) // might be empty
        {
            return 0;
        }

        //return householdbag.size();   // this won't work because we don't remove households
        int total = 0;
        for (int i = 0; i < householdsbag.size(); i++)
        {
            Household h = (Household) householdsbag.objs[i];
            if (!h.isDisplaced())
            {
                total++;
            }
        }
        assert(repOK());
        return total;
    }
    
    /** @return the current population of herding activities in the simulation */
    public int getCurrentPopulationHerders()
    {
        Bag householdsbag = getHouseholds();
        if (householdsbag == null || householdsbag.isEmpty()) // might be empty
        {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < householdsbag.size(); i++)
        {
            Household thisagent = (Household) householdsbag.objs[i];
            if (thisagent.getHerding() != null)
            {
                total = total + thisagent.getHerding().getPopulation();
            }
        }
        assert(repOK());
        return total;
    }

    /** @return Get the current population of laboring people */
    public int getCurrentPopulationLaborers()
    {
        Bag householdsbag = getHouseholds();
        if (householdsbag == null || householdsbag.isEmpty()) // might be empty
        {
            return 0;
        }

        //return householdbag.size();   // this won't work because we don't remove households
        int total = 0;
        for (int i = 0; i < householdsbag.size(); i++)
        {
            Household thisagent = (Household) householdsbag.objs[i];
            if (thisagent.getLaboring() != null)
            {
                total = total + thisagent.getLaboring().getPopulation();
            }
        }
        assert(repOK());
        return total;
    }

    /** @return Get the current population of non-displaced people. */
    public int getNonDisplacedPopulation()
    {
        Bag householdsbag = getHouseholds();
        if (householdsbag == null || householdsbag.isEmpty()) // might be empty
        {
            return 0;
        }

        int total = 0;
        for (Object h : householdsbag)
            if (!((Household)h).isDisplaced())
                total += ((Household)h).getPopulation();
        assert(repOK());
        return total;
    }
    
    /** @return Get the current population change rate in terms of percent per year */
    public double calculateCurrentPopulationChangeRate()
    {
        int totalPop = getCurrentPopulationFarmers()
                + getCurrentPopulationHerders()
                + getCurrentPopulationLaborers()
                + getCurrentPopulationDisplaced();
        //System.out.println( " world>calculateCurrentPopulationChangeRate> total pop = " + totalPop);

        double changeRate;
        if (totalPop != 0)
        // from daily to annual to percentage
        {
            changeRate = ((double) (totalPop - getLastTotalPop()) * 365.0 * 100.0) / (double) totalPop;
        } else
        {
            changeRate = 0;
        }
        //System.out.println( " world>calculateCurrentPopulationChangeRate> last pop = " + getLastTotalPop());
        //System.out.println( " world>calculateCurrentPopulationChangeRate> pop change = " + (totalPop - getLastTotalPop()));

        setLastTotalPop(totalPop);

        setCurrentPopulationChangeRate(changeRate);
        //System.out.println( " world>calculateCurrentPopulationChangeRate> change rate = " + changeRate);
        //if (changeRate > 0.0)  System.out.println( "greater than zero.");
        //if (changeRate == 0.0) System.out.println( "exactly zero.");    

        assert(repOK());
        return changeRate;
    }
    
    /**
     * Returns the culture ID corresponding to the parcel grid coordinate.
     *
     * @note A zero maps to non-land; e.g., Lake Victoria.
     *
     * @param x
     * @param y
     * @return culture ID as found in MurdockMOA's ID_CULTURE attribute, or will
     * return 0 if no corresponding culture ID found
     *
     */
    public int determineCulture(int x, int y)
    {
        Point p = this.populationGrid.toPoint(x, y);

        Bag coveringObjects = this.ethnicRegions.getCoveringObjects(p);

        // If the coordinate falls outside all the culture boundaries, then
        // coveringObjects will be empty.
        if (coveringObjects.isEmpty())
        {
            return 0;
        }

        // Can't have more than one ethnic region for this guy to be in, so error out.
        // NOTE: due to errors in the shape file it *is* possible for an agent to
        // be in more than one culture area.  In those rare cases we'll just
        // arbitrarily pick the first one.
//        assert coveringObjects.size() == 1 : x + " " + y + " " + coveringObjects.size();

        MasonGeometry masonGeometry = (MasonGeometry) coveringObjects.objs[0];

        int culture = masonGeometry.getIntegerAttribute("ID_CULTURE");
        
        assert(repOK());
        return culture;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public Population(Parameters params, String datapath, Land land)
    {
        assert(params != null);
        assert(datapath != null);        
        this.params = params;
        houseHoldRandomSequence = new RandomSequence(new Steppable[0]);
        
        herdingGrid = new SparseGrid2D(land.getWidth(), land.getHeight());
        farmingGrid = new SparseGrid2D(land.getWidth(), land.getHeight());
        householdsGrid = new SparseGrid2D(land.getWidth(), land.getHeight());
        
        loadEthnicRegions(datapath);
        
        String populationFileName = datapath + params.world.getPopulationFile();
        try
        {
            Logger.getLogger(World.class.getName()).info("Population: Loading data...");
            InputStream populationStream;
            if ("".equals(datapath))
            {
                populationStream = new BufferedInputStream(getClass().getResourceAsStream(params.world.getDatapath() + params.world.getPopulationFile()));
            }
            else
            {
                populationStream = new BufferedInputStream( new FileInputStream(populationFileName) );
            }

            if (populationStream == null)
            {
                throw new FileNotFoundException(populationFileName);
            }

            // This is the grid that holds the LandScan data that is used to
            // populate households biased by population density.
            ArcInfoASCGridImporter.read(populationStream, GeomGridField.GridDataType.INTEGER, populationGrid);

        } catch (IOException ex)
        {
            World.getLogger().log(Level.SEVERE, null, ex);
        }        
        Logger.getLogger(World.class.getName()).info("Population: Finished Loading data...");
        assert(repOK());
    } 
    
    /**
     * load the Murdoch ethnic regions
     *
     * XXX Should consider moving these to WorldBuilder since this is, well,
     * building stuff.
     */
    private void loadEthnicRegions(String dataPath)
    {
        Bag masked = new Bag();
        masked.add("NAME");
        masked.add("ID_CULTURE");

        try
        {
            URL murdockURL;
            if ("".equals(dataPath))
            {
                murdockURL = getClass().getResource(params.world.getDatapath() + params.world.getEthnicRegionsFile());
            }
            else
            {
                File murdockFile = new File(dataPath + params.world.getEthnicRegionsFile());
                murdockURL = murdockFile.toURI().toURL();

                if (murdockURL == null)
                {
                    throw new FileNotFoundException(murdockFile.getName());
                }
            }

            ShapeFileImporter.read(murdockURL, ethnicRegions, masked);

            System.out.println("Loaded " + ethnicRegions.getGeometries().numObjs + " ethnic regions");
        } catch (Exception ex)
        {
            World.getLogger().log(Level.SEVERE, null, ex);
        }

        createEthnicIDMap(ethnicRegions, ethnicIDToName);
    }

    /**
     * Population he ethnic ID to ethnic name map
     *
     * @param ethnicRegions contains the GIS ethnic region data
     * @param ethnicIDToName is the dictionary we'll population from
     * ethnicRegions
     */
    private void createEthnicIDMap(GeomVectorField ethnicRegions, Map<Integer, String> ethnicIDToName)
    {
        Bag ethnicGeometries = ethnicRegions.getGeometries();
        for (int i = 0; i < ethnicGeometries.size(); i++)
        {
            MasonGeometry geometry = (MasonGeometry) ethnicGeometries.objs[i];
            int id = geometry.getIntegerAttribute("ID_CULTURE");
            String name = geometry.getStringAttribute("NAME");
            ethnicIDToName.put(id, name);
        }
    }

    /** @return true if a household can be placed at (x,y) */
    private static boolean isValidHouseholdSite(final Land land, int x, int y)
    {
        final Parcel p = (Parcel) land.getParcel(x, y);
        if (p instanceof GrazableArea)
            return true;

        return false;
    }

    public void populate(final Land land, final WaterHoles waterHoles, final MersenneTwisterFast random)
    {
        if (params.world.isOnlyOneHousehold())
        {
            final int xLoc = params.world.getOneHouseholdX();
            final int yLoc = params.world.getOneHouseholdY();
            if (!isValidHouseholdSite(land, xLoc, yLoc))
                throw new IllegalStateException("Population.populate: Attempted to place only one household on an invalid location.");
            
            final int cultureID = determineCulture(xLoc, yLoc);
            final int countryID = land.getCountry(xLoc, yLoc);
            final GrazableArea location = (GrazableArea) land.getParcel(xLoc, yLoc);
            
            final int size = getNewHouseholdSize(random);
            final int farmSize = params.households.canFarm() ? size : 0;
            Household newHousehold = new Household(params, waterHoles, this, location, farmSize, cultureID, countryID, size, random);
            houseHoldRandomSequence.addSteppable(newHousehold);
        }
        else
        {
            placeHouseholdsUsingPopulationData(land, waterHoles, random);
        }
        assert(repOK());
    }
    
    /** Place the population based on Landscan data.
     * <p>
     * Note that World.initTruePopulationPortion dictates what percentage of
     * the true population we should use.  I.e., we may only want 1% of the true
     * population so that running the simulation on lower powered machines
     * becomes feasable.
     */
    private void placeHouseholdsUsingPopulationData(final Land land, final WaterHoles waterHoles, final MersenneTwisterFast random)
    {
        
        class RowPopulator implements Runnable
        {
            private final int row;
            private final MersenneTwisterFast localRandom;
            RowPopulator(int row, long seed)
            {
                this.row = row;
                localRandom = new MersenneTwisterFast(seed); // MersenneTwisterFast is not threadsafe, so we chain PRNG's.
            }
            
            @Override
            public void run() {
                for (int x = land.getSubAreaUpperLeft().x; x < land.getSubAreaLowerRight().x; x++)
                {
                    if ( isValidHouseholdSite(land, x, row) )
                        populateParcel(x, row, land, localRandom, waterHoles);
                }
                Logger.getLogger(World.class.getName()).info(String.format("Row %d populated", row));
            }
        }
        
        Logger.getLogger(World.class.getName()).info("Entering Population.placeHouseholdsInRegionUsingData(World world)");
        householdsGrid.clear(); // Clear out any households from any previous runs.
        if (params.world.getInitTruePopulationPortion() <= 0)
            return;

        ExecutorService executor = Executors.newFixedThreadPool(params.system.getNumthreads());
        for (int y = land.getSubAreaUpperLeft().y; y < land.getSubAreaLowerRight().y; y++)
        {
            executor.execute(new RowPopulator(y, random.nextLong()));
        }
        executor.shutdown();
        while(!executor.isTerminated()) { }
    }
    
    private void populateParcel(int x, int y, Land land, MersenneTwisterFast random, WaterHoles waterHoles)
    {
        final int desiredParcelPop = (int)Math.round(((IntGrid2D) populationGrid.getGrid()).get(x, y) * params.world.getInitTruePopulationPortion());
        final int cultureID = determineCulture(x, y);
        final int countryID = land.getCountry(x, y);
        final GrazableArea location = (GrazableArea) land.getParcel(x, y);
        
        int parcelPopSoFar = 0;
        List<Household> newHouseholds = new ArrayList<Household>(50);
        
        // Generate households that make up parcelPop persons.
        while(parcelPopSoFar < desiredParcelPop)
        {
            int householdPop = getNewHouseholdSize(random);
            
            // If this is the last household, cull its size so we don't overfill the parcel.
            final int newPop = parcelPopSoFar + householdPop;
            assert(parcelPopSoFar != desiredParcelPop);
            if (newPop > desiredParcelPop)
                householdPop = desiredParcelPop - parcelPopSoFar;
            
            // Initialize the household with a phony farm size.  
            // We will come back and reset all the farm sizes once we know how many households are placed on this parcel.
            final int initialFarmSize = 0;
            Household newHousehold = new Household(params, waterHoles, this, location, initialFarmSize, cultureID, countryID, householdPop, random);
            
            newHouseholds.add(newHousehold);
            parcelPopSoFar += householdPop;
        }
        
        if (!newHouseholds.isEmpty())
        {
            if (params.households.canFarm())
            {
                final double maxSizeInHectares = 100.0/newHouseholds.size();
                for (Household h : newHouseholds)
                {
                    // initialize to either 1 hectare per person or max possible allocated
                    h.setFarmAreaInHectares(Math.min(maxSizeInHectares, h.getFarming().getPopulation()));
                    assert (h.getFarmAreaInHectares() > 0);
                }
            }
            
            for (Household h : newHouseholds)
            {
                synchronized(houseHoldRandomSequence)
                {
                    houseHoldRandomSequence.addSteppable(h);
                }
            }
        }
    }
    // </editor-fold>
    
    final public boolean repOK()
    {
        assert(!(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(householdsGrid.getAllObjects(), Household.class)));
        assert(!(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(herdingGrid.getAllObjects(), Herding.class)));
        assert(!(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(farmingGrid.getAllObjects(), Farming.class)));
        
        return params != null
                && populationGrid != null
                && householdsGrid != null
                && herdingGrid != null
                && farmingGrid != null
                && ethnicRegions != null
                && houseHoldRandomSequence != null
                && ethnicIDToName != null
                && displacementEvents != null
                && displacedPopulation >= 0
                && !(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(householdsGrid.getAllObjects(), Household.class))
                && !(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(herdingGrid.getAllObjects(), Herding.class))
                && !(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(farmingGrid.getAllObjects(), Farming.class));
    }
}
