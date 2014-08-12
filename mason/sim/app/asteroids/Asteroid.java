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

/** An Asteroid is an element representing, well, an Asteroid.   Asteroids may have a variety of shapes to choose
    from, and also various sizes.  Asteroids have random initial orientations and rotational velocity,
    limited by MAXIMUM_ROTATIONAL_VELOCITY.
*/

public class Asteroid extends Element
    {
    private static final long serialVersionUID = 1;

    /** The Asteroid's size, a number from 0 ... MAXIMUM_SIZE - 1.  This size indicates
        the set of valid shapes in the shapes[] array, and also which sizes to break into
        when breaking apart (from the breakMap[] array). */
    public int size;
    
    /** The default initial velocity of an Asteroid. */
    public static final double INITIAL_VELOCITY = 0.3;
        
    /** The maximum initial rotational velocity of an Asteroid. */
    public static final double MAXIMUM_ROTATIONAL_VELOCITY = Math.PI / 180.0 * 1;

    /** The maximum size of an asteroid.  Sizes are integers from 0 ... MAXIMUM_SIZE - 1. */
    public static final int MAXIMUM_SIZE = 4;
        
    /** The largest possible explosion force when breaking apart into smaller asteroids.  */
    public static final double MAXIMUM_EXPLOSION_FORCE = 1;

    /** The smallest possible explosion force when breaking apart into smaller asteroids.  */
    public static final double MINIMUM_EXPLOSION_FORCE = 0.5;
        
    /** Creates an asteroid, and adds it to the field and to the schedule.  */
    public Asteroid(Asteroids asteroids, int size, MutableDouble2D velocity, Double2D location)
        {
        this.size = size;
        this.velocity = velocity;
        stopper = asteroids.schedule.scheduleRepeating(this);
        shape = shapes[size][asteroids.random.nextInt(shapes[size].length)];
        rotationalVelocity = asteroids.random.nextDouble() * MAXIMUM_ROTATIONAL_VELOCITY *
            (asteroids.random.nextBoolean() ? 1.0 : -1.0);
        orientation = asteroids.random.nextDouble() * Math.PI * 2;
        asteroids.field.setObjectLocation(this, location);
        asteroids.asteroidCount++;
        }
        
    /** Arrays of possible asteroid shapes, one array per size. */
    final static Shape[][] shapes = new Shape[/* Size */][/* Various Shapes */]
    {
    { new Rectangle.Double(-3,-3,5,5) },
        { new Rectangle.Double(-4,-4,8,8) },
        { new Rectangle.Double(-4,-4,8,8) },
        { new Rectangle.Double(-4,-4,9,9) },
        { new Rectangle.Double(-5,-5,10,10) },
    };
        
    /** Maps of possible ways asteroids may break up.  A Map is an array of groups of Asteroids, one Map per size. */
    // list the smaller asteroids first, so the largest one incurs the resulting left-over force
    final static int[/*My size*/][/*breakUpNumber*/][/*Children*/] breakMap = new int[][][]
    {
    { { } },                                                                // 0
        { { 0, 0 } },                                                   // 1
        { { 0, 1 }, { 0, 0, 0 } },                              // 2
        { { 0, 0, 1 }, { 0, 2 }, { 1, 1 } },    // 3
        { { 1, 2 }, { 0, 3 }, { 0, 1, 1 } }             // 4
    };
        
    /** Breaks up an Asteroid into smaller Asteroids, removing the original Asteroid and inserting
        the new Asteroids in the field.  If there are no asteroids to break into, the asteroid
        is simply replaced with Shards.  Smaller Asteroids are given an appropriate velocity
        which combines the current Asteroid's velocity plus a velocity caused by the explosion force. */
    public void breakApart(final Asteroids asteroids)
        {
        Double2D location = asteroids.field.getObjectLocation(this);
        int[] sizes = breakMap[size][asteroids.random.nextInt(breakMap[size].length)];
                
        if (sizes.length > 0)
            {
            // break into asteroids
            int sum = 0;
            for(int i = 0; i < sizes.length; i++)
                sum += sizes[i];
                        
            // compute velocities
            double explosionForce = asteroids.random.nextDouble() * (MAXIMUM_EXPLOSION_FORCE - MINIMUM_EXPLOSION_FORCE) + MINIMUM_EXPLOSION_FORCE;
            double sumForceX = 0;
            double sumForceY = 0;
            for(int i = 0; i < sizes.length; i++)
                {
                double angle = asteroids.random.nextDouble() * Math.PI * 2;
                double force = explosionForce / sizes.length;
                double forceX = force * Math.cos(angle);
                double forceY = force * Math.sin(angle);
                if (i == sizes.length - 1)
                    { forceX = -sumForceX; forceY = -sumForceY; }  // last one ought to balance out the others.  It's best if it's the biggest one, hence why we list smaller asteroids first
                else { sumForceX += forceX; sumForceY += forceY; }
                // yes, this is a dead store
                Asteroid a = new Asteroid(asteroids, sizes[i], new MutableDouble2D(velocity.x + forceX, velocity.y + forceY), location);
                }
            }
        else
            {
            breakIntoShards(asteroids);
            }
        end(asteroids);
        asteroids.asteroidCount--;
        if (asteroids.asteroidCount <= 0)
            {
            asteroids.schedule.scheduleOnceIn(asteroids.WAIT_PERIOD, new Steppable()
                {
                public void step(SimState state)
                    {
                    asteroids.createAsteroids();
                    }
                });
            }
        }
    }
