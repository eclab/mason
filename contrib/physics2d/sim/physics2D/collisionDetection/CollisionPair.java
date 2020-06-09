
package sim.physics2D.collisionDetection;

import sim.physics2D.physicalObject.PhysicalObject2D;
//import sim.physics2D.util.Double2D;
import sim.physics2D.util.PhysicalObjectPair;
import sim.util.Double2D;

/** CollisionPair represents a pair of objects that are currently colliding
 */
public class CollisionPair extends PhysicalObjectPair
    {
    public CollisionPair(PhysicalObject2D c1, PhysicalObject2D c2)
        {
        super(c1, c2);
        }
        
    // Used in Narrow phase for closest Feature Tracking
    Integer closestFeature1 = null;
    Integer closestFeature2 = null;
        
    Double2D colPoint1 = null;
    Double2D colPoint2 = null;
    Double2D normal = null;
        
    double relVel;
        
    boolean stickyCol = false;
        
    // If this is set to true, the narrow phase processor will
    // never schedule the pair to collide. This is used when 
    // objects are colliding and it can't be determined when
    // they collided (e.g. they started on top of each other)
    boolean noCollision = false;
        
    /** Returns a vector pointing from the center of object 1 to
     * the collision point
     */
    public Double2D getColPoint1()
        {
        return colPoint1;
        }
        
    /** Returns a vector pointing from the center of object 2 to
     * the collision point
     */
    public Double2D getColPoint2()
        {
        return colPoint2;
        }
        
    /** Returns the collision normal. The collision normal points out
     * from object 2.
     */
    public Double2D getNormal()
        {
        return normal;
        }
        
    /** Returns the relative velocity of the objects along the collision normal
     */
    public double getRelativeVelocity()
        {
        return relVel;
        }
        
    /** Indicates that this collision should be perfectly inelastic (so the objects
     * lose all energy along the collision normal).
     */
    public void setSticky()
        {
        stickyCol = true;
        }
        
    /** Returns a value indicated whether this is a sticky collision or not.
     */
    public boolean getSticky()
        {
        return stickyCol;
        }
        
    public void clear()
        {
        this.closestFeature1 = null;
        this.closestFeature2 = null;
        this.colPoint1 = null;
        this.colPoint2 = null;
        this.relVel = 0;
        }
    }
