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


package sim.app.jbox2d.charactercollision;
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
import java.awt.geom.*;

public class CharacterCollision extends SimState
    {
    private static final long serialVersionUID = 1;

    public Continuous2D boxes;

    public CharacterCollision(long seed)
        {
        super(seed);
        }
        
    public Rectangle2D.Double getDimensions() 
    	{ 
    	return new Rectangle2D.Double(-20, -20, 40, 40); 
    	}
     
    public void start()
        {
        super.start();
        Rectangle2D.Double  dim = getDimensions();
        boxes = new Continuous2D(dim.getWidth(), dim.getHeight(), dim.getWidth());
        Vec2 shift = new Vec2(20f, 20f); 
        
		// We will lots of moving stuff.  Here's our array of agents for it.
		  ArrayList agents = new ArrayList();

        // Build the JBox2D World
    	Vec2 gravity = new Vec2(0, -10f);
    	World world = new World(gravity);

		// Make the Ground
		{
	      BodyDef bd = new BodyDef();
		  bd.position.set(bd.position.add(shift));
	      Body ground = world.createBody(bd);
	
	      EdgeShape shape = new EdgeShape();
	      shape.set(new Vec2(-20.0f, 0.0f), new Vec2(20.0f, 0.0f));
	      Fixture fix = ground.createFixture(shape, 0.0f);
		  JBox2DObject obj = new JBox2DObject(ground, boxes);
		}

		// Collinear edges
		// This shows the problematic case where a box shape can hit
		// an internal vertex.
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(bd.position.add(shift));
		  Body ground = world.createBody(bd);

		  EdgeShape shape = new EdgeShape();
		  shape.m_radius = 0.0f;
		  shape.set(new Vec2(-8.0f, 1.0f), new Vec2(-6.0f, 1.0f));
		  Fixture fix = ground.createFixture(shape, 0.0f);
		  JBox2DObject obj = new JBox2DObject(ground, boxes);

		  shape.set(new Vec2(-6.0f, 1.0f), new Vec2(-4.0f, 1.0f));
		  fix = ground.createFixture(shape, 0.0f);

		  shape.set(new Vec2(-4.0f, 1.0f), new Vec2(-2.0f, 1.0f));
		  fix = ground.createFixture(shape, 0.0f);
		}

		// Chain shape
		{
		  BodyDef bd = new BodyDef();
		  bd.angle = 0.25f * MathUtils.PI;
		  bd.position.set(bd.position.add(shift).add(new Vec2(6.5f, 7.5f)));
		  Body ground = world.createBody(bd);

		  Vec2[] vs = new Vec2[4];
		  vs[0] = new Vec2(5.0f - 6.5f, 7.0f - 7.5f);
		  vs[1] = new Vec2(6.0f - 6.5f, 8.0f - 7.5f);
		  vs[2] = new Vec2(7.0f - 6.5f, 8.0f - 7.5f);
		  vs[3] = new Vec2(8.0f - 6.5f, 7.0f - 7.5f);
		  ChainShape shape = new ChainShape();
		  shape.createChain(vs, 4);
		  Fixture fix = ground.createFixture(shape, 0.0f);
		  JBox2DObject obj = new JBox2DObject(ground, boxes);
		}

		// Square tiles. This shows that adjacency shapes may
		// have non-smooth collision. There is no solution
		// to this problem.
		
		/// NOTE: The MASON version of this code is slightly different from the
		/// standard Box2D demo code because we need to make sure that the center
		/// of the body is also the center of the boxes so they look normal when we
		/// drag them around and rotate them.
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(6.0f, 3.0f);
		  bd.position.set(bd.position.add(shift));
		  Body tile = world.createBody(bd);

		  PolygonShape tileshape = new PolygonShape();
		  tileshape.setAsBox(1.0f, 1.0f, new Vec2(-2.0f, 0.0f), 0.0f);
		  tile.createFixture(tileshape, 0.0f);
		  JBox2DObject obj = new JBox2DObject(tile, boxes);
		  
		  tileshape.setAsBox(1.0f, 1.0f, new Vec2(0.0f, 0.0f), 0.0f);
		  tile.createFixture(tileshape, 0.0f);

		  tileshape.setAsBox(1.0f, 1.0f, new Vec2(2.0f, 0.0f), 0.0f);
		  tile.createFixture(tileshape, 0.0f);
		}

		// Square made from an edge loop. Collision should be smooth.
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(bd.position.add(shift).add(new Vec2(0f, 4f)));
		  Body ground = world.createBody(bd);

		  Vec2[] vs = new Vec2[4];
		  vs[0] = new Vec2(-1.0f, 3.0f).add(new Vec2(0f, -4f));
		  vs[1] = new Vec2(1.0f, 3.0f).add(new Vec2(0f, -4f));
		  vs[2] = new Vec2(1.0f, 5.0f).add(new Vec2(0f, -4f));
		  vs[3] = new Vec2(-1.0f, 5.0f).add(new Vec2(0f, -4f));
		  ChainShape shape = new ChainShape();
		  shape.createLoop(vs, 4);
		  Fixture fix = ground.createFixture(shape, 0.0f);
		  JBox2DObject obj = new JBox2DObject(ground, boxes);
		}

		// Edge loop. Collision should be smooth.
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(-10.0f, 4.0f).add(new Vec2(0f, 1.5f));
		  bd.position.set(bd.position.add(shift));
		  Body ground = world.createBody(bd);

		  Vec2[] vs = new Vec2[10];
		  vs[0] = new Vec2(0.0f, 0.0f).add(new Vec2(0f, -1.5f));
		  vs[1] = new Vec2(6.0f, 0.0f).add(new Vec2(0f, -1.5f));
		  vs[2] = new Vec2(6.0f, 2.0f).add(new Vec2(0f, -1.5f));
		  vs[3] = new Vec2(4.0f, 1.0f).add(new Vec2(0f, -1.5f));
		  vs[4] = new Vec2(2.0f, 2.0f).add(new Vec2(0f, -1.5f));
		  vs[5] = new Vec2(0.0f, 2.0f).add(new Vec2(0f, -1.5f));
		  vs[6] = new Vec2(-2.0f, 2.0f).add(new Vec2(0f, -1.5f));
		  vs[7] = new Vec2(-4.0f, 3.0f).add(new Vec2(0f, -1.5f));
		  vs[8] = new Vec2(-6.0f, 2.0f).add(new Vec2(0f, -1.5f));
		  vs[9] = new Vec2(-6.0f, 0.0f).add(new Vec2(0f, -1.5f));
		  ChainShape shape = new ChainShape();
		  shape.createLoop(vs, 10);
		  Fixture fix = ground.createFixture(shape, 0.0f);
		  JBox2DObject obj = new JBox2DObject(ground, boxes);
		}

		// Square character 1
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(-3.0f, 8.0f);
		  bd.position.set(bd.position.add(shift));
		  bd.type = BodyType.DYNAMIC;
		  bd.fixedRotation = true;
		  bd.allowSleep = false;

		  Body body = world.createBody(bd);

		  PolygonShape shape = new PolygonShape();
		  shape.setAsBox(0.5f, 0.5f);

		  FixtureDef fd = new FixtureDef();
		  fd.shape = shape;
		  fd.density = 20.0f;
		  Fixture fix = body.createFixture(fd);
		  JBox2DObject obj = new JBox2DObject(body, boxes);
		  agents.add(obj);
		}

		// Square character 2
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(-5.0f, 5.0f);
		  bd.position.set(bd.position.add(shift));
		  bd.type = BodyType.DYNAMIC;
		  bd.fixedRotation = true;
		  bd.allowSleep = false;

		  Body body = world.createBody(bd);

		  PolygonShape shape = new PolygonShape();
		  shape.setAsBox(0.25f, 0.25f);

		  FixtureDef fd = new FixtureDef();
		  fd.shape = shape;
		  fd.density = 20.0f;
		  Fixture fix = body.createFixture(fd);
		  JBox2DObject obj = new JBox2DObject(body, boxes);
		  agents.add(obj);
		}

		// Hexagon character
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(-5.0f, 8.0f);
		  bd.position.set(bd.position.add(shift));
		  bd.type = BodyType.DYNAMIC;
		  bd.fixedRotation = true;
		  bd.allowSleep = false;

		  Body body = world.createBody(bd);

		  float angle = 0.0f;
		  float delta = MathUtils.PI / 3.0f;
		  Vec2 vertices[] = new Vec2[6];
		  for (int i = 0; i < 6; ++i) {
			vertices[i] = new Vec2(0.5f * MathUtils.cos(angle), 0.5f * MathUtils.sin(angle));
			angle += delta;
		  }

		  PolygonShape shape = new PolygonShape();
		  shape.set(vertices, 6);

		  FixtureDef fd = new FixtureDef();
		  fd.shape = shape;
		  fd.density = 20.0f;
		  Fixture fix = body.createFixture(fd);
		  JBox2DObject obj = new JBox2DObject(body, boxes);
		  agents.add(obj);
		}

		// Circle character
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(3.0f, 5.0f);
		  bd.position.set(bd.position.add(shift));
		  bd.type = BodyType.DYNAMIC;
		  bd.fixedRotation = true;
		  bd.allowSleep = false;

		  Body body = world.createBody(bd);

		  CircleShape shape = new CircleShape();
		  shape.m_radius = 0.5f;

		  FixtureDef fd = new FixtureDef();
		  fd.shape = shape;
		  fd.density = 20.0f;
		  Fixture fix = body.createFixture(fd);
		  JBox2DObject obj = new JBox2DObject(body, boxes);
		  agents.add(obj);
		}

		// Circle character
		{
		  BodyDef bd = new BodyDef();
		  bd.position.set(-7.0f, 6.0f);
		  bd.position.set(bd.position.add(shift));
		  bd.type = BodyType.DYNAMIC;
		  bd.allowSleep = false;

		  Body m_character = world.createBody(bd);

		  CircleShape shape = new CircleShape();
		  shape.m_radius = 0.25f;

		  FixtureDef fd = new FixtureDef();
		  fd.shape = shape;
		  fd.density = 20.0f;
		  fd.friction = 1;
		  Fixture fix = m_character.createFixture(fd);
		  JBox2DObject obj = new JBox2DObject(m_character, boxes);
		  agents.add(obj);
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
        doLoop(CharacterCollision.class, args);
        System.exit(0);
        }    
    }
