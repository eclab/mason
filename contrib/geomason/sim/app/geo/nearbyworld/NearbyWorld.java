/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package nearbyworld;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.util.geo.MasonGeometry;

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
	public static final int WIDTH = 300;
	public static final int HEIGHT = 300;
    
    private static final int NUM_POINTS = 40;
    private static final int NUM_RECTANGLES = 35;
    public static final int NUM_LINES = 35;

    /** Average number of line segments in randomly generated lines
     */
    private static final int NUM_LINE_SEGMENTS = 6;

    
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
        for (int i = 0; i < NUM_LINES; i++)
        {
            addRandomLine(WIDTH, HEIGHT);
        }

        for (int i = 0; i < NUM_RECTANGLES; i++)
        {
            addRandomRectangle(WIDTH, HEIGHT);
        }

        for (int i = 0; i < NUM_POINTS; i++)
        {
            addRandomPoint(WIDTH, HEIGHT);
        }

        agentField.setMBR(objects.getMBR());
        nearbyField.setMBR(objects.getMBR());
    }


        
    @Override
    public void start()
    {
        super.start();

        // position the agent at a random starting location
        agent = new Agent(random.nextInt(WIDTH), random.nextInt(HEIGHT));

        // Add the agent
        agentField.addGeometry(agent.getGeometry());

        schedule.scheduleRepeating(agent);

        schedule.scheduleRepeating(agentField.scheduleSpatialIndexUpdater(), 1, 1.0);
    }


    /** Used to create JTS geometries
     *
     * @see addRandomPoint
     * @see addRandomRectangle
     * @see addRandomLine
     */
    private GeometryFactory geometryFactory = new GeometryFactory();


    /** Add a random point to the objects layer
     *
     * @param width of the MBR
     * @param height of the MBR
     */
    private void addRandomPoint(final int width, final int height)
    {
        Point location = geometryFactory.createPoint(new Coordinate(random.nextInt(width), random.nextInt(height)));
        objects.addGeometry(new MasonGeometry(location));
    }
    

    /** Add a rectangle to within the given dimensions
     *
     * @param width of the MBR
     * @param height of the MBR
     */
    private void addRandomRectangle(final int width, final int height)
    {
        GeometricShapeFactory factory = new GeometricShapeFactory();

        // Randomly establish the lower left corner of the rectangle while
        // ensuring that the upper right corner doesn't go outside the area.
        Coordinate lowerLeft = new Coordinate(random.nextDouble() * (width - 5),
                random.nextDouble() * (height - 5));

        factory.setBase(lowerLeft);
        factory.setWidth(random.nextDouble() * 15);
        factory.setHeight(random.nextDouble() * 15);

        Polygon rectangle = factory.createRectangle();

        objects.addGeometry(new MasonGeometry(rectangle));

    }


    /** Add a line to within the given dimensions
     *
     * @param width of the MBR
     * @param height of the MBR
     */
    private void addRandomLine(final int width, final int height)
    {
        // We may have up to five line segments
        int numSegments = random.nextInt(NUM_LINE_SEGMENTS - 1) + 2;

        Coordinate coordinates[] = new Coordinate[numSegments];

        // Pick location of line start

        coordinates[0] = new Coordinate();

        coordinates[0].x = random.nextDouble() * WIDTH;
        coordinates[0].y = random.nextDouble() * HEIGHT;

        for (int i = 1; i < coordinates.length; i++)
        {
            // regenerate points until we get a point that's inside the boundary
            do
            {
                int goLeftOrRight = random.nextBoolean() ? -1 : 1;
                int goUpOrDown = random.nextBoolean() ? -1 : 1;

                coordinates[i] = new Coordinate();

                coordinates[i].x = coordinates[i - 1].x + random.nextDouble() * 10 * goLeftOrRight;
                coordinates[i].y = coordinates[i - 1].y + random.nextDouble() * 10 * goUpOrDown;

            } while (coordinates[i].x > WIDTH - 1 || coordinates[i].y > HEIGHT - 1 ||
                     coordinates[i].x < 0.0 || coordinates[i].y < 0.0);
        }

        LineString line = null;

        try
        {
            line = geometryFactory.createLineString(coordinates);
        } catch (Exception e)
        {
            System.err.println(e);
        }

        objects.addGeometry(new MasonGeometry(line));
    }


    public static void main(String[] args)
    {
        doLoop(NearbyWorld.class, args);
        System.exit(0);
    }
}
