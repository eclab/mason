/**
 * 
 */
package de.thinktel.foragingBee.simulation;

import de.thinktel.foragingBee.masonGlue.IAgentVisualization;

/**
 * This interface provides visual information about an agent. This interface
 * extends the {@link IMovingAgent} interface because a visual agent needs a
 * location and size (provided by {@link IMovingAgent}) to be visualized.
 * 
 * @author hoehne
 * 
 */
public interface IVisualAgent extends IMovingAgent {

	/**
	 * Return the object that will be used for visualizing the this agent. The
	 * type of the object is not known due to different simulation environments
	 * so an interface {@link IAgentVisualization} is returned.
	 * 
	 * @return The visualization object.
	 */
	IAgentVisualization getVisualizationObject();

	/**
	 * Set the object that will be used for visualizing the this agent. The type
	 * of the object is not known due to different simulation environments so an
	 * interface {@link IAgentVisualization} is used.
	 * 
	 * @param visual
	 */
	void setVisualizationObject(IAgentVisualization visual);
}
