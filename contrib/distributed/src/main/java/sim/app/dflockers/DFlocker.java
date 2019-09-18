/*
\  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers;

import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

import java.util.List;

import ec.util.*;

public class DFlocker extends AbstractStopping implements sim.portrayal.Orientable2D {
	private static final long serialVersionUID = 1;

	public DoublePoint loc;
	public DoublePoint lastd = new DoublePoint(0, 0);
	public boolean dead = false;

	public DFlocker(final DoublePoint location) {
		loc = location;
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
		lastd = new DoublePoint(Math.cos(val), Math.sin(val));
	}

	public double orientation2D() {
		if (lastd.c[0] == 0 && lastd.c[1] == 0)
			return 0;
		return Math.atan2(lastd.c[1], lastd.c[0]);
	}

	public DoublePoint momentum() {
		return lastd;
	}

	public DoublePoint consistency(final List<DFlocker> b, final NContinuous2D<DFlocker> flockers) {
		if (b == null || b.size() == 0)
			return new DoublePoint(0, 0);

		double x = 0;
		double y = 0;
		int i = 0;
		int count = 0;
		for (i = 0; i < b.size(); i++) {
			final DFlocker other = (b.get(i));
			if (!other.dead) {
				final DoublePoint m = b.get(i).momentum();
				count++;
				x += m.c[0];
				y += m.c[1];
			}
		}
		if (count > 0) {
			x /= count;
			y /= count;
		}
		return new DoublePoint(x, y);
	}

	public DoublePoint cohesion(final List<DFlocker> b, final NContinuous2D<DFlocker> flockers) {
		if (b == null || b.size() == 0)
			return new DoublePoint(0, 0);

		double x = 0;
		double y = 0;

		int count = 0;
		int i = 0;
		for (i = 0; i < b.size(); i++) {
			final DFlocker other = (b.get(i));
			if (!other.dead) {
				final double dx = flockers.tdx(loc.c[0], other.loc.c[0]);
				final double dy = flockers.tdy(loc.c[1], other.loc.c[1]);
				count++;
				x += dx;
				y += dy;
			}
		}
		if (count > 0) {
			x /= count;
			y /= count;
		}
		return new DoublePoint(-x / 10, -y / 10);
	}

	public DoublePoint avoidance(final List<DFlocker> b, final NContinuous2D<DFlocker> flockers) {
		if (b == null || b.size() == 0)
			return new DoublePoint(0, 0);
		double x = 0;
		double y = 0;

		int i = 0;
		int count = 0;

		for (i = 0; i < b.size(); i++) {
			final DFlocker other = (b.get(i));
			if (other != this) {
				final double dx = flockers.tdx(loc.c[0], other.loc.c[0]);
				final double dy = flockers.tdy(loc.c[1], other.loc.c[1]);
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
		return new DoublePoint(400 * x, 400 * y);
	}

	public DoublePoint randomness(final MersenneTwisterFast r) {
		final double x = r.nextDouble() * 2 - 1.0;
		final double y = r.nextDouble() * 2 - 1.0;
		final double l = Math.sqrt(x * x + y * y);
		return new DoublePoint(0.05 * x / l, 0.05 * y / l);
	}

	public void step(final SimState state) {
		final DFlockers dFlockers = (DFlockers) state;
		final DoublePoint oldloc = loc;
		loc = (DoublePoint) dFlockers.flockers.getLocation(this);
		if (loc == null) {
			System.out.printf("pid %d oldx %g oldy %g", dFlockers.getPartition().pid, oldloc.c[0], oldloc.c[1]);
			Thread.dumpStack();
			System.exit(-1);
		}

		if (dead)
			return;

		final List<DFlocker> b = dFlockers.flockers.getNeighborsWithin(this, DFlockers.neighborhood);

		final DoublePoint avoid = avoidance(b, dFlockers.flockers);
		final DoublePoint cohe = cohesion(b, dFlockers.flockers);
		final DoublePoint rand = randomness(dFlockers.random);
		final DoublePoint cons = consistency(b, dFlockers.flockers);
		final DoublePoint mome = momentum();

		double dx = DFlockers.cohesion * cohe.c[0] + DFlockers.avoidance * avoid.c[0]
				+ DFlockers.consistency * cons.c[0]
				+ DFlockers.randomness * rand.c[0] + DFlockers.momentum * mome.c[0];
		double dy = DFlockers.cohesion * cohe.c[1] + DFlockers.avoidance * avoid.c[1]
				+ DFlockers.consistency * cons.c[1]
				+ DFlockers.randomness * rand.c[1] + DFlockers.momentum * mome.c[1];

		// re-normalize to the given step size
		final double dis = Math.sqrt(dx * dx + dy * dy);
		if (dis > 0) {
			dx = dx / dis * DFlockers.jump;
			dy = dy / dis * DFlockers.jump;
		}

		final DoublePoint old = loc;
		loc = new DoublePoint(dFlockers.flockers.stx(loc.c[0] + dx), dFlockers.flockers.sty(loc.c[1] + dy));

		dFlockers.flockers.moveAgent(old, loc, this);
//		try {
//			final int dst = dFlockers.partition.toPartitionId(new double[] { loc.c[0], loc.c[1] });
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
}
