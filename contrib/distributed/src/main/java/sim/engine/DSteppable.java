/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

import java.util.concurrent.atomic.AtomicInteger;

/** A simple implementation of Stopping in a Steppable. */

public abstract class DSteppable implements Steppable, Stopping {
	private static final long serialVersionUID = 1;

	static final AtomicInteger counter = new AtomicInteger();

	public final int pid;
	public final int agentId;

	Stoppable stop = null;

	public DSteppable(int pid) {
		super();
		this.pid = pid;
		this.agentId = counter.getAndIncrement();
	}

	public Stoppable getStoppable() {
		return stop;
	}

	public void setStoppable(Stoppable stop) {
		this.stop = stop;
	}

	public long getId() {
		return (10000 + (long) pid) * 100000000 + (long) agentId;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + agentId;
		result = prime * result + pid;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DSteppable other = (DSteppable) obj;
		if (agentId != other.agentId)
			return false;
		if (pid != other.pid)
			return false;
		return true;
	}

//public abstract class DSteppable implements Steppable, Stopping {
//	private static final long serialVersionUID = 1;
//
//	private Stoppable stop = null;
//
//	Object[] lock = new Object[0];
//
//	public Stoppable getStoppable() {
//		synchronized (lock) {
//			return stop;
//		}
//	}
//
//	public void setStoppable(Stoppable stop) {
//		synchronized (lock) {
//			this.stop = stop;
//		}
//	}
//}

}
