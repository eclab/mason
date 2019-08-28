/**
 * 
 */
package de.thinktel.foragingBee.masonGlue.dimension3;

import javax.vecmath.Point3d;

import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import de.thinktel.foragingBee.masonGlue.ForagingHoneyBeeSimulation;
import de.thinktel.foragingBee.masonGlue.IAgentVisualization;
import de.thinktel.foragingBee.simulation.Bee;
import de.thinktel.foragingBee.simulation.IAgentLocator;
import de.thinktel.foragingBee.simulation.IMovingAgent;
import de.thinktel.foragingBee.simulation.IVisualAgent;

/**
 * This is the 3D version of {@link ForagingHoneyBeeSimulation}.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class ForagingHoneyBeeSimulation3D extends ForagingHoneyBeeSimulation {
	/**
	 * An environment the agents are moving in. This object has to be created in
	 * an early stage because it will referenced during the creation of agents
	 * because on creation agents will update their location. The locations are
	 * stored in the {@link #environment} object.
	 */
	public Continuous3D environment = new Continuous3D(100, WIDTH, HEIGHT,
			LENGTH);

	/**
	 * The public constructor. Takes a seed for initializing the random number
	 * generator and a mode switch to 3d.
	 * 
	 * @param seed The seed for the random number generator.
	 * @param is3dMode Set to true if this simulation runs in 3d (usually true).
	 */
	public ForagingHoneyBeeSimulation3D(long seed, boolean is3dMode) {
		super(seed, is3dMode);
	}

	void init() {
		environment = new Continuous3D(100, WIDTH, HEIGHT, LENGTH);
	}

	/**
	 * This is a basic glue method demanded by the {@link IAgentLocator}
	 * interface. This method allows every agent to inform the simulation about
	 * a change in the agents location.
	 * 
	 * @param agent
	 *            The agent whose location has to be updated.
	 */
	public void updateLocation(IVisualAgent agent) {
		Point3d location = agent.getLocation();
		IAgentVisualization visual = (IAgentVisualization) agent
				.getVisualizationObject();

		environment.setObjectLocation(visual, new Double3D(location.x,
				location.y, location.z));
	}

	/**
	 * Create a visualization object for the given agent. This method will
	 * return an object that will be able to display according to the given
	 * type.
	 * 
	 * @param agent
	 *            The agent for which the visualization object will be created
	 *            for.
	 * @return The visualization object.
	 */
	public IAgentVisualization createVisual(IVisualAgent agent) {
		if (agent instanceof Bee) {
			return new AgentVisualization3DCube(agent);
		}

		return new AgentVisualization3DSpheric(agent);
	}

	/**
	 * Remove this agent from visualization.
	 * 
	 * @param agent
	 *            The agent to be removed.
	 */
	public Object removeFromVisualization(IVisualAgent agent) {
		return environment.remove(agent.getVisualizationObject());
	}

	/**
	 * Compute every agent that is within a certain location from an agent. This
	 * method assumes all objects are points so only the objects. Due to the
	 * algorithm provided by MASON objects with a distance at least as provided
	 * are included.<br>
	 * Because MASON stores the visual objects in the {@link #environment} the
	 * simulation agents have to be retrieved first before the array is
	 * returned.
	 * 
	 * @param agent
	 *            The agent where to start.
	 * @param distance
	 *            The distance (radius) the returned agents should lie within.
	 * @return All objects that are at least in the distance.
	 */
	public Object[] getObjectsWithinDistance(IMovingAgent agent, double distance) {
		Point3d location = agent.getLocation();
		Bag b = environment.getObjectsWithinDistance(new Double3D(location.x,
				location.y, location.z), distance, false, false);

		Object[] agents = new Object[b.numObjs];
		int i;
		for (i = 0; i < agents.length; i++)
			agents[i] = ((IAgentVisualization) b.objs[i]).getAgent();
		return agents;
	}
}
