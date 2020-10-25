package sim.engine;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.locks.ReentrantLock;

import sim.engine.registry.DRegistry;
import sim.field.partitioning.IntRect2D;
import sim.field.proxy.VisualizationProcessor;
import sim.field.storage.GridStorage;

public class RemoteProcessor implements VisualizationProcessor {
	final DSimState dSimState;
	private final ReentrantLock lock = new ReentrantLock(true); // Fair lock
	public static final String NAME_PREFIX = "processor_pid: ";
	public final String name;

	/**
	 * Creates a processor and registers it to the RMI Registry
	 * 
	 * @param dSimState
	 */
	public RemoteProcessor(DSimState dSimState) {
		super();

		this.dSimState = dSimState;
		name = NAME_PREFIX + DSimState.getPID();

		try {
			if (!DRegistry.getInstance().registerObject(name, this))
				throw new RuntimeException("Failed to register processor: " + name);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void lock() throws RemoteException {
		lock.lock();
	}

	public void unlock() throws RemoteException {
		lock.unlock();
	}

	public IntRect2D getBounds() throws RemoteException {
		return dSimState.getPartitioning().getBounds();
	}

	public GridStorage getStorage(int storageId) throws RemoteException {
		return dSimState.fieldRegistry.get(storageId).localStorage;
	}

	public int getNumProcessors() throws RemoteException {
		return dSimState.getPartitioning().numProcessors;
	}

	public IntRect2D getWorldBounds() throws RemoteException {
		return dSimState.getPartitioning().getWorldBounds();
	}

	public static RemoteProcessor getRemoteProcessor(final int pid) {
		try {
			return (RemoteProcessor) DRegistry.getInstance().getObject(NAME_PREFIX + pid);
		} catch (RemoteException | NotBoundException e) {
			throw new RuntimeException(e);
		}
	}
}
