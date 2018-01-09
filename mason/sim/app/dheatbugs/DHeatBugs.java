/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dheatbugs;
import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;

import mpi.*;
import java.nio.*;

public class DHeatBugs extends SimState {
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
    public int bugCount;
    DHeatBug[] bugs;

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
    public double getRandomMovementProbability() { return randomMovementProbability; }

    public void setRandomMovementProbability( double t ) {
        if (t >= 0 && t <= 1) {
            randomMovementProbability = t;
            for ( int i = 0 ; i < bugCount ; i++ )
                if (bugs[i] != null)
                    bugs[i].setRandomMovementProbability( randomMovementProbability );
        }
    }
    public Object domRandomMovementProbability() { return new Interval(0.0, 1.0); }

    public double getMaximumHeat() { return MAX_HEAT; }

    // we presume that no one relies on these DURING a simulation
    public int getGridHeight() { return gridHeight; }
    public void setGridHeight(int val) { if (val > 0) gridHeight = val; }
    public int getGridWidth() { return gridWidth; }
    public void setGridWidth(int val) { if (val > 0) gridWidth = val; }
    public int getBugCount() { return bugCount; }
    public void setBugCount(int val) { if (val >= 0) bugCount = val; }

    public DDoubleGrid2D valgrid;
    public DDoubleGrid2D valgrid2;

    /** Creates a HeatBugs simulation with the given random number seed. */
    public DHeatBugs(long seed) {
        this(seed, 1000, 1000, 1000, 1);
    }

    public int p_w, p_h, px, py, procCount, aoi, pid, cell_width, cell_height;

    Comm schedComm, schedAtomicComm;
    IntBuffer migrate_counter_buf;
    DoubleBuffer migrated_agents_buf;
    public Win schedWin, schedAtomicWin;

    public DHeatBugs(long seed, int width, int height, int count, int aoi) {
        super(seed);

        gridWidth = width;
        gridHeight = height;
        bugCount = count;
        this.aoi = aoi;

        createGrids();

        p_w = valgrid.dims[0];
        p_h = valgrid.dims[1];
        procCount = p_w * p_h;
        cell_width = gridWidth / p_w;
        cell_height = gridHeight / p_h;
        try {
            pid = MPI.COMM_WORLD.getRank();
        } catch (MPIException e) {
            e.printStackTrace(System.out);
            assert false;
        }
        px = (int)(pid / p_h);
        py = pid % p_h;
    }

    protected void createGrids() {
        bugs = new DHeatBug[bugCount];
        valgrid = new DDoubleGrid2D(gridWidth, gridHeight, aoi, 0);
        valgrid2 = new DDoubleGrid2D(gridWidth, gridHeight, aoi, 0);
    }

    /** Resets and starts a simulation */
    public void start() {
        super.start();  // clear out the schedule

        // make new grids
        //createGrids();

        migrate_counter_buf = MPI.newIntBuffer(1);
        migrated_agents_buf = MPI.newDoubleBuffer(bugCount * 5);

        try {
            schedAtomicWin = new Win(migrate_counter_buf, 1, 1, new Info(), MPI.COMM_WORLD);
            schedWin = new Win(migrated_agents_buf, bugCount * 5, 1, new Info(), MPI.COMM_WORLD);
        } catch (MPIException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        schedule.scheduleRepeating(Schedule.EPOCH, 0, new Synchronizer(), 1);

        for (int x = 0; x < bugCount / procCount; x++) {
            bugs[x] = new DHeatBug(random.nextDouble() * (maxIdealTemp - minIdealTemp) + minIdealTemp,
                                  random.nextDouble() * (maxOutputHeat - minOutputHeat) + minOutputHeat,
                                  randomMovementProbability,
                                  random.nextInt(cell_width) + cell_width * px,
                                  random.nextInt(cell_height) + cell_height * py
                                 );
            schedule.scheduleOnce(bugs[x], 1);
        }

        schedule.scheduleRepeating(Schedule.EPOCH, 2, new Diffuser(), 1);
        schedule.scheduleRepeating(Schedule.EPOCH, 3, new Synchronizer(), 1);
        schedule.scheduleRepeating(Schedule.EPOCH, 4, new Migrator(), 1);
    }

    public void stop() {
        return;
    }

    public static void main(String[] args) throws MPIException {
        MPI.Init(args);
        doLoop(DHeatBugs.class, args);
        MPI.Finalize();
        System.exit(0);
    }

    private class Synchronizer implements Steppable {
        private static final long serialVersionUID = 1;

        public void step(SimState state) {
            DHeatBugs hb = (DHeatBugs)state;

            try {
                hb.valgrid.sync();
                //hb.valgrid2.sync();

                //hb.schedAtomicWin.fence(0);
                hb.schedWin.fence(0);
            } catch (MPIException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private class Migrator implements Steppable {
        private static final long serialVersionUID = 1;

        public void step(SimState state) {
            DHeatBugs hb = (DHeatBugs)state;

            int acount = hb.migrate_counter_buf.get(0);
            //System.out.printf("PID %d Incoming agents total %d\n", pid, acount);

            // Handle migrated agents;
            for (int i = 0; i < acount; i += 5) {
                DHeatBug bug = new DHeatBug(
                    hb.migrated_agents_buf.get(i),
                    hb.migrated_agents_buf.get(i + 1),
                    hb.migrated_agents_buf.get(i + 2),
                    (int)hb.migrated_agents_buf.get(i + 3),
                    (int)hb.migrated_agents_buf.get(i + 4)
                );
                hb.schedule.scheduleOnce(bug);
                //System.out.printf("PID %d Incoming agents (%d, %d)\n", pid, bug.loc_x, bug.loc_y);
            }

            hb.migrate_counter_buf.put(0, 0);
        }
    }
}