package conflictdiamonds;

import conflictdiamonds.ConflictDiamonds.Action;
import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * The resident agent represents the individuals in the model that have not rebelled
 * Resident extends Person
 * 
 * @author bpint
 *
 */
public class Resident extends Person implements Steppable {
	
    public Resident(ConflictDiamonds conflict, Parcel par, Region r) {
        super(conflict, par, r);       
    }	
	
    public void step(SimState state) {

        if ( this.residingParcel != null ) {
            //determien behavior by running intensity analyzer
            this.determineBehavior();
            //if opportunity and motivation exists to mine, move towards diamond mines and make employee diamond miner
            if ( this.getCurrentAction() == Action.Move_Closer_to_Diamond_Mines_Mine ) {
                //if I work for other employer, leave that employer and join diamond miner employer
                if ( conflict.otherEmployer != null ) {    
                    conflict.otherEmployer.removeEmployee(this);
                    this.getResidingRegion().removeFormalEmployee(this);
                    this.setDiamondMiner(conflict.diamondMinerEmployer);
                    conflict.diamondMinerEmployer.addEmployee(this);
                    this.getResidingRegion().addInformalEmployee(this);
                }
                //otherwise, join diamond miner employer
                else {    	
                    this.setDiamondMiner(conflict.diamondMinerEmployer);
                    conflict.diamondMinerEmployer.addEmployee(this);
                    this.getResidingRegion().addInformalEmployee(this);
                }
                if (this.getIncomeLevel() == 0) { this.getResidingRegion().removeFoodPoor(this); }
                
                //as a diamond miner, income is set to level one
                this.setIncomeLevel(1);
                this.getResidingRegion().addTotalPoor(this);
   
            }  	
            //if employed as a diamond miner, move closer to diamond mines
            if ( conflict.diamondMinerEmployer.isEmployedHere(this)) {
                this.move();   			
            }

            //if should rebel is true, make person a rebel object
            if ( this.getCurrentAction() == Action.Move_Close_to_Diamond_Mines_Rebel ) {
                rebel();   
            }  		 			
    	}
      	// schedule self to update next turn!
    	state.schedule.scheduleOnce( this );
    }	

    //determine if person object is a resident
    public boolean isPersonType(Person obj) {
        if (obj!=null && obj instanceof Resident) { return false; }
        else { return true; }
    }
		
}
