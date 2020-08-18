package sim.engine.registry;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import mpi.MPI;
import sim.util.MPIUtil;

/**
 * This class enables agents access to the information of another agent in any
 * position in the field. By using this class an agent can be visible to
 * everyone else by registering on it. Any agent interested in the information
 * of another agent can perform a lookup operation on this register, obtain the
 * reference to that agent and invoke a method on that agent in order to obtain
 * the information he wishes.
 */
public class DRegistry {
	public static Logger logger;

	private static DRegistry instance;

	private static int port;
	private static int rank;

	private static Registry registry;
	private static HashMap<String, Object> exported_names = new HashMap<String, Object>();
	private static HashMap<Object, String> exported_objects = new HashMap<Object, String>();
	private ArrayList<String> migrated_names = new ArrayList<String>();

	/**
	 * Clear the list of the registered agent’s keys on the registry
	 */
	public void clearMigratedNames() {
		migrated_names.clear();
	}

	/**
	 * @return the List of the agent’s keys on the registry.
	 */
	public ArrayList<String> getMigratedNames() {
		return migrated_names;
	}

	public void addMigratedName(Object obj) {
		migrated_names.add(exported_objects.get(obj));
	}

	private static void initLocalLogger(final String loggerName) {
		DRegistry.logger = Logger.getLogger(DRegistry.class.getName());
		DRegistry.logger.setLevel(Level.ALL);
		DRegistry.logger.setUseParentHandlers(false);

		final ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new java.util.logging.Formatter() {
			public synchronized String format(final LogRecord rec) {
				return String.format(loggerName + " [%s][%-7s] %s%n",
						new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
						rec.getLevel().getLocalizedName(), rec.getMessage());
			}
		});
		DRegistry.logger.addHandler(handler);
	}

	private DRegistry() throws NumberFormatException, Exception {

		if (instance != null) {
			throw new RuntimeException(
					"Use getInstance() method to get the single instance of the Distributed Registry.");
		}
		rank = MPI.COMM_WORLD.getRank();
		initLocalLogger(String.format("MPI-Job-%d", rank));

		port = getAvailablePort();
		String myip = InetAddress.getLocalHost().getHostAddress();

		if (rank == 0) {
			startLocalRegistry(myip, port);
		}

		final String master_data[] = MPIUtil.<String>bcast(myip + ":" + port, 0).split(":");

		if (rank != 0) {

			startLocalRegistry(master_data[0], Integer.parseInt(master_data[1]));
		}

		MPI.COMM_WORLD.barrier();

	}

	private void startLocalRegistry(String master_ip, int master_port) {

		try {
			registry = rank == 0 ? LocateRegistry.createRegistry(port)
					: LocateRegistry.getRegistry(master_ip, master_port);
		} catch (RemoteException e1) {
			logger.log(Level.SEVERE, "Error Distributed Registry lookup for MPI node on master port: " + master_port);
			e1.printStackTrace();
		}

		try {
			registry.list();
		} catch (AccessException e) {
			logger.log(Level.SEVERE, "Error Distributed Registry lookup for MPI node on master port: " + master_port);
			e.printStackTrace();
		} catch (RemoteException e) {
			logger.log(Level.SEVERE, "Error Distributed Registry lookup for MPI node on master port: " + master_port);
			e.printStackTrace();
		}
		logger.log(Level.INFO, "Distributed Registry created/obtained on MPI node on master port: " + port);
	}

	public static DRegistry getInstance() {
		try {
			return instance = instance == null ? new DRegistry() : instance;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error Distributed Registry started for MPI node.");

			return null;
		}
	}

	private static Integer getAvailablePort() throws IOException {

		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}

	/**
	 * Register an object obj with kay name on the registry
	 * 
	 * @param name
	 * @param obj
	 * 
	 * @return true if successful
	 * @throws AccessException
	 * @throws RemoteException
	 */
	public boolean registerObject(String name, Remote obj) throws AccessException, RemoteException {
		if (!exported_names.containsKey(name)) {
			try {
				Remote stub = UnicastRemoteObject.exportObject(obj, 0);
				registry.bind(name, stub);
				exported_names.put(name, obj);
				exported_objects.put(obj, name);
			} catch (AlreadyBoundException e) {
				return false;
			}
			return true;
		}
		return false;

	}

	public String getLocalExportedName(Object obj) {
		return exported_objects.get(obj);
	}

	/**
	 * @param name
	 * 
	 * @return the object with key name from the registry.
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public Object getObject(String name) throws AccessException, RemoteException, NotBoundException {
		return registry.lookup(name);
	}

	/**
	 * Remove the object with key name from the registry
	 * 
	 * @param name
	 * 
	 * @return true if successful
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public boolean unRegisterObject(String name) throws AccessException, RemoteException, NotBoundException {

		if (exported_names.containsKey(name)) {
			registry.unbind(name);
			Object obj = exported_names.remove(name);
			UnicastRemoteObject.unexportObject((Remote) obj, true);
			exported_objects.remove(obj);
			return true;
		}
		return false;
	}

	/**
	 * @param agent
	 * @return rue if the object agent is reigested on the registry.
	 */
	public boolean isExported(Object agent) {
		return exported_objects.containsKey(agent);
	}

}
