/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package foragingBee;

import java.awt.Color;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import masonGlue.ForagingHoneyBeeSimulation;
import sim.engine.SimState;

/**
 * This class simulates the entrance of a hive. This class will attach itself to
 * the provided hive. Its superclass is {@link AbstractSphericAgent} which does
 * all the drawing and the simulation step {@link #step(SimState)} (nothing).
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class HiveEntrance extends AbstractSphericAgent {
	/**
	 * The hive the entrance belongs to.
	 */
	Hive hive;

	/**
	 * The constructor for an hive entrance. This constructor calls the
	 * constructor of the superclass by providing the entrance's color.
	 * 
	 * @param simulation
	 *            The simulation where this entrance resides in.
	 * @param hive
	 *            The hive this entrance belongs to.
	 * @param direction
	 *            The direction in degrees where the entrance is located at the
	 *            outer rim of the hive.
	 */
	public HiveEntrance(ForagingHoneyBeeSimulation simulation, Hive hive,
			double direction) {
		super(simulation, new Point3d(), new Vector3d(), 1, Color.gray);
		this.hive = hive;

		setSize(hive.getSize() / 5);
		double r = hive.getSize() / 2;
		double x = Math.cos(direction) * r + hive.getLocation().x;
		double y = Math.sin(direction) * r + hive.getLocation().y;
		double z = hive.getLocation().z;
		this.setLocation(x, y, z);
	}

	/**
	 * This method is performed when the next step for the agent is computed.
	 * This agent does nothing, so nothing is inside the body of the method.
	 * 
	 * @param state
	 *            The {@link SimState} environment.
	 */
	public void step(SimState state) {
	}
}
