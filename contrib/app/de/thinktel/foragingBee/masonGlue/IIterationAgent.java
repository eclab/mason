/**
 * 
 */
package de.thinktel.foragingBee.masonGlue;

import sim.engine.Steppable;

/**
 * This interface provides information for the simulation environment for
 * accessing the scheduler for calling the interation method of this agent.
 * 
 * @author hoehne
 * 
 */
public interface IIterationAgent extends Steppable {
	/**
	 * Return the information a simulation environment needs to access
	 * information to the scheduler regarding this agent.
	 * 
	 * @return The information for the scheduler.
	 */
	Object getSchedulerInformation();

	/**
	 * Set information a simulation environment needs to access information to
	 * the scheduler regarding this agent.
	 * 
	 * @param o
	 *            The information.
	 */
	void setSchedulerInformation(Object o);
}
