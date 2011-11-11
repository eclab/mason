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

/** Shard is an Element representing a broken-apart piece of something dying.    Shards vary
    in color and are provided their color during construction.  Shards don't
    collide with anything, and automatically end after a certain countdown, during which
    they fade from their color to black.
*/

public class Shard extends Element 
    {
    private static final long serialVersionUID = 1;

    // current countdown of the Shard.  Used to determine the current color of the Shard.
    int count;
        
    // initial color of the Shard before it starts to fade
    Color color;
    
    /** Initial default velocity of a Shard */
    public static final double VELOCITY = 1;
        
    /** Default lifetime of a Shart. */
    public static final int LIFETIME = 100;
        
    /** The largest possible explosion force when breaking into Shards.  */
    public static final double MAXIMUM_EXPLOSION_FORCE = 0.5;

    /** The smallest possible explosion force when breaking into Shards.  */
    public static final double MAXIMUM_ROTATIONAL_VELOCITY = Math.PI / 180.0 * 5;
        
    /** Creates a Shard and adds it to the field and to the schedule. */
    public Shard(Asteroids asteroids, Shape shape, double orientation,
        MutableDouble2D velocity, Double2D location, Color color)
        {
        this.velocity = velocity;
        this.shape = shape;
        this.color = color;
        count = LIFETIME;
        stopper = asteroids.schedule.scheduleRepeating(this);
        rotationalVelocity = asteroids.random.nextDouble() * MAXIMUM_ROTATIONAL_VELOCITY *
            (asteroids.random.nextBoolean() ? 1.0 : -1.0);
        this.orientation = orientation;
        asteroids.field.setObjectLocation(this, location);
        asteroids.schedule.scheduleOnceIn(LIFETIME, 
            new Steppable()
                {
                public void step(SimState state)
                    {
                    end((Asteroids) state);
                    } 
                });
        }
        
    /** Steps the Shard, decreasing its count. */
    public void step(SimState state)
        {
        super.step(state);
        count--;
        }
                
    /** Shards vary in color.  They start with their given color, then gradually fade to transparent. */
    public Color getColor()
        {
        double v = count / (double)LIFETIME;
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * v));
        }
        
    }
