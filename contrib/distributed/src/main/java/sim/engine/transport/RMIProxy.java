package sim.engine.transport;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.UUID;

import mpi.MPI;
import sim.field.partitioning.PartitionInterface;
import sim.util.MPIUtil;
import sim.util.*;

/**
 * RMIProxy for using Java RMI. RMI is used for point to point communication
 * between nodes that are not neighbors.
 *
 * @param <P> The Type of PointND to use
 * @param <T> The Type of Object in the field
 */
public class RMIProxy<T extends Serializable, P> {

	ArrayList<TransportRMIInterface<T, P>> remoteFields;

	private static boolean isReady = false;
	private static int hostPort = -1;
	private static String hostAddr = null;
	private static Registry registry = null;
	private static ArrayList<Remote> exported = null;

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

	public static void init() {
		try {
			RMIProxy.hostPort = getFreePort();
			RMIProxy.hostAddr = InetAddress.getLocalHost().getHostAddress();
			RMIProxy.registry = LocateRegistry.createRegistry(RMIProxy.hostPort);

			System.out.printf("Starting rmiregistry in %s on port %d\n", RMIProxy.hostAddr, RMIProxy.hostPort);

			// Creating a barrier to ensure that all LP's are initialized
			// before isReady is true
			MPI.COMM_WORLD.barrier();
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		RMIProxy.exported = new ArrayList<>();
		RMIProxy.isReady = true;
	}

	// TODO hook this to MPI finalize so that this will be called before exit
	public static void terminate() {
		RMIProxy.isReady = false;

		try {
			for (final Remote f : RMIProxy.exported)
				UnicastRemoteObject.unexportObject(f, true);
			if (RMIProxy.registry != null)
				UnicastRemoteObject.unexportObject(RMIProxy.registry, true);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@SuppressWarnings("unchecked")
	public RMIProxy(final PartitionInterface ps, Remote field) {
		if (!RMIProxy.isReady)
			throw new IllegalArgumentException("RMI Registry has not been started yet!");

		final String name = UUID.randomUUID().toString();

		// We're creating a registry per LP. For each LP we are then calling allGather
		// so that we can exchange the remote fields for between all LP's
		try {
			// Create a RMI server and register it in the registry
			RMIProxy.registry.bind(name, UnicastRemoteObject.exportObject(field, 0));

			// Performance should not be a big concern below since we are only calling this
			// method once per field

			// Exchange the hosts with all other LPs so that each LP can create RemoteField
			// clients for all other LPs
			final ArrayList<String> hosts = MPIUtil.<String>allGather(ps,
					name + ":" + RMIProxy.hostAddr + ":" + RMIProxy.hostPort);

			remoteFields = new ArrayList<>(ps.numProcessors);

			for (final String host : hosts) {

				final String[] name_host_Port = host.split(":");

				final Registry remoteReg = LocateRegistry.getRegistry(name_host_Port[1],
						Integer.parseInt(name_host_Port[2]));

				remoteFields.add((TransportRMIInterface<T, P>) remoteReg.lookup(name_host_Port[0]));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// Keep record of all the exported object so that
		// they can be properly unexported when stopRMIRegistry() is called
		// to prevent stuck on exit.
		RMIProxy.exported.add(field);
	}

	public TransportRMIInterface<T, P> getField(final int pid) {
		return remoteFields.get(pid);
	}

}
