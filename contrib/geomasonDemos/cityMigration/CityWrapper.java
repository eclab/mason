package cityMigration;

import riftland.PopulationCenter;
import sim.engine.SimState;
import sim.engine.Steppable;

@SuppressWarnings("serial")
public class CityWrapper implements Steppable
{
	PopulationCenter city;
	public CityWrapper(PopulationCenter city) {
		this.city = city;
	}

	@Override
	public void step(SimState state) {		
	}

}
