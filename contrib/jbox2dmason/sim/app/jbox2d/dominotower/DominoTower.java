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


package sim.app.jbox2d.dominotower;
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

public class DominoTower extends SimState
    {
    private static final long serialVersionUID = 1;

    public Continuous2D boxes;

    public DominoTower(long seed)
        {
        super(seed);
        }
    
  public Fixture makeDomino(float x, float y, boolean horizontal, World world, float ddensity, float dfriction, float dwidth, float dheight) {
    PolygonShape sd = new PolygonShape();
    sd.setAsBox(.5f * dwidth, .5f * dheight);
    FixtureDef fd = new FixtureDef();
    fd.shape = sd;
    fd.density = ddensity;
    BodyDef bd = new BodyDef();
    bd.type = BodyType.DYNAMIC;
    fd.friction = dfriction;
    fd.restitution = 0.65f;
    bd.position = new Vec2(x, y).addLocal(50, 50);
    bd.angle = horizontal ? (float) (Math.PI / 2.0) : 0f;
    Body myBody = world.createBody(bd);
    return myBody.createFixture(fd);
  }

    public void start()
        {
        super.start();
        boxes = new Continuous2D(100, 100, 100);
        
        // Build the JBox2D World
    	Vec2 gravity = new Vec2(0, -10f);
    	World world = new World(gravity);

		// We will lots of moving stuff.  Here's our array of agents for it.
		  ArrayList agents = new ArrayList();

		// Make the Ground

		  {
		  BodyDef gbd = new BodyDef();
		  gbd.position = new Vec2(0.0f, -10.0f).addLocal(50, 50);
		  Body ground = world.createBody(gbd);
      	  PolygonShape sd = new PolygonShape();
      	  sd.setAsBox(50.0f, 10.0f);
		  Fixture efixture = ground.createFixture(sd, 0.0f);
		
		// Add the Ground to the MASON 
		  JBox2DObject eobject = new JBox2DObject(ground, boxes);
		  eobject.setCanSetOrientation2D(false);
		  eobject.setCanSetLocation(false);
		  }

		// Add the bullets
  float ddensity;// = 10f;

      	{
		  ddensity = 10f;
		  PolygonShape sd = new PolygonShape();
		  sd.setAsBox(.7f, .7f);
		  FixtureDef fd = new FixtureDef();
		  fd.density = 35f;
		  BodyDef bd = new BodyDef();
		  bd.type = BodyType.DYNAMIC;
		  fd.shape = sd;
		  fd.friction = 0f;
		  fd.restitution = 0.85f;
		  bd.bullet = true;
		  bd.position = new Vec2(30f, 50f).addLocal(50, 50);
		  Body b = world.createBody(bd);
		  Fixture bullet1fix = b.createFixture(fd);
		  b.setLinearVelocity(new Vec2(-25f, -25f));
		  b.setAngularVelocity(6.7f);

		// add to MASON
		  JBox2DObject eobject1 = new JBox2DObject(b, boxes);

		  fd.density = 25f;
		  bd.position = new Vec2(-30, 25f).addLocal(50, 50);
		  b = world.createBody(bd);
		  Fixture bullet2fix = b.createFixture(fd);
		  b.setLinearVelocity(new Vec2(35f, -10f));
		  b.setAngularVelocity(-8.3f);
		  
		// add to MASON
		  JBox2DObject eobject2 = new JBox2DObject(b, boxes);

		// add to schedule
		  agents.add(eobject1);
		  agents.add(eobject2);		
		}

		// add the dominos
		
		  final float dwidth = .20f;
  final float dheight = 1.0f;
  final float dfriction = 0.1f;
  int baseCount = 25;

    {
      float currX;
      // Make base
      for (int i = 0; i < baseCount; ++i) {
        currX = i * 1.5f * dheight - (1.5f * dheight * baseCount / 2f);
        Fixture fix = makeDomino(currX, dheight / 2.0f, false, world, ddensity, dfriction, dwidth, dheight);
          agents.add(new JBox2DObject(fix.getBody(), boxes));
        Fixture fix1 = makeDomino(currX, dheight + dwidth / 2.0f, true, world, ddensity, dfriction, dwidth, dheight);
          agents.add(new JBox2DObject(fix1.getBody(), boxes));
      }
      currX = baseCount * 1.5f * dheight - (1.5f * dheight * baseCount / 2f);
      // Make 'I's
      for (int j = 1; j < baseCount; ++j) {
        if (j > 3)
          ddensity *= .8f;
        float currY = dheight * .5f + (dheight + 2f * dwidth) * .99f * j; // y at center of 'I' structure

        for (int i = 0; i < baseCount - j; ++i) {
          currX = i * 1.5f * dheight - (1.5f * dheight * (baseCount - j) / 2f);// + parent.random(-.05f, .05f);
          ddensity *= 2.5f;
          if (i == 0) {
            Fixture fix = makeDomino(currX - (1.25f * dheight) + .5f * dwidth, currY - dwidth, false, world, ddensity, dfriction, dwidth, dheight);
            agents.add(new JBox2DObject(fix.getBody(), boxes));
          }
          if (i == baseCount - j - 1) {
            // if (j != 1) //djm: why is this here? it makes it off balance
            Fixture fix = makeDomino(currX + (1.25f * dheight) - .5f * dwidth, currY - dwidth, false, world, ddensity, dfriction, dwidth, dheight);
            agents.add(new JBox2DObject(fix.getBody(), boxes));
          }
          ddensity /= 2.5f;
          Fixture fix1 = makeDomino(currX, currY, false, world, ddensity, dfriction, dwidth, dheight);
          agents.add(new JBox2DObject(fix1.getBody(), boxes));
          Fixture fix2 = makeDomino(currX, currY + .5f * (dwidth + dheight), true, world, ddensity, dfriction, dwidth, dheight);
          agents.add(new JBox2DObject(fix2.getBody(), boxes));
          Fixture fix3 = makeDomino(currX, currY - .5f * (dwidth + dheight), true, world, ddensity, dfriction, dwidth, dheight);
          agents.add(new JBox2DObject(fix3.getBody(), boxes));
        }
      }
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
        doLoop(DominoTower.class, args);
        System.exit(0);
        }    
    }
