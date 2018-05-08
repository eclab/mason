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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

/** 
 * Simple agent for the NearbyWorld GeoMASON example.  The agent wanders randomly around the field, 
 * and at every step, prints out the all objects that are within <i>DISTANCE</i> units.
 * 
 */
public class Agent implements Steppable
{

    private static final long serialVersionUID = -7366623247320036818L;

	// point that denotes agent's position
    private MasonGeometry location;

    // How much to move the agent by in each step()
    private static double moveRate = 2.0;

    // DISTANCE for determining if objects are close
    public static double DISTANCE = 20.0;

    private static GeometryFactory fact = new GeometryFactory();

    public Agent()
    {
    	this(25, 25); 
    }
    
    public Agent(int x, int y)
    {
        location = new MasonGeometry( fact.createPoint(new Coordinate(x,y)) );
//        System.out.println("agent: " + location);
    }

    // return geometry representing agent location
    public MasonGeometry getGeometry()
    {
        return location;
    }
    

    public void step(SimState state)
    {
        NearbyWorld world = (NearbyWorld) state;

        // Clear out any objects previously identififed as being near
        world.nearbyField.clear();

        // Move the agent to a random valid location within the field.
        move(state.random);

        // Now determine if we're near something in objects
        Bag nearbyObjects = world.objects.getObjectsWithinDistance(location, Agent.DISTANCE);

        if (nearbyObjects.isEmpty())
        {
//            System.out.println("Nothing nearby");
        } else
        {
//            System.out.println("# nearby objects: " + nearbyObjects.numObjs);
            
            for (int i = 0; i < nearbyObjects.size(); i++)
            {
//                System.out.println(nearbyObjects.objs[i] + " is near me");
                world.nearbyField.addGeometry((MasonGeometry) nearbyObjects.objs[i]);
            }

            // nearbyField.clear() and all the addGeometry() will have reset the MBR
            // to something that doesn't match the MBR of the other layers;
            // if we leave it alone then the nearbyField objects won't be
            // rendered in their proper place in the display.  We must sync
            // the MBR with that of the other layers.  You can
            // comment out the following line to see the effect of not
            // synchronizing the MBR.
            world.nearbyField.setMBR(world.objects.getMBR());
        }
    }

    

    // returns false if the given point is outside the bounds, else true
    private boolean isValidMove(final Coordinate c)
    {
        // Uses magic numbers.  :(
        if (c.x < 0.0 || c.x > NearbyWorld.WIDTH ||
            c.y < 0.0 || c.y > NearbyWorld.HEIGHT)
            {
                return false;
            }

        return true;
    }

    // move the agent in a random direction within a bounds
    private void move(MersenneTwisterFast random)
    {
        Coordinate coord = (Coordinate) location.geometry.getCoordinate().clone();

        int direction = random.nextInt(8);

        AffineTransformation translate = null;

        switch (direction)
            {
            case 0 : // move up
                translate = AffineTransformation.translationInstance(0.0, moveRate);
                coord.y += Agent.moveRate;
                break;
            case 1 : // move down
                translate = AffineTransformation.translationInstance(0.0, -moveRate);
                coord.y -= Agent.moveRate;
                break;
            case 2 : // move right
                translate = AffineTransformation.translationInstance(moveRate, 0.0);
                coord.x += Agent.moveRate;
                break;
            case 3 : // move left
                translate = AffineTransformation.translationInstance(- moveRate, 0.0);
                coord.x -= Agent.moveRate;
                break;
            case 4 : // move upper left
                translate = AffineTransformation.translationInstance(- moveRate,moveRate);
                coord.x -= Agent.moveRate;
                coord.y += Agent.moveRate;
                break;
            case 5 : // move upper right
                translate = AffineTransformation.translationInstance( moveRate, moveRate );
                coord.x += Agent.moveRate;
                coord.y += Agent.moveRate;
                break;
            case 6 : // move lower left
                translate = AffineTransformation.translationInstance(- moveRate, - moveRate);
                coord.x -= Agent.moveRate;
                coord.y -= Agent.moveRate;
                break;
            case 7 : // move lower right
                translate = AffineTransformation.translationInstance( moveRate, - moveRate);
                coord.x += Agent.moveRate;
                coord.y -= Agent.moveRate;
                break;
            default : // Ummm, what?
                break;
            }

        if (isValidMove(coord))
            {
                location.geometry.apply(translate);
//                System.out.println("agent:" + location);
            }        
    }
}
