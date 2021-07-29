/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers;

import java.util.List;

import ec.util.MersenneTwisterFast;
import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.field.continuous.DContinuous2D;
import sim.portrayal.Oriented2D;
import sim.util.*;
import java.util.*;

public class DFlocker extends DSteppable implements Oriented2D
{

	private static final long serialVersionUID = 1;
	public Double2D loc;
	public Double2D lastd = new Double2D(0, 0);
	public boolean dead = false;

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

    public ArrayList<DFlocker> getNeighbors(DFlockers dFlockers)
        {
        return dFlockers.flockers.getNeighborsExactlyWithinDistance(loc, (double)DFlockers.neighborhood);
        }

	public void step(final SimState state)
	{
		
		
		final DFlockers dFlockers = (DFlockers) state;

		final Double2D oldloc = loc;

		if (dead)
			return;
		//List<DFlocker> b = null;
		//List<DFlocker> b = new ArrayList<DFlocker>();

		// try {
		//b = dFlockers.flockers.getNeighborsWithin(this, DFlockers.neighborhood);
		//b = dFlockers.flockers.getStorage().getNeighborsWithin(this, DFlockers.neighborhood); //this works too

		//this is the newest version
		
		//Bag b_bag = dFlockers.flockers.getNeighborsWithinDistance(this.loc, (double)DFlockers.neighborhood, true, true, null);
		//ArrayList b_bag = dFlockers.flockers.getNeighborsWithinDistance(this.loc, (double)DFlockers.neighborhood, null);
		//for(int i=0; i<b_bag.size(); i++)
		//{
		//	b.add((DFlocker)b_bag.get(i));
		//}
		ArrayList<DFlocker> b = getNeighbors(dFlockers);
		
		
		
		
		
		
//		}catch (Exception e) {
//			System.out.println("SIMULATION ERROR: agent "+this+ " on pid"+dFlockers.getPartition().getPid());
//		}

		final Double2D avoid = avoidance(b, dFlockers.flockers);
		final Double2D cohe = cohesion(b, dFlockers.flockers);
		final Double2D rand = randomness(dFlockers.random);
		final Double2D cons = consistency(b, dFlockers.flockers);
		final Double2D mome = momentum();

		double dx = DFlockers.cohesion * cohe.x + DFlockers.avoidance * avoid.x
				+ DFlockers.consistency * cons.x
				+ DFlockers.randomness * rand.x + DFlockers.momentum * mome.x;
		double dy = DFlockers.cohesion * cohe.y + DFlockers.avoidance * avoid.y
				+ DFlockers.consistency * cons.y
				+ DFlockers.randomness * rand.y + DFlockers.momentum * mome.y;

		// re-normalize to the given step size
		final double dis = Math.sqrt(dx * dx + dy * dy);
		if (dis > 0)
		{
			dx = dx / dis * DFlockers.jump;
			dy = dy / dis * DFlockers.jump;
		}
		lastd = new Double2D(dx, dy);
		loc = new Double2D(dFlockers.flockers.stx(loc.x + dx), dFlockers.flockers.sty(loc.y + dy));
		
//		// TESTING >>>>>>>>
//		// Sometimes, randomly send flocker to root node
//		if (state.random.nextBoolean(0.0001)) {
//			//TODO lastd?
//			loc = new Double2D(0,0);
//			System.out.println("moved flocker to origin");
//		}
//		// <<<<<<<<<<<<<<<<
		
		try
		{
			dFlockers.flockers.moveAgent(loc, this);
		}
		catch (Exception e)
		{
			System.err.println("error on agent " + this + " in step " + dFlockers.schedule.getSteps() + "on pid "
					+ dFlockers.getPartition().getPID());
			throw new RuntimeException(e);
		}
	}

	public String toString()
	{
		return super.toString() +
				" [loc=" + loc + ", lastd=" + lastd + ", dead=" + dead + "]";
	}
}