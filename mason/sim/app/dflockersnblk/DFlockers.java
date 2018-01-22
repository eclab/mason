/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockersnblk;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

import mpi.*;
import java.nio.*;
import java.lang.Math;

public class DFlockers extends SimState {
    private static final long serialVersionUID = 1;

    public DContinuous2DNBLK flockers;
    public double width = 1000;
    public double height = 1000;
    public int numFlockers = 1000;
    public static int pw = 1, ph = 1;
    public double cohesion = 1.0;
    public double avoidance = 1.0;
    public double randomness = 1.0;
    public double consistency = 1.0;
    public double momentum = 1.0;
    public double deadFlockerProbability = 0.1;
    public double neighborhood = 10;
    public double jump = 0.7;  // how far do we move in a timestep?

    public double getCohesion() { return cohesion; }
    public void setCohesion(double val) { if (val >= 0.0) cohesion = val; }
    public double getAvoidance() { return avoidance; }
    public void setAvoidance(double val) { if (val >= 0.0) avoidance = val; }
    public double getRandomness() { return randomness; }
    public void setRandomness(double val) { if (val >= 0.0) randomness = val; }
    public double getConsistency() { return consistency; }
    public void setConsistency(double val) { if (val >= 0.0) consistency = val; }
    public double getMomentum() { return momentum; }
    public void setMomentum(double val) { if (val >= 0.0) momentum = val; }
    public int getNumFlockers() { return numFlockers; }
    public void setNumFlockers(int val) { if (val >= 1) numFlockers = val; }
    public double getWidth() { return width; }
    public void setWidth(double val) { if (val > 0) width = val; }
    public double getHeight() { return height; }
    public void setHeight(double val) { if (val > 0) height = val; }
    public double getNeighborhood() { return neighborhood; }
    public void setNeighborhood(double val) { if (val > 0) neighborhood = val; }
    public double getDeadFlockerProbability() { return deadFlockerProbability; }
    public void setDeadFlockerProbability(double val) { if (val >= 0.0 && val <= 1.0) deadFlockerProbability = val; }

    public Double2D[] getLocations() {
        if (flockers == null) return new Double2D[0];
        Bag b = flockers.getAllObjects();
        if (b == null) return new Double2D[0];
        Double2D[] locs = new Double2D[b.numObjs];
        for (int i = 0; i < b.numObjs; i++)
            locs[i] = flockers.getObjectLocation(b.objs[i]);
        return locs;
    }

    public Double2D[] getInvertedLocations() {
        if (flockers == null) return new Double2D[0];
        Bag b = flockers.getAllObjects();
        if (b == null) return new Double2D[0];
        Double2D[] locs = new Double2D[b.numObjs];
        for (int i = 0; i < b.numObjs; i++) {
            locs[i] = flockers.getObjectLocation(b.objs[i]);
            locs[i] = new Double2D(locs[i].y, locs[i].x);
        }
        return locs;
    }

    /** Creates a Flockers simulation with the given random number seed. */
    public DFlockers(long seed) {
        super(seed);
    }

    public void start() {
        super.start();

        // set up the flockers field.  It looks like a discretization
        // of about neighborhood / 1.5 is close to optimal for us.  Hmph,
        // that's 16 hash lookups! I would have guessed that
        // neighborhood * 2 (which is about 4 lookups on average)
        // would be optimal.  Go figure.
        flockers = new DContinuous2DNBLK(neighborhood / 1.5, width, height, pw, ph, neighborhood);

        // make a bunch of flockers and schedule 'em.  A few will be dead
        for (int x = 0; x < numFlockers / (pw * ph); x++) {
            Double2D location = new Double2D((random.nextDouble() + flockers.px) * flockers.cell_width, (random.nextDouble() + flockers.py) * flockers.cell_height);
            DFlocker flocker = new DFlocker(location);
            if (random.nextBoolean(deadFlockerProbability))
                flocker.dead = true;
            flockers.setObjectLocation(flocker, location);
            flocker.flockers = flockers;
            flocker.theFlock = this;
            schedule.scheduleOnce(flocker, 0);
        }

        schedule.scheduleRepeating(Schedule.EPOCH, 1, new Synchronizer(), 1);

        Receiver r = new Receiver(this);
        r.start();
    }

    public static void main(String[] args) throws MPIException {
        MPI.InitThread(args, MPI.THREAD_MULTIPLE);
        int np = MPI.COMM_WORLD.getSize();
        pw = (int)Math.sqrt((double)np);
        ph = pw;
        doLoop(DFlockers.class, args);
        MPI.Finalize();
        System.exit(0);
    }

    private class Receiver extends Thread {

        DFlockers theFlock;

        public Receiver(DFlockers theFlock) {
            this.theFlock = theFlock;
        }

        @Override
        public void run() {
            while (true) {
                DoubleBuffer data = MPI.newDoubleBuffer(3);
                try {
                    MPI.COMM_WORLD.recv(data, 3, MPI.DOUBLE, MPI.ANY_SOURCE, MPI.ANY_TAG);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                Double2D loc = new Double2D(data.get(0), data.get(1));
                if (!theFlock.flockers.isPrivate(loc)) {
                    System.out.printf("PID %d invalid incoming flocker (%g, %g) from %g should be %d\n", theFlock.flockers.pid, loc.x, loc.y, data.get(2), theFlock.flockers.toNeighborRank(loc));
                    System.out.printf("cw %g ch %g pw %d ph %d px %d py %d\n", theFlock.flockers.cell_width, theFlock.flockers.cell_height, theFlock.flockers.pw, theFlock.flockers.ph, theFlock.flockers.px, theFlock.flockers.py);
                }
                DFlocker f = new DFlocker(loc);
                f.flockers = theFlock.flockers;
                f.theFlock = theFlock;
                theFlock.flockers.setObjectLocation(f, loc);
                theFlock.schedule.scheduleOnce(f, 0);
            }
        }
    }

    private class Synchronizer implements Steppable {
        private static final long serialVersionUID = 1;

        public void step(SimState state) {
            flockers.sync();
            //long steps = schedule.getSteps();
            try {
                MPI.COMM_WORLD.barrier();
            } catch(Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}
