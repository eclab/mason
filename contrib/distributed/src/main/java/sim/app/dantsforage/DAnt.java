/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dantsforage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.util.Int2D;

public class DAnt extends DSteppable
{
	private static final long serialVersionUID = 1;

	public boolean getHasFoodItem()
	{
		return hasFoodItem;
	}

	public void setHasFoodItem(boolean val)
	{
		hasFoodItem = val;
	}

	public boolean hasFoodItem = false;
	double reward = 0;

	int x;
	int y;

	Int2D last;

	public DAnt(double initialReward, final int loc_x, final int loc_y)
	{
		reward = initialReward;
		x = loc_x;
		y = loc_y;
	}

	public void depositPheromone(final SimState state) throws RemoteException
	{
		final DAntsForage af = (DAntsForage) state;

		if (DAntsForage.ALGORITHM == DAntsForage.ALGORITHM_VALUE_ITERATION)
		{
			// test all around
			if (hasFoodItem) // deposit food pheromone
			{
				double max = af.toFoodGrid.get(new Int2D(x, y)).getDouble();
				for (int dx = -1; dx < 2; dx++)
					for (int dy = -1; dy < 2; dy++)
					{
						int _x = dx + x;
						int _y = dy + y;
						if (_x < 0 || _y < 0 || _x >= DAntsForage.GRID_WIDTH || _y >= DAntsForage.GRID_HEIGHT)
							continue; // nothing to see here
						double m = af.toFoodGrid.get(new Int2D(_x, _y)).getDouble() *
								(dx * dy != 0 ? // diagonal corners
										af.diagonalCutDown : af.updateCutDown)
								+
								reward;
						if (m > max)
							max = m;
					}
				af.toFoodGrid.set(new Int2D(x, y), max);
			}
			else
			{
				double max = af.toHomeGrid.get(new Int2D(x, y)).getDouble();
				for (int dx = -1; dx < 2; dx++)
					for (int dy = -1; dy < 2; dy++)
					{
						int _x = dx + x;

						int _y = dy + y;
						if (_x < 0 || _y < 0 || _x >= DAntsForage.GRID_WIDTH || _y >= DAntsForage.GRID_HEIGHT)
							continue; // nothing to see here
						double m = af.toHomeGrid.get(new Int2D(_x, _y)).getDouble() *
								(dx * dy != 0 ? // diagonal corners
										af.diagonalCutDown : af.updateCutDown)
								+
								reward;
						if (m > max)
							max = m;
					}
				af.toHomeGrid.set(new Int2D(x, y), max);
			}
		}
		reward = 0.0;
	}

	public void act(final SimState state) throws RemoteException
	{
		final DAntsForage af = (DAntsForage) state;

		if (hasFoodItem) // follow home pheromone
		{
			double max = DAntsForage.IMPOSSIBLY_BAD_PHEROMONE;
			int max_x = x;
			int max_y = y;
			int count = 2;
			for (int dx = -1; dx < 2; dx++)
				for (int dy = -1; dy < 2; dy++)
				{
					int _x = dx + x;
					int _y = dy + y;
					if ((dx == 0 && dy == 0) ||
							_x < 0 || _y < 0 ||
							_x >= DAntsForage.GRID_WIDTH || _y >= DAntsForage.GRID_HEIGHT ||
							af.obstacles.get(new Int2D(_x, _y)).getInt() == 1)
						continue; // nothing to see here
					double m = af.toHomeGrid.get(new Int2D(_x, _y)).getDouble();
					if (m > max)
					{
						count = 2;
					}
					// no else, yes m > max is repeated
					if (m > max || (m == max && state.random.nextBoolean(1.0 / count++))) // this little magic makes all
																							// "==" situations equally
																							// likely
					{
						max = m;
						max_x = _x;
						max_y = _y;
					}
				}
			if (max == 0 && last != null) // nowhere to go! Maybe go straight
			{
				if (state.random.nextBoolean(af.momentumProbability))
				{
					int xm = x + (x - last.x);
					int ym = y + (y - last.y);
					if (xm >= 0 && xm < DAntsForage.GRID_WIDTH && ym >= 0 && ym < DAntsForage.GRID_HEIGHT
							&& af.obstacles.get(new Int2D(xm, ym)).getInt() == 0)
					{
						max_x = xm;
						max_y = ym;
					}
				}
			}
			else if (state.random.nextBoolean(af.randomActionProbability)) // Maybe go randomly
			{
				int xd = (state.random.nextInt(3) - 1);
				int yd = (state.random.nextInt(3) - 1);
				int xm = x + xd;
				int ym = y + yd;
				if (!(xd == 0 && yd == 0) && xm >= 0 && xm < DAntsForage.GRID_WIDTH && ym >= 0
						&& ym < DAntsForage.GRID_HEIGHT && af.obstacles.get(new Int2D(xm, ym)).getInt() == 0)
				{
					max_x = xm;
					max_y = ym;
				}
			}
			
            int old_x = x;
            int old_y = y;

			last = new Int2D(x, y);

			x = max_x;
			y = max_y;
			
			
			
			if (af.sites.get(new Int2D(max_x, max_y)).getInt() == DAntsForage.HOME) // reward me next time! And change my status
			{
				reward = af.reward;
				hasFoodItem = !hasFoodItem;
			}
			
			af.buggrid.moveAgent(new Int2D(old_x, old_y), new Int2D(max_x, max_y), this);

		}
		else
		{
			
			double max = DAntsForage.IMPOSSIBLY_BAD_PHEROMONE;
			int max_x = x;
			int max_y = y;
			int count = 2;
			for (int dx = -1; dx < 2; dx++)
				for (int dy = -1; dy < 2; dy++)
				{

					int _x = dx + x;
					int _y = dy + y;
					if ((dx == 0 && dy == 0) ||
							_x < 0 || _y < 0 ||
							_x >= DAntsForage.GRID_WIDTH || _y >= DAntsForage.GRID_HEIGHT ||
							af.obstacles.get(new Int2D(_x, _y)).getInt() == 1)
						continue; // nothing to see here
					double m = af.toFoodGrid.get(new Int2D(_x, _y)).getDouble();
					
                    if (m > max)
					{
						count = 2;
					}
					// no else, yes m > max is repeated
					if (m > max || (m == max && state.random.nextBoolean(1.0 / count++))) // this little magic makes all
																							// "==" situations equally
																							// likely
					{
						max = m;
						max_x = _x;
						max_y = _y;
					}
				}
			

			
			if (max == 0 && last != null) // nowhere to go! Maybe go straight
			{
				if (state.random.nextBoolean(af.momentumProbability))
				{
					System.out.println("momentum direction: "+(x - last.x)+" "+(y - last.y));
					int xm = x + (x - last.x);
					int ym = y + (y - last.y);
					if (xm >= 0 && xm < DAntsForage.GRID_WIDTH && ym >= 0 && ym < DAntsForage.GRID_HEIGHT
							&& af.obstacles.get(new Int2D(xm, ym)).getInt() == 0)
					{
						max_x = xm;
						max_y = ym;
					}
				}
			}
			else if (state.random.nextBoolean(af.randomActionProbability)) // Maybe go randomly
			{
				int xd = (state.random.nextInt(3) - 1);
				int yd = (state.random.nextInt(3) - 1);
				int xm = x + xd;
				int ym = y + yd;
				if (!(xd == 0 && yd == 0) && xm >= 0 && xm < DAntsForage.GRID_WIDTH && ym >= 0
						&& ym < DAntsForage.GRID_HEIGHT && af.obstacles.get(new Int2D(xm, ym)).getInt() == 0)
				{
					max_x = xm;
					max_y = ym;
				}
			}
			
            int old_x = x;
            int old_y = y;

			
			//af.buggrid.add(new Int2D(max_x, max_y), this);
			last = new Int2D(old_x, old_y);
			x = max_x;
			y = max_y;			
			

			if (af.sites.get(new Int2D(max_x, max_y)).getInt() == DAntsForage.FOOD) // reward me next time! And change my status
			{
				reward = af.reward;
				hasFoodItem = !hasFoodItem;
			}
			
			//System.out.println("Ant : from "+new Int2D(old_x,old_y)+" to "+new Int2D(max_x, max_y));
			af.buggrid.moveAgent(new Int2D(old_x,old_y), new Int2D(max_x, max_y), this); //this needs to be last, otherwise rest of code may not execute if moving partitions

		}
	}

	public void step(final SimState state)
	{
		try
		{
		depositPheromone(state);
		
		
		act(state);
		
		}
		catch (Exception e)
		{
			System.out.println(e);
			System.exit(-1);
		}
	}

	// a few tweaks by Sean
	private Color noFoodColor = Color.black;
	private Color foodColor = Color.red;

	public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		if (hasFoodItem)
			graphics.setColor(foodColor);
		else
			graphics.setColor(noFoodColor);

		// this code was stolen from OvalPortrayal2D
		int x = (int) (info.draw.x - info.draw.width / 2.0);
		int y = (int) (info.draw.y - info.draw.height / 2.0);
		int width = (int) (info.draw.width);
		int height = (int) (info.draw.height);
		graphics.fillOval(x, y, width, height);

	}
}
