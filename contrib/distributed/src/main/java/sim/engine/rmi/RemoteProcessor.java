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

public class RemoteProcessor extends UnicastRemoteObject implements VisualizationProcessor {
	// public class RemoteProcessor implements VisualizationProcessor {
	private static final long serialVersionUID = 1L;

	final DSimState dSimState;
	private final ReentrantLock lock = new ReentrantLock(true); // Fair lock
	public static final String NAME_PREFIX = "processorPId: ";
	public final String processorName;
	private static final ArrayList<VisualizationProcessor> processorCache = new ArrayList<>();

	/**
	 * Creates a processor and registers it to the RMI Registry
	 * 
	 * @param dSimState
	 * @throws RemoteException
	 */
	public RemoteProcessor(DSimState dSimState) throws RemoteException {
		// super(DSimState.getPID());
		// TODO: What constructor to use for UnicastRemoteObject?
		super();

		this.dSimState = dSimState;
		final int pid = DSimState.getPID();
		processorName = RemoteProcessor.getProcessorName(pid);

		try {
			if (!DRegistry.getInstance().registerObject(processorName, this))
				throw new RuntimeException("Failed to register processor: " + processorName);
		} catch (RemoteException e) {
			throw new RuntimeException("Failed to register processor: " + processorName + ";  " +
					e.getMessage());
		}
	}

	public void lock() throws RemoteException {
		lock.lock();
	}

	public void unlock() throws RemoteException {
		lock.unlock();
	}

	public IntRect2D getStorageBounds() throws RemoteException {
		return dSimState.getPartition().getHaloBounds();
	}

	public GridStorage getStorage(int fieldId) throws RemoteException {
		return dSimState.getFieldRegistry().get(fieldId).getStorage();
	}

	public TransportRMIInterface getTransportRMI(int fieldId) throws RemoteException {
		return dSimState.getFieldRegistry().get(fieldId);
	}

	public int getNumProcessors() throws RemoteException {
		return dSimState.getPartition().getNumProcessors();
	}

	public IntRect2D getWorldBounds() throws RemoteException {
		return dSimState.getPartition().getWorldBounds();
	}

	public long getSteps() throws RemoteException {
		return dSimState.schedule.getSteps();
	}

	public double getTime() throws RemoteException {
		return dSimState.schedule.getTime();
	}

	public ArrayList<IntRect2D> getAllLocalBounds() throws RemoteException {
		return dSimState.getPartition().getAllBounds();
	}

	public static VisualizationProcessor getProcessor(final int pid) {
		VisualizationProcessor processor;
		if (processorCache.size() <= pid) {
			// Extend the dynamic array
			for (int i = processorCache.size(); i <= pid; i++)
				processorCache.add(null);
			processor = fetchAndUpdate(pid);
		} else {
			processor = processorCache.get(pid);
			if (processor == null)
				processor = fetchAndUpdate(pid);
		}
		return processor;
	}

	public static String getProcessorName(final int pid) {
		return NAME_PREFIX + pid;
	}

	private static VisualizationProcessor fetchAndUpdate(int pid) {
		try {
			VisualizationProcessor proc = DRegistry.getInstance().getObjectT(getProcessorName(pid));
			processorCache.set(pid, proc);
			return proc;
		} catch (RemoteException | NotBoundException e) {
			throw new RuntimeException(e);
		}
	}

	public int getAOI() throws RemoteException {
		return dSimState.getPartition().getAOI();
	}

	public int getProcessorLevels() throws RemoteException {
		return dSimState.getPartition().getTreeDepth();
	}

	public int[] getProcessorNeighborhood(int level) throws RemoteException {
		return dSimState.getPartition().getProcessorNeighborhood(level);
	}

	public ArrayList<Stat> getStatList() throws RemoteException {
		return dSimState.getStatList();
	}

	public void initStat() throws RemoteException {
		dSimState.recordStats = true;
	}

	public ArrayList<Stat> getDebugList() throws RemoteException {
		return dSimState.getDebugList();
	}

	public void initDebug() throws RemoteException {
		dSimState.recordDebug = true;
	}

//	// TODO: do we extend this to other Remote objects?
//	private Remote getRemote(final String key) {
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
