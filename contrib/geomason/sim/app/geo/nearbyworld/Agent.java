/*
 * Agent.java
 *
 * The agent that will be moving around the GeomVectorField.  It will report
 * all other objects that are within a certain distance.
 *
 * $Id: Agent.java,v 1.3 2010-08-20 00:52:36 mcoletti Exp $
 */

package sim.app.geo.nearbyworld;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomVectorField;
import sim.util.Bag;

/** Agent that moves through GeomVectorField
 *
 * It will move randomly within a bounds.
 *
 * @author mcoletti
 */
public class Agent implements Steppable {

    // point that denotes agent's position
    private Point location;

    // How much to move the agent by in each step()
    private static double moveRate = 1.0;

    private static double distance = 10.0;

    public Agent()
    {
        GeometryFactory fact = new GeometryFactory();
        location = fact.createPoint(new Coordinate(25,25)); // XXX magic numbers
        System.out.println("agent: " + location);
    }

    /** return geometry representing agent location
     * 
     * @return geometry of location
     */
    public Geometry getGeometry()
    {
        return location;
    }
    

    public void step(SimState state)
    {
        move(state.random);

        // Now determine if we're covered by something in the world or not.
        GeomVectorField world = ((NearbyWorld)state).world;

        Bag nearbyObjects = world.getObjectsWithinDistance(location, Agent.distance);

        if (nearbyObjects.isEmpty())
            System.out.println("Nothing nearby");
        else
            {
                System.out.println("# nearby objects: " + nearbyObjects.numObjs);
                for (int i = 0; i < nearbyObjects.numObjs; i++)
                    System.out.println(nearbyObjects.objs[i] + " is near me");
            }
    }

    /** returns false if the given point is outside the bounds, else true
     *
     */
    private boolean isValidMove(final Coordinate c)
    {
        // XXX Uses magic numbers.  :(
        if (c.x < 0.0 || c.x > 100.0 ||
            c.y < 0.0 || c.y > 100.0)
            {
                return false;
            }

        return true;
    }

    /** move the agent in a random direction within a bounds
     * 
     */
    private void move(MersenneTwisterFast random)
    {
        Coordinate coord = (Coordinate) location.getCoordinate().clone();

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
                location.apply(translate);
                System.out.println("agent:" + location);
            }        
    }
}
