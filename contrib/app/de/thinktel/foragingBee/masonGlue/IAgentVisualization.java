/**
 * 
 */
package de.thinktel.foragingBee.masonGlue;

import java.awt.Color;

import de.thinktel.foragingBee.simulation.IVisualAgent;

/**
 * The interface with the methods a visualization object has to support.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 */
public interface IAgentVisualization {
	/**
	 * Return the agent the visual object is visualizing.
	 * 
	 * @return The visualized agent that has to be displayed by an
	 *         {@link IAgentVisualization}.
	 */
	IVisualAgent getAgent();

	/**
	 * Return the current agent's color.
	 * 
	 * @return The current color of the agent.
	 */
	public Color getColor();

	/**
	 * Set the color of the agent's visualization.
	 * 
	 * @param color
	 */
	void setColor(Color color);
}
