/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

import java.util.List;


import ec.util.*;

public class DFlocker implements Steppable, sim.portrayal.Orientable2D {
    private static final long serialVersionUID = 1;

    public DoublePoint loc = new DoublePoint(0, 0);
    public DoublePoint lastd = new DoublePoint(0, 0);
    public boolean dead = false;

    public DFlocker(DoublePoint location) { loc = location;}

    public double getOrientation() { return orientation2D(); }
    public boolean isDead() { return dead; }
    public void setDead(boolean val) { dead = val; }

    public void setOrientation2D(double val) {
        lastd = new DoublePoint(Math.cos(val), Math.sin(val));
    }

    public double orientation2D() {
        if (lastd.c[0] == 0 && lastd.c[1] == 0) return 0;
        return Math.atan2(lastd.c[1], lastd.c[0]);
    }

    public DoublePoint momentum() {
        return lastd;
    }

    public DoublePoint consistency(List<DFlocker> b, NContinuous2D<DFlocker> flockers) {
        if (b == null || b.size() == 0) return new DoublePoint(0, 0);

        double x = 0;
        double y = 0;
        int i = 0;
        int count = 0;
        for (i = 0; i < b.size(); i++) {
            DFlocker other = (DFlocker)(b.get(i));
            if (!other.dead) {
            	DoublePoint m = ((DFlocker)b.get(i)).momentum();
                count++;
                x += m.c[0];
                y += m.c[1];
            }
        }
        if (count > 0) { x /= count; y /= count; }
        return new DoublePoint(x, y);
    }

    public DoublePoint cohesion(List<DFlocker> b, NContinuous2D<DFlocker> flockers) {
        if (b == null || b.size() == 0) return new DoublePoint(0, 0);

        double x = 0;
        double y = 0;

        int count = 0;
        int i = 0;
        for (i = 0; i < b.size(); i++) {
            DFlocker other = (DFlocker)(b.get(i));
            if (!other.dead) {
                double dx = flockers.tdx(loc.c[0], other.loc.c[0]);
                double dy = flockers.tdy(loc.c[1], other.loc.c[1]);
                count++;
                x += dx;
                y += dy;
            }
        }
        if (count > 0) { x /= count; y /= count; }
        return new DoublePoint(-x / 10, -y / 10);
    }

    public DoublePoint avoidance(List<DFlocker> b, NContinuous2D<DFlocker> flockers) {
        if (b == null || b.size() == 0) return new DoublePoint(0, 0);
        double x = 0;
        double y = 0;

        int i = 0;
        int count = 0;

        for (i = 0; i < b.size(); i++) {
            DFlocker other = (DFlocker)(b.get(i));
            if (other != this ) {
                double dx = flockers.tdx(loc.c[0], other.loc.c[0]);
                double dy = flockers.tdy(loc.c[1], other.loc.c[1]);
                double lensquared = dx * dx + dy * dy;
                count++;
                x += dx / (lensquared * lensquared + 1);
                y += dy / (lensquared * lensquared + 1);
            }
        }
        if (count > 0) { x /= count; y /= count; }
        return new DoublePoint(400 * x, 400 * y);
    }

    public DoublePoint randomness(MersenneTwisterFast r) {
        double x = r.nextDouble() * 2 - 1.0;
        double y = r.nextDouble() * 2 - 1.0;
        double l = Math.sqrt(x * x + y * y);
        return new DoublePoint(0.05 * x / l, 0.05 * y / l);
    }

    public void step(SimState state) {
        final DFlockers flock = (DFlockers)state;
        DoublePoint oldloc = loc;
        loc = (DoublePoint) flock.flockers.getLocation(this);
        if (loc == null) {
            System.out.printf("pid %d oldx %g oldy %g", flock.partition.pid, oldloc.c[0], oldloc.c[1]);
            Thread.dumpStack();
            System.exit(-1);
        }

        if (dead) return;
        
        List<DFlocker> b = flock.flockers.getNeighborsWithin(this, flock.neighborhood);
        
        DoublePoint avoid = avoidance(b, flock.flockers);
        DoublePoint cohe = cohesion(b, flock.flockers);
        DoublePoint rand = randomness(flock.random);
        DoublePoint cons = consistency(b, flock.flockers);
        DoublePoint mome = momentum();

        double dx = flock.cohesion * cohe.c[0] + flock.avoidance * avoid.c[0] + flock.consistency * cons.c[0] + flock.randomness * rand.c[0] + flock.momentum * mome.c[0];
        double dy = flock.cohesion * cohe.c[1] + flock.avoidance * avoid.c[1] + flock.consistency * cons.c[1] + flock.randomness * rand.c[1] + flock.momentum * mome.c[1];

        // renormalize to the given step size
        double dis = Math.sqrt(dx * dx + dy * dy);
        if (dis > 0) {
            dx = dx / dis * flock.jump;
            dy = dy / dis * flock.jump;
        }

        lastd = new DoublePoint(dx, dy);
        loc = new DoublePoint(flock.flockers.stx(loc.c[0] + dx), flock.flockers.sty(loc.c[1] + dy));
        
        try {
            int dst = flock.partition.toPartitionId(new double[] {loc.c[0], loc.c[1]});
            if (dst != flock.partition.getPid()) {
            	// Need to migrate to other partition, 
            	// remove from current partition 
                flock.flockers.removeObject(this);
                flock.queue.migrate(this, dst, loc);           
            } else {
            	// Set to new location in current partition
                flock.flockers.setLocation(this, loc);
                flock.schedule.scheduleOnce(this, 1);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(-1);
        }
    }
}
