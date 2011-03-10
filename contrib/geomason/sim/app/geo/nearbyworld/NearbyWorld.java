package sim.app.geo.nearbyworld;

import com.vividsolutions.jts.geom.Coordinate;
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
    
	public GeomVectorField world = new GeomVectorField(WIDTH, HEIGHT);
    public GeomVectorField agentField = new GeomVectorField(WIDTH, HEIGHT);

    // Agent that moves around the world
    Agent agent;

	// size of the display 
	public static final int WIDTH=300; 
	public static final int HEIGHT=300; 

    
    public NearbyWorld(long seed) 
    {
        super(seed);
    }
        
    public void start()
    {
        super.start();
        // position the agent at a random starting location 
	agent = new Agent(random.nextInt(WIDTH), random.nextInt(HEIGHT)); 

 	world = new GeomVectorField(WIDTH, HEIGHT);
 	agentField = new GeomVectorField(WIDTH, HEIGHT);

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
                world.addGeometry(new MasonGeometry(line));

                line = (LineString) (rdr.read("LINESTRING (75 20, 35 19, 50 50, 50 90)"));
                world.addGeometry(new MasonGeometry(line));
                        
                polygon = (Polygon) (rdr.read("POLYGON (( 25 45, 25 75, 45 75, 45 45, 25 45 ))"));
                world.addGeometry(new MasonGeometry(polygon));
            }
        catch (ParseException parseException)
            {
                System.out.println("Bogus line string" + parseException);
            }

        
        // Add the agent
        agentField.addGeometry(new MasonGeometry(agent.getGeometry()));

        // Ensure that both GeomVectorField layers cover the same area otherwise the
        // agent won't show up in the display.
        agentField.setMBR(world.getMBR());
        
        schedule.scheduleRepeating(agent);
    }
    
    void addPoint(final double x, final double y)
    {
        GeometryFactory fact = new GeometryFactory();
        Point location = fact.createPoint(new Coordinate(x, y));
        world.addGeometry(new MasonGeometry(location));
    }

    public static void main(String[] args)
    {
        doLoop(NearbyWorld.class, args);
        System.exit(0);
    }
}
