package sim.app.geo.cityMigration;

import java.io.*;
import java.util.ArrayList;

import cityMigration.cityMigrationData.CityMigrationData;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.field.network.stats.NetworkStatistics;
import sim.field.network.stats.WeightedEdgeMetric;

/**
 * Class that calculates the shortest path between all pairs of nodes using the
 * Floyd-Warshall algorithm. If the network is changed, you'll need to create a new one.
 * Note: the order of the elements in the pathLength and next arrays will match the order
 * in which they're housed in the network.
 * 
 * @author jharrison
 */
public class AllPairsShortestPath implements Serializable
{	
	private static final long serialVersionUID = 1L;
	
	final public int[][] next;
	final private double[][] pathLength;
	
	public AllPairsShortestPath(Network network, boolean printProgress) {
		int n = network.getAllNodes().numObjs;
		pathLength = new double[n][n];
		next = new int[n][n];
		
		long progress = 0;
		long progressTotal = (long)n*(long)n + (long)n*(long)n*(long)n;
		long progressStep = progressTotal / 1000;
		
		if (printProgress)
			System.out.format("AllPairsShortestPath: Performing %d operations for %d nodes\n", progressTotal, n);
		
		// initialize each pathLength[i][j] to edgeCost(i,j)
		Edge[][] edgeMatrix = network.getAdjacencyMatrix();
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++) {
				next[i][j] = -1;	// initialize this
				if (i == j)
					pathLength[i][j] = 0;
				else if (edgeMatrix[i][j] != null)
					pathLength[i][j] = edgeMatrix[i][j].getWeight();
				else 
					pathLength[i][j] = Double.POSITIVE_INFINITY;
				
				progress++;
				if (printProgress && ((progress % progressStep) == 0))
					System.out.format("%.1f%%\n", 100.0 * (progress / (double)progressTotal));
				
			}
		
		// calculate using Floyd-Warshall
		for (int k = 0; k < n; k++)
			for (int i = 0; i < n; i++)
				for (int j = 0; j < n; j++) {
					if (pathLength[i][k]+pathLength[k][j] < pathLength[i][j]) {
						pathLength[i][j] = pathLength[i][k]+pathLength[k][j];
						if (i != j)
							next[i][j] = k;
					}

					progress++;
					if (printProgress && ((progress % progressStep) == 0))
						System.out.format("%.1f%%\n", 100.0 * (progress / (double)progressTotal));
				}

//		testPathLengths();
//		compareResults(network);
	}

	public AllPairsShortestPath(Network network) { this(network, false); }
	
	private void compareResults(Network network) {
		System.out.println("Comparing AllPairsShortestPath results to NetworkStatistics.floydWarshallShortestPathsMatrix()...");
		double[][] nsPathLength = NetworkStatistics.floydWarshallShortestPathsMatrix(network, WeightedEdgeMetric.defaultInstance);
		
		int n = nsPathLength.length;
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++) {
				if (Math.abs(pathLength[i][j] - nsPathLength[i][j]) > 1e-6) {
					System.out.format("Comparison fails: [%d][%d], %f != %f\n", i, j, pathLength[i][j], nsPathLength[i][j]);
					return;
				}
			}
		
		System.out.println("Results match.");
	}
	
	public void testPathLengths() {
		// make sure array is symmetrical 
		int n = pathLength.length;
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++) {
				if (pathLength[i][j] != pathLength[j][i])
					System.out.format("PathLength not symmetrical. %d,%d: %f, %d,%d: %f\n", i, j, pathLength[i][j], j, i, pathLength[j][i]);
				
//				if (Double.isInfinite(pathLength[i][j]))
//					System.out.format("PathLength[%d][%d] is infinity.\n", i, j);
			}
	}
	
	public ArrayList<Integer> getPath(int from, int to) {
		if (Double.isInfinite(pathLength[from][to]))
			return null;
		int intermediate = next[from][to];
		if (intermediate == -1)
			return new ArrayList<Integer>();
		ArrayList<Integer> p = new ArrayList<Integer>();
		p.addAll(getPath(from, intermediate));
		p.add(intermediate);
		p.addAll(getPath(intermediate, to));
		return p;
	}

	public double getPathLength(int from, int to) {
		return pathLength[from][to];
	}
	
	public double getPathLength(long from, long to) {
		return getPathLength((int)from, (int)to);
	}
	
	static public void writeToFile(AllPairsShortestPath apsp, String filename) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(filename);
			out = new ObjectOutputStream(fos);
			out.writeObject(apsp);
			out.close();
		}
		catch (IOException ex) { ex.printStackTrace(); }
	}
	
	static public AllPairsShortestPath readFromFile(String filename) {
		AllPairsShortestPath apsp = null;
		InputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = CityMigrationData.class.getResourceAsStream(filename);
			in = new ObjectInputStream(fis);
			apsp = (AllPairsShortestPath) in.readObject();
			in.close();
		}
		catch (IOException ex) { ex.printStackTrace(); }
		catch (ClassNotFoundException ex) {	ex.printStackTrace(); }

		return apsp;
	}
}
