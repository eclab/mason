package sim.physics2D.physicalObject;

import sim.physics2D.util.*;
import sim.physics2D.shape.*;
import sim.util.Double2D;

/**
 * MobileObject2D represents a physical object that can move. 
 */
public abstract class MobileObject2D extends PhysicalObject2D
    {
    protected double coefficientOfFriction; 
    protected double coefficientOfStaticFriction;

    protected double mass = 0;
    protected double massMomentOfInertia = 0;
        
    private static final double zeroVelocity = 0.01;
    private double normalForce;
    private static final double gravity = .1;
        
    public MobileObject2D()
        {
        this.setPose(new Double2D(0,0), new Angle(0));
        this.setVelocity(new Double2D(0,0));
        this.setAngularVelocity(0);
        }
        
    /** Returns the object's mass
     */
    public double getMass()
        {
        return mass;
        }

    /** Sets an object's mass. The mass moment of inertia is calculated
     * by the object's associated shape
     */
    public void setMass(double mass)
        {       
        massMomentOfInertia = shape.getMassMomentOfInertia(mass);
                
        double massMomentOfInertiaInverse;
        if (massMomentOfInertia > 0)
            massMomentOfInertiaInverse = 1 / this.massMomentOfInertia;
        else
            massMomentOfInertiaInverse = 0;
                
        // Precompute inverses since we need them a lot in collision response
        double massInverse = 1 / mass;
        physicsState.setMassInverse(massInverse, massMomentOfInertiaInverse, index);
                
        this.mass = mass;
        normalForce = mass * gravity;
        }

    /** Apply a force to the MobileObject */
    public void addForce(Double2D force)
        {
        physicsState.addExternalForce(force, this.index);
        }
        
    /** Apply a torque to the MobileObject */
    public void addTorque(double torque)
        {
        physicsState.addExternalTorque(torque, this.index);
        }
        
    /** Positive value representing the coefficient of friction of the
     * object with the background surface. 0 is no friction
     */
    public double getCoefficientOfFriction()
        {
        return coefficientOfFriction;
        }
        
    /** Positive value representing the coefficient of friction of the
     * object with the background surface. 0 is no friction
     */
    public void setCoefficientOfFriction(double coefficientOfFriction)
        {
        this.coefficientOfFriction = coefficientOfFriction;
        }
        
    /** Positive value representing the coefficient of static friction of the
     * object with the background surface. 0 is no static friction
     */
    public double getCoefficientOfStaticFriction()
        {
        return coefficientOfStaticFriction;
        }

    /** Positive value representing the coefficient of static friction of the
     * object with the background surface. 0 is no static friction
     */
    public void setCoefficientOfStaticFriction(double coefficientOfStaticFriction)
        {
        this.coefficientOfStaticFriction = coefficientOfStaticFriction;
        }

    /** Set the shape of the object which determines how it is displayed, 
     * when it is colliding with another object, and how its mass moment of 
     * inertia is calculated
     */
    public void setShape(Shape shape, double mass)
        {
        this.shape = shape;
        this.shape.setIndex(this.index);
        this.shape.calcMaxDistances(true);
        setMass(mass);
        }
        
    /** Updates the pose to where the object would be in only a percentage
     * of a time step. Useful for searching for exact moment of collision.
     */
    public void updatePose(double percent)
        {
        this.setPose(this.getPosition().add(physicsState.getLastVelocity(this.index).multiply(percent)), this.getOrientation().add(physicsState.getLastAngularVelocity(this.index)));
        }
        
    /** Move the object back to its previous location */
    public void resetLastPose()
        {
        this.setPose(physicsState.getLastPosition(this.index), physicsState.getLastOrientation(this.index));
        }
        
    /** Restores an object to its current location */
    public void restorePose()
        {
        this.setPose(physicsState.getSavedPosition(this.index), physicsState.getSavedOrientation(this.index));
        }

    /** Returns the object's velocity */
    public Double2D getVelocity()
        {
        return physicsState.getVelocity(index);
        }
        
    /** Updates the object's velocity */
    public void setVelocity(Double2D velocity)
        {
        physicsState.setVelocity(velocity, index);
        }
        
    /** How fast the object is rotating in radians per second.
     * A positive angular velocity means the object is rotating
     * counter clockwise
     */
    public double getAngularVelocity()
        {
        return physicsState.getAngularVelocity(index);
        }
        
    /** How fast the object is rotating in radians per second.
     * A positive angular velocity means the object is rotating
     * counter clockwise
     */
    public void setAngularVelocity(double angularVelocity)
        {
        physicsState.setAngularVelocity(angularVelocity, index);
        }
        
    /** Returns a vector that represents a combination of 
     * all the forces applied to it
     */
    public Double2D getForceAccumulator()
        {
        return physicsState.getExternalForce(index);
        }
        
    /** Returns a number that represents a combination of 
     * all the torques applied to it
     */
    public double getTorqueAccumulator()
        {
        return physicsState.getExternalTorque(index);
        }

    /** 1 / mass. Used in collision response */
    public double getMassInverse()
        {
        return physicsState.getMassInverse(index);
        }
        
    /** 1 / massMomentOfInertia. Used in collision response */
    public double getMassMomentOfInertiaInverse()
        {
        return physicsState.getMassMomentOfInertiaInverse(index);
        }
        
    /** Calculates and adds the static and dynamic friction forces on the object
     * based on the coefficients of friction. 
     */
    public void addFrictionForce()
        {
        if (this.coefficientOfFriction > 0 || this.coefficientOfStaticFriction > 0)
            {
            Double2D velocity = this.getVelocity(); 
            double velLength = velocity.length();
            if (velLength < zeroVelocity)
                {
                // static friction
                Double2D externalForce = this.getForceAccumulator();
                if (normalForce * this.getCoefficientOfStaticFriction() > externalForce.length())
                    this.addForce(new Double2D(-externalForce.x, -externalForce.y));
                }
                        
            // add dynamic friction
            if (velLength > 0)
                {
                Double2D velRot = new Double2D(-velocity.x, -velocity.y);
                this.addForce(velRot.multiply(this.getCoefficientOfFriction() * normalForce));
                }
            }
        }
    }
