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
 * A class simulating the hive. Its superclass is {@link AbstractSphericAgent}
 * which does all the drawing and the simulation step {@link #step(SimState)}
 * (nothing).
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class Hive extends AbstractSphericAgent {
	/**
	 * The hive's entrance.
	 */
	private HiveEntrance entrance;

	/**
	 * The current amount of honey in [µl] inside the hive.
	 */
	private double honeyStore = 0;

	/**
	 * The constructor for a hive. This constructor calls the constructor of the
	 * superclass by providing the hive's color.
	 * 
	 * @param simulation
	 *            The simulation where this hive resides in.
	 * @param location
	 *            The location of the hive.
	 * @param size
	 *            The size (diameter) of the hive.
	 * @param honey
	 *            The initial amount of honey the hive contains.
	 */
	public Hive(ForagingHoneyBeeSimulation simulation, Point3d location,
			double size, double honey) {
		super(simulation, location, new Vector3d(), size, Color.blue);
		setHoney(honey);
	}

	/**
	 * Set the entrance for this hive.
	 * 
	 * @param entrance
	 *            The entrance.
	 */
	public void setEntrance(HiveEntrance entrance) {
		this.entrance = entrance;
	}

	/**
	 * Get the entrance for this hive.
	 * 
	 * @return The current entrance.
	 */
	public HiveEntrance getEntrance() {
		return this.entrance;
	}

	/**
	 * Get some honey out of the honey store of this hive. If more honey is
	 * requested than available the amount of honey is set to the available
	 * amount.
	 * 
	 * @param amount
	 * @return The requested amount of honey, less otherwise.
	 */
	public double getHoney(double amount) {
		double total = getHoneyAmount();
		amount = Math.min(amount, total);
		total -= amount;

		return amount;
	}

	/**
	 * Add some honey to the honey store of this hive.
	 * 
	 * @param honey
	 */
	public void storeHoney(double honey) {
		setHoney(getHoneyAmount() + honey);
	}

	/**
	 * Return the amount of honey stored in this hive.
	 * 
	 * @return honeyStore
	 */
	public double getHoneyAmount() {
		return honeyStore;
	}

	/**
	 * Set the amount of honey stored in this hive.
	 * 
	 * @param honey
	 */
	public void setHoney(double honey) {
		honeyStore = honey;
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
