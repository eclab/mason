/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.sweep;

public interface SimulationJobSupplier
    {
    // both of these methods should be threadsafe
    public SimulationJob getNextJob();      // returns null if all jobs are finished.
    public void jobResult(SimulationJob job, double[] result);
    }
