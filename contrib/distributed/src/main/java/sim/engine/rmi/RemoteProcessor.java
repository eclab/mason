package sim.engine.rmi;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import sim.display.*;
import sim.field.storage.*;
import sim.util.*;
import sim.engine.*;

/***
	REMOTE PROCESSOR
	
	<p>
	This RMI object, which is registered with the RMI Registry, is the top-level RMI access
	object for a given processor.  Remote visualization tools should lock() on this object
	before accessing elements from it, then unlock() when they are finished.  This guarantees
	that the object is in-between steps and the visualization tool has atomic control over it.
*/

public class RemoteProcessor extends UnicastRemoteObject implements VisualizationProcessor
{
	private static final long serialVersionUID = 1L;

	DSimState state;
	ReentrantLock lock = new ReentrantLock(true); // Fair lock
	public final String processorName;
	static ArrayList<VisualizationProcessor> processorCache = new ArrayList<>();

	/**
	 * Creates a processor and registers it to the RMI Registry
	 * 
	 * @param state
	 * @throws RemoteException
	 */
	public RemoteProcessor(DSimState state) throws RemoteException
	{
		// super(DSimState.getPID());
		// TODO: What constructor to use for UnicastRemoteObject?
		super();

		this.state = state;
		int pid = DSimState.getPID();
		processorName = RemoteProcessor.getProcessorName(pid);

		try
		{
			if (!DRegistry.getInstance().registerObject(processorName, this))
				throw new RuntimeException("Failed to register processor: " + processorName);
		}
		catch (RemoteException e)
		{
			throw new RuntimeException("Failed to register processor: " + processorName + ";  " +
					e.getMessage());
		}
	}

	public void lock() throws RemoteException
	{
		lock.lock();
	}

	public void unlock() throws RemoteException
	{
		lock.unlock();
	}

	public IntRect2D getStorageBounds() throws RemoteException
	{
		return state.getPartition().getHaloBounds();
	}

	public GridStorage getStorage(int fieldId) throws RemoteException
	{
		return state.getFieldList().get(fieldId).getStorage();
	}

	public GridRMI getGrid(int fieldId) throws RemoteException
	{
		return state.getFieldList().get(fieldId);
	}

	public int getNumProcessors() throws RemoteException
	{
		return state.getPartition().getNumProcessors();
	}

	public IntRect2D getWorldBounds() throws RemoteException
	{
		return state.getPartition().getWorldBounds();
	}

	public long getSteps() throws RemoteException
	{
		return state.schedule.getSteps();
	}

	public double getTime() throws RemoteException
	{
		return state.schedule.getTime();
	}

	public ArrayList<IntRect2D> getAllLocalBounds() throws RemoteException
	{
		return state.getPartition().getAllBounds();
	}

	public static VisualizationProcessor getProcessor(int pid)
	{
		VisualizationProcessor processor;
		if (processorCache.size() <= pid)
		{
			// Extend the dynamic array
			for (int i = processorCache.size(); i <= pid; i++)
				processorCache.add(null);
			processor = fetchAndUpdate(pid);
		}
		else
		{
			processor = processorCache.get(pid);
			if (processor == null)
				processor = fetchAndUpdate(pid);
		}
		return processor;
	}

	public static String getProcessorName(int pid)
	{
		return "<Processor "  + pid + ">" ;
	}

	private static VisualizationProcessor fetchAndUpdate(int pid)
	{
		try
		{
			VisualizationProcessor proc = DRegistry.getInstance().getObjectT(getProcessorName(pid));
			processorCache.set(pid, proc);
			return proc;
		}
		catch (RemoteException | NotBoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	public int getAOI() throws RemoteException
	{
		return state.getPartition().getAOI();
	}

	public int getProcessorLevels() throws RemoteException
	{
		return state.getPartition().getTreeDepth();
	}

	public int[] getProcessorNeighborhood(int level) throws RemoteException
	{
		return state.getPartition().getProcessorNeighborhood(level);
	}
	
	//Raj: input pids and get all neighbors in the lowest point in quadtree that contains inputed pids
	public int[] getMinimumNeighborhood(int[] proc_ids) throws RemoteException
	{
		if (proc_ids.length == 1)
		{
			return proc_ids;
		}
		
		int selected_level = state.getPartition().getTreeDepth(); //-1?
		
		for (int i=selected_level; i>=0; i--)
		{
			boolean all_contained = true;
			
			int[] chosen_neighborhood = getProcessorNeighborhood(i); //this should contain all partitions
			
			for (int a : chosen_neighborhood)
			{
				System.out.println(a);
			}

			
			for (int proc_id : proc_ids)
			{
				boolean contained = false;
				for (int neigh_id : chosen_neighborhood)
				{
					if (proc_id == neigh_id)
					{
						contained = true;
						break; //found
					}
					
				}
				
				if (contained == false)
				{
					all_contained = false;
					break;
				}
			}
			
			if (all_contained == true)
			{
				return chosen_neighborhood;
			}
		}
		
        throw new RemoteException("some proc_ids not in quad tree");    
	}

	public ArrayList<Stat> getStatList() throws RemoteException
	{
		return state.getStatList();
	}

	public ArrayList<Stat> getDebugList() throws RemoteException
	{
		return state.getDebugList();
	}

	public void initStat() throws RemoteException
	{
		state.recordStats = true;
	}

	public void initDebug() throws RemoteException
	{
		state.recordDebug = true;
	}

	public void stopStat() throws RemoteException
	{
		state.recordStats = false;
	}

	public void stopDebug() throws RemoteException
	{
		state.recordDebug = false;
	}

//	// TODO: do we extend this to other Remote objects?
//	private Remote getRemote(String key) {
//		Remote remote = cache.get(key);
//		if (remote != null)
//			return remote;
//		else {
//			try {
//				remote = (Remote) DRegistry.getInstance().getObject(key);
//				cache.put(key, remote);
//				return remote;
//			} catch (RemoteException | NotBoundException e) {
//				throw new RuntimeException(e);
//			}
//		}
//	}
}
