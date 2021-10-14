package sim.util.geo;

import sim.engine.SimState;
import sim.engine.Stoppable;
import sim.engine.Stopping;

public abstract class DGeomSteppable extends DGeomObject implements Stopping{
	private static final long serialVersionUID = 1L;

	Stoppable stop = null;

	public Stoppable getStoppable()
		{
		return stop;
		}

	public void setStoppable(Stoppable stop)
		{
		this.stop = stop;
		}

	public boolean isStopped()
		{
		return stop == null;
		}


}
