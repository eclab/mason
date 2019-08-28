/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package foragingBee;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import masonGlue.ForagingHoneyBeeSimulation;
import sim.portrayal.DrawInfo2D;

/**
 * This class implements an agent that can draw itself as a sphere. Several
 * subclasses uses this feature to simplify their own definition.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public abstract class AbstractSphericAgent extends AbstractMovingAgent {
	/**
	 * The constructor for a spherical agent.
	 * 
	 * @param simulation
	 *            The simulation where this agent resides in.
	 * @param location
	 *            The location of the agent.
	 * @param velocity
	 *            The velocity of the agent.
	 * @param size
	 *            The size (diameter) of the food source.
	 * @param color
	 *            The color of the food source.
	 */
	public AbstractSphericAgent(ForagingHoneyBeeSimulation simulation,
			Point3d location, Vector3d velocity, double size, Color color) {
		super(simulation, location, velocity, size, color);
	}

	// ========== SimplePortrayal2D ==========

	/**
	 * This is the final method this agent uses to draw itself as a sphere.
	 * 
	 * @param object The object itself.
	 * @param graphics Where (output device) to draw.
	 * @param info Where (location) to draw.
	 */
	public final void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		double diamx = info.draw.width * getSize();
		double diamy = info.draw.height * getSize();

		graphics.setColor(getColor());

		graphics.fillOval((int) (info.draw.x - diamx / 2),
				(int) (info.draw.y - diamy / 2), (int) (diamx), (int) (diamy));
	}

}
