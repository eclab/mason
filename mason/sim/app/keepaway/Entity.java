/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

// Class Entity
package sim.app.keepaway;
import sim.util.*;
import sim.portrayal.simple.*;
import java.awt.*;

public abstract /*strictfp*/ class Entity extends OvalPortrayal2D
    {
    public MutableDouble2D loc, velocity, bump;
    public MutableDouble2D force = new MutableDouble2D();
    public MutableDouble2D accel = new MutableDouble2D();
    public MutableDouble2D newLoc = new MutableDouble2D();
    public MutableDouble2D sumVector = new MutableDouble2D(0,0);

    public double speed, radius;
    
    public double cap;
    
    public double mass;
        
    // Accessors for inspector
    public double getX() { return loc.x; }
    public void setX( double newX ) { loc.x = newX; }
    
    public double getY() { return loc.y; }
    public void setY( double newY ) { loc.y = newY; }
    
    public double getVelocityX() { return velocity.x; }
    public void setVelocityX( double newX ) { velocity.x = newX; }
    
    public double getVelocityY() { return velocity.y; }
    public void setVelocityY( double newY ) { velocity.y = newY; }
 
    public double getSpeed() { return speed; }
    public void setSpeed( double newSpeed ) { speed = newSpeed; }   
    
    public double getRadius() { return radius; }
    public void setRadius( double newRadius ) 
        {
        radius = newRadius;
        scale = 2 * radius;  // so our ovalportrayal knows how to draw/hit us right
        } 
    
    public double getMass() { return mass; }
    public void setMass( double newMass ) { mass = newMass; } 
    
    // Constructor
    public Entity( double newX, double newY, double newRadius, Color c )
        {
        super(c, newRadius * 2);  // scale is twice the radius
        
        loc = new MutableDouble2D(newX, newY);
        velocity = new MutableDouble2D(0, 0);
        bump = new MutableDouble2D(0, 0);
        radius = newRadius;
        
        mass = 1.0;
        cap = 1.0;
        
        speed = 0.4;
        }
    
    public boolean isValidMove( final Keepaway keepaway, final MutableDouble2D newLoc)
        {
        Bag objs = keepaway.fieldEnvironment.getObjectsWithinDistance(new Double2D(loc.x, loc.y), 10);

        double dist = 0;

        // check objects
        for(int x=0; x<objs.numObjs; x++)
            {
            if(objs.objs[x] != this)
                {
                dist = ((Entity)objs.objs[x]).loc.distance(newLoc);

                if((((Entity)objs.objs[x]).radius + radius) > dist)  // collision!
                    return false;
                }
            }

        // check walls
        if(newLoc.x > keepaway.xMax)
            {
            if (velocity.x > 0) velocity.x = -velocity.x;
            return false;
            }
        else if(newLoc.x < keepaway.xMin)
            {
            if (velocity.x < 0) velocity.x = -velocity.x;
            return false;
            }
        else if(newLoc.y > keepaway.yMax)
            {
            if (velocity.y > 0) velocity.y = -velocity.y;
            return false;
            }
        else if(newLoc.y < keepaway.yMin)
            {
            if (velocity.y < 0) velocity.y = -velocity.y;
            return false;
            }
        
        // no collisions: return, fool
        return true;
        }
    
    public void capVelocity()
        {
        if(velocity.length() > cap)
            velocity = velocity.setLength(cap);
        }
    
    }
