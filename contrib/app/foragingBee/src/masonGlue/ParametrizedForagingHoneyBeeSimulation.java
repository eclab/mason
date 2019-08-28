/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */
package masonGlue;

import java.util.Vector;

import sim.util.Interval;
import foragingBee.FoodSource;
import foragingBee.Hive;
import foragingBee.Obstacle;

/**
 * This class provides only some public getter and setter methods hence
 * simulation properties can be accessed by a MASON panel and an attached
 * inspector.
 * <p>
 * This class allows the access to the properties:
 * <ul>
 * <li>Maximum number of bees {@link #maxBees},</li>
 * <li>control over the avoidance of obstacles {@link #avoidObstacles},</li>
 * <li>the maximum number of search steps when a bee a searching a food source
 * {@link #maxSearchSteps},</li>
 * <li>the communication noise {@link #comNoise} when bees exchanging
 * information,</li>
 * <li>the probability a bee forgets about its food source
 * {@link #pForgettingSource},</li>
 * <li>the probability a bee starts foraging again {@link #pForagingAgain},</li>
 * <li>the probability a bee starts scouting {@link #pStartScouting},</li>
 * <li>the colony nectar need {@link #colonyNectarNeed},</li>
 * <li>access to the hives in the simulation {@link #getHives()},</li>
 * <li>access to the food sources in the simulation {@link #getFoodSources()},</li>
 * <li>access to the obstacles in the simulation {@link #getObstacles()}.</li>
 * </ul>
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class ParametrizedForagingHoneyBeeSimulation extends
		ForagingHoneyBeeSimulation {

	/**
	 * The constructor that has the same parameters as the superclass.
	 * 
	 * @param seed
	 *            The random seed for initializing the random generator.
	 */
	public ParametrizedForagingHoneyBeeSimulation(long seed) {
		super(seed);
	}

	// ========== Number of Bees ==========

	/**
	 * Get the maximum number of bees in the simulation.
	 * 
	 * @return The current maximum number of bees.
	 */
	public int getNumberOfBees() {
		return maxBees;
	}

	/**
	 * Set the maximum number of bees in the simulation.
	 * 
	 * @param value
	 *            The current maximum number.
	 */
	public void setNumberOfBees(int value) {
		maxBees = value;
	}

	/**
	 * The interval the number of bees lies in.
	 * 
	 * @return The interval allowed for setting.
	 */
	public Interval domNumberOfBees() {
		return new Interval(1, 2000);
	}

	// ========== Avoid obstacles ==========

	/**
	 * Get if obstacles (other agents) have to be avoided..
	 * 
	 * @return The current state, true if avoiding.
	 */
	public boolean getAvoidObstacles() {
		return avoidObstacles;
	}

	/**
	 * Set if obstacles (other agents) have to be avoided..
	 * 
	 * @param value
	 *            The the current state, true if avoiding.
	 */
	public void setAvoidObstacles(boolean value) {
		avoidObstacles = value;
	}

	// ========== Maximum search time given in steps ==========

	/**
	 * Get the maximum number of steps the bees is looking for a food source.
	 * 
	 * @return The current maximum of search steps.
	 */
	public int getMaxSearchSteps() {
		return maxSearchSteps;
	}

	/**
	 * Set the maximum number of steps the bees is looking for a food source.
	 * 
	 * @param value
	 *            The current maximum number of search steps.
	 */
	public void setMaxSearchSteps(int value) {
		maxSearchSteps = value;
	}

	/**
	 * The interval the number of maximum search steps lies in.
	 * 
	 * @return The interval allowed for setting.
	 */
	public Interval domMaxSearchSteps() {
		return new Interval(1, 200);
	}

	// ========== Communication noise ==========

	/**
	 * Get the communication noise between the bees.
	 * 
	 * @return The current communication noise.
	 */
	public double getCommunicationNoise() {
		return comNoise;
	}

	/**
	 * Set the communication noise between the bees.
	 * 
	 * @param value
	 *            The current communication noise.
	 */
	public void setCommunicationNoise(double value) {
		comNoise = value;
	}

	/**
	 * The interval the communication noise factor lies in.
	 * 
	 * @return The interval allowed for setting.
	 */
	public Interval domCommunicationNoise() {
		return new Interval(0, .2);
	}

	// ========== The probability to forget the source ==========

	/**
	 * Get the probability a bee forgets the source.
	 * 
	 * @return The current probability.
	 */
	public double getpForgettingSource() {
		return pForgettingSource;
	}

	/**
	 * Set the probability a bee forgets the source.
	 * 
	 * @param value
	 */
	public void setpForgettingSource(double value) {
		pForgettingSource = value;
	}

	/**
	 * The interval the probability the bee forgets the source lies in.
	 * 
	 * @return The interval allowed for setting.
	 */
	public Interval dompForgettingSource() {
		return new Interval(1E-6, 100E-6);
	}

	// ========== The probability to forage again ==========

	/**
	 * Get the probability a bee forgets the source.
	 * 
	 * @return The current probability.
	 */
	public double getpForagingAgain() {
		return pForagingAgain;
	}

	/**
	 * Set the probability a bee forgets the source.
	 * 
	 * @param value
	 */
	public void setpForagingAgain(double value) {
		pForagingAgain = value;
	}

	/**
	 * The interval the probability the bee forgets the source lies in.
	 * 
	 * @return The interval allowed for setting.
	 */
	public Interval dompForagingAgain() {
		return new Interval(0, 500E-6);
	}

	// ========== The probability to forage again ==========

	/**
	 * Get the probability a bee forgets the source.
	 * 
	 * @return The current probability.
	 */
	public double getpStartScouting() {
		return pStartScouting;
	}

	/**
	 * Set the probability a bee forgets the source.
	 * 
	 * @param value
	 */
	public void setpStartScouting(double value) {
		pStartScouting = value;
	}

	/**
	 * The interval the probability the bee forgets the source lies in.
	 * 
	 * @return The interval allowed for setting.
	 */
	public Interval dompStartScouting() {
		return new Interval(0, 250E-6);
	}

	// ========== Colony nectar need ==========

	/**
	 * Get the general urge to collect nectar.
	 * 
	 * @return The current colony nectar need.
	 */
	public double getColonyNectarNeed() {
		return colonyNectarNeed;
	}

	/**
	 * Set the general urge to collect nectar.
	 * 
	 * @param value
	 */
	public void setColonyNectarNeed(double value) {
		colonyNectarNeed = value;
	}

	/**
	 * The interval the colony nectar need factor lies in.
	 * 
	 * @return The interval allowed for setting.
	 */
	public Interval domColonyNectarNeed() {
		return new Interval(0, 1.0);
	}

	// ========== Make the hives available in the GUI ==========

	/**
	 * Return the food sources in the simulation.
	 * 
	 * @return The {@link Vector} with the hives of type {@link Hive}.
	 */
	public Vector<Hive> getHives() {
		return hives;
	}

	// ========== Make the food sources available in the GUI ==========

	/**
	 * Return the food sources in the simulation.
	 * 
	 * @return The {@link Vector} with the food sources of type
	 *         {@link FoodSource}.
	 */
	public Vector<FoodSource> getFoodSources() {
		return foodSources;
	}

	// ========== Make the obstacles available in the GUI ==========

	/**
	 * Return the food sources in the simulation.
	 * 
	 * @return The {@link Vector} with the obstacles of type {@link Obstacle}.
	 */
	public Vector<Obstacle> getObstacles() {
		return obstacles;
	}
}
