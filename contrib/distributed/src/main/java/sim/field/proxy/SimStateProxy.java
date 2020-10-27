package sim.field.proxy;
import sim.engine.*;
import sim.field.*;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.util.*;

/**
	A subclass of SimState designed to visualize remote distributed models.
	
	You set up this SimState something like this:
	
	<pre>
	
	public class MySimStateProxy extends SimStateProxy
		{
		public MySimStateProxy()
			{
			setRegistryHost("my.host.org");
			setRegistryPort(21242);
			}
		
		DoubleGrid2DProxy heat = new DoubleGrid2DProxy(1,1);	// width and height don't matter, they'll be changed
		DenseGrid2DProxy bugs = new DenseGrid2DProxy(1,1);		// width and height don't matter, they'll be changed
		
		public void start()
			{
			super.start();
			registerFieldProxy(heat);
			registerFieldProxy(bugs);
			}
		}
	
	</pre>

	... then you'd set up MASON visualization for these two fields as usual.

*/

public class SimStateProxy extends SimState
	{
	String host = "localhost";
	/** Returns the IP address of the distributed RMI registry.  You need to set this before start() is called. */
	public String registryHost() { return host; }
	/** Sets the IP address of the distributed RMI registry.  You need to set this before start() is called. */
	public void setRegistryHost(String host) { this.host = host; }
	
	int port = 5000;
	/** Returns the IP address of the distributed RMI registry.  You need to set this before start() is called. */
	public int registryPort() { return port; }
	/** Sets the IP address of the distributed RMI registry.  You need to set this before start() is called. */
	public void setRegistryPort(int port) { this.port = port; }
	
	/** Returns the string by which the visualization root (a VisualizationRoot instance) is registered with the Registry. */
	public final String visualizationRootString() { return visualizationProcessorString(visualizationRootPId()); }						
	
	/** Returns the pid by which the visualization root (a VisualizationRoot instance) is registered with the Registry. */
	public final int visualizationRootPId() { return 0; }						
	
	/** Returns the string by which a given visualization processor (a VisualizationProcessor instance) is registered with the Registry. */
	public final String visualizationProcessorString(int pid) { return RemoteProcessor.getProcessorName(pid); }		
	
	public static final int SLEEP = 25;	// ms
	public long refresh = 0;
	public static final int DEFAULT_RATE = 1000;
	public int rate = DEFAULT_RATE;
	/** Returns the update rate in ms */
	public int rate() { return rate; }
	/** Sets the update rate in ms */
	public void setRate(int val) { rate = val; }
	public long lastSteps = -1;
	
	// The registry proper
	Registry registry = null;
	// World bounds
	IntRect2D worldBounds = null;
	// The visualization root
	VisualizationProcessor visualizationRoot = null;
	// a cache of Visualization Processors so we don't keep querying for them
	VisualizationProcessor[] visualizationCache = null;
	// The number of pids.
	int numProcessors = 0;
	// which processor are we currently visualizing?
	int processor = 0;
	// The SimState's fields (on the MASON side), all field proxies.
	// These need to be in the same order as the order associated with the remote grids
	ArrayList<UpdatableProxy> fields = new ArrayList<UpdatableProxy>();
	ArrayList<Integer> indices = new ArrayList<Integer>();
	
	/** Registers a field proxy with the SimState.  Each timestep or whatnot the proxy will get updated,
		which causes it to go out and load information remotely.  The order in which the fields are registered
		must be the same as the order associated with the remote grids' storage objects returned by the VisualizationProcessor. */
	public void registerFieldProxy(UpdatableProxy proxy, int index)
		{
		fields.add(proxy);
		indices.add(index);
		}
	
	/** Returns the registry */
	public Registry registry()
		{
		return registry;
		}
	
	/** Returns the number of processors */
	public int numProcessors() { return numProcessors; }

	/** Sets the current processor to be visualized */
	public void setCurrentProcessor(int pid)
		{
		if (pid < 0 || pid > numProcessors) return;
		processor = pid;
		}
		
	/** Sets the current processor to be visualized */
	public int currentProcessor() { return processor; }
	
	/** Returns the current Visualization Processor either cached or by fetching it remotely. */
	public VisualizationProcessor visualizationProcessor() throws RemoteException, NotBoundException
		{
//		return RemoteProcessor.getProcessor(processor);
		
//		System.err.println("visualizationProcessor()");
		if (visualizationCache[processor] == null)
			{
//			System.err.println("visualizationProcessor() -> registry lookup");
			visualizationCache[processor] = (VisualizationProcessor)(registry.lookup(visualizationProcessorString(processor)));
			}
//		System.err.println("-visualizationProcessor()");
		return visualizationCache[processor];
		}
		
	/** Fetches the requested storage from the current Visualization Processor. */
	public GridStorage storage(int storage) throws RemoteException, NotBoundException
		{
		return visualizationProcessor().getStorage(storage);
		}
		
	/** Fetches the halo bounds from the current Visualization Processor. */
	public IntRect2D bounds() throws RemoteException, NotBoundException

		{
		System.err.println(visualizationProcessor().getStorageBounds());
		return visualizationProcessor().getStorageBounds();
		}
		
	public void start()
		{
//			System.err.println("start()");
		super.start();
		lastSteps = -1;
		refresh = System.currentTimeMillis();
		
		try
			{
//			System.err.println("start()->getRegistry()");
			// grab the registry and query it for basic information
			registry = LocateRegistry.getRegistry(registryHost(), registryPort());
//			System.err.println("start()->registry root lookup");
//			visualizationRoot = RemoteProcessor.getProcessor(visualizationRootPId());
			visualizationRoot = (VisualizationProcessor)(registry.lookup(visualizationRootString()));
//			System.err.println("start()->registry get world bounds");
			worldBounds = visualizationRoot.getWorldBounds();
//			System.err.println("start()->registry get num processors");
			numProcessors = visualizationRoot.getNumProcessors();
			
			// set up the cache
			visualizationCache = new VisualizationProcessor[numProcessors];

			// set up the field proxies to be updated.  We may wish to change the rate at which they're updated, dunno
			schedule.scheduleRepeating(new Steppable()
				{
				public void step(SimState state)
					{
					// First we sleep a little bit so we don't just constantly poll
					try
						{
						Thread.currentThread().sleep(SLEEP);
						}
					catch (InterruptedException ex)
						{
						ex.printStackTrace();
						}
					
					// Next we check to see if enough time has elapsed to bother querying the remote processor
					long cur = System.currentTimeMillis();
					if (cur - refresh >= DEFAULT_RATE)
						{
						refresh = cur;
						try
							{
//			System.err.println("step()");
							// Now we query the remote processor to see if a new step has elapsed
							VisualizationProcessor vp = visualizationProcessor();
//			System.err.println("vp->getSteps()");
							long steps = vp.getSteps();
							if (steps > lastSteps)
								{
								
								// Okay it's worth updating, so let's grab the data
//			System.err.println("lock()");
								vp.lock();
//			System.err.println("-lock()");
								for(int i = 0; i < fields.size(); i++)
									{
//			System.err.println("update field " + i);
									fields.get(i).update(SimStateProxy.this, indices.get(i));
//			System.err.println("-update field " + i);
									}
//			System.err.println("unlock()");
								vp.unlock();
//			System.err.println("-unlock()");
								}
							lastSteps = steps;
							}
						catch (RemoteException | NotBoundException ex)
							{
							ex.printStackTrace();
							}
						}
					}
				});
			}
		catch (RemoteException ex)
			{
			ex.printStackTrace();
			// we're done
			}	
		catch (NotBoundException ex)
			{
			ex.printStackTrace();
			}	
			System.err.println("-start()");
		}
		
	public SimStateProxy(long seed)
		{
		super(seed);
		}

    public boolean isRemoteProxy()
    	{
    	return true;
    	}
    	
    public long remoteSteps()
    	{
    	try
    		{
	    	return visualizationProcessor().getSteps();
	    	}
	    catch (RemoteException | NotBoundException ex)
	    	{
	    	ex.printStackTrace();
		    return 0;
	    	}
    	}
    
    public double remoteTime()
    	{
    	try
    		{
	    	return visualizationProcessor().getTime();
	    	}
	    catch (RemoteException | NotBoundException ex)
	    	{
	    	ex.printStackTrace();
		    return Schedule.BEFORE_SIMULATION;
	    	}
    	}
    
	}
	