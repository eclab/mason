package sim.display;

import sim.field.continuous.*;
import sim.field.geo.GeomVectorField;
import sim.app.geo.dcampusworld.display.CampusWorldProxy;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import java.util.Map.Entry;

import sim.util.*;

@SuppressWarnings("rawtypes")
public class GeomVectorFieldProxy extends GeomVectorField implements UpdatableProxy {
	private static final long serialVersionUID = 1L;

    public GeomVectorFieldProxy() {
        super();
    }
    
    public GeomVectorFieldProxy(int w, int h) {
    	super(w, h);
    }

	@Override
	public void update(SimStateProxy proxy, int storage, int[] partition_list)
			throws RemoteException, NotBoundException {
		// TODO Auto-generated method stub
		
	}

//	public void update(SimStateProxy stateProxy, int proxyIndex) throws RemoteException, NotBoundException {
////		System.out.println("stateProxy: " + stateProxy);
////		System.out.println("proxyIndex: " + proxyIndex);
////		System.out.println("stateProxy.storage(proxyIndex): " + stateProxy.storage(proxyIndex));
////		System.out.println("stateProxy.storage(proxyIndex).getClass(): " + stateProxy.storage(proxyIndex).getClass());
////		
//////		CampusWorldProxy cwp = (CampusWorldProxy) stateProxy;
//////		stateProxy.
////		
////		//TODO
////		// Get the latest geomvector data that has changed and update this one to match it
////		// but only update the dynamic geomvectors, not the static ones
////		// and from the dynamic ones, only update the ones that have changed...
////
////		ContinuousStorage storage = (ContinuousStorage) (stateProxy.storage(proxyIndex));
////		HashMap<Long, Double2D> map = storage.getStorageMap();
////		
////		// Clear all geometries and MBR
////		clear();
////
////		//TODO ...
////		// Reconstruct geometries? How? We're just storing locations in storage, no?
////		// Although, currently, they're just points, but still, they need to be GeomVectorFields
////		// Quadtree? index? etc...
//	}
}