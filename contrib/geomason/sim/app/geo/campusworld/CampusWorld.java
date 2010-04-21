/*
 * CampusWorld.java
 *
 * $Id: CampusWorld.java,v 1.3 2010-04-21 20:04:29 mcoletti Exp $
 */
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
import sim.util.geo.GeomWrapper;

/** Set up a GeoField with a number of points and a corresponding portrayal.
 *
 * @author mcoletti
 */
public class CampusWorld extends SimState
{
    public int numAgents = 1000;

    private static final String dataDirectory = "sim/app/data/";

    // where all the stream geometry lives
    public GeomField walkways = new GeomField();
    public GeomField roads = new GeomField();
    public GeomField buildings = new GeomField();

    // where all the agents live
    public GeomField agents = new GeomField();

    // The Importer is responsible for reading in the GIS files.
    //OGRImporter importer = new OGRImporter();
    //GeoToolsImporter importer = new GeoToolsImporter();
    ShapeFileImporter importer = new ShapeFileImporter();


    // Stores transportation network connections
    public Network network = new Network();
    public GeomField junctions = new GeomField(); // nodes for intersections


    public CampusWorld(long seed)
    {
        super(seed);

    }

    public int getNumAgents()
    {
        return numAgents;
    }

    public void setNumAgents(int n)
    {
        numAgents = n;
    }

    /** add agents to the simulation
     */
    private void addAgents()
    {
        for (int i = 0; i < numAgents; i++)
            {
                Agent a = new Agent();
                agents.addGeometry(new GeomWrapper(a.getGeometry(), null));
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
    private void addIntersectionNodes(Iterator nodeIterator, GeomField intersections)
    {
        GeometryFactory fact = new GeometryFactory();
        Coordinate coord = null;
        Point point = null;
        int counter = 0;

        while (nodeIterator.hasNext())
            {
                Node node = (Node) nodeIterator.next();

                if ( counter % 10 == 0 )
                    System.out.print(".");

                coord = node.getCoordinate();
                point = fact.createPoint(coord);

                junctions.addGeometry(new GeomWrapper(point, null));
                counter++;
            }
    }

    public static void main(String[] args)
    {
        doLoop(CampusWorld.class, args);
        System.exit(0);
    }
}
