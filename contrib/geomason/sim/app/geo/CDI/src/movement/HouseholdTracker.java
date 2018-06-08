package CDI.src.movement;

import sim.engine.SimState;

public class HouseholdTracker {
	
	Household household;
	NorthLandsMovement model;
	boolean flag=false;

	public HouseholdTracker(NorthLandsMovement model) {
		this.model=model;
		
	}
	
	public void track() {
		
		if (model.schedule.getTime() >= model.parameters.trackFromTime) {
			
			
			if (household==null && flag==false) {
				for (Household temp : model.households) {
					if (temp.currentCell.x==model.parameters.trackFromCellX && temp.currentCell.y==model.parameters.trackFromCellY) {
						this.household=temp;	
					}
				}
			}
			
			flag=true;
			
			if (household==null && flag==true) {
				System.out.println("Sorry, no household to track in selected cell.");
			}
			
			if (household!=null) {
				model.collector.updateHouseholdFile(household);
			}
		}
		
		
		
	}
	
	
}
