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
import sim.util.*;

public class DAgent extends DSteppable implements Remote {
	private static final long serialVersionUID = 1;
	public Double2D loc;
	public Double2D initLoc;
	public ArrayList<Long> neighbours;

	public DAgent(final Double2D location) {
		this.loc = location;
		this.initLoc = location;
		this.neighbours = new ArrayList<>();
	}

	public void step(final SimState state) {
		final DSimulation dSimstate = (DSimulation) state;
		Double2D curr_loc = loc;
		double curr_x = loc.c(0);
		double curr_y = loc.c(1);
		double new_x = curr_x;
		double new_y = curr_y;

		if (dSimstate.schedule.getSteps() < 46) {
			if (curr_x < 300) {
				new_x += 5;
			} else if (curr_x > 300) {
				new_x -= 5;
			}
			if (curr_y < 300) {
				new_y += 5;
			} else if (curr_y > 300) {
				new_y -= 5;
			}
		} else {
			if (curr_x < initLoc.c(0)) {
				new_x += 5;
			} else if (curr_x > initLoc.c(0)) {
				new_x -= 5;
			}
			if (curr_y < initLoc.c(1)) {
				new_y += 5;
			} else if (curr_y > initLoc.c(1)) {
				new_y -= 5;
			}
		}
		List<DAgent> tmp = dSimstate.field.getNeighborsWithin(this, dSimstate.neighborhood);
		for (DAgent a : tmp) {
			if (!neighbours.contains(a.getID())) {
				neighbours.add(a.getID());
			//if (!neighbours.contains(a.getId())) {
			//	neighbours.add(a.getId());
			}
		}
		Double2D new_loc = new Double2D(new_x, new_y);
		loc = new_loc;
		dSimstate.field.moveAgent(curr_loc, new_loc, this);

	}
}
