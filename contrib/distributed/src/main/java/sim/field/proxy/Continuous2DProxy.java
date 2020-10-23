package sim.field.proxy;
import sim.field.continuous.*;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import sim.util.*;

public class Continuous2DProxy extends Continuous2D implements UpdatableProxy
	{
	public Continuous2DProxy(double discretization, double width, double height) { super(discretization, width, height); }

	public void update(SimStateProxy stateProxy, int proxyIndex) throws RemoteException, NotBoundException
		{
		// reshape if needed
		IntHyperRect bounds = stateProxy.bounds();
		double width = bounds.br.x - (double)bounds.ul.x;
		double height = bounds.br.y - (double)bounds.ul.y;
		if (width != this.width || height != this.height)
			reshape(width, height);
		
		// load storage
		
		// FIXME: one problem here is that ContStorage is at least twice as big as we need
		// because we just need the hashmap, not the hashset array.
		// Perhaps we should make it a Remote object and grab only the data we need
		// rather than pushing the whole object over the network for visualization
		
		ContStorage storage = (ContStorage)(stateProxy.storage(proxyIndex));
		HashMap<Object, Double2D> map = (HashMap)(storage.getStorageObjects());
		
		// FIXME: discretization is final, cannot be assigned
		discretization = storage.getDiscretization();
		
		clear();
		
		Iterator iterator = map.keySet().iterator();
		while(iterator.hasNext())
			{
			Object obj = iterator.next();
			setObjectLocation(obj, map.get(obj));
			}
		}
	}