/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.foragingBee.masonGlue;

import java.awt.Color;
import java.util.ListIterator;
import java.util.Vector;

import javax.vecmath.Point3d;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.Interval;
import de.thinktel.foragingBee.simulation.Bee;
import de.thinktel.foragingBee.simulation.FoodSource;
import de.thinktel.foragingBee.simulation.Hive;
import de.thinktel.foragingBee.simulation.HiveEntrance;
import de.thinktel.foragingBee.simulation.IAgentLocator;
import de.thinktel.foragingBee.simulation.IMovingAgent;
import de.thinktel.foragingBee.simulation.IVisualAgent;
import de.thinktel.foragingBee.simulation.Obstacle;

/**
 * This abstract class hold all information for the simulation. This class
 * implements the simulated world of a hive ({@link Hive}), bees ({@link Bee})
 * and food sources ({@link FoodSource}). This class does not provide any user
 * interface.
 * <p>
 * This class implements {@link Steppable} and extends the type {@link SimState}
 * . Invoking the method {@link #step(SimState)} will provide an instance of
 * this class.
 * <p>
 * This class allows the access to certain simulation parameters by getter and
 * setter methods. These methods will be examined by an inspector to present the
 * access to these parameters in the GUI of the simulation. The parameters are:
 * *
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
 * Changes:
 * <ul>
 * <li>20090825: Added the {@link #bees} property to count the bee agents.</li>
 * <li>20090825: Modified the {@link #addAgent(IMovingAgent)} and
 * {@link #removeAgent(IMovingAgent)} methods to add bee to their properties.</li>
 * <li>20090827: Removed an error which causes no honey was drawn by this hive.</li>
 * <li>20090828: Made this class abstract for 2D and 3D computation.</li>
 * <li>20090829: Added the setter and getter methods.</li>
 * </ul>
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public abstract class ForagingHoneyBeeSimulation extends SimState implements
		Steppable, IAgentLocator {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2187248402977949189L;

	/**
	 * A flag indicating if this simulation shall run in a 3d simulation
	 * environment.
	 */
	private boolean is3dMode = false;

	/**
	 * The minimum x bounds.
	 */
	static public final int X_MIN = 0;

	/**
	 * The maximum x bounds exclusive.
	 */
	static public final int X_MAX = 400;

	/**
	 * The minimum y bounds.
	 */
	static public final int Y_MIN = 0;

	/**
	 * The maximum y bounds exclusive.
	 */
	static public final int Y_MAX = 400;

	/**
	 * The minimum z bounds.
	 */
	static public final int Z_MIN = 0;

	/**
	 * The maximum z bounds exclusive.
	 */
	static public final int Z_MAX = 400;

	/**
	 * The width (x-axis).
	 */
	static public final int WIDTH = X_MAX - X_MIN;

	/**
	 * The middle of the x-axis.
	 */
	static public final int MIDDLE_WIDTH = X_MIN + (WIDTH / 2);
	// static public final int MIDDLE_WIDTH = 0;

	/**
	 * The height (y-axis).
	 */
	static public final int HEIGHT = Y_MAX - Y_MIN;

	/**
	 * The middle of the y-axis.
	 */
	static public final int MIDDLE_HEIGHT = Y_MIN + (HEIGHT / 2);
	// static public final int MIDDLE_HEIGHT = 0;

	/**
	 * The length (z-axis).
	 */
	static public final int LENGTH = Z_MAX - Z_MIN;

	/**
	 * The middle of the z-axis.
	 */
	static public final int MIDDLE_LENGTH = Z_MIN + (LENGTH / 2);
	// static public final int MIDDLE_LENGTH = 0;

	/**
	 * The diameter of the hive.
	 */
	static public final double HIVE_DIAMETER = 80;

	/**
	 * Set it to true if obstacles (other agents) are to be avoided.
	 */
	public boolean avoidObstacles = true;

	/**
	 * The maximum number of bees in the simulation. This value is defined
	 * protected only because a parametrizing subclass has to access this
	 * property.
	 */
	public int maxBees = 800;

	/**
	 * The communication problems between bees. It is noise when transferring
	 * information.
	 */
	public double comNoise = 0.01;

	/**
	 * A value describing the probability a bee forgets its source in every
	 * iteration step.
	 */
	public double pForgettingSource = 30E-6;

	/**
	 * A value describing the probability to start scouting.
	 * 
	 */
	public double pStartScouting = 39E-6;

	/**
	 * A value describing the probability to forage again if the be returned
	 * from foraging.
	 * 
	 */
	public double pForagingAgain = 500E-6;

	/**
	 * The general urge the colony needs some nectar. This is a highly
	 * aggregated variable including several aspects.
	 */
	public double colonyNectarNeed = 0.5;

	/**
	 * A value describing the maximum steps for a bee searching a food source.
	 */
	public int maxSearchSteps = 100;

	/**
	 * The maximum sphere radius of all objects in the simulation.
	 */
	double maxAgentSphereRadius = Double.MIN_VALUE;

	/**
	 * All agents in the simulation.
	 */
	Vector<IMovingAgent> agents = new Vector<IMovingAgent>();

	/**
	 * All hives of type {@link Hive} in the simulation.
	 */
	Vector<Hive> hives = new Vector<Hive>();

	/**
	 * All food sources of type {@link FoodSource} in the simulation.
	 */
	Vector<FoodSource> foodSources = new Vector<FoodSource>();

	/**
	 * All obstacles of type {@link Obstacle} in the simulation.
	 */
	Vector<Obstacle> obstacles = new Vector<Obstacle>();

	/**
	 * All bees of type {@link Bee}.
	 */
	Vector<Bee> bees = new Vector<Bee>();

	/**
	 * The public constructor. Takes a seed for initializing the random number
	 * generator and also a parameter set.
	 * 
	 * @param seed
	 */
	public ForagingHoneyBeeSimulation(long seed, boolean is3dMode) {
		super(seed);
		this.is3dMode = is3dMode;
	}

	/**
	 * Overwritten method for execution of this {@link SimState} object.
	 */
	public void start() {
		super.start();

		// initialize the simulation
		initSimulation();

		// add this simulation to the scheduler
		schedule.scheduleRepeating(this);
	}

	/**
	 * This method is performed when the next step for the simulation is
	 * computed. This simulation does nothing, so nothing is inside the body of
	 * the method.
	 * <p>
	 * This method is used because this object is placed in the scheduler.
	 * 
	 * 
	 * @param state
	 *            The {@link SimState} environment, in this simulation an
	 *            instance of type {@link ForagingHoneyBeeSimulation} or more
	 *            likely one of the subclasses.
	 */
	public void step(SimState state) {
		if (!is3dMode) {
			ListIterator<IMovingAgent> li = agents.listIterator();
			while (li.hasNext()) {
				IMovingAgent a = li.next();
				if (Math.abs(a.getLocation().z) > 0.000001) {
					System.err.println("Agent " + a + "  z:"
							+ a.getLocation().z);
				}
			}
		}
	}

	/**
	 * This method prepares the simulation by creating all necessary objects.
	 * This step is not really necessary because this can be done in the method
	 * {@link #start()} that is called prior executing the simulation. But to
	 * provide ready initialized objects for an user interface an even prior
	 * call is a precondition so the objects are instantiated before even
	 * {@link #start()} is called because the user interface and a model
	 * inspector are created before calling {@link #start()}.
	 */
	public void prepareSimulation() {
		// create the hive
		Hive hive = new Hive(this, is3dMode,
				new Point3d(50, 50, MIDDLE_LENGTH), HIVE_DIAMETER, 2000);
		addAgent(hive);
		// create the entrance
		HiveEntrance entrance = new HiveEntrance(this, is3dMode, hive, 0);
		hive.setEntrance(entrance);
		addAgent(entrance);

		FoodSource f1 = new FoodSource(this, is3dMode, new Point3d(300, 100,
				MIDDLE_LENGTH), 20, new Color(0xd0, 0x00, 0x00), 100);
		addAgent(f1);
		FoodSource f2 = new FoodSource(this, is3dMode, new Point3d(50, 350,
				MIDDLE_LENGTH), 15, new Color(0xc0, 0xc0, 0x00), 200);
		addAgent(f2);
		FoodSource f3 = new FoodSource(this, is3dMode, new Point3d(200, 200,
				MIDDLE_LENGTH), 35, new Color(0xd0, 0x00, 0xd0), 300);
		addAgent(f3);
		FoodSource f4 = new FoodSource(this, is3dMode, new Point3d(180, 250,
				MIDDLE_LENGTH), 18, new Color(0x00, 0xd0, 0xd0), 300);
		addAgent(f4);

		Obstacle o;
		o = new Obstacle(this, is3dMode, new Point3d(155, 130, MIDDLE_LENGTH),
				20);
		addAgent(o);
		o = new Obstacle(this, is3dMode, new Point3d(0, 0, MIDDLE_LENGTH), 20);
		addAgent(o);
	}

	/**
	 * This method will be called by the {@link #start()} method that will start
	 * the simulation. The agents are created and placed into the simulation.
	 * The method {@link #prepareSimulation()} is called first to prepare the
	 * simulation and provide the user interface with data.
	 * <p>
	 * This method places <b>every</b> agent in the scheduler, even those that
	 * are created in {@link #prepareSimulation()}.
	 */
	private void initSimulation() {
		/*
		 * If a hive exists create the maximum number of bees.
		 */
		Hive hive = hives.get(0);
		if (hive != null) {
			Point3d fsl = hive.getLocation();

			int i;
			for (i = 0; i < maxBees; i++) {
				Bee b = new Bee(this, is3dMode, hive, new Point3d(fsl.x, fsl.y,
						MIDDLE_LENGTH));
				addAgent(b);
			}
		}

		/*
		 * Bee b = new Bee(this, hive, new Point3d(170, 135, MIDDLE_LENGTH));
		 * addAgent(b); b.setState(Bee.State.returnWithoutInfo);
		 * b.headTo(hive.getEntrance());
		 */
		/*
		 * Place *every* agent in the scheduler.
		 */
		ListIterator<IMovingAgent> li;
		li = agents.listIterator();
		while (li.hasNext()) {
			IIterationAgent agent = (IIterationAgent) li.next();
			Object o = schedule.scheduleRepeating(agent);
			agent.setSchedulerInformation(o);
		}
	}

	/**
	 * Check if the center of the agent is outside the simulation bounds. For
	 * every bound on the axis it is checked if it is below the minimum or above
	 * or equal the maximum: the maximum value is outside.
	 * 
	 * @param agent
	 * @return True if the agent is outside the boundaries.
	 */
	public boolean isOutside(IMovingAgent agent) {
		Point3d l = agent.getLocation();

		return ((l.x < X_MIN) | (l.x >= X_MAX) | (l.y < Y_MIN) | (l.y >= Y_MAX)
				| (l.z < Z_MIN) | (l.z >= Z_MAX));
	}

	/**
	 * Check if the center of the agent is outside the xy-pane of the simulation
	 * bounds. For every bound on the axis it is checked if it is below the
	 * minimum or above or equal the maximum: the maximum value is outside.
	 * 
	 * @param agent
	 * @return True if the agent is outside the boundaries.
	 */
	public boolean isOutsideXY(IMovingAgent agent) {
		Point3d l = agent.getLocation();

		return ((l.x < X_MIN) | (l.x >= X_MAX) | (l.y < Y_MIN) | (l.y >= Y_MAX));

	}

	/**
	 * Check if the center of the agent is outside the z-axis of the simulation
	 * bounds. For every bound on the axis it is checked if it is below the
	 * minimum or above or equal the maximum: the maximum value is outside.
	 * 
	 * @param agent
	 * @return True if the agent is outside the boundaries.
	 */
	public boolean isOutsideZ(IMovingAgent agent) {
		Point3d l = agent.getLocation();

		return (l.z < Z_MIN) | (l.z >= Z_MAX);
	}

	/**
	 * Add an agent to the simulation. The maximum sphere of all objects is
	 * computed during successfully adding an agent. Some agent classes are
	 * additionally stored in list to provide access in the user interface.
	 * 
	 * @param agent
	 * @return True, if the agent has successfully added; false otherwise.
	 */
	public boolean addAgent(IMovingAgent agent) {
		if (!agents.contains(agent)) {
			agents.add(agent);

			if (agent instanceof Bee) {
				bees.add((Bee) agent);
			} else {
				if (agent instanceof Hive) {
					hives.add((Hive) agent);
				} else {
					if (agent instanceof FoodSource) {
						foodSources.add((FoodSource) agent);
					} else {
						if (agent instanceof Obstacle) {
							obstacles.add((Obstacle) agent);
						}
					}
				}
			}

			double r = agent.getSphereRadius();
			if (r > maxAgentSphereRadius)
				maxAgentSphereRadius = r;

			return true;
		}

		return false;
	}

	/**
	 * Remove an agent from the simulation. If the agent has a sphere which
	 * represents the maximum radius of all agents spheres the (possible) new
	 * maximum sphere is computed.
	 * <p>
	 * The agents are also removed from the additional lists if necessary.
	 * <p>
	 * The agent is also removed from the simulation's scheduler.
	 * 
	 * @param agent
	 * @return True if the agent has been removed, false otherwise (because it
	 *         has never been stored).
	 */
	public boolean removeAgent(IMovingAgent agent) {
		boolean status = agents.remove(agent);
		if (agent instanceof Bee) {
			bees.remove(agent);
		} else {
			if (agent instanceof Hive) {
				hives.remove(agent);
			} else {
				if (agent instanceof FoodSource) {
					foodSources.remove(agent);
				} else {
					if (agent instanceof Obstacle) {
						obstacles.remove(agent);
					}
				}
			}
		}

		if (status) {
			double radius = agent.getSphereRadius();
			if (radius >= maxAgentSphereRadius) {
				maxAgentSphereRadius = computeMaxAgentSphereRadius();
			}
		}

		if (agent instanceof IIterationAgent) {
			IIterationAgent a = (IIterationAgent) agent;
			Stoppable stop = (Stoppable) a.getSchedulerInformation();
			stop.stop();
		}

		if (agent instanceof IVisualAgent) {
			Object o = removeFromVisualization((IVisualAgent) agent);
			System.out.println("removeFromVisualization: " + o);
		}

		return status;
	}

	/**
	 * Compute the maximum radius of all agents inside this simulation.
	 * 
	 * @return The computed maximum sphere currently in the simulation.
	 */
	private double computeMaxAgentSphereRadius() {
		double max = Double.MIN_VALUE;
		double r;

		ListIterator<IMovingAgent> li = agents.listIterator();
		while (li.hasNext()) {
			r = li.next().getSphereRadius();
			if (r > max)
				max = r;
		}

		return max;
	}

	/**
	 * Compute every agent that is within a certain location from an agent. This
	 * method assumes all objects are points so only the objects. Due to the
	 * algorithm provided by MASON objects with a distance at least as provided
	 * are included.
	 * 
	 * @param agent
	 *            The agent where to start.
	 * @param distance
	 *            The distance (radius) the returned agents should lie within.
	 * @return All objects that are at least in the distance.
	 */
	public abstract Object[] getObjectsWithinDistance(IMovingAgent agent,
			double distance);

	/**
	 * Return the maximum radius of all agents inside this simulation.
	 * 
	 * @return The current maximum sphere.
	 */
	public double getMaxSphereRadius() {
		return this.maxAgentSphereRadius;
	}

	// ==================== Getter and Setter methods ====================

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
