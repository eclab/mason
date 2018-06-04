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


package sim.app.jbox2d.blobtest;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

import org.jbox2d.callbacks.*;
import org.jbox2d.collision.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.serialization.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.dynamics.joints.*;
import java.util.*;
import sim.field.*;

public class BlobTest extends SimState
    {
    private static final long serialVersionUID = 1;

    public Continuous2D boxes;

    public BlobTest(long seed)
        {
        super(seed);
        }
    
    public void start()
        {
        super.start();
        boxes = new Continuous2D(200, 200, 200);
        
        // Build the JBox2D World
    	Vec2 gravity = new Vec2(0, -10f);
    	World world = new World(gravity);

		// Make the Borders

		Body ground = null;
		{
		  PolygonShape sd = new PolygonShape();
		  sd.setAsBox(50.0f, 0.4f);

		  BodyDef bd = new BodyDef();
		  bd.position.set(0.0f, 0.0f).addLocal(100, 100);
		  ground = world.createBody(bd);
		  Fixture efixture = ground.createFixture(sd, 0f);
		  JBox2DObject eobject = new JBox2DObject(ground, boxes);

		  sd.setAsBox(0.4f, 50.0f, new Vec2(-10.0f, 0.0f), 0.0f);
		   efixture = ground.createFixture(sd, 0f);

		  sd.setAsBox(0.4f, 50.0f, new Vec2(10.0f, 0.0f), 0.0f);
		   efixture = ground.createFixture(sd, 0f);
		}
		
		// We will lots of moving stuff.  Here's our array of agents for it.
		  ArrayList agents = new ArrayList();

		// Define a Box Shape
    ConstantVolumeJointDef cvjd = new ConstantVolumeJointDef();

    float cx = 0.0f;
    float cy = 10.0f;
    float rx = 5.0f;
    float ry = 5.0f;
    int nBodies = 20;
    float bodyRadius = 0.5f;
    for (int i = 0; i < nBodies; ++i) {
      float angle = MathUtils.map(i, 0, nBodies, 0, 2 * 3.1415f);
      BodyDef bd = new BodyDef();

      bd.fixedRotation = true;

      float x = cx + rx * (float) Math.sin(angle);
      float y = cy + ry * (float) Math.cos(angle);
      bd.position.set(new Vec2(x, y));
      bd.type = BodyType.DYNAMIC;
      Body body = world.createBody(bd);
      body.setTransform(body.getPosition().addLocal(100, 100), body.getAngle());

      FixtureDef fd = new FixtureDef();
      CircleShape cd = new CircleShape();
      cd.m_radius = bodyRadius;
      fd.shape = cd;
      fd.density = 1.0f;
      
      Fixture eFixture = body.createFixture(fd);
      cvjd.addBody(body);
      
 	  JBox2DObject eobject = new JBox2DObject(body, boxes);
	  agents.add(eobject);
   }

    cvjd.frequencyHz = 10.0f;
    cvjd.dampingRatio = 1.0f;
    cvjd.collideConnected = false;
    world.createJoint(cvjd);

    BodyDef bd2 = new BodyDef();
    bd2.type = BodyType.DYNAMIC;
    PolygonShape psd = new PolygonShape();
    psd.setAsBox(3.0f, 1.5f, new Vec2(cx, cy + 15.0f), 0.0f);
    bd2.position = new Vec2(cx, cy + 15.0f).addLocal(100, 100);
    Body fallingBox = world.createBody(bd2);
    Fixture eFixture = fallingBox.createFixture(psd, 1.0f);
 	 JBox2DObject eobject = new JBox2DObject(fallingBox, boxes);
	  agents.add(eobject);
    
    
	
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
        doLoop(BlobTest.class, args);
        System.exit(0);
        }    
    }
