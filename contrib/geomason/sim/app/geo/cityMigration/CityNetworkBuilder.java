/*
 * Builds a network of interactions between cities for purposes of moving refugees
 * around between cities.  Each city is connected to one or more cities larger than
 * itself.  Variables allow for setting of minimum and maximum number of links per
 * city, as well as setting a threshold, so that each city has sufficient links to
 * account for x percent of its interaction with larger cities.
 */

package cityMigration;

import java.util.ArrayList;
import riftland.PopulationCenter;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;

/**
 *
 * @author Tim
 */
public class CityNetworkBuilder {

	/**
	 * 
	 * @param cityInteractions
	 * @param minLinksPerCity minimum number of larger cities each city examines
	 * @param maxLinksPerCity maximum number of larger cities each city examines (400+ is all)
	 * @param thresholdLinks include links until a certain percentage of interaction is accounted
	 * @param spatialDecayExp exponent for spatial influence decay range 
	 * between .4 and 3.33 -- see Stillwell 1978 -- negative power function higher values may be 
	 * justifed because we are talking about emergency relocation -- probably on foot, rather 
	 * than relocation in Sweden or UK
	 * @param onlyMigrateToLargerCities 
	 */
    public static void buildCityNetwork(Network cityInteractions, int minLinksPerCity, int maxLinksPerCity, 
    		double thresholdLinks, double spatialDecayExp, CityInteractionMetric metric) {

    	EdgeComparator comparator = new EdgeComparator();
    	Bag cities = new Bag(cityInteractions.getAllNodes());

    	for(int i=0; i < cities.size(); i++) {
    		PopulationCenter city = (PopulationCenter)cities.get(i);
    		double totalOutEdgeWeights = 0;

    		for(int j=0; j < cities.size(); j++)
    			if (i != j) {	// don't make self-edges
	    			PopulationCenter cityPartner = (PopulationCenter)cities.get(j);
    				double interaction = metric.calcInteraction(city, cityPartner);
    				cityInteractions.addEdge(new Edge(city, cityPartner, interaction));
    				totalOutEdgeWeights = totalOutEdgeWeights + interaction;
    			}

    		Bag cityLinksOriginal = cityInteractions.getEdgesOut(city);
    		Bag cityLinks = new Bag(cityLinksOriginal);
    		cityLinks.sort(comparator);

    		double topOutEdgeWeights = 0;
    		for (int k=0; k < cityLinks.size(); k++)
    		{
    			Edge e = (Edge)cityLinks.get(k);

    			if (((topOutEdgeWeights >= totalOutEdgeWeights * thresholdLinks) && (k >= minLinksPerCity)) || k >= maxLinksPerCity){
    				cityInteractions.removeEdge(e);
    			}
    			topOutEdgeWeights += e.getWeight(); // this is below so that we are sure we get to the target %
    		}
    	}
    	System.out.println("Done building network.");
    }
	/**
	 * 
	 * @param cityInteractions
	 * @param minLinksPerCity minimum number of larger cities each city examines
	 * @param maxLinksPerCity maximum number of larger cities each city examines (400+ is all)
	 * @param thresholdLinks include links until a certain percentage of interaction is accounted
	 * @param spatialDecayExp exponent for spatial influence decay range 
	 * between .4 and 3.33 -- see Stillwell 1978 -- negative power function higher values may be 
	 * justifed because we are talking about emergency relocation -- probably on foot, rather 
	 * than relocation in Sweden or UK
	 * @param onlyMigrateToLargerCities 
	 */
    public static void buildCityNetworkFromPopulation(Network cityInteractions, int minLinksPerCity, int maxLinksPerCity, 
    		double thresholdLinks, final double spatialDecayExp) {
    	
    	buildCityNetwork(cityInteractions, minLinksPerCity, maxLinksPerCity, thresholdLinks, spatialDecayExp, new CityInteractionMetric() {
			@Override
			public double calcInteraction(PopulationCenter c1, PopulationCenter c2) {
				double distance = c1.getCentroid().distance(c2.getCentroid());
				return ((double)c1.getUrbanites() * (double)c2.getUrbanites()) / Math.pow(distance,spatialDecayExp);
			}
		});
    }

    public static void buildCityNetworkFromCapacity(Network cityInteractions, int minLinksPerCity, int maxLinksPerCity, 
    		double thresholdLinks, final double spatialDecayExp) {
    	
    	buildCityNetwork(cityInteractions, minLinksPerCity, maxLinksPerCity, thresholdLinks, spatialDecayExp, new CityInteractionMetric() {
			@Override
			public double calcInteraction(PopulationCenter c1, PopulationCenter c2) {
				double distance = c1.getCentroid().distance(c2.getCentroid());
				return ((double)c1.getRefugeeCapacity() * (double)c2.getRefugeeCapacity()) / Math.pow(distance,spatialDecayExp);
			}
		});
    }

    public static void buildCityNetworkFromEdgeList(Network cityInteractions, ArrayList<CityEdge> edges, 
    		double thresholdLinks, double spatialDecayExp) {

    	for (CityEdge edge : edges) {
    		PopulationCenter city = edge.city1;
    		PopulationCenter cityPartner = edge.city2;

			double edgeDistance = city.getCentroid().distance(cityPartner.getCentroid());    
			double interaction = ((double)city.getAggregateCapacity() * (double)cityPartner.getAggregateCapacity())/Math.pow(edgeDistance,spatialDecayExp);
//			cityInteractions.addEdge(new Edge(city, cityPartner, interaction));
			cityInteractions.addEdge(new Edge(city, cityPartner, edgeDistance));
    	}
    }
}
