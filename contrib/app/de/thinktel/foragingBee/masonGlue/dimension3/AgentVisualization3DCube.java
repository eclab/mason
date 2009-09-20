/**
 * 
 */
package de.thinktel.foragingBee.masonGlue.dimension3;

import java.awt.Color;

import javax.media.j3d.TransformGroup;

import sim.portrayal3d.simple.BoxPortrayal3D;
import sim.portrayal3d.simple.CubePortrayal3D;
import de.thinktel.foragingBee.masonGlue.IAgentVisualization;
import de.thinktel.foragingBee.simulation.IVisualAgent;

/**
 * This abstract class is used to gather all information used for displaying an
 * agent in subclasses in a (pseudo) three dimensional displaying world.
 * <p>
 * This is a glue class to MASON so it inherits from {@link CubePortrayal3D} for
 * displaying a cube.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class AgentVisualization3DCube extends BoxPortrayal3D implements
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
	 * A flag indicating the color of the visualization changed.
	 */
	boolean colorChanged = false;

	/**
	 * The constructor that simply takes the {@link IVisualAgent} as an
	 * argument.
	 * 
	 * @param agent
	 */
	public AgentVisualization3DCube(IVisualAgent agent) {
		super((float) (agent.getSphereRadius() * 2));
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
	 */
	public void setColor(Color color) {
		// store the color for later use
		colorChanged = this.color == null;
		if (!colorChanged) {
			colorChanged = !this.color.equals(color);
		}
		if (colorChanged)
			this.color = color;
	}

	public TransformGroup getModel(Object obj, TransformGroup j3dModel) {
		if (j3dModel != null) {
			if (colorChanged) {
				setAppearance(j3dModel, appearanceForColors(color, // ambient
																	// color
						null, // emissive color (black)
						color, // diffuse color
						null, // specular color (white)
						1.0f, // no shininess
						1.0f // full opacity
						));
				colorChanged = false;
			}
		}

		return super.getModel(obj, j3dModel);
	}
}
