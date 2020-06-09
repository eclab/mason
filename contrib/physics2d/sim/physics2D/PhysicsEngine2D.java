package sim.physics2D;

import sim.engine.SimState;
import sim.engine.Steppable;

import sim.physics2D.forceGenerator.*;
import sim.physics2D.integrator.*;
import sim.physics2D.constraint.*;
import sim.physics2D.collisionDetection.*;
import sim.physics2D.physicalObject.*;

import sim.util.Bag;

/** PhysicsEngine2D coordinates all the activities of the physics engine.
 */
public class PhysicsEngine2D implements Steppable
    {
    private final static double ZERO_VELOCITY = 0.000001;
    private final static double STICKY_THRESHOLD = 0.02;
        
    private ODESolver objODE;
    private CollisionDetectionEngine objCDE;
    private PhysicsState physicsState;
    private ConstraintEngine objCE;
    private ForceEngine objFE;
                
    public PhysicsEngine2D()
        {
        physicsState = PhysicsState.reset();
        objCE = ConstraintEngine.reset();
        objFE = ForceEngine.reset();
                
        objCDE = new CollisionDetectionEngine();
        objODE = new ODERungeKuttaSolver();
        }
        
    /** Replace the default runge-kutta ODE integrator with a new one.
     */
    public void setODESolver(ODESolver solver)
        {
        this.objODE = solver;
        }
        
    public void step(SimState state)
        {
        physicsState.backupCurrentPosition();
                
        // Handle collisions
        Bag contactList = objCDE.getCollisions();
        Bag collidingList = new Bag();
                
        // Notify each object involved in collisions of the collision
        for (int i = 0; i < contactList.size(); i++)
            {
            CollisionPair pair = (CollisionPair)contactList.objs[i];
                        
            int colType1 = pair.c1.handleCollision(pair.c2, pair.getColPoint1());
            int colType2 = pair.c2.handleCollision(pair.c1, pair.getColPoint2());
                        
            if (colType1 != 0 && colType2 != 0)
                {
                if (colType1 == 2 || colType2 == 2)
                    pair.setSticky();       
                collidingList.add(pair);
                }
            }
        objCE.addCollisionResponses(collidingList);
                
        // Handle resting contacts and other forces/constraints
        physicsState.saveLastState(); 
        objODE.solve(1);
        }
                
    /** Registers a physical object, force generator, or constraint
     * with the physics engine.
     */
    public void register(Object obj)
        {
        if (obj instanceof PhysicalObject2D)
            objCDE.register((PhysicalObject2D)obj);

        if (obj instanceof MobileObject2D)
            objFE.registerMobileObject((MobileObject2D)obj);
                
        if (obj instanceof ForceGenerator)
            objFE.registerForceGenerator((ForceGenerator)obj);
                
        if (obj instanceof ForceConstraint)
            objCE.registerForceConstraint((ForceConstraint)obj);
                
        if (obj instanceof ImpulseConstraint)
            objCE.registerImpulseConstraint((ImpulseConstraint)obj);
        }

    /** Turns off collision response for a pair of objects 
     */
    public void setNoCollisions(PhysicalObject2D c1, PhysicalObject2D c2)
        {
        objCE.setNoCollisions(c1, c2);
        }
        
    /** Removes a constraint
     */
    public void unRegister(Object obj)
        {
        if (obj instanceof ForceConstraint)
            objCE.unRegisterForceConstraint((ForceConstraint)obj);
                
        if (obj instanceof ImpulseConstraint)
            objCE.unRegisterImpulseConstraint((ImpulseConstraint)obj);
        }
    }
