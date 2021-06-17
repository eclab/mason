package sim.display;

import java.rmi.NotBoundException;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import ec.util.MersenneTwisterFast;
import sim.engine.DSimState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.rmi.RemoteProcessor;
import sim.field.partitioning.QuadTreeNode;
import sim.field.storage.GridStorage;
import sim.util.IntRect2D;

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
	
	int port = sim.util.DRegistry.PORT;
	
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
	public static final int DEFAULT_STEP_SIZE = 1000;
	
	public long refresh = 0;
	protected int sleep = DEFAULT_SLEEP;
	protected int stepSize = DEFAULT_STEP_SIZE;
	/** Returns the update rate in ms */
	public int stepSize()
		{
		return stepSize;
		}
	
	/** Sets the update rate in ms */
	public void setStepSize(int val)
		{
		stepSize = val;
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
	
	/** Ordered stat data (or placeholder if no data) for each processor from the earliest timestep saved in the queues {@link SimStateProxy#statsSmallestTimestep} to the current one
	<p> TODO WELL atm, it's really this: ArrayList&ltArrayList&ltInteger | Stat&gt&gt */
	ArrayList<ArrayList<Object>> statQueues;
	
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
			
			for (int i = 0; i < numProcessors; i++)
			{
				statQueues.add(new ArrayList<>());
			}

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
						Thread.sleep(sleep);
						}
					catch (InterruptedException ex)
						{
						ex.printStackTrace();
						}
					
					// Next we check to see if enough time has elapsed to bother querying the remote processor
					long cur = System.currentTimeMillis();
					if (cur - refresh >= stepSize)
						{
						refresh = cur;
						try
							{
							// Now we query the remote processor to see if a new step has elapsed
							VisualizationProcessor vp = visualizationProcessor(); 
                            							
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
								vp.unlock();

								// === Stats & Debug === //
								// Determine if any stats are registered (on *any* processor)
								boolean statsExist = false; // flag for if any stats are registered
								ArrayList<ArrayList<Stat>> allStats = new ArrayList<>();
								for (int p = 0; p < numProcessors; p++)
								{
									VisualizationProcessor vp1 = visualizationProcessor(p);
									ArrayList<Stat> statList = vp1.getStatList();
									allStats.add(statList);
									if (!statsExist && !statList.isEmpty())
										statsExist = true;
								}
								
								if (statsExist)
								{
									// Find minimum step over all processor stats
									long minStep = Long.MAX_VALUE;
									long maxStep = Long.MIN_VALUE;
									for (int p = 0; p < allStats.size(); p++) {
										ArrayList<Stat> stats = allStats.get(p);
										if (stats.get(0).steps < minStep)
										{
											minStep = stats.get(0).steps;
										}
										if (stats.get(stats.size() - 1).steps > maxStep) //TODO What if stats is empty? can it be?
										{
											maxStep = stats.get(stats.size() - 1).steps;
										}
									}
									// TODO Overflow check?...or no?
									if (minStep < 0)
									{
										throw new IllegalStateException();
									}
									
									for (int p = 0; p < allStats.size(); p++)
									{
										ArrayList<Stat> stats = allStats.get(p);
										ArrayList<Object> queue = statQueues.get(p);
										
	//									TODO? long currStep = minStep;
										long currStep = 0; // if queue is empty, start at timestep 0
										if (!queue.isEmpty()) // ...otherwise, start at last timestep + 1
										{
											currStep = getStep(p, queue.size() - 1, minStep) + 1;
										}
	
										// Purpose: Verification - to make sure all timesteps are accounted for
										if (!queue.isEmpty())
										{
											getStep(p, queue.size() - 1, minStep); // <- Note: this will throw error this creates inconsistent timsteps in queues
										}
	
										int currIndex = 0; // index of incoming stats
										while (currIndex < stats.size())
										{
											// Fill in any skipped steps
											while (currStep < stats.get(currIndex).steps)
											{
//												System.out.println("off steps: " + currStep + " and " + stats.get(currIndex).steps);
												long lastStep = getStep(p, queue.size() - 1, minStep);
												queue.add(lastStep);
												currStep++;
											}
											// <- currStep == stat.steps
											// Add *this* stat and the rest of the stats at *this* timestep
											ArrayList<Stat> currTimeStepStats = new ArrayList<Stat>();
											do
											{
												currTimeStepStats.add(stats.get(currIndex));
												currIndex++;
											}
											while (currIndex < stats.size() && stats.get(currIndex).steps == currStep);
											queue.add(currTimeStepStats);
											
											currStep++;
											// <- currStep == last stat.steps + 1
										}
									}
									
									// Test:
//										ArrayList<Serializable> stats = getStats(maxStep);
									System.out.println();
									System.out.println("***************************************");
									System.out.println("*** Stats dumped for timestep " + maxStep);
									String header = "";
									for (int p = 0; p < numProcessors; p++)
									{
										header += "P" + p + " | ";
									}
									System.out.println(header.substring(0, header.length() - " | ".length()));
									
									for (int i = 0; i < maxStep - 1; i++)
									{
										System.out.println(getStatsAsCSV(i));
									}
									
									System.out.println("***************************************");
									MersenneTwisterFast random = new MersenneTwisterFast();
									long randStart = Math.abs(random.nextLong()) % (maxStep - minStep);
									long randEnd = Math.abs(random.nextLong()) % (maxStep - minStep);
									if (randStart > randEnd) // swap if necessary
									{
										long tmp = randStart;
										randStart = randEnd;
										randEnd = tmp;
									}
									System.out.println("*** Stats dumped for timesteps [" + randStart + "," + randEnd + ")");
									System.out.println(header.substring(0, header.length() - " | ".length()));
									
									System.out.println(getStatsAsCSV(randStart, randEnd));
									
//										// === Write to File === //
//										// TODO this will not work for jars, right? Need a zip file tracer?
//										File statsFile = new File(statsFileName);
//										// Create the file if it doesn't exist
//										if (!statsFile.exists()) {
//											statsFile.mkdirs();
//										}
//										
//										BufferedWriter writer;
//										try {
//											writer = new BufferedWriter(new FileWriter(statsFileName, true));
//										    writer.append(getStatsAsCSV(minStep, maxStep));									    
//											writer.close();
//										} catch (IOException e1) {
//											// TODO Auto-generated catch block
//											e1.printStackTrace();
//										}
									
									//TODO Do the same for Debug
									// <<<<<<Stats & Debug
									}
//									}
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

	private long getStep(int pid, int index, long minStep)
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
	public String getStatsAsCSV(long step)
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
				str += "__" + (long) obj + "__" + ",";//TODO just comma
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
	
	/**
	 * Retrieve stats from a time interval [start, end)
	 */
	public String getStatsAsCSV(long start, long end)
	{
		String str = "";
		for (long index = start; index < end; index++)
		{
			str += getStatsAsCSV(index) + "\n";
		}
		return str;
	}
	
	/**
	 * Returns and clears the stat queues
	 */
	public ArrayList<ArrayList<Object>> getStats()
	{
		ArrayList<ArrayList<Object>> ret = statQueues;
		statQueues = new ArrayList<>();
		return ret;
	}
	
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
	