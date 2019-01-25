/*
  Copyright 2017 by Sean Luke and George Mason University
  Significant Portions Copyright 2013 by Daniel Murphy.
  This file is distributed under the license below.
*/

/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/


package sim.app.jbox2d.pyramidtest;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

import org.jbox2d.callbacks.*;
import org.jbox2d.collision.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.serialization.*;
import org.jbox2d.collision.shapes.*;
import java.util.*;
import sim.field.*;

public class PyramidTest extends SimState
    {
    private static final long serialVersionUID = 1;

    public Continuous2D boxes;

    public PyramidTest(long seed)
        {
        super(seed);
        }
    
    public void start()
        {
        super.start();
        boxes = new Continuous2D(80, 80, 80);
        
        // Build the JBox2D World
    	Vec2 gravity = new Vec2(0, -10f);
    	World world = new World(gravity);

		// Make the Ground
		  BodyDef gbd = new BodyDef();
		  Body ground = world.createBody(gbd);
		  EdgeShape eshape = new EdgeShape();
		  eshape.set(new Vec2(0.0f, 40f), new Vec2(80f, 40f));
		  Fixture efixture = ground.createFixture(eshape, 0.0f);
		
		// Add the Ground to the MASON 
		  JBox2DObject eobject = new JBox2DObject(ground, boxes);

		// Define a Box Shape
		  float a = .5f;
		  PolygonShape shape = new PolygonShape();
		  shape.setAsBox(a, a);
		  Vec2 x = new Vec2(-7.0f, 0.75f);
		  Vec2 y = new Vec2(0, 0);
		  Vec2 deltaX = new Vec2(0.5625f, 1.25f);
		  Vec2 deltaY = new Vec2(1.125f, 0.0f);

		// We will make 20 boxes.  Each box needs to be added to the world AND
		// needs to be added to the schedule.  We add them to the schedule as a
		// sequence so we first need to create an ArrayList for that.
		  ArrayList agents = new ArrayList();

		// Create the Boxes based on the Box shape.  
		  int count = 20;
		  for (int i = 0; i < count; ++i) 
			{
			y.set(x);

			for (int j = i; j < count; ++j) 
				{
				BodyDef bd = new BodyDef();
				bd.type = BodyType.DYNAMIC;
				bd.position.set(y.add(new Vec2(40.0f, 40.0f)));
				Body body = world.createBody(bd);
				Fixture fixture = body.createFixture(shape, 5.0f);
				y.addLocal(deltaY);

				// Add to to MASON
				JBox2DObject object = new JBox2DObject(body, boxes);
				
				// Add to the ArrayList we'll submit to the schedule
				agents.add(object);
				}

			x.addLocal(deltaX);
			}
	
		// Now we need to add a special Steppable which updates the JBox2D World.
		// By default its timestep is 1/60 of a second
		JBox2DStep step = new JBox2DStep(world);
		schedule.scheduleRepeating(step.getTimestep(), 0, step);
		
		// We schedule the agents to all get updated immediately after this Steppable,
		// so they're in Ordering 1.
		schedule.scheduleRepeating(step.getTimestep(), 1, new Sequence(agents));
		}

    public static void main(String[] args)
        {
        doLoop(PyramidTest.class, args);
        System.exit(0);
        }    
    }
