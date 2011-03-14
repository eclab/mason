package sim.app.geo.nearbyworld;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.util.geo.*; 

/** 
 *  The NearbyWorld GeoMASON example show how to add Geometries to a field, and also how to query 
 *  the field for the closest objects.  In this simulation, we create severals points, two lines, and 
 *  a polygon.  An agent then randomly wanders around, reporting the closest objects.  
 *  
 *  <p> The agent wanders in a GeomVectorField so that we can use the spatial indexing feature to 
 *  determine the closest objects. 
 */
public class NearbyWorld extends SimState
{

    private static final long serialVersionUID = 752764560336956655L;

    // Contains the objects in which the agent will be wandering
	public GeomVectorField objects = new GeomVectorField(WIDTH, HEIGHT);

    // Field for just the agent
    public GeomVectorField agentField = new GeomVectorField(WIDTH, HEIGHT);

    // Field that's used to highlight nearby objects
    public GeomVectorField nearbyField = new GeomVectorField(WIDTH, HEIGHT);

    // Agent that moves around the objects
    Agent agent;

	// size of the display 
	public static final int WIDTH=300; 
	public static final int HEIGHT=300; 

    
    public NearbyWorld(long seed) 
    {
        super(seed);

        createWorld();
    }


    /** set up the static geometry in the objects
     *
     */
    private void createWorld()
    {
        // Add a few points for the agent to move around

        addPoint(0, 0);
        addPoint(100, 100);
        addPoint(25, 13);
        addPoint(7, 8);
        addPoint(80, 44);
        addPoint(12, 66);
        addPoint(19, 19);
        addPoint(45, 8);
        addPoint(99, 8);

        // Add a lines and a polygon

        WKTReader rdr = new WKTReader();

        LineString line = null;
        Polygon polygon = null;

        try
        {
            line = (LineString) (rdr.read("LINESTRING (0 0, 10 10, 20 20)"));
            objects.addGeometry(new MasonGeometry(line));

            line = (LineString) (rdr.read("LINESTRING (75 20, 35 19, 50 50, 50 90)"));
            objects.addGeometry(new MasonGeometry(line));

            polygon = (Polygon) (rdr.read("POLYGON (( 25 45, 25 75, 45 75, 45 45, 25 45 ))"));
            objects.addGeometry(new MasonGeometry(polygon));
        } catch (ParseException parseException)
        {
            System.out.println("Bogus line string" + parseException);
        }

        Envelope e = objects.getMBR();
        e.expandBy(5.0);
        objects.setMBR(e);
    }


        
    @Override
    public void start()
    {
        super.start();

        agentField.clear(); // remove any agents from previous runs
        nearbyField.clear();

        // position the agent at a random starting location 
        agent = new Agent(random.nextInt(WIDTH), random.nextInt(HEIGHT));

        // Add the agent
        agentField.addGeometry(new MasonGeometry(agent.getGeometry()));

        // Ensure that both GeomVectorField layers cover the same area otherwise the
        // agent won't show up in the display.
        agentField.setMBR(objects.getMBR());
        nearbyField.setMBR(objects.getMBR());

        schedule.scheduleRepeating(agent);
    }


    
    private void addPoint(final double x, final double y)
    {
        GeometryFactory fact = new GeometryFactory();
        Point location = fact.createPoint(new Coordinate(x, y));
        objects.addGeometry(new MasonGeometry(location));
    }


    public static void main(String[] args)
    {
        doLoop(NearbyWorld.class, args);
        System.exit(0);
    }
}
