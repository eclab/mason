/**
 * 
 */
package de.thinktel.foragingBee.masonGlue.dimension2;

import java.awt.Graphics2D;

import sim.portrayal.DrawInfo2D;
import de.thinktel.foragingBee.simulation.IVisualAgent;

/**
 * This class provides the ability to draw itself as an sphere in a 2D world.
 * 
 * @author hoehne
 * 
 */
public class AgentVisualization2DSpheric extends AgentVisualization2D {
	/**
	 * The constructor that takes a visual agent interface as an argument.
	 * 
	 * @param agent
	 */
	public AgentVisualization2DSpheric(IVisualAgent agent) {
		super(agent);
	}

	// ========== SimplePortrayal2D ==========

	/**
	 * This is the final method this agent uses to draw itself as a sphere.
	 * 
	 * @param object
	 *            The object itself.
	 * @param graphics
	 *            Where (output device) to draw.
	 * @param info
	 *            Where (location) to draw.
	 */
	public final void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		double radiusX = info.draw.width * agent.getSphereRadius();
		double radiusY = info.draw.height * agent.getSphereRadius();

		graphics.setColor(getColor());

		graphics.fillOval((int) (info.draw.x - radiusX),
				(int) (info.draw.y - radiusY), (int) (radiusX * 2),
				(int) (radiusY * 2));
	}
}
