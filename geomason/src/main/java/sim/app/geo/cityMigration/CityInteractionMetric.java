package sim.app.geo.cityMigration;

import sim.app.geo.riftland.PopulationCenter;

public interface CityInteractionMetric
{
	public double calcInteraction(PopulationCenter c1, PopulationCenter c2);

}
