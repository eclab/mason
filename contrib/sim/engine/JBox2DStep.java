/*
  Copyright 2017 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

import sim.field.continuous.*;
import sim.util.*;
import org.jbox2d.callbacks.*;
import org.jbox2d.collision.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.serialization.*;
import org.jbox2d.collision.shapes.*;

/**
	A wrapper for JBox2D Worlds which steps them to keep them in sync with MASON.  When you create
	a JBox2DStep, you then add it to the Schedule to be stepped repeatedly every 1.0 timesteps.
	
	<p>When stepped, JBox2DStep will call world.step(...), passing in the current JBox2D timestep,
	velocity iterations, and position iterations.  These are by default 1/60, 6, and 2 respectively. 
**/


public class JBox2DStep implements Steppable
    {
    public static final float DEFAULT_TIMESTEP = 1f/60f;
    public static final int DEFAULT_VELOCITY_ITERATIONS = 6;
    public static final int DEFAULT_POSITION_ITERATIONS = 2;

    World world;
    float timestep;
    int velocityIterations;
    int positionIterations;
    
    public JBox2DStep(World world)
    	{
    	this.world = world;
    	timestep = DEFAULT_TIMESTEP;
    	velocityIterations = DEFAULT_VELOCITY_ITERATIONS;
    	positionIterations = DEFAULT_POSITION_ITERATIONS;
    	}
    
    public double getTimestep() { return timestep; }
    public void setTimestep(double val) { timestep = (float) val; }
    public int getVelocityIterations() { return velocityIterations; }
    public void setVelocityIterations(int val) { velocityIterations = val; }
    public int getPositionIterations() { return positionIterations; }
    public void setPositionIterations(int val) { positionIterations = val; }
    public World getWorld() { return world; }
    
    public void step(SimState state)	
		{
		world.step(timestep, velocityIterations, positionIterations);
		}
    }
