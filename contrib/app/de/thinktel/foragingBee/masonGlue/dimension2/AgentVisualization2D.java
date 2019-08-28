/**
 * 
 */
package de.thinktel.foragingBee.masonGlue.dimension2;

import java.awt.Color;

import sim.portrayal.SimplePortrayal2D;
import de.thinktel.foragingBee.masonGlue.IAgentVisualization;
import de.thinktel.foragingBee.simulation.IVisualAgent;

/**
 * This abstract class is used to gather all information used for displaying an
 * agent in subclasses in a two dimensional displaying world.
 * <p>
 * This is a glue class to MASON so it inherits from {@link SimplePortrayal2D}
 * for displaying.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public abstract class AgentVisualization2D extends SimplePortrayal2D implements
		IAgentVisualization {
	/**
	 * The agent that will be visualized but has no ability to do it on its own.
	 */
	IVisualAgent agent;

	/**
	 * The agent's current color.
	 */
	Color color;

	/**
	 * The constructor that simply takes the {@link IVisualAgent} as an
	 * argument.
	 * 
	 * @param agent
	 */
	public AgentVisualization2D(IVisualAgent agent) {
		this.agent = agent;
	}

	/**
	 * Return the agent the visual object is visualizing.
	 * 
	 * @return The visualized agent that has to be displayed by an
	 *         {@link IAgentVisualization}.
	 */
	public IVisualAgent getAgent() {
		return agent;
	}

	/**
	 * Return the current agent's color.
	 * 
	 * @return The current color of the agent.
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Set the color of the agent's visualization.
	 * 
	 * @param color
	 *            The new color of the agent.
	 */
	public void setColor(Color color) {
		this.color = color;
	}
}
