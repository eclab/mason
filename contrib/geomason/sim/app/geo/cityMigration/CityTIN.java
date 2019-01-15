package sim.app.geo.cityMigration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cityMigration.cityMigrationData.CityMigrationData;
import riftland.PopulationCenter;
import riftland.World;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.field.grid.IntGrid2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.IntBag;
import sim.util.geo.MasonGeometry;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import java.net.URL;

/**
 * Triangulated Irregular Network connecting cities.
 *
 * @author jharrison
 */
public class CityTIN {
    public GeomVectorField nodes;
    public GeomVectorField edges;
    public Network network = new Network(false);
    public AllPairsShortestPath allPairsShortestPath;    // this gets initialized in the buildNetwork() function

    public HashMap<MasonGeometry, PopulationCenter> nodesToCities = new HashMap<MasonGeometry, PopulationCenter>();

    public CityTIN(String nodesFilename, String edgesFilename, int width, int height) {

        System.out.println("reading road network...");

        nodes = new GeomVectorField(width, height);
        edges = new GeomVectorField(width, height);


        try {
            //File targetFile = createLocalFile(nodesFilename);
            URL nodesFile = getUrl(nodesFilename);

            URL edgesFile = getUrl(edgesFilename);


            // read in the roads to create the transit network
            ShapeFileImporter.read(nodesFile, nodes);
            ShapeFileImporter.read(edgesFile, edges);


            Bag geoms = nodes.getGeometries();
            System.out.format("CityTIN: nodes.size: %d\n", geoms.numObjs);

            for (int i = 0; i < geoms.numObjs; i++) {
                MasonGeometry mg = (MasonGeometry) geoms.get(i);
                mg.isMovable = true;
//            	System.out.format("Node[%d]: %s\n", i, mg.geometry);
            }

            geoms = edges.getGeometries();
            System.out.format("CityTIN: edges.size: %d\n", geoms.numObjs);
        } catch (Exception ex) {
        }

    }

    private URL getUrl(String nodesFilename) throws IOException {
        InputStream nodeStream = CityMigrationData.class.getResourceAsStream(nodesFilename);
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

    public void matchPopulationCenters(GeomGridField grid, Map<String, PopulationCenter> populationCenters) {

        Bag geoms = nodes.getGeometries();
        IntGrid2D intGrid = (IntGrid2D) grid.getGrid();

        for (int i = 0; i < geoms.numObjs; i++) {
            MasonGeometry mg = (MasonGeometry) geoms.get(i);
            Point pt = mg.geometry.getCentroid();
            int x = grid.toXCoord(pt);
            int y = grid.toYCoord(pt);
            int id = intGrid.get(x, y);

            if (id == -9999)
                id = findNearestPopulationCenter(intGrid, x, y);

//        	System.out.format("%s : %d\n", pt, id);

            PopulationCenter city = populationCenters.get(Integer.toString(id));
            city.setCentroid(new Double2D(x, y));
            nodesToCities.put(mg, city);
        }

    }

    public int findNearestPopulationCenter(IntGrid2D intGrid, int x, int y) {
        IntBag xPos = new IntBag();
        IntBag yPos = new IntBag();

        int id = -9999;
        for (int i = 0; i < 10; i++) {
            intGrid.getNeighborsMaxDistance(x, y, i, false, xPos, yPos);
            for (int j = 0; j < xPos.numObjs; j++) {
                id = intGrid.get(xPos.get(j), yPos.get(j));
                if (id != -9999)
                    return id;
            }
        }

        return -9999;
    }

    public ArrayList<CityEdge> matchEdges() {

        ArrayList<CityEdge> edgeList = new ArrayList<CityEdge>();
        Bag edgeGeoms = edges.getGeometries();
        Bag nodeGeoms = nodes.getGeometries();

        PopulationCenter city1 = null, city2 = null;

        // loop through edges
        for (int i = 0; i < edgeGeoms.numObjs; i++) {
            city1 = city2 = null;
            MasonGeometry mg = (MasonGeometry) edgeGeoms.get(i);
            if (mg.geometry instanceof LineString) {
                LineString edge = (LineString) mg.geometry;

                // loop through nodes
                for (int j = 0; j < nodeGeoms.numObjs; j++) {
                    MasonGeometry node = (MasonGeometry) nodeGeoms.get(j);
                    if (edge.covers(node.geometry)) {
                        // we found a match
                        if (city1 == null)
                            city1 = nodesToCities.get(node);
                        else {
                            city2 = nodesToCities.get(node);
                            break;
                        }
                    }
                } // end loop through nodes

                if ((city1 != null) && (city2 != null))
                    // add edge
                    edgeList.add(new CityEdge(city1, city2));
            }
        }

        System.out.format("EdgeGeoms.size: %d\n", edgeGeoms.numObjs);
        System.out.format("EdgeList.size: %d\n", edgeList.size());

        return edgeList;
    }

    public void buildNetwork(Map<String, PopulationCenter> populationCenters) {

        network.clear();
        // Add nodes
        for (PopulationCenter city : populationCenters.values())
            network.addNode(city);

        // Add edges
        ArrayList<CityEdge> edges = matchEdges();

        for (CityEdge edge : edges) {
            PopulationCenter city = edge.city1;
            PopulationCenter cityPartner = edge.city2;

            double edgeDistance = city.getCentroid().distance(cityPartner.getCentroid());
            network.addEdge(new Edge(city, cityPartner, edgeDistance));
        }

        allPairsShortestPath = new AllPairsShortestPath(network);
    }

}
