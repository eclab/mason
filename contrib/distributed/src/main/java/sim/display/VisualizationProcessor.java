package sim.display;

import java.rmi.Remote;



import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.engine.rmi.GridRMI;
import sim.field.storage.GridStorage;
import sim.util.IntRect2D;
import sim.util.Properties;
import sim.util.SimpleProperties;

/**
 VISUALIZATION PROCESSOR is the remote interface of each partitition exposed to
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

public interface VisualizationProcessor extends Remote
{
	public static final int NUM_STAT_TYPES = 2;
	public static final int STAT_TYPE_STATISTICS = 0;
	public static final int STAT_TYPE_DEBUG = 1;
	
	/**
	 * Blocks until the remote processor is in a state where we can safely grab
	 * storage objects without creating a race condition; then holds a lock to
	 * prevent the remote processor from continuing.
	 */
	public void lock() throws RemoteException;

	/**
	 * Releases the lock obtained by lock(), thus letting the remote processor
	 * continue its work.
	 */
	public void unlock() throws RemoteException;

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
	public int[] getMinimumNeighborhood(int[] proc_ids) throws RemoteException;

	public ArrayList<Stat> getStats(int statType) throws RemoteException;
	
	public void startStats(int statType) throws RemoteException;
	
	public void stopStats(int statType) throws RemoteException;

	//public Properties getProperties() throws RemoteException; //added by Raj
	
	//public Object getPropertiesValues(int index) throws RemoteException;
	
	//public PropertiesRequester getPropRequester();

	public Object getPropertiesValue(int index) throws RemoteException;

	
	public int getPropertiesNumProperties() throws RemoteException;
	

    
    public String getPropertiesName(int index)  throws RemoteException;
 

    public String getPropertiesDescription(int index) throws RemoteException;

    

    public Object getPropertiesDomain(int index) throws RemoteException;

    public boolean propertiesIsHidden(int index) throws RemoteException;
    

	
	
	// TODO: Adding here for now
	// Not sure if one class can implement two remote interfaces
	public GridRMI getGrid(int fieldId) throws RemoteException;


}