package sim.engine;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import sim.engine.registry.DRegistry;
import sim.field.partitioning.IntHyperRect;
import sim.field.proxy.VisualizationProcessor;
import sim.field.storage.GridStorage;

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
	 */
	public RemoteProcessor(DSimState dSimState) {
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

	public GridStorage getStorage(int storageId) throws RemoteException {
		return dSimState.fieldRegistry.get(storageId).localStorage;
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

//	public RemoteProcessor getRemoteProcessor(final int pid) {
//		// TODO: what's better, ArrayList or HashMap?
////		return (RemoteProcessor) processorCache.get(pid);
//		return (RemoteProcessor) getRemote(NAME_PREFIX + pid);
//	}
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
