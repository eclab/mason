/*
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package sim.app.geo.campusworld;
import sim.app.geo.campusworld.data.CampusWorldData;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileExporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
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
    public GeomVectorField walkways = new GeomVectorField(CampusWorld.WIDTH, CampusWorld.HEIGHT);
    public GeomVectorField roads = new GeomVectorField(CampusWorld.WIDTH, CampusWorld.HEIGHT);
    public GeomVectorField buildings = new GeomVectorField(CampusWorld.WIDTH,CampusWorld.HEIGHT);

    // where all the agents live
    public GeomVectorField agents = new GeomVectorField(CampusWorld.WIDTH, CampusWorld.HEIGHT);


    // Stores the walkway network connections.  We represent the walkways as a PlanarGraph, which allows
    // easy selection of new waypoints for the agents.
    public GeomPlanarGraph network = new GeomPlanarGraph();
    public GeomVectorField junctions = new GeomVectorField(CampusWorld.WIDTH, CampusWorld.HEIGHT); // nodes for intersections



    public CampusWorld(final long seed)
    {
        super(seed);

        try
        {
            System.out.println("reading buildings layer");

            // this Bag lets us only display certain fields in the Inspector, the non-masked fields
            // are not associated with the object at all
            final Bag masked = new Bag();
            masked.add("NAME");
            masked.add("FLOORS");
            masked.add("ADDR_NUM");

//                System.out.println(System.getProperty("user.dir"));

            // read in the buildings GIS file

            final URL bldgGeometry = CampusWorldData.class.getResource("bldg.shp");
            final URL bldgDB = CampusWorldData.class.getResource("bldg.dbf");
            ShapeFileImporter.read(bldgGeometry, bldgDB, buildings, masked);

            // We want to save the MBR so that we can ensure that all GeomFields
            // cover identical area.
            final Envelope MBR = buildings.getMBR();

            System.out.println("reading roads layer");

            final URL roadGeometry = CampusWorldData.class.getResource("roads.shp");
            final URL roadDB = CampusWorldData.class.getResource("roads.dbf");
            ShapeFileImporter.read(roadGeometry, roadDB, roads);

            MBR.expandToInclude(roads.getMBR());

            System.out.println("reading walkways layer");

            final URL walkWayGeometry = CampusWorldData.class.getResource("walk_ways.shp");
            final URL walkWayDB = CampusWorldData.class.getResource("walk_ways.dbf");
            ShapeFileImporter.read(walkWayGeometry, walkWayDB, walkways);

            MBR.expandToInclude(walkways.getMBR());

            System.out.println("Done reading data");

            // Now synchronize the MBR for all GeomFields to ensure they cover the same area
            buildings.setMBR(MBR);
            roads.setMBR(MBR);
            walkways.setMBR(MBR);

            network.createFromGeomField(walkways);

            addIntersectionNodes(network.nodeIterator(), junctions);

        } catch (final Exception ex)
        {
            Logger.getLogger(CampusWorld.class.getName()).log(Level.SEVERE, null, ex);
        }

    }


    public int getNumAgents() { return numAgents; }
    public void setNumAgents(final int n) { if (n > 0) numAgents = n; }

    /**
     * Add agents to the simulation and to the agent GeomVectorField. Note that
     * each agent does not have any attributes.
     */
    void addAgents()
    {
        for (int i = 0; i < numAgents; i++)
        {
            final Agent a = new Agent(this);

            agents.addGeometry(a.getGeometry());

            schedule.scheduleRepeating(a);

            // we can set the userData field of any MasonGeometry.  If the userData is inspectable,
            // then the inspector will show this information
            //if (i == 10)
            //	buildings.getGeometry("CODE", "JC").setUserData(a);
        }
    }



    @Override
    public void finish()
    {
        super.finish();

        // Save the agents layer, which has no corresponding originating
        // shape file.
        ShapeFileExporter.write("agents", agents);
    }


    @Override
    public void start()
    {
        super.start();

        agents.clear(); // clear any existing agents from previous runs
        

        
        addAgents();
        

        agents.setMBR(buildings.getMBR());

		

        
        // Ensure that the spatial index is made aware of the new agent
        // positions.  Scheduled to guaranteed to run after all agents moved.
        schedule.scheduleRepeating( agents.scheduleSpatialIndexUpdater(), Integer.MAX_VALUE, 1.0);
    }



    /** adds nodes corresponding to road intersections to GeomVectorField
     *
     * @param nodeIterator Points to first node
     * @param intersections GeomVectorField containing intersection geometry
     *
     * Nodes will belong to a planar graph populated from LineString network.
     */
    private void addIntersectionNodes(final Iterator nodeIterator, final GeomVectorField intersections)
    {
        final GeometryFactory fact = new GeometryFactory();
        Coordinate coord = null;
        Point point = null;
        int counter = 0;

        while (nodeIterator.hasNext())
            {
                final Node node = (Node) nodeIterator.next();
                coord = node.getCoordinate();
                point = fact.createPoint(coord);

                junctions.addGeometry(new MasonGeometry(point));
                counter++;
            }
    }

    public static void main(final String[] args) {
        doLoop(CampusWorld.class, args);
        System.exit(0);
    }
}
