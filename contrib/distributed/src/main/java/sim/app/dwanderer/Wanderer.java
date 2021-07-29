/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dwanderer;

import java.io.Serializable;
import sim.engine.*;
import sim.util.*;
import java.util.*;

public class Wanderer extends DSteppable {

	private static final long serialVersionUID = 1;

	// position of the agent in the field
	public Double2D loc;

	public int myPID = DSimState.getPID();

	// queue of RemotePromise to use
	ArrayList<String> remotePromiseQueue = new ArrayList<String>();

	// queue of RemotePromise already used
	ArrayList<String> remotePromiseCompleted = new ArrayList<String>();

	public Wanderer(final Double2D location) {
		this.loc = location;
	}

	public void step(final SimState state) {
		final Wanderers wanderersState = (Wanderers) state;

		myPID = DSimState.getPID();
		// System.out.println("Into the step I am " + this.getExportedName() + " this is
		// myPID " + myPID + " and loc " + this.loc);

		// System.out.println("%%%%%%% " + wanderersState.getDRegistry().getMigratedNames().size());

		String myAgentID;
		String otherAgentID;

		// check if I am agent A or agent B
		if (this.getExportedName().equals("agentA")) {
			// I am agent A and I want data from agent B
			myAgentID = "agentA";
			otherAgentID = "agentB";
		} else {
			// I am agent B and I want data from agent A
			myAgentID = "agentB";
			otherAgentID = "agentA";
		}


		// iterates on the queue of RemotePromise to use them
		try {
			for (String promiseID : remotePromiseQueue) {
				// get the remote promise from the registry using the ID in the queue
				Promised promise = (Promised) wanderersState.getDRegistry().getObject(promiseID);
				// check if the promise is ready, i.e. if it has been fulfilled
				if (promise.isReady()) {
					// the promise is ready, I can use the content
					System.out.println(
							"I am " + myAgentID + " on proc " + myPID + " with loc " + loc + " - Promise from " + otherAgentID + " contains: " + promise.get());
					// add the promiseID in the completed queue
					remotePromiseCompleted.add(promiseID);
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
			// System.out.println("Promise non available!");
		}

		// remove the promises already used
		for (String c : remotePromiseCompleted) {
			remotePromiseQueue.remove(c);
		}

		// get the PID of the other agent using the remote method
		try {
			// get the other agent from the registry
			Distinguished otherAgent = (Distinguished) wanderersState.getDRegistry().getObject(otherAgentID);
			// System.out.println(myAgentID + " get " + otherAgentID + " - " + otherAgent);
			// request for data creating a RemotePromise
			String idPromise = otherAgent.createRemotePromise(0, null);
			// add the ID of the RemotePromise in the queue
			// I will use the ID to get the RemotePromise content in the next step
			remotePromiseQueue.add(idPromise);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// moving the agents clockwise to go to the center of another area
		// in this way the agents are always in different processors

		// proc A: center (50,50), field (0<x<100)-(0<y<100)
		// proc B: center (150,50), field (100<x<200)-(0<y<100)
		// proc C: center (150,150), field (100<x<200)-(100<y<200)
		// proc D: center (50,150), field (0<x<100)-(100<y<200)
		Double2D oldLoc = loc;

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
		// System.out.println("I am " + myAgentID + " with loc " + loc);

		try {
			wanderersState.wanderers.moveAgent(loc, this);
			myPID = wanderersState.getPartition().getPID();
		} catch (Exception e) {
			System.err.println("Error on agent " + this + " in step " + wanderersState.schedule.getSteps() + "on PID "
					+ wanderersState.getPartition().getPID());
			throw new RuntimeException(e);
		}
	}

	// implementation of the abstract method used to fulfill RemotePromises
	// returns the PID where the agents is scheduled
	public Serializable fillRemotePromise(Integer tag, Serializable argument) {
		switch (tag) {
			case 0:
				return " I am on proc " + myPID + " with loc " + loc;
			case 1:
				return this.loc;
		}
		return null;
	}

}