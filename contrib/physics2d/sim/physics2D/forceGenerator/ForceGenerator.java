
package sim.physics2D.forceGenerator;

/** A ForceGenerator is anything that can apply a force to an object (i.e. a spring or
 * a robot controller). It is important that objects don't apply forces from within their
 * "step" functions because integrators may require forces to be added more than one time
 * per timestep (i.e. the runge-kutta integrator needs objects to calculate and apply forces 
 * 4 times per timestep).
 */
public interface ForceGenerator 
    {
    public abstract void addForce();
    }
