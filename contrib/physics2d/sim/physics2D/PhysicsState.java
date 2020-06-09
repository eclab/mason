package sim.physics2D;

import sim.util.Bag;
import sim.physics2D.util.*;
import java.util.*;
import sim.physics2D.physicalObject.*;
import sim.util.Double2D;

/** PhysicsState holds the state of the physical objects in the system. The state
 * consists of the state vector which holds the positions and velocities for all objects,
 * the external forces vector which holds the force and torque accumulators for all objects,
 * and the mass inverse matrix which holds the mass and mass moment of intertia inverses for
 * all objects.  
 * 
 * Each of these variables is stored in blocks of 3 variables - x, y, and orientation.
 * An object's position, external force, and mass inverse block starts at its index times 3.
 * An object's velocity block starts at the number of objects in the system plus its
 * index times 3.
 * 
 * PhysicsState implements the "singleton" pattern so that any object
 * can get a reference to the current PhysicsState object. 
 */
public class PhysicsState 
    {
    static private PhysicsState instance = null;
    private Hashtable mapping;
    public Bag physObjs;
                
    private sim.util.matrix.DiagonalMatrix massInverseMatrix;
    private sim.util.matrix.Vector externalForcesVector;
        
    private sim.util.matrix.Vector stateVector;
    private sim.util.matrix.Vector lastStateVector;
    private sim.util.matrix.Vector savedStateVector;
    private sim.util.matrix.Vector tmpStateVector;
        
    //public LCP lcp;
        
    private PhysicsState()
        {
        physObjs = new Bag();   
        mapping = new Hashtable();
        //lcp = new LCP();
        }
        
    /** Returns the current PhysicsState instance 
     */
    static public PhysicsState getInstance()
        {
        if (instance == null)
            instance = new PhysicsState();
        return instance;
        }
        
    /** Resets the PhysicsState
     */
    public static PhysicsState reset()
        {
        instance = new PhysicsState();  
        return instance;
        }
        
    /** Returns the state vector that contains the positions and velocities
     * of all objects in the system 
     */
    public sim.util.matrix.Vector getStateVector()
        {
        return stateVector;
        }
        
    /** Updates the state vector
     */
    public void setStateVector(sim.util.matrix.Vector stateVector)
        {
        this.stateVector = stateVector;
        }
        
    /** Returns a copy of the state vector
     */
    public sim.util.matrix.Vector getStateVectorCopy()
        {
        return stateVector.copyInto(tmpStateVector);
        }
        
    /** Copies the "current state" vector into the "last state" vector. This
     * is run at the end of each timestep after all state updates are made.
     */
    public void saveLastState()
        {
        lastStateVector = stateVector.copyInto(lastStateVector);
        }
        
    /** Sets the state of the objects to what they were at the end of the previous
     * timestep. 
     */
    public void revertPosition()
        {
        lastStateVector.copyInto(stateVector);
        }
        
    /** Copies the "current state" vector into the "saved state" vector. 
     * This is used for collision detection so a penetrating pair of objects can 
     * be moved back in time over the last timestep to search for their exact 
     * collision point and then restored once the collision is found. 
     */
    public void backupCurrentPosition()
        {
        stateVector.copyInto(savedStateVector);
        }
        
    /** Restores the state of the object to the last time "backupCurrentPosition" 
     * was run. 
     */
    public void restore()
        {
        savedStateVector.copyInto(stateVector);
        }
        
    /** Updates an object's position variables in the state vector
     */
    public void setPosition(Double2D position, int index)
        {
        int posIndex = index * 3;
        stateVector.vals[posIndex] = position.x;
        stateVector.vals[posIndex + 1] = position.y;
        }
        
    /** Returns an object's position 
     */
    public Double2D getPosition(int index)
        {
        int posIndex = index * 3;
        return new Double2D(stateVector.vals[posIndex], stateVector.vals[posIndex + 1]);
        }
        
    /** Returns an object's last position 1 timestep ago 
     */
    public Double2D getLastPosition(int index)
        {
        int posIndex = index * 3;
        return new Double2D(lastStateVector.vals[posIndex], lastStateVector.vals[posIndex + 1]);
        }
        
    /** Returns an object's backed up position. 
     */
    public Double2D getSavedPosition(int index)
        {
        int posIndex = index * 3;
        return new Double2D(savedStateVector.vals[posIndex], savedStateVector.vals[posIndex + 1]);
        }
        
    /** Updates an object's orientation variable in the state vector
     */
    public void setOrientation(Angle orientation, int index)
        {
        stateVector.vals[index * 3 + 2] = orientation.radians;
        }
        
    /** Returns an object's orientation 
     */
    public Angle getOrientation(int index)
        {
        return new Angle(stateVector.vals[index * 3 + 2]);
        }
        
    /** Returns an object's orientation 1 timestep ago 
     */
    public Angle getLastOrientation(int index)
        {
        return new Angle(lastStateVector.vals[index * 3 + 2]);
        }
        
    /** Returns an object's backed up orientation 
     */
    public Angle getSavedOrientation(int index)
        {
        return new Angle(savedStateVector.vals[index * 3 + 2]);
        }
        
    /** Updates an object's linear velocity variables in the state vector 
     */
    public void setVelocity(Double2D velocity, int index)
        {
        int velIndex = (physObjs.numObjs + index) * 3;
        stateVector.vals[velIndex] = velocity.x;
        stateVector.vals[velIndex + 1] = velocity.y;
        }
        
    /** Returns an object's linear velocity 
     */
    public Double2D getVelocity(int index)
        {
        int velIndex = (physObjs.numObjs + index) * 3;
        return new Double2D(stateVector.vals[velIndex], stateVector.vals[velIndex + 1]);
        }
        
    /** Returns an object's linear velocity one timestep ago 
     */
    public Double2D getLastVelocity(int index)
        {
        int velIndex = (physObjs.numObjs + index) * 3;
        return new Double2D(lastStateVector.vals[velIndex], lastStateVector.vals[velIndex + 1]);
        }
        
    /** Returns an object's backed up linear velocity 
     */
    public Double2D getSavedVelocity(int index)
        {
        int velIndex = (physObjs.numObjs + index) * 3;
        return new Double2D(savedStateVector.vals[velIndex], savedStateVector.vals[velIndex + 1]);
        }
        
    /** Updates an object's angular velocity variable in the state vector
     */
    public void setAngularVelocity(double angularVelocity, int index)
        {
        int velIndex = (physObjs.numObjs + index) * 3 + 2;
        stateVector.vals[velIndex] = angularVelocity;
        }
        
    /** Returns an object's angular velocity 
     */
    public double getAngularVelocity(int index)
        {
        int velIndex = (physObjs.numObjs + index) * 3 + 2;
        return stateVector.vals[velIndex];
        }
        
    /** Returns an object's angular velocity 1 timestep ago 
     */
    public double getLastAngularVelocity(int index)
        {
        int velIndex = (physObjs.numObjs + index) * 3 + 2;
        return lastStateVector.vals[velIndex];
        }
        
    /** Returns an object's backed up angular velocity 
     */
    public double getSavedAngularVelocity(int index)
        {
        int velIndex = (physObjs.numObjs + index) * 3 + 2;
        return savedStateVector.vals[velIndex];
        }
        
    /** Returns the external forces vector that holds the force and torque
     * accumluators for every object in the system 
     */
    public sim.util.matrix.Vector getExternalForcesVector()
        {
        return externalForcesVector;
        }
        
    /** Adds a force to an object's force accumulator in the external forces
     * vector
     */
    public void addExternalForce(Double2D force, int index)
        {
        Double2D existingForce = getExternalForce(index);
        Double2D newForce = existingForce.add(force);
        
        int forceIndex = index * 3;
        externalForcesVector.vals[forceIndex] = newForce.x;
        externalForcesVector.vals[forceIndex + 1] = newForce.y;
        }
        
    /** Returns an object's force accumulator
     */
    public Double2D getExternalForce(int index)
        {
        int forceIndex = index * 3;
        return new Double2D(externalForcesVector.vals[forceIndex], externalForcesVector.vals[forceIndex + 1]);
        }
        
    /** Adds a torque to an object's torque accumulator in the external forces vector
     */
    public void addExternalTorque(double torque, int index)
        {
        int forceIndex = index * 3;
        externalForcesVector.vals[forceIndex + 2] = torque + getExternalTorque(index);
        }
        
    /** Returns an object's torque accumulator 
     */
    public double getExternalTorque(int index)
        {
        int forceIndex = index * 3;
        return externalForcesVector.vals[forceIndex + 2];
        }
        
    /** Clears all forces and torques 
     */
    public void clearAllForces()
        {
        externalForcesVector.clear();
        }
        
    /** Returns the diagonal mass inverse matrix that contains the
     * mass and mass moment of inertia inverses for all objects in the 
     * system
     */
    public sim.util.matrix.DiagonalMatrix getMassInverseMatrix()
        {
        return massInverseMatrix;
        }
        
    /** Updates an object's mass inverse variables in the 
     * mass inverse matrix 
     */
    public void setMassInverse(double massInverse, double massMomentOfInertiaInverse, int index)
        {
        int massIndex = index * 3;
        massInverseMatrix.vals[massIndex] = massInverse;
        massInverseMatrix.vals[massIndex + 1] = massInverse;
        massInverseMatrix.vals[massIndex + 2] = massMomentOfInertiaInverse;
        }
        
    /** Returns an object's mass inverse 
     */
    public double getMassInverse(int index)
        {
        int massIndex = index * 3;
        return massInverseMatrix.vals[massIndex];
        }
        
    /** Returns an object's mass moment inertia inverse 
     */
    public double getMassMomentOfInertiaInverse(int index)
        {
        int massIndex = index * 3 + 2;
        return massInverseMatrix.vals[massIndex];
        }
        
    /** Returns the number of physical objects in the system 
     */
    public int numObjs()
        {
        return physObjs.numObjs;
        }
        
    /** Adds a physical object to the system, expanding all state matrices and
     * vectors to accomodate it.
     */ 
    public void addBody(PhysicalObject2D mobj)
        {
        mobj.index = physObjs.numObjs;
        physObjs.add(mobj);
                
        int threeNum = 3 * (mobj.index + 1);
        sim.util.matrix.DiagonalMatrix newMassInverseMatrix = new sim.util.matrix.DiagonalMatrix(threeNum);
                
        sim.util.matrix.Vector newStateVector = new sim.util.matrix.Vector(threeNum * 2);
        sim.util.matrix.Vector newExternalForcesVector = new sim.util.matrix.Vector(threeNum);
                
        if (mobj.index > 0)
            {
            for (int i = 0; i < massInverseMatrix.m; i++)
                {
                newStateVector.vals[i] = stateVector.vals[i];
                newStateVector.vals[i+threeNum] = stateVector.vals[i+threeNum-3];
                newExternalForcesVector.vals[i] = externalForcesVector.vals[i];
                newMassInverseMatrix.vals[i] = massInverseMatrix.vals[i];       
                }       
            }
        massInverseMatrix = newMassInverseMatrix;               
        stateVector = newStateVector;
        externalForcesVector = newExternalForcesVector;
                
        lastStateVector = stateVector.copy();
        savedStateVector = stateVector.copy();
        tmpStateVector = stateVector.copy();
        }
    }
