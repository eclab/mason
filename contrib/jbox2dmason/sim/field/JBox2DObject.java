/*
  Copyright 2018 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field;

import java.util.HashMap;
import sim.portrayal.*;
import sim.field.continuous.*;
import sim.util.*;

import org.jbox2d.callbacks.*;
import org.jbox2d.collision.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.serialization.*;
import org.jbox2d.collision.shapes.*;
import sim.engine.*;


/**
	A wrapper for JBox2D Bodies which associates them with a Continuous2D field.  The purpose of this
	is to allow JBox2D Bodies to be displayed and manipulated by Continuous2D and Continuous3D field 
	portrayals.  You should not manipulate JBox2DObjects programmatically in the Continuous2D -- 
	rather, you should manipulate their underlying Bodies directly in the JBox2D simulation environment. 
	
	<p>By constructing a JBox2DObject, the JBox2DObject automatically inserts itself into the Continuous2D, 
	so there is no need to do that again.  After building a JBox2DObject, you should insert it into 
	the Schedule to update itself with regularity so as to make sure that its location in the 
	Continuous2D is the same as the position of the underlying Body is in the JBox2D world.  
**/

public class JBox2DObject implements Constrained, Steppable, Orientable2D
    {
 	Body body;
    Continuous2D field;
    boolean invertY;
    boolean canSetOrientation2D = true;
    boolean canSetLocation = true;

	/** 
		Same as <tt>new JBox2D(body, field, true, null, null);</tt>
	 */

    public JBox2DObject(Body body, Continuous2D field)
    	{
    	this(body, field, true, null, null);
    	}

	/** 
		Same as <tt>new JBox2D(body, field, invertY, null, null);</tt>
	 */

    public JBox2DObject(Body body, Continuous2D field, boolean invertY)
    	{
    	this(body, field, invertY, null, null);
    	}

	/** 
		Same as <tt>new JBox2D(body, field, true, translation, null);</tt>
	 */

    public JBox2DObject(Body body, Continuous2D field, Vec2 translation)
    	{
    	this(body, field, true, translation, null);
    	}

	/** 
		Same as <tt>new JBox2D(body, field, true, null, map);</tt>
	 */

    public JBox2DObject(Body body, Continuous2D field, HashMap map)
    	{
    	this(body, field, true, null, map);
    	}

	/** 
		Same as <tt>new JBox2D(body, field, invertY, translation, null);</tt>
	 */
		
    public JBox2DObject(Body body, Continuous2D field, boolean invertY, Vec2 translation)
    	{
    	this(body, field, invertY, translation, null);
    	}

	/** Constructs a JBox2DObject for the provided body (and all its fixtures) in MASON and inserts it 
		into the given field.  The location in the field will be the current position of the body, plus
		the provided translation, if any (the translation can be <0,0> or null).  If invertY is true,
		which is the default, then the JBox2D world is assumed to be inverted in the Y dimension relative 
		to the standard MASON Continuous2D world.  If map is provided (it can be null) then a mapping of
		[Body -> JBox2DObject] is added to the map.  This might be useful for you later on if you
		wanted to determine which JBox2DObject is affiliated with a given JBox2D Body. We are currently
		using this model rather than placing the JBox2D in the JBox2D userdata, so you can use the userdata
		for whatever you need (for now). */
		
    public JBox2DObject(Body body, Continuous2D field, boolean invertY, Vec2 translation, HashMap map)
    	{
    	this.body = body;
    	this.field = field;
    	this.invertY = invertY;

    	if (map != null)
    		{
    		map.put(body, this);
    		}

    	if (translation != null)
	    	body.setTransform(body.getPosition().add(translation), body.getAngle());

    	updateFieldPosition();
    	}

	/** Sets whether this object can be rotated by the user. */
    public void setCanSetOrientation2D(boolean val)
    	{
    	canSetOrientation2D = val;
    	}

	/** Returns whether this object can be rotated by the user. */
    public boolean getCanSetOrientation2D()
    	{
    	return canSetOrientation2D;
    	}

	/** Sets whether this object can be moved by the user. */
    public void setCanSetLocation(boolean val)
    	{
    	canSetLocation = val;
    	}

	/** Returns whether this object can be moved by the user. */
    public boolean getCanSetLocation()
    	{
    	return canSetLocation;
    	}

	/** Returns the body affiliated with this JBox2DObject. */
    public Body getBody()
    	{
    	return body;
    	}

	/** Returns whether Y is inverted. */
    public boolean getInvertY() { return invertY; }

    public Object constrainLocation(Object field, Object location)
    	{
    	if (!canSetLocation) return null;

    	if (field instanceof Continuous2D)
    		{
    		Double2D loc = (Double2D) location;
    		Vec2 vec2d = null;
    		if (invertY)
    			{
    			vec2d = new Vec2((float)loc.x, (float)(((Continuous2D)field).getHeight() - loc.y));
    			}
    		else
    			{
    			vec2d = new Vec2((float)loc.x, (float)loc.y);
    			}
    		body.setTransform(vec2d, body.getAngle());
    		body.setAwake(true);
    		}
    	return location;
    	}

    public void setOrientation2D(double val)
    	{
    	if (canSetOrientation2D)
    		{
	    	body.setTransform(body.getPosition(), -(float)val);
    		body.setAwake(true);
	    	}
    	}

	public double orientation2D()
		{
		return -body.getAngle();
		}

	/** Removes the object from the Continuous2D and destroys its body and removes it from the JBox2D world. */
	public void remove()
		{
		body.getWorld().destroyBody(body);
		field.remove(this);
		}

	/** Updates the position in the Continuous2D to reflect the body position in the underlying JBox2D world. */
	public void step(SimState state)
		{
		updateFieldPosition();
		}

	public String toString()
		{
		return body.toString();
		}

	void updateFieldPosition()
		{
		if (field != null)
			{
			Vec2 pos = body.getPosition();
			Double2D dpos = null;
			if (invertY)
				{
				dpos = new Double2D(pos.x, field.getHeight() - pos.y);
				}
			else
				{
				dpos = new Double2D(pos.x, pos.y);
				}
			field.setObjectLocation(this, dpos);
			}
		}
	}
