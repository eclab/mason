package sim.engine;

import java.io.Serializable;

import sim.engine.Stopping;

// This class is not supposed to be used by the modelers
/**
 * Used internally
 *
 */
class AgentWrapper implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Ordering for the scheduler <br>
	 * Optional field <br>
	 * <br>
	 * Default: 1
	 */
	final int ordering;

	/**
	 * time for the scheduler. Values less than zero are considered invalid <br>
	 * Optional field <br>
	 * <br>
	 * Default: -1.0
	 */
	final double time;

	final Stopping agent;

	AgentWrapper(final Stopping agent) {
		ordering = 1;
		time = -1.0;
		this.agent = agent;
	}

	AgentWrapper(final int ordering, final Stopping agent) {
		this.ordering = ordering;
		time = -1.0;
		this.agent = agent;
	}

	AgentWrapper(final double time, final Stopping agent) {
		ordering = 1;
		this.time = time;
		this.agent = agent;
	}

	AgentWrapper(final int ordering, final double time, final Stopping agent) {
		this.ordering = ordering;
		this.time = time;
		this.agent = agent;
	}

	public String toString() {
		return "AgentWrapper [ordering=" + ordering + ", time=" + time + ", agent=" + agent + "]";
	}

}
