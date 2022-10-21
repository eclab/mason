package sim.app.geo.dcampusworld.display;

import sim.display.geo.*;
import sim.display.*;
import sim.field.geo.GeomVectorField;
import sim.portrayal.geo.GeomVectorFieldPortrayal;

public class CampusWorldProxy extends SimStateProxy {
	private static final long serialVersionUID = 1;

	public CampusWorldProxy(long seed) {
		super(seed);
		setRegistryHost("localhost");
		// setRegistryPort(5000);
	}

	// TODO
	double discretization = 6;

	//Continuous2DProxy agents = new Continuous2DProxy(discretization, 100, 100);
	GeomVectorContinuousStorageProxy agents = new GeomVectorContinuousStorageProxy(100, 100);

	public void start() {
		super.start();
		sleep = 5;
		//stepSize = 100;

		// TODO indexing. Needs to match same index ordering as...
//		registerFieldProxy(walkwaysPortrayal, 0);
//		registerFieldProxy(buildingPortrayal, 1);
//		registerFieldProxy(roadsPortrayal, 2);
		registerFieldProxy(agents, 0);
	}
}