package sim.app.geo.dsleuth.display;

import sim.display.ObjectGrid2DProxy;
import sim.display.SimStateProxy;

public class SleuthWorldProxy extends SimStateProxy{

	private static final long serialVersionUID = 1;

	public SleuthWorldProxy(long seed) {
		super(seed);
		setRegistryHost("localhost");
		// setRegistryPort(5000);
	}

	// TODO
	double discretization = 6;

	//Continuous2DProxy agents = new Continuous2DProxy(discretization, 100, 100);
	//GeomVectorContinuousStorageProxy agents = new GeomVectorContinuousStorageProxy(100, 100);

	ObjectGrid2DProxy landscape = new ObjectGrid2DProxy(100, 100);
	
	
	public void start() {
		super.start();
		//sleep = 5;
		//stepSize = 100;

		// TODO indexing. Needs to match same index ordering as...

		registerFieldProxy(landscape, 0);
	}
	
}
