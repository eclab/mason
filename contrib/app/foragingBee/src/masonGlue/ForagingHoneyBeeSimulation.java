/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package masonGlue;

import java.awt.Color;
import java.util.ListIterator;
import java.util.Vector;

import javax.vecmath.Point3d;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.util.Bag;
import sim.util.Double2D;
import foragingBee.Bee;
import foragingBee.FoodSource;
import foragingBee.Hive;
import foragingBee.HiveEntrance;
import foragingBee.IAgentLocator;
import foragingBee.IMovingAgent;
import foragingBee.Obstacle;

/**
 * This class hold all information for the simulation. This class implements the
 * simulated world of a hive ({@link Hive}), bees ({@link Bee}) and food sources
 * ({@link FoodSource}). This class does not provide any user interface.
 * <p>
 * This class implements {@link Steppable} and extends the type {@link SimState}
 * . Invoking the method {@link #step(SimState)} will provide an instance of
 * this class.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class ForagingHoneyBeeSimulation extends SimState implements Steppable,
		IAgentLocator {

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
	 * The width (x axis).
	 */
	static public final int WIDTH = X_MAX - X_MIN;

	/**
	 * The height (y axis).
	 */
	static public final int HEIGHT = Y_MAX - Y_MIN;

	/**
	 * The length (z axis).
	 */
	static public final int LENGTH = Z_MAX - Z_MIN;

	/**
	 * The diameter of the hive.
	 */
	static public final double HIVE_DIAMETER = 80;

	/**
	 * Set it to true if obstacles (other agents) are to be avoided.
	 */
	public boolean avoidObstacles = false;

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
	 * 
	 */
	private static final long serialVersionUID = -2187248402977949189L;

	/**
	 * An environment the agents are moving in. This object has to be created in
	 * an early stage because it will referenced during the creation of agents
	 * because on creation agents will update their location. The locations are
	 * stored in the {@link #environment} object.
	 */
	public Continuous2D environment = new Continuous2D(100, WIDTH, HEIGHT);

	/**
	 * The public constructor. Takes a seed for initializing the random number
	 * generator and also a parameter set.
	 * 
	 * @param seed
	 */
	public ForagingHoneyBeeSimulation(long seed) {
		super(seed);

		prepareSimulation();
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
	 *            instance of type {@link ForagingHoneyBeeSimulation} or
	 *            {@link ParametrizedForagingHoneyBeeSimulation}.
	 */
	public void step(SimState state) {
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
	private void prepareSimulation() {
		// FIXME z coordinate is zero, should be LENGTH/2
		// create the hive
		Hive hive = new Hive(this, new Point3d(50, 50, 0), HIVE_DIAMETER, 5000);
		addAgent(hive);
		// create the entrance
		HiveEntrance entrance = new HiveEntrance(this, hive, 0);
		hive.setEntrance(entrance);
		addAgent(entrance);

		FoodSource f1 = new FoodSource(this, new Point3d(300, 100, 0), 20,
				new Color(0xd0, 0x00, 0x00), 100);
		addAgent(f1);
		FoodSource f2 = new FoodSource(this, new Point3d(50, 350, 0), 15,
				new Color(0xff, 0x80, 0x80), 200);
		addAgent(f2);
		FoodSource f3 = new FoodSource(this, new Point3d(200, 200, 0), 35,
				new Color(0xd0, 0x00, 0xd0), 300);
		addAgent(f3);
		FoodSource f4 = new FoodSource(this, new Point3d(180, 250, 0), 18,
				new Color(0x00, 0xd0, 0xd0), 300);
		addAgent(f4);

		Obstacle o = new Obstacle(this, new Point3d(155, 130, 0), 20);
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
				Bee b = new Bee(this, hive, new Point3d(fsl.x, fsl.y, 0));
				addAgent(b);
			}
		}

		/**
		 * Place *every* agent in the scheduler.
		 */
		ListIterator<IMovingAgent> li;
		li = agents.listIterator();
		while (li.hasNext()) {
			schedule.scheduleRepeating((Steppable) li.next());
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

			if (agent instanceof Hive) {
				hives.add((Hive) agent);
			}
			if (agent instanceof FoodSource) {
				foodSources.add((FoodSource) agent);
			}

			if (agent instanceof Obstacle)
				obstacles.add((Obstacle) agent);

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
	 * 
	 * @param agent
	 * @return True if the agent has been removed, false otherwise (because it
	 *         has never been stored).
	 */
	public boolean removeAgent(IMovingAgent agent) {
		boolean status = agents.remove(agent);
		if (agent instanceof Hive)
			hives.remove(agent);
		if (agent instanceof FoodSource)
			foodSources.remove(agent);
		if (agent instanceof Obstacle)
			obstacles.remove(agent);

		if (status) {
			double radius = agent.getSphereRadius();
			if (radius >= maxAgentSphereRadius) {
				maxAgentSphereRadius = computeMaxAgentSphereRadius();
			}
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
	 * This is a basic glue method demanded by the {@link IAgentLocator}
	 * interface. This method allows every agent to inform the simulation about
	 * a change in the agents location.
	 * 
	 * @param agent
	 *            The agent whose location has to be updated.
	 */
	public void updateLocation(IMovingAgent agent) {
		Point3d location = agent.getLocation();
		environment.setObjectLocation(agent, new Double2D(location.x,
				location.y));
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
	public Object[] getObjectsWithinDistance(IMovingAgent agent, double distance) {
		Point3d location = agent.getLocation();
		Bag b = environment.getObjectsWithinDistance(new Double2D(location.x,
				location.y), distance, false, false);
		return b.toArray();
	}

	/**
	 * Return the maximum radius of all agents inside this simulation.
	 * 
	 * @return The current maximum sphere.
	 */
	public double getMaxSphereRadius() {
		return this.maxAgentSphereRadius;
	}
}
