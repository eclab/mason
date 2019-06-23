package sim.field;

import java.io.Serializable;

import sim.engine.Steppable;

/**
 * Used internally
 *
 */
public class AgentWrapper implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Ordering for the scheduler <br>
	 * Optional field <br>
	 * <br>
	 * <b>Default:</b> 1
	 */
	public final int ordering;

	/**
	 * time for the scheduler. Values less than zero are considered invalid <br>
	 * Optional field <br>
	 * <br>
	 * <b>Default:</b> -1.0
	 */
	public final double time;

	public final Steppable agent;

	public AgentWrapper(final Steppable agent) {
		ordering = 1;
		time = -1.0;
		this.agent = agent;
	}

	public AgentWrapper(final int ordering, final Steppable agent) {
		this.ordering = ordering;
		time = -1.0;
		this.agent = agent;
	}

	public AgentWrapper(final double time, final Steppable agent) {
		ordering = 1;
		this.time = time;
		this.agent = agent;
	}

	public AgentWrapper(final int ordering, final double time, final Steppable agent) {
		this.ordering = ordering;
		this.time = time;
		this.agent = agent;
	}

	public String toString() {
		return "AgentWrapper [ordering=" + ordering + ", time=" + time + ", agent=" + agent + "]";
	}

}
