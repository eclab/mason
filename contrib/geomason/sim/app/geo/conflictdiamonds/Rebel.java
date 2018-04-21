package conflictdiamonds;

import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * The rebel agent represents the individuals in the model that have rebelled 
 * Rebel extends Persons
 * 
 * @author bpint
 *
 */
public class Rebel extends Person implements Steppable {
	
    public Rebel(ConflictDiamonds conflict, Parcel par, Region r) {
            super(conflict, par, r);    
    
    }
	
    public void step(SimState state) {  
        
        //move towards goal (i.e. diamond mines)
        if ( this.getOpposition() == false ) {
            this.move();
        }		

        // schedule self to update next turn!
        state.schedule.scheduleOnce( this );
    }	
    
    //determines if object is a rebel
    public boolean isPersonType(Person obj) {
        return (obj!=null && obj instanceof Rebel);
    }

}
