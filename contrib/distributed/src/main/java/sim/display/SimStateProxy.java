package sim.display;

import java.rmi.NotBoundException;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.rmi.RemoteProcessor;
import sim.field.partitioning.QuadTreeNode;
import sim.field.storage.GridStorage;
import sim.util.IntRect2D;
import sim.util.Properties;
import sim.util.RemoteSimpleProperties;

import sim.engine.*;

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
	private static final long serialVersionUID = 1L;

	String host = "localhost";
	
	/** Returns the IP address of the distributed RMI registry.  You need to set this before start() is called. */
	public String registryHost()
		{
		return host;
		}
	
	/** Sets the IP address of the distributed RMI registry.  You need to set this before start() is called. */
	public void setRegistryHost(String host)
		{
		this.host = host;
		}
	
	int port = DistinguishedRegistry.PORT;
	
	/** Returns the IP address of the distributed RMI registry.  You need to set this before start() is called. */
	public int registryPort()
		{
		return port; 
		}
	
	/** Sets the IP address of the distributed RMI registry.  You need to set this before start() is called. */
	public void setRegistryPort(int port)
		{
		this.port = port;
		}
	
	/** Returns the string by which the visualization root (a VisualizationRoot instance) is registered with the Registry. */
	public final String visualizationRootString()
		{
		return visualizationProcessorString(visualizationRootPID());
		}						
	
	/** Returns the pid by which the visualization root (a VisualizationRoot instance) is registered with the Registry. */
	public final int visualizationRootPID()
		{
		return 0;
		}						
	
	/** Returns the string by which a given visualization processor (a VisualizationProcessor instance) is registered with the Registry. */
	public final String visualizationProcessorString(int pid)
		{
		return RemoteProcessor.getProcessorName(pid);
		}		
	
	public static final int DEFAULT_SLEEP = 25;	// ms
	public static final int DEFAULT_PAUSE_INTERVAL = 1000;	// ms
	
	public long refresh = 0;
	protected int sleep = DEFAULT_SLEEP;
	protected int pauseInterval = DEFAULT_PAUSE_INTERVAL;
	/** Returns the update rate in ms */
	public int pauseInterval()
		{
		return pauseInterval;
		}
	
	/** Sets the update rate in ms */
	public void setPauseInterval(int val)
		{
		pauseInterval = val;
		}
	
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
	
	// The visualization overview
	Overview overview = null;
	
	int[] chosenNodePartitionList = {0}; //list of partitions based on chosen node of the tree
	
	ArrayList<Stat> statLists[][];
	
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
	public int numProcessors()
		{
		return numProcessors;
		}

	/** Sets the current processor to be visualized */
	public void setCurrentProcessor(int pid)
		{
		if (pid < 0 || pid > numProcessors) return;
		processor = pid;

		}
		
	/** Returns the current processor to be visualized */
	public int currentProcessor()
		{
		return processor;
		}
	
	/** Sets the current processor and returns Visualization Processor. */
	public VisualizationProcessor visualizationProcessor(int pid) throws RemoteException, NotBoundException
	{
		if (pid < 0 || pid > numProcessors) throw new IllegalArgumentException(pid+"");
		setCurrentProcessor(pid);
		return visualizationProcessor();
	}
	
	/** Returns the current Visualization Processor either cached or by fetching it remotely. */
	public VisualizationProcessor visualizationProcessor() throws RemoteException, NotBoundException
		{
		if (visualizationCache[processor] == null)
			{
			visualizationCache[processor] = (VisualizationProcessor)(registry.lookup(visualizationProcessorString(processor)));
			}
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
		return visualizationProcessor().getStorageBounds();
		}
	
	public void finish()
		{
		try
			{
			VisualizationProcessor vp = visualizationProcessor(0);							
			vp.lock();
			for(int proc = 0; proc < numProcessors; proc++)
				{
				VisualizationProcessor sv = visualizationProcessor(proc);
				for(int s = 0; s < VisualizationProcessor.NUM_STAT_TYPES; s++)
					{
					sv.stopStats(s);
					}					
				}
			vp.unlock();
			}
		catch (RemoteException ex)
			{
			ex.printStackTrace();
			}
		catch (NotBoundException ex)
			{
			ex.printStackTrace();
			}
		}
		
	public void start()
		{
		super.start();
		lastSteps = -1;
		refresh = System.currentTimeMillis();
		
		try
			{
			// grab the registry and query it for basic information
			registry = LocateRegistry.getRegistry(registryHost(), registryPort());
			visualizationRoot = (VisualizationProcessor)(registry.lookup(visualizationRootString()));
			worldBounds = visualizationRoot.getWorldBounds();
			numProcessors = visualizationRoot.getNumProcessors();
			
			// set up the cache
			visualizationCache = new VisualizationProcessor[numProcessors];
			
 			statLists = new ArrayList[VisualizationProcessor.NUM_STAT_TYPES][numProcessors];
 
 			for(int i = 0; i < statLists.length; i++)
 				for(int j = 0; j < statLists[i].length; j++)
 					{
 					statLists[i][j] = new ArrayList<>();
 					}

		try
			{
			VisualizationProcessor vp = visualizationProcessor(0);	
			vp.lock();						
			for(int proc = 0; proc < numProcessors; proc++)
				{
				VisualizationProcessor sv = visualizationProcessor(proc);
				for(int s = 0; s < VisualizationProcessor.NUM_STAT_TYPES; s++)
					{
					sv.startStats(s);
					}					
				}
			vp.unlock();
			}
		catch (RemoteException ex)
			{
			ex.printStackTrace();
			}
		catch (NotBoundException ex)
			{
			ex.printStackTrace();
			}



			// set up the field proxies to be updated.  We may wish to change the rate at which they're updated, dunno
			schedule.scheduleRepeating(new Steppable()
				{
				public void step(SimState state)
					{
					System.out.println("stepping...");
					// First we sleep a little bit so we don't just constantly poll
					try
						{
						Thread.sleep(sleep);
						}
					catch (InterruptedException ex)
						{
						ex.printStackTrace();
						}
					
					// Next we check to see if enough time has elapsed to bother querying the remote processor
					long currentTime = System.currentTimeMillis();
					if (currentTime - refresh >= pauseInterval)
						{
						refresh = currentTime;
						try
							{
							// Now we query the remote processor to see if a new step has elapsed
							//VisualizationProcessor vp = visualizationProcessor(); 
							VisualizationProcessor vp = visualizationProcessor(0);							
							if (overview != null)
								{
								overview.update(vp.getAllLocalBounds());
								}
							
							long steps = vp.getSteps();
							if (steps > lastSteps)
								{
								// Okay it's worth updating, so let's grab the data
								vp.lock();

								//(I did this in the update method)								
								//for loop: for each processor
								   //get processorbounds
								   //determine offsets
								//get union of bounds (this is what we report to reshapeAndClear()
								
								for(int i = 0; i < fields.size(); i++)
									{
									//reshapeAndClear()
									fields.get(i).update(SimStateProxy.this, indices.get(i), chosenNodePartitionList);
									}
								
								//I did this in the update method
								//for each field
								   //for each processor
								      //change processor
								        //update field with appropiate offset

								// Grab all the statistics and debug information
								for(int proc = 0; proc < numProcessors; proc++)
									{
									VisualizationProcessor sv = visualizationProcessor(proc);
									for(int s = 0; s < VisualizationProcessor.NUM_STAT_TYPES; s++)
										{
										statLists[s][proc].addAll(sv.getStats(s));
										}					
									}
								vp.unlock();
								lastSteps = steps;
								}
							}
						catch (RemoteException | NotBoundException ex)
							{
							ex.printStackTrace();
							}
						}
						
						
					// Process the statistics lists
					for(int type = 0; type < VisualizationProcessor.NUM_STAT_TYPES; type++)
						{
						long startSteps = Long.MAX_VALUE;		// way more than is feasible
						long endSteps = -1;						// smaller than the minimum step
						
						// determine timesteps
						for(int proc = 0; proc < numProcessors; proc++)
							{
							ArrayList<Stat> processorStats = statLists[type][proc];
							for(Stat stat : processorStats)
								{
								if (stat.steps < startSteps) startSteps = stat.steps;
								if (stat.steps > endSteps) endSteps = stat.steps;
								}
							}
						
						// we now know our start and end steps.  Do we have any statistics at all?
						if (endSteps != -1)	// we have stats!
							{	
							
							
							// Build array of [proc][timestep]
							ArrayList<Stat>[][] stats = new ArrayList[numProcessors][(int)(endSteps - startSteps + 1)];
							double[] times = new double[(int)(endSteps - startSteps + 1)];
							for(int proc = 0; proc < numProcessors; proc++)
								{
								for(int t = 0; t < stats[proc].length; t++)
									{
									stats[proc][t] = new ArrayList<Stat>();
									}
								}
								
							// Load the array
							for(int proc = 0; proc < numProcessors; proc++)
								{
								ArrayList<Stat> processorStats = statLists[type][proc];
								for(Stat stat : processorStats)
									{
									stats[proc][(int)(stat.steps - startSteps)].add(stat);
									times[(int)(stat.steps - startSteps)] = stat.time;		// yes, this is redundant
									}
								}

							// Reset the stat lists
							for(int proc = 0; proc < numProcessors; proc++)
								{
								statLists[type][proc] = new ArrayList<Stat>();
								}
							
							// Submit the array
							outputStatistics(type, times, stats, startSteps, endSteps);
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

/*
	long getStep(int pid, int index, long minStep)
	{
		if (pid < 0 || pid > numProcessors)
		{
			throw new IndexOutOfBoundsException(); //TODO
		}
		ArrayList<Object> queue = statQueues.get(pid);
		
		if (index < 0 || index > queue.size())
		{
			throw new IllegalArgumentException();//TODO
		}
		Object stats = queue.get(index);
		long lastStep; // <- the last step that was added to queue
		if (stats instanceof Long)
		{
			lastStep = (long) stats;
		}
		else
		{
			// Elements are either Stat objects or a placeholder as a long value holding the current timestep
			if (!(stats instanceof ArrayList))
			{
				throw new IllegalStateException();
				//TODO CHECK INNER TYPE (ArrayList<Stat>)
			}
			Stat firstStat = ((ArrayList<Stat>) stats).get(0);
			lastStep = ((Stat) firstStat).steps;
		}
		
		return lastStep;
	}
	
	//TODO no checks
	String getStatsAsCSV(long step)
	{
		ArrayList<Object> statsAtTimeStep = new ArrayList<Object>();
		for (int p = 0; p < numProcessors; p++)
		{
			ArrayList<Object> queue = statQueues.get(p);
			Object s = queue.get(0);
			if (!(s instanceof ArrayList))
				throw new IllegalStateException("Stat queues are required to start w/ a ArrayList<Stat> object. Instead, received " + s.getClass().getSimpleName());
			//TODO CHECK INNER TYPE (ArrayList<Stat>)
			ArrayList<Stat> stats = (ArrayList<Stat>) s;
			long firstElemSteps = stats.get(0).steps;
			int index = (int) (step - firstElemSteps);

			// Fail-safe
			if (index > queue.size() - 1)
			{
				index = queue.size() - 1;
			}
			
			Object elemAtStep = queue.get(index);
			statsAtTimeStep.add(elemAtStep);
		}
		
		String str = "";
		for (int i = 0; i < statsAtTimeStep.size(); i++)
		{
			Object obj = statsAtTimeStep.get(i);
			if (obj instanceof Long)
			{
				str += "__" + (long) obj + "__" + ",";   //TODO just comma
			}
			else
			{
				// Elements are either Stat objects or a placeholder as a long value holding the current timestep
				if (!(obj instanceof ArrayList))
				{
					throw new IllegalStateException();
					//TODO CHECK INNER TYPE (ArrayList<Stat>)
				}
				ArrayList<Stat> stats = (ArrayList<Stat>) obj;
				for (int j = 0; j < stats.size(); j++) {
					Stat stat = stats.get(j);
					str += ((Stat) stat).data + ", ";
				}
				str = str.substring(0, str.length() - ", ".length());
				str += " | ";
			}
		}
		
		return "step-" + step + ": " + str;
	}
	*/
	
	/**
	 * Retrieve stats from a time interval [start, end)
	 */
	 /*
	public String getStatsAsCSV(long start, long end)
	{
		String str = "";
		for (long index = start; index < end; index++)
		{
			str += getStatsAsCSV(index) + "\n";
		}
		return str;
	}
	*/
	
	/**
	 * Returns and clears the stat queues
	 */
	 /*
	public ArrayList<ArrayList<Object>> stats()
	{
		ArrayList<ArrayList<Object>> ret = statQueues;
		statQueues = new ArrayList<>();
		return ret;
	}
	*/
	
	/*
	public ArrayList<Object> getStatsAligned()
	{
		ArrayList<Object> ret2 = new ArrayList<Object>();
		ArrayList<ArrayList<Object>> ret = statQueues;
		
		for (int i=0; i<ret.size(); i++) 
		{
			for (int j=0; j<ret.get(i).size(); j++) 
			{	
				ret2.add(ret.get(i).get(j));
			}
		}
		statQueues = new ArrayList<>();
		return ret2;
	}
	*/
	
	public SimStateProxy(long seed)
		{
		super(seed);
		}

    public boolean remoteProxy()
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
    
    public Properties getProperties(int partition) {
    	
    	try {
    	VisualizationProcessor vp1 = visualizationProcessor(partition);

    	return new RemoteSimpleProperties(vp1);
    	
    	}
    	
    	catch(Exception e) {
    		System.out.println("Problem with Remote Properties");
    		System.out.println(e);
    		System.exit(-1);
    	    return null;	
    	}
    	
    	
    }
    
    /** Called to process the output statistics for stat type stat (either VisualizationProcessor.STATISTICS or 	
    	VisualizationProcessor.DEBUG).  There are some N MODEL STEPS, consisting of startSteps through endSteps inclusive.
    	For each MODEL STEPS, the array times[timestep - startSteps] indicates the model time at that step, and the
    	array stats[timestep - startSteps][processor] provides the statistics emitted by the given processor
    	at that time.  Each statistics is an ArrayList<Stat> holding Stat messages emitted by the processor at that
    	model step.  Usually this ArrayList will be empty or hold a single Stat message; though it is possible it may
    	hold many.
    */
    public void outputStatistics(int statType, double[] times, ArrayList<Stat>[][] stats, long startSteps, long endSteps)
    	{
    	// For the moment we're just dumping the data to debug it
    	/*
    	System.err.println("STATISTICS OUTPUT " + statType);
    	for(long i = startSteps; i < endSteps; i++)
    		{
    		System.err.print("Step: " + i + "\tTime: " + times[(int)(i - startSteps)]);
    		System.out.println("stats : "+stats);
    		System.out.println("startSteps "+startSteps);
    		System.out.println("endSteps "+endSteps);
    		System.out.println("stat size "+stats.length);
    		System.out.println("stats[] : "+stats[(int)(i - startSteps)]); //OOV ind: 4
    		System.out.println("--");

    		
    		for (int j = 0; j < stats[(int)(i - startSteps)].length; j++)
    			{
    			ArrayList<Stat> statList = stats[(int)(i - startSteps)][j];
    			if (!statList.isEmpty())
    				{
	    			System.err.print("\t" + j + ": ");
	    			boolean first = true;
	    			for(Stat stat : statList)
    					{
    					if (!first) System.err.println(", ");
    					first = false;
	    				System.err.print(stat.data);
	    				}
	    			}
    			}
    		}
    		*/
    	
    	System.err.println("STATISTICS OUTPUT " + statType);
    	for(long i = startSteps; i < endSteps; i++)
    		{
    		
    		for (int j = 0; j < stats.length; j++) //for each partition
    			{
    			ArrayList<Stat> statList = stats[j][(int)(i - startSteps)];
    			if (!statList.isEmpty())
    				{
	    			System.err.print("\t" + j + ": ");
	    			boolean first = true;
	    			for(Stat stat : statList)
    					{
    					if (!first) System.err.println(", ");
    					first = false;
	    				System.err.print(stat.data);
	    				}
	    			}
    			}
    		}
    	
    	}
    
    //this probably won't work bc properties can't be sent remotely
    /*
    //should we instead tabulate each partition's properties?  unsure
    public Properties getProperties(int partition) {
    	try {
    	VisualizationProcessor vp1 = visualizationProcessor(partition);
    	return vp1.getProperties();
    	//return null;
    	
    	}
    	
    	catch(Exception e) {
    		System.out.println("Problem with Properties");
    		System.out.println(e);
    		System.exit(-1);
    	    return null;	
    	}
    	
    }
    
    //for each partition
    public Properties[] getAllProperties() {
    	Properties[] prop_array = new Properties[numProcessors];
    	
    	for (int i=0; i<numProcessors; i++) {
    		prop_array[i] = getProperties(i);
    	}
    	return prop_array;
    }
    */
    
    //return list of partitions from the node in the tree
    public int[] buildPartitionsList(QuadTreeNode chosenNode)
    {
    	
    	int[] selectedNodes = new int[chosenNode.getLeaves().size()];
    	ArrayList<QuadTreeNode> leafNodes = chosenNode.getLeaves();
    	for (int i=0; i<selectedNodes.length; i++)
    	{
    		selectedNodes[i] = leafNodes.get(i).getProcessor();
    	}
    	
    	return selectedNodes;
    }
    
	/** Override this to add a tab to the Console. Normally you'd not do this from the SimState, but the Distributed code needs to use this. */
    public javax.swing.JComponent provideAdditionalTab()
    	{
    	return (overview = new Overview(this));
    	}
    
	/** Override this to add a tab name to the Console. Normally you'd not do this from the SimState, but the Distributed code needs to use this.  */
    public String provideAdditionalTabName()
    	{
    	return "Overview";
    	}
    
    
    
	}
	