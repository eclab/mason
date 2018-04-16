package cityMigration;


import com.vividsolutions.jts.geom.Envelope;
import riftland.Parameters;
import riftland.PopulationCenter;
import riftland.RefugeeGroup;
import riftland.household.Household;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.geo.GeomVectorField;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.field.network.Network.IndexOutIn;
import sim.field.network.stats.NetworkStatistics;
import sim.field.network.stats.WeightedEdgeMetric;
import sim.io.geo.ArcInfoASCGridImporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Interval;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import cityMigration.cityMigrationData.CityMigrationData;
/**
 * The City Migration model depicts the movement of people who have been displaced.
 * The displaced are moved between cities, which are connected with a spatial interaction network
 * and a transportation network.
 * 
 * @author Joey Harrison
 *
 */
public class CityMigrationModel extends SimState
{
	private static final long serialVersionUID = 1L;
	
	private final int width = 1694;
	private final int height = 1630;
	
	//Network cityInteractions = new Network(true);
	//Continuous2D cityField;
	
    /** Corresponds to cities, towns, and villages. Keyed by a name or unique ID. */
    public Map<String, PopulationCenter> populationCenters = new HashMap<String, PopulationCenter>();
    
    Parameters params;

    public CityTIN cityTIN;
//	GeomGridField urbanUseGrid = new GeomGridField();
//	GeomGridField landUseGrid = new GeomGridField();
    public GeomVectorField politicalBoundaries = new GeomVectorField(width, height);
	Envelope MBR;	// minimum bounding rectangle
	public RoadNetwork roadNetwork;
	public RoadNetwork roadNetworkSimp;
	public RoadNetwork roadNetworkSimpWeighted;
	public RoadNetwork cityInteractionNetwork;
	public RoadNetwork workingNetwork;		// this will just point at one of the others
	
	public Network shortestPathDebug = new Network(false);
	
	private DisplacementLogReader reader;
	public String displacementLogFilename;
	
	public String cityStatsLogFilename = "cityStats.csv";
	public CityStatsCollector cityStatsCollector;
	
	
	boolean dirty = false;		// has the network been changed since the last step?

	public double citySizeMultiplier = 0.1;
	public double getCitySizeMultiplier() { return citySizeMultiplier; }
	public void setCitySizeMultiplier(double val) { citySizeMultiplier = val; }
    public Object domCitySizeMultiplier() { return new Interval(1.0e-3, 1.0); }
    public String desCitySizeMultiplier() { return "This adjusts the size at which cities are drawn."; }
    
	public double roadNodeSizeMultiplier = 0.1;
	public double getRoadNodeSizeMultiplier() { return roadNodeSizeMultiplier; }
	public void setRoadNodeSizeMultiplier(double val) { roadNodeSizeMultiplier = val; }
    public Object domRoadNodeSizeMultiplier() { return new Interval(1.0e-3, 1.0); }
    
	public double cityIDPsSizeMultiplier = 0.5;
	public double getCityIDPsSizeMultiplier() { return cityIDPsSizeMultiplier; }
	public void setCityIDPsSizeMultiplier(double val) { cityIDPsSizeMultiplier = val; }
    public Object domCityIDPsSizeMultiplier() { return new Interval(1.0e-3, 10.0); }

	int minLinksPerCity = 1;    //minimum number of larger cities each city exaimines
    public int getMinLinksPerCity() { return minLinksPerCity; }
	public void setMinLinksPerCity(int val) { minLinksPerCity = val; dirty = true; }
    public Object domMinLinksPerCity() { return new Interval(0, 10); }

    int maxLinksPerCity = 200;    //maximum number of larger cities each city examines (400+ is all)
	public int getMaxLinksPerCity() { return maxLinksPerCity; }	
	public void setMaxLinksPerCity(int val) { maxLinksPerCity = val; dirty = true; }
    public Object domMaxLinksPerCity() { return new Interval(1, 400); }

    double thresholdLinks = .9; //include links until a certain percentage of interaction is accounted
	public double getThresholdLinks() { return thresholdLinks; }
	public void setThresholdLinks(double val) { thresholdLinks = val; dirty = true; }
    public Object domThresholdLinks() { return new Interval(0.0, 1.0); }

    double spatialDecayExp = 4.0; // exponent for spatial influence decay
                                  // range between .4 and 3.33 -- see Stillwell 1978 -- negative power function
                                  // higher values may be justifed because we are talking about emergency
                                  // relocation -- probably on foot, rather than relocation in Sweden or UK
    
	public double getSpatialDecayExp() { return spatialDecayExp; }
	public void setSpatialDecayExp(double val) { spatialDecayExp = val; dirty = true; }
    public Object domSpatialDecayExp() { return new Interval(0.0, 10.0); }

    double simplifyRoadsDistanceExp = 1.3; 
	public double getSimplifyRoadsDistanceExp() { return simplifyRoadsDistanceExp; }
	public void setSimplifyRoadsDistanceExp(double val) { simplifyRoadsDistanceExp = val; createSimpWeightedNetwork(); }
    //public Object domSimplifyRoadsDistanceExp() { return new Interval(0.0, 10.0); }

    public int initialIDPsPerCity = 0;
    public int getInitialIDPsPerCity() { return initialIDPsPerCity; }
    public void setInitialIDPsPerCity(int val) { initialIDPsPerCity = val; }
    
    public int IDPsAddedPerStep = 0;
    public int getIDPsAddedPerStep() { return IDPsAddedPerStep; }
    public void setIDPsAddedPerStep(int val) { IDPsAddedPerStep = val; }


    public int initialIDPsInCity = 0;
    public int getInitialIDPsInCity() { return initialIDPsInCity; }
    public void setInitialIDPsInCity(int val) { initialIDPsInCity = val; }
    
    public int initialIDPsInCityID = 3476;
    public int getInitialIDPsInCityID() { return initialIDPsInCityID; }
    public void setInitialIDPsInCityID(int val) { initialIDPsInCityID = val; }
    
    public double IDPLoadThreshold = 1.0;
    public double getIDPLoadThreshold() { return IDPLoadThreshold; }
    public void setIDPLoadThreshold(double val) { IDPLoadThreshold = val; }
    public Object domIDPLoadThreshold() { return new Interval(0.0, 2.0); }

    public double consumptionRate = 0.0;
    public double getConsumptionRate() { return consumptionRate; }
    public void setConsumptionRate(double val) { consumptionRate = val; }
    public Object domConsumptionRate() { return new Interval(0.0, 2.0); }
    
    public double travelPerDay = 1000;
    public double getTravelPerDay() { return travelPerDay; }
    public void setTravelPerDay(double val) { travelPerDay = val; }
    
    public boolean moveOneCityPerStep = true;
    public boolean getMoveOneCityPerStep() { return moveOneCityPerStep; }
    public void setMoveOneCityPerStep(boolean val) { moveOneCityPerStep = val; }

	public double IDPGroupSize = 0.1;
	public double getIDPGroupSize() { return IDPGroupSize; }
	public void setIDPGroupSize(double val) { IDPGroupSize = val; }
    public Object domIDPGroupSize() { return new Interval(0, 1.0); }
    
    public int networkFormationMethod = 0;
    public int getNetworkFormationMethod() { return networkFormationMethod; }
    public void setNetworkFormationMethod(int val) {
    	int oldVal = networkFormationMethod;
    	networkFormationMethod = val; 
    	if (cityInteractionNetwork != null) {
	    	initCityNodes();
	    	dirty = true;
    	}
		propertyChangeSupport.firePropertyChange("NetworkFormationMethod", oldVal, val);
    }
    public Object domNetworkFormationMethod() {
    	return new String[] { "Population", "Capacity", "Aggregate Capacity", "TIN" };
    }
    
    public int IDPsDestinationMethod = 5;
    public int getIDPsDestinationMethod() { return IDPsDestinationMethod; }
    public void setIDPsDestinationMethod(int val) {
//    	int oldVal = IDPsDestinationMethod;
    	IDPsDestinationMethod = val; 
//		propertyChangeSupport.firePropertyChange("IDPsDestinationMethod", oldVal, val);
    }
    public Object domIDPsDestinationMethod() {
    	return new String[] { "Spare Capacity", "Dist-Wtd Spare Capacity", "Nearest Viable", "Roads", "Hybrid (TIN)", "Hybrid (Roads)" };
    }
    
    public int pathTestCity1 = 0;
    public int getPathTestCity1() { return pathTestCity1; }
    public void setPathTestCity1(int val) { pathTestCity1 = val; testShortestPath(); }
    
    public int pathTestCity2 = 0;
    public int getPathTestCity2() { return pathTestCity2; }
    public void setPathTestCity2(int val) { pathTestCity2 = val; testShortestPath(); }
    
    public int workingNetworkIndex = 2;
    public int getWorkingNetwork() { return workingNetworkIndex; }
    public void setWorkingNetwork(int val) { 
    	int oldVal = workingNetworkIndex;
    	workingNetworkIndex = val;
    	switch (val) {
    	case 0:	workingNetwork = roadNetwork;				break;
    	case 1:	workingNetwork = roadNetworkSimp;			break;
    	case 2:	workingNetwork = roadNetworkSimpWeighted;	break;    	
    	}
		propertyChangeSupport.firePropertyChange("WorkingNetwork", oldVal, val);
		testShortestPath();
    }
    public Object domWorkingNetwork() {	return new String[] { "Road Network", "Simp. Network", "Simp. Wtd. Network" }; }

    public boolean analyzeRoadNetwork = false;
    public boolean getAnalyzeRoadNetwork() { return analyzeRoadNetwork; }
    public void setAnalyzeRoadNetwork(boolean val) {
    	analyzeRoadNetwork = val;
    	//if (analyzeRoadNetwork)
    	//	roadNetwork.calcAllPairsShortestPaths(true);
//    	workingNetwork.testAllPairsShortestPathIndices();
//    	workingNetwork.testAllPairsShortestPath_NextLinks();
    }
    
    public boolean getSaveAllPairsShortestPath() { return false; }
    public void setSaveAllPairsShortestPath(boolean val) {
		try{System.err.println("i'm trying to do the thing");
			//cityMigration.CityMigrationData.
			//System.err.println(new File(cityMigration.CityMigrationData.class.getResource("").toURI()).getAbsolutePath());

            }catch(Exception e){e.printStackTrace();}
    	roadNetwork.saveAllPairsShortestPath("/cityMigration/cityMigrationData/roadNetworkShortestPaths.ser");
    }

    public boolean getLoadAllPairsShortestPath() { return false; }
    public void setLoadAllPairsShortestPath(boolean val) {
    		roadNetwork.loadAllPairsShortestPath("/cityMigration/cityMigrationData/roadNetworkShortestPaths.ser");
    }

    public boolean getCreateSimplifiedRoadNetwork() { return false; }
    public void setCreateSimplifiedRoadNetwork(boolean val) {
    	System.out.println("Simplifying network...");
    	roadNetworkSimp = roadNetwork.createSimplifiedNetwork();
    	System.out.println("done");
    }

    public boolean getWriteSimplifiedRoadNetwork() { return false; }
    public void setWriteSimplifiedRoadNetwork(boolean val) {
    	System.out.println("Writing simplified road network to road_nodes_simp.txt and road_edges_simp.txt...");
    	roadNetworkSimp.writeNetwork("/cityMigration/cityMigrationData/road_nodes_simp.txt", "/cityMigration/cityMigrationData/road_edges_simp.txt");
    	System.out.println("done");
    }

    public boolean getReadSimplifiedRoadNetwork() { return false; }
    public void setReadSimplifiedRoadNetwork(boolean val) {
    	System.out.println("Reading simplified road network from road_nodes_simp.txt and road_edges_simp.txt...");
    	roadNetworkSimp.readRoadNetwork("/cityMigration/cityMigrationData/road_nodes_simp.txt", "/cityMigration/cityMigrationData/road_edges_simp.txt", MBR, false);
    	System.out.println("done");
    }

	public double getRoadProblemDistThreshold() { return roadNetwork.roadProblemDistThreshold; }
	public void setRoadProblemDistThreshold(double val) {
		roadNetwork.roadProblemDistThreshold = val;
		if (analyzeRoadNetwork && roadNetwork != null)
			roadNetwork.checkForRoadNetworkProblems(ignoreUnconnectedPairs, testCitiesOnly);
	}
    public Object domRoadProblemDistThreshold() { return new Interval(1.0, 10.0); }


    public boolean ignoreUnconnectedPairs = false;
    public boolean getIgnoreUnconnectedPairs() { return ignoreUnconnectedPairs; }
    public void setIgnoreUnconnectedPairs(boolean val) {
    	ignoreUnconnectedPairs = val;
		if (analyzeRoadNetwork && roadNetwork != null)
			roadNetwork.checkForRoadNetworkProblems(ignoreUnconnectedPairs, testCitiesOnly);
    }

    public boolean testCitiesOnly = false;
    public boolean getTestCitiesOnly() { return testCitiesOnly; }
    public void setTestCitiesOnly(boolean val) {
    	testCitiesOnly = val;
		if (analyzeRoadNetwork && roadNetwork != null)
			roadNetwork.checkForRoadNetworkProblems(ignoreUnconnectedPairs, testCitiesOnly);
    }

    public boolean readDisplacementEventLog = false;
    public boolean getReadDisplacementEventLog() { return readDisplacementEventLog; }
    public void setReadDisplacementEventLog(boolean val) { readDisplacementEventLog = val; }


	public PropertyChangeSupport propertyChangeSupport;
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	// =============================================================================================
	// This marks the end of the properties, setters, and getters
	// =============================================================================================


	/**
	 * This constructor gets called from CityMigrationModelWithUI.
	 * @param seed
	 */
	public CityMigrationModel(long seed) {
		this(seed, new String[] { "-file", "/riftland/riftlandData/Params/default.params" });
	}

	/**
	 * This constructor gets called from the other constructor and also from
	 * RiftlandWithMigration.
	 *
	 * @param seed
	 * @param args
	 */
	public CityMigrationModel(long seed, String[] args) {
		super(seed);

		params = new Parameters(args);
		propertyChangeSupport = new PropertyChangeSupport(this);

		readFiles();

		roadNetwork = new RoadNetwork(width, height);
		roadNetwork.readRoadNetwork("/cityMigration/cityMigrationData/road_nodes.txt", "/cityMigration/cityMigrationData/road_edges.txt", MBR, true);
//    	setLoadAllPairsShortestPath(true);	// read
//    	setCreateSimplifiedRoadNetwork(true);

    	roadNetworkSimp = new RoadNetwork("/cityMigration/cityMigrationData/road_nodes_simp.txt", "/cityMigration/cityMigrationData/road_edges_simp.txt", MBR, false);

    	if (cityStatsLogFilename != null)
    		cityStatsCollector = new CityStatsCollector(this, cityStatsLogFilename);
	}


	private void readFiles() {
        try {
            // Read in population center metadata

        	GeomGridField populationGrid = new GeomGridField(); // only used for its MBR
            InputStream populationStream = CityMigrationData.class.getResourceAsStream("/riftland/riftlandData/PopData/riftpopulation.asc");
            ArcInfoASCGridImporter.read(populationStream, GridDataType.INTEGER, populationGrid);
            populationStream.close();
            MBR = populationGrid.MBR;

        	cityTIN = new CityTIN("/cityMigration/cityMigrationData/tin/RLUrbNodes.shp", "/cityMigration/cityMigrationData/tin/RLUrbLinks.shp", width, height);

        	//File politicalBoundariesFile = new File("/riftland/riftlandData/political/RiftLand_Boundary.shp");
            URL boundaryFile = getUrl("/riftland/riftlandData/political/RiftLand_Boundary.shp");//politicalBoundariesFile.toURI().toURL();
            if (boundaryFile == null)
                throw new FileNotFoundException(boundaryFile.toString());
            ShapeFileImporter.read(boundaryFile, politicalBoundaries);
            
            MBR.expandToInclude(politicalBoundaries.getMBR());
            politicalBoundaries.setMBR(MBR);

        	Envelope tinMBR = cityTIN.edges.getMBR();
        	tinMBR.expandToInclude(cityTIN.nodes.getMBR());
        	tinMBR.expandToInclude(MBR);
        	
        	cityTIN.edges.setMBR(tinMBR);
        	cityTIN.nodes.setMBR(tinMBR);
        } 
        catch (Exception ex) { ex.printStackTrace(); }
	}
	
	private void initPopulationCentersFromSimplifiedRoadNetwork() {
		populationCenters.clear();

		for (PopulationCenter pc : roadNetworkSimp.getNodes()) {
			pc.setRefugeeCapacity(pc.getUrbanites() * 0.1);	
			populationCenters.put(Integer.toString(pc.getID()), pc);
		}
	}
	private static URL getUrl(String nodesFilename) throws IOException {
		InputStream nodeStream = CityMigrationData.class.getResourceAsStream(nodesFilename);
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
	private void init() {
		System.out.println("CityMigrationModel.init()...");
		System.out.format("minLinksPerCity: %d\n", minLinksPerCity);
		System.out.format("maxLinksPerCity: %d\n", maxLinksPerCity);
		System.out.format("thresholdLinks: %f\n", thresholdLinks);
		System.out.format("spatialDecayExp: %f\n", spatialDecayExp);
		
    	roadNetworkSimpWeighted = new RoadNetwork(roadNetworkSimp);
    	createSimpWeightedNetwork();
    	workingNetwork = roadNetworkSimpWeighted;
    	
    	for (PopulationCenter pc : roadNetworkSimpWeighted.getNodes())
			pc.setRefugeeCapacity(pc.getUrbanites() * 0.1);	

		cityInteractionNetwork = new RoadNetwork(width, height);
		
		initPopulationCentersFromSimplifiedRoadNetwork();

		for (PopulationCenter city : populationCenters.values()) {
//				System.out.format("%d, City: %s, Size: %d, pixels: %d, centroid: %s\n", index,
//						city.getName(), city.getUrbanites(), city.urbanAreas.size(), city.getCentroid());

			cityInteractionNetwork.addNode(city, city.getCentroid());
		}
		
		initCityNodes();
		buildCityNetwork();
		if (analyzeRoadNetwork)
			roadNetwork.checkForRoadNetworkProblems(ignoreUnconnectedPairs, testCitiesOnly);
	}
	
	private void initCityNodes() {
		boolean directed = (networkFormationMethod != 3); // everything but TIN is directed
		cityInteractionNetwork.edgeField.reset(directed);
		
		for (PopulationCenter city : populationCenters.values()) {
			city.clearRefugees();
			cityInteractionNetwork.edgeField.addNode(city);
		}
	}

        
	/**
	 * Build the city network based on which method the user has selected.
	 */
	private void buildCityNetwork() {

		switch (networkFormationMethod) {
		case 0:	// Population
			removeAllEdges(cityInteractionNetwork.edgeField);
			CityNetworkBuilder.buildCityNetworkFromPopulation(cityInteractionNetwork.edgeField, minLinksPerCity, maxLinksPerCity, thresholdLinks, spatialDecayExp); 
			break;
		case 1:	// Capacity
			removeAllEdges(cityInteractionNetwork.edgeField);
			CityNetworkBuilder.buildCityNetworkFromCapacity(cityInteractionNetwork.edgeField, minLinksPerCity, maxLinksPerCity, thresholdLinks, spatialDecayExp); 
			break;			
		case 3: // TIN
			removeAllEdges(cityInteractionNetwork.edgeField);
        	ArrayList<CityEdge> edges = cityTIN.matchEdges();
        	CityNetworkBuilder.buildCityNetworkFromEdgeList(cityInteractionNetwork.edgeField, edges, thresholdLinks, spatialDecayExp);
        	dirty = false;
        	break;
		}
		
		printNetworkStats();
	}
	
	/**
	 * Simplify the road network by penalizing long roads. We do this by raising the length
	 * of the roads to an exponent, then finding the shortest paths between cities using the
	 * weighted distances.
	 */
	private void createSimpWeightedNetwork() {
		
		System.out.format("Creating simplified weighted network with weight: %f\n", simplifyRoadsDistanceExp);
		RoadNetwork temp = new RoadNetwork(roadNetworkSimp);

		int n = temp.edgeField.allNodes.numObjs;
		Edge[][] edgeMatrix = temp.edgeField.getAdjacencyMatrix();
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++) 
				if (edgeMatrix[i][j] != null)
					edgeMatrix[i][j].setInfo(Math.pow(edgeMatrix[i][j].getWeight(), simplifyRoadsDistanceExp));
		
		temp.copySimplifiedEdgesTo(roadNetworkSimpWeighted);
		
		// Return the edges back to their original scale (in kilometers)

		n = roadNetworkSimpWeighted.edgeField.allNodes.numObjs;
		edgeMatrix = roadNetworkSimpWeighted.edgeField.getAdjacencyMatrix();
		double invExp = 1.0 / this.simplifyRoadsDistanceExp;
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++) 
				if (edgeMatrix[i][j] != null)
					edgeMatrix[i][j].setInfo(Math.pow(edgeMatrix[i][j].getWeight(), invExp));
	}
	
	private void printNetworkStats() {
		int edgeCount = 0;

		Iterator<?> i = cityInteractionNetwork.edgeField.indexOutInHash.values().iterator();
		while (i.hasNext()) {
			IndexOutIn ioi = (IndexOutIn)i.next();
			if (ioi.out != null)
				edgeCount += ioi.out.numObjs;
		}
		
		System.out.format("cityInteractions.edgeCount: %d\n", edgeCount);
	}
	
	/**
	 * Remove all edges from the network. 
	 * Note: this function belongs in Network.java and is only here to avoid build
	 * errors for the people who are working from an old MASON jar.
	 * 
	 */
	private void removeAllEdges(Network network) {
        int n = network.allNodes.numObjs;
		Iterator<?> i = network.indexOutInHash.values().iterator();
		for (int k = 0; k < n; k++) {
			IndexOutIn ioi = (IndexOutIn)i.next();
			if (ioi.in != null)		ioi.in.clear();
			if (ioi.out != null)	ioi.out.clear();
		}
	}

	private void clearIDPs() {
		for (PopulationCenter city : populationCenters.values())
			city.setNumRefugees(0);
	}
	
	/** Add the given number of IDPs to each city. */
	private void addIDPs(int num) {
		for (PopulationCenter city : populationCenters.values())
			addIDPsInCity(city, num);
	}

	/** Add the given number of IDPs to the given city. */
	private void addIDPsInCity(PopulationCenter city, int numRefugees) {
		int householdSize = 10;
		for (; numRefugees > 0; numRefugees -= householdSize) {
			int size = (numRefugees >= householdSize) ? householdSize : numRefugees;
			Household h = Household.createDisplacedHousehold(params, 0, 0, size);
			city.addRefugees(h);
		}
	}

	public PopulationCenter findDestinationForIDPs(PopulationCenter city) {
		switch(IDPsDestinationMethod) {		
		case 0:	return findDestinationForIDPs_SpareCapacity(city);
		case 1:	return findDestinationForIDPs_DistanceWeightedSpareCapacity(city);
		case 2: return findDestinationForIDPs_NearestViableCity(city);
		case 3: return findDestinationForIDPs_Roads(city);
		case 4: return findDestinationForIDPs_HybridTIN(city);
		case 5: return findDestinationForIDPs_HybridRoads(city);
		}		
		
		return null;	// this shouldn't ever happen		
	}

	/**
	 * Find the given city's neighbor with the most spare capacity (in persons).
	 */
	private PopulationCenter findDestinationForIDPs_SpareCapacity(PopulationCenter city) {
		// Choose city
		PopulationCenter bestNeighbor = null;
		double largestSpareCapacity = Double.NEGATIVE_INFINITY;
		Bag edges = cityInteractionNetwork.edgeField.getEdgesOut(city);
		for (int i = 0; i < edges.numObjs; i++) {
			Edge e = (Edge)edges.get(i);
			PopulationCenter toCity = (PopulationCenter)e.getOtherNode(city);
			if ((toCity != city) && (toCity.getSpareRefugeeCapacity() > largestSpareCapacity)) {
				largestSpareCapacity = toCity.getSpareRefugeeCapacity();
				bestNeighbor = toCity;
			}
		}
		
		return bestNeighbor;
	}


	/**
	 * Find the given city's neighbor with the most spare capacity (in persons).
	 */
	private PopulationCenter findDestinationForIDPs_DistanceWeightedSpareCapacity(PopulationCenter city) {
		Bag edges = cityInteractionNetwork.edgeField.getEdgesOut(city);
		if (edges.numObjs == 0) {
			System.out.format("City %d has no outbound edges in the interaction network.\n", city.getID());
			return null;
		}

		double[] weights = new double[edges.numObjs];
		double lowestWeight = Double.POSITIVE_INFINITY;
		for (int i = 0; i < edges.numObjs; i++) {
			Edge e = (Edge) edges.get(i);
			PopulationCenter toCity = (PopulationCenter) e.getOtherNode(city);
			double dist = city.getCentroid().distance(toCity.getCentroid());
			weights[i] = toCity.getSpareRefugeeCapacity() / Math.pow(dist, spatialDecayExp);
			if (weights[i] < lowestWeight) lowestWeight = weights[i];
		}

		if (lowestWeight < 0) {
			for (int i = 0; i < weights.length; i++)
				weights[i] -= lowestWeight; // calm down, it's negative
		}

		int index = Utilities.chooseStochastically(weights, random);
		Edge e = (Edge) edges.get(index);
		return (PopulationCenter) e.getOtherNode(city);
	}

	/**
	 * Find the viable city nearest to the given city (not necessarily a neighbor).
	 * A city is "viable" if it has enough spare capacity to handle the number of IDPs the given
	 * city wants to send.
	 */
	private PopulationCenter findDestinationForIDPs_NearestViableCity(PopulationCenter city) {
		int numIDPs = (int)(city.getNumRefugees() * IDPGroupSize);
		
		PopulationCenter nearestViableCity = null;
		double minDistance = Double.MAX_VALUE;
		// find nearest city with spare capacity
		for (PopulationCenter toCity : populationCenters.values())
			if ((toCity != city) && (toCity.getSpareRefugeeCapacity() >= numIDPs)) {
				double distance = city.getCentroid().distance(toCity.getCentroid());
				if (distance < minDistance) {
					minDistance = distance;
					nearestViableCity = toCity;
				}
			}
		
		if (nearestViableCity == null)
			return null;
		
		// send IDPs there
		ArrayList<PopulationCenter> path = workingNetwork.getPath(city, nearestViableCity);
		if (path == null)
			return null;
		
		// if path is empty, go directly there
		PopulationCenter dest = nearestViableCity;

		if (path.size() > 0)
			dest = path.get(0);
		
		return dest;
	}

	/**
	 * Find a destination for some IDPs using a hybrid method. First we find a city
	 * based on spare capacity, then we plan the shortest path to get there. The 
	 * destination city is the first hop along that path.
	 */
	private PopulationCenter findDestinationForIDPs_HybridTIN(PopulationCenter city) {
		
		PopulationCenter dest = findDestinationForIDPs_DistanceWeightedSpareCapacity(city);
		
		if (dest == null)
			return null;

		//TODO this needs to be fixed so that it actually uses the TIN
		ArrayList<PopulationCenter> path = workingNetwork.getPath(city, dest);
		if (path == null)
			return null;
		
		// if the path has any intermediate steps, go to the first one
		if (path.size() > 0)
			dest = path.get(0);
		
		return dest;
	}

	/**
	 * Find a destination for some IDPs using a hybrid method. First we find a city
	 * based on spare capacity, then we plan the shortest path to get there. The 
	 * destination city is the first hop along that path.
	 */
	private PopulationCenter findDestinationForIDPs_HybridRoads(PopulationCenter city) {
		PopulationCenter dest = findDestinationForIDPs_DistanceWeightedSpareCapacity(city);
		return calcFirstStepOnPath(city, dest);
	}
	
	/**
	 * Find a destination for some IDPs using the road network alone.
	 */
	private PopulationCenter findDestinationForIDPs_Roads(PopulationCenter city) {
		Bag edges = workingNetwork.edgeField.getEdgesOut(city);
		double[] weights = new double[edges.numObjs];
		double lowestWeight = Double.POSITIVE_INFINITY;
		for (int i = 0; i < edges.numObjs; i++) {
			Edge e = (Edge) edges.get(i);
			PopulationCenter toCity = (PopulationCenter) e.getOtherNode(city);
			double dist = city.getCentroid().distance(toCity.getCentroid());
			weights[i] = toCity.getSpareRefugeeCapacity() / Math.pow(dist, spatialDecayExp);
			if (weights[i] < lowestWeight) lowestWeight = weights[i];
		}

		if (lowestWeight < 0) {
			for (int i = 0; i < weights.length; i++)
				weights[i] -= lowestWeight; // calm down, it's negative
		}

		int index = Utilities.chooseStochastically(weights, random);
		Edge e = (Edge) edges.get(index);
		return (PopulationCenter) e.getOtherNode(city);
	}
	
	/**
	 * Calculate and return the first city in the shortest path from src to dest.
	 * @param src Source city
	 * @param dest Destination city
	 * @return First city along shortest path. May be dest. If no path exists, will be null.
	 */
	public PopulationCenter calcFirstStepOnPath(PopulationCenter src, PopulationCenter dest) {
		if (dest == null)
			return null;
		
		if (IDPsDestinationMethod <= 3)		// Spare Capacity, Distance-Weighted Spare Capacity, or Nearest Viable
			return dest;

		ArrayList<PopulationCenter> path = workingNetwork.getPath(src, dest);
		if (path == null)
			return null;
		
		// if the path has any intermediate steps, go to the first one
		if (path.size() > 0)
			dest = path.get(0);
		
		return dest;
	}
	
	private void expellIDPs(PopulationCenter city) {
		PopulationCenter dest = findDestinationForIDPs(city);
		if (dest == null) {
			System.out.println("CityMigrationModel.expellIDPS: can't find destination for IDPs, so they're staying in city " + city.getID());
			return;
		}
		
//		int numIDPs = (int)(city.getNumRefugees() * IDPGroupSize);
		int numIDPs = city.getNumRefugees() - (int)city.getRefugeeCapacity();
		
		numIDPs -= city.getRefugeeGroupPopulation();	// subtract the refugees already in groups. they're leaving anyway
		
		while (numIDPs > 0) {
			RefugeeGroup rg = city.spawnRefugeeGroup(1, random);	// TODO make the number of households variable
			rg.destination = dest;
			schedule.scheduleOnceIn(0, rg);
			numIDPs -= rg.getPopulation();
		}		
	}
	
	/**
	 * Choose and return the better of the two destinations based on distance-weighted spare capacity.
	 */
	public PopulationCenter chooseBetterDestination(PopulationCenter src, PopulationCenter dest1, PopulationCenter dest2) {
		if (dest1 == null)
			return dest2;
		if (dest2 == null)
			return dest1;
    	double d1 = workingNetwork.getPathLength(src, dest1);
    	double d2 = workingNetwork.getPathLength(src, dest2);
    	double wd1 = dest1.getSpareRefugeeCapacity() / Math.pow(d1, spatialDecayExp);
    	double wd2 = dest2.getSpareRefugeeCapacity() / Math.pow(d2, spatialDecayExp);

    	return (wd1 > wd2) ? dest1 : dest2;
	}
	
	public void processEvent(DisplacementEvent event) {

//		System.out.format("Step %.2f: %s\n", schedule.getTime(), event.toString());
		
		// -- Find nearest city to parcelX,parcelY
		Double2D pos = new Double2D(event.parcelX, event.parcelY);
		Bag nearestCities = new Bag();
		cityInteractionNetwork.nodeField.getNearestNeighbors(pos, 1, false, false, true, nearestCities);

		PopulationCenter nearestCity = null;

		if (nearestCities.isEmpty())
			System.out.println("Error: CityMigrationModel.processEvent - getNearestNeighbors found nothing.");
		else {
			double shortestDistance = Double.MAX_VALUE;
			
			for (int i = 0; i < nearestCities.numObjs; i++) {
				PopulationCenter city = (PopulationCenter)nearestCities.get(i);
				double d = pos.distance(city.getCentroid());
				if (d < shortestDistance) {
					shortestDistance = d;
					nearestCity = city;
				}
			}
		}
		
		// -- Place the displaced household into the nearest city
		if (nearestCity != null)
			nearestCity.addRefugees(Household.createDisplacedHousehold(params, event.culture, event.citizenship, event.groupSize));		
	}

	@SuppressWarnings("serial")
	@Override
	public void start() {
		super.start();

		init();
		clearIDPs();
		addIDPs(initialIDPsPerCity);
		// if the user would like to add IDPs to a specific city at the start, do so
		if (initialIDPsInCityID != -1) {
			PopulationCenter city = populationCenters.get(Integer.toString(initialIDPsInCityID));
			if (city != null)
				addIDPsInCity(city, initialIDPsInCity);
		}
		

		// Update the network if it's been changed
		schedule.scheduleRepeating(0, 0, new Steppable() {
			public void step(SimState state) {
				if (dirty) {
					dirty = false;
					buildCityNetwork();
					propertyChangeSupport.firePropertyChange("Network", 1, 2);
				}
			}
		});

		schedule.scheduleRepeating(0, 1, new Steppable() {
			public void step(SimState state) {
				// add IDPs to the system
				addIDPs(IDPsAddedPerStep);
			}
		});
		
		// -- Read the log file with displacement events and schedule them up
		if (readDisplacementEventLog) {
			reader = new DisplacementLogReader(displacementLogFilename);
			
			schedule.scheduleOnce(new Steppable() {
				DisplacementEvent event = null;
				public void step(SimState state) {
					if (event != null)
						processEvent(event);

					// process all the events at this timestep
					while (((event = reader.getNextEvent()) != null) && (event.timestamp <= state.schedule.getTime()))
						processEvent(event);
					
					if (event != null)
						state.schedule.scheduleOnce(event.timestamp, this);
				}
			});
		}

		// Migration logic
		int start = 0;
		for (PopulationCenter _city : populationCenters.values())
			schedule.scheduleRepeating(start, 3, new CityWrapper(_city) {
				public void step(SimState state) {
					// the refugees have to eat
					city.consumeCapacity(consumptionRate);
					if (city.getRefugeeLoad() > IDPLoadThreshold)
						expellIDPs(city);
				}
			});
		
		if (cityStatsCollector != null) {
			cityStatsCollector.start();
			schedule.scheduleRepeating(cityStatsCollector, 4, 1);
		}
	}
		
	private void testShortestPath() {			
		try {
			shortestPathDebug.clear();
			PopulationCenter c1 = workingNetwork.getCity(pathTestCity1);
			if (c1 == null) {
				System.out.format("No city found with ID: %d\n", pathTestCity1);
				return;
			}
			PopulationCenter c2 = workingNetwork.getCity(pathTestCity2);
			if (c2 == null) { 
				System.out.format("No city found with ID: %d\n", pathTestCity2);
				return;
			}

			ArrayList<PopulationCenter> path = workingNetwork.getPath(c1, c2);
		
			System.out.format("Path from %d to %d: ", pathTestCity1, pathTestCity2);
			if (path == null)
				System.out.println("null");
			else {
				for (PopulationCenter c : path)
					System.out.format(" %d", c.getID());
				System.out.println();
				System.out.format("Total path distance:    %f\n", workingNetwork.getPathLength(c1, c2));
				System.out.format("Crow distance:       %f\n", c1.getCentroid().distance(c2.getCentroid()));
			}
			System.out.format("Dijkstra distance:   %f\n", NetworkStatistics.getShortestPath(workingNetwork.edgeField, c1, c2, WeightedEdgeMetric.defaultInstance));
			
			// visualize the shortest path
			if (path != null) {
				path.add(0, c1);
				path.add(c2);
				PopulationCenter prev = null;
				for (PopulationCenter c : path) {
					if (prev != null)
						shortestPathDebug.addEdge(prev, c, null);
					prev = c;
				}
				
				//testNetworkList();
						
			}
		}
		catch (Exception ex) { ex.printStackTrace(); }
	}
	
	private void testNetworkList() {

		int n = workingNetwork.edgeField.getAllNodes().numObjs;
		Edge[][] edgeMatrix = workingNetwork.edgeField.getAdjacencyMatrix();
		System.out.println("Number of nodes: " + n);

		int count = 0;
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++) {
				if (edgeMatrix[i][j] != null)
					System.out.format("Edge (%4d): %d,%d\n", count++, ((PopulationCenter)edgeMatrix[i][j].from()).getID(), ((PopulationCenter)edgeMatrix[i][j].to()).getID());
			}
	}
	
	@Override
	public void finish() {
		super.finish();
		
		if (reader != null)
			reader.closeFile();
		
		if (cityStatsCollector != null)
			cityStatsCollector.close();
	}
	
	public static void main(String[] args) {
        doLoop(CityMigrationModel.class, args);
        System.exit(0);
	}

}
