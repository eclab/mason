package sim.app.dflockers.dregistry;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ec.util.MersenneTwisterFast;
import mpi.MPI;
import mpi.MPIException;
import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.field.continuous.DContinuous2D;
import sim.util.*;

interface DFlockerDummyRemote extends Remote{
	public  int addAndGetVal() throws RemoteException;
	public int getVal() throws RemoteException;

}

public class DFlockerWithDRegistry extends DSteppable implements DFlockerDummyRemote {
	private static final long serialVersionUID = 1;
	public Double2D loc;
	public Double2D lastd = new Double2D(0, 0);
	public boolean dead = false;
	
	public boolean amItheBoss = false;
	public int id;
	
	public int oldProcessingMPInode = 0;
	
	AtomicInteger val = new AtomicInteger(0);

	public DFlockerWithDRegistry(final Double2D location, final int id) {
		loc = location;
		this.id = id;
	}

	public double getOrientation() {
		return orientation2D();
	}

	public boolean isDead() {
		return dead;
	}

	public void setDead(final boolean val) {
		dead = val;
	}

	public void setOrientation2D(final double val) {
		lastd = new Double2D(Math.cos(val), Math.sin(val));
	}

	public double orientation2D() {
		if (lastd.c(0) == 0 && lastd.c(1) == 0)
			return 0;
		return Math.atan2(lastd.c(1), lastd.c(0));
	}

	public Double2D momentum() {
		return lastd;
	}

	public Double2D consistency(final List<DFlockerWithDRegistry> b, final DContinuous2D<DFlockerWithDRegistry> flockers) {
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);

		double x = 0;
		double y = 0;
		int i = 0;
		int count = 0;
		for (i = 0; i < b.size(); i++) {
			final DFlockerWithDRegistry other = (b.get(i));
			if (!other.dead) {
				final Double2D m = b.get(i).momentum();
				count++;
				x += m.c(0);
				y += m.c(1);
			}
		}
		if (count > 0) {
			x /= count;
			y /= count;
		}
		return new Double2D(x, y);
	}

	public Double2D cohesion(final List<DFlockerWithDRegistry> b, final DContinuous2D<DFlockerWithDRegistry> flockers) {
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);

		double x = 0;
		double y = 0;

		int count = 0;
		int i = 0;
		for (i = 0; i < b.size(); i++) {
			final DFlockerWithDRegistry other = (b.get(i));
			if (!other.dead) {
				final double dx = flockers.tdx(loc.c(0), other.loc.c(0));
				final double dy = flockers.tdy(loc.c(1), other.loc.c(1));
				count++;
				x += dx;
				y += dy;
			}
		}
		if (count > 0) {
			x /= count;
			y /= count;
		}
		return new Double2D(-x / 10, -y / 10);
	}

	public Double2D avoidance(final List<DFlockerWithDRegistry> b, final DContinuous2D<DFlockerWithDRegistry> flockers) {
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);
		double x = 0;
		double y = 0;

		int i = 0;
		int count = 0;

		for (i = 0; i < b.size(); i++) {
			final DFlockerWithDRegistry other = (b.get(i));
			if (other != this) {
				final double dx = flockers.tdx(loc.c(0), other.loc.c(0));
				final double dy = flockers.tdy(loc.c(1), other.loc.c(1));
				final double lensquared = dx * dx + dy * dy;
				count++;
				x += dx / (lensquared * lensquared + 1);
				y += dy / (lensquared * lensquared + 1);
			}
		}
		if (count > 0) {
			x /= count;
			y /= count;
		}
		return new Double2D(400 * x, 400 * y);
	}

	public Double2D randomness(final MersenneTwisterFast r) {
		final double x = r.nextDouble() * 2 - 1.0;
		final double y = r.nextDouble() * 2 - 1.0;
		final double l = Math.sqrt(x * x + y * y);
		return new Double2D(0.05 * x / l, 0.05 * y / l);
	}
	
	public int addAndGetVal() {
		return val.addAndGet(1);
	}
	public  int getVal() {
		return val.get();
	}

	public void step(final SimState state) {
		
		try {
			if(amItheBoss && oldProcessingMPInode != MPI.COMM_WORLD.getRank()) {
				System.out.println(MPI.COMM_WORLD.getRank()+"] cafebabe moved from "+oldProcessingMPInode+" to "+ MPI.COMM_WORLD.getRank());
				oldProcessingMPInode= MPI.COMM_WORLD.getRank();
			}
		} catch (MPIException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		final DFlockersWithDRegistry dFlockers = (DFlockersWithDRegistry) state;
		
		final Double2D oldloc = loc;
		loc = (Double2D) dFlockers.flockers.getLocation(this);
	
		if (loc == null) {
			System.out.printf("pid %d oldx %g oldy %g", dFlockers.getPartitioning().pid, oldloc.c(0), oldloc.c(1));
			Thread.dumpStack();
			System.exit(-1);
		}

		if (dead)
			return;

		final List<DFlockerWithDRegistry> b = dFlockers.flockers.getNeighborsWithin(this, DFlockersWithDRegistry.neighborhood);

		final Double2D avoid = avoidance(b, dFlockers.flockers);
		final Double2D cohe = cohesion(b, dFlockers.flockers);
		final Double2D rand = randomness(dFlockers.random);
		final Double2D cons = consistency(b, dFlockers.flockers);
		final Double2D mome = momentum();

		double dx = DFlockersWithDRegistry.cohesion * cohe.c(0) + DFlockersWithDRegistry.avoidance * avoid.c(0)
				+ DFlockersWithDRegistry.consistency * cons.c(0)
				+ DFlockersWithDRegistry.randomness * rand.c(0) + DFlockersWithDRegistry.momentum * mome.c(0);
		double dy = DFlockersWithDRegistry.cohesion * cohe.c(1) + DFlockersWithDRegistry.avoidance * avoid.c(1)
				+ DFlockersWithDRegistry.consistency * cons.c(1)
				+ DFlockersWithDRegistry.randomness * rand.c(1) + DFlockersWithDRegistry.momentum * mome.c(1);

		// re-normalize to the given step size
		final double dis = Math.sqrt(dx * dx + dy * dy);
		if (dis > 0) {
			dx = dx / dis * DFlockersWithDRegistry.jump;
			dy = dy / dis * DFlockersWithDRegistry.jump;
		}

		final Double2D old = loc;
		loc = new Double2D(dFlockers.flockers.stx(loc.c(0) + dx), dFlockers.flockers.sty(loc.c(1) + dy));

		try {
			DFlockerDummyRemote myfriend = (DFlockerDummyRemote) dFlockers.getDRegistry().getObject("cafebabe");
			myfriend.addAndGetVal();
			
		} catch (AccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		dFlockers.flockers.moveAgent(old, loc, this);

	}
}
