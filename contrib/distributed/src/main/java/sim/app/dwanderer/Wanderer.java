/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dwanderer;

import java.io.Serializable;
import java.rmi.RemoteException;

import sim.engine.*;
import sim.util.*;

public class Wanderer extends DSteppable implements Distinguished {

	private static final long serialVersionUID = 1;

	// position of the agent in the field
	public Double2D loc;

	public int myPID = DSimState.getPID();

	public String name = null;


	public Wanderer(final Double2D location) {
		this.loc = location;
	}
	
	public void step(final SimState state) {
		final Wanderers wanderersState = (Wanderers) state;

		String otherAgentID = name.equals("A")?"B":"A";

		// moving the agents clockwise to go to the center of another area
		// in this way the agents are always in different processors

		// proc A: center (50,50), field (0<x<100)-(0<y<100)
		// proc B: center (150,50), field (100<x<200)-(0<y<100)
		// proc C: center (150,150), field (100<x<200)-(100<y<200)
		// proc D: center (50,150), field (0<x<100)-(100<y<200)
		// Double2D oldLoc = loc;

		if ((loc.x >= 50 && loc.x < 150) && (loc.y == 50)) {
			// System.out.println("I am heading east from A to B with position: " + loc + ".
			// Proc: " + wanderersState.getPartition().getPID());
			loc = new Double2D(loc.x + 5, loc.y);
		} else if ((loc.x == 150) && (loc.y >= 50 && loc.y < 150)) {
			// System.out.println("I am heading south from B to C with position: " + loc +
			// ". Proc: " + wanderersState.getPartition().getPID());
			loc = new Double2D(loc.x, loc.y + 5);
		} else if ((loc.x > 50 && loc.x <= 150) && (loc.y == 150)) {
			// System.out.println("I am heading west from C to D with position: " + loc + ".
			// Proc: " + wanderersState.getPartition().getPID());
			loc = new Double2D(loc.x - 5, loc.y);
		} else if ((loc.x == 50) && (loc.y > 50 && loc.y <= 150)) {
			// System.out.println("I am heading north from D to A with position: " + loc +
			// ". Proc: " + wanderersState.getPartition().getPID());
			loc = new Double2D(loc.x, loc.y - 5);
		}
	
		try {
			System.out.println(
			state.schedule.getSteps()+"]"+
			"I am " + name + 
			" on proc "+ DSimState.getPID() +
			" my friend is " + otherAgentID +
			" on proc "+
			((Distinguished)DRegistry.getInstance().getObject(otherAgentID)).remoteMessage(0, null));

			wanderersState.wanderers.moveAgent(loc, this);
			myPID = wanderersState.getPartition().getPID();
		} catch (Exception e) {
			System.err.println("Error on agent " + this + " in step " + wanderersState.schedule.getSteps() + "on PID "
					+ wanderersState.getPartition().getPID());
			throw new RuntimeException(e);
		}
	}

	public Serializable remoteMessage(Integer tag, Serializable argument) throws RemoteException {
		switch (tag) {
			case 0:
				return DSimState.getPID();
			default:
				return null;
		}
	}

}