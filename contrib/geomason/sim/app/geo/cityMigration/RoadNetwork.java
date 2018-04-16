package cityMigration;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;


import cityMigration.cityMigrationData.CityMigrationData;
import com.vividsolutions.jts.geom.Envelope;

import riftland.PopulationCenter;
import riftland.World;
import sim.field.continuous.Continuous2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.geo.GeometryUtilities;

public class RoadNetwork
{
//	World world;

	Network edgeField = new Network(false);
	public Continuous2D nodeField;
	private ArrayList<PopulationCenter> nodes;

	Network roadProblemField = new Network(false);
	public AllPairsShortestPath allPairsShortestPaths;
	public double roadProblemDistThreshold = 2;

	private HashMap<PopulationCenter, Integer> cityToIndexMap = new HashMap<PopulationCenter, Integer>();
	private HashMap<Integer, PopulationCenter> indexToCityMap = new HashMap<Integer, PopulationCenter>();
	private HashMap<Integer, PopulationCenter> idToCityMap = new HashMap<Integer, PopulationCenter>();

	
	/**
	 * Create an empty RoadNetwork.
	 */
	public RoadNetwork(double width, double height) {
		edgeField = new Network(false);
		nodeField = new Continuous2D(Double.MAX_VALUE, width, height);
		nodes = new ArrayList<PopulationCenter>();
	}
	
	public RoadNetwork(RoadNetwork other) {
		nodes = new ArrayList<PopulationCenter>();
		nodeField = new Continuous2D(Double.MAX_VALUE, other.nodeField.getWidth(), other.nodeField.getHeight());
		for (PopulationCenter c : other.nodes)
			this.addNode(c, c.getCentroid());
		this.edgeField = new Network(other.edgeField);
		fixIndices();
	}
	
	public RoadNetwork(String nodesFilename, String edgesFilename, Envelope MBR, boolean reproject) {
		edgeField = new Network(false);
		//nodeField = new Continuous2D(Double.MAX_VALUE, world.getLand().getWidth(), world.getLand().getHeight());
		nodeField = new Continuous2D(Double.MAX_VALUE, MBR.getWidth(), MBR.getHeight());
		nodeField = new Continuous2D(Double.MAX_VALUE, 1694, 1630);
		nodes = new ArrayList<PopulationCenter>();
		readRoadNetwork(nodesFilename, edgesFilename, MBR, reproject);
	}
	
	public RoadNetwork(String nodesFilename, String edgesFilename, Envelope MBR) {
		this(nodesFilename, edgesFilename, MBR, true);
	}
	
	public final ArrayList<PopulationCenter> getNodes() {
		return nodes;
	}
	
	public void addNode(PopulationCenter node, Double2D location) {
		nodes.add(node);
		nodeField.setObjectLocation(node, location);
		idToCityMap.put(node.getID(), node);
	}
	
	/***
	 * Make it so the indices in the hash tables match the indices in the network. 
	 * This must be called after adding edges.
	 */
	private void fixIndices() {
		cityToIndexMap.clear();
		indexToCityMap.clear();
		for (PopulationCenter c : nodes) {
			int index = edgeField.getNodeIndex(c);
			cityToIndexMap.put(c, index);
			indexToCityMap.put(index, c);			
		}
	}
	
	public PopulationCenter getCity(int ID) {
		return idToCityMap.get(ID);
	}
	
	/**
	 * Calculating the all-pairs-shortest-path can take a long time (it's N^3),
	 * and this function allows us to delay the calculation until we need it.
	 */
	private void calcAllPairsShortestPaths(boolean showProgress, boolean force) {
		if (allPairsShortestPaths != null && !force)
			return;		// already initialized

		fixIndices();
		allPairsShortestPaths = new AllPairsShortestPath(edgeField, showProgress);
	}
	
	/**
	 * Calculating the all-pairs-shortest-path can take a long time (it's N^3),
	 * and this function allows us to delay the calculation until we need it.
	 */
	private void calcAllPairsShortestPaths(boolean showProgress) {
		calcAllPairsShortestPaths(showProgress, false);
	}
	
	/**
	 * Read in the road network from the nodes and edges files and put them into a network.
	 * The nodes must be projected from Lat/Lon to world coordinates (0-1694, 0-1630).
	 */
	public void readRoadNetwork(String nodesFilename, String edgesFilename, Envelope MBR, boolean reproject) {
		try {
			edgeField.clear();
			nodeField.clear();
			nodes.clear();

//			Envelope MBR = world.getPopulation().getPopulationGrid().getMBR();
			Rectangle2D.Double viewport = new Rectangle2D.Double(0, 0, 1694, 1630);
			AffineTransform transform = GeometryUtilities.worldToScreenTransform(MBR, viewport);
			double degToMeters = Double.NaN;
			Point2D point1 = null, point1M = null;
			
			// --- Read the nodes
			BufferedReader fin = new BufferedReader(new InputStreamReader(CityMigrationData.class.getResourceAsStream(
			        nodesFilename), "UTF-8"));
			String s;
			s = fin.readLine();		// skip the headers
			
			while ((s = fin.readLine()) != null) {
				String [] tokens = s.split(",", 7);
				double x = Double.parseDouble(tokens[1].trim());
				double y = Double.parseDouble(tokens[2].trim());
				int id = Integer.parseInt(tokens[3].trim()); 	
				int pop = Integer.parseInt(tokens[4].trim());
				String name = tokens[5].trim().replaceAll("^\"|\"$", ""); // get rid of the quote marks
				
				Point2D pt = new Point2D.Double(x,y);
				Point2D newPt = new Point2D.Double();
				transform.transform(pt, newPt);
				Double2D loc = new Double2D(reproject ? newPt : pt);
				
				PopulationCenter pc = new PopulationCenter(name, id, pop, loc);
				
				addNode(pc, loc);
				
				// init the conversion factor TODO take this out after Ates redoes the file in meters
				if (point1 == null) {
					point1 = pt;
					point1M = newPt;
				}
				else if (Double.isNaN(degToMeters)) {
					double degDist = point1.distance(pt);
					double meterDist = point1M.distance(newPt);
					degToMeters = meterDist / degDist;
				}
			}
			fin.close();
			
			// --- Read the edges
			fin = new BufferedReader(new InputStreamReader(CityMigrationData.class.getResourceAsStream(
                    edgesFilename), "UTF-8"));;
			s = fin.readLine();		// skip the headers
			while ((s = fin.readLine()) != null) {
				String [] tokens = s.split(",", 7);
				int fromNode = Integer.parseInt(tokens[1].trim());	// make the index zero-based
				int toNode = Integer.parseInt(tokens[2].trim());	// make the index zero-based
				double length = Double.parseDouble(tokens[3].trim());

				PopulationCenter node1 = idToCityMap.get(fromNode);		
				PopulationCenter node2 = idToCityMap.get(toNode);
				
				// convert from the file's coordinate system to the model's.
				// Note: this may not actually be degrees to meters, but it doesn't matter
				// since it's just a conversion factor.
				edgeField.addEdge(node1, node2, length * degToMeters);
			}
			
			fin.close();
			fixIndices();
			allPairsShortestPaths = null;
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * Look for nodes that possibly should be connected that aren't. As a first
	 * approximation, we'll identify pairs of nodes for which the road distance 
	 * is more than twice the straight-line distance.
	 */
	public void checkForRoadNetworkProblems(boolean ignoreUnconnectedPairs, boolean testCitiesOnly) {
		System.out.println("checkForRoadNetworkProblems - start");
		roadProblemField.clear();
		PopulationCenter nearest;
		double nearestDist;
		int count = 0;
		
		calcAllPairsShortestPaths(false);

		for (PopulationCenter node1 : nodes) {
			if (testCitiesOnly && (node1.getUrbanites() == 0))
				continue;
			nearest = null;
			nearestDist = Double.POSITIVE_INFINITY;
			for (PopulationCenter node2 : nodes) {
				if (testCitiesOnly && (node2.getUrbanites() == 0))
					continue;
				if (node1 != node2) {
					double roadDist = allPairsShortestPaths.getPathLength(cityToIndexMap.get(node1), cityToIndexMap.get(node2));
					double crowDist = node1.getCentroid().distance(node2.getCentroid());
					if ((!Double.isInfinite(roadDist) || !ignoreUnconnectedPairs) && (roadDist > roadProblemDistThreshold*crowDist))
						if (crowDist < nearestDist) {
							nearest = node2;
							nearestDist = crowDist;
						}							
				}
			}
			if (nearest != null) {
				roadProblemField.addEdge(node1, nearest, nearestDist);
				count++;
			}
		}
		System.out.format("checkForRoadNetworkProblems - done  (%d potential problems)", count);
	}
	
	/**
	 * Check to see if two nodes are neighboring cities. If there is not path between them,
	 * of if the shortest path between them contains other cities, then they are not.
	 */
	private boolean areNeighboringCities(PopulationCenter A, PopulationCenter B) {
		if (A == B)
			return false;

		ArrayList<Integer> SP = allPairsShortestPaths.getPath(cityToIndexMap.get(A), cityToIndexMap.get(B));
		if (SP == null)	// no path
			return false;
		
		for (Integer i : SP) {
			PopulationCenter node = indexToCityMap.get(i);
			if ((node == A) || (node == B))
				continue;
			if (node.getUrbanites() > 0)
				return false;
		}
		
		return true;
	}
	
	public RoadNetwork createSimplifiedNetwork() {
		/*
		 * Each node is either a city node (population > 0) or a non-city node (population == 0).
		 * We need to create a simplified network that contains only the city nodes, 
		 * with edges between city nodes that don't have a city between them.
		 * More specifically:
		 * 	Assume city nodes A, B
		 * 	Let SP be the shortest path between A and B
		 * 	if all the nodes in SP (excluding A and B) are non-city nodes, 
		 * 		connect A and B with an edge with length = the summed length of SP (including A and B)
		 */
		
		RoadNetwork simp = new RoadNetwork(nodeField.getWidth(), nodeField.getHeight());

		ArrayList<PopulationCenter> simpNodes = new ArrayList<PopulationCenter>();
		
		// create a new list containing just the city nodes
		for (PopulationCenter node : nodes)
			if (node.getUrbanites() > 0)
				//simpNodes.add((PopulationCenter)node.clone());
				simpNodes.add(node);

		ArrayList<Edge> simpEdges = getSimplifiedEdges(simpNodes);
	
		// copy the simplifed nodes and edges to the new RoadNetwork
		for (PopulationCenter node : simpNodes) {
			simp.addNode(node, node.getCentroid());
		}
		
		for (Edge edge : simpEdges)
			simp.edgeField.addEdge(edge);
		
		simp.fixIndices();
//		simp.testAllPairsShortestPathIndices();
		
		return simp;
	}
	
	/**
	 * Get the simplified set of edges. Breaking this function out allows it to be used seperately. 
	 * @param simpNodes
	 * @return
	 */
	private ArrayList<Edge> getSimplifiedEdges(ArrayList<PopulationCenter> simpNodes) {
		calcAllPairsShortestPaths(false);
		ArrayList<Edge> simpEdges = new ArrayList<Edge>();
		
		for (int i = 0; i < simpNodes.size(); i++)
			for (int j = i+1; j < simpNodes.size(); j++) {	// start at i+1 to avoid duplicate edges (A <-> B and B <-> A)
				PopulationCenter A = simpNodes.get(i);
				PopulationCenter B = simpNodes.get(j);
				if (areNeighboringCities(A, B))
					simpEdges.add(new Edge(A, B, allPairsShortestPaths.getPathLength(cityToIndexMap.get(A), cityToIndexMap.get(B))));				
			}
		
		return simpEdges;
	}
	
	public void copySimplifiedEdgesTo(RoadNetwork other) {
		ArrayList<PopulationCenter> simpNodes = new ArrayList<PopulationCenter>();
		
		// create a new list containing just the city nodes
		for (PopulationCenter node : nodes)
			if (node.getUrbanites() > 0)
				simpNodes.add(node);

		ArrayList<Edge> simpEdges = getSimplifiedEdges(simpNodes);
		
		other.edgeField.clear();
		for (Edge edge : simpEdges)
			other.edgeField.addEdge(edge);
				
		other.fixIndices();
		other.allPairsShortestPaths = null;
	}
	
	public ArrayList<Edge> getEdgeList() {
		Edge[][] adjacencyList = this.edgeField.getAdjacencyList(true);
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for (int i = 0; i < adjacencyList.length; i++)
			for (int j = 0; j < adjacencyList[i].length; j++)
				if (!edges.contains(adjacencyList[i][j]))
					edges.add(adjacencyList[i][j]);
		
		return edges;
	}
	
	public void writeNetwork(String nodesFilename, String edgesFilename) {
		ArrayList<Edge> edges = getEdgeList();
		writeNodesToFile(nodes, nodesFilename);
		writeEdgesToFile(edges, edgesFilename);
	}

	private void writeNodesToFile(ArrayList<PopulationCenter> _nodes, String filename) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(filename));
			out.println("FID_,ET_X,ET_Y,RD_ID,Population,Name");	// write the headers
			
			// ,36.681000,-5.771310,1,0," "
			for (PopulationCenter node : _nodes) 
				out.format(",%.6f,%.6f,%d,%d,\"%s\"\n", node.getCentroid().x, node.getCentroid().y, 
						node.getID(),
						node.getUrbanites(), node.getName());
			
			out.close();
		}
		catch (IOException e) {	e.printStackTrace(); }
	}
	
	private void writeEdgesToFile(ArrayList<Edge> _edges, String filename) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(filename));
			out.println("FID_,FNODE_,TNODE_,LengthKM");	// write the headers
			
			// ,1,2,0.078088
			for (Edge edge : _edges) {
				int from = ((PopulationCenter)edge.from()).getID();	
				int to = ((PopulationCenter)edge.to()).getID();		

				out.format(",%d,%d,%f\n", from, to, edge.getWeight());
			}
			
			out.close();
		}
		catch (IOException e) {	e.printStackTrace(); }
	}
	
    public void saveAllPairsShortestPath(String filename) {		
		calcAllPairsShortestPaths(true);
    	System.out.println("Saving all-pairs shortest-path...");
		AllPairsShortestPath.writeToFile(allPairsShortestPaths, filename);
    	System.out.println("done");		
    }
    
    public void loadAllPairsShortestPath(String filename) { 
    	System.out.println("Loading all-pairs shortest-path...");
    	allPairsShortestPaths = AllPairsShortestPath.readFromFile(filename);
    	System.out.println("done");
    }
	
	public ArrayList<PopulationCenter> getPath(PopulationCenter source, PopulationCenter dest) {
		calcAllPairsShortestPaths(false);
		ArrayList<PopulationCenter> path = new ArrayList<PopulationCenter>();

		int s = cityToIndexMap.get(source);
		int d = cityToIndexMap.get(dest);
		ArrayList<Integer> indexList = allPairsShortestPaths.getPath(s, d);
		if (indexList == null)
			return null;
		for (Integer i : indexList)
			path.add(indexToCityMap.get(i));
				
		return path;
	}
	
	public double getPathLength(PopulationCenter source, PopulationCenter dest) {
		calcAllPairsShortestPaths(false);
		int s = cityToIndexMap.get(source);
		int d = cityToIndexMap.get(dest);
		return allPairsShortestPaths.getPathLength(s, d);		
	}
	
	public void testAllPairsShortestPathIndices() {
		
		for (PopulationCenter c : nodes) {
			System.out.format("City ID: %d, index: %d, Network.getNodeIndex: %d\n", c.getID(), cityToIndexMap.get(c), edgeField.getNodeIndex(c));
		}
		
		Edge[][] edgeMatrix = edgeField.getAdjacencyMatrix();
		int n = edgeMatrix.length;
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++) {
				Edge e = edgeMatrix[i][j];
				if (e == null)
					continue;
				
				PopulationCenter c1 = (PopulationCenter)e.from();
				PopulationCenter c2 = (PopulationCenter)e.to();
				
				PopulationCenter c1b = indexToCityMap.get(i);
				PopulationCenter c2b = indexToCityMap.get(j);
				
				if ((c1 != c1b) || (c2 != c2b)) {
					if ((c1 == c2b) && (c2 == c1b))
						continue;
					System.out.format("EdgeMatrix non-match: %d, %d != %d, %d\n", c1.getID(), c2.getID(), c1b.getID(), c2b.getID());
				}
			}
		System.out.println("done.");
	}

	public void testAllPairsShortestPath_NextLinks() {
		int n = nodes.size();
		for (int i = 0; i < n; i++)
			for (int j = i+1; j < n; j++) {
				int index1 = cityToIndexMap.get(nodes.get(i));
				int index2 = cityToIndexMap.get(nodes.get(j));
				int next = allPairsShortestPaths.next[index1][index2];
				if (next == -1)
					continue;
				Bag edges = edgeField.getEdgesOut(nodes.get(i));
				boolean found = false;
				for (int k = 0; k < edges.size() && !found; k++) {
					int other = cityToIndexMap.get(((Edge)edges.get(k)).getOtherNode(nodes.get(i)));
					if (other == next)
						found = true;
				}
				if (!found)
					System.out.format("From: %d, to: %d, next: %d not found\n", index1, index2, next);
			}
	}
	
	public ArrayList<Edge> severRoads(int x1, int y1, int x2, int y2) {

		// get all the edges out of the network
		ArrayList<Edge> allEdges = new ArrayList<Edge>();
		Bag allNodes = edgeField.getAllNodes();
		for (int i = 0; i < allNodes.numObjs; i++) {
			Bag edges = edgeField.getEdgesOut(allNodes.get(i));
			for (int j = 0; j < edges.numObjs; j++)
				allEdges.add((Edge)edges.get(j));			
		}
		
		// remove duplicates
		Set<Edge> edgeSet = new LinkedHashSet<Edge>(allEdges);
		allEdges.clear();
		allEdges.addAll(edgeSet);
		
		// find intersecting edges and add them to the list
		Line2D.Double severLine = new Line2D.Double(x1, y1, x2, y2);
		ArrayList<Edge> edgesToSever = new ArrayList<Edge>();
		for (Edge e : allEdges) {
			Double2D p1 = ((PopulationCenter)e.getFrom()).getCentroid();
			Double2D p2 = ((PopulationCenter)e.getTo()).getCentroid();
			if (severLine.intersectsLine(new Line2D.Double(p1.x, p1.y, p2.x, p2.y)))
				edgesToSever.add(e);
		}
		
		System.out.format("RoadNetwork.severRoads: severing %d edges\n", edgesToSever.size());
		for (Edge e : edgesToSever)
			edgeField.removeEdge(e);
		
		// Update this because it will be out of date
		calcAllPairsShortestPaths(false, true);
		
		return edgesToSever;
	}
	
	public void restoreRoads(ArrayList<Edge> edges) {
		for (Edge e : edges)
			edgeField.addEdge(e);

		// Update this because it will be out of date
		calcAllPairsShortestPaths(false, true);
	}
}
