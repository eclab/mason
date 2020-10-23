package sim.engine;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import sim.engine.registry.DRegistry;
import sim.engine.transport.TransportRMIInterface;
import sim.field.partitioning.IntHyperRect;
import sim.field.proxy.VisualizationProcessor;
import sim.field.storage.GridStorage;

//public class RemoteProcessor extends UnicastRemoteObject implements VisualizationProcessor {
public class RemoteProcessor implements VisualizationProcessor {
	final DSimState dSimState;
	private final ReentrantLock lock = new ReentrantLock(true); // Fair lock
	public static final String NAME_PREFIX = "processorPId: ";
	public final String processorName;
//	final List<RemoteProcessor> processorCache;
//	final Map<String, Remote> cache = new HashMap<>();

	/**
	 * Creates a processor and registers it to the RMI Registry
	 * 
	 * @param dSimState
	 * @throws RemoteException
	 */
	public RemoteProcessor(DSimState dSimState) throws RemoteException {
		super();

		this.dSimState = dSimState;
		final int pid = DSimState.getPID();
		processorName = NAME_PREFIX + pid;

		try {
			if (!DRegistry.getInstance().registerObject(processorName, this))
				throw new RuntimeException("Failed to register processor: " + processorName);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}

//		processorCache = new ArrayList<>();
//		for (int i = 0; i < dSimState.getPartitioning().numProcessors; i++) {
//			if (i == pid)
//				processorCache.add(pid, this);
//			else
//				processorCache.add(null);
//		}
	}

	public void lock() throws RemoteException {
		lock.lock();
	}

	public void unlock() throws RemoteException {
		lock.unlock();
	}

	public IntHyperRect getBounds() throws RemoteException {
		return dSimState.getPartitioning().getBounds();
	}

	public GridStorage getStorage(int fieldId) throws RemoteException {
		return dSimState.fieldRegistry.get(fieldId).localStorage;
	}

	public TransportRMIInterface getTransportRMI(int fieldId) throws RemoteException {
		return (TransportRMIInterface) dSimState.fieldRegistry.get(fieldId);
	}

	public int getNumProcessors() throws RemoteException {
		return dSimState.getPartitioning().numProcessors;
	}

	public IntHyperRect getWorldBounds() throws RemoteException {
		return dSimState.getPartitioning().getWorldBounds();
	}

	public long getSteps() throws RemoteException {
		return dSimState.schedule.getSteps();
	}

	public double getTime() throws RemoteException {
		return dSimState.schedule.getTime();
	}

	public ArrayList<IntHyperRect> getAllLocalBounds() throws RemoteException {
		return dSimState.getPartitioning().getAllBounds();
	}

	public static VisualizationProcessor getProcessor(final int pid) {
		// TODO: what's better, ArrayList or HashMap?
//		return (RemoteProcessor) processorCache.get(pid);
//		return (RemoteProcessor) getRemote(NAME_PREFIX + pid);
		try {
			return (VisualizationProcessor) DRegistry.getInstance().getObject(NAME_PREFIX + pid);
		} catch (RemoteException | NotBoundException e) {
			throw new RuntimeException(e);
		}
	}
//
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
