package sim.physics2D.physicalObject;

import java.awt.Graphics2D;
import sim.portrayal.*;
import sim.physics2D.shape.*;
import sim.physics2D.util.*;
import sim.physics2D.*;
import sim.util.Double2D;

/** PhysicalObject2D is an abstract class representing objects that can be
 * operated on by PhysicsEngine2D
 */
abstract public class PhysicalObject2D extends SimplePortrayal2D
    {
    // Data members common to all physical objects
    public int index;
    protected Shape shape;
    protected PhysicsState physicsState = PhysicsState.getInstance();
    protected double coefficientOfRestitution; // elasticity
        
    /** Returns the object's index, which uniquely identifies the object
     * and determines where its state variables are kept in the state
     * vectors and matrices.
     */
    public int getIndex()
        {
        return index;
        }
        
    public void setIndex(int index)
        {
        this.index = index;
        }
        
    public PhysicalObject2D()
        {
        physicsState.addBody(this);
        }
        
    /** Returns an object's associated shape
     */
    public Shape getShape()
        {
        return shape;
        }

    /** Returns an object's current position
     */
    public Double2D getPosition()
        {
        return physicsState.getPosition(index);
        }
        
    /** Returns an object's current orientation 
     */
    public Angle getOrientation()
        {
        return physicsState.getOrientation(index);
        }
        
    /** Represents the elasticity of an object
     * 1 is perfectly elastic and 0 is perfectly inelastic. 
     * Determines how much momentum is conserved when objects collide
     */
    public double getCoefficientOfRestitution()
        {
        return coefficientOfRestitution;
        }
        
    /** Represents the elasticity of an object
     * 1 is perfectly elastic and 0 is perfectly inelastic. 
     * Determines how much momentum is conserved when objects collide
     */
    public void setCoefficientOfRestitution(double coefficientOfRestitution)
        {
        this.coefficientOfRestitution = coefficientOfRestitution;
        }
        
    /** Display the object */
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        shape.draw(object, graphics, info);
        }
        
    /** Set the pose of the object */
    public void setPose(Double2D position, Angle orientation)
        {
        physicsState.setPosition(position, index);
        physicsState.setOrientation(orientation, index);
        }
        
    // Member functions that vary between mobile and stationary objects
    abstract public Double2D getVelocity();
        
    /** How fast the object is rotating in radians per second.
     * A positive angular velocity means the object is rotating
     * counter clockwise
     */
    abstract public double getAngularVelocity();
        
    /** Provides a default implementation for the function used by the collision 
     * detection engine to notify an object when it has collided with another object. 
     */
    public int handleCollision(PhysicalObject2D other, Double2D colPoint)
        {
        return 1; //regular collision
        }
        
    /////////////////////////////////////////////////////
    // Abstract functions used by collision detection
    /////////////////////////////////////////////////////
    abstract public void resetLastPose();
    abstract public void updatePose(double percent);
    abstract public void restorePose();
    abstract public double getMassInverse();
    abstract public double getMassMomentOfInertiaInverse();
    }
