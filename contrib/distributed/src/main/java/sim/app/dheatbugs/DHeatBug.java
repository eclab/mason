/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dheatbugs;

import java.util.ArrayList;

import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.field.grid.DDoubleGrid2D;
import sim.util.Int2D;

public class DHeatBug extends DSteppable
{
	private static final long serialVersionUID = 1;

	public int loc_x, loc_y;
	public boolean isFirstStep = true;
	public double idealTemp;
	public double heatOutput;
	public double randomMovementProbability;

	public DHeatBug(final double idealTemp, final double heatOutput, final double randomMovementProbability,
			final int loc_x, final int loc_y)
	{
		this.heatOutput = heatOutput;
		this.idealTemp = idealTemp;
		this.randomMovementProbability = randomMovementProbability;
		this.loc_x = loc_x;
		this.loc_y = loc_y;
	}

	public double addHeat(final DDoubleGrid2D grid, final int x, final int y, final double old_heat,
			final double heat)
	{
		double new_heat = old_heat + heat;
		if (new_heat > DHeatBugs.MAX_HEAT)
			new_heat = DHeatBugs.MAX_HEAT;
		grid.set(new Int2D(x, y), new_heat);
		return new_heat;
	}

	public void step(final SimState state)
	{
		final DHeatBugs dHeatBugs = (DHeatBugs) state;

//		double old_heat;
//		try {
//			RemoteFulfillable promise = dHeatBugs.valgrid.get(new Int2D(loc_x, loc_y));
//			// FIXME: This should only be used after the promise has been fulfilled
//			old_heat = promise.getDouble();
//		} catch (RemoteException e) {
//			throw new RuntimeException(e);
//		}
		
		if (!dHeatBugs.valgrid.isHalo(new Int2D(loc_x, loc_y)))
		{
			
            for (ArrayList<DHeatBug> a : dHeatBugs.bugs.getStorageArray())
            {
            	if (a != null)
            	{
            	    if (a.contains(this))
            	    {
            		    System.out.println("in storage on this partition");
            	    }
            	}
            }
            //System.out.println(this+" "+loc_x+" "+loc_y+" " +dHeatBugs.getPID()+" "+dHeatBugs.getPartition().getLocalBounds());
		}
		double old_heat = dHeatBugs.valgrid.getLocal(new Int2D(loc_x, loc_y));
		double new_heat = 0;

		// Skip addHeat for the first step
		if (!isFirstStep)
		{
			new_heat = addHeat(dHeatBugs.valgrid, loc_x, loc_y, old_heat, heatOutput);
		}
		else
		{
			isFirstStep = false;
		}

		final int START = -1;
		int bestx = START;
		int besty = 0;
		double best_heat = 0;

		if (state.random.nextBoolean(randomMovementProbability)) // go to random place
		{
			bestx = state.random.nextInt(3) - 1 + loc_x;
			besty = state.random.nextInt(3) - 1 + loc_y;
		}
		else if (new_heat > idealTemp) // go to coldest place
		{
			for (int x = -1; x < 2; x++)
				for (int y = -1; y < 2; y++)
					if (!(x == 0 && y == 0))
					{
						final int xx = (x + loc_x);
						final int yy = (y + loc_y);

//						Promise promiseHeat = dHeatBugs.valgrid.get(new Int2D(xx, yy));
//						// FIXME: This should only be used after the promise has been fulfilled
//						double heat = promiseHeat.getDouble();
						double heat = dHeatBugs.valgrid.getLocal(new Int2D(xx, yy));

						// not uniform, but enough to break up the go-up-and-to-the-left syndrome
						if (bestx == START || heat < best_heat || (heat == best_heat && state.random.nextBoolean()))
						{
							bestx = xx;
							besty = yy;
//							promiseBest_heat = dHeatBugs.valgrid.get(new Int2D(bestx, besty));
//							// FIXME: This should only be used after the promise has been fulfilled
//							best_heat = promiseBest_heat.getDouble();
							//best_heat = dHeatBugs.valgrid.getLocal(new Int2D(bestx, besty));
							best_heat = heat;
						}
					}
		}
		else if (new_heat < idealTemp) // go to warmest place
			{
			for (int x = -1; x < 2; x++)
				for (int y = -1; y < 2; y++)
					if (!(x == 0 && y == 0))
					{
						final int xx = (x + loc_x);
						final int yy = (y + loc_y);
//						Promise promiseHeat = dHeatBugs.valgrid.get(new Int2D(xx, yy));
//						// FIXME: This should only be used after the promise has been fulfilled
//						double heat = promiseHeat.getDouble();
						double heat = dHeatBugs.valgrid.getLocal(new Int2D(xx, yy));

						// not uniform, but enough to break up the go-up-and-to-the-left syndrome
						if (bestx == START || heat > best_heat || (heat == best_heat && state.random.nextBoolean()))
						{
							bestx = xx;
							besty = yy;
//							promiseBest_heat = dHeatBugs.valgrid.get(new Int2D(bestx, besty));
//							// FIXME: This should only be used after the promise has been fulfilled
//							best_heat = promiseBest_heat.getDouble();
							//best_heat = dHeatBugs.valgrid.getLocal(new Int2D(bestx, besty));
							best_heat = heat;
						}
					}
		}
		else // stay put
		{
			bestx = loc_x;
			besty = loc_y;
		}

		final int old_x = loc_x;
		final int old_y = loc_y;
		loc_x = dHeatBugs.valgrid.stx(bestx);
		loc_y = dHeatBugs.valgrid.sty(besty);
		

		
	
		

//		System.out.println(DSimState.getPID() + " : " + dHeatBugs.bugs.get(new Int2D(0, 0)));
		
		//System.out.println("moving "+this+" "+(new Int2D(old_x, old_y))+new Int2D(loc_x, loc_y));
		
		
		dHeatBugs.bugs.moveAgent(new Int2D(old_x, old_y), new Int2D(loc_x, loc_y), this);
	}

	public double getIdealTemperature()
	{
		return idealTemp;
	}

	public void setIdealTemperature(final double t)
	{
		idealTemp = t;
	}

	public double getHeatOutput()
	{
		return heatOutput;
	}

	public void setHeatOutput(final double t)
	{
		heatOutput = t;
	}

	public double getRandomMovementProbability()
	{
		return randomMovementProbability;
	}

	public void setRandomMovementProbability(final double t)
	{
		if (t >= 0 && t <= 1)
			randomMovementProbability = t;
	}
}
