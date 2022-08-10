package sim.app.geo.dturkana;

import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.DoubleBag;
import sim.util.Int2D;
import sim.util.IntBag;


public class DTurkanian extends DSteppable{
	
	static final long serialVersionUID = 1L;
	DTurkanaSouthModel model = null;

	public double energy = 0;	// represents how well fed the agent is
	public double getEnergy() { return energy; }
	
	public int x, y;	// location of the agent
	
	public DTurkanian(int x, int y) {
		//this.model = model; //I don't think we can transfer this we keep track of this, instead use state in step
		this.x = x; 
		this.y = y;
	}
	
	/**
	 * The agent's step function. Agent will eat some grass if it's there, otherwise
	 * it will move elsewhere. Agent will use energy and if energy is too low it will die.
	 * If agent energy gets above a threshold it will reproduce.
	 */
	@Override
	public void step(SimState state) {
		
		boolean movedToNewPartition = false;
		
		DTurkanaSouthModel model = (DTurkanaSouthModel) state; //I think this should be ok, as I assume if will point to the region it corresponds to
		
        //EDIT UNDER HERE ----------------------------------------------------
		
		double effectiveConsumptionRate = model.vegetationConsumptionRate / model.ticksPerMonth;
		
		
		
		
		if (model.vegetationGrid.getLocal(new Int2D(x,y)) > effectiveConsumptionRate){
			
			//System.out.println("agent "+this);
			
			// if there's enough grass here, eat it
			this.energy += effectiveConsumptionRate * model.energyPerUnitOfVegetation;
			//model.vegetationGrid.field[x][y] -= effectiveConsumptionRate;
			model.vegetationGrid.setLocal(new Int2D(x,y), model.vegetationGrid.getLocal(new Int2D(x,y)) - effectiveConsumptionRate);
		}
		else {
			// look for greener pastures
			//DoubleBag result = new DoubleBag();
			IntBag xPos = new IntBag(), yPos = new IntBag();
			//model.vegetationGrid.getNeighborsMaxDistance(x, y, , false, result, xPos, yPos);
			model.vegetationGrid.getMooreLocations(x, y, model.herderVision, 0, true, xPos, yPos ); //NOT PASSING RESULT HERE!
			
			
			double bestVeg = Double.MIN_VALUE;
			int bestVegIndex = -1;
			for (int i = 0; i < xPos.numObjs; i++)
            {
				
				double vegResult = model.vegetationGrid.getLocal(new Int2D(xPos.objs[i], yPos.objs[i]));
				
                if (vegResult > bestVeg) {
                    bestVeg = vegResult;
                    bestVegIndex = i;

                    //System.out.println("best veg found "+bestVeg);
                    //System.exit(-1);
                    
                }
            }
			
			if (bestVegIndex != -1) {
				// move to the best spot
				int old_x = x;
				int old_y = y;
				x = xPos.objs[bestVegIndex];
				y = yPos.objs[bestVegIndex];
				//model.agentGrid.addLocal(new Int2D(x,y), this);  //should this just be add? in case goes into halo?
				//model.agentGrid.add(new Int2D(x,y), this);  //should this just be add? in case goes into halo?
				model.agentGrid.moveAgent(new Int2D(old_x,old_y), new Int2D(x,y), this);  //should this just be add? in case goes into halo?
                
				if (!model.agentGrid.getLocalBounds().contains(new Int2D(x,y))) {
					
		            //model.agentGrid.removeLocal(new Int2D(x,y), this); //This is handled in moveAgent
		            model.agents.remove(this);
		            movedToNewPartition = true;
					
				}
				
			}
		}
		//move above
		
		//email about this
		if (!movedToNewPartition) {
			//System.out.println("even though moved, continuing code!");
			//System.exit(-1);
		
		// use energy
		energy -= model.energyConsumptionRate / model.ticksPerMonth;
		
		// consider reproducing
		if (energy > model.birthEnergy)
        {
            model.createOffspring(this);
        }
		
		// consider starving to death
		if (energy < model.starvationLevel)
        {
            //model.removeAgent(this);
            model.agentGrid.removeLocal(new Int2D(x,y), this);
            model.agents.remove(this);
        }	// starved to death :(
		else
        {
            model.schedule.scheduleOnce(this);
        }	// live to graze another day
		}
	}

}
