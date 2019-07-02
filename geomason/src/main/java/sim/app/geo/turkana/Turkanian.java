/**
 ** Turkanian.java
 **
 ** Copyright 2011 by Andrew Crooks, Joey Harrison, Mark Coletti, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.turkana;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.DoubleBag;
import sim.util.IntBag;

/**
 * Agent class for the TurkanaSouth model, represents a herd of grazing animals
 * and the herder with them.
 * 
 * @author Joey Harrison
 */
public class Turkanian implements Steppable
{
	private static final long serialVersionUID = 1L;
	TurkanaSouthModel model = null;

	public double energy = 0;	// represents how well fed the agent is
	public double getEnergy() { return energy; }
	
	public int x, y;	// location of the agent
	
	public Turkanian(TurkanaSouthModel model, int x, int y) {
		this.model = model;
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
		double effectiveConsumptionRate = model.vegetationConsumptionRate / model.ticksPerMonth;
		if (model.vegetationGrid.field[x][y] > effectiveConsumptionRate) {
			// if there's enough grass here, eat it
			this.energy += effectiveConsumptionRate * model.energyPerUnitOfVegetation;
			model.vegetationGrid.field[x][y] -= effectiveConsumptionRate;
		}
		else {
			// look for greener pastures
			DoubleBag result = new DoubleBag();
			IntBag xPos = new IntBag(), yPos = new IntBag();
			model.vegetationGrid.getNeighborsMaxDistance(x, y, model.herderVision, false, result, xPos, yPos);
			
			double bestVeg = Double.MIN_VALUE;
			int bestVegIndex = -1;
			for (int i = 0; i < result.numObjs; i++)
            {
                if (result.objs[i] > bestVeg) {
                    bestVeg = result.objs[i];
                    bestVegIndex = i;
                }
            }
			
			if (bestVegIndex != -1) {
				// move to the best spot
				x = xPos.objs[bestVegIndex];
				y = yPos.objs[bestVegIndex];
				model.agentGrid.setObjectLocation(this, x, y);				
			}
		}
		
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
            model.removeAgent(this);
        }	// starved to death :(
		else
        {
            model.schedule.scheduleOnce(this);
        }	// live to graze another day
	}

}
