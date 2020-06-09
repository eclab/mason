package sim.physics2D.integrator;

/** Represents the interface for ordinary differential equation solvers. These
 * solvers take the current state of the system (position, velocity, and external forces) 
 * and solve for the next state. 
 */
public interface ODESolver 
    {
    abstract public void solve(double stepSize);
    }
