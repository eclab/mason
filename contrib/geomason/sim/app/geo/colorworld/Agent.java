/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package colorworld;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomVectorField;

/**
 *  Our simple agent for the ColorWorld GeoMASON example.  The agents move in one of the eight cardinal directions 
 *  until they hit the boundary of Fairfax County.  Then, they choose a random direction, and repeat.   
 *
 */

public class Agent implements Steppable {

    private static final long serialVersionUID = -5318720825474063385L;
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

    // How much to move the agent by in each step()
    double moveRate = 100.0;
     
    public Agent(int d)
    {
        direction = d;
    }
            
    public void setLocation(Point p) { location = p; }

    public Geometry getGeometry() { return location; }
    
    public void step(SimState state)
    {
        // try to move the agent, keeping the agent inside its political region
                
    	ColorWorld cState = (ColorWorld)state; 
        GeomVectorField world = cState.county;
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
        if (world.isInsideUnion(coord))  { 
        	//cState.county.updateTree(location, translate); 
        	location.apply(translate);
        }
        else // try randomly moving in different direction if trying to stray
            direction = state.random.nextInt(8);
    }
}
