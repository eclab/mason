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
 * This class simulates a food source. The simulation is far away to be
 * realistic because e.g. the nectar never exhausts. The behaviour may be more
 * realistic in later simulations.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 */
public class FoodSource extends AbstractSphericAgent {
	/**
	 * The current concentration of nectar in this source.
	 */
	double concentration;

	/**
	 * The constructor for a food source. This constructor calls the constructor
	 * of the superclass by providing the food source's color.
	 * 
	 * @param simulation
	 *            The simulation where this food source resides in.
	 * @param location
	 *            The location of the food source.
	 * @param size
	 *            The size (diameter) of the food source.
	 * @param color
	 *            The color of the food source.
	 * @param concentration
	 *            The concentration of the nectar in this food source.
	 */
	public FoodSource(ForagingHoneyBeeSimulation simulation, Point3d location,
			float size, Color color, double concentration) {
		super(simulation, location, new Vector3d(), size, color);

		setConcentration(concentration);
	}

	/**
	 * Return the concentration of nectar of this source.
	 * 
	 * @return the concentration
	 */
	public final double getConcentration() {
		return concentration;
	}

	/**
	 * Set the concentration of nectar of this source.
	 * 
	 * @param concentration
	 *            the concentration to set
	 */
	public final void setConcentration(double concentration) {
		this.concentration = concentration;
	}

	/**
	 * Return the asked amount [µl] of nectar. If this source has less the
	 * maximum available amount is returned.
	 * 
	 * @param amount
	 * @return The requested amount of nectar if available, less otherwise.
	 */
	public double getNectar(double amount) {
		return amount;
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
