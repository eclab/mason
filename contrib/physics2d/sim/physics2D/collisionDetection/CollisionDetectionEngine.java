
package sim.physics2D.collisionDetection;

import sim.physics2D.physicalObject.PhysicalObject2D;
import sim.util.Bag;

/** The CollisionDetectionEngine coordinates and abstracts the collision 
 * detection logic.
 */
public class CollisionDetectionEngine 
    {
    private BroadPhaseCollision2D objBPCollision; 
    private Collision2D objCollision;
        
    public CollisionDetectionEngine()
        {
        objCollision = new Collision2D();
        objBPCollision = new BroadPhaseCollision2D();
        }
        
    /** Returns a list of the pairs of objects currently colliding.
     */
    public Bag getCollisions()
        {
        objBPCollision.testCollisions();
        return objCollision.testCollisions(objBPCollision.getActiveList());
        }
        
    /** Registers an object with the collision detection engine.
     */
    public void register(PhysicalObject2D objCol)
        {
        objBPCollision.register(objCol);
        }
    }
