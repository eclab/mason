/*
 * SimpleMediator.java
 * 
 * $Id: SimpleMediator.java 1671 2013-03-07 06:35:16Z escott8 $
 * 
 */

package sim.app.geo.riftland.conflict;

import sim.engine.SimState;

/** Mediator that does nothing.
 *
 * @author mcoletti
 */
public class SimpleMediator extends Mediator {

    public SimpleMediator(int width, int height)
    {
        super(width, height);
    }
    
    @Override
    public
    void reconcile(Conflict conflict)
    {
        //System.out.println("reconciling conflict at " + conflict.getParcel());
    }

    public
    void step(SimState state)
    {
        System.err.println("# conflicts: " + this.getNumConflicts());
    
        for ( Conflict conflict : this.conflicts() )
        {
            reconcile(conflict);
        }
        
        this.clearConflicts();        
    }

}
