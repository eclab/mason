package hotspots.sim;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileLock;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import hotspots.hotspotsData.HotSpotsData;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.geo.GeomVectorField;
import sim.field.grid.Grid2D;
import sim.field.grid.IntGrid2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.io.geo.ArcInfoASCGridImporter;
import sim.util.Bag;
import sim.util.geo.AttributeValue;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;
import hotspots.objects.Agent;

import swise.agents.communicator.Communicator;
import swise.agents.communicator.Information;
import swise.disasters.Wildfire;
import swise.objects.AStar;
import swise.objects.NetworkUtilities;
import swise.objects.PopSynth;
import swise.objects.network.GeoNode;
import swise.objects.network.ListEdge;

import org.jfree.data.xy.XYSeries;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import ec.util.MersenneTwisterFast;

/**
 * Hotspots is the core of a simulation which projects the behavior of agents in the aftermath
 * of an incident.
 * 
 * @author swise
 *
 */
public class Hotspots extends SimState {

	/////////////// Model Parameters ///////////////////////////////////
	
	private static final long serialVersionUID = 1L;
	public int grid_width = 700;
	public int grid_height = 700;
	public static double resolution = 5;// the granularity of the simulation 
				// (fiddle around with this to merge nodes into one another)

	double communication_success_prob = -1;
	double contact_success_prob = -1;
	double tweet_prob = -1;
	double retweet_prob = -1;
	double comfortDistance = -1;
	double observationDistance = -1;
	double decayParam = -1;
	double speed = -1;

	
	/////////////// Data Sources ///////////////////////////////////////
	
	String dirName = "/hotspots/hotspotsData/";
	
	public static String communicatorFilename = "communicatorEvents.txt";
	public static String agentFilename = "synthPopulationHOUSEHOLD.txt";

	String record_speeds_filename = "speeds/speeds", 
			record_sentiment_filename = "sentiments/sentiment",
			record_heatmap_filename = "heatmaps/heatmap",
			record_info_filename = "infos/info";

	BufferedWriter record_speeds, record_sentiment, record_heatmap;
	public BufferedWriter record_info;

	//// END Data Sources ////////////////////////
	
	/////////////// Containers ///////////////////////////////////////

	public GeomVectorField baseLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField roadLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField agentsLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField networkLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField networkEdgeLayer = new GeomVectorField(grid_width, grid_height);	
	public GeomVectorField majorRoadNodesLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField evacuationAreas = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField fireLayer = new GeomVectorField(grid_width, grid_height);
	public ArrayList <GeomVectorField> firePoints = new ArrayList <GeomVectorField>();
	public GeomGridField vegetation = new GeomGridField();
	public GeomGridField elevation = new GeomGridField();
	public GeomGridField impermeable = new GeomGridField();
	

	ArrayList <ListEdge> badRoads = null;
	
	public GeomGridField heatmap = new GeomGridField();

	public GeomVectorField hi_roadLayer = new GeomVectorField(grid_width, grid_height);
	public Network hiNetwork = new Network();

	/////////////// End Containers ///////////////////////////////////////

	/////////////// Objects //////////////////////////////////////////////

	
	public Bag roadNodes = new Bag();
	public Network roads = new Network(false);
	HashMap <MasonGeometry, ArrayList <GeoNode>> localNodes;
	public Bag terminus_points = new Bag();

	MediaInstance media = new MediaInstance();
	public ArrayList <Agent> agents = new ArrayList <Agent> (200000);
	public Network agentSocialNetwork = new Network();
	
	public GeometryFactory fa = new GeometryFactory();
	
	long mySeed = 0;
	
	Envelope MBR = null;
	
	public Wildfire wildfire;
	
	boolean verbose = false;
	
	public int numEvacuated = 0;
	public int numDied = 0;
	
	/////////////// END Objects //////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////
	/////////////////////////// BEGIN functions ///////////////////////////////
	///////////////////////////////////////////////////////////////////////////	
	
	/**
	 * Default constructor function
	 * @param seed
	 */
	public Hotspots(long seed) {
		super(seed);
		random = new MersenneTwisterFast(12345);
	}


	/**
	 * Read in data and set up the simulation
	 */
	public void start()
    {
		super.start();
		try {
			
			//////////////////////////////////////////////
			///////////// READING IN DATA ////////////////
			//////////////////////////////////////////////
		
			readInVectorLayer(baseLayer, dirName + "focusedArea.shp", "census tracts", new Bag());
			readInVectorLayer(roadLayer, dirName + "cleanedRoads.shp", "road network", new Bag());
			readInVectorLayer(evacuationAreas, dirName + "evacuationMETERS.shp", "evacuation areas", new Bag());
			readInRasterLayer(vegetation, dirName + "landcover_final.txt", "landcover", GridDataType.INTEGER);
			readInRasterLayer(elevation, dirName + "ned_final.txt", "elevation", GridDataType.DOUBLE);
			readInRasterLayer(impermeable, dirName + "impermeable_final.txt", "impermeability", GridDataType.INTEGER);

			// OPTIONAL: Real Wildfire Data 

	/*		System.out.print("Reading in wildfires...");
			File fireFolder = new File("/Users/swise/Dissertation/Colorado/data/fires_denverPost/shapefiles");
			File [] fireFiles = fireFolder.listFiles();
			for(File f: fireFiles){
				if(!f.getName().endsWith(".shp")) continue;
				GeomVectorField fireFile = new GeomVectorField(grid_width, grid_height);
				ShapeFileImporter.read(f.toURL(), fireFile);
				firePoints.add(fireFile);
			}
			schedule.scheduleRepeating(0, new Steppable(){

				@Override
				public void step(SimState state) {
					if(firePoints.size() <= 0) return;
					fireLayer.clear();
					GeomVectorField fire = firePoints.remove(0);
					for(Object o: fire.getGeometries()){
						fireLayer.addGeometry((MasonGeometry)o);
					}
					fireLayer.setMBR(MBR);
				}
			}, 30); // not accurate, only for visualization
			System.out.println("done");
			
*/
			
			//////////////////////////////////////////////
			////////////////// CLEANUP ///////////////////
			//////////////////////////////////////////////

			// standardize the MBRs so that the visualization lines up
			
			MBR = baseLayer.getMBR();
			MBR.init(501370, 521370, 4292000, 4312000);
			baseLayer.setMBR(MBR);

			this.grid_width = baseLayer.fieldWidth;
			this.grid_height = baseLayer.fieldHeight;

			evacuationAreas.setMBR(MBR);
			vegetation.setMBR(MBR);
			elevation.setMBR(MBR);
			impermeable.setMBR(MBR);

			baseLayer.setMBR(MBR);
			
			heatmap = new GeomGridField();
			heatmap.setMBR(MBR);
			heatmap.setGrid(new IntGrid2D((int)(MBR.getWidth() / 100), (int)(MBR.getHeight() / 100), 0));


			// clean up the road network
			
			System.out.print("Cleaning the road network...");
			
			roads = NetworkUtilities.multipartNetworkCleanup(roadLayer, roadNodes, resolution, fa, random, 0);
			roadNodes = roads.getAllNodes();
			testNetworkForIssues(roads);
			
			// set up roads as being "open" and assemble the list of potential terminii
			roadLayer = new GeomVectorField(grid_width, grid_height);
			for(Object o: roadNodes){
				GeoNode n = (GeoNode) o;
				networkLayer.addGeometry(n);
				
				boolean potential_terminus = false;
				
				// check all roads out of the nodes
				for(Object ed: roads.getEdgesOut(n)){
					
					// set it as being (initially, at least) "open"
					ListEdge edge = (ListEdge) ed;
					((MasonGeometry)edge.info).addStringAttribute("open", "OPEN");
					networkEdgeLayer.addGeometry( (MasonGeometry) edge.info);
					roadLayer.addGeometry((MasonGeometry) edge.info);
					((MasonGeometry)edge.info).addAttribute("ListEdge", edge);
					
					String type = ((MasonGeometry)edge.info).getStringAttribute("TYPE");
					if(type.equals("motorway") || type.equals("primary") || type.equals("trunk"))
						potential_terminus = true;
				}
				
				// check to see if it's a terminus
				if(potential_terminus && !MBR.contains(n.geometry.getCoordinate()) && roads.getEdges(n, null).size() == 1){
					terminus_points.add(n);
				}

			}

			// reset MBRS in case it got messed up during all the manipulation
			roadLayer.setMBR(MBR);			
			networkLayer.setMBR(MBR);
			networkEdgeLayer.setMBR(MBR);
			roadLayer.setMBR(MBR);
			
			System.out.println("done");

			/////////////////////
			///////// Clean up roads for Agents to use ///////////
			/////////////////////
						
			Network majorRoads = extractMajorRoads();
			testNetworkForIssues(majorRoads);

			// assemble list of secondary versus local roads
			ArrayList <Edge> myEdges = new ArrayList <Edge> ();
			GeomVectorField secondaryRoadsLayer = new GeomVectorField(grid_width, grid_height);
			GeomVectorField localRoadsLayer = new GeomVectorField(grid_width, grid_height);
			for(Object o: majorRoads.allNodes){
				
				majorRoadNodesLayer.addGeometry((GeoNode)o);
				
				for(Object e: roads.getEdges(o, null)){
					Edge ed = (Edge) e;
					
					myEdges.add(ed);
										
					String type = ((MasonGeometry)ed.getInfo()).getStringAttribute("class");
					if(type.equals("secondary"))
							secondaryRoadsLayer.addGeometry((MasonGeometry) ed.getInfo());
					else if(type.equals("local"))
							localRoadsLayer.addGeometry((MasonGeometry) ed.getInfo());					
				}
			}

			System.gc();
			
			//////////////////////////////////////////////
			////////////////// AGENTS ///////////////////
			//////////////////////////////////////////////

			// set up the agents in the simulation
			setupAgentsFromFile(dirName + agentFilename);
			agentsLayer.setMBR(MBR);
			
			// for each of the Agents, set up relevant, environment-specific information
			int aindex = 0;
			for(Agent a: agents){
				
				if(a.familiarRoadNetwork == null){
					
					// the Agent knows about major roads
					Network familiar = majorRoads.cloneGraph();

					// connect the major network to the Agent's location
					connectToMajorNetwork(a.getNode(), familiar);
					
					a.familiarRoadNetwork = familiar;

					// add local roads into the network
					for(Object o: agentsLayer.getObjectsWithinDistance(a, 50)){
						Agent b = (Agent) o;
						if(b == a || b.familiarRoadNetwork != null || b.getNode() != a.getNode()) continue;
						b.familiarRoadNetwork = familiar.cloneGraph();
					}

				}
				
				// connect the Agent's work into its personal network
				if(a.getWork() != null)
					connectToMajorNetwork(getClosestGeoNode(a.getWork()), a.familiarRoadNetwork);
				
				// set up its basic paths (fast and quicker and recomputing each time)
				a.setupPaths();

				if(aindex % 100 == 0){ // print report of progress
					System.out.println("..." + aindex + " of " + agents.size());
				}
				aindex++;
			}

			//////////////////////////////////////////////
			////////////////// WILDFIRE ///////////////////
			//////////////////////////////////////////////

			wildfire = new Wildfire(vegetation, elevation, impermeable, MBR);
			schedule.scheduleRepeating(wildfire, 0, 12); 
			wildfire.initiateFire(505556, 4306026);
			
			// schedule the road network to update as the wildfire moves
			this.schedule.scheduleRepeating(new Steppable(){
				private static final long serialVersionUID = 1L;

				@Override
				public void step(SimState state) {

					// check to see if any roads have been overtaken by the wildfire: if so, remove them from the network
					badRoads = new ArrayList <ListEdge> ();
					Bag overlappers = networkEdgeLayer.getObjectsWithinDistance(wildfire.extent, resolution);
					for(Object o: overlappers){
						ListEdge aBadRoad = (ListEdge) ((AttributeValue) ((MasonGeometry) o).getAttribute("ListEdge")).getValue();
						badRoads.add( aBadRoad);
					}

					// close the closed roads
					for(ListEdge r: badRoads){
						((MasonGeometry)r.info).addStringAttribute("open", "CLOSED");
					}
				}
				
			}, 10, 12);
			fireLayer.setMBR(MBR);
		
			// schedule the fire layer to update as the wildfire moves
			schedule.scheduleRepeating(new Steppable(){

				@Override
				public void step(SimState state) {
					fireLayer.clear();
					MBR = baseLayer.getMBR();
					MBR.init(501370, 521370, 4292000, 4312000);
					fireLayer.setMBR(MBR);
					fireLayer.addGeometry(new MasonGeometry(wildfire.extent));
				}
				
			}, 12);
			
			// set up the evacuation orders to be inserted into the social media environment
			setupCommunicators(dirName + communicatorFilename);
		
			// seed the simulation randomly
			seedRandom(System.currentTimeMillis());

			// schedule the reporter to run
			setupReporter();

		} catch (Exception e) { e.printStackTrace();}
    }
	
	/**
	 * Schedule the regular 
	 */
	public void setupReporter() {

		// set up the reporting files
		try {
			String mySettings = communication_success_prob + "_" + contact_success_prob + "_" 
					+ tweet_prob + "_" + retweet_prob + "_" + comfortDistance + "_" + 
					observationDistance + "_" + decayParam + "_" + speed + "_";

			record_sentiment = new BufferedWriter(new FileWriter(dirName
					+ record_sentiment_filename + mySettings + mySeed + ".txt"));
			record_speeds = new BufferedWriter(new FileWriter(dirName
					+ record_speeds_filename + mySettings + mySeed + ".txt"));

			record_info = new BufferedWriter(new FileWriter(dirName
					+ record_info_filename + mySettings + mySeed + ".txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// schedule the simulation to report on Agents every tick
		this.schedule.scheduleRepeating(0, 10000, new Steppable() {

			DecimalFormat formatter = new DecimalFormat("#.##");

			@Override
			synchronized public void step(SimState state) {
				try {
					int time = (int) state.schedule.getTime();

					String speeds = time + "", sentiments = "";
					int numSentAgents = 0;
					for (Agent a : agents) {
						if (a.getActivity() == Agent.activity_evacuate || a.getActivity() == Agent.activity_travel)
							speeds += "\t" + Math.max(0, a.myLastSpeed);
						if (a.getValence() > 0) {
							sentiments += "\t" + formatter.format(a.getValence());
							numSentAgents++;
						}
					}
					record_sentiment.write(time + "\t" + numSentAgents + sentiments + "\n");
					record_speeds.write(speeds + "\n");

					record_sentiment.flush();
					record_speeds.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 12);
	}
	
	/**
	 * Connect the GeoNode to the given subnetwork using the complete road network
	 * 
	 * @param n - the target node
	 * @param subNetwork - the existing subnetwork
	 */
	void connectToMajorNetwork(GeoNode n, Network subNetwork) {

		try {
			Bag subNetNodes;			
			subNetNodes = (Bag) subNetwork.allNodes.clone();
			
			// find a path using the whole set of roads in the environment 
			AStar pathfinder = new AStar();
			ArrayList <Edge> edges = pathfinder.astarPath(n, new ArrayList <GeoNode> (subNetNodes), roads);
			
			if(edges == null) return; // maybe no such path exists!

			//  otherwise, add the edges into the subnetwork
			for(Edge e: edges){
				GeoNode a = (GeoNode) e.getFrom(), b = (GeoNode) e.getTo();
				if(!subNetwork.nodeExists(a) || !subNetwork.nodeExists(b))
					subNetwork.addEdge(a, b, e.info);
			}

		} catch (CloneNotSupportedException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * Finish the simulation and clean up
	 */
	public void finish(){
		super.finish();
		try{
			
			// clean up and finish recording everything
			
			this.record_sentiment.close();
			this.record_speeds.close();
			
			// create part of the title to record all the paramters used in this simulation
			String mySettings = communication_success_prob + "_" + contact_success_prob + "_" + tweet_prob + "_" + 
					retweet_prob + "_" + comfortDistance + "_" + observationDistance + "_" + decayParam + "_" + speed + "_";

			// SAVE THE HEATMAP
			record_heatmap = new BufferedWriter(new FileWriter(dirName + record_heatmap_filename + mySettings + mySeed + ".txt"));
			IntGrid2D myHeatmap = ((IntGrid2D) this.heatmap.getGrid());

			// write a header
			record_heatmap.write(myHeatmap.getWidth() + "\t" + myHeatmap.getHeight() + "\t" + (int)schedule.getTime() + "\n");
			for(int i = 0; i < myHeatmap.getWidth(); i++){
				String output = "";
				for(int j = 0; j < myHeatmap.getHeight(); j++){
					output += myHeatmap.field[i][j] + "\t";
				}
				record_heatmap.write(output + "\n");
			}
			record_heatmap.close();

			// print a record out
			System.out.println(this.mySeed + "\t" + this.numDied + "\t" + this.numEvacuated);
			
			// SAVE ALL AGENT INFO
			
			for(Agent a: agents){
				String myID = a.toString();
				for(Object o: a.knowledge.keySet()){
					Information i = a.knowledge.get(o);
					Object source = i.getSource();
					String sourceStr;
					if(source instanceof Agent)
						sourceStr = ((Agent)source).toString();
					else if(source == null)
						sourceStr = "null";
					else
						sourceStr = source.toString();
					
					try {
						record_info.write(myID + "\t" + sourceStr + "\t" + i.getTime() + "\t" + o.toString() + "\n");
					} catch (IOException e) {e.printStackTrace();}
				}

			}

			this.record_info.close();

		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Make sure the network doesn't have any problems
	 * 
	 * @param n - the network to be tested
	 */
	static void testNetworkForIssues(Network n){
		System.out.println("testing");
		for(Object o: n.allNodes){
			GeoNode node = (GeoNode) o;
			for(Object p: n.getEdgesOut(node)){
				sim.field.network.Edge e = (sim.field.network.Edge) p;
				LineString ls = (LineString)((MasonGeometry)e.info).geometry;
				Coordinate c1 = ls.getCoordinateN(0);
				Coordinate c2 = ls.getCoordinateN(ls.getNumPoints()-1);
				GeoNode g1 = (GeoNode) e.getFrom();
				GeoNode g2 = (GeoNode) e.getTo();
				if(c1.distance(g1.geometry.getCoordinate()) > 1)
					System.out.println("found you");
				if(c2.distance(g2.geometry.getCoordinate()) > 1)
					System.out.println("found you");
			}
		}
	}
	
	/**
	 * Set up the evacuation orders from the given file
	 * @param filename
	 */
	public void setupCommunicators(String filename){
		try {
			
			// Open the communicators file
			FileInputStream fstream = new FileInputStream(filename);
			
			// Convert our input stream to a BufferedReader
			BufferedReader communicatorData = new BufferedReader(new InputStreamReader(fstream));
			String s;
			
			while ((s = communicatorData.readLine()) != null) {
				String[] bits = s.split("\t");
				
				int time = Integer.parseInt(bits[0]);
				
				// create the evacuation orders as appropriate
				if(bits[1].equals("EvacuationOrder")){
					Geometry evacZone = null;
					for(Object o: evacuationAreas.getGeometries()){
						if(((MasonGeometry)o).getStringAttribute("zone").equals(bits[2])){
							evacZone = ((MasonGeometry)o).geometry;
							break;
						}
					}
					
					// if the area has a proper evacuation zone, store that in the media object
					if(evacZone != null)
						media.learnAbout(null, new EvacuationOrder(evacZone, time, null));;
				}
					
			}
			
			// schedule the media object to push out the information when appropriate
			if(media.storage.size() > 0)
				schedule.scheduleOnce(media.storage.get(0).getTime(), media);
			
			// clean up
			communicatorData.close();
			
		} catch (Exception e) {
			System.err.println("File input error: " + filename);
		}

	}
	
	/**
	 * Media object which pushes information out into the social media network
	 * upon a pre-appointed timetable
	 * @author swise
	 *
	 */
	public class MediaInstance implements Communicator, Steppable {

		ArrayList <Information> storage = new ArrayList <Information> ();
		ArrayList <Information> socialMediaPosts = new ArrayList <Information> ();

		@Override
		public ArrayList getInformationSince(double time) {
			ArrayList <Object> result = new ArrayList <Object> ();
			for(Object o: socialMediaPosts){
				long myTime = ((Information)o).getTime();
				if(myTime >= time)
					result.add(o);
			}
			return result;
		}

		@Override
		public void learnAbout(Object o, Information i) {
			storage.add(i);			
		}

		@Override
		public void step(SimState state) {
			Information i = storage.get(0);
			if(i.getTime() <= state.schedule.getTime()){
				socialMediaPosts.add(i);
				storage.remove(0);
			}
			if(storage.size() > 0)
				schedule.scheduleOnce(storage.get(0).getTime(), this);
		}
		
	}
	
	/**
	 * Given a record file of a set of Agents, create Agents with the assigned characteristics
	 * and add them to the simulation
	 * 
	 * @param agentsFilename - the file in which the agent records are stored
	 */
	synchronized void setupAgentsFromFile(String agentsFilename){
		try {
			
			System.out.println("Reading in agents from " + agentsFilename);
			
			// Open the tracts file
			FileInputStream fstream = new FileInputStream(agentsFilename);

			// Convert our input stream to a BufferedReader
			BufferedReader agentData = new BufferedReader(new InputStreamReader(fstream));
			String s;


			System.out.println("BEGIN READING IN PEOPLE");

			HashMap <Agent, HashMap <String, Integer>> socialNetwork = new HashMap <Agent, HashMap <String, Integer>> ();
			HashMap <Agent, ArrayList <String>> socialMediaNetwork = new HashMap <Agent, ArrayList <String>> ();
			HashMap <String, Agent> agentNameMapping = new HashMap <String, Agent> ();
			
			int indexy = -1;
			while ((s = agentData.readLine()) != null) {
				String[] bits = s.split("\t");
				
				indexy++;
				
				// recreate the Agent from the record
				String id = bits[0];
				Integer age = Integer.parseInt(bits[1]);
				Integer sex = Integer.parseInt(bits[2]);
				Coordinate homeCoord = readCoordinateFromFile(bits[3]);
				Coordinate workCoord = readCoordinateFromFile(bits[4]);
				
				Agent a;
				
				if(speed > 0)
					a = new Agent(id, homeCoord, homeCoord, workCoord, this, 
						communication_success_prob, contact_success_prob, tweet_prob, retweet_prob, 
						comfortDistance, observationDistance, decayParam, speed);
				else
					a = new Agent(id, homeCoord, homeCoord, workCoord, this);
				
				a.addIntegerAttribute("sex", sex);
				a.addIntegerAttribute("age", age);
				agentNameMapping.put(id, a);

				this.agents.add(a);

				// SOCIAL NETWORK GENERATION
				
				// store social information for complete creation later
				socialNetwork.put(a, new HashMap <String, Integer> ());
				Integer networkSize = Integer.parseInt(bits[5]);
				for(int i = 0; i < networkSize; i++){
					int index = i + 6;
					String [] contact = bits[index].split(" ");
					String contactName = contact[0];
					Integer contactWeight = (int) (10 * Double.parseDouble(contact[1]));
					
					// TODO: possibly change back
					if(contactWeight > PopSynth.acquaintenceWeight)
						socialNetwork.get(a).put(contactName, contactWeight);
				}

				
				// SOCIAL MEDIA NETWORK GENERATION
				
				if(bits.length <= networkSize + 6) 
					continue;
				
				socialMediaNetwork.put(a, new ArrayList <String> ());
				Integer mediaNetworkSize = Integer.parseInt(bits[networkSize + 6]);
				for(int i = 0; i < mediaNetworkSize; i++){
					int index = i + 7 + networkSize;
					String contactName = bits[index]; 
					socialMediaNetwork.get(a).add(contactName);
				}
			}

			agentData.close();

			System.out.println("REINFLATING SOCIAL TIES...");

			indexy = -1;
			// reinflate the stored social network from the records
			for(Agent a: socialNetwork.keySet()){

				indexy++;
				if(indexy % 1000 == 0)
					System.out.println("\t" + indexy + "...");

				for(Entry e: socialNetwork.get(a).entrySet()){
					String contact = (String) e.getKey();
					int weight = (Integer) e.getValue();
					Agent b = agentNameMapping.get(contact);
					if(a == null || b == null) continue;
					a.addContact(b, weight);
					b.addContact(a, weight);
				}				
			}
			
			System.out.println("REINFLATING MEDIA TIES...");

			indexy = -1;
			// reinflate the stored social media network from the records
			for(Agent a: socialMediaNetwork.keySet()){

				indexy++;
				if(indexy % 1000 == 0)
					System.out.println("\t" + indexy + "...");

				for(String b: socialMediaNetwork.get(a)){
					Agent c = agentNameMapping.get(b);
					if(c == null) continue;
					a.addSocialMediaContact(c);
					c.addSocialMediaContact(a);
				}
			}
			
			for(Agent a: agents){
				// EVERYONE IS IN TOUCH WITH MASS MEDIA! (social media just amplifies the signal)
				a.addSocialMediaContact(media);
			}
			
			System.out.println("DONE READING IN PEOPLE");
			// clean up
			
			
			schedule.scheduleRepeating(1316, new Steppable(){

				@Override
				public void step(SimState state) {
					resetLayers();
					
				}
				
			});

		} catch (Exception e) {
			System.err.println("File input error: " + agentsFilename);
		}
	}
	
	/**
	 * Coordinate reader helper function
	 * @param s
	 * @return
	 */
	Coordinate readCoordinateFromFile(String s){
		if(s.equals("")) 
			return null;
		
		String [] bits = s.split(" ");
		Double x = Double.parseDouble( bits[1].substring(1) );
		Double y = Double.parseDouble(bits[2].substring(0, bits[2].length() - 2));
		return new Coordinate(x,y);
	}
	
	//////////////////////////////////////////////
	////////// UTILITIES /////////////////////////
	//////////////////////////////////////////////

	/**
	 * Method to read in a vector layer
	 * @param layer
	 * @param filename
	 * @param layerDescription
	 * @param attributes - optional: include only the given attributes
	 */
	synchronized void readInVectorLayer(GeomVectorField layer, String filename, String layerDescription, Bag attributes){
		try {
				System.out.print("Reading in " + layerDescription + "from " + filename + "...");
				//File file = new File(filename);
				if(attributes == null || attributes.size() == 0)
					ShapeFileImporter.read(getUrl(filename), layer);
				else
					ShapeFileImporter.read(getUrl(filename), layer, attributes);
				System.out.println("done");	

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static URL getUrl(String nodesFilename) throws IOException {
		InputStream nodeStream = HotSpotsData.class.getResourceAsStream(nodesFilename);
		try {
			if (!new File("./shapeFiles/").exists()) {
				new File("./shapeFiles/").mkdir();
			}
			File targetFile = new File("./shapeFiles/" + nodesFilename.split("/")[nodesFilename.split("/").length - 1]);
			OutputStream outStream = new FileOutputStream(targetFile);
			//outStream.write(buffer);
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = nodeStream.read(bytes)) != -1) {
				outStream.write(bytes, 0, read);
			}
			outStream.close();
			nodeStream.close();
			if (nodesFilename.endsWith(".shp")) {
				getUrl(nodesFilename.replace("shp", "dbf"));
				getUrl(nodesFilename.replace("shp", "prj"));
				getUrl(nodesFilename.replace("shp", "sbx"));
				getUrl(nodesFilename.replace("shp", "sbn"));
				getUrl(nodesFilename.replace("shp", "shx"));
			}
			return targetFile.toURI().toURL();
		} catch (Exception e) {
			if (nodesFilename.endsWith("shp")) {
				e.printStackTrace();
				return null;
			} else {
				//e.printStackTrace();
				return null;
			}
		}
	}
	
	/**
	 * Method ot read in a raster layer
	 * @param layer
	 * @param filename
	 * @param layerDescription
	 * @param type
	 */
	synchronized void readInRasterLayer(GeomGridField layer, String filename, String layerDescription, GridDataType type){
		try {
				
				System.out.print("Reading in " + layerDescription + "from " + filename + "...");
				FileInputStream fstream = new FileInputStream(filename);
				ArcInfoASCGridImporter.read(fstream, type, layer);
				fstream.close();
				System.out.println("done");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Extract the major roads from the road network
	 * @return a connected network of major roads
	 */
	public Network extractMajorRoads(){
		Network majorRoads = new Network();
		
		// go through all nodes
		for(Object o: roads.getAllNodes()){
		
			GeoNode n = (GeoNode) o;
			
			// go through all edges
			for(Object p: roads.getEdgesOut(n)){
				
				sim.field.network.Edge e = (sim.field.network.Edge) p;
				String type = ((MasonGeometry)e.info).getStringAttribute("class");
				
				// save major roads
				if(type.equals("major"))
						majorRoads.addEdge(e.from(), e.to(), e.info);
			}
		}
		
		// merge the major roads into a connected component
		NetworkUtilities.attachUnconnectedComponents(majorRoads, roads);
		
		return majorRoads;
	}
		
	/**
	 * Convenient method for incrementing the heatmap
	 * @param geom - the geometry of the object that is impacting the heatmap
	 */
	public void incrementHeatmap(Geometry geom){
		Point p = geom.getCentroid();
		
		int x = (int)(heatmap.getGrid().getWidth()*(MBR.getMaxX() - p.getX())/(MBR.getMaxX() - MBR.getMinX())), 
				y = (int)(heatmap.getGrid().getHeight()*(MBR.getMaxY() - p.getY())/(MBR.getMaxY() - MBR.getMinY()));
		if(x >= 0 && y >= 0 && x < heatmap.getGrid().getWidth() && y < heatmap.getGrid().getHeight())
			((IntGrid2D) this.heatmap.getGrid()).field[x][y]++;
	}
	
	/**
	 * Return the GeoNode in the road network which is closest to the given coordinate
	 * 
	 * @param c
	 * @return
	 */
	public GeoNode getClosestGeoNode(Coordinate c){
		
		// find the set of all nodes within *resolution* of the given point
		Bag objects = networkLayer.getObjectsWithinDistance(fa.createPoint(c), resolution);
		if(objects == null || networkLayer.getGeometries().size() <= 0) 
			return null; // problem with the network layer

		// among these options, pick the best
		double bestDist = resolution; // MUST be within resolution to count
		GeoNode best = null;
		for(Object o: objects){
			double dist = ((GeoNode)o).geometry.getCoordinate().distance(c);
			if(dist < bestDist){
				bestDist = dist;
				best = ((GeoNode)o);
			}
		}
		
		// if there is a best option, return that!
		if(best != null && bestDist == 0) 
			return best;
		
		// otherwise, closest GeoNode is associated with the closest Edge, so look for that!
		
		ListEdge edge = getClosestEdge(c);
		
		// find that edge
		if(edge == null){
			edge = getClosestEdge(c, resolution * 10);
			if(edge == null)
				return null;
		}
		
		// of that edge's endpoints, find the closer of the two and return it
		GeoNode n1 = (GeoNode) edge.getFrom();
		GeoNode n2 = (GeoNode) edge.getTo();
		
		if(n1.geometry.getCoordinate().distance(c) <= n2.geometry.getCoordinate().distance(c))
			return n1;
		else 
			return n2;
	}
	
	/**
	 * Return the ListEdge in the road network which is closest to the given coordinate
	 * 
	 * @param c
	 * @return
	 */
	public ListEdge getClosestEdge(Coordinate c){
		
		// find the set of all edges within *resolution* of the given point
		Bag objects = networkEdgeLayer.getObjectsWithinDistance(fa.createPoint(c), resolution);
		if(objects == null || networkEdgeLayer.getGeometries().size() <= 0) 
			return null; // problem with the network edge layer
		
		Point point = fa.createPoint(c);
		
		// find the closest edge among the set of edges
		double bestDist = resolution;
		ListEdge bestEdge = null;
		for(Object o: objects){
			double dist = ((MasonGeometry)o).getGeometry().distance(point);
			if(dist < bestDist){
				bestDist = dist;
				bestEdge = (ListEdge) ((AttributeValue) ((MasonGeometry) o).getAttribute("ListEdge")).getValue();
			}
		}
		
		// if it exists, return it
		if(bestEdge != null)
			return bestEdge;
		
		// otherwise return failure
		else
			return null;
	}
	
	/**
	 * Return the ListEdge in the road network which is closest to the given coordinate, within the given resolution
	 * 
	 * @param c
	 * @param resolution
	 * @return
	 */
	public ListEdge getClosestEdge(Coordinate c, double resolution){

		// find the set of all edges within *resolution* of the given point
		Bag objects = networkEdgeLayer.getObjectsWithinDistance(fa.createPoint(c), resolution);
		if(objects == null || networkEdgeLayer.getGeometries().size() <= 0) 
			return null; // problem with the network edge layer
		
		Point point = fa.createPoint(c);
		
		// find the closest edge among the set of edges
		double bestDist = resolution;
		ListEdge bestEdge = null;
		for(Object o: objects){
			double dist = ((MasonGeometry)o).getGeometry().distance(point);
			if(dist < bestDist){
				bestDist = dist;
				bestEdge = (ListEdge) ((AttributeValue) ((MasonGeometry) o).getAttribute("ListEdge")).getValue();
			}
		}
		
		// if it exists, return it
		if(bestEdge != null)
			return bestEdge;
		
		// otherwise return failure
		else
			return null;
	}
	
	/**
	 * RoadClosure structure holds information about a road closure
	 */
	public class RoadClosure extends Information {
		public RoadClosure(Object o, long time, Object source) {
			super(o, time, source, 5);
		}
	}
	
	/**
	 * EvacuationOrder structure holds information about an evacuation order
	 */
	public class EvacuationOrder extends Information {
		public Geometry extent = null;
		public EvacuationOrder(Object o, long time, Object source) {
			super(o, time, source, 8);
			extent = (Geometry) o;
		}		
	}
	
	/** set the seed of the random number generator */
	void seedRandom(long number){
		random = new MersenneTwisterFast(number);
		mySeed = number;
	}
	
	// reset the agent layer's MBR
	public void resetLayers(){
		MBR = baseLayer.getMBR();
		MBR.init(501370, 521370, 4292000, 4312000);
		this.agentsLayer.setMBR(MBR);
		this.roadLayer.setMBR(MBR);
	}
	
	/**
	 * To run the model without visualization
	 */
	public static void main(String[] args)
    {
		
		if(args.length < 8){
			System.out.println("usage error");
			System.exit(0);
		}
		
		Hotspots hspot = new Hotspots(System.currentTimeMillis());
		
		hspot.communication_success_prob = Double.parseDouble(args[0]);
		hspot.contact_success_prob = Double.parseDouble(args[1]);
		hspot.tweet_prob = Double.parseDouble(args[2]);
		hspot.retweet_prob = Double.parseDouble(args[3]);
		hspot.comfortDistance = Double.parseDouble(args[4]);
		hspot.observationDistance = Double.parseDouble(args[5]);
		hspot.decayParam = Double.parseDouble(args[6]);
		hspot.speed = Double.parseDouble(args[7]);
		
		System.out.println("Loading...");

		hspot.start();

		System.out.println("Running...");

		for(int i = 0; i < 288 * 3; i++){
			hspot.schedule.step(hspot);
		}
		
		hspot.finish();
		
		System.out.println("...run finished");

		System.exit(0);
    }
}