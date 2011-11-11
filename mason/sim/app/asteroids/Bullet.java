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

/** A Bullet is an element representing a bullet shot from a Ship.   Bullets have a finite lifetime
    which is set up when they are constructed, after which they end themselves automatically.
    When a Bullet collides with an Asteroid or a Ship, it causes a collision.  Bullets have zero
    rotational velocity.
*/

public class Bullet extends Element 
    {
    private static final long serialVersionUID = 1;

    /** How much to translate the Bullet each timestep. */ 
    public static final double VELOCITY = 1;

    /** The default lifetime of a bullet.  */
    public static final int LIFETIME = 100;
        
    /** Constructs a Bullet and adds it to the field and to the schedule. */
    public Bullet(Asteroids asteroids, MutableDouble2D velocity, Double2D location, int lifetime)
        {
        this.shape = new Ellipse2D.Double(-1,-1,2,2);
        this.velocity = velocity;
        this.rotationalVelocity = 0;
        stopper = asteroids.schedule.scheduleRepeating(this);
        asteroids.field.setObjectLocation(this, location);
        asteroids.schedule.scheduleOnceIn(lifetime, 
            new Steppable()
                {
                public void step(SimState state)
                    {
                    end((Asteroids) state);
                    } 
                });
        }
        
    /** Tests for collisions, then moves the Bullet appropriately. */
    public void step(SimState state)
        {
        testForHit((Asteroids)state);
        super.step(state);
        }
        
    /** Bullets are white. */
    public Color getColor() { return Color.white; }

    /** Tests if we've collided with an asteroid.  If so, we break apart the Asteroid and end ourselves.
        Ships test for collision with Bullets on their own, not here. */
    public void testForHit(Asteroids asteroids)
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
                    end(asteroids);
                    asteroid.breakApart(asteroids);
                    asteroids.score++;
                    break;
                    }
                }
            }
        }
    }
