/*
  Copyright 2020 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import sim.engine.rmi.RemotePromise;
import sim.engine.transport.MigratableObject;

/**
 * A superclass for objects that may be accessed and queried remotely. To do
 * this, such objects need a unique system-wide ID. This ID is generated through
 * a combination of the PID on which the object was first created, and a counter
 * special to that PID. DObject implements a basic form of equals and hashCode
 * based on comparing these IDs.
 */

public abstract class DObject implements Distinguished, MigratableObject {

	private static final long serialVersionUID = 1L;

	// name used to register the object on the registry
	private String exportedName = null;

	public void setExportedName(String exportedName) {
		this.exportedName = exportedName;
	}

	public String getExportedName() {
		return this.exportedName;
	}

	private static int idCounter = 0;
	private static final AtomicInteger threadSafeCounter = new AtomicInteger();

	static int nextCounter() {
		if (DSimState.isMultiThreaded())
			return threadSafeCounter.getAndIncrement();
		else
			return idCounter++;
	}

	public final int firstpid; // this is the PID of the *original* processor on which this object was created
	public final int localid; // this is a unique ID special to processor PID

	// queue of remote request to fill
	protected ArrayList<RemotePromise> unfilledPromises = new ArrayList<RemotePromise>();

	// used to create a unique ID for each promise
	private int promiseId = 0;

	public DObject() {
		firstpid = DSimState.getPID(); // called originally to get the FIRST PID
		localid = nextCounter();
	}

	public boolean equals(Object other) {
		if (other == null)
			return false;
		else if (other == this)
			return true;
		else if (!(other instanceof DObject))
			return false;
		else
			return (firstpid == ((DObject) other).firstpid && localid == ((DObject) other).localid);
	}

	public final int hashCode() {
		// stolen from Int2D.hashCode()
		int y = this.localid;
		y += ~(y << 15);
		y ^= (y >>> 10);
		y += (y << 3);
		y ^= (y >>> 6);
		y += ~(y << 11);
		y ^= (y >>> 16);
		return firstpid ^ y;
	}

	/** Returns a unique system-wideID to this object. */
	public final long ID() {
		return (((long) firstpid) << 32) | localid;
	}

	/**
	 * Returns a string consisting of the original pid, followed by the unique local
	 * id of the object. Together these form a unique, permanent systemwide id.
	 */
	public final String toIDString() {
		return firstpid + "/" + localid;
	}

	/** Returns a string consisting of the form CLASSNAME:UNIQUEID@CURRENTPID */
	public String toString() {
		int pid = -1;

		try {
			pid = DSimState.getPID();
		} catch (Throwable e) {
			// if its running on the remote visualizer its not going have a pid
		}

		return this.getClass().getName() + ":" + toIDString() + "@" + pid;
	}

	/**
	 * Creates an unfilled RemotePromise that contains the request from some agent.
	 * Add the RemotePromise to the queue so it will be filled then. The promise
	 * needs information about:
	 * 
	 * @param tag      information about what data is required
	 * @param argument optional arguments
	 */

	public String createRemotePromise(int tag, Serializable args) throws RemoteException {
		String uniquePromiseId = "promise" + exportedName + Integer.toString(promiseId);
		RemotePromise remotePromise = new RemotePromise(tag, args, uniquePromiseId);
		unfilledPromises.add(remotePromise);
		promiseId++;

		// add me to the queue of objects that has to fulfill a promise
		if (!DSimState.globalRemotePromises.contains(this))
			DSimState.globalRemotePromises.add(this);

		// return the id that can be used to retrieve the promise from the DRegistry
		return uniquePromiseId;
	}

	/**
	 * Method that must be override by the developer that has the logic for fulfill
	 * a remote promise.
	 * 
	 * @param tag      is the tag used to understand which method to use to fill the
	 *                 promise
	 * @param argument is the optional argument that could be needed
	 */
	public Serializable fillRemotePromise(Integer tag, Serializable argument) throws RemoteException {
		return null;
	}

	public ArrayList<RemotePromise> getUnfilledPromises() {
		return unfilledPromises;
	}
}
