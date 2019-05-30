/*
  Copyright 2009 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.asteroids;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import java.awt.geom.*;


/** Asteroids is the SimState of the game.  It contains a single field, which holds all the
    Elements in the game (Ships, Asteroids, Bullets, Shards), plus an array of actions[] which
    tell Ships what their users want them to do.  
*/

public class Asteroids extends SimState
    {
    private static final long serialVersionUID = 1;

    /** How long, in ticks, should we wait after the asteroids and/or ship have been
        entirely destroyed before resetting? */
    public static final int WAIT_PERIOD = 60;
        
    /** How far should a ship or asteroid be placed from one another so as not to be
        too unfair on the user? */
    public static final double SAFE_DISTANCE = 30;
        
    /** Field of all Elements in the game. */
    public Continuous2D field;
        
    /** Width of the field. */
    public double width = 150;
        
    /** Height of the field. */
    public double height = 150;
        
    /** Number of initial asteroids. */
    public int numAsteroids = 5;
        
    /** Number of initial ships.  */
    public int numShips= 1;
        
    /** Current score. */
    public int score = 0;
        
    /** Lives lost. */
    public int deaths = 0;
        
    /** Level. */
    public int level = 0;
        
    /** Remaining Asteroids */
    public int asteroidCount = 0;

    /** Array of actions, one per ship, indicating what the user wishes the ship to do.
        Actions are ORed constants: the LEFT, FORWARD, RIGHT, FIRE, and HYPERSPACE
        constants in the Ship class. */
    public int actions[] = new int[numShips];
        
    /** Ships */
    public Ship[] ships = new Ship[numShips];

    /** Creates a Asteroids simulation with the given random number seed. */
    public Asteroids(long seed)
        {
        super(seed);
        }

    /** Generate Ship */
    public void createShip(int tag)
        {
        // make the ship
        ships[tag] = new Ship(this, new MutableDouble2D(0,0), new Double2D(width / 2, height/ 2), tag);
        }

    /** Generate Asteroids */
    public void createAsteroids()
        {
        level++;
        // make a bunch of asteroids on the periphery
        for(int x=0;x<numAsteroids;x++)
            {
            double angle = random.nextDouble() * Math.PI * 2;
            Double2D loc = null;
            for(int i = 0; i < 1000; i++)  // try 1000 times, so as to break theoretical deadlock
                {
                if (random.nextBoolean())
                    loc = new Double2D(0, random.nextDouble() * height);
                else
                    loc = new Double2D(random.nextDouble() * width, 0);
                boolean bad = false;
                for(int j = 0; j < numShips; j++)
                    if (ships[j] != null && field.getObjectLocation(ships[j]).distance(loc) < SAFE_DISTANCE)
                        { bad = true; break; }
                if (!bad) break;
                }
            new Asteroid(this, Asteroid.MAXIMUM_SIZE, new MutableDouble2D(
                    Asteroid.INITIAL_VELOCITY * Math.cos(angle),
                    Asteroid.INITIAL_VELOCITY * Math.sin(angle)),
                loc);
            }
        }

    /** Starts the Asteroids simulation. */
    public void start()
        {
        super.start();
        
        // set up the asteroids fields
        field = new Continuous2D(width,width,height);
                
        for(int x=0;x < numShips; x++)
            createShip(x);
        createAsteroids();
        }

    /** Default main loop for MASON. */
    public static void main(String[] args)
        {
        doLoop(Asteroids.class, args);
        System.exit(0);
        }    
    }
