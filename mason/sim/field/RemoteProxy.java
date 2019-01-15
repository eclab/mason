package sim.field;

import java.util.UUID;
import java.util.ArrayList;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

import mpi.*;

import sim.field.DPartition;
import sim.field.HaloField;
import sim.util.MPIUtil;

public class RemoteProxy {

	RemoteField[] remoteFields;

	private static boolean isReady = false;
	private static int hostPort = -1;
	private static String hostAddr = null;
	private static Registry registry = null;
	private static ArrayList<RemoteField> exported = null;

	private static int getFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return -1;
	}

	public static void Init(int pid) {
		try {
			if (MPI.COMM_WORLD.getRank() == pid) {
				hostPort = getFreePort();
				hostAddr = InetAddress.getLocalHost().getHostAddress();
				registry = LocateRegistry.createRegistry(hostPort);
				// System.out.printf("Starting rmiregistry in %s on port %d\n", hostAddr, hostPort);
			}

			// Broadcast that LP's ip address/port to other LPs 
			// so they can connect to the registry later
			hostAddr = MPIUtil.<String>bcast(hostAddr, pid);
			hostPort = MPIUtil.<Integer>bcast(new Integer(hostPort), pid);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		exported = new ArrayList<RemoteField>();
		isReady = true;
	}

	// TODO hook this to MPI finalize so that this will be called before exit
	public static void Finalize() {
		isReady = false;

		try {
			for (RemoteField f : exported)
				UnicastRemoteObject.unexportObject(f, true);
			if (registry != null)
				UnicastRemoteObject.unexportObject(registry, true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public RemoteProxy(DPartition ps, RemoteField field) {
		if (!isReady)
			throw new IllegalArgumentException("RMI Registry has not been started yet!");

		String name = UUID.randomUUID().toString();
		
		try {
			// Create a RMI server and register it in the registry
			Registry reg = LocateRegistry.getRegistry(hostAddr, hostPort);
			reg.bind(name, (RemoteField)UnicastRemoteObject.exportObject(field, 0));

			// Exchange the names with all other LPs so that each LP can create RemoteField clients for all other LPs
			ArrayList<String> names = MPIUtil.<String>allGather(ps, name);
			remoteFields = new RemoteField[ps.np];
			for (int i = 0; i < ps.np; i++)
				remoteFields[i] = (RemoteField)reg.lookup(names.get(i));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// Keep record of all the exported object so that
		// they can be properly unexported when stopRMIRegistry() is called
		// to prevent stuck on exit.
		exported.add(field);
	}

	public RemoteField getField(int pid) {
		return remoteFields[pid];
	}

	public static void main(String[] args) throws MPIException {
		MPI.Init(args);

		Init(0);

		sim.field.DNonUniformPartition p = sim.field.DNonUniformPartition.getPartitionScheme(new int[] {10, 10}, false, new int[] {1, 1});
		p.initUniformly(null);
		p.commit();

		FakeRemoteField f = new FakeRemoteField(p.getPid() * 100000);
		RemoteProxy proxy = new RemoteProxy(p, f);

		sim.util.MPITest.execOnlyIn(0, x -> {
			try {
				for (int i = 0; i < p.getNumProc(); i++)
					System.out.printf("Value from Pid %d is %d\n", i, proxy.getField(i).getRMI(null));
			} catch (RemoteException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		});

		Finalize();

		MPI.Finalize();
	}

	static class FakeRemoteField implements RemoteField {
		int val;

		public FakeRemoteField(int val) {
			this.val = val;
		}

		public java.io.Serializable getRMI(sim.util.IntPoint p) throws RemoteException {
			return new Integer(val);
		}
	}
}