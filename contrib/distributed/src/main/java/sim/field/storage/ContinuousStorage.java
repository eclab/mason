package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import sim.app.dflockers.DFlocker;
import sim.engine.DObject;
import sim.util.*;

public class ContinuousStorage<T extends DObject> extends GridStorage<T, Double2D> {
	private static final long serialVersionUID = 1L;

	int dWidth;
	int dHeight;
//	int[] dsize;
	double discretization;
	public HashMap<Long, Double2D> m;
	public HashMap<Long, T>[] storage;
	public boolean removeEmptyBags = true;

	public ContinuousStorage(final IntRect2D shape, final double discretization) {
		super(shape);
		this.discretization = discretization;
		clear();
	}

	/*
	 * public GridStorage getNewStorage(final IntRect2D shape) { return new
	 * ContinuousStorage<>(shape, discretization); }
	 */

	public void clear() {
		int size = shape.getArea();
//		this.dsize = new int[2]; // 2 = num dimensions
//		for (int i = 0; i < this.dsize.length; i++) {
//
//			this.dsize[i] = (int) Math.ceil(shape.getSizes()[i] / discretization) + 1;
//		}

		dWidth = (int) Math.ceil(shape.getSizes()[0] / discretization) + 1;
		dHeight = (int) Math.ceil(shape.getSizes()[1] / discretization) + 1;

		// Overwrite the original height with the new height of dsize;
		// so that getFlatIdx() can correctly get the cell index of a discretized point
		// TODO better approach?
//		height = dsize[1]; // getHeight(dsize);
		height = dHeight;
		this.m = new HashMap<>();

		//int volume = 1;
		//for (int i = 0; i < this.dsize.length; i++) { // <- size of 2
		//	volume *= this.dsize[i];
		//}
		//storage = new HashMap[volume];
		storage = new HashMap[dWidth * dHeight];
//		for (int i = 0; i < volume; i++) {
		for (int i = 0; i < storage.length; i++) {
			storage[i] = new HashMap<>();
		
		}
	}

	public String toString() {
		final StringBuffer string = new StringBuffer(String.format("ContStorage-%s\n", shape));

//		TODO: Should we use StringBuilder here?
//		StringBuffer uses synchronization and is generally slower than StringBuilder
//		final StringBuilder string = new StringBuilder(String.format("ContStorage-%s\n", shape));

		//for (int x = 0; x < dsize[0]; x++) {
		//	for (int y = 0; y < dsize[1]; y++) {
		for (int x = 0; x < dWidth; x++) {
			for (int y = 0; y < dHeight; y++) {
				HashMap<Long, T> cell = getCelldp(x, y);
				if (cell.size() > 0)
					string.append("Cell (" + x + ", " + y + "):\t" + cell + "\n");
			}
		}

		return string.toString();
	}

	// This returns a list of a list of dissimilar Objects
	// They are either of type T or of type Double2D
	public Serializable pack(final MPIParam mp) {
		final ArrayList<ArrayList<Serializable>> ret = new ArrayList<>();

		for (final IntRect2D rect : mp.rects) {
			final ArrayList<Serializable> objs = new ArrayList<>();
			for (final T obj : getObjects(rect.shift(shape.ul().toArray()))) {
				objs.add(obj);
				// Append the object's location relative to the rectangle
				objs.add(m.get(obj.ID()).subtract(shape.ul().toArray()).subtract(rect.ul().toArray()));
			}
			ret.add(objs);
		}

		return ret;
	}

	public int unpack(final MPIParam mp, final Serializable buf) {
		final ArrayList<ArrayList<Serializable>> objs = (ArrayList<ArrayList<Serializable>>) buf;


		// Remove any objects that are in the unpack area (overwrite the area)
		// shift the rect with local coordinates back to global coordinates
		for (final IntRect2D rect : mp.rects)
			removeObjects(rect.shift(shape.ul().toArray()));
		


		for (int k = 0; k < mp.rects.size(); k++)
			for (int i = 0; i < objs.get(k).size(); i += 2) 
				addToLocation((T) objs.get(k).get(i), 
					((Double2D) objs.get(k).get(i + 1)).add(mp.rects.get(k).ul().toArray()).add(shape.ul().toArray()));
		
		int sum = 0;
		for (int i = 0; i < objs.size(); i++) {
			sum += objs.get(i).size();
		}
		

		return sum;
	}

	public Int2D discretize(final NumberND p) {

		final double[] offsets = shape.ul().getOffsets(p);

		int[] ans = new int[2];
		for (int i = 0; i < offsets.length; i++) {
			ans[i] = -(int) (offsets[i] / discretization);
		}
		return new Int2D(ans);
	}

	public void setCell(final Double2D p, HashMap<Long, T> cell) {
		setCelldp(discretize(p), cell);
	}

	public void setCelldp(final Int2D p, HashMap<Long, T> cell) {
		storage[getFlatIdx(p)] = cell;
	}

	public void setCelldp(int x, int y, HashMap<Long, T> cell) {
		storage[getFlatIdx(x, y)] = cell;
	}

	// Get the corresponding cell given a continuous point
	public HashMap<Long, T> getCell(final Double2D p) {
		
		try {
			storage[getFlatIdx(discretize(p))] = storage[getFlatIdx(discretize(p))]; //check for null pointer
			
		}
		
		catch(Exception e) {
			System.out.println("shape.ul : "+shape.ul());
			System.out.println("shape.br : "+shape.br());

			System.out.println("getCell p : "+p);
			System.out.println("getCell discretize(p) : "+discretize(p));
			System.out.println("getFlatIdx(discretize(p)) : "+getFlatIdx(discretize(p)));
			System.out.println("storage size : "+storage.length);
			
			System.out.println(e);
			System.exit(-1);
			
		}
		

		return getCelldp(discretize(p));
	}

	// Get the corresponding cell given a discretized point
	public HashMap<Long, T> getCelldp(final Int2D p) {

		return storage[getFlatIdx(p)];
	}

	public HashMap<Long, T> getCelldp(int x, int y) {
		return storage[getFlatIdx(x, y)];
	}

	// Return a list of neighbors of the given object within the given radius
	public List<T> getNeighborsWithin(final T obj, final double radius) {
		Double2D tmp = null;
		try {
			tmp = m.get(obj.ID());
		} catch (Exception e) {
			System.exit(-1);
		}
		final Double2D loc = tmp;
		Int2D tmp2 = null;
		try {
			tmp2 = discretize(loc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		final Int2D dloc = tmp2;
		final ArrayList<T> objs = new ArrayList<T>();

		// Calculate how many discretized cells we need to search
		final int[] offsets = new int[2];
		for (int i = 0; i < offsets.length; i++) {
			offsets[i] = (int) Math.ceil(radius / discretization);
		}

		// Generate the start/end point subject to the boundaries
		int[] ansUl = new int[2]; // 2 = num dimensions
		ansUl[0] = Math.max(dloc.x - offsets[0], 0);
		ansUl[1] = Math.max(dloc.y - offsets[1], 0);
		final Int2D ul = new Int2D(ansUl);

		int[] ansBr = new int[2]; // 2 = num dimensions
		//ansBr[0] = Math.min(dloc.x + offsets[0] + 1, dsize[0]);
		//ansBr[1] = Math.min(dloc.y + offsets[1] + 1, dsize[1]);

		ansBr[0] = Math.min(dloc.x + offsets[0] + 1, dWidth);
		ansBr[1] = Math.min(dloc.y + offsets[1] + 1, dHeight);

		final Int2D br = new Int2D(ansBr);
		for (int x = ul.x; x < br.x; x++) {
			for (int y = ul.y; y < br.y; y++) {
				for (T foo : getCelldp(x, y).values()) {
					if (foo != obj && m.get(foo.ID()).distanceSq(loc) <= radius * radius) {
					}
				}
			}
		}

		return objs;
	}

	public HashMap<Long, Double2D> getStorageMap() {
		return m;
	}

	public double getDiscretization() {
		return discretization;
	}

	// Get the location of the given location
	public Double2D getLocation(final T obj) {
		return m.get(obj.ID());
	}

	// Get the location of the given location
	public Double2D getLocation(final long id) {
		return m.get(id);
	}

	///// GRIDSTORAGE GUNK

	// Put the object to the given point
	public void addToLocation(final T obj, final Double2D p) {
		final Double2D old = m.put(obj.ID(), p);
		if (old != null)
			getCell(old).remove(obj.ID());
		getCell(p).put(obj.ID(), obj);
	}

	public void removeObject(Double2D p, final T obj) {
		getCell(m.remove(obj.ID())).remove(obj);
	}

	// Remove all the objects at the given point
	public void removeObjects(final Double2D p) {
		for (T obj : getObjects(p)) {
			removeObject(obj);
		}
	}

	public void removeObject(Double2D p, long id) {
		// TODO: this may throw a null pointer if object is not there
		removeObject(getCell(p).get(id));
	}

	// Remove the object from the storage
	public void removeObject(final T obj) {
        if (m.get(obj.ID()) == null) {
        	System.out.println("removeObject");
        	System.out.println("ul : "+this.shape.ul());
        	System.out.println("br : "+this.shape.br());
        	System.out.println("obj : "+obj);
        	System.out.println(((DFlocker)obj).loc);
        }
        

		
		Double2D dd = m.remove(obj.ID());
		HashMap a = getCell(dd);
		
		Object o = a.remove(obj.ID());
	}

	// Get all the objects at the given point
	public ArrayList<T> getObjects(final Double2D p) {
		final ArrayList<T> objects = new ArrayList<>();

		if (getCell(p) != null) {
		for (final T t : getCell(p).values()) {
			if (m.get(t.ID()).equals(p))
				objects.add(t);
		}
		}
		return objects;
	}

	public T getObjects(Double2D p, long id) {
		return getCell(p).get(id);
	}

	/// INTERNAL METHODS FOR PACKING AND UNPACKING

	// Remove all the objects inside the given rectangle
	void removeObjects(final IntRect2D r) {
		for (T obj : getObjects(r)) {
			removeObject(obj);
		}
	}

	// Get all the objects inside the given rectangle
	public List<T> getObjects(final IntRect2D r) {
		final ArrayList<T> objs = new ArrayList<T>();
		int[] offset = { 1, 1 };

		final Int2D ul = discretize(r.ul()), br = discretize(r.br()).add(offset);

	// I believe this code is just doing:

		for (int x = ul.x; x < br.x; x++) {
			for (int y = ul.y; y < br.y; y++) {
				
				HashMap<Long, T> cell = getCelldp(x, y);
				
				if (cell != null) {
					
				
				for (T obj : cell.values()) { //need to offset/discretize!
					
					if (m.get(obj.ID()) == null) {
						System.out.println(this.shape);
						System.out.println("r : "+r);
						System.out.println("x : "+x);
						System.out.println("y : "+y);
						System.out.println("ul : "+ul);
						System.out.println("br : "+br);
						System.out.println("obj loc: "+this.getLocation(obj));
						System.out.println("dflocker loc : "+((DFlocker)obj).loc);
						System.out.println("dflocker disc loc : "+discretize(((DFlocker)obj).loc));
						System.out.println("----");
						System.out.println(obj);
						System.out.println(m.keySet());
						//System.exit(-1);
						
					}
					
					if (m.get(obj.ID()) == null){
						System.out.println("-!!!!-");
						System.out.println(this);
					}
					
					if (r.contains(m.get(obj.ID()))) {
						objs.add(obj);
					}
				}
				}
			}
		}

		return objs;
	}
	
}
