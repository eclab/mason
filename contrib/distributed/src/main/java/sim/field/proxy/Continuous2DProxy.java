package sim.field.proxy;
import sim.field.continuous.*;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import java.util.Map.Entry;

import sim.util.*;

@SuppressWarnings("rawtypes")
public class Continuous2DProxy extends Continuous2D implements UpdatableProxy
	{
	public Continuous2DProxy(double discretization, double width, double height) { super(discretization, width, height); }

	public void update(SimStateProxy stateProxy, int proxyIndex) throws RemoteException, NotBoundException
		{
		// reshape if needed
		IntRect2D bounds = stateProxy.getBounds();
		int width = bounds.br().x - bounds.ul().x;
		int height = bounds.br().y - bounds.ul().y;
		if (width != this.width || height != this.height)
			reshape(width, height);
		
		// load storage
		
		// FIXME: one problem here is that ContStorage is at least twice as big as we need
		// because we just need the hashmap, not the hashset array.
		// Perhaps we should make it a Remote object and grab only the data we need
		// rather than pushing the whole object over the network for visualization
		
		// FIXME: HashMap in ContStorage contains global coordinates, not local ones.
		ContStorage storage = (ContStorage)(stateProxy.storage(proxyIndex));
		HashMap<Object, Double2D> map = (HashMap)(storage.getStorageObjects());
		
		discretization = storage.getDiscretization();
		
		clear();
		
		// Using EntrySet because its faster (no additional map.get) and 
		// doesn't create additional objects either
		// KeySet returns nextNode().key
		// EntrySet returns nextNode()
		for (Entry<Object, Double2D> entry : map.entrySet()) 
			setObjectLocation(entry.getKey(), entry.getValue());
		
//		Iterator iterator = map.keySet().iterator();
//		while(iterator.hasNext())
//			{
//			Object obj = iterator.next();
//			setObjectLocation(obj, map.get(obj));
//			}
		}
	}