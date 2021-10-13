package sim.engine.mpi;

import java.io.Serializable;

import sim.engine.Stopping;

// This class is not supposed to be used by the modelers
/**
 * Used internally
 *
 */
public class AgentWrapper extends MigratableObject implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Ordering for the scheduler <br>
	 * Optional field <br>
	 * <br>
	 * Default: 1
	 */
	public int ordering;

	/**
	 * time for the scheduler. Values less than zero are considered invalid <br>
	 * Optional field <br>
	 * <br>
	 * Default: -1.0
	 */
	public double time;

	public Stopping agent;
	
	public double interval; 

	public AgentWrapper(final int ordering, final double time, final Stopping agent)
	{
		this.ordering = ordering;
		this.time = time;
		this.agent = agent;
		this.interval = -1;
	}

	public AgentWrapper(final int ordering, final double time, final Stopping agent, double interval)
	{
		this.ordering = ordering;
		this.time = time;
		this.agent = agent;
		this.interval = interval;
	}
	
	public boolean isRepeating()
		{
		return interval < 0;
		}

	public String toString()
	{
		return "AgentWrapper [ordering=" + ordering + ", time=" + time + ", agent=" + agent + ", interval=" + (interval < 0 ? "None" : interval) + "]";
	}

}
