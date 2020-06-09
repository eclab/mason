package sim.physics2D.forceGenerator;

import sim.physics2D.physicalObject.MobileObject2D;
import sim.physics2D.PhysicsState;
//import sim.physics2D.util.Double2D;
import sim.util.Bag;
import sim.util.Double2D;

/** The ForceEngine manages the application of forces and torques to objects. 
 */
public class ForceEngine 
    {
    private Bag forceGenerators;
        
    // NOTE - Resting contact is currently commented out. Hopefully it will
    // be implemented soon...
    // resting contact works like this:
    // Determine a small relative velocity where objects are considered 
    // to have 0 relative velocity (STICKY_THRESHOLD). Since resting contact
    // only works to cancel forces (it assumes 0 relative velocity), we need
    // to get rid of the small relative velocity. Do this by setting the 
    // coefficient of restitution to 0 (collision.setSticky()). 
    // After the collision, calculate the relative velocity. If the collision
    // was sticky (either because we set it or because one of the objects has
    // a zero coefficient of restitution), the new relative velocity should be
    // very very small (i.e. 1E-32). See if the new relvel is within (in either
    // direction) of ZERO_VELOCITY from 0. If so, add this to the resting contact
    // list.
        
    private PhysicsState physicsState = null;
    private Double2D forceFields;
    private Bag mobileObjs = new Bag();
        
    private static ForceEngine instance = null;
        
    public static ForceEngine getInstance()
        {
        if (instance == null)
            instance = new ForceEngine();
        return instance;
        }
        
    public static ForceEngine reset()
        {
        instance = new ForceEngine();
        return instance;
        }
        
    private ForceEngine()
        {
        forceFields = new Double2D(0,0);
        physicsState = PhysicsState.getInstance();
        forceGenerators = new Bag();
        }
        
    /** Registers a force generator with the force engine.
     */
    public void registerForceGenerator(ForceGenerator forceGenerator)
        {
        this.forceGenerators.add(forceGenerator);
        }
        
    /** Registers a mobile object with the force generator. All mobile objects
     * to whom force fields or friction should be applied need to be registered 
     * with the force engine.
     */
    public void registerMobileObject(MobileObject2D objMO)
        {
        mobileObjs.add(objMO);
        }       
        
    /** Adds a force that is applied to every object at 
     * every timestep. An example of a force field is gravity.
     */
    public void addForceField(Double2D forceField)
        {
        forceFields = forceFields.add(forceField);
        }

    /** Clear all force fields */
    public void clearForceField()
        {
        forceFields = new Double2D(0,0);
        }

    private void addForceField(MobileObject2D objMO)
        {
        objMO.addForce(forceFields);
        }
        
    /** Causes all force generators that are registered with the 
     * dynamics engine to add their forces to the appropriate objects
     */
    public void addForces()
        {
        physicsState.clearAllForces();
        Bag forceGenerators = this.forceGenerators;
        for (int i = 0; i < forceGenerators.numObjs; i++)
            {
            ForceGenerator fg = (ForceGenerator)forceGenerators.objs[i];
            fg.addForce();
            }
                
        Bag mobileObjs = this.mobileObjs;
        for (int i = 0; i < mobileObjs.numObjs; i++)
            {
            MobileObject2D mobj = (MobileObject2D)mobileObjs.objs[i]; 
            addForceField(mobj);
            mobj.addFrictionForce();
            }
                
        /*
        // Add resting contact forces
        LCP lcp = physicsState.lcp;
        if (lcp.contacts.numObjs > 0)
        {
        System.out.println("Computing Resting Contact Forces!!! ALMOST DONE!");
        lcp.addPins();
        Bag contactPairs = new Bag();
        //System.out.println("HERE: " + lcp.contacts.numObjs);
        for (int i = 0; i < lcp.contacts.numObjs; i++)
        {
        LCP.Contact pair = (LCP.Contact)lcp.contacts.objs[i];
        // make sure they haven't started separating
                                
        double relVelNormal = pair.relVel.dotProduct(pair.normal); 
        if (relVelNormal > ZERO_VELOCITY)
        lcp.contacts.remove(i);
        else
        contactPairs.add(pair);
        }
        if (lcp.contacts.numObjs > 0)
        lcp.computeForces();
                        
        for (int i = 0; i < contactPairs.numObjs; i++)
        {
        LCP.Contact pair = (LCP.Contact)contactPairs.objs[i];
        Double2D contactForce = pair.normal.scalarMult(lcp.f.get(i, 0));
                                
        // add in a small force to get rid of any relative velocity and to prevent interpenetration
        double dist = pair.ra.add(pair.A.getPosition()).subtract(pair.rb.add(pair.B.getPosition())).dotProduct(pair.normal);
        contactForce = contactForce.add(pair.normal.scalarMult(pair.relVel.dotProduct(pair.normal) * .3 + dist * .3));
                                
        if (pair.A instanceof MobileObject2D)
        {
        MobileObject2D mobjA = (MobileObject2D)pair.A;
        Double2D ra = pair.ra; //rotate(mobjA.getOrientation().radians);
        mobjA.addForce(contactForce);
        mobjA.addTorque(ra.perpDot(contactForce));
        }
                                        
        if (pair.B instanceof MobileObject2D)
        {
        contactForce = contactForce.scalarMult(-1);
        MobileObject2D mobjB = (MobileObject2D)pair.B;
        Double2D rb = pair.rb; //rotate(mobjB.getOrientation().radians);
        mobjB.addForce(contactForce);
        mobjB.addTorque(rb.perpDot(contactForce));
        }
        }
        }
        */
        }
    }
