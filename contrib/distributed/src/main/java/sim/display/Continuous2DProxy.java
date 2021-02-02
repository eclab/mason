package sim.display;

import sim.field.continuous.*;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import java.util.Map.Entry;

import sim.util.*;

@SuppressWarnings("rawtypes")
public class Continuous2DProxy extends Continuous2D implements UpdatableProxy {
	private static final long serialVersionUID = 1L;

	public Continuous2DProxy(double discretization, double width, double height) {
		super(discretization, width, height);
	}

	public void update(SimStateProxy stateProxy, int proxyIndex) throws RemoteException, NotBoundException {
		// reshape if needed
		IntRect2D bounds = stateProxy.bounds();
		int width = bounds.br().x - bounds.ul().x;
		int height = bounds.br().y - bounds.ul().y;
		if (width != this.width || height != this.height)
			reshape(width, height);

		// load storage

		// FIXME: one problem here is that ContStorage is at least twice as big as we
		// need
		// because we just need the hashmap, not the hashset array.
		// Perhaps we should make it a Remote object and grab only the data we need
		// rather than pushing the whole object over the network for visualization

		// FIXME: HashMap in ContStorage contains global coordinates, not local ones.
		ContinuousStorage storage = (ContinuousStorage) (stateProxy.storage(proxyIndex));
		HashMap<Long, Double2D> map = storage.getStorageMap();

		discretization = storage.getDiscretization();

		clear();

		Int2D origin = bounds.ul();

		// Using EntrySet because its faster (no additional map.get) and
		// doesn't create additional objects either
		// KeySet returns nextNode().key
		// EntrySet returns nextNode()
		for (Entry<Long, Double2D> entry : map.entrySet()) {
			Double2D loc = (Double2D) (entry.getValue());
			Double2D newLoc = new Double2D(loc.x - origin.x, loc.y - origin.y);
			setObjectLocation(entry.getKey(), newLoc);
		}
	}

}