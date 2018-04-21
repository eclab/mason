package ebola;

import com.vividsolutions.jts.geom.*;
import ebola.ebolaData.EbolaData;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.field.grid.*;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.io.geo.*;
import sim.util.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.*;
import java.util.*;

import net.sf.csv4j.*;
import sim.util.geo.MasonGeometry;

/**
 * Created by rohansuri on 7/8/15.
 */
public class EbolaBuilder
{
    public static EbolaABM ebolaSim;
    /**
     * Contains keys that are the integer ids for each of the country's provinces/counties.
     * The arraylist is has distributions for the following age groups in the following order by increasing intervals of five years:
     * 0-4, 5-9, 10-14, 15-19, 20-24, 25-29, 30-34, 35-39, ... , 70-74, 75-80, 80+
     * A total of 17 groups, distribution is cumalitive so the last group should be ~ 1.0
     */
    private static HashMap<Integer, ArrayList<Double>> age_dist;

    public static HashSet<Geometry> removeGeometry = new HashSet<Geometry>();
    public static HashSet<LineString> allLineStrings = new HashSet<LineString>();
    public static void initializeWorld(EbolaABM sim, String pop_file, String admin_file, String age_dist_file)
    {
        ebolaSim = sim;
        age_dist = new HashMap<Integer, ArrayList<Double>>();
        ebolaSim.world_height = 9990;
        ebolaSim.world_width = 9390;

        ebolaSim.allRoadNodes = new SparseGrid2D(ebolaSim.world_width, ebolaSim.world_height);
        ebolaSim.roadNetwork = new Network();
        ebolaSim.roadLinks = new GeomVectorField(ebolaSim.world_width, ebolaSim.world_height);
        System.out.println("(" + ebolaSim.world_width + ", " + ebolaSim.world_height + ")");
        GeomVectorField schools_vector = new GeomVectorField();
        GeomVectorField farms_vector = new GeomVectorField();
        GeomVectorField hospitals_vector = new GeomVectorField();
        GeomVectorField places_vector = new GeomVectorField();

        ebolaSim.schoolGrid = new SparseGrid2D(ebolaSim.world_width, ebolaSim.world_height);
        ebolaSim.farmGrid = new SparseGrid2D(ebolaSim.world_width, ebolaSim.world_height);
        ebolaSim.hospitalGrid = new SparseGrid2D(ebolaSim.world_width, ebolaSim.world_height);
        ebolaSim.placesGrid = new SparseGrid2D(ebolaSim.world_width, ebolaSim.world_height);


        //initialize node map for all work locations
        for(int i = 0; i < Parameters.WORK_SIZE_BY_SECTOR.length; i++)
            ebolaSim.workNodeStructureMap.add(new HashMap<Node, List<Structure>>(10000, 0.75f));

        try
        {
            String[] files = {Parameters.ROADS_SHAPE_PATH, Parameters.SCHOOLS_PATH, Parameters.FARMS_PATH, Parameters.HOSPITALS_PATH, "/ebola/ebolaData/places_shapefile/all_places.shp"};//all the files we want to read in
            GeomVectorField[] vectorFields = {ebolaSim.roadLinks, schools_vector, farms_vector, hospitals_vector, places_vector};//all the vector fields we want to fill
            readInShapefile(files, vectorFields);

            System.out.println("Done getting information, now analyzing.");

            //get a grid as a base for the mbr
            GeomGridField gridField = new GeomGridField();//just to align mbr
            InputStream inputStream = EbolaData.class.getResourceAsStream(Parameters.ADMIN_ID_PATH);
            ArcInfoASCGridImporter.read(inputStream, GeomGridField.GridDataType.INTEGER, gridField);
            ebolaSim.admin_id = (IntGrid2D)gridField.getGrid();
            //System.out.println("236, 507 = " + ebolaSim.admin_id.get(236, 507));
            inputStream = EbolaData.class.getResourceAsStream(Parameters.POP_PATH);
            ArcInfoASCGridImporter.read(inputStream, GeomGridField.GridDataType.INTEGER, gridField);

            //align mbr for all vector files read
            System.out.println("Algining");
            alignVectorFields(gridField, vectorFields);

            //Read in the road cost file
            long now = System.currentTimeMillis();
            System.out.print("Reading in road_cost ");
            readInRoadCost();
            System.out.println("[" + (System.currentTimeMillis() - now) / 1000 + " secs ]");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        //read in movement patters
        setUpMovementMap(Parameters.MOVEMENT_PATH);

        //construct network of roads from roadLinks
        extractFromRoadLinks(ebolaSim.roadLinks, ebolaSim);
        System.out.println("Un trimmed network size = " + ebolaSim.allRoadNodes.size());

        //add schools from vectorfield
        readInStructures(schools_vector, ebolaSim.schoolGrid, ebolaSim.schools, new School(null));

        //add farms from vectorfield
        //readInStructures(farms_vector, ebolaSim.farmGrid, ebolaSim.farms, new WorkLocation(null, Constants.AGRICULTURE));

        //add hospitals from vectorfield
        //readInStructures(hospitals_vector, ebolaSim.hospitalGrid, new Bag(), new WorkLocation(null, Constants.HEALTH));

        //add all places from vectorfield
        readInStructures(places_vector, ebolaSim.placesGrid, new Bag(), new Structure(null));

        //assignNearest Nodes to all facilities except households
        assignNearestNode(ebolaSim.schoolGrid, ebolaSim.workNodeStructureMap.get(Constants.EDUCATION));
        //assignNearestNode(ebolaSim.farmGrid, ebolaSim.workNodeStructureMap.get(Constants.AGRICULTURE));
        //assignNearestNode(ebolaSim.hospitalGrid, ebolaSim.workNodeStructureMap.get(Constants.HEALTH));
        assignNearestNode(ebolaSim.placesGrid, ebolaSim.placesNodes);

        //read in csv that gives the distribution of ages for the three countries from landscan data
        setUpAgeDist(age_dist_file);

        //Create the population - note, this call assumes all structures have been read in
        addHousesAndResidents(pop_file, admin_file);

        //now give each resident a sector_id and worklocatino
        setWorkLocationsForAllResidents(new HashSet<WorkLocation>(), ebolaSim.world.getAllObjects());

        // set up the locations and nearest node capability
        long time = System.currentTimeMillis();
        System.out.println("Assigning nearestNodes...");
        //assignNearestNode(ebolaSim.householdGrid, ebolaSim.householdNodes);
        System.out.println("time = " + ((System.currentTimeMillis() - time) / 1000 / 60) + " minutes");
    }

    static void alignVectorFields(GeomGridField base, GeomVectorField[] others)
    {
        Envelope globalMBR = base.getMBR();

        for(GeomVectorField vf: others)
            globalMBR.expandToInclude(vf.getMBR());
        for(GeomVectorField vf: others)
            vf.setMBR(globalMBR);
    }
    private static URL getUrl(String nodesFilename) throws IOException {
        InputStream nodeStream = EbolaData.class.getResourceAsStream(nodesFilename);
        try {
            if (!new File("./shapeFiles/").exists()) {
                new File("./shapeFiles/").mkdir();
            }
            File targetFile = new File("./shapeFiles/" + nodesFilename.split("/")[nodesFilename.split("/").length - 1]);
            OutputStream outStream = new FileOutputStream(targetFile);
            //outStream.write(buffer);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = nodeStream.read(bytes)) != -1) {
                outStream.write(bytes, 0, read);
            }
            outStream.close();
            nodeStream.close();
            if (nodesFilename.endsWith(".shp")) {
                getUrl(nodesFilename.replace("shp", "dbf"));
                getUrl(nodesFilename.replace("shp", "prj"));
                getUrl(nodesFilename.replace("shp", "sbx"));
                getUrl(nodesFilename.replace("shp", "sbn"));
                getUrl(nodesFilename.replace("shp", "shx"));
            }
            return targetFile.toURI().toURL();
        } catch (Exception e) {
            if (nodesFilename.endsWith("shp")) {
                e.printStackTrace();
                return null;
            } else {
                //e.printStackTrace();
                return null;
            }
        }
    }
    static void readInShapefile(String[] files, GeomVectorField[] vectorFields)
    {
        try
        {
            for(int i = 0; i < files.length; i++)
            {
                String filePath = files[i];
                Bag schools_masked = new Bag();
                System.err.println("URL: " + files[i]);
                URL shapeURI = getUrl(filePath);;
                ShapeFileImporter.read(shapeURI, vectorFields[i], schools_masked);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    static void readInRoadCost()
    {
        try
        {
            ebolaSim.road_cost = new DoubleGrid2D(ebolaSim.world_width, ebolaSim.world_height);

           // FileInputStream fileInputStream = new FileInputStream(new File(Parameters.ROADS_COST_PATH));
            DataInputStream dataInputStream = new DataInputStream(EbolaData.class.getResourceAsStream(Parameters.ROADS_COST_PATH));

            for(int i = 0; i < ebolaSim.world_width; i++)
                for(int j = 0; j < ebolaSim.world_height; j++)
                    ebolaSim.road_cost.set(i, j, dataInputStream.readDouble());
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private static void editAndWriteRaster()
    {
        //TEMP
//            roads_grid = new GeomGridField();
//            InputStream is = new FileInputStream("data/all_roads_trim_raster.asc");
//            ArcInfoASCGridImporter.read(is, GeomGridField.GridDataType.INTEGER, roads_grid);
        //TEMP AF
//        IntGrid2D grid = (IntGrid2D)roads_grid.getGrid();
//        for(int i = 0; i < grid.getWidth(); i++)
//            for(int j = 0; j < grid.getHeight(); j++)
//                if(grid.get(i,j) != -9999)
//                    grid.set(i,j,0);
//        roads_grid.setGrid(grid);
//        //now write it
//        try {
//            BufferedWriter writer = new BufferedWriter( new FileWriter("roads_trim_zero.asc") );
//            ArcInfoASCGridExporter.write(roads_grid, writer);
//            writer.close();
//        } catch (IOException ex) {
//        /* handle exception */
//            ex.printStackTrace();
//        }
    }

    private static void remove()
    {
        //            int sum = 0;
//            int max = 0;
//            int[] frequency = new int[100];
//            for(int i = 0; i < allNetworks.size(); i++)
//            {
//                HashSet<LineString> set = allNetworks.get(i);
//                int total_nodes = 0;
//                for(LineString lineString: set)
//                    total_nodes += lineString.getNumPoints();
//
//                sum += total_nodes;
//                if(total_nodes > max)
//                    max = total_nodes;
//
//                if(total_nodes < 500)
//                    frequency[total_nodes/5]++;
//                else
//                    frequency[99]++;
//
//                if(total_nodes > 100)
//                {
//                    for(LineString lineString: set)
//                    {
//                        //removeGeometry.add((Geometry)lineString);
//                        MasonGeometry mg = new MasonGeometry();
//                        mg.geometry = lineString;
//                        ebolaSim.roadLinks.addGeometry(mg);
//                    }
//                }
//                else
//                {
//                    for(LineString lineString: set)
//                    {
//                        removeGeometry.add((Geometry)lineString);
//                    }
//                }
//
//            }
//            System.out.println("Max allRoadNodes = " + max);
//            //System.out.println("Average allRoadNodes = " + sum*1.0/allNetworks.size());
//
//            String[] s = new String[frequency.length];
//            for(int i = 0; i < frequency.length; i++)
//            {
//                s[i] = (i+1)*5 + "";
//            }
//
//            for(int i = 0; i < frequency.length; i++)
//            {
//                System.out.print(frequency[i] + " \t\t\t");
//                ebolaSim.roadNetworkDistribution.addValue(frequency[i],"Number of allRoadNodes",s[i]);
//            }
//            System.out.println();
//            for(int i = 0; i < frequency.length; i++)
//            {
//                System.out.print((i+1)*5 + " \t\t\t\t");
//            }
//            System.out.println("\nExporting...");
//            GeoToolsImporter.removeAndExport(removeGeometry);
    }

    /**
     * Reads in from a vector_field and adds to SparseGrid2d and the bag supplied
     * @param vector_field
     * @param grid Grid to fill up
     * @param addTo
     * @param type Should be a subclass of Structure, if none is given, then it just uses a generic structure
     */
    static void readInStructures(GeomVectorField vector_field, SparseGrid2D grid, Bag addTo, Structure type)
    {
        Bag school_geom = vector_field.getGeometries();

        Envelope e = vector_field.getMBR();
        double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e.getMaxY();
        int xcols = ebolaSim.world_width - 1, ycols = ebolaSim.world_height - 1;
        //System.out.println("Number of schools = " + school_geom.size());
        for(Object o: school_geom)
        {
            MasonGeometry school = (MasonGeometry)o;
            Point point = vector_field.getGeometryLocation(school);
            double x = point.getX(), y = point.getY();
            int xint = (int) Math.floor(xcols * (x - xmin) / (xmax - xmin)), yint = (int) (ycols - Math.floor(ycols * (y - ymin) / (ymax - ymin))); // REMEMBER TO FLIP THE Y VALUE
            Structure structure;
            if(type instanceof School)
            {
                structure = new School(new Int2D(xint, yint));
                ebolaSim.allWorkLocations.add((WorkLocation)structure);
            }
            else if(type instanceof WorkLocation)
            {
                structure = new WorkLocation(new Int2D(xint, yint), ((WorkLocation) type).getSector_id());
                ebolaSim.allWorkLocations.add((WorkLocation)structure);

//                if(structure.getCapacity() < farmDistanceFrequency.length)
//                    farmDistanceFrequency[structure.getCapacity()]++;
//                else
//                    farmDistanceFrequency[farmDistanceFrequency.length-1]++;
            }
            else
                structure = new Structure(new Int2D(xint, yint));
            addTo.add(structure);
            //System.out.println("(" + xint + ", " + yint + ")");
            grid.setObjectLocation(structure, xint, yint);
        }
    }

    /**
     * Function will assign each structure in the SparseGrid a nearest node on the road network found in allRoadNodes at sim state.
     * @param grid a sparsegrid that contains strctures.
     */
    static void assignNearestNode(SparseGrid2D grid, Map<Node, List<Structure>> nodeStructureMap)
    {
        double max_distance = 0;
        int count = 0;
        double sum = 0;
        Bag objects  = grid.getAllObjects();
        for(Object o: objects)
        {
            Structure structure = (Structure)o;
            Node node = getNearestNode(structure.getLocation().getX(), structure.getLocation().getY());
            if(node != null)
            {
                structure.setNearestNode(node);

                //create a node on the road network that connects this structure to the road network
                Node newNode = new Node(structure.location);
                Edge e = new Edge(newNode, node, (int)newNode.location.distance(node.location));
                newNode.links.add(e);
                node.links.add(e);
                structure.setNearestNode(newNode);
                addToListInMap(nodeStructureMap, newNode, structure);

                double distance = structure.getLocation().distance(node.location);
                distance *= (Parameters.POP_BLOCK_METERS/Parameters.WORLD_TO_POP_SCALE)/1000.0;
                if(distance > max_distance)
                    max_distance = distance;
                sum += distance;
                count++;
            }
        }
        for(int i = 0; i < frequency.length; i++)
        {
            System.out.print(frequency[i] + "\t\t");
        }
        System.out.println("\nAverage distance = " + sum/count + " km");
        System.out.println("Max distance household to node = " + max_distance + " kilometers");
    }

    private static void addToListInMap(Map map, Object key, Object value)
    {
        List list;
        if(!map.containsKey(key))
            map.put(key, new LinkedList<Structure>());
        list = (List)map.get(key);
        list.add(value);
    }

    private static int[] frequency = new int[300];
    /**
     *
     * @param x source x coordinate
     * @param y source y coordinate
     * @return Road node nearest to the x, y coordinates
     */
    static Node getNearestNode(int x, int y)
    {
        int cX = x;
        int cY = y;

        while(ebolaSim.road_cost.get(cX, cY) != 0)
        {
            DoubleBag val = new DoubleBag();
            IntBag xBag = new IntBag();
            IntBag yBag = new IntBag();

            ebolaSim.road_cost.getRadialNeighbors(cX, cY, 1, Grid2D.BOUNDED, true, val, xBag, yBag);
            double min = Double.MAX_VALUE;
            int index = 0;
            for (int i = 0; i < val.size(); i++)
                if (val.get(i) < min)
                {
                    min = val.get(i);
                    index = i;
                }
            cY = yBag.get(index);
            cX = xBag.get(index);
        }

        Bag nodes = ebolaSim.allRoadNodes.getObjectsAtLocation(cX, cY);
        Bag val = new Bag();
        IntBag xBag = new IntBag();
        IntBag yBag = new IntBag();

        if(nodes == null || nodes.isEmpty())
        {
            for(int i = 1; i < 300; i++)
            {
                ebolaSim.allRoadNodes.getRadialNeighbors(cX, cY, i, Grid2D.BOUNDED, true, val, xBag, yBag);
                if(val != null && !val.isEmpty())
                {
                    frequency[i]++;
                    //System.out.println("Radial neihghbor found!! at " + i);
                    return (Node)val.get(0);
                }
            }

            System.out.println("NO NODE NEARBY!!!!!!!!!!!!!");
            frequency[9]++;
            return new Node(new Int2D(cX, cY));
        }
        else
        {
            frequency[0]++;
            //System.out.println("On a NODE!!");
        }

        return (Node)nodes.get(0);

    }

    static void extractFromRoadLinks(GeomVectorField roadLinks, EbolaABM ebolaSim)
    {
        Bag geoms = roadLinks.getGeometries();
        Envelope e = roadLinks.getMBR();
        double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e.getMaxY();
        int xcols = ebolaSim.world_width - 1, ycols = ebolaSim.world_height - 1;
        int count = 0;

        //allNetworks = new LinkedList<HashSet<LineString>>();

        // extract each edge
        for (Object o : geoms)
        {
            MasonGeometry gm = (MasonGeometry) o;
            if (gm.getGeometry() instanceof LineString)
            {
                count++;
                readLineString((LineString) gm.getGeometry(), xcols, ycols, xmin, ymin, xmax, ymax, ebolaSim);

            } else if (gm.getGeometry() instanceof MultiLineString)
            {
                MultiLineString mls = (MultiLineString) gm.getGeometry();
                for (int i = 0; i < mls.getNumGeometries(); i++)
                {
                    count++;
                    readLineString((LineString) mls.getGeometryN(i), xcols, ycols, xmin, ymin, xmax, ymax, ebolaSim);
                }
            }
            if(count%10000 == 0)
                System.out.println("# of linestrings = " + count);

        }

    }

//    /**
//     * Converts an individual linestring into a series of links and nodes in the
//     * network
//     * int width, int height, Dadaab dadaab
//     * @param geometry
//     * @param xcols - number of columns in the field
//     * @param ycols - number of rows in the field
//     * @param xmin - minimum x value in shapefile
//     * @param ymin - minimum y value in shapefile
//     * @param xmax - maximum x value in shapefile
//     * @param ymax - maximum y value in shapefile
//     */
//    static void readLineString(LineString geometry, int xcols, int ycols, double xmin,
//                               double ymin, double xmax, double ymax, EbolaABM ebolaSim)
//    {
//
//        CoordinateSequence cs = geometry.getCoordinateSequence();
//
//        // iterate over each pair of coordinates and establish a link between
//        // them
//        Node oldNode = null; // used to keep track of the last node referenced
//        for (int i = 0; i < cs.size(); i++) {
//
//            // calculate the location of the node in question
//            double x = cs.getX(i), y = cs.getY(i);
//            int xint = (int) Math.floor(xcols * (x - xmin) / (xmax - xmin)), yint = (int) (ycols - Math.floor(ycols * (y - ymin) / (ymax - ymin))); // REMEMBER TO FLIP THE Y VALUE
//
//            if (xint >= ebolaSim.world_width) {
//                continue;
//            } else if (yint >= ebolaSim.world_height) {
//                continue;
//            }
//
//            // find that node or establish it if it doesn't yet exist
//            Bag ns = ebolaSim.allRoadNodes.getObjectsAtLocation(xint, yint);
//            Node n;
//            if (ns == null) {
//                n = new Node(new Location(xint, yint));
//                ebolaSim.allRoadNodes.setObjectLocation(n, xint, yint);
//            } else {
//                n = (Node) ns.get(0);
//            }
//
//            if (oldNode == n) // don't link a node to itself
//            {
//                continue;
//            }
//
//            // attach the node to the previous node in the chain (or continue if
//            // this is the first node in the chain of links)
//
//            if (i == 0) { // can't connect previous link to anything
//                oldNode = n; // save this node for reference in the next link
//                continue;
//            }
//
//            int weight = (int) n.location.distanceTo(oldNode.location); // weight is just
//            // distance
//
//            // create the new link and save it
//            Edge e = new Edge(oldNode, n, weight);
//            ebolaSim.roadNetwork.addEdge(e);
//            oldNode.links.add(e);
//            n.links.add(e);
//
//            oldNode = n; // save this node for reference in the next link
//        }
//    }

    /**
     * Converts an individual linestring into a series of links and allRoadNodes in the
     * network
     * int width, int height, Dadaab dadaab
     * @param geometry
     * @param xcols - number of columns in the field
     * @param ycols - number of rows in the field
     * @param xmin - minimum x value in shapefile
     * @param ymin - minimum y value in shapefile
     * @param xmax - maximum x value in shapefile
     * @param ymax - maximum y value in shapefile
     */
    static void readLineString(LineString geometry, int xcols, int ycols, double xmin,
                               double ymin, double xmax, double ymax, EbolaABM ebolaSim) {

        CoordinateSequence cs = geometry.getCoordinateSequence();
        // iterate over each pair of coordinates and establish a link between
        // them

        if(!allLineStrings.add(geometry)) //Uncomment for linestring trimming
            return;

        //linestring trimming: HashSet<LineString> curSet = new HashSet<LineString>();
        //curSet.add(geometry);
        //allNetworks.addFirst(curSet);
//        ListIterator<HashSet<LineString>> listIterator = allNetworks.listIterator();
//        listIterator.next();
//        int removeIndex = 0;
        Node oldNode = null; // used to keep track of the last node referenced
        Node oldNodeTrimmed = null; //used to keep track of last trimmed node referenced
        int trimmed_distance = 0;
        for (int i = 0; i < cs.size(); i++)
        {
            // calculate the location of the node in question
            double x = cs.getX(i), y = cs.getY(i);
            int xint = (int) Math.floor(xcols * (x - xmin) / (xmax - xmin)), yint = (int) (ycols - Math.floor(ycols * (y - ymin) / (ymax - ymin))); // REMEMBER TO FLIP THE Y VALUE

            if (xint >= ebolaSim.world_width)
                continue;
            else if (yint >= ebolaSim.world_height)
                continue;

            // find that node or establish it if it doesn't yet exist
            Bag ns = ebolaSim.allRoadNodes.getObjectsAtLocation(xint, yint);
            Node n;
            if (ns == null)
            {
                n = new Node(new Int2D(xint, yint));
                n.lineStrings.add(geometry);
                n.index = i;
                ebolaSim.allRoadNodes.setObjectLocation(n, xint, yint);
            }
            else //this means that we are connected to another linestring or this linestring
            {
                n = (Node) ns.get(0);
                //USE FOR NETWORK COLLAPSE
//                LineString searchFor = n.lineString;
//                ListIterator<HashSet<LineString>> nextIterator = allNetworks.listIterator();
//                //search for the other linestring
//                int temp = -1;
//                while(nextIterator.hasNext())
//                {
//                    HashSet<LineString> next = nextIterator.next();
//                    temp++;
//                    if(next.contains(searchFor))
//                    {
//                        if(next != curSet)
//                        {
//                            //add all from the previous hashset to this one
//                            next.addAll(curSet);
//                            curSet = next;
//
//                            //remove the earlier position
//                            //listIterator.remove();
//                            if(removeIndex != 0) {
//                                int john = 1;
//                                john++;
//                            }
//                            allNetworks.remove(removeIndex);
//                            if(removeIndex < temp)
//                                temp--;
//                            removeIndex = temp;
//                            //now reset the position of the iterator and change locations
//                            //removeIndex = nextIterator.nextIndex();
//
//                            if(removeIndex < 0 || !allNetworks.get(removeIndex).contains(geometry))
//                                System.out.println("ERROR ERROR ERROR ERROR!!!!!!!!!!!!!!!");
//                        }
//                        break;
//                    }
//                }
            }

            // attach the node to the previous node in the chain (or continue if
            // this is the first node in the chain of links)

            if (i == 0) { // can't connect previous link to anything
                oldNode = n; // save this node for reference in the next link
                continue;
            }

            int weight = (int) n.location.distance(oldNode.location); // weight is just
            // distance
            //add it to the thinned network if it is the first or last in the cs.

            if (oldNode == n) // don't link a node to itself
            {
                continue;
            }

            // create the new link and save it
            Edge e = new Edge(oldNode, n, weight);
            ebolaSim.roadNetwork.addEdge(e);

            oldNode.links.add(e);
            n.links.add(e);
            n.weightOnLineString = trimmed_distance;
            oldNode = n; // save this node for reference in the next link
        }

        //if we haven't found any links the network should be null

    }

    private static void setUpAgeDist(String age_dist_file)
    {
        try
        {
            // buffer reader for age distribution data
            CSVReader csvReader = new CSVReader(new FileReader(new File(age_dist_file)));
            csvReader.readLine();//skip the headers
            List<String> line = csvReader.readLine();
            while(!line.isEmpty())
            {
                //read in the county ids
                int county_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(0)).intValue();
                //relevant info is from 5 - 21
                ArrayList<Double> list = new ArrayList<Double>();
                //double sum = 0;
                for(int i = 5; i <= 21; i++)
                {
                    list.add(Double.parseDouble(line.get(i)));
                    //sum += Double.parseDouble(line.get(i));
                    //Use cumalitive probability
                    if(i != 5)
                        list.set(i-5, list.get(i-5) + list.get(i-5 - 1));
                    //System.out.print(list.get(i-5));
                }
                //System.out.println("sum = " + sum);
                //System.out.println();
                //now add it to the hashmap
                age_dist.put(county_id, list);
                line = csvReader.readLine();
            }
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch(java.text.ParseException e)
        {
            e.printStackTrace();
        }
    }

    private static void addHousesAndResidents(String pop_file, String admin_file)
    {
        try
        {
            System.out.print("Adding houses ");
            long time = System.currentTimeMillis();
            // buffer reader - read ascii file for population data
            BufferedReader pop_reader = new BufferedReader(new InputStreamReader(EbolaData.class.getResourceAsStream(pop_file)));
            String pop_line;

            //buffer reader - read ascii file for admin data
            BufferedReader admin_reader = new BufferedReader(new InputStreamReader(EbolaData.class.getResourceAsStream(admin_file)));
            String admin_line;
            String[] admin_tokens;

            // first read the dimensions - admin and pop should be same height and width
            pop_line = pop_reader.readLine(); // read line for width
            String[] curr_tokens = pop_line.split("\\s+");
            String[] prev_tokens = null;
            String[] next_tokens = null;
            int width = Integer.parseInt(curr_tokens[1]);
            ebolaSim.pop_width = width;
            ebolaSim.world_width = width*Parameters.WORLD_TO_POP_SCALE;

            pop_line = pop_reader.readLine();
            curr_tokens = pop_line.split("\\s+");
            int height = Integer.parseInt(curr_tokens[1]);
            ebolaSim.pop_height = height;
            ebolaSim.world_height = height*Parameters.WORLD_TO_POP_SCALE;

            //instantiate world to hold agents
            ebolaSim.world = new Continuous2D(Parameters.WORLD_DISCRETIZTION, width*Parameters.WORLD_TO_POP_SCALE, height*Parameters.WORLD_TO_POP_SCALE);
            //instantiate grid to hold houses
            ebolaSim.householdGrid = new SparseGrid2D((int)(width*Parameters.WORLD_TO_POP_SCALE), (int)(height*Parameters.WORLD_TO_POP_SCALE));
            ebolaSim.urbanAreasGrid = new SparseGrid2D((int)(width), (int)(height));
            ebolaSim.worldPopResolution = new SparseGrid2D((int)(width), (int)(height));
            ebolaSim.hotSpotsGrid = new SparseGrid2D((int)width, (int)height);

            for(int i = 0; i < 4; i++)//skip the next couple of lines (contain useless metadata)
                pop_reader.readLine();

            for(int i = 0; i < 6; i++)//skip useless metadata
                admin_reader.readLine();

            pop_line = pop_reader.readLine();
            pop_line = pop_line.substring(1);
            curr_tokens = pop_line.split("\\s+");
            //System.out.println(curr_tokens.length);
            for(int i = 0; i < height; i++)
            {
                if(i != 0)//store three lines at a time so you can check surrounding cells
                {
                    prev_tokens = curr_tokens;
                    curr_tokens = next_tokens;
                }

                if(i != width - 1)
                {
                    pop_line = pop_reader.readLine();
                    pop_line = pop_line.substring(1);
                    next_tokens = pop_line.split("\\s+");
                }

                admin_line = admin_reader.readLine();
                admin_tokens = admin_line.split("\\s+");
                //iterate over the row
                for(int j = 0; j < curr_tokens.length; j++)
                {
                    //number of people within this row
                    int num_people = Integer.parseInt(curr_tokens[j]);
                    if(num_people > 0)
                    {
                        ebolaSim.total_pop += num_people;

                        int scaled_num_people = scale(num_people, Parameters.SCALE);//Scale the number of agents to reduce size of simulation

                        ebolaSim.total_scaled_pop += scaled_num_people;

                        //determine current country
                        int country = determineCountry(Integer.parseInt(admin_tokens[j]));
                        //county id, counties/provinces within each country
                        int county_id = Integer.parseInt(admin_tokens[j]);

                        //add up total pop stats for later
                        if(country == Parameters.GUINEA)
                            ebolaSim.total_guinea_pop += num_people;
                        else if(country == Parameters.LIBERIA)
                            ebolaSim.total_lib_pop += num_people;
                        else
                            ebolaSim.total_sl_pop += num_people;

                        boolean isUrban = false;
                        if(num_people > Parameters.MIN_POP_URBAN || nearbyUrban(prev_tokens, curr_tokens, next_tokens, i, j))//determine if location is urban
                        {
                            ebolaSim.urbanAreasGrid.setObjectLocation(new Object(), j, i);
                            ebolaSim.total_urban_pop += num_people;
                            ebolaSim.total_scaled_urban_pop += scaled_num_people;
                            isUrban = true;
                            if(country == Parameters.GUINEA)
                                ebolaSim.guinea_urban_pop += num_people;
                            else if(country == Parameters.LIBERIA)
                                ebolaSim.lib_urban_pop += num_people;
                            else
                                ebolaSim.sl_urban_pop += num_people;
                        }
                        else
                            ebolaSim.total_scaled_rural_pop += num_people;

                        //add urban center to maps
                        if(isUrban)
                        {
                            if(country == Parameters.GUINEA)
                            {
                                List list;
                                if(!ebolaSim.admin_id_gin_urban.containsKey(ebolaSim.admin_id.get(j, i)))
                                    ebolaSim.admin_id_gin_urban.put(ebolaSim.admin_id.get(j, i), new LinkedList<Int2D>());
                                list = ebolaSim.admin_id_gin_urban.get(ebolaSim.admin_id.get(j, i));
                                list.add(new Int2D(j, i));
                            }
                            else if(country == Parameters.LIBERIA)
                            {
                                List list;
                                if(!ebolaSim.admin_id_lib_urban.containsKey(ebolaSim.admin_id.get(j, i)))
                                    ebolaSim.admin_id_lib_urban.put(ebolaSim.admin_id.get(j, i), new LinkedList<Int2D>());
                                list = ebolaSim.admin_id_lib_urban.get(ebolaSim.admin_id.get(j, i));
                                list.add(new Int2D(j, i));
                            }
                            else
                            {
                                List list;
                                if(!ebolaSim.admin_id_sle_urban.containsKey(ebolaSim.admin_id.get(j, i)))
                                    ebolaSim.admin_id_sle_urban.put(ebolaSim.admin_id.get(j, i), new LinkedList<Int2D>());
                                list = ebolaSim.admin_id_sle_urban.get(ebolaSim.admin_id.get(j, i));
                                list.add(new Int2D(j, i));
                            }
                        }

                        //iterate over each house
                        while(scaled_num_people > 0)
                        {
                            int x_coord, y_coord;
                            //randomly pick a space within the square kilometer
                            do
                            {
                                y_coord = (i*Parameters.WORLD_TO_POP_SCALE) + (int)(ebolaSim.random.nextDouble() * Parameters.WORLD_TO_POP_SCALE);
                                x_coord = (j*Parameters.WORLD_TO_POP_SCALE) + (int)(ebolaSim.random.nextDouble() * Parameters.WORLD_TO_POP_SCALE);

                            } while (false);//ebolaSim.householdGrid.getObjectsAtLocation(x_coord, y_coord) != null);
                            Household h = new Household(new Int2D(x_coord, y_coord));
                            h.setCountry(country);
                            h.setNearestNode(getNearestNode(h.getLocation().getX(), h.getLocation().getY()));//give it a nearest node
                            h.setAdmin_id(ebolaSim.admin_id.get(j, i));
                            //addNearestNode to the network
                            Node newNode = new Node(h.location);
                            Edge e = new Edge(newNode, h.getNearestNode(), (int)newNode.location.distance(h.getNearestNode().location));
                            newNode.links.add(e);
                            h.getNearestNode().links.add(e);
                            h.setNearestNode(newNode);

                            ebolaSim.householdGrid.setObjectLocation(h, new Int2D(x_coord, y_coord));
                            addToListInMap(ebolaSim.householdNodes, h.getNearestNode(), h);

                            int household_size  = pickHouseholdSize(country);//use log normal distribution to pick correct household size

                            //get nearest school
                            //School nearest_school = (School)getNearestStructureByRoute(h, ebolaSim.schoolNodes);//getNearestSchool(h.getLocation().getX(), h.getLocation().getY());

                            //add members to the household
                            for(int m = 0; m < household_size; m++)
                            {
//                                if(scaled_num_people == 0)
//                                    break;
                                scaled_num_people--;
                                Resident r = createResident(new Int2D(x_coord, y_coord), h, isUrban, county_id);
                                ebolaSim.schedule.scheduleRepeating(r);
                                r.setPop_density(scaled_num_people);

                                //used for movement flow
                                if(country == Parameters.SL)
                                {
                                    Bag residents;
                                    if(!ebolaSim.admin_id_sle_residents.containsKey(r.getHousehold().getAdmin_id()))
                                        residents = ebolaSim.admin_id_sle_residents.put(r.getHousehold().getAdmin_id(), new Bag());
                                    residents = ebolaSim.admin_id_sle_residents.get(r.getHousehold().getAdmin_id());
                                    residents.add(r);
                                }
                                else if(country == Parameters.GUINEA)
                                {
                                    Bag residents;
                                    if(!ebolaSim.admin_id_gin_residents.containsKey(r.getHousehold().getAdmin_id()))
                                        residents = ebolaSim.admin_id_gin_residents.put(r.getHousehold().getAdmin_id(), new Bag());
                                    residents = ebolaSim.admin_id_gin_residents.get(r.getHousehold().getAdmin_id());
                                    residents.add(r);
                                }
                                else if(country == Parameters.LIBERIA)
                                {
                                    Bag residents;
                                    if(!ebolaSim.admin_id_lib_residents.containsKey(r.getHousehold().getAdmin_id()))
                                        residents = ebolaSim.admin_id_lib_residents.put(r.getHousehold().getAdmin_id(), new Bag());
                                    residents = ebolaSim.admin_id_lib_residents.get(r.getHousehold().getAdmin_id());
                                    residents.add(r);
                                }
//                                if(nearest_school != null)
//                                    r.setNearestSchool(nearest_school);
//                                else
//                                    System.out.println("SCHOOL IS NULL!!");
//                                if(r.getAge() >= 5 && r.getAge() <= 14 && nearest_school != null)
//                                {
//                                    nearest_school.addMember(r);
//                                }
                                ebolaSim.worldPopResolution.setObjectLocation(r, j, i);
                                ebolaSim.world.setObjectLocation(r, new Double2D(x_coord, y_coord));
                                h.addMember(r);//add the member to the houshold
                            }
                        }
                    }
                }
            }
            System.out.println("[" + (System.currentTimeMillis()-time)/1000 + " secs]");
            System.out.println("total scaled pop = " + ebolaSim.total_scaled_pop);
            System.out.println("total pop = " + ebolaSim.total_pop);
            System.out.println("expected scaled pop = " + ebolaSim.total_pop*1.0*Parameters.SCALE);
            System.out.println("total_urban_pop = " + ebolaSim.total_urban_pop);
            System.out.println("total_rural_pop = " + (ebolaSim.total_pop - ebolaSim.total_urban_pop));
            System.out.println("sierra_leone urban percentage = " + ebolaSim.sl_urban_pop*1.0/ebolaSim.total_sl_pop);
            System.out.println("liberia urban percentage = " + ebolaSim.lib_urban_pop*1.0/ebolaSim.total_lib_pop);
            System.out.println("guinea urban percentage = " + ebolaSim.guinea_urban_pop*1.0/ebolaSim.total_guinea_pop);
            System.out.println("no school count = " + ebolaSim.no_school_count);
            System.out.println("average distance = " + ebolaSim.distance_sum/ebolaSim.distance_count);
            System.out.println("max distance = " + ebolaSim.max_distance);
            int sum = 0;
            int count = 0;
            int max_size = 0;
            for(Object o: ebolaSim.schools)
            {
                School school = (School)o;
                sum += school.getMembers().size();
                count++;
                if(school.getMembers().size() > max_size)
                    max_size = school.getMembers().size();
            }
            System.out.println("average school population = " + sum*1.0/count);
            System.out.println("max school pop = " + max_size);

            //print distribution of distance to farm
            Bag allResidents = ebolaSim.world.getAllObjects();
            int go_nowhere = 0;
            int employed = 0;
            for(Object o: allResidents)
            {
                Resident resident = (Resident)o;
                Route route = AStar.getNearestNode(resident.getHousehold().getNearestNode(), ebolaSim.placesNodes, Parameters.convertFromKilometers(100), false, Parameters.WALKING_SPEED);
                double distance;// = 50;
                if(route != null)
                {
                    distance = route.getTotalDistance();
                    if(Math.round(distance) < farmDistanceFrequency.length)
                        farmDistanceFrequency[(int)Math.round(distance)]++;
                }

                //stats for printing
                if(resident.isEmployed() && resident.getWorkDayDestination() == null)
                    go_nowhere++;
                if(resident.isEmployed())
                    employed++;

            }
            System.out.println("Percent of employed people who are at a bad node = " + go_nowhere*1.0/employed*100);
            for(int i = 0; i < farmDistanceFrequency.length; i++)
            {
                ebolaSim.distribution.addValue((Number)(farmDistanceFrequency[i] * 1.0 / ebolaSim.total_scaled_pop), "All distances", i);
                if((farmDistanceFrequency[i] * 1.0 / ebolaSim.total_scaled_pop * 100) > ebolaSim.max)
                    ebolaSim.max = (farmDistanceFrequency[i] * 1.0 / ebolaSim.total_scaled_pop * 100);
                System.out.print(farmDistanceFrequency[i] * 1.0 * 1.0 / ebolaSim.total_scaled_pop * 100 + "%" + "\t\t");
            }
            System.out.println("");
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private static Resident createResident(Int2D location, Household household, boolean isUrban, int county_id)
    {
        //first pick sex
        int sex;
        if(ebolaSim.random.nextBoolean())
            sex = Constants.MALE;
        else
            sex = Constants.FEMALE;

        //now get age
        int age = pick_age(age_dist, county_id);

        Resident resident = new Resident(location, household, sex, age, isUrban);

        if(age < 5)//if so young must be inactive and just stay home
            resident.setInactive(true);
        else if(isUrban)//Urban
        {
            if(sex == Constants.MALE)//urban male
            {
                setWorkDemographics(resident, Parameters.URBAN_MALE_LF_BY_AGE, Parameters.URBAN_MALE_INACTIVE_SCHOOL, Parameters.URBAN_MALE_UNEMPLOYMENT, Parameters.URBAN_MALE_SECTORS);
                //setDailyWorkHours(resident, Parameters.MALE_WEEKLY_HOURS_BY_SECTOR);
                if(resident.isEmployed())
                    ebolaSim.urban_male_employed++;
            }
            else//urban female
            {
                setWorkDemographics(resident, Parameters.URBAN_FEMALE_LF_BY_AGE, Parameters.URBAN_FEMALE_INACTIVE_SCHOOL, Parameters.URBAN_FEMALE_UNEMPLOYMENT, Parameters.URBAN_FEMALE_SECTORS);
                //setDailyWorkHours(resident, Parameters.FEMALE_WEEKLY_HOURS_BY_SECTOR);
                if(resident.isEmployed())
                    ebolaSim.urban_female_employed++;
            }
        }
        else//rural
        {
            if(sex == Constants.MALE)//rural male
            {
                setWorkDemographics(resident, Parameters.RURAL_MALE_LF_BY_AGE, Parameters.RURAL_MALE_INACTIVE_SCHOOL, Parameters.RURAL_MALE_UNEMPLOYMENT, Parameters.RURAL_MALE_SECTORS);
                //setDailyWorkHours(resident, Parameters.MALE_WEEKLY_HOURS_BY_SECTOR);
                if(resident.isEmployed())
                    ebolaSim.rural_male_employed++;
            }
            else//rural female
            {
                setWorkDemographics(resident, Parameters.RURAL_FEMALE_LF_BY_AGE, Parameters.RURAL_FEMALE_INACTIVE_SCHOOL, Parameters.RURAL_FEMALE_UNEMPLOYMENT, Parameters.RURAL_FEMALE_SECTORS);
                //setDailyWorkHours(resident, Parameters.FEMALE_WEEKLY_HOURS_BY_SECTOR);
                if(resident.isEmployed())
                    ebolaSim.rural_female_employed++;
            }
        }

        //now we have to set the daily destination
        //don't set work destination because that will be done later
//        if(resident.isEmployed())
//            setWorkDestination(resident);

        return resident;
    }

    /**
     * Modifies the resident to set inactivity, unemployment
     * @param resident Resident to be modified
     * @param labour_force_by_age Percent participating in labour force by age.  All not in the labour force are considered inactive.
     * @param inactive_school Percent that are inactive because they go to school.
     * @param unemployment Percent unemployed by age
     * @param economic_sectors Distribution in each economic sector by age.
     */
    private static void setWorkDemographics(Resident resident, double[] labour_force_by_age, double[] inactive_school, double[] unemployment, double[] economic_sectors)
    {
        boolean inactive;
        int age_index = (resident.getAge()-5)/5;//subtract five because first age group (0-4) is not included
        double rand = ebolaSim.random.nextDouble();

        double inactive_chance = 1-labour_force_by_age[age_index];
        inactive = rand < inactive_chance;
        resident.setInactive(inactive);

        if(resident.isInactive())
        {
            //now determine if he stays at school or not
            rand = ebolaSim.random.nextDouble();
            int index = (resident.getAge()-5)/10;
            if(index >= 5)//it combines one group for 20 years
                index = 4;
            else if(index == 4)
                index = 3;
            if(rand < inactive_school[index])
            {
                Structure nearestSchool  = getNearestStructureByRoute(resident.getHousehold(), ebolaSim.workNodeStructureMap.get(Constants.EDUCATION), Double.MAX_VALUE, false);
                resident.setWorkDayDestination(nearestSchool);//add school as workday destination
            }
            else
                resident.setWorkDayDestination(resident.getHousehold());//if you don't go to school you pretty much just stay home doing nothing (retired or something)
        }
        else//resident is a part of the labour force
        {
            //decide if employed or not
            if(resident.getAge() < 15)//different statistics for 15+ and <15//TODO Incoroporate child labour
            {

            }
            else
            {
                rand = ebolaSim.random.nextDouble();
                int index = (resident.getAge()-5)/10;
                if(index >= 5)//it combines one group for 20 years
                    index = 4;
                else if(index == 4)
                    index = 3;
                if(rand < unemployment[index])//unemployed
                {
                    resident.setEmployed(false);
                }
                else//employment
                {
                    resident.setEmployed(true);
                    //now decide what economic sector
                    //setSectorId(resident, economic_sectors);//COMMENT FOR FILLING WORKLOCATION TO CAPCITY
                }
            }
        }
    }

    private static void setSectorId(Resident resident, double[] economic_sectors)
    {
        double rand = ebolaSim.random.nextDouble();
        double sum = 0;
        for(int i = 0; i < economic_sectors.length; i++)
        {
            sum += economic_sectors[i];
            if(rand < sum)//set the sector
            {
                resident.setSector_id(i);
                return;
            }
        }
        System.out.println("No sector id fits!!!!");
    }

    private static void setDailyWorkHours(Resident resident, int sector_id, double[][] weekly_hours_by_sector)
    {
        if(resident.isInactive())
        {
            resident.setDailyWorkHours(Parameters.STUDENT_DAILY_HOURS);
            return;
        }
        if(!resident.isEmployed())
            return;
        int sector = resident.getSector_id();
        double rand = ebolaSim.random.nextDouble();
        double sum = 0;
        for(int i = 0; i < weekly_hours_by_sector[sector].length; i++)
        {
            sum += weekly_hours_by_sector[sector][i];
            if(rand < sum)
            {
                //found the right hours, now convert the index to hours
                //TODO use guassian distribution
                double hours;
                if(i == 0)// < 25 hours
                    hours = 20;
                else if(i == 1)
                    hours = ebolaSim.random.nextInt(10) + 25;
                else if(i == 2)
                    hours = ebolaSim.random.nextInt(5) + 35;
                else if(i == 3)
                    hours = ebolaSim.random.nextInt(9) + 40;
                else if(i == 4)
                    hours = ebolaSim.random.nextInt(11) + 49;
                else
                    hours = 65;
                //change hours to daily
                hours /= 5.0;
                resident.setDailyWorkHours((int)Math.round(hours));
            }
        }
    }

    private static int[] farmDistanceFrequency = new int[101];

    /**
     * Sets teh workday destination for a resident.  If agent is at an isolated node, workdayDestination is set to null
     * @param resident
     */
    public static void setWorkDestination(Resident resident)
    {
        double max_distance = Stats.normalToLognormal(Stats.calcLognormalMu(Parameters.AVERAGE_FARM_MAX, Parameters.STDEV_FARM_MAX), Stats.calcLognormalSigma(Parameters.AVERAGE_FARM_MAX, Parameters.STDEV_FARM_MAX), ebolaSim.random.nextGaussian());
        max_distance = Parameters.convertFromKilometers(max_distance);//convert back to world units
        WorkLocation workLocation = (WorkLocation)getNearestStructureByRoute(resident.getHousehold(), ebolaSim.workNodeStructureMap.get(resident.getSector_id()), max_distance, true);
        resident.setWorkDayDestination(workLocation);
        if(workLocation != null)
        {
            workLocation.addMember(resident);
            Route routeToWork = resident.getHousehold().getRoute(workLocation, Parameters.WALKING_SPEED);
//                int distance = (int)Math.round(Parameters.convertToKilometers(routeToWork.getTotalDistance()));
//                if(distance < farmDistanceFrequency.length)
//                    farmDistanceFrequency[distance]++;
//                else
//                    farmDistanceFrequency[farmDistanceFrequency.length-1]++;
        }
        else
        {
            //could not find a nearby farm, now create one
            double farm_commute_on_road = Stats.normalToLognormal(Stats.calcLognormalMu(Parameters.AVERAGE_FARM_DISTANCE, Parameters.STDEV_FARM_DISTANCE), Stats.calcLognormalSigma(Parameters.AVERAGE_FARM_DISTANCE, Parameters.STDEV_FARM_DISTANCE), ebolaSim.random.nextGaussian());
            //System.out.println("Looking for node " + farm_commute_on_road + " km away");
            farm_commute_on_road = Parameters.convertFromKilometers(farm_commute_on_road);//convert back to world units
            //System.out.println("converted back to km = " + Parameters.convertToKilometers(farm_commute_on_road));
            double farm_commute_off_road = Stats.normalToLognormal(Stats.calcLognormalMu(Parameters.OFF_ROAD_AVERAGE, Parameters.OFF_ROAD_STDEV), Stats.calcLognormalSigma(Parameters.OFF_ROAD_AVERAGE, Parameters.OFF_ROAD_STDEV), ebolaSim.random.nextGaussian());
            farm_commute_off_road = Parameters.convertFromKilometers(farm_commute_off_road);//convert back to world units

            workLocation = createWorkLocation(resident, farm_commute_on_road, farm_commute_off_road, ebolaSim.workNodeStructureMap.get(resident.getSector_id()), ebolaSim.farmGrid);
            if(workLocation != null)
            {
                resident.setWorkDayDestination(workLocation);
                workLocation.addMember(resident);
            }//TODO why are we getting zeroed out parameters?? look into null pointer exception
        }
    }

    private static void setWorkLocationsForAllResidents(Set<WorkLocation> existingLocations, Bag residents)
    {
        //let us start by filling up existing locations
        for(WorkLocation workLocation: existingLocations)
        {
            fillMembers(workLocation);
        }

        for(Object o: residents)
        {
            Resident resident = (Resident)o;
            if(resident.isEmployed() && resident.getWorkDayDestination() == null)
            {
                setSectorId(resident, resident.getIsUrban()?(resident.getSex() == Constants.MALE?Parameters.URBAN_MALE_SECTORS:Parameters.URBAN_FEMALE_SECTORS):(resident.getSex() == Constants.MALE?Parameters.RURAL_MALE_SECTORS:Parameters.RURAL_FEMALE_SECTORS));
                setWorkDestination(resident);
                if(resident.getWorkDayDestination() != null)
                    fillMembers((WorkLocation)resident.getWorkDayDestination());
            }
            setDailyWorkHours(resident, resident.getSector_id(), resident.getSex() == Constants.MALE?Parameters.MALE_WEEKLY_HOURS_BY_SECTOR:Parameters.FEMALE_WEEKLY_HOURS_BY_SECTOR);
        }
    }

    /**
     * Tries to fill up the worklocation to needed capacity.
     * Assumes agents and households have already been placed with age labour force particiaption, and employement assigned.
     * Sex should not be assigned to any agent during this process as it will be assigned here.
     * @param workLocation
     */
    private static void fillMembers(WorkLocation workLocation)
    {
        if(workLocation.getMembers().size() >= workLocation.getCapacity())
            return;//no need to add more members, capacity reached

        double max_distance = Stats.normalToLognormal(Stats.calcLognormalMu(Parameters.AVERAGE_FARM_MAX, Parameters.STDEV_FARM_MAX), Stats.calcLognormalSigma(Parameters.AVERAGE_FARM_MAX, Parameters.STDEV_FARM_MAX), ebolaSim.random.nextGaussian());
        max_distance = Parameters.convertFromKilometers(max_distance);//convert back to world units

        List<Node> nearByNodes = AStar.getNodesWithinDistance(workLocation.getNearestNode(), ebolaSim.householdNodes, max_distance, Parameters.WALKING_SPEED);
        ListIterator listIterator = nearByNodes.listIterator();

        while(listIterator.hasNext())
        {
            List<Household> households = ebolaSim.householdNodes.get(listIterator.next());
            for(Household household: households)
                for(Resident resident: household.getMembers())//at this point the resident is guarenteed to have all work demographics but not a workday destination
                    if(resident.isEmployed() && resident.getWorkDayDestination() == null)//only look at employed persons and people who have not already been assigned a place
                    {
                        double[] economic_sector_probabilities;
                        int total;
                        if(resident.getIsUrban())
                            if(resident.getSex() == Constants.MALE)
                            {
                                economic_sector_probabilities = Parameters.URBAN_MALE_SECTORS;
                                total = ebolaSim.urban_male_employed;
                            }
                            else
                            {
                                economic_sector_probabilities = Parameters.URBAN_FEMALE_SECTORS;
                                total = ebolaSim.urban_female_employed;
                            }
                        else
                        if(resident.getSex() == Constants.MALE)
                        {
                            economic_sector_probabilities = Parameters.RURAL_MALE_SECTORS;
                            total = ebolaSim.rural_male_employed;
                        }
                        else
                        {
                            economic_sector_probabilities = Parameters.RURAL_FEMALE_SECTORS;
                            total = ebolaSim.rural_female_employed;
                        }

                        //abort if probability is below zero
                        if(economic_sector_probabilities[workLocation.getSector_id()] < 0)
                            continue;
                        //first set their work location to this one
                        resident.setWorkDayDestination(workLocation);
                        resident.setSector_id(workLocation.getSector_id());

                        //at this point the resident should know if they are urban/rural and we need to set the sex
    //                    setSexBasedOnSector(resident, resident.getSector_id(), resident.getIsUrban() ? Parameters.URBAN_MALE_SECTORS : Parameters.RURAL_MALE_SECTORS,
    //                            resident.getIsUrban() ? Parameters.URBAN_FEMALE_SECTORS : Parameters.RURAL_FEMALE_SECTORS);

                        //now change the previous parameter values to reduce chance of getting this sector other areas.
                        reduceProbability(economic_sector_probabilities, workLocation.getSector_id(), total);

                        //add this resident to the workLocation
                        workLocation.addMember(resident);

                        //return if the workLocation is filled up
                        if(workLocation.getMembers().size() >= workLocation.getCapacity())
                            return;
                    }
        }
    }

    private static void setSexBasedOnSector(Resident resident, int sector_id, double[] maleBySectorId, double[] femaleBySectorId)
    {
        //we should know urban/rural
        //let's first decide if their sex (use Bayes Rule)
        double probability_male = (maleBySectorId[sector_id] * 0.5) / ((maleBySectorId[sector_id] * 0.5) + femaleBySectorId[sector_id] * 0.5);
        if(ebolaSim.random.nextDouble() < probability_male)
            resident.setSex(Constants.MALE);
        else
            resident.setSex(Constants.FEMALE);
    }

    private static void reduceProbability(double[] sectorProbabilities, int index, int total)
    {
        double newValue = ((sectorProbabilities[index]*total)-1)/(total-1);
        for(int i = 0; i < sectorProbabilities.length; i++)
            if(i == index)
                sectorProbabilities[i] = newValue;
            else
                sectorProbabilities[i] = ((sectorProbabilities[i]*total))/(total-1);
        if(sectorProbabilities[index] < 0)
            System.out.println("Sector Id dropped below zero for sector " + index);
    }

    private static WorkLocation createWorkLocation(Resident resident, double on_road_distance, double off_road_distance, Map<Node, List<Structure>> nodeStructureMap, SparseGrid2D grid)
    {
        Route route = AStar.getNodeAtDistance(resident.getHousehold().getNearestNode(), on_road_distance, Parameters.WALKING_SPEED);
        if(route != null)
        {
            Node tempEndNode = route.getEnd();
            Int2D endLocation = moveAwayFromRoad(tempEndNode.location, off_road_distance);
            route.addToEnd(endLocation);
            //create a new node at this location and connect the two
            Node endNode = new Node(endLocation);
            Edge e = new Edge(endNode, tempEndNode, (int)endNode.location.distance(tempEndNode.location));
            endNode.links.add(e);
            tempEndNode.links.add(e);

            //create the WorkLocation and set nearest node
            WorkLocation workLocation = new WorkLocation(endLocation, resident.getSector_id());
//            if(workLocation.getCapacity() < farmDistanceFrequency.length)
//                farmDistanceFrequency[workLocation.getCapacity()]++;
//            else
//                farmDistanceFrequency[farmDistanceFrequency.length-1]++;
            grid.setObjectLocation(workLocation, workLocation.getLocation());
            workLocation.setNearestNode(endNode);

            //add the node to the structure map so that it can be found
            addToListInMap(nodeStructureMap, endNode, workLocation);

            //cache the route
            resident.getHousehold().cacheRoute(route, workLocation);
            return workLocation;
        }
        return null;//nothing we can do if can't get a route
    }

    private static Int2D moveAwayFromRoad(Int2D location, double off_road_distance)
    {
        int cX = location.getX();
        int cY = location.getY();

        while(location.distance(cX, cY) <= off_road_distance)
        {
            //simply pick the largest one using road cost
            DoubleBag val = new DoubleBag();
            IntBag xBag = new IntBag();
            IntBag yBag = new IntBag();

            ebolaSim.road_cost.getRadialNeighbors(cX, cY, 1, Grid2D.BOUNDED, true, val, xBag, yBag);
            double max = Double.MIN_VALUE;
            int index = 0;
            for (int i = 0; i < val.size(); i++)
                if (val.get(i) > max)
                {
                    max = val.get(i);
                    index = i;
                }
            if(cX == xBag.get(index) && cY == yBag.get(index))//if no change then just return
                break;
            cY = yBag.get(index);
            cX = xBag.get(index);
        }
        return new Int2D(cX, cY);
    }

    /**
     * Reads in the movement patterns from csv file and populates given map
     * @param file
     */
    private static void setUpMovementMap(String file)
    {
        try
        {
            // buffer reader for age distribution data
            CSVReader csvReader = new CSVReader(new InputStreamReader(EbolaData.class.getResourceAsStream(file)));
            csvReader.readLine();//skip the headers
            List<String> line = csvReader.readLine();
            while(!line.isEmpty())
            {
                EbolaABM.MovementPattern mp = new EbolaABM.MovementPattern();

                String from = line.get(0);
                String to = line.get(1);

                int from_admin_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(from.substring(0, from.length()-3)).intValue();
                int from_country = convertCountryStringToInt(from.substring(from.length() - 3));

                int to_admin_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(from.substring(0, from.length()-3)).intValue();
                int to_country = convertCountryStringToInt(from.substring(from.length() - 3));

                double x = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(9)).intValue();
                double y = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(10)).intValue();
                Int2D location = convertToWorld(x, y);

                double amount = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(15)).intValue();

                mp.source_admin = from_admin_id;
                mp.to_admin = to_admin_id;
                mp.annual_amnt = amount;
                mp.destination = location;
                mp.source_country = from_country;
                mp.to_country = to_country;

                if(from_country == Parameters.SL)
                {
                    List list;
                    if(!ebolaSim.movementPatternMapSLE.containsKey(from_admin_id))
                        ebolaSim.movementPatternMapSLE.put(from_admin_id, new LinkedList<EbolaABM.MovementPattern>());
                    list = ebolaSim.movementPatternMapSLE.get(from_admin_id);
                    list.add(mp);
                }
                else if(from_country == Parameters.LIBERIA)
                {
                    List list;
                    if(!ebolaSim.movementPatternMapLIB.containsKey(from_admin_id))
                        ebolaSim.movementPatternMapLIB.put(from_admin_id, new LinkedList<EbolaABM.MovementPattern>());
                    list = ebolaSim.movementPatternMapLIB.get(from_admin_id);
                    list.add(mp);
                }
                else if(from_country == Parameters.GUINEA)
                {
                    List list;
                    if(!ebolaSim.movementPatternMapGIN.containsKey(from_admin_id))
                        ebolaSim.movementPatternMapGIN.put(from_admin_id, new LinkedList<EbolaABM.MovementPattern>());
                    list = ebolaSim.movementPatternMapGIN.get(from_admin_id);
                    list.add(mp);
                }

                line = csvReader.readLine();
            }
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch(java.text.ParseException e)
        {
            e.printStackTrace();
        }
    }

    private static int convertCountryStringToInt(String countryString)
    {
        if(countryString.equals("SLE"))
        {
            return Parameters.SL;
        }
        else if(countryString.equals("LBR"))
        {
            return Parameters.LIBERIA;
        }
        else if(countryString.equals("GIN"))
        {
            return Parameters.GUINEA;
        }
        System.out.println("ERROR ERROR countryString invalid");
        return -1;
    }

    /**
     * Takes lat/lng and converts it to a INT2d on the world
     * @param x
     * @param y
     * @return
     */
    public static Int2D convertToWorld(double x, double y)
    {
        Envelope e = ebolaSim.roadLinks.getMBR();
        double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e.getMaxY();
        int xcols = ebolaSim.world_width - 1, ycols = ebolaSim.world_height - 1;

        int xint = (int) Math.floor(xcols * (x - xmin) / (xmax - xmin)), yint = (int) (ycols - Math.floor(ycols * (y - ymin) / (ymax - ymin))); // REMEMBER TO FLIP THE Y VALUE
        return new Int2D(xint, yint);
    }

    private static Structure getNearestStructureByRoute(Structure start, Map<Node, List<Structure>> endNodes, double max_distance, boolean check_capacity)
    {
        //first check if route is cached TODO: Assumes that all cached paths are closest to the structure
        Iterator<Structure> it = start.getCachedRoutes().keySet().iterator();
        while(it.hasNext())
        {
            Structure st = it.next();
            if(endNodes.containsKey(st.getNearestNode()))
                return st;
        }

        Route route = AStar.getNearestNode(start.getNearestNode(), endNodes, max_distance, check_capacity, Parameters.WALKING_SPEED);

        if(route == null)
            return null;

        //cache the path with the end structure
        List<Structure> structureList = endNodes.get(route.getEnd());
        for(Structure destination: structureList)
            start.cacheRoute(route, destination);

        //pick a random structure to return for all that are at the same location
        return structureList.get(ebolaSim.random.nextInt(structureList.size()));
    }

    private static School getNearestSchool(int x, int y)
    {
        //find nearest school
        Bag schools = new Bag();

        for(int i = 3; i <= 54; i += 3)//increment the radius to lookup i in kilometers
        {
            int radius = (int)Math.round(i*1000/Parameters.POP_BLOCK_METERS*Parameters.WORLD_TO_POP_SCALE);//convert to grid units
            ebolaSim.schoolGrid.getRadialNeighborsAndLocations(x, y, radius, SparseGrid2D.BOUNDED, false, schools, null, null);
            if(schools.size() != 0)
                break;
        }

        School nearest_school = null;
        double min_distance = Double.MAX_VALUE;
        for(Object o: schools)
        {
            School school = (School)o;
            double distance = school.getLocation().distance(new Double2D(x, y));
            if(distance < min_distance)
            {
                min_distance = distance;
                nearest_school = school;
            }
        }
        min_distance *= (Parameters.POP_BLOCK_METERS/Parameters.WORLD_TO_POP_SCALE)/1000.0;//convert to kilometers
        if(nearest_school == null)
            ebolaSim.no_school_count++;
        else
        {
            ebolaSim.distance_count++;
            ebolaSim.distance_sum += min_distance;
            if(min_distance > ebolaSim.max_distance)
                ebolaSim.max_distance = min_distance;
        }
        return nearest_school;
    }

    /**
     * @param county_id Id of county
     * @return country, 0 - Guinea, 1 - Sierra Leone, 2 - Liberia okay
     */
    private static int determineCountry(int county_id)
    {
        if(county_id >= Parameters.MIN_GUINEA_COUNTY_ID && county_id <= Parameters.MAX_GUINEA_COUNTY_ID)
            return Parameters.GUINEA;
        else if(county_id >= Parameters.MIN_SL_COUNTY_ID && county_id <= Parameters.MAX_SL_COUNTY_ID)
            return Parameters.SL;
        else //if(county_id >= Parameters.MIN_LIB_COUNTY_ID && county_id <= Parameters.MAX_LIB_COUNTY_ID)
            return Parameters.LIBERIA;

        //System.out.println("!!!!!!ERRORRORORORORO!!!!!!! " + county_id);
        //return 10000;
    }

    /**
     *
     * @param prev_tokens previous row
     * @param curr_tokens current row
     * @param next_tokens next row
     * @param i column index
     * @param j row index
     * @return whether the nearby cells have a total population density greater than 500 people per square mile
     */
    private static boolean nearbyUrban(String[] prev_tokens, String[] curr_tokens, String[] next_tokens, int i, int j)
    {
        int sum = 0;
        int count = 0;
        if(i < ebolaSim.pop_height-1)//not the last row
        {
            if(Integer.parseInt(next_tokens[j]) > 0)
            {
                sum += Integer.parseInt(next_tokens[j]);//add cell underneath
                count++;
            }
            if(j > 0 && Integer.parseInt(next_tokens[j-1]) > 0)
            {
                sum += Integer.parseInt(next_tokens[j-1]);//add the cell diagnol bottom left
                count++;
            }
            if(j < ebolaSim.pop_width - 1 && Integer.parseInt(next_tokens[j+1]) > 0)
            {
                sum += Integer.parseInt(next_tokens[j+1]);//add the cell diagnol bottom right
                count++;
            }
        }
        if(i > 0)//not the first row
        {
            if(Integer.parseInt(prev_tokens[j]) > 0)
            {
                sum += Integer.parseInt(prev_tokens[j]);//add cell above
                count++;
            }
            if(j > 0)
            {
                sum += Integer.parseInt(prev_tokens[j-1]);//add cell the diagnol top left
                count++;
            }
            if(j < ebolaSim.pop_width - 1)
            {
                if(Integer.parseInt(prev_tokens[j+1]) > 0)
                {
                    sum += Integer.parseInt(prev_tokens[j + 1]);//add cell diagnol top right
                    count++;
                }
            }
        }
        if(j < ebolaSim.pop_width - 1)//not last column
        {
            if(Integer.parseInt(curr_tokens[j+1]) > 0)
            {
                count++;
                sum += Integer.parseInt(curr_tokens[j+1]);//add cell to the right
            }
        }
        if(j > 0)//not first column
        {
            if(Integer.parseInt(curr_tokens[j-1]) > 0)
            {
                sum += Integer.parseInt(curr_tokens[j - 1]);//add cell to the left
                count++;
            }
        }
        return sum > (count * Parameters.MIN_POP_SURROUNDING);
    }

    /**
     * Scales integer values and rounds the number appropiately so aggregate scaling is similar to segregated scaling.
     * @param val The value that neeeds scaling.  Must be an int
     * @param scalar The percentage to scale normally a ndouble from 0-1
     * @return the value scaled
     */
    public static int scale(int val, double scalar)
    {
        int scaled = 0;
        double val_scaled = val*scalar;
        scaled = (int)val_scaled;
        val_scaled -= (int)val_scaled;
        if(ebolaSim.random.nextDouble() < val_scaled)
            scaled += 1;
        return scaled;
    }

    public static int pickHouseholdSize(int country)
    {
        double average;
        if(country == Parameters.GUINEA)
            average = Parameters.GUINEA_AVG_HOUSEHOLD_SIZE;
        else if(country == Parameters.LIBERIA)
            average = Parameters.LIB_AVG_HOUSEHOLD_SIZE;
        else
            average = Parameters.SL_AVG_HOUSEHOLD_SIZE;

        double stdv = Parameters.LIB_HOUSEHOLD_STDEV;
        return (int)Stats.normalToLognormal(Stats.calcLognormalMu(average, stdv), Stats.calcLognormalSigma(average, stdv),
                ebolaSim.random.nextGaussian());
    }

    /**
     * Picks an age based on the the age_dist hashmap.  Pick the highest age within the range.
     * Within each fivebucket range it picks an age randomly
     */
    private static int pick_age(HashMap<Integer, ArrayList<Double>> age_dist, int county_id)
    {
        double rand = ebolaSim.random.nextDouble();
        if(county_id == -9999)
            county_id = Parameters.MIN_LIB_COUNTY_ID;
        ArrayList<Double> dist = age_dist.get(county_id);
        int i;
        for(i = 0; i < dist.size(); i++)
        {
            if(rand < dist.get(i))
                break;
        }
        int age = i*5 + ebolaSim.random.nextInt(5);
        //System.out.println(age + " years");
        return age;
    }

    public static class Node
    {
        public Int2D location;

        ArrayList<Edge> links;
        double weightOnLineString;//measures the weight on the line string from 0
        public HashSet<LineString> lineStrings = new HashSet<LineString>();
        public int index;
        public Node(Int2D l)
        {
            location = l;
            links = new ArrayList<Edge>();
        }

        public ArrayList<Edge> getLinks() {
            return links;
        }
        @Override
        public String toString()
        {
            return "(" + location.getX() + ", " + location.getY() + ")";
        }
    }

}
