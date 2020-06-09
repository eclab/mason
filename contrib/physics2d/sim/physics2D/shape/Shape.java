package sim.physics2D.shape;

import sim.portrayal.*;
import java.awt.*;
import sim.physics2D.*;
import sim.physics2D.util.*;
import sim.util.Double2D;

/** Each physical object has an associated shape. The type of shape associated
 * with the object determines how it is displayed, when it is colliding with another
 * object, and how its mass moment of inertia is calculated.
 * 
 * Shape is an abstract class representing any shape that can be associated with a 
 * physical object
 */
public abstract class Shape
    {
    private PhysicsState physicsState = PhysicsState.getInstance();
        
    protected Paint paint;
    protected boolean stationary;
    protected int index;
        
    public Shape(boolean stationary) 
        { 
        this.stationary = stationary; 
        }
        
    public Shape()
        {
        this.stationary = false;
        }
        
    public abstract void draw(Object object, Graphics2D graphics, DrawInfo2D info);
        
    protected Double2D getPosition()
        {
        return physicsState.getPosition(index);
        }
        
    /** Tells the shape the index of its associated physical object. Used by shapes
     * to get the object's pose from the state vector.
     */
    public void setIndex(int index)
        {
        this.index = index;
        }
        
    public void setPaint(Paint paint)
        {
        this.paint = paint;
        }
        
    public Paint getPaint()
        {
        return paint;
        }
        
    protected Angle getOrientation()
        {
        return physicsState.getOrientation(index);
        }
        
    /** Return the mass moment of inertia of this shape */
    public abstract double getMassMomentOfInertia(double mass);
        
    /////////////////////////////////////////////////////////////////
    // These functions are used by the broad phase Collision detection 
    // logic
    /////////////////////////////////////////////////////////////////
        
    /** Calculate the max distance a point can be from the center of the object.
        For polygons, this can be different if the object is moving (rotating).
        For circles, this is alway the same. */
    public abstract void calcMaxDistances(boolean mobile);
    public abstract double getMaxXDistanceFromCenter();
    public abstract double getMaxYDistanceFromCenter();
        
    }
