package sim.field;

import java.io.Serializable;
import sim.util.DoublePoint;

public class MigratingAgent implements Serializable
{
	public int destination;
	public boolean migrate;
	public Object wrappedAgent;
	public DoublePoint loc;
	public int identityHashcode;

	public MigratingAgent(final int dst, final Object agent, final DoublePoint loc, final boolean migrate) {
		this.destination = dst;
		this.wrappedAgent = agent;
		this.migrate = migrate;
		this.loc = loc;
	}

	public MigratingAgent(final int dst, final Object agent, final DoublePoint loc) {
		this(dst, agent, loc, false);
	}
}
