/*
 * Agent.java
 *
 * An agent that will be moving within a county political region
 *
 * $Id: Agent.java,v 1.1 2010-04-05 17:21:53 mcoletti Exp $
 */

package sim.app.geo.colorworld;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.*;
import sim.engine.*;
import sim.field.geo.*;

/** Agent that moves through GeomField
 *
 * It will move randomly within a bounds.
 *
 * @author mcoletti
 */
public class Agent implements Steppable {

	// possible directions of movement
	final int N  = 0; 
	final int NW = 1; 
	final int W  = 2;
	final int SW = 3;
	final int S  = 4;
	final int SE = 5; 
	final int E  = 6; 
	final int NE = 7;

    // Current direction the agent is moving
    int direction;
    
    // agent's position
	Point location = null;

    // agent's politcal region
    Polygon region = null;

    // How much to move the agent by in each step()
	double moveRate = 100.0;

    // We reuse this geometry factory to convert coordinates to points
//	GeometryFactory geometryFactory = new GeometryFactory();


	
    public Agent(int d, Polygon r)
    {
		direction = d;
        region = r;
    }
	
	public void setLocation(Point p) { location = p; }

    public Geometry getGeometry() { return location; }
    
    public void step(SimState state)
    {
		// try to move the agent, keeping the agent inside its political region
		
        GeomField world = ((ColorWorld)state).county;
        Coordinate coord = (Coordinate) location.getCoordinate().clone();
        AffineTransformation translate = null;

        switch (direction)
        {
            case N : // move up
                translate = AffineTransformation.translationInstance(0.0, moveRate);
                coord.y += moveRate;
                break;
            case S : // move down
                translate = AffineTransformation.translationInstance(0.0, -moveRate);
                coord.y -= moveRate;
                break;
            case E : // move right
                translate = AffineTransformation.translationInstance(moveRate, 0.0);
                coord.x += moveRate;
                break;
            case W : // move left
                translate = AffineTransformation.translationInstance(-moveRate, 0.0);
                coord.x -= moveRate;
                break;
            case NW : // move upper left
                translate = AffineTransformation.translationInstance(-moveRate,moveRate);
                coord.x -= moveRate;
				coord.y += moveRate; 
                break;
            case NE : // move upper right
                translate = AffineTransformation.translationInstance( moveRate, moveRate );
                coord.x += moveRate;
				coord.y += moveRate;
                break;
            case SW : // move lower left
                translate = AffineTransformation.translationInstance(-moveRate, -moveRate);
                coord.x -= moveRate;
				coord.y -= moveRate;
                break;
            case SE : // move lower right
                translate = AffineTransformation.translationInstance( moveRate, -moveRate);
                coord.x += moveRate;
				coord.y -= moveRate;
                break;
		}

		// is the new position still within the county?
        //if (world.isCovered(coord))
		//if (world.isInsideConvexHull(coord)) 
		if (world.isInsideUnion(coord)) 
//        if (SimplePointInAreaLocator.containsPointInPolygon(coord, region))
            location.apply(translate);
        else // try randomly moving in different direction if trying to stray
            direction = state.random.nextInt(8);
    }
}
