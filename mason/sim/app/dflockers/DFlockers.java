/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.*;

import mpi.*;
import java.nio.*;
import java.lang.Math;

public class DFlockers extends SimState {
    private static final long serialVersionUID = 1;

    public DContinuous2D flockers;
    public double width = 1000;
    public double height = 1000;
    public int numFlockers = 50000;
    public double cohesion = 1.0;
    public double avoidance = 1.0;
    public double randomness = 1.0;
    public double consistency = 1.0;
    public double momentum = 1.0;
    public double deadFlockerProbability = 0.1;
    public double neighborhood = 10;
    public double jump = 0.7;  // how far do we move in a timestep?

    DUniformPartition partition;

    /** Creates a Flockers simulation with the given random number seed. */
    public DFlockers(long seed) {
        super(seed);
    }

    public void start() {
        super.start();

        partition = new DUniformPartition(new int[] {(int)width, (int)height});
        flockers = new DContinuous2D(neighborhood / 1.5, width, height, neighborhood, partition, this.schedule);

        schedule.scheduleRepeating(Schedule.EPOCH, 0, new Synchronizer(), 1);

        for (int x = 0; x < numFlockers / partition.np; x++) {
            Double2D location = new Double2D(
                (random.nextDouble() + partition.coords[0]) * flockers.f.lsize[0],
                (random.nextDouble() + partition.coords[1]) * flockers.f.lsize[1]
            );
            DFlocker flocker = new DFlocker(location);

            if (random.nextBoolean(deadFlockerProbability))
                flocker.dead = true;

            flockers.setObjectLocation(flocker, location);
        }
    }

    public static void main(String[] args) throws MPIException {
        MPI.Init(args);
        doLoop(DFlockers.class, args);
        MPI.Finalize();
        System.exit(0);
    }

    private class Synchronizer implements Steppable {
        private static final long serialVersionUID = 1;

        public void step(SimState state) {
            try {
                flockers.sync();
                //String s = String.format("PID %d Steps %d Number of Agents %d\n", partition.pid, schedule.getSteps(), flockers.size() - flockers.ghosts.size());
                //System.out.print(s);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}
