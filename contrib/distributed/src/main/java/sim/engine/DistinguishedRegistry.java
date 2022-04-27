package sim.engine;

import java.net.InetAddress;
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
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import mpi.MPI;
import sim.util.*;

/**
	The utility class to register and update Distinguished objects on the RMI Registry.  This class
	allows you to register them, and also handled re-registering them with new DSimStates as the
	objects migrate from partition to partition.
**/


public class DistinguishedRegistry
{
	 static final long serialVersionUID = 1L;

	public static final int PORT = 5000;
	
	public static Logger logger;

	 static DistinguishedRegistry instance;

	 static int port;
	 static int rank;

	 static Registry registry;
	 static HashMap<String, Remote> exportedNames = new HashMap<>();
	 static HashMap<Remote, String> exportedObjects = new HashMap<>();
	 /* ID of the migrated objects */
	 static List<String> migratedNames = new ArrayList<String>();

	 static void initLocalLogger(final String loggerName)
	 {
		DistinguishedRegistry.logger = Logger.getLogger(DistinguishedRegistry.class.getName());
		DistinguishedRegistry.logger.setLevel(Level.ALL);
		DistinguishedRegistry.logger.setUseParentHandlers(false);

		final ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new java.util.logging.Formatter()
		{
			public synchronized String format(final LogRecord rec)
			{
				return String.format(loggerName + " [%s][%-7s] %s%n",
						new SimpleDateFormat("MM-dd-YYYY HH:mm:ss.SSS").format(new Date(rec.getMillis())),
						rec.getLevel().getLocalizedName(), rec.getMessage());
			}
		});
		DistinguishedRegistry.logger.addHandler(handler);
	}

	 DistinguishedRegistry() throws NumberFormatException, Exception
	 {
		if (instance != null)
		{
			throw new RuntimeException(
					"Use getInstance() method to get the single instance of the Distributed Registry.");
		}
		rank = MPI.COMM_WORLD.getRank();
		initLocalLogger(String.format("MPI-Job-%d", rank));

		// TODO: hard coding the port for now
		// port = getAvailablePort();
		port = PORT;
		String myip = InetAddress.getLocalHost().getHostAddress();

		if (rank == 0)
		{
			startLocalRegistry(myip, port);
		}

		final String masterData[] = MPIUtil.<String>bcast(myip + ":" + port, 0).split(":");

		if (rank != 0)
		{
			startLocalRegistry(masterData[0], Integer.parseInt(masterData[1]));
		}

		MPI.COMM_WORLD.barrier();

	}

	 void startLocalRegistry(String masterIP, int masterPort)
	 {

		try
		{
			registry = rank == 0 ? LocateRegistry.createRegistry(port)
					: LocateRegistry.getRegistry(masterIP, masterPort);
		}
		catch (RemoteException e1)
		{
			logger.log(Level.SEVERE, "Error Distributed Registry lookup for MPI node on master port: " + masterPort);
			e1.printStackTrace();
		}

		try
		{
			registry.list();
		}
		catch (AccessException e)
		{
			logger.log(Level.SEVERE, "Error Distributed Registry lookup for MPI node on master port: " + masterPort);
			e.printStackTrace();
		}
		catch (RemoteException e)
		{
			logger.log(Level.SEVERE, "Error Distributed Registry lookup for MPI node on master port: " + masterPort);
			e.printStackTrace();
		}
		logger.log(Level.INFO, "Distributed Registry created/obtained on MPI node on master port: " + port);
	}

	public static DistinguishedRegistry getInstance()
	{
		try
		{
			return instance = instance == null ? new DistinguishedRegistry() : instance;
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, "Error Distributed Registry started for MPI node.");
			return null;
		}
	}

/*
	 static Integer getAvailablePort() throws IOException {

		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}
*/

	/**
	 * Register an object obj with key name on the registry
	 * 
	 * @param obj 
	 * @param simstate
	 * 
	 * @return true if successful
	 * @throws AccessException
	 * @throws RemoteException
	 */
	public boolean registerObject(Distinguished obj, DSimState simstate) throws AccessException, RemoteException
	{
		String name = obj.distinguishedName();
		
		if (!exportedNames.containsKey(name))
		{
			try
			{
				DistinguishedRemoteObject remoteObj = new DistinguishedRemoteObject(obj, simstate);
				Remote stub = UnicastRemoteObject.exportObject(remoteObj, 0);
				
				
				registry.bind(name, stub);
				exportedNames.put(name, remoteObj);
				exportedObjects.put(remoteObj, name);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
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
	public boolean unregisterObject(Distinguished obj) throws AccessException, RemoteException, NotBoundException
	{
		String name = obj.distinguishedName();
		Remote remote = exportedNames.remove(name);
		if (remote != null)
		{
			registry.unbind(name);
			UnicastRemoteObject.unexportObject(remote, true);
			exportedObjects.remove(remote);
			return true;
		}
		return false;
	}
	
	public List<DistinguishedRemoteObject> getAllLocalExportedObjects(){
		List<DistinguishedRemoteObject> tor = new ArrayList<DistinguishedRemoteObject>();
		
		for (Remote obj : exportedNames.values()) {
			if (obj instanceof DistinguishedRemoteObject){
				tor.add((DistinguishedRemoteObject) obj);
			}
		}
		return tor;
	}
	
	/**
	 * Register an already exported UnicastRemoteObject obj with key name on the
	 * registry
	 * 
	 * @param name
	 * @param obj
	 * 
	 * @return true if successful
	 * @throws AccessException
	 * @throws RemoteException
	 */
	public boolean registerObject(String name, UnicastRemoteObject obj) throws AccessException, RemoteException
	{
		if (!exportedNames.containsKey(name))
		{
			try
			{
				Remote stub = UnicastRemoteObject.toStub(obj);
				registry.bind(name, stub);
				exportedNames.put(name, obj);
				exportedObjects.put(obj, name);
				
			}
			catch (AlreadyBoundException e)
			{
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;

	}

	/**
	 * @param name
	 * 
	 * @return the object with key name from the registry.
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public Remote getObject(String name) throws AccessException, RemoteException, NotBoundException
	{
		return registry.lookup(name);
	}
	/**
	 * This method unchecked casts the return Remote Object to type T. <br>
	 * To ensure type safety make sure that the Object bound to the give "name" is
	 * of type T.
	 * 
	 * @param <T>  Type of Object to be returned
	 * @param name
	 * 
	 * @return Remote Object bound to "name" cast to Type T
	 * 
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Remote> T getObjectT(String name) throws AccessException, RemoteException, NotBoundException
	{
		return (T) registry.lookup(name);
	}
	
	/**
	 * @param agent
	 * @return True if the object agent is migrated
	 */
	boolean isMigrated(Distinguished agent)
	{
		return migratedNames.contains(agent.distinguishedName());
	}
	/**
	 * Clear the list of the registered agent’s keys on the registry
	 */
	void clearMigratedNames()
	{
		migratedNames.clear();
	}

	/**
	 * If Object obj isExported is True then, add its name to migrated names and
	 * return the name. Returns null if isExported is False.
	 * 
	 * @param obj
	 * @return Object name if isExported is True, null otherwise.
	 */
	public String ifExportedThenAddMigratedName(Distinguished obj)
	{
		String name = obj.distinguishedName();
		Remote remote = exportedNames.get(name);
		if (remote != null)
			migratedNames.add(name);
		return name;
	}

}
