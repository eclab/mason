/*
 * NetworkWorld.java
 *
 * $Id: NetworkWorld.java,v 1.1 2010-04-10 18:20:05 kemsulli Exp $
 */
package sim.app.geo.networkworld;

import sim.util.geo.Network;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.planargraph.Node;
import java.util.Iterator;
import sim.engine.SimState;
import sim.field.geo.GeomField;
import sim.util.geo.GeomWrapper;

/** Set up a GeoField with a number of points and a corresponding portrayal.
 *
 * @author mcoletti
 */
public class NetworkWorld extends SimState
{

    public GeomField world = new GeomField(); // contains road network
    public GeomField junctions = new GeomField(); // nodes for intersections
    public GeomField agents = new GeomField(); // agents moving through network

    // Stores transportation network connections
    public Network network = new Network();

    // Agent that moves around the world
    Agent a = new Agent();

    public NetworkWorld(long seed) throws ParseException
    {
        super(seed);

        // Add a lines and a polygon

        WKTReader rdr = new WKTReader();
        LineString line = null;

        try
            {
                line = (LineString) (rdr.read("LINESTRING (10 50, 20 50)"));
                world.addGeometry(new GeomWrapper(line, null));

                line = (LineString) (rdr.read("LINESTRING (20 50, 30 50)"));
                world.addGeometry(new GeomWrapper(line, null));

                line = (LineString) (rdr.read("LINESTRING (30 50, 40 50)"));
                world.addGeometry(new GeomWrapper(line, null));

                line = (LineString) (rdr.read("LINESTRING (20 50, 20 10, 30 10)"));
                world.addGeometry(new GeomWrapper(line, null));

                line = (LineString) (rdr.read("LINESTRING (30 50, 30 20, 40 20)"));
                world.addGeometry(new GeomWrapper(line, null));
            
                // zoom out to see all of line
                Envelope mbr = world.getMBR();
                mbr.expandToInclude(0.0, 0.0);

                agents.addGeometry(new GeomWrapper(a.getGeometry(), null));
                mbr.expandToInclude(agents.getMBR());

                mbr.expandBy(20.0); // fluff it out so we can see everything

                agents.setMBR(mbr);
                world.setMBR(mbr);
            }
        catch (ParseException parseException)
            {
                System.out.println("Bogus line string");
            }

        network.createFromGeomField(world);
        addIntersectionNodes( network.nodeIterator(), junctions) ;
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

        while (nodeIterator.hasNext())
            {
                Node node = (Node) nodeIterator.next();
                System.out.println("node: " + node.getCoordinate() + " " + node.getDegree());
                coord = node.getCoordinate();
                point = fact.createPoint(coord);
                junctions.addGeometry(new GeomWrapper(point, null));
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
        GeometryFactory fact = new GeometryFactory(); // XXX consider making this static member
        Point location = fact.createPoint(new Coordinate(x, y));
        world.addGeometry(new GeomWrapper(location, null));
    }

    public static void main(String[] args)
    {
        doLoop(NetworkWorld.class, args);
        System.exit(0);
    }
}
