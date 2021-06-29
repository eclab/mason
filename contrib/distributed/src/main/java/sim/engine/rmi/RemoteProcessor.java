package sim.engine.rmi;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import sim.engine.transport.*;
import sim.display.*;
import sim.field.storage.*;
import sim.util.*;
import sim.engine.*;

public class RemoteProcessor extends UnicastRemoteObject implements VisualizationProcessor
{
	// public class RemoteProcessor implements VisualizationProcessor {
	private static final long serialVersionUID = 1L;

	DSimState dSimState;
	ReentrantLock lock = new ReentrantLock(true); // Fair lock
	public static final String NAME_PREFIX = "processorPId: ";
	public final String processorName;
	static ArrayList<VisualizationProcessor> processorCache = new ArrayList<>();

	/**
	 * Creates a processor and registers it to the RMI Registry
	 * 
	 * @param dSimState
	 * @throws RemoteException
	 */
	public RemoteProcessor(DSimState dSimState) throws RemoteException
	{
		// super(DSimState.getPID());
		// TODO: What constructor to use for UnicastRemoteObject?
		super();

		this.dSimState = dSimState;
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
		return dSimState.getPartition().getHaloBounds();
	}

	public GridStorage getStorage(int fieldId) throws RemoteException
	{
		return dSimState.getFieldList().get(fieldId).getStorage();
	}

	public TransportRMIInterface getTransportRMI(int fieldId) throws RemoteException
	{
		return dSimState.getFieldList().get(fieldId);
	}

	public int getNumProcessors() throws RemoteException
	{
		return dSimState.getPartition().getNumProcessors();
	}

	public IntRect2D getWorldBounds() throws RemoteException
	{
		return dSimState.getPartition().getWorldBounds();
	}

	public long getSteps() throws RemoteException
	{
		return dSimState.schedule.getSteps();
	}

	public double getTime() throws RemoteException
	{
		return dSimState.schedule.getTime();
	}

	public ArrayList<IntRect2D> getAllLocalBounds() throws RemoteException
	{
		return dSimState.getPartition().getAllBounds();
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
		return NAME_PREFIX + pid;
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
		return dSimState.getPartition().getAOI();
	}

	public int getProcessorLevels() throws RemoteException
	{
		return dSimState.getPartition().getTreeDepth();
	}

	public int[] getProcessorNeighborhood(int level) throws RemoteException
	{
		return dSimState.getPartition().getProcessorNeighborhood(level);
	}
	
	//Raj: input pids and get all neighbors in the lowest point in quadtree that contains inputed pids
	public int[] getMinimumNeighborhood(int[] proc_ids) throws RemoteException
	{
		if (proc_ids.length == 1)
		{
			return proc_ids;
		}
		
		int selected_level = dSimState.getPartition().getTreeDepth(); //-1?
		
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
        //return null;
	}

	public ArrayList<Stat> getStatList() throws RemoteException
	{
		return dSimState.getStatList();
	}

	public ArrayList<Stat> getDebugList() throws RemoteException
	{
		return dSimState.getDebugList();
	}

	public void initStat() throws RemoteException
	{
		dSimState.recordStats = true;
	}

	public void initDebug() throws RemoteException
	{
		dSimState.recordDebug = true;
	}

	public void stopStat() throws RemoteException
	{
		dSimState.recordStats = false;
	}

	public void stopDebug() throws RemoteException
	{
		dSimState.recordDebug = false;
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
