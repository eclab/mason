package sim.util.sweep;

public interface SimulationJobSupplier
    {
    // both of these methods should be threadsafe
    public SimulationJob getNextJob();      // returns null if all jobs are finished.
    public void jobResult(SimulationJob job, double[] result);
    }
