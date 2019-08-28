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
 * This class simulates a food source. The simulation is far away to be
 * realistic because e.g. the nectar never exhausts. The behaviour may be more
 * realistic in later simulations.
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
public class FoodSource extends AbstractMovingAgent {
	/**
	 * A factor computed once to compute the current concentration of nectar in
	 * this food source.
	 */
	private final double sizeFactor;
	/**
	 * The initial nectar concentration in this source.
	 */
	double initialConcentration;

	/**
	 * The current nectar concentration in this source.
	 */
	double concentration;

	/**
	 * The current amount of nectar in this source. The amount is the product of
	 * concentration multiplied with the size of the food source.
	 */
	double amount;

	/**
	 * A counter that will increased by {@link #beeLock()} and decreased by
	 * {@link #beeUnlock()}.
	 */
	int beeCount;

	/**
	 * This factor determines the speed the nectar concentration is refilled
	 * until the initial value. This factor determines the fraction of the
	 * differenz of the current concentration to the initial one.
	 */
	double refillFactor = 0.025;

	/**
	 * The maximum value the refilling will happen every step. This value clamps
	 * the computed using the {@link #refillFactor} variable.
	 */
	double maxAbsoluteRefill = 10.0;

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
	public FoodSource(ForagingHoneyBeeSimulation simulation, boolean is3dMode,
			Point3d location, double size, Color color, double concentration) {
		super(simulation, is3dMode, location, new Vector3d(), size, color);

		sizeFactor = 1 / getSize();
		initialConcentration = concentration;
		setConcentration(initialConcentration);
		computeAmount();
	}

	/**
	 * Compute the amount of nectar in this food source. The amount is the
	 * product of the current concentration and the size of the food source.
	 */
	private void computeAmount() {
		amount = concentration * getSize();
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
	 * Set the concentration of nectar of this source. By setting the
	 * concentration the amount of nectar is calculated. The amount is the
	 * concentration multiplied by the size.
	 * 
	 * @param concentration
	 *            the concentration to set
	 */
	private final void setConcentration(double concentration) {
		this.concentration = concentration;
		computeAmount();
	}

	/**
	 * Return the asked amount [µl] of nectar. If this source has less the
	 * maximum available amount is returned.
	 * 
	 * @param amount
	 * @return The requested amount of nectar if available, less otherwise.
	 */
	public double getNectar(double amount) {
		amount = Math.min(amount, this.amount);
		this.amount -= amount;
		computeConcentration();
		return amount;
	}

	/**
	 * Return the refill factor that is used every step to increase the nectar
	 * concentration.
	 * 
	 * @return the refillFactor
	 */
	public final double getRefillFactor() {
		return refillFactor;
	}

	/**
	 * Set the refill factor that is used every step to increase the nectar
	 * concentration.
	 * 
	 * @param refillFactor
	 *            the refillFactor to set
	 */
	public final void setRefillFactor(double refillFactor) {
		this.refillFactor = refillFactor;
	}

	/**
	 * Get the maximum absolute refill value that clamps the by
	 * {@link #refillFactor} computed value.
	 * 
	 * @return the maxAbsoluteRefill
	 */
	public final double getMaxAbsoluteRefill() {
		return maxAbsoluteRefill;
	}

	/**
	 * Set the maximum absolute refill value that clamps the by
	 * 
	 * @param maxAbsoluteRefill
	 *            the maxAbsoluteRefill to set
	 */
	public final void setMaxAbsoluteRefill(double maxAbsoluteRefill) {
		this.maxAbsoluteRefill = maxAbsoluteRefill;
	}

	/**
	 * A bee tells the food source about its locking (the bee is heading for the
	 * food source.
	 */
	public void beeLock() {
		beeCount++;
	}

	/**
	 * A bee tells the food source about it unlocking (the bee is not heading
	 * for the food source anymore.
	 */
	public void beeUnlock() {
		beeCount--;
	}

	/**
	 * Compute the current concentration of nectar according to the food
	 * source's size and amount of nectar.
	 */
	private final void computeConcentration() {
		concentration = amount * sizeFactor;
	}

	/**
	 * Return the number of bees locked to this food source.
	 * 
	 * @return The number of bees locked on this food source.
	 */
	public int getBeeCount() {
		return beeCount;
	}

	/**
	 * Increase the current concentration by a certain amount. According to
	 * increase the concentration the amount of nectar has to be recomputed.
	 * 
	 * @param by
	 *            The amount the concentration will be increased.
	 */
	private final void increaseConcentration(double by) {
		setConcentration(getConcentration() + by);
	}

	/**
	 * This method is performed when the next step for the agent is computed.
	 * This agent does nothing, so nothing is inside the body of the method.
	 * 
	 * @param state
	 *            The {@link SimState} environment.
	 */
	public void step(SimState state) {
		double diff = initialConcentration - concentration;
		if (diff > 0) {
			diff *= .0025;

			// clamp the maximum value for increasing the nectar concentration
			diff = Math.min(diff, 10);

			increaseConcentration(diff);
		}
	}
}
