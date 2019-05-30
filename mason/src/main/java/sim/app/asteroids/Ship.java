/*
  Copyright 2009 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.asteroids;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import ec.util.*;
import java.awt.geom.*;
import java.awt.*;
import sim.portrayal.*;

/** A Ship is an element representing the user's space ship.   Ships have tags indicating
    the array slot they should consult in the Asteroids.actions array -- this makes it possible to
    have more than one ship controlled by more than one user.  By default we have that turned
    off but it's easy to turn on in AsteroidsWithUI.
        
    <p>Ships are capable of shooting bullets and can collide with asteroids (and break into shards
    as a result).  Bullets can only be shot once every BULLET_COUNTDOWN steps.  Ships can move forward,
    turn left, turn right, fire, and warp into hyperspace (move to a random location).  
        
    <p>Ships are unusual in that each Ship has a <i>subsidiary</i> Element called a Fire, which draws
    the "fire" of the thruster when the Ship is moving forward.
        
    <p>Ships always have a zero rotational velocity, and are rather rotated by the user.
*/


public class Ship extends Element
    {
    private static final long serialVersionUID = 1;

    /** The ship's tag. */
    public int tag;
        
    /** The ship's Fire. */
    public Fire fire = new Fire();

    // how long we have to wait to be able to fire another bullet
    int bulletCountdown;

    /** Minimum length of time between bullets. */
    public static final int BULLET_COUNTDOWN = 10;
        
    /** An approximation of the radius of the ship body, mostly used to determine where to place the initial bullet so it doesn't impact on the ship. */
    public static final double MAXIMUM_RADIUS = 6;
        
    /** How much to translate each step. */
    public static final double VELOCITY_INCREMENT = 0.03;
        
    /** How much to rotate each step. */
    public static final double ORIENTATION_INCREMENT = 0.05;

    /** How many steps we should draw the Fire after thrusting a single time. */
    public static final int MAXIMUM_THRUST_DRAW_LENGTH = 5;

    public static final int NOTHING = 0;
    public static final int LEFT = 1;
    public static final int FORWARD = 2;
    public static final int RIGHT = 4;
    public static final int FIRE = 8;
    public static final int HYPERSPACE = 16;
    
    /** Makes a ship, and adds it to the field and the schedule. */
    public Ship(Asteroids asteroids, MutableDouble2D velocity, Double2D location, int tag)
        {
        this.velocity = velocity;
        this.rotationalVelocity = 0;
        stopper = asteroids.schedule.scheduleRepeating(this);
        orientation = asteroids.random.nextDouble() * Math.PI * 2;
        asteroids.field.setObjectLocation(this, location);
        GeneralPath gp = new GeneralPath();
        gp.moveTo(-2,-2);
        gp.lineTo(2,0);
        gp.lineTo(-2,2);
        gp.lineTo(0, 0);
        // gp.lineTo(-2,-2);
        gp.closePath();
        shape = gp;
        this.tag = tag;
        }
        
    /** Shoots a bullet if the countdown permits it. */
    public void shoot(Asteroids asteroids)
        {
        if (bulletCountdown <= 0)
            {
            bulletCountdown = BULLET_COUNTDOWN;
            MutableDouble2D v2 = new MutableDouble2D(velocity);
            v2.x += Bullet.VELOCITY * Math.cos(orientation);
            v2.y += Bullet.VELOCITY * Math.sin(orientation);
            Double2D location = asteroids.field.getObjectLocation(this);
            Double2D l2 = new Double2D(location.x + (MAXIMUM_RADIUS + 1) * Math.cos(orientation),
                location.y + (MAXIMUM_RADIUS + 1) * Math.sin(orientation));
            // yes this is a dead store
            Bullet b = new Bullet(asteroids, v2, l2, Bullet.LIFETIME);
            }
        }
        
    // thrust drawing countdown
    int thrust = 0;

    /** Step the ship.  This first changes the velocity or orientation of the ship, or relocates 
        it via hyperspace, or fires a bullet (or all of these things!).  Then we move the ship
        and rotate it by calling super.step().  Then we test for collisions and end ourselves if so.
        Finally, we decrease the bullet countdown by one. */
                
    public void step(SimState state)
        {
        Asteroids asteroids = (Asteroids)state;
                
        // Collect data
        if ((asteroids.actions[tag] & FORWARD) == FORWARD)
            {
            velocity.x += Math.cos(orientation) * VELOCITY_INCREMENT;
            velocity.y += Math.sin(orientation) * VELOCITY_INCREMENT;
            thrust = MAXIMUM_THRUST_DRAW_LENGTH;
            }
        if ((asteroids.actions[tag] & LEFT) == LEFT)
            {
            orientation -= ORIENTATION_INCREMENT;
            }
        if ((asteroids.actions[tag] & RIGHT) == RIGHT)
            {
            orientation += ORIENTATION_INCREMENT;
            }
        if ((asteroids.actions[tag] & FIRE) == FIRE)
            {
            shoot(asteroids);
            asteroids.actions[tag] &= ~FIRE;  // force the user to have to repeatedly press the button
            }
        if ((asteroids.actions[tag] & HYPERSPACE) == HYPERSPACE)
            {
            asteroids.field.setObjectLocation(this, 
                new Double2D(asteroids.random.nextDouble() * asteroids.field.width,
                    asteroids.random.nextDouble() * asteroids.field.height));
            asteroids.actions[tag] &= ~HYPERSPACE;  // force the user to have to repeatedly press the button
            }
                
        super.step(state);  // move object

        // did I collide?
        testForHit(asteroids);
        bulletCountdown--;
        }


    /** Ships are green. */
    public Color getColor() { return Color.green; }

    /** Draws the ship *and* optionally the Fire. */
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        super.draw(object, graphics, info);
                
        // also need to draw fire
        if (thrust > 0)
            {
            fire.orientation = orientation;
            fire.draw(object, graphics, info);
            }
        thrust--;
        }
                
    void respawn(final Asteroids asteroids)
        {
        asteroids.schedule.scheduleOnceIn(asteroids.WAIT_PERIOD, new Steppable()
            {
            public void step(SimState state)
                {
                // check if it's safe
                Double2D respawnLocation = new Double2D(asteroids.width / 2, asteroids.height / 2);
                Bag o = asteroids.field.getAllObjects();
                boolean safe = true;
                for(int i = 0; i < o.numObjs; i++)
                    {
                    Double2D loc = asteroids.field.getObjectLocation(o.objs[i]);
                    if (loc.distance(respawnLocation) < asteroids.SAFE_DISTANCE) // uh oh
                        { safe = false; break; }
                    }
                                        
                // build the ship if it's safe, else respawn again
                if (safe)
                    asteroids.createShip(tag);
                else respawn(asteroids);
                }
            });
        }
                
    /** Tests if we've collided with an asteroid or a bullet.  If so, we end ourselves and them,
        and break into shards.  Asteroids also break apart. */
    public void testForHit(final Asteroids asteroids)
        {
        // just check ALL the asteroids, yes it's expensive but there aren't enough of them to matter
        Bag a = asteroids.field.getAllObjects();
        for(int i = 0; i < a.numObjs; i++)
            {
            Object obj = (a.objs[i]);
            if (obj instanceof Asteroid)
                {
                Asteroid asteroid = (Asteroid)(a.objs[i]);
                if (asteroid.collisionWithElement(asteroids, this))
                    {
                    asteroid.breakApart(asteroids);
                    this.breakIntoShards(asteroids);
                    asteroids.score++;
                    asteroids.deaths++;
                    asteroids.ships[tag] = null;
                    end(asteroids);
                    respawn(asteroids);
                    break;
                    }
                }
                        
            /*   // BY DEFAULT THIS IS TURNED OFF -- it makes gameplay yucky 
                 else if (obj instanceof Bullet)
                 {
                 Bullet bullet = (Bullet)(a.objs[i]);
                 if (bullet.collisionWithElement(asteroids, this))
                 {
                 bullet.end(asteroids);
                 this.breakIntoShards(asteroids);
                 end(asteroids);
                 break;
                 }
                 }
            */
            }
        }
    }
