package cityMigration;

import riftland.PopulationCenter;

import java.net.URL;

/**
 * Simple pair class for use with city networks.
 * 
 * @author Joey Harrison
 *
 */
public class CityEdge
{
	PopulationCenter city1;
	PopulationCenter city2;


	public CityEdge(PopulationCenter city1, PopulationCenter city2) {
		this.city1 = city1;
		this.city2 = city2;

	}
	
}
