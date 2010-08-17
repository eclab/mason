package sim.app.geo.campusworld;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;
import java.io.*;
import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.field.geo.GeomField;
import sim.io.geo.*;
import sim.util.geo.Network;
import sim.util.*;
import sim.util.geo.MasonGeometry;

/** 
 * This simple example shows how to setup GeomFields and run agents around the fields.  The simulation has 
 * multiple agents following the walkways at George Mason University.  GIS information about the walkways, buildings, 
 * and roads provides the environment for the agents.  During the simulation, the agents wander randomly on the walkways.
 */
public class CampusWorld extends SimState
{
    private static final long serialVersionUID = -4554882816749973618L;

    /** How many agents in the simulation */ 
	public int numAgents = 1000;

	/** Where the GIS files are stored */ 
    private static final String dataDirectory = "sim/app/data/";

    /** Fields to hold the associated GIS information */ 
    public GeomField walkways = new GeomField();
    public GeomField roads = new GeomField();
    public GeomField buildings = new GeomField();

    // where all the agents live
    public GeomField agents = new GeomField();

    // The Importer is responsible for reading in the GIS files.  If you have installed either 
    // GeoTools and/or OGR on your system, you can use those importers.  The ShapeFileImporter does 
    // not rely on external libraries.  
    //OGRImporter importer = new OGRImporter();
    //GeoToolsImporter importer = new GeoToolsImporter();
    ShapeFileImporter importer = new ShapeFileImporter();


    // Stores the walkway network connections.  We represent the walkways as a PlanarGraph, which allows 
    // easy selection of new waypoints for the agents.  
    public Network network = new Network();
    public GeomField junctions = new GeomField(); // nodes for intersections


    public CampusWorld(long seed) { super (seed); } 
        

    public int getNumAgents() { return numAgents; } 
    public void setNumAgents(int n) { if (n > 0) numAgents = n; } 

    /** Add agents to the simulation and to the agent GeomField.  Note that each agent does not have 
     * any attributes.   */
    void addAgents()
    {
        for (int i = 0; i < numAgents; i++)
            {
                Agent a = new Agent();
                agents.addGeometry(new MasonGeometry(a.getGeometry(), null));
                a.start(this);
                schedule.scheduleRepeating(a);
            }
    }

    public void finish()
    {
        super.finish();
        agents.clear();
    }
    public void start()
    {
        super.start();
        try
            {
                System.out.println("reading buildings layer");

                // this Bag lets us only display certain fields in the Inspector, the non-masked fields
                // are not associated with the object at all
                Bag masked = new Bag();
                masked.add("NAME");
                masked.add("FLOORS");
                masked.add("ADDR_NUM");

                // read in the buildings GIS file 
                importer.ingest(dataDirectory + "bldg.shp", buildings, null);

                // We want to save the MBR so that we can ensure that all GeomFields
                // cover identical area.
                Envelope MBR = buildings.getMBR();

                System.out.println("reading roads layer");
                importer.ingest(dataDirectory + "roads.shp", roads, null);

                MBR.expandToInclude(roads.getMBR());

                System.out.println("reading walkways layer");
                importer.ingest(dataDirectory + "walk_ways.shp", walkways, null);

                MBR.expandToInclude(walkways.getMBR());

                System.out.println("Done reading data");

                // Now synchronize the MBR for all GeomFields to ensure they cover the same area
                buildings.setMBR(MBR);
                roads.setMBR(MBR);
                walkways.setMBR(MBR);

                network.createFromGeomField(walkways);

                addIntersectionNodes( network.nodeIterator(), junctions ) ;
                addAgents();
                agents.setMBR(MBR);

            }
        catch (FileNotFoundException ex)
            {
                Logger.getLogger(CampusWorld.class.getName()).log(Level.SEVERE, null, ex);
            }
    }



    /** adds nodes corresponding to road intersections to GeomField
     *
     * @param nodeIterator Points to first node
     * @param intersections GeomField containing intersection geometry
     *
     * Nodes will belong to a planar graph populated from LineString network.
     */
    private void addIntersectionNodes(Iterator<?> nodeIterator, GeomField intersections)
    {
        GeometryFactory fact = new GeometryFactory();
        Coordinate coord = null;
        Point point = null;
        int counter = 0;

        while (nodeIterator.hasNext())
            {
                Node node = (Node) nodeIterator.next();
                coord = node.getCoordinate();
                point = fact.createPoint(coord);

                junctions.addGeometry(new MasonGeometry(point, null));
                counter++;
            }
    }

    public static void main(String[] args)
    {
        doLoop(CampusWorld.class, args);
        System.exit(0);
    }
}
