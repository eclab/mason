/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

// Class Bot
package sim.app.keepaway;
import java.awt.*;
import sim.portrayal.*;
import sim.util.*;
import sim.engine.*;

public /*strictfp*/ class Bot extends Entity implements Steppable
    {
    public Bot( final double x, final double y, Color c)
        {
        super(x,y,2,c);
        }

    public void draw(Object object,  final Graphics2D g, final DrawInfo2D info )
        {
        // draw the circle
        super.draw(object, g,info);
        
        // draw our line as well
        
        final double width = info.draw.width * radius * 2;
        final double height = info.draw.height * radius * 2;
            
        g.setColor(Color.white);
        double d = velocity.angle();
        g.drawLine((int)info.draw.x,
            (int)info.draw.y,
            (int)(info.draw.x) + (int)(width/2 * /*Strict*/Math.cos(d)),
            (int)(info.draw.y) + (int)(height/2 * /*Strict*/Math.sin(d)));
        }


    public MutableDouble2D tempVector = new MutableDouble2D();
        
    public MutableDouble2D getForces( final Keepaway keepaway)
        {
        sumVector.setTo(0,0);
        Bag objs = keepaway.fieldEnvironment.getObjectsWithinDistance(new Double2D(loc.x, loc.y), 100);

        double dist = 0;

        //http://www.martinb.com/physics/dynamics/collision/twod/index.htm
        double mass1;
        double mass2;

        for(int x=0; x<objs.numObjs; x++)
            {
            if(objs.objs[x] != this)
                {               
                dist = ((Entity)objs.objs[x]).loc.distance(loc);
                    
                if((((Entity)objs.objs[x]).radius + radius)*1.25 > dist)  // collision!
                    {
                    // 10% chance of kicking the ball, if it's a ball
                    // and kicking is not especially interesting.. its just accelerated impact
                    if(objs.objs[x] instanceof Ball && keepaway.random.nextDouble() < .1)
                        {
                        tempVector.subtract(((Entity)objs.objs[x]).loc, loc);
                        tempVector.normalize().multiplyIn(2.0);
                        ((Entity)objs.objs[x]).velocity.addIn(tempVector);
                        }
                    else        // else just ram it...
                        {               // shouldnt matter what type of object collision occurrs with
                        tempVector.x = 0;
                        tempVector.y = 0;
                        
                        mass1 = mass - ((Entity)objs.objs[x]).mass;
                        mass1 /= (mass + ((Entity)objs.objs[x]).mass);
                        
                        mass2 = 2 * ((Entity)objs.objs[x]).mass;
                        mass2 /= (mass + ((Entity)objs.objs[x]).mass);
                        
                        // self = object a
                        tempVector.x = velocity.x * mass1 + ((Entity)objs.objs[x]).velocity.x * mass2;
                        tempVector.y = velocity.y * mass1 + ((Entity)objs.objs[x]).velocity.y * mass2;
                        
                        // collided object = object 
                        ((Entity)objs.objs[x]).bump.x = velocity.x * mass2 - ((Entity)objs.objs[x]).velocity.x * mass1;
                        ((Entity)objs.objs[x]).bump.y = velocity.y * mass2 - ((Entity)objs.objs[x]).velocity.y * mass1;
                        
                        velocity.x = tempVector.x;
                        velocity.y = tempVector.y;
                        }
                    }
                else if(objs.objs[x] instanceof Ball)
                    {
                    // if we didn't hit the ball, we want to go towards it
                    tempVector.subtract(((Entity)objs.objs[x]).loc, loc);
                    tempVector.setLength(0.5);
                    sumVector.addIn(tempVector);
                    }
                }
            }
        // bump forces
        sumVector.addIn(bump);
        bump.x = 0;
        bump.y = 0;
        return sumVector;
        }
 
        
    public void step( final SimState state )
        {
        Keepaway keepaway = (Keepaway)state;
        
        // get force
        final MutableDouble2D force = getForces(keepaway);
        
        // acceleration = f/m
        accel.multiply(force, 1/mass); // resets accel
        
        // v = v + a
        velocity.addIn(accel);
        capVelocity();
        
        // L = L + v
        newLoc.add(loc,velocity);  // resets newLoc
        
        // is new location valid?
        if(isValidMove(keepaway, newLoc))
            loc = newLoc;
        
        keepaway.fieldEnvironment.setObjectLocation(this, new Double2D(loc));
        }
    }
