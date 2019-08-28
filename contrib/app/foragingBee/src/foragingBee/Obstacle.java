/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package foragingBee;

import java.awt.Color;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import masonGlue.ForagingHoneyBeeSimulation;
import masonGlue.ParametrizedForagingHoneyBeeSimulation;
import sim.engine.SimState;

/**
 * A class representing a simple obstacle. Its superclass is
 * {@link AbstractSphericAgent} which does all the drawing and the simulation
 * step {@link #step(SimState)} (nothing).
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 */
public class Obstacle extends AbstractSphericAgent {
	/**
	 * The default color of an obstacle.
	 */
	public final static Color STD_COLOR = new Color(0xff, 0x80, 0x00);

	/**
	 * The constructor for an obstacle. This constructor calls the constructor
	 * of the superclass by providing the obstacle's color.
	 * 
	 * @param simulation The simulation where this obstacle resides in.
	 * @param location The location of the obstacle.
	 * @param size The size (diameter) of the obstacle.
	 */
	public Obstacle(ForagingHoneyBeeSimulation simulation, Point3d location,
			double size) {
		super(simulation, location, new Vector3d(), size, STD_COLOR);
	}

	/**
	 * This method is performed when the next step for the agent is computed.
	 * This agent does nothing, so nothing is inside the body of the method.
	 * 
	 * @param state
	 *            The {@link SimState} environment, in this simulation an
	 *            instance of type {@link ForagingHoneyBeeSimulation} or
	 *            {@link ParametrizedForagingHoneyBeeSimulation}.
	 */
	public void step(SimState state) {
	}
}
