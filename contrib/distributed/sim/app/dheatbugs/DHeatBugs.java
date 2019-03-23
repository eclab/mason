/*
   Copyright 2006 by Sean Luke and George Mason University
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.dheatbugs;

import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;
import sim.field.*;

import mpi.*;
import java.nio.*;

import sim.util.Timing;
import sim.util.MPITest;

public class DHeatBugs extends DSimState {
    // Import HeatBugs fields
    private static final long serialVersionUID = 1;

    public double minIdealTemp = 17000;
    public double maxIdealTemp = 31000;
    public double minOutputHeat = 6000;
    public double maxOutputHeat = 10000;

    public double evaporationRate = 0.993;
    public double diffusionRate = 1.0;
    public static final double MAX_HEAT = 32000;
    public double randomMovementProbability = 0.1;

    public int gridHeight;
    public int gridWidth;
    public int bugCount, privBugCount; // the replacement for get/setBugCount ?

    /* missing:
     * HeatBug[] bugs
     */

    // Same getters and setters as HeatBugs
    public double getMinimumIdealTemperature() { return minIdealTemp; }
    public void setMinimumIdealTemperature( double temp ) { if ( temp <= maxIdealTemp ) minIdealTemp = temp; }
    public double getMaximumIdealTemperature() { return maxIdealTemp; }
    public void setMaximumIdealTemperature( double temp ) { if ( temp >= minIdealTemp ) maxIdealTemp = temp; }
    public double getMinimumOutputHeat() { return minOutputHeat; }
    public void setMinimumOutputHeat( double temp ) { if ( temp <= maxOutputHeat ) minOutputHeat = temp; }
    public double getMaximumOutputHeat() { return maxOutputHeat; }
    public void setMaximumOutputHeat( double temp ) { if ( temp >= minOutputHeat ) maxOutputHeat = temp; }
    public double getEvaporationConstant() { return evaporationRate; }
    public void setEvaporationConstant( double temp ) { if ( temp >= 0 && temp <= 1 ) evaporationRate = temp; }
    public Object domEvaporationConstant() { return new Interval(0.0, 1.0); }
    public double getDiffusionConstant() { return diffusionRate; }
    public void setDiffusionConstant( double temp ) { if ( temp >= 0 && temp <= 1 ) diffusionRate = temp; }
    public Object domDiffusionConstant() { return new Interval(0.0, 1.0); }


    // Missing getBugXPos, getBugYPos

    // public void setRandomMovementProbability( double t ) {
    //      if (t >= 0 && t <= 1) {
    //              randomMovementProbability = t;
    //              for ( int i = 0 ; i < bugCount ; i++ )
    //                      if (bugs[i] != null)
    //                              bugs[i].setRandomMovementProbability( randomMovementProbability );
    //      }
    // }
    public Object domRandomMovementProbability() { return new Interval(0.0, 1.0); }
    public double getRandomMovementProbability() { return randomMovementProbability; }


    public double getMaximumHeat() { return MAX_HEAT; }

    /* Missing
     * get/setGridHeight get/setGridWidth get/setBugCount
     */

    public NDoubleGrid2D valgrid; // Instead of DoubleGrid2D
    public NDoubleGrid2D valgrid2; // Instead of DoubleGrid2D
    public NObjectGrid2D bugs; // Instead op SparseGrid2D


    //public IntHyperRect myPart;
    //


    public DHeatBugs(long seed) {
        this(seed, 1000, 1000, 0, 5);
    }

    public DHeatBugs(long seed, int width, int height, int count, int aoi) {
        super(seed, width, height, aoi);
        gridWidth = width; gridHeight = height; bugCount = count;

        try {
            // DNonUniformPartition ps = DNonUniformPartition.getPartitionScheme(new int[] {width, height}, true, this.aoi);
            // assert ps.np == 4;
            // ps.insertPartition(new IntHyperRect(0, new IntPoint(0, 0), new IntPoint(100, 100)));
            // ps.insertPartition(new IntHyperRect(1, new IntPoint(0, 100), new IntPoint(100, 1000)));
            // ps.insertPartition(new IntHyperRect(2, new IntPoint(100, 0), new IntPoint(1000, 100)));
            // ps.insertPartition(new IntHyperRect(3, new IntPoint(100, 100), new IntPoint(1000, 1000)));
            // ps.commit();

            createGrids();
            privBugCount = bugCount / p.np; // np?

            //lb = new LoadBalancer(this.aoi, 100);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(-1);
        }

        //myPart = p.getPartition();
    }

    protected void createGrids()
    {
        valgrid = new NDoubleGrid2D(p, aoi, 0);
        valgrid2 = new NDoubleGrid2D(p, aoi, 0);
        bugs = new NObjectGrid2D<DHeatBug>(p, aoi, s -> new DHeatBug[s]);
    }

    public void start() {
        super.start();

        int[] size = p.getPartition().getSize();
        double rangeIdealTemp = maxIdealTemp - minIdealTemp;
        double rangeOutputHeat = maxOutputHeat - minOutputHeat;

        for (int x = 0; x < privBugCount; x++) { // The difference between this and bugCount?
            double idealTemp = random.nextDouble() * rangeIdealTemp + minIdealTemp;
            double heatOutput = random.nextDouble() * rangeOutputHeat + minOutputHeat;
            int px, py; // Why are we doing this? Relationship?
            do {
                px = random.nextInt(size[0]) + p.getPartition().ul().getArray()[0];
                py = random.nextInt(size[1]) + p.getPartition().ul().getArray()[1];
            } while (bugs.get(px, py) != null);
            DHeatBug b = new DHeatBug(idealTemp, heatOutput, randomMovementProbability, px, py);
            bugs.set(px, py, b);
            schedule.scheduleOnce(b, 1);
        }

        // Does this have to happen here? I guess.
        schedule.scheduleRepeating(Schedule.EPOCH, 2, new Diffuser(), 1);
        schedule.scheduleRepeating(Schedule.EPOCH, 3, new Synchronizer(), 1);
        schedule.scheduleRepeating(Schedule.EPOCH, 4, new Balancer(), 1);
        schedule.scheduleRepeating(Schedule.EPOCH, 5, new Inspector(), 10);
    }

    // Missing availableProcessors() ; Is this really needed? Maybe not.

    // Includes three private classes
    // Synchronizer, Balancer, Inspector
    // Doesn't have stop()

    private class Synchronizer implements Steppable {
        private static final long serialVersionUID = 1;

        public void step(SimState state) {
            DHeatBugs hb = (DHeatBugs)state;
            // Here we have completed all the computation work - stop the timer
            Timing.stop(Timing.LB_RUNTIME);

            Timing.start(Timing.MPI_SYNC_OVERHEAD);

            try {
                hb.valgrid.sync();
                hb.bugs.sync();
                hb.queue.sync();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }

            for (Object obj : hb.queue) {
                privBugCount++;
                DHeatBug b = (DHeatBug)obj;
                bugs.set(b.loc_x, b.loc_y, b);
                schedule.scheduleOnce(b, 1);
            }
            hb.queue.objects.clear();

            Timing.stop(Timing.MPI_SYNC_OVERHEAD);
        }
    }

    private class Balancer implements Steppable {
        public void step (final SimState state) {
            DHeatBugs hb = (DHeatBugs)state;
            try {
                DQuadTreePartition ps = (DQuadTreePartition)hb.p;
                Double runtime = Timing.get(Timing.LB_RUNTIME).getMovingAverage();
                Timing.start(Timing.LB_OVERHEAD);
                ps.balance(runtime, 0);
                Timing.stop(Timing.LB_OVERHEAD);

                //if (hb.lb.balance((int)hb.schedule.getSteps()) > 0) {
                //      myPart = p.getPartition();
                //      MPITest.execInOrder(x -> System.out.printf("[%d] Balanced at step %d new Partition %s\n", x, hb.schedule.getSteps(), p.getPartition()), 500);
                //}
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private class Inspector implements Steppable {
        public void step( final SimState state ) {
            DHeatBugs hb = (DHeatBugs)state;
            //String s = String.format("PID %d Step %d Agent Count %d\n", hb.partition.pid, hb.schedule.getSteps(), hb.queue.size());
            //state.logger.info(String.format("PID %d Step %d Agent Count %d\n", hb.p.pid, hb.schedule.getSteps(), hb.privBugCount));
            //if (DNonUniformPartition.getPartitionScheme().getPid() == 0) {
            ((DSimState)state).logger.info(String.format("[%d][%d] Step Runtime: %g \tSync Runtime: %g \t LB Overhead: %g\n",
                    hb.p.getPid(),
                    hb.schedule.getSteps(),
                    Timing.get(Timing.LB_RUNTIME).getMovingAverage(),
                    Timing.get(Timing.MPI_SYNC_OVERHEAD).getMovingAverage(),
                    Timing.get(Timing.LB_OVERHEAD).getMovingAverage()
                    ));
            //}
            // for (Steppable i : hb.queue) {
            //      DHeatBug a = (DHeatBug)i;
            //      s += a.toString() + "\n";
            // }
            //System.out.print(s);
        }
    }

    public static void main(String[] args) throws MPIException {
        doLoopMPI(DHeatBugs.class, args);
        System.exit(0);
    }
}
