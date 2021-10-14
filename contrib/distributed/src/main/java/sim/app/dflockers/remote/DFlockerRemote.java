/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers.remote;

import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import ec.util.MersenneTwisterFast;
import sim.engine.DSteppable;
import sim.engine.Distinguished;
import sim.engine.Promised;
import sim.engine.SimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Double2D;

public class DFlockerRemote extends DSteppable implements Distinguished{

	private static final long serialVersionUID = 1;
	public Double2D loc;
	public Double2D lastd = new Double2D(0, 0);
	public boolean dead = false;

	public DFlockerRemote(final Double2D location) throws RemoteException {
		this.loc = location;
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
		if (lastd.x == 0 && lastd.y == 0)
			return 0;
		return Math.atan2(lastd.y, lastd.x);
	}

	public Double2D momentum() {
		return lastd;
	}

	public Double2D consistency(final List<DFlockerRemote> b, final DContinuous2D<DFlockerRemote> flockers) {
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);

		double x = 0;
		double y = 0;
		int i = 0;
		int count = 0;
		for (i = 0; i < b.size(); i++) {
			final DFlockerRemote other = (b.get(i));
			if (!other.dead) {
				final Double2D m = b.get(i).momentum();
				count++;
				x += m.x;
				y += m.y;
			}
		}
		if (count > 0) {
			x /= count;
			y /= count;
		}
		return new Double2D(x, y);
	}

	public Double2D cohesion(final List<DFlockerRemote> b, final DContinuous2D<DFlockerRemote> flockers) {
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);

		double x = 0;
		double y = 0;

		int count = 0;
		int i = 0;
		for (i = 0; i < b.size(); i++) {
			final DFlockerRemote other = (b.get(i));
			if (!other.dead) {
				final double dx = flockers.tdx(loc.x, other.loc.x);
				final double dy = flockers.tdy(loc.y, other.loc.y);
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

	public Double2D avoidance(final List<DFlockerRemote> b, final DContinuous2D<DFlockerRemote> flockers) {
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);
		double x = 0;
		double y = 0;

		int i = 0;
		int count = 0;

		for (i = 0; i < b.size(); i++) {
			final DFlockerRemote other = (b.get(i));
			if (other != this) {
				final double dx = flockers.tdx(loc.x, other.loc.x);
				final double dy = flockers.tdy(loc.y, other.loc.y);
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

    public ArrayList<DFlockerRemote> getNeighbors(DFlockersRemote dFlockers)
        {
        return dFlockers.flockers.getNeighborsExactlyWithinDistance(loc, (double)DFlockersRemote.neighborhood);
        }

	public void step(final SimState state) {
		final DFlockersRemote dFlockersRemote = (DFlockersRemote) state;

		final Double2D oldloc = loc;

		if (dead)
			return;
		
		ArrayList<DFlockerRemote> b = getNeighbors(dFlockersRemote);

		final Double2D avoid = avoidance(b, dFlockersRemote.flockers);
		final Double2D cohe = cohesion(b, dFlockersRemote.flockers);
		final Double2D rand = randomness(dFlockersRemote.random);
		final Double2D cons = consistency(b, dFlockersRemote.flockers);
		final Double2D mome = momentum();

		double dx = DFlockersRemote.cohesion * cohe.x + DFlockersRemote.avoidance * avoid.x
				+ DFlockersRemote.consistency * cons.x
				+ DFlockersRemote.randomness * rand.x + DFlockersRemote.momentum * mome.x;
		double dy = DFlockersRemote.cohesion * cohe.y + DFlockersRemote.avoidance * avoid.y
				+ DFlockersRemote.consistency * cons.y
				+ DFlockersRemote.randomness * rand.y + DFlockersRemote.momentum * mome.y;

		// re-normalize to the given step size
		final double dis = Math.sqrt(dx * dx + dy * dy);
		if (dis > 0) {
			dx = dx / dis * DFlockersRemote.jump;
			dy = dy / dis * DFlockersRemote.jump;
		}
		lastd = new Double2D(dx, dy);
		loc = new Double2D(dFlockersRemote.flockers.stx(loc.x + dx), dFlockersRemote.flockers.sty(loc.y + dy));
		
		try {
//			if(state.schedule.isRemote(agent)) // do remote
			Promised remoteData = dFlockersRemote.contactRemoteObj("cafebabe", 0, null);
			if(remoteData.isReady()) { 
				System.out.println("I got this data from cafebabe: \n" + remoteData.get());
			}
			
		} catch (AccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NotBoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
//		try {
//			for(int i=0; i<10; i++) {
//				DRemoteObj myfriend = dFlockersRemote.getDRegistry().getObjectT("flock" + i);
//				System.out.println("------Hey I'm flock" + i + " enjoy " + myfriend.respondToRemote());
//				String idRemoteObj = "flock" + i;
//				dFlockersRemote.contactRemoteObj(idRemoteObj, new String());
//			}
//		} catch (AccessException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (RemoteException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (NotBoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		
		try {
			dFlockersRemote.flockers.moveAgent(loc, this);
			System.out.println("Agent moved!!");
		} catch (Exception e) {
			System.err.println("error on agent " + this + " in step " + dFlockersRemote.schedule.getSteps() + "on pid "
					+ dFlockersRemote.getPartition().getPID());
			throw new RuntimeException(e);
		}
	}

	public Serializable respondToRemote(Integer tag, Serializable argument) {
		return "My location is" + this.loc + "]";
	}
	
	public String toString() {
		return super.toString() +
				" [loc=" + loc + ", lastd=" + lastd + ", dead=" + dead + "]";
	}

}