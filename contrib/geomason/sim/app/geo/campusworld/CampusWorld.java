/* 
Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
George Mason University Mason University Licensed under the Academic
Free License version 3.0

See the file "LICENSE" for more information
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
import sim.field.geo.GeomVectorField;
import sim.io.geo.*;
import sim.util.*;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.MasonGeometry;

/** 
 * This simple example shows how to setup GeomVectorFields and run agents around the fields.  The simulation has 
 * multiple agents following the walkways at George Mason University.  GIS information about the walkways, buildings, 
 * and roads provides the environment for the agents.  During the simulation, the agents wander randomly on the walkways.
 */
public class CampusWorld extends SimState
{
    private static final long serialVersionUID = -4554882816749973618L;

    public static final int WIDTH = 300; 
    public static final int HEIGHT = 300; 
    
    /** How many agents in the simulation */ 
	public int numAgents = 1000;

    /** Fields to hold the associated GIS information */ 
    public GeomVectorField walkways = new GeomVectorField(WIDTH, HEIGHT);
    public GeomVectorField roads = new GeomVectorField(WIDTH, HEIGHT);
    public GeomVectorField buildings = new GeomVectorField(WIDTH,HEIGHT);

    // where all the agents live
    public GeomVectorField agents = new GeomVectorField(WIDTH, HEIGHT);

    // The Importer is responsible for reading in the GIS files.  If you have installed either 
    // GeoTools and/or OGR on your system, you can use those importers.  The ShapeFileImporter does 
    // not rely on external libraries.  
    //OGRImporter importer = new OGRImporter();
    //GeoToolsImporter importer = new GeoToolsImporter();
    ShapeFileImporter importer = new ShapeFileImporter();


    // Stores the walkway network connections.  We represent the walkways as a PlanarGraph, which allows 
    // easy selection of new waypoints for the agents.  
    public GeomPlanarGraph network = new GeomPlanarGraph();
    public GeomVectorField junctions = new GeomVectorField(WIDTH, HEIGHT); // nodes for intersections


    public CampusWorld(long seed) { super (seed); } 
        

    public int getNumAgents() { return numAgents; } 
    public void setNumAgents(int n) { if (n > 0) numAgents = n; } 

    /** Add agents to the simulation and to the agent GeomVectorField.  Note that each agent does not have
     * any attributes.   */
    void addAgents()
    {
        for (int i = 0; i < numAgents; i++)
            {
                Agent a = new Agent();
                MasonGeometry mg = new MasonGeometry(a.getGeometry()); 
                mg.isMovable = true; 
                agents.addGeometry(mg);
                a.start(this);
                schedule.scheduleRepeating(a);
                
                // we can set the userData field of any MasonGeometry.  If the userData is inspectable, 
                // then the inspector will show this information 
                //if (i == 10) 
                //	buildings.getGeometry("CODE", "JC").setUserData(a); 
            }
    }

    public void finish()
    {
        super.finish();
;
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
                importer.ingest("../../data/bldg", CampusWorld.class, buildings, masked);

                // We want to save the MBR so that we can ensure that all GeomFields
                // cover identical area.
                Envelope MBR = buildings.getMBR();

                System.out.println("reading roads layer");
                importer.ingest("../../data/roads", CampusWorld.class, roads, null);

                MBR.expandToInclude(roads.getMBR());

                System.out.println("reading walkways layer");
                importer.ingest("../../data/walk_ways", CampusWorld.class, walkways, null);

                MBR.expandToInclude(walkways.getMBR());

                System.out.println("Done reading data");

                // Now synchronize the MBR for all GeomFields to ensure they cover the same area
                buildings.setMBR(MBR);
                roads.setMBR(MBR);
                walkways.setMBR(MBR);

                network.createFromGeomField(walkways);

                addIntersectionNodes( network.nodeIterator(), junctions ) ;

                agents.clear(); // clear any existing agents from previous runs
                addAgents();
                agents.setMBR(MBR);

            }
        catch (FileNotFoundException ex)
            {
                Logger.getLogger(CampusWorld.class.getName()).log(Level.SEVERE, null, ex);
            }
    }



    /** adds nodes corresponding to road intersections to GeomVectorField
     *
     * @param nodeIterator Points to first node
     * @param intersections GeomVectorField containing intersection geometry
     *
     * Nodes will belong to a planar graph populated from LineString network.
     */
    private void addIntersectionNodes(Iterator nodeIterator, GeomVectorField intersections)
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

                junctions.addGeometry(new MasonGeometry(point));
                counter++;
            }
    }

    public static void main(String[] args)
    {
        doLoop(CampusWorld.class, args);
        System.exit(0);
    }
}
