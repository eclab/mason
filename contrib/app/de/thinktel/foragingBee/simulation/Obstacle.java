/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.foragingBee.simulation;

import java.awt.Color;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import sim.engine.SimState;
import de.thinktel.foragingBee.masonGlue.ForagingHoneyBeeSimulation;

/**
 * A class representing a simple obstacle. Its superclass is
 * {@link AbstractMovingAgent} which performs in the simulation step
 * {@link #step(SimState)} (nothing).
 * <p>
 * Changes:
 * <ul>
 * <li>20090901: This class inherits directly from {@link AbstractMovingAgent}
 * because the visualization for this object is defined in the MASON specific
 * part of the simulation.</li>
 * </ul>
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 */
public class Obstacle extends AbstractMovingAgent {
	/**
	 * The default color of an obstacle.
	 */
	public final static Color STD_COLOR = new Color(0xff, 0x80, 0x00);

	/**
	 * The constructor for an obstacle. This constructor calls the constructor
	 * of the superclass by providing the obstacle's color.
	 * 
	 * @param simulation
	 *            The simulation where this obstacle resides in.
	 * @param location
	 *            The location of the obstacle.
	 * @param size
	 *            The size (diameter) of the obstacle.
	 */
	public Obstacle(ForagingHoneyBeeSimulation simulation, boolean is3dMode, Point3d location,
			double size) {
		super(simulation, is3dMode, location, new Vector3d(), size, STD_COLOR);
	}

	/**
	 * This method is performed when the next step for the agent is computed.
	 * This agent does nothing, so nothing is inside the body of the method.
	 * 
	 * @param state
	 *            The {@link SimState} environment, in this simulation an
	 *            instance of type {@link ForagingHoneyBeeSimulation} or more
	 *            likely one of the subclasses.
	 */
	public void step(SimState state) {
//		if (r.nextInt(100) == 0) {
//			setColor(new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
//		}
	}
}
