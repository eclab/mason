package sim.field;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.UUID;

import mpi.MPI;
import sim.util.MPIUtil;
import sim.util.NdPoint;

public class RemoteProxy<T extends Serializable, P extends NdPoint> {

	ArrayList<RemoteField<T, P>> remoteFields;

	private static boolean isReady = false;
	private static int hostPort = -1;
	private static String hostAddr = null;
	private static Registry registry = null;
	private static ArrayList<RemoteField<? extends Serializable, ? extends NdPoint>> exported = null;

//	private static Integer rootHostPort;
//	private static String rootHostAddr;

	private static int getFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return -1;
	}

	public static void Init() {
		try {
			RemoteProxy.hostPort = getFreePort();
			RemoteProxy.hostAddr = InetAddress.getLocalHost().getHostAddress();
			RemoteProxy.registry = LocateRegistry.createRegistry(RemoteProxy.hostPort);

			System.out.printf("Starting rmiregistry in %s on port %d\n", RemoteProxy.hostAddr, RemoteProxy.hostPort);

			// Creating a barrier to ensure that all LP's are initialized
			// before isReady is true
			MPI.COMM_WORLD.barrier();
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		RemoteProxy.exported = new ArrayList<>();
		RemoteProxy.isReady = true;
	}

	// TODO hook this to MPI finalize so that this will be called before exit
	public static void Finalize() {
		RemoteProxy.isReady = false;

		try {
			for (final RemoteField<? extends Serializable, ? extends NdPoint> f : RemoteProxy.exported)
				UnicastRemoteObject.unexportObject(f, true);
			if (RemoteProxy.registry != null)
				UnicastRemoteObject.unexportObject(RemoteProxy.registry, true);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@SuppressWarnings("unchecked")
	public RemoteProxy(final DPartition ps, final RemoteField<? extends Serializable, ? extends NdPoint> field) {
		if (!RemoteProxy.isReady)
			throw new IllegalArgumentException("RMI Registry has not been started yet!");

		final String name = UUID.randomUUID().toString();

		// We're creating a registry per LP. For each LP we are then calling allGather
		// so that we can exchange the remote fields for between all LP's
		try {
			// Create a RMI server and register it in the registry
			RemoteProxy.registry.bind(name, UnicastRemoteObject.exportObject(field, 0));

			// Performance should not be a big concern below since we are only calling this
			// method once per field

			// Exchange the hosts with all other LPs so that each LP can create RemoteField
			// clients for all other LPs
			final ArrayList<String> hosts = MPIUtil.<String>allGather(ps,
					name + ":" + RemoteProxy.hostAddr + ":" + RemoteProxy.hostPort);

			remoteFields = new ArrayList<>(ps.numProcessors);

			for (final String host : hosts) {
				final String[] name_host_Port = host.split(":");
				final Registry remoteReg = LocateRegistry.getRegistry(name_host_Port[1],
						Integer.parseInt(name_host_Port[2]));
				remoteFields.add((RemoteField<T, P>) remoteReg.lookup(name_host_Port[0]));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// Keep record of all the exported object so that
		// they can be properly unexported when stopRMIRegistry() is called
		// to prevent stuck on exit.
		RemoteProxy.exported.add(field);
	}

	public RemoteField<T, P> getField(final int pid) {
		return remoteFields.get(pid);
	}

	/*
	 * public static void main(String[] args) throws MPIException { MPI.Init(args);
	 *
	 * Init(0);
	 *
	 * sim.field.DNonUniformPartition p =
	 * sim.field.DNonUniformPartition.getPartitionScheme(new int[] {10, 10}, false,
	 * new int[] {1, 1}); p.initUniformly(null); p.commit();
	 *
	 * FakeRemoteField f = new FakeRemoteField(p.getPid() * 100000); RemoteProxy
	 * proxy = new RemoteProxy(p, f);
	 *
	 * sim.util.MPITest.execOnlyIn(0, x -> { try { for (int i = 0; i <
	 * p.getNumProc(); i++) System.out.printf("Value from Pid %d is %d\n", i,
	 * proxy.getField(i).getRMI(null)); } catch (RemoteException e) {
	 * e.printStackTrace(); System.exit(-1); } });
	 *
	 * Finalize();
	 *
	 * MPI.Finalize(); }
	 */

//	static class FakeRemoteField<P extends NdPoint> implements RemoteField<P> {
//		int val;
//
//		public FakeRemoteField(final int val) {
//			this.val = val;
//		}
//
//		public java.io.Serializable getRMI(final P p) throws RemoteException {
//			return new Integer(val);
//		}
//	}
}
