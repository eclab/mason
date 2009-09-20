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
 * A class simulating the hive. Its superclass is {@link AbstractMovingAgent}
 * which performs in the simulation step {@link #step(SimState)} (nothing).
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
 * 
 */
public class Hive extends AbstractMovingAgent {
	/**
	 * Generated serial version id.
	 */
	private static final long serialVersionUID = -4592228162041859561L;
	/**
	 * The standard color for this object.
	 */
	public static final Color STD_COLOR = new Color(0x00, 0x00, 0xa0, 0xa0);
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
	public Hive(ForagingHoneyBeeSimulation simulation, boolean is3dMode,
			Point3d location, double size, double honey) {
		super(simulation, is3dMode, location, new Vector3d(), size, STD_COLOR);
		setHoneyAmount(honey);
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
		setHoneyAmount(total);

		return amount;
	}

	/**
	 * Add some honey to the honey store of this hive.
	 * 
	 * @param honey
	 */
	public void storeHoney(double honey) {
		setHoneyAmount(getHoneyAmount() + honey);
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
	public void setHoneyAmount(double honey) {
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
