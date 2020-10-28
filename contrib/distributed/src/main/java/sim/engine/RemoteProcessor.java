package sim.engine;

import java.rmi.NotBoundException;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import sim.engine.registry.DRegistry;
import sim.field.partitioning.IntRect2D;
import sim.engine.transport.TransportRMIInterface;
//import sim.field.partitioning.IntHyperRect;
import sim.field.proxy.VisualizationProcessor;
import sim.field.storage.GridStorage;

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
		return dSimState.getPartitioning().getHaloBounds();
	}

	public GridStorage getStorage(int fieldId) throws RemoteException {
		return dSimState.fieldRegistry.get(fieldId).localStorage;
	}

	public TransportRMIInterface getTransportRMI(int fieldId) throws RemoteException {
		return dSimState.fieldRegistry.get(fieldId);
	}

	public int getNumProcessors() throws RemoteException {
		return dSimState.getPartitioning().numProcessors;
	}

	public IntRect2D getWorldBounds() throws RemoteException {
		return dSimState.getPartitioning().getWorldBounds();
	}

	public long getSteps() throws RemoteException {
		return dSimState.schedule.getSteps();
	}

	public double getTime() throws RemoteException {
		return dSimState.schedule.getTime();
	}

	public ArrayList<IntRect2D> getAllLocalBounds() throws RemoteException {
		return dSimState.getPartitioning().getAllBounds();
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
			processorCache.add(pid, proc);
			return proc;
		} catch (RemoteException | NotBoundException e) {
			throw new RuntimeException(e);
		}
	}

	public int getAOI() throws RemoteException {
		// TODO: aoi is multi dimentional
		return dSimState.partition.aoi[0];
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
