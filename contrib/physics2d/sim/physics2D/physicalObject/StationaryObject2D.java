package sim.physics2D.physicalObject;

import sim.physics2D.util.*;
import sim.physics2D.shape.*;
import sim.physics2D.*;
import sim.util.Double2D;

/**
 * StationaryObject2D represents a physical object that can't move. 
 * Stationary objects have infinite mass.
 */
public abstract class StationaryObject2D extends PhysicalObject2D
    {               
    private static PhysicsState physicsState = PhysicsState.getInstance();
    private Double2D velocity = new Double2D(0, 0);
        
    public StationaryObject2D()
        {
        physicsState.setMassInverse(0, 0, index);
        }

    /** Set the shape of the object which determines how it is displayed 
     * and when it is colliding with another object
     */
    public void setShape(Shape shape)
        {
        this.shape = shape;
        this.shape.setIndex(this.index);
        this.shape.calcMaxDistances(false);
        }
        
    /** Returns the velocity of the object. Since this is a stationary
     * object, it is always 0.
     */
    public Double2D getVelocity()
        {
        return velocity;
        }
        
    /** How fast the object is rotating in radians per second.
     * A positive angular velocity means the object is rotating
     * counter clockwise. Since this is a stationary object,
     * it is always 0
     */
    public double getAngularVelocity()
        {
        return 0;
        }
        
    /** 1 / mass. Used in collision response. Because a stationary
     * object's mass is infinite, this always returns 0 */
    public double getMassInverse()
        {
        // mass of a stationary object is infinite, so inverse is 0
        return 0;
        }
        
    /** 1 / mass moment of inertia. Used in collision response. Because a stationary
     * object's mass moment of inertia is infinite, this always returns 0 */
    public double getMassMomentOfInertiaInverse()
        {
        // mass moment of inertia of a stationary object is infinite, so inverse is 0
        return 0;
        }
        
    // Do nothing in these functions since the object doesn't move
    public void resetLastPose() { }
    public void updatePose(double percent) { }
    public void restorePose() { }
    }
