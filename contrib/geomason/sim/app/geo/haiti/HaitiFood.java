package sim.app.geo.haiti;



import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import sim.engine.RandomSequence;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.engine.TentativeStep;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.field.grid.IntGrid2D;
import sim.field.grid.ObjectGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.io.geo.ArcInfoASCGridImporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;



public class HaitiFood extends SimState
{
    private static final long serialVersionUID = 1L;

    public static int noRoadValue = 15;

    public static int maximumDensity = 20;

    public static int riotDensity = 18;

    public static int reportOrder = 5;


    /**
     * Main function allows simulation to be run in stand-alone, non-GUI mode
     */
    public static void main(String[] args)
    {
        doLoop(HaitiFood.class, args);
        System.exit(0);
    }

    public GeomGridField roads;

    public GeomGridField destruction;

    public SparseGrid2D centers;

    public SparseGrid2D population;

    ArrayList<IntGrid2D> distanceGradients = new ArrayList<IntGrid2D>();

    public ObjectGrid2D locations;

    public SparseGrid2D nodes;

    /** the road nodes closest to each of the locations */
    public ObjectGrid2D closestNodes; // 

    Network roadNetwork = new Network();

    ArrayList<Center> centersList = new ArrayList<Center>();

    ArrayList<Agent> peopleList = new ArrayList<Agent>();

    int centersInitialFood = -1;

    int energyPerFood = -1;

    int gridWidth;

    int gridHeight;

    // the scheduling order in which processes fire
    int resetOrder = 1;

    int centerOrder = 2;

    int personOrder = 3;

    int rumorOrder = 4;

    int deaths_total = 0;

    int deaths_this_tick = 0;

    int rioting = 0;

    // agent parameters
    double enToStay = -1, enWalkPaved = -1, enWalkUnpav = -1, enRiot = -1;

    int interval = -1;

    // Relief File Settings ///
    String reliefFile = null;


    String[] reliefFiles = new String[]
    {
        "data/relief1.asc.gz",
        "data/reliefBETTA.asc.gz",
        "data/reliefOKBETTA.asc.gz",
        "data/reliefBAD.asc.gz",
        "data/reliefSingle.asc.gz"
    };


    String[] reliefFilesNames = new String[]
    {
        "Neutral", "Good", "Better", "Bad", "Single"
    };

    // making the Relief file modifiable
    int reliefFileIndex = 0;


    String roadsFile, roadVectorFile, destructionFile, popFile;


    /**
     * Constructor
     */
    public HaitiFood(long seed)
    {
        super(seed);

        roadsFile = "data/roads1.asc.gz";
        roadVectorFile = "data/Haiti_all_roads_Clip.shp";
        destructionFile = "data/destruction.asc.gz";

        if (reliefFile == null)
        {
            reliefFile = "data/relief1.asc.gz";
        }

        popFile = "data/pop.asc.gz";

    }


    public HaitiFood(long seed, int maxDen, int riotDen, int initFood, int energyFood, double enToStay, double enWalkPaved, double enWalkUnpav, double enRiot, int interval)
    {
        super(seed);
        maximumDensity = maxDen;
        riotDensity = riotDen;

        centersInitialFood = initFood;
        energyPerFood = energyFood;

        this.enToStay = enToStay;
        this.enWalkPaved = enWalkPaved;
        this.enWalkUnpav = enWalkUnpav;
        this.enRiot = enRiot;
        this.interval = interval;

        roadsFile = "data/roads1.asc.gz";
        roadVectorFile = "data/Haiti_all_roads_Clip.shp";
        destructionFile = "data/destruction.asc.gz";
        popFile = "data/pop.asc.gz";
        if (reliefFile == null)
        {
            reliefFile = reliefFiles[0]; // pick the default
        }
    }


    public int getReliefFileIndex()
    {
        return reliefFileIndex;
    }

    public void setReliefFileIndex(int r)
    {
        reliefFileIndex = r;
        reliefFile = reliefFiles[r];
    }


    public Object domReliefFileIndex()
    {
        return reliefFilesNames;
    }


    /**
     * Initialization
     */
    @Override
    public void start()
    {
        super.start();

        // ---- read in data ----

        try
        {
            // --- ROADS ---

            // read in the raw roads raster (the situation "on the ground")
            System.out.println("reading roads layer...");
            roads = readIntGridDataWithDefaultValue(roadsFile, noRoadValue);

            // store the information about the size of the simulation space
            gridWidth = roads.getGridWidth();
            gridHeight = roads.getGridHeight();

            // read in the road vector information (the way people *think* about
            // the road network)
            GeomVectorField roadLinks = new GeomVectorField();

            URL roadVectorFileURL = HaitiFood.class.getResource(roadVectorFile);
            ShapeFileImporter.read(roadVectorFileURL, roadLinks);
            
            nodes = new SparseGrid2D(gridWidth, gridHeight);
            extractFromRoadLinks(roadLinks); // construct a network of roads

            // set up the locations and nearest node capability
            initializeLocations();
            closestNodes = setupNearestNodes();

            // --- DESTRUCTION ---

            // read in the destruction information
            System.out.println("reading destruction layer...");
            destruction = setupDestructionFromFile(destructionFile);

            // --- DISTRIBUTION CENTERS ---

            // read in the information about food distribution centers
            System.out.println("reading distribution centers layer...");
            centers = setupCentersFromFile(reliefFile);

            // set them up
            for (Center c : centersList)
            {
                c.loc = (Location) locations.get(c.loc.x, c.loc.y);
            }

            // ---- AGENTS ----
            System.out.println("reading population layer...");
            GeomGridField tempPop = readIntGridDataWithDefaultValue(popFile, 0);
            population = new SparseGrid2D(tempPop.getGridWidth(),
                                          tempPop.getGridHeight());
            populate(tempPop); // set it up

        } catch (IOException ex)
        {
            Logger.getLogger(HaitiFood.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex)
        {
            Logger.getLogger(HaitiFood.class.getName()).log(Level.SEVERE, null, ex);
        }

        // ---- set up rumors/information spread ----

        RumorMill rumorMill = new RumorMill();
        rumorMill.gridWidth = gridWidth;
        rumorMill.gridHeight = gridHeight;
        schedule.scheduleRepeating(rumorMill, rumorOrder, 1);

        // spread initial information
        triggerRumors();

        schedule.scheduleRepeating(new Steppable()
        {
            public void step(SimState state)
            {
                deaths_this_tick = 0;
                rioting = 0;
            }


        }, resetOrder, 1);
    }


    /**
     * Converts information extracted from the shapefile into links determined
     * by LineString subsequences
     */
    void extractFromRoadLinks(GeomVectorField roadLinks)
    {
        Bag geoms = roadLinks.getGeometries();
        Envelope e = roadLinks.getMBR();
        double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e
            .getMaxY();
        int xcols = gridWidth - 1, ycols = gridHeight - 1;

        // extract each edge
        for (Object o : geoms)
        {

            MasonGeometry gm = (MasonGeometry) o;
            if (gm.getGeometry() instanceof LineString)
            {
                readLineString((LineString) gm.getGeometry(), xcols, ycols,
                               xmin, ymin, xmax, ymax);
            } else if (gm.getGeometry() instanceof MultiLineString)
            {
                MultiLineString mls = (MultiLineString) gm.getGeometry();
                for (int i = 0; i < mls.getNumGeometries(); i++)
                {
                    readLineString((LineString) mls.getGeometryN(i), xcols,
                                   ycols, xmin, ymin, xmax, ymax);
                }
            }
        }
    }


    /**
     * Converts an individual linestring into a series of links and nodes in the
     * network
     * <p/>
     * @param geometry
     * @param xcols    - number of columns in the field
     * @param ycols    - number of rows in the field
     * @param xmin     - minimum x value in shapefile
     * @param ymin     - minimum y value in shapefile
     * @param xmax     - maximum x value in shapefile
     * @param ymax     - maximum y value in shapefile
     */
    void readLineString(LineString geometry, int xcols, int ycols, double xmin,
                        double ymin, double xmax, double ymax)
    {

        CoordinateSequence cs = geometry.getCoordinateSequence();

        // iterate over each pair of coordinates and establish a link between
        // them
        Node oldNode = null; // used to keep track of the last node referenced
        for (int i = 0; i < cs.size(); i++)
        {

            // calculate the location of the node in question
            double x = cs.getX(i), y = cs.getY(i);
            int xint = (int) Math.floor(xcols * (x - xmin) / (xmax - xmin)), yint = (int) (ycols - Math
                                                                                           .floor(ycols * (y - ymin) / (ymax - ymin))); // REMEMBER TO FLIP THE Y VALUE

            if (xint >= gridWidth)
            {
                continue;
            } else if (yint >= gridHeight)
            {
                continue;
            }

            // find that node or establish it if it doesn't yet exist
            Bag ns = nodes.getObjectsAtLocation(xint, yint);
            Node n;
            if (ns == null)
            {
                n = new Node(new Location(xint, yint));
                nodes.setObjectLocation(n, xint, yint);
            } else
            {
                n = (Node) ns.get(0);
            }

            if (oldNode == n) // don't link a node to itself
            {
                continue;
            }

            // attach the node to the previous node in the chain (or continue if
            // this is the first node in the chain of links)

            if (i == 0)
            { // can't connect previous link to anything
                oldNode = n; // save this node for reference in the next link
                continue;
            }

            int weight = (int) n.loc.distanceTo(oldNode.loc); // weight is just
            // distance

            // create the new link and save it
            Edge e = new Edge(oldNode, n, weight);
            roadNetwork.addEdge(e);
            oldNode.links.add(e);
            n.links.add(e);

            oldNode = n; // save this node for reference in the next link
        }
    }

    /** set up locations on the location grid for ease of reference */
    void initializeLocations()
    {
        locations = new ObjectGrid2D(gridWidth, gridHeight);
        for (int i = 0; i < gridWidth; i++)
        {
            for (int j = 0; j < gridHeight; j++)
            {
                Location l = new Location(i, j);
                locations.set(i, j, l);
            }
        }
    }


    /**
     * Trigger the initial rumors in a 5 tile area around the centers
     */
    void triggerRumors()
    {
        int infoRadius = 5;

        for (int i = 0; i < centersList.size(); i++)
        {
            Center c = centersList.get(i);
            Bag seeFoodAtCenter = new Bag();
            population.getNeighborsMaxDistance(c.loc.x, c.loc.y, infoRadius,
                                               false, seeFoodAtCenter, null, null);
            for (Object o : seeFoodAtCenter)
            {
                Agent a = (Agent) o;
                a.centerInfo += Math.pow(2, i);
            }
        }

    }


    void populate(GeomGridField pop)
    {
        long forReference = 0;

        // initialize all pop holders
        for (int i = 0; i < pop.getGridWidth(); i++)
        {

            System.out.println("finished: " + i);

            for (int j = 0; j < pop.getGridHeight(); j++)
            {

                ArrayList<Agent> ps = new ArrayList<Agent>();

                // get the population
                int tile = ((IntGrid2D)pop.getGrid()).get(i, j);
                double tilePop = tile / 10000.0; //this is a sample drawn from landscan data

                if (tilePop > 0)
                {
                    forReference += tile;

                    int intPop;

                    // with equal probability, round up or down
                    if (this.random.nextBoolean())
                    {
                        intPop = (int) Math.floor(tilePop);
                    } else
                    {
                        intPop = (int) Math.ceil(tilePop);
                    }

                    Location here = new Location(i, j);
                    int destructionLevel = ((IntGrid2D)destruction.getGrid()).get(i, j);

                    for (int x = 0; x < Math.min(intPop, 12); x++)
                    {
                        Agent a;
                        if (interval < 0)
                        {
                            a = new Agent(here, here.copy(), destructionLevel);
                        } else
                        {
                            a = new Agent(here, here.copy(), destructionLevel, enToStay,
                                          enWalkPaved, enWalkUnpav, enRiot, interval);
                        }

                        population.setObjectLocation(a, i, j);

                        //			a.stopper = schedule.scheduleRepeating(a, personOrder, 1);

                        peopleList.add(a);
                    }
                }
            }
        }

        Steppable[] steppers = new Steppable[peopleList.size()];
        for (int i = 0; i < steppers.length; i++)
        {
            steppers[i] = (Steppable) (peopleList.get(i));
            Agent a = (Agent) steppers[i];
            steppers[i] = new TentativeStep(a);
            a.stopper = (Stoppable) steppers[i];
        }

        RandomSequence seq = new RandomSequence(steppers);
        schedule.scheduleRepeating(seq, personOrder);

        System.out.println("Population Size: " + peopleList.size());
        System.out.println("Sum of tiles: " + forReference);
        if (interval < 0)
        {
            enToStay = Agent.ENERGY_TO_STAY;
            enWalkPaved = Agent.ENERGY_TO_WALK_PAVED;
            enWalkUnpav = Agent.ENERGY_TO_WALK_UNPAVED;
            enRiot = Agent.ENERGY_TO_RIOT;
        }

    }


    /**
     * @param filename is the name of the file that holds the raster data
     * @param defaultValue is what the NO DATA values are set to
     */
    GeomGridField readIntGridDataWithDefaultValue(String filename, int defaultValue)
    {
        GZIPInputStream fstream = null;

        GeomGridField myGrid = new GeomGridField();

        try
        {
            InputStream inputStream = HaitiFood.class.getResourceAsStream(filename);

            if (inputStream == null)
            {
                    throw new FileNotFoundException(filename);
            }

            fstream = new GZIPInputStream(inputStream);

            ArcInfoASCGridImporter.read(fstream, GeomGridField.GridDataType.INTEGER, myGrid);
            
        } catch(FileNotFoundException ex)
        {
            Logger.getLogger(HaitiFood.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(HaitiFood.class.getName()).log(Level.SEVERE, null, ex);
        } finally
        {
            try
            {
                fstream.close();
            } catch (IOException ex)
            {
                Logger.getLogger(HaitiFood.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // -9999 is the NO DATA value; so we just replace that with the desired
        // default value.
        Utilities.gridReplace((IntGrid2D)myGrid.getGrid(), -9999, defaultValue);

        return myGrid;
    }



    /**
     * @param filename - the name of the file that holds the data
     */
    GeomGridField readIntGridData(String filename)
    {
        GZIPInputStream fstream = null;

        GeomGridField myGrid = new GeomGridField();

        try
        {
            InputStream inputStream = HaitiFood.class.getResourceAsStream(filename);

            if (inputStream == null)
            {
                    throw new FileNotFoundException(filename);
            }

            fstream = new GZIPInputStream(inputStream);

            ArcInfoASCGridImporter.read(fstream, GeomGridField.GridDataType.INTEGER, myGrid);

        } catch(FileNotFoundException ex)
        {
            Logger.getLogger(HaitiFood.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(HaitiFood.class.getName()).log(Level.SEVERE, null, ex);
        } finally
        {
            try
            {
                fstream.close();
            } catch (IOException ex)
            {
                Logger.getLogger(HaitiFood.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return myGrid;
    }


    /**
     * @param filename - the name of the file that holds the data
     */
    GeomGridField setupDestructionFromFile(String filename)
    {
        GeomGridField field = readIntGridData(filename);

        for (int x = 0; x < field.getGridWidth(); x++)
        {
            for (int y = 0; y < field.getGridHeight(); y++)
            {
                switch( ((IntGrid2D)field.getGrid()).get(x, y) )
                {
                    case -9999 : // no data, ignore
                    case 51 :
                    default :
                        ((IntGrid2D)field.getGrid()).set(x, y, 0);
                        break;
                    case 102 : // light damage
                        ((IntGrid2D)field.getGrid()).set(x, y, 1);
                        break;
                    case 153 :
                        ((IntGrid2D)field.getGrid()).set(x, y, 2);
                        break;
                    case 204 :
                        ((IntGrid2D)field.getGrid()).set(x, y, 3);
                        break;
                    case 255 : // severe damage
                        ((IntGrid2D)field.getGrid()).set(x, y, 4);
                        break;
                }
            }
        }

        return field;
    }
    


    /**
     * @param filename - the name of the file that holds the data
     * @param field    - the field to be populated
     */
    SparseGrid2D setupCentersFromFile(String filename)
    {

        SparseGrid2D field = null;

        try
        { // to read in a file

            // Open the file

            URL inputFile = HaitiFood.class.getResource(filename);

            FileInputStream fstream = new FileInputStream(inputFile.getFile());

            GZIPInputStream zippedInputStream = new GZIPInputStream(fstream);

            // Convert our input stream to a BufferedReader
            BufferedReader d = new BufferedReader(
                new InputStreamReader(zippedInputStream));

            // get the parameters from the file
            String s;
            int width = 0, height = 0;
            double nodata = -1;
            for (int i = 0; i < 6; i++)
            {

                s = d.readLine();

                // format the line appropriately
                String[] parts = s.split(" ", 2);
                String trimmed = parts[1].trim();

                // save the data in the appropriate place
                if (i == 1)
                {
                    height = Integer.parseInt(trimmed);
                } else if (i == 0)
                {
                    width = Integer.parseInt(trimmed);
                } else if (i == 5)
                {
                    nodata = Double.parseDouble(trimmed);
                } else
                {
                    continue;
                }
            }

            // set up the field to hold the data
            field = new SparseGrid2D(width, height);

            // read in the data from the file and store it in tiles
            int i = 0, j = 0;
            while ((s = d.readLine()) != null)
            {
                String[] parts = s.split(" ");

                for (String p : parts)
                {

                    int value = Integer.parseInt(p);
                    if (value == 1)
                    {
                        // update the field
                        Bag otherCenters = field.getNeighborsHamiltonianDistance(
                            j, i, 5, false, new Bag(), null, null);
                        if (otherCenters.size() > 0)
                        // there is already another center established here: we're describing the same center
                        {
                            continue;
                        }

                        Center c;
                        if (centersInitialFood < 0)
                        {
                            c = new Center(j, i, 100); // IF YOU WANT TO INCREASE INITIAL FOOD ALLOCATION FOR CENTERS, HERE
                        } else
                        {
                            c = new Center(j, i, centersInitialFood, energyPerFood);
                        }

                        field.setObjectLocation(c, j, i);
                        centersList.add(c);
                        schedule.scheduleRepeating(c, centerOrder, 1);
                    }
                    j++; // increase the column count
                }

                j = 0; // reset the column count
                i++; // increase the row count
            }

            if (centersInitialFood < 0)
            {
                centersInitialFood = 100;
                energyPerFood = Center.ENERGY_FROM_FOOD;
            }
        } // if it messes up, print out the error
        catch (Exception e)
        {
            System.out.println(e);
        }
        return field;
    }




    /**
     * Calculate the nodes nearest to each location and store the information
     * <p/>
     * @param closestNodes - the field to populate
     */
    ObjectGrid2D setupNearestNodes()
    {
        ObjectGrid2D myClosestNodes = new ObjectGrid2D(gridWidth, gridHeight);
        ArrayList<Crawler> crawlers = new ArrayList<Crawler>();

        for (Object o : roadNetwork.allNodes)
        {
            Node n = (Node) o;
            Crawler c = new Crawler(n, n.loc);
            crawlers.add(c);
        }

        // while there is unexplored space, continue!
        while (crawlers.size() > 0)
        {
            ArrayList<Crawler> nextGeneration = new ArrayList<Crawler>();

            // randomize the order in which cralwers are considered
            int size = crawlers.size();

            for (int i = 0; i < size; i++)
            {

                // randomly pick a remaining crawler
                int index = random.nextInt(crawlers.size());
                Crawler c = crawlers.remove(index);

                // check if the location has already been claimed
                Node n = (Node) myClosestNodes.get(c.loc.x, c.loc.y);

                if (n == null)
                { // found something new! Mark it and reproduce

                    // set it
                    myClosestNodes.set(c.loc.x, c.loc.y, c.node);

                    // reproduce
                    Bag neighbors = new Bag();
                    locations.getNeighborsHamiltonianDistance(c.loc.x, c.loc.y,
                                                              1, false, neighbors, null, null);
                    for (Object o : neighbors)
                    {
                        Location l = (Location) o;
                        if (l == c.loc)
                        {
                            continue;
                        }
                        Crawler newc = new Crawler(c.node, l);
                        nextGeneration.add(newc);
                    }
                }
                // otherwise just die
            }
            crawlers = nextGeneration;
        }
        return myClosestNodes;
    }


    /**
     * Write the results out to file and clean up after the model
     */
    public void finish()
    {
        String report = "haitiResults.txt";
        try
        {
            // Convert our stream to a BufferedWriter
            BufferedWriter w = new BufferedWriter(new FileWriter(report, true));

            int totalNumberPeople = peopleList.size() + deaths_total;
            long totalEnergyInSystem = 0L;
            for (Agent a : peopleList)
            {
                totalEnergyInSystem += a.energyLevel;
            }

            int totalFoodLeft = 0;
            for (Center c : centersList)
            {
                totalFoodLeft += c.foodLevel;
            }

            // make a csv by replacing each '\t' with ','
            // popfile is output to tell you which scale you're using
            String output = popFile + "\t" + reliefFile + "\t" + maximumDensity + "\t" + riotDensity + "\t"
                + centersInitialFood + "\t" + energyPerFood + "\t"
                + enToStay + "\t" + enWalkPaved + "\t" + enWalkUnpav + "\t" + enRiot + "\t"
                + interval + "\t" + schedule.getSteps() + "\t" + totalNumberPeople + "\t" + deaths_total + "\t"
                + totalEnergyInSystem + "\t" + centersList.size() + "\t" + totalFoodLeft;

            w.write(output);
            w.newLine();
            w.flush();

            w.close();

        } catch (Exception e)
        {
            System.err.println("File input error");
        }


        kill();
    }



    /**
     * Used to store information about the road network
     */
    class Node
    {
        Location loc;

        ArrayList<Edge> links;

        public Node(Location l)
        {
            loc = l;
            links = new ArrayList<Edge>();
        }
    }



    /**
     * Used to find the nearest node for each space
     * <p/>
     */
    class Crawler
    {
        Node node;

        Location loc;

        public Crawler(Node n, Location l)
        {
            node = n;
            loc = l;
        }
    }


}