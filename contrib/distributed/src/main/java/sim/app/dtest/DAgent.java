/*
\  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dtest;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;

import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.util.Double2D;

public class DAgent extends DSteppable implements Remote
{
	private static final long serialVersionUID = 1;
	public Double2D loc;
	public Double2D initLoc;
	public ArrayList<Long> neighbours;

	public DAgent(final Double2D location)
	{
		this.loc = location;
		this.initLoc = location;
		this.neighbours = new ArrayList<>();
	}

	public void step(final SimState state)
	{
		final DSimulation dSimstate = (DSimulation) state;
		Double2D curr_loc = loc;
		double curr_x = loc.x;
		double curr_y = loc.y;
		double new_x = curr_x;
		double new_y = curr_y;

		if (dSimstate.schedule.getSteps() < 46)
		{
			if (curr_x < 300)
			{
				new_x += 5;
			}
			else if (curr_x > 300)
			{
				new_x -= 5;
			}
			if (curr_y < 300)
			{
				new_y += 5;
			}
			else if (curr_y > 300)
			{
				new_y -= 5;
			}
		}
		else
		{
			if (curr_x < initLoc.x)
			{
				new_x += 5;
			}
			else if (curr_x > initLoc.x)
			{
				new_x -= 5;
			}
			if (curr_y < initLoc.y)
			{
				new_y += 5;
			}
			else if (curr_y > initLoc.y)
			{
				new_y -= 5;
			}
		}
		List<DAgent> tmp = dSimstate.field.getNeighborsWithinDistance(loc, dSimstate.neighborhood);
		for (DAgent a : tmp)
		{
			if (!neighbours.contains(a.ID()))
			{
				neighbours.add(a.ID());
				// if (!neighbours.contains(a.getId())) {
				// neighbours.add(a.getId());
			}
		}
		Double2D new_loc = new Double2D(new_x, new_y);
		loc = new_loc;
		dSimstate.field.moveAgent(new_loc, this);

	}
}