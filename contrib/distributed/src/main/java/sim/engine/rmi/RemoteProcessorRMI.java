/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
        
package sim.engine.rmi;

import java.rmi.Remote;

import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.engine.rmi.GridRMI;
import sim.field.storage.GridStorage;
import sim.util.IntRect2D;
import sim.util.Properties;
import sim.util.SimpleProperties;

/**
   REMOTE PROCESSOR RMI is the remote interface of each partitition exposed to
   the visualization program (probably on your laptop).  It does at least the following:
 
   <ul>
   <li> Synchronization with remote partitions so we can safely access them without
   interfere with the model running.
   <li> Getting current simulation time and steps
   <li> Accessing the processor neighborhoods and topology
   <li> Accessing data from the remote fields and their accompanying grid storage.
   <li> Getting the latest statistics and debug streams
   <li> Accessing properties of remote objects.
*/

public interface RemoteProcessorRMI extends Remote
    {
    public static final int NUM_STAT_TYPES = 2;
    public static final int STAT_TYPE_STATISTICS = 0;
    public static final int STAT_TYPE_DEBUG = 1;
        
    /**
     * Only the root partition responds to this: blocks until the 
     * root partition is in a state where we can safely grab
     * storage objects without creating a race condition; then holds a lock to
     * prevent the remote processor from continuing.
     */
    public void lock() throws RemoteException;

    /**
     * Releases the lock obtained by lock(), thus letting the root processor
     * continue its work.
     */
    public void unlock() throws RemoteException;

    /**
     * Blocks until the p partition is in a state where we can safely grab
     * storage objects without creating a race condition; then holds a lock to
     * prevent the remote processor from continuing.
     */
    public void lockPartition() throws RemoteException;

    /**
     * Releases the lock obtained by lockPartition(), thus letting the processor
     * continue its work.
     */
    public void unlockPartition() throws RemoteException;
    
    /** Returns the current bounds of the processor's local region. */

    public IntRect2D getStorageBounds() throws RemoteException;

    /** Returns a full copy of GridStorage object number STORAGE. */
    public GridStorage getStorage(int storage) throws RemoteException;

    /**
     * Returns the number of processors in the distributed model. We presume their
     * pids go 0...n
     */
    public int getNumProcessors() throws RemoteException;

    /** Returns the world (non-toroidal) bounds of the distributed model */

    public IntRect2D getWorldBounds() throws RemoteException;
        
    //// WE ALSO NEED THE FOLLOWING, WHICH DO *NOT* REQUIRE LOCKING BECAUSE THEY ARE SYNCHRONIZED.
    //// JUST CALL THE SECHEDULE'S VERSION

    /** Returns the schedule's current steps */
    public long getSteps() throws RemoteException;

    /** Returns the schedule's current time */
    public double getTime() throws RemoteException;

    /// Not sure how to do this one -- grab information from the quad tree?
    /// The IntHyperRects in this method should return the LOCAL storage size, not
    /// including the halo rect, I think.
    public ArrayList<IntRect2D> getAllLocalBounds() throws RemoteException;

    /// Need this one too
    public int getAOI() throws RemoteException;

    public int getProcessorLevels() throws RemoteException;

    public int[] getProcessorNeighborhood(int level) throws RemoteException;
        
    //Raj: input pids and get all neighbors in the lowest point in quadtree that contains inputed pids
    public int[] getMinimumNeighborhood(int[] processorIDs) throws RemoteException;

    public ArrayList<Stat> getStats(int statType) throws RemoteException;
        
    public void startStats(int statType) throws RemoteException;
        
    public void stopStats(int statType) throws RemoteException;

    public Object getPropertyValue(int index) throws RemoteException;

    public int getNumProperties() throws RemoteException;
            
    public String getPropertyName(int index)  throws RemoteException;

    public String getPropertyDescription(int index) throws RemoteException;

    public Object getPropertyDomain(int index) throws RemoteException;

    public boolean getPropertyIsHidden(int index) throws RemoteException;
            
    public GridRMI getGrid(int fieldID) throws RemoteException;
    }
