/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package foragingBee;

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
	public void updateLocation(IMovingAgent agent);
}
