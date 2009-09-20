/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.foragingBee.simulation;

import de.thinktel.foragingBee.masonGlue.IAgentVisualization;

/**
 * This interface defines methods agents will expect from the simulation
 * environment. These methods are glue methods to allow the agents to access
 * MASON specific functionality.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public interface IAgentLocator {
	/**
	 * An agent will call this method to inform the simulation framework that
	 * the agents location changed.
	 * 
	 * @param agent
	 */
	void updateLocation(IVisualAgent agent);

	/**
	 * Create a visualization object for the given agent. The type of
	 * visualization is not known due to the usage of different simulation
	 * environments.
	 * 
	 * @param agent
	 *            The agent for which the visualization object will be created
	 *            for.
	 * @return The visualization object.
	 */
	IAgentVisualization createVisual(IVisualAgent agent);

	/**
	 * Remove this agent from visualization.
	 * 
	 * @param agent
	 *            The agent to be removed.
	 * @return Some information.
	 */
	Object removeFromVisualization(IVisualAgent agent);
}
