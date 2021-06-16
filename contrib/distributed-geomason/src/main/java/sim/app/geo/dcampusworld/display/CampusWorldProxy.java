package sim.app.geo.dcampusworld.display;

import java.rmi.NotBoundException;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map.Entry;

import sim.app.geo.dcampusworld.DAgent;
import sim.app.geo.dcampusworld.DCampusWorld;
import sim.display.*;
import sim.engine.DObject;
import sim.field.geo.GeomVectorField;
import sim.field.storage.ContinuousStorage;
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

	/** Data is registered/transferred as Continuous2D **/
//	MyProxy agents2 = new MyProxy(discretization, 100, 100);
	MyProxy agentProxy = new MyProxy(1, 1, 1);
	//GeomVectorFieldProxy agents = new GeomVectorFieldProxy(100, 100);

	/** Remote data is reloaded into this field for portrayal reasons **/
	GeomVectorField agentRepresentations = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
	//GeomVectorField walkways = new GeomVectorField(100, 100);
	//GeomVectorField roads = new GeomVectorField(100, 100);
	//GeomVectorField buildings = new GeomVectorField(100, 100);
	
	

	ContinuousPortrayal2D agentPortrayal = new ContinuousPortrayal2D();

	public void start() {
		super.start();
//		sleep = 5;
//		stepSize = 100;

		// TODO indexing. Needs to match same index ordering as...

		registerFieldProxy(agentProxy, 0);
		
		//Doesn't work because GeomVectorFieldProxy doesn't have a GridStorage object?
		//registerFieldProxy(walkways, 1);
		//registerFieldProxy(roads, 2);
		//registerFieldProxy(buildings, 3);
	}

	class MyProxy extends Continuous2DProxy {
		private static final long serialVersionUID = 1L;
	
		public MyProxy(double discretization, double width, double height) {
			super(discretization, width, height);
		}
	
		public void update(SimStateProxy stateProxy, int proxyIndex, int[] quad_tree_partitions) throws RemoteException, NotBoundException {
			
			super.update(stateProxy, proxyIndex, quad_tree_partitions);
			
			// Update our agent representations
			agentRepresentations.clear();
			
			Bag objs = getAllObjects();
			System.out.println("Updating " + objs.numObjs + " objects");
			for (int i=0; i<objs.numObjs; i++) {
				System.out.println("proxy storage object #" + (i+1) + "/" + objs.numObjs + " = " + objs.objs[i].getClass() + ": " + objs.objs[i]);
				if (!(objs.objs[i] instanceof DAgent)) {
//					System.out.println("Skipped " + objs.objs[i].getClass());
					continue;
				}
				((DAgent)objs.objs[i]).transfer(getObjectLocation(objs.objs[i]), agentRepresentations);
			}
			System.out.println("Done Updating agents: " + agentRepresentations.getGeometries().size());
			
			
//			// Alternative way?
//			// Move all agents into GeomVectorField
//            ContinuousStorage storage = (ContinuousStorage)(stateProxy.storage(proxyIndex));
//            HashMap<Long, Double2D> map = storage.getStorageMap();
//            // ^ object ids? to locations
//			System.out.println("Updating " + map.entrySet().size() + " objects");
//            for (Entry<Long, Double2D> entry : map.entrySet()) {
//            	Long objID = entry.getKey(); // object ID?
//            	Double2D loc = entry.getValue(); // object location
//            	DObject obj = storage.getObject(loc, objID); // <- getting nulls here so...
//            	DAgent agent = (DAgent) obj;
//				agent.transfer(loc, agentRepresentations);
//            }
//			System.out.println("Done Updating agents: " + agentRepresentations.getGeometries().size());
		}
	}
}