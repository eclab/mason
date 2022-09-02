package sim.app.geo.dschellingpolygon.display;

import sim.display.GeomVectorContinuousStorageProxy;
import sim.display.SimStateProxy;

public class PolySchellingProxy extends SimStateProxy{
	private static final long serialVersionUID = 1;

	public PolySchellingProxy(long seed) {
		super(seed);
		setRegistryHost("localhost");
		// setRegistryPort(5000);
	}

	// TODO

	//Continuous2DProxy agents = new Continuous2DProxy(discretization, 100, 100);
	GeomVectorContinuousStorageProxy poly = new GeomVectorContinuousStorageProxy(100, 100);

	public void start() {
		super.start();
		sleep = 5;
		//stepSize = 100;

		// TODO indexing. Needs to match same index ordering as...
//		registerFieldProxy(walkwaysPortrayal, 0);
//		registerFieldProxy(buildingPortrayal, 1);
//		registerFieldProxy(roadsPortrayal, 2);
		registerFieldProxy(poly, 0);
	}
}
