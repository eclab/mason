/*
\  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockerstest;

import java.rmi.Remote;
import java.util.List;

import ec.util.MersenneTwisterFast;
import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.field.continuous.DContinuous2D;
import sim.util.*;

public class DFlocker extends DSteppable implements Remote
{
	private static final long serialVersionUID = 1;
	public Double2D loc;
	public Double2D lastd = new Double2D(0, 0);
	public boolean dead = false;

//	public int id;

	public DFlocker(final Double2D location)
	{
		this.loc = location;
	}

	public double getOrientation()
	{
		return orientation2D();
	}

	public boolean isDead()
	{
		return dead;
	}

	public void setDead(final boolean val)
	{
		dead = val;
	}

	public void setOrientation2D(final double val)
	{
		lastd = new Double2D(Math.cos(val), Math.sin(val));
	}

	public double orientation2D()
	{
		if (lastd.x == 0 && lastd.y == 0)
			return 0;
		return Math.atan2(lastd.y, lastd.x);
	}

	public Double2D momentum()
	{
		return lastd;
	}

	public Double2D consistency(final List<DFlocker> b, final DContinuous2D<DFlocker> flockers)
	{
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);

		double x = 0;
		double y = 0;
		int i = 0;
		int count = 0;
		for (i = 0; i < b.size(); i++)
		{
			final DFlocker other = (b.get(i));
			if (!other.dead)
			{
				final Double2D m = b.get(i).momentum();
				count++;
				x += m.x;
				y += m.y;
			}
		}
		if (count > 0)
		{
			x /= count;
			y /= count;
		}
		return new Double2D(x, y);
	}

	public Double2D cohesion(final List<DFlocker> b, final DContinuous2D<DFlocker> flockers)
	{
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);

		double x = 0;
		double y = 0;

		int count = 0;
		int i = 0;
		for (i = 0; i < b.size(); i++)
		{
			final DFlocker other = (b.get(i));
			if (!other.dead)
			{
				final double dx = flockers.tdx(loc.x, other.loc.x);
				final double dy = flockers.tdy(loc.y, other.loc.y);
				count++;
				x += dx;
				y += dy;
			}
		}
		if (count > 0)
		{
			x /= count;
			y /= count;
		}
		return new Double2D(-x / 10, -y / 10);
	}

	public Double2D avoidance(final List<DFlocker> b, final DContinuous2D<DFlocker> flockers)
	{
		if (b == null || b.size() == 0)
			return new Double2D(0, 0);
		double x = 0;
		double y = 0;

		int i = 0;
		int count = 0;

		for (i = 0; i < b.size(); i++)
		{
			final DFlocker other = (b.get(i));
			if (other != this)
			{
				final double dx = flockers.tdx(loc.x, other.loc.x);
				final double dy = flockers.tdy(loc.y, other.loc.y);
				final double lensquared = dx * dx + dy * dy;
				count++;
				x += dx / (lensquared * lensquared + 1);
				y += dy / (lensquared * lensquared + 1);
			}
		}
		if (count > 0)
		{
			x /= count;
			y /= count;
		}
		return new Double2D(400 * x, 400 * y);
	}

	public Double2D randomness(final MersenneTwisterFast r)
	{
		final double x = r.nextDouble() * 2 - 1.0;
		final double y = r.nextDouble() * 2 - 1.0;
		final double l = Math.sqrt(x * x + y * y);
		return new Double2D(0.05 * x / l, 0.05 * y / l);
	}

	public void step(final SimState state)
	{
		final DFlockersTest dFlockers = (DFlockersTest) state;

		final Double2D oldloc = loc;

		if (dead)
			return;
		List<DFlocker> b = null;

		try
		{
			b = dFlockers.flockers.getNeighborsWithinDistance(loc, DFlockersTest.neighborhood);
		}
		catch (Exception e)
		{
			System.out.println(dFlockers.getPartition().getPID());
		}

		final Double2D avoid = avoidance(b, dFlockers.flockers);
		final Double2D cohe = cohesion(b, dFlockers.flockers);
		final Double2D rand = randomness(dFlockers.random);
		final Double2D cons = consistency(b, dFlockers.flockers);
		final Double2D mome = momentum();

		double dx = DFlockersTest.cohesion * cohe.x + DFlockersTest.avoidance * avoid.x
				+ DFlockersTest.consistency * cons.x
				+ DFlockersTest.randomness * rand.x + DFlockersTest.momentum * mome.x;
		double dy = DFlockersTest.cohesion * cohe.y + DFlockersTest.avoidance * avoid.y
				+ DFlockersTest.consistency * cons.y
				+ DFlockersTest.randomness * rand.y + DFlockersTest.momentum * mome.y;

		// re-normalize to the given step size
		final double dis = Math.sqrt(dx * dx + dy * dy);
		if (dis > 0)
		{
			dx = dx / dis * DFlockersTest.jump;
			dy = dy / dis * DFlockersTest.jump;
		}
		lastd = new Double2D(dx, dy);
		loc = new Double2D(dFlockers.flockers.stx(loc.x + dx), dFlockers.flockers.sty(loc.y + dy));
		try
		{
			dFlockers.flockers.moveAgent(loc, this);
		}
		catch (Exception e)
		{
			System.err.println("error on agent " + this + " in step " + dFlockers.schedule.getSteps() + "on pid "
					+ dFlockers.getPartition().getPID());
			System.exit(1);
		}

//		try {
//			final int dst = dFlockers.partition.toPartitionPID(new double[] { loc.c(0), loc.c(1) });
//			if (dst != dFlockers.partition.getPid()) {
//				// Need to migrate to other partition,
//				// remove from current partition
//				dFlockers.flockers.remove(this);
//				// TODO: Abstract away the migration from the model
//				dFlockers.transporter.migrateAgent(this, dst, loc, dFlockers.flockers.fieldIndex);
//			} else {
//				// Set to new location in current partition
//				// TODO: to use moveAgent in the future
//				dFlockers.flockers.move(old, loc, this);
//				dFlockers.schedule.scheduleOnce(this, 1);
//			}
//		} catch (final Exception e) {
//			e.printStackTrace(System.out);
//			System.exit(-1);
//		}
	}

	public String toString()
	{
		return super.toString() +
				" [loc=" + loc + ", lastd=" + lastd + ", dead=" + dead + "]";
	}

}