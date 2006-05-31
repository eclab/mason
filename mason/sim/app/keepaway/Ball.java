/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

// Class Ball
package sim.app.keepaway;
import java.awt.*;
import sim.util.*;
import sim.engine.*;

public /*strictfp*/ class Ball extends Entity implements Steppable
    {
    // used to determine if the ball is stuck
    public MutableDouble2D stillPos;                           // last position
    public double dt;                                   // delta time--how many steps it has been still

    public Ball( final double x, final double y)
        {
        super(x,y,1,Color.white);
        
        cap = 2.0;
        
        bump = new MutableDouble2D(0,0);
        stillPos = new MutableDouble2D(0,0);
        dt = 0;
        }
 
        

    public MutableDouble2D getForces( final Keepaway keepaway)
        {
        sumVector.setTo(0,0);
        Bag objs = keepaway.fieldEnvironment.getObjectsWithinDistance(new Double2D(loc.x, loc.y), 100);

        double dist = 0;

        for(int x=0; x<objs.numObjs; x++)
            {
            if(objs.objs[x] != this)
                {              
                dist = ((Entity)objs.objs[x]).loc.distance(loc);
                    
                if((((Entity)objs.objs[x]).radius + radius) > dist)  // collision!
                    {
                    if(objs.objs[x] instanceof Ball)
                        {
                        // ball
                        // actually this is not possible with current settings
                        }
                    else // if(objs.objs[x] instanceof Ball)
                        {
                        // bot
                        // and this is handled by the bots themselves
                        }
                    }
                }
            }
        
        // add bump vector
        sumVector = sumVector.addIn(bump);
        bump.x = 0;
        bump.y = 0;
        return sumVector;
        }
    
    MutableDouble2D friction = new MutableDouble2D();
    MutableDouble2D stuckPos = new MutableDouble2D();
        
    public void step( final SimState state )
        {
        Keepaway keepaway = (Keepaway)state;
        
        // get force
        final MutableDouble2D force = getForces(keepaway);
        
        // acceleration = f/m
        accel.multiply(force, 1/mass); // resets accel
                
        // hacked friction
        friction.multiply(velocity, -0.025);  // resets friction
        
        // v = v + a
        velocity.addIn(accel);
        velocity.addIn(friction);
        capVelocity();
        
        // L = L + v
        newLoc.add(loc,velocity);  // resets newLoc
        
        // is new location valid?
        if(isValidMove(keepaway, newLoc))
            {
            loc = newLoc;
            }
        
        // check if ball hasn't moved much
        if(loc.distanceSq(stuckPos) < (0.1*0.1))
            dt++;
        else
            {
            dt = 0;
            stuckPos.setTo(loc);
            }
                
        // might be stuck...  move to random location!
        if(dt > 1000)
            {
            System.out.println("stuck");
            dt = 0;
            stuckPos.setTo(loc);
            loc.x = keepaway.random.nextDouble()*keepaway.xMax;
            loc.y = keepaway.random.nextDouble()*keepaway.yMax;
            }
                    
        keepaway.fieldEnvironment.setObjectLocation(this, new Double2D(loc));
        }
    }
