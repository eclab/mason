package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import sim.engine.DObject;
import sim.field.partitioning.IntRect2D;
import sim.util.*;

public class ContinuousStorage<T extends DObject> extends GridStorage<T, Double2D> {
	private static final long serialVersionUID = 1L;

	int[] dsize;
	double discretization;
	public HashMap<Long, Double2D> m;
	public HashMap<Long, T>[] storage; // HashMap<Long, T>[]

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
		this.dsize = new int[2]; // 2 = num dimensions
		for (int i = 0; i < this.dsize.length; i++) {

			this.dsize[i] = (int) Math.ceil(shape.getSizes()[i] / discretization) + 1;
		}

		// Overwrite the original height with the new height of dsize;
		// so that getFlatIdx() can correctly get the cell index of a discretized point
		// TODO better approach?
		height = dsize[1]; // getHeight(dsize);
		this.m = new HashMap<>();

		int volume = 1;
		for (int i = 0; i < this.dsize.length; i++) { // <- size of 2
			volume *= this.dsize[i];
		}
		storage = new HashMap[volume];
		for (int i = 0; i < volume; i++) {
			storage[i] = new HashMap<>();
		}
	}

	public String toString() {
		final StringBuffer string = new StringBuffer(String.format("ContStorage-%s\n", shape));

//		TODO: Should we use StringBuilder here?
//		StringBuffer uses synchronization and is generally slower than StringBuilder
//		final StringBuilder string = new StringBuilder(String.format("ContStorage-%s\n", shape));

//		for (final Int2D dp : IntPointGenerator.getBlock(dsize))
//			if (getCelldp(dp).size() > 0)
//				string.append("Cell " + dp + ":\t" + getCelldp(dp) + "\n");

		for (int x = 0; x < dsize[0]; x++) {
			for (int y = 0; y < dsize[1]; y++) {
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
//				objs.add(m.get(obj.getID()).rshift(shape.ul().toArray()).rshift(rect.ul().toArray()));

				
				//m.get(obj) is null?

				objs.add(m.get(obj.getID()).subtract(shape.ul().toArray()).subtract(rect.ul().toArray()));
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
				addToLocation((T) objs.get(k).get(i), ((Double2D) objs.get(k).get(i + 1))
						.add(mp.rects.get(k).ul().toArray()).add(shape.ul().toArray()));

//		return objs.stream().mapToInt(x -> x.size()).sum();
		int sum = 0;
		for (int i = 0; i < objs.size(); i++) {
			sum += objs.get(i).size();
		}
		return sum;
	}

	public Int2D discretize(final NumberND p) {

//		final double[] offsets = shape.ul().getOffsetsDouble(p);
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

//<<<<<<< HEAD
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
			//System.exit(-1);
			
		}
		

		return getCelldp(discretize(p));
//=======
//	// Put the object to the given point
//	public void addToLocation(final T obj, final Double2D p) {
//		final Double2D old = m.put(obj, p);
//		if (old != null)
//			getCell(old).remove(obj);
//		getCell(p).add(obj);
//	}
//
//	// Get the location of the given location
//	public Double2D getLocation(final T obj) {
//		return m.get(obj);
//	}
//
//	// Get all the objects at the given point
//	public ArrayList<T> getObjects(final Double2D p) {
//		final ArrayList<T> objects = new ArrayList<>();
//		for (final T t : getCell(p)) {
//			if (m.get(t).equals(p))
//				objects.add(t);
//		}
//		return objects;
//	}
//
//	// Get all the objects inside the given rectangle
//	public List<T> getObjects(final IntRect2D r) {
//		final ArrayList<T> objs = new ArrayList<T>();
//
//		final Int2D ul = discretize(r.ul());
//		
//		int [] offset = {1,1};
//		
//		final Int2D br = discretize(r.br()).add(offset);
//
////		for (final Int2D dp : IntPointGenerator.getBlock(ul, br)) {
//// //			getCelldp(dp)
//// //				.stream()
//// //				.filter(obj -> r.contains(m.get(obj)))	// 
//// //				.forEach(obj -> objs.add(obj));
////			for (T obj : getCelldp(dp)) {
////				if (r.contains(m.get(obj))) {
////					objs.add(obj);
////				}
////			}
////		}
//
//// I believe this code is just doing:
//
//	for(int x = ul.x; x < br.x; x++)
//		{
//		for(int y = ul.y; y < br.y; y++)
//			{
//			for(T obj: getCelldp(x, y))
//				{
//				if (r.contains(m.get(obj))) 
//					{
//					objs.add(obj);
//					}
//				}
//			}
//		}
//
//		return objs;
//	}
//
//	// Remove the object from the storage
//	public void removeObject(final T obj) {
//		getCell(m.remove(obj)).remove(obj);
//	}
//
//	public void removeObject(final T obj, Double2D p) {
//		getCell(m.remove(obj)).remove(obj);
//>>>>>>> 5a7347af137247b63139aa9f8f7b6717db8010f9
	}

	// Get the corresponding cell given a discretized point
	public HashMap<Long, T> getCelldp(final Int2D p) {

		return storage[getFlatIdx(p)];
	}

	public HashMap<Long, T> getCelldp(int x, int y) {
		return storage[getFlatIdx(x, y)];
	}

	/**
	 * // Return a list of k nearest neighbors sorted by their distances // to the
	 * query obj, if that many exists public List<T> getNearestNeighbors(final T
	 * obj, final int need) { final Double2D loc = m.get(obj.getID()); final Int2D
	 * dloc = discretize(loc); // final int maxLayer = IntStream.range(0, 2) // 0,
	 * ..., 2 - 1 // 2 = num dimensions // .map(i -> Math.max(dloc.c(i), dsize[i] -
	 * dloc.c(i))) // .max() // .getAsInt(); int max = Integer.MIN_VALUE; for (int i
	 * = 0; i < 2; i++) // 2 = num dimensions { int mapI = Math.max(dloc.c(i),
	 * dsize[i] - dloc.c(i)); if (mapI > max) { max = mapI; } } final int maxLayer =
	 * max;
	 * 
	 * final ArrayList<T> objs = new ArrayList<T>(); final ArrayList<T> candidates =
	 * new ArrayList<T>(getCelldp(dloc)); candidates.remove(obj); // remove self
	 * 
	 * int currLayer = 1;
	 * 
	 * while (objs.size
	 * 
	 * while (objs.size() < need && currLayer <= maxLayer) { for (final Int2D dp :
	 * IntPointGenerator.getLayer(dloc, currLayer)) { boolean flag = true; for (int
	 * i = 0; i < 2; i++) // 2 = num dimensions { if (!(dp.c(i) >= 0 && dp.c(i) <
	 * dsize[i])) { flag = false; break; } } if (flag) {
	 * candidates.addAll(getCelldp(dp)); } // if (IntStream.range(0, 2) // 2 = num
	 * dimensions // .allMatch(i -> dp.c(i) >= 0 && dp.c(i) < dsize[i])) { //
	 * candidates.addAll(getCelldp(dp)); // } }
	 * 
	 * candidates.sort(Comparator.comparingDouble(o ->
	 * m.get(o.getID()).getDistanceSq(loc))); objs.addAll(candidates.subList(0,
	 * Math.min(candidates.size(), need - objs.size()))); candidates.clear();
	 * 
	 * currLayer++; }
	 * 
	 * return objs; }
	 */

	// Return a list of neighbors of the given object within the given radius
	public List<T> getNeighborsWithin(final T obj, final double radius) {
		Double2D tmp = null;
		try {
			tmp = m.get(obj.getID());
		} catch (Exception e) {
			// System.out.println( );
			// System.out.println(storage);
			System.exit(-1);
		}
		final Double2D loc = tmp;
		Int2D tmp2 = null;
		try {
			tmp2 = discretize(loc);
		} catch (Exception e) {

			// System.out.println(this.toString());
			// System.out.println("m: "+ m.keySet());
			// System.out.println("object "+obj+" loc "+loc);
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
		ansBr[0] = Math.min(dloc.x + offsets[0] + 1, dsize[0]);
		ansBr[1] = Math.min(dloc.y + offsets[1] + 1, dsize[1]);

		final Int2D br = new Int2D(ansBr);

//		// Collect all the objects that are not obj itself and within the given radius
//		for (final Int2D dp : IntPointGenerator.getBlock(ul, br)) {
// //			getCelldp(dp).stream()
// //					.filter(x -> x != obj && m.get(x.getID()).getDistanceSq(loc) <= radius * radius)
// //					.forEach(x -> objs.add(x));
//			for (T x : getCelldp(dp)) {
//				if (x != obj && m.get(x.getID()).getDistanceSq(loc) <= radius * radius) {
//					objs.add(x);
//				}
//			}
//		}

//<<<<<<< HEAD
		for (int x = ul.x; x < br.x; x++) {
			for (int y = ul.y; y < br.y; y++) {
				for (T foo : getCelldp(x, y).values()) {
					if (foo != obj && m.get(foo.getID()).distanceSq(loc) <= radius * radius) {
//=======
//		for(int x = ul.x; x < br.x; x++)
//			{
//			for(int y = ul.y; y < br.y; y++)
//				{
//				for(T foo: getCelldp(x, y))
//					{
//					if (foo != obj && m.get(foo).distanceSq(loc) <= radius * radius) 
//						{
//>>>>>>> 5a7347af137247b63139aa9f8f7b6717db8010f9
						objs.add(foo);
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
		return m.get(obj.getID());
	}

	// Get the location of the given location
	public Double2D getLocation(final long id) {
		return m.get(id);
	}

	///// GRIDSTORAGE GUNK

	// Put the object to the given point
	public void addToLocation(final T obj, final Double2D p) {
		final Double2D old = m.put(obj.getID(), p);
		if (old != null)
			getCell(old).remove(obj);
		getCell(p).put(obj.getID(), obj);
	}

	public void removeObject(Double2D p, final T obj) {
		getCell(m.remove(obj.getID())).remove(obj);
	}

	// Remove all the objects at the given point
	public void removeObjects(final Double2D p) {
//		getObjects(p).stream().forEach(obj -> removeObject(obj));
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
		getCell(m.remove(obj.getID())).remove(obj.getID());
	}

	// Get all the objects at the given point
	public ArrayList<T> getObjects(final Double2D p) {
		final ArrayList<T> objects = new ArrayList<>();
		for (final T t : getCell(p).values()) {
			if (m.get(t.getID()).equals(p))
				objects.add(t);
		}
		return objects;
	}

	public T getObjects(Double2D p, long id) {
		return getCell(p).get(id);
	}

	/// INTERNAL METHODS FOR PACKING AND UNPACKING

	// Remove all the objects inside the given rectangle
	void removeObjects(final IntRect2D r) {
//		getObjects(r).stream().forEach(obj -> removeObject(obj));
		for (T obj : getObjects(r)) {
			removeObject(obj);
		}
	}

	// Get all the objects inside the given rectangle
	List<T> getObjects(final IntRect2D r) {
		final ArrayList<T> objs = new ArrayList<T>();
		int[] offset = { 1, 1 };

		//final Int2D ul = discretize(r.ul()), br = discretize(r.br()).add(offset);
		final Int2D ul = discretize(r.ul()), br = discretize(r.br()).add(offset);

//		for (final Int2D dp : IntPointGenerator.getBlock(ul, br)) {
// //			getCelldp(dp)
// //				.stream()
// //				.filter(obj -> r.contains(m.get(obj.getID()))	// 
// //				.forEach(obj -> objs.add(obj));
//			for (T obj : getCelldp(dp)) {
//				if (r.contains(m.get(obj.getID()))) {
//					objs.add(obj);
//				}
//			}
//		}

// I believe this code is just doing:

		for (int x = ul.x; x < br.x; x++) {
			for (int y = ul.y; y < br.y; y++) {
				for (T obj : getCelldp(x, y).values()) { //need to offset/discretize!
					if (r.contains(m.get(obj.getID()))) {
						objs.add(obj);
					}
				}
			}
		}

		return objs;
	}

}
