package sim.app.geo.dcampusworld.display;

import sim.app.geo.dcampusworld.DCampusWorld;
import sim.display.*;
import sim.field.geo.GeomVectorField;
import sim.portrayal.continuous.ContinuousPortrayal2D;
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

	Continuous2DProxy agents = new Continuous2DProxy(discretization, 100, 100);
	
	//Doesn't work because GeomVectorFieldProxy cannot access a GridStorage object?
	
	//GeomVectorFieldProxy walkways = new GeomVectorFieldProxy(100, 100);
	//GeomVectorFieldProxy roads = new GeomVectorFieldProxy(100, 100);
	//GeomVectorFieldProxy buildings = new GeomVectorFieldProxy(100, 100);
	

	
	
	

	ContinuousPortrayal2D agentPortrayal = new ContinuousPortrayal2D();

	public void start() {
		super.start();
		sleep = 5;
		stepSize = 100;

		// TODO indexing. Needs to match same index ordering as...

		registerFieldProxy(agents, 0);
		
		//Doesn't work because GeomVectorFieldProxy doesn't have a GridStorage object?
		//registerFieldProxy(walkways, 1);
		//registerFieldProxy(roads, 2);
		//registerFieldProxy(buildings, 3);
	}
}
