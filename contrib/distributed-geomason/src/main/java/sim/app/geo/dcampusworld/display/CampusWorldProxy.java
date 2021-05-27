package sim.app.geo.dcampusworld.display;

import java.rmi.NotBoundException;

import java.rmi.RemoteException;

import sim.app.geo.dcampusworld.DAgent;
import sim.app.geo.dcampusworld.DCampusWorld;
import sim.display.*;
import sim.field.geo.GeomVectorField;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.Bag;
import sim.util.Double2D;

public class CampusWorldProxy extends SimStateProxy {
	private static final long serialVersionUID = 1;

	public CampusWorldProxy(long seed) {
		super(seed);
		setRegistryHost("localhost");
		// setRegistryPort(5000);
	}

	// TODO
	double discretization = 6;

	Continuous2DProxy agents2 = new Continuous2DProxy(discretization, 100, 100);
	//GeomVectorFieldProxy agents = new GeomVectorFieldProxy(100, 100);

	//Doesn't work because GeomVectorFieldProxy cannot access a GridStorage object?
	
	//GeomVectorFieldProxy walkways = new GeomVectorFieldProxy(100, 100);
	//GeomVectorFieldProxy roads = new GeomVectorFieldProxy(100, 100);
	//GeomVectorFieldProxy buildings = new GeomVectorFieldProxy(100, 100);
	
	GeomVectorField agents = new GeomVectorField(100, 100);
	//GeomVectorField walkways = new GeomVectorField(100, 100);
	//GeomVectorField roads = new GeomVectorField(100, 100);
	//GeomVectorField buildings = new GeomVectorField(100, 100);
	
	

	ContinuousPortrayal2D agentPortrayal = new ContinuousPortrayal2D();

	public void start() {
		super.start();
		sleep = 5;
		stepSize = 100;

		// TODO indexing. Needs to match same index ordering as...

		registerFieldProxy(agents2, 0);
		
		//Doesn't work because GeomVectorFieldProxy doesn't have a GridStorage object?
		//registerFieldProxy(walkways, 1);
		//registerFieldProxy(roads, 2);
		//registerFieldProxy(buildings, 3);
	}



class MyProxy extends Continuous2DProxy{
	
	public MyProxy(double discretization, double width, double height) {
		super(discretization, width, height);
	}

	public void update(SimStateProxy stateProxy, int proxyIndex, int[] quad_tree_partitions) throws RemoteException, NotBoundException {
		
		super.update(stateProxy, proxyIndex, quad_tree_partitions);
		
		Bag objs = getAllObjects();
		
		agents.clear();
		
		for (int i=0; i<objs.numObjs; i++) {
			
			((DAgent)objs.objs[i]).transfer(getObjectLocation(objs.objs[i]),  agents);
		}
		
		
	}
		

}
}