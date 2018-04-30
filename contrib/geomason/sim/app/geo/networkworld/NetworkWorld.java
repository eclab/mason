/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: NetworkWorld.java,v 1.6 2010-08-25 20:06:50 mcoletti Exp $
 * 
 */
package networkworld;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.planargraph.Node;
import java.util.Iterator;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.MasonGeometry;

/**
 *  The NetworkWorld GeoMASON example shows how to add geometries into a GeomVectorField as a network.  We add several 
 *  lines and create intersections at each junction using a JTS PlanarGraph.  Next, we add an agent in, which moves randomly
 *  along the network, choosing a random direction at each junction.   
 */
public class NetworkWorld extends SimState
{

    private static final long serialVersionUID = 2025934565604118804L;
    
    public static final int WIDTH = 300; 
    public static final int HEIGHT = 300; 
    
    public GeomVectorField world = new GeomVectorField(WIDTH, HEIGHT); // contains road network
    public GeomVectorField junctions = new GeomVectorField(WIDTH, HEIGHT); // nodes for intersections
    public GeomVectorField agents = new GeomVectorField(WIDTH, HEIGHT); // agents moving through network

    // Stores transportation network connections
    public GeomPlanarGraph network = new GeomPlanarGraph();
    
    // Agent that moves around the world
    Agent a = new Agent();

    public NetworkWorld(long seed)
    {
        super(seed);

        // Add a lines and a polygon

        WKTReader rdr = new WKTReader();
        LineString line = null;

        try
        {
            line = (LineString) (rdr.read("LINESTRING (10 50, 20 50)"));
            world.addGeometry(new MasonGeometry(line));

            line = (LineString) (rdr.read("LINESTRING (20 50, 30 50)"));
            world.addGeometry(new MasonGeometry(line));

            line = (LineString) (rdr.read("LINESTRING (30 50, 40 50)"));
            world.addGeometry(new MasonGeometry(line));

            line = (LineString) (rdr.read("LINESTRING (20 50, 20 10, 30 10)"));
            world.addGeometry(new MasonGeometry(line));

            line = (LineString) (rdr.read("LINESTRING (30 50, 30 20, 40 20)"));
            world.addGeometry(new MasonGeometry(line));


            agents.addGeometry(new MasonGeometry(a.getGeometry()));

        } catch (ParseException parseException)
        {
            System.out.println("Bogus line string" + parseException);
        }

        network.createFromGeomField(world);
        addIntersectionNodes(network.nodeIterator(), junctions);
        
        // Ensure that the minimum bounding rectangles (MBRs) are all in sync,
        // else the layers won't be properly aligned when rendering

        // zoom out to see all of line
        Envelope mbr = world.getMBR();

        mbr.expandToInclude(agents.getMBR());
        mbr.expandToInclude(junctions.getMBR());

        mbr.expandBy(20.0); // fluff it out so we can see everything

        agents.setMBR(mbr);
        world.setMBR(mbr);
        junctions.setMBR(mbr);
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

        while (nodeIterator.hasNext())
        {
            Node node = (Node) nodeIterator.next();
            System.out.println("node: " + node.getCoordinate() + " " + node.getDegree());
            coord = node.getCoordinate();
            point = fact.createPoint(coord);
            junctions.addGeometry(new MasonGeometry(point));
        }
    }

    public void start()
    {
        super.start();
        a.start(this);
        schedule.scheduleRepeating(a);
    }

    void addPoint(final double x, final double y)
    {
        GeometryFactory fact = new GeometryFactory();
        Point location = fact.createPoint(new Coordinate(x, y));
        world.addGeometry(new MasonGeometry(location));
    }

    public static void main(String[] args)
    {
        doLoop(NetworkWorld.class, args);
        System.exit(0);
    }

}
