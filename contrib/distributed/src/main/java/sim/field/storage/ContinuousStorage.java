package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import sim.field.partitioning.IntRect2D;
import sim.util.MPIParam;
import sim.util.*;

public class ContinuousStorage<T extends Serializable> extends GridStorage<T, Double2D> {

	int[] dsize;
	double discretization;
	public HashMap<T, Double2D> m;

	public ContinuousStorage(final IntRect2D shape, final double discretization) {
		super(shape);

		this.discretization = discretization;
		storage = allocate(shape.getArea());
	}


	public GridStorage getNewStorage(final IntRect2D shape) {
		return new ContinuousStorage<>(shape, discretization);
	}

	protected Object[] allocate(final int size) {
		this.dsize = new int[2];					// 2 = num dimensions
		for (int i = 0; i < this.dsize.length; i++) {

			this.dsize[i] = (int) Math.ceil(shape.getSizes()[i] / discretization) + 1;	
		}
		
		// Overwrite the original stride with the new stride of dsize;
		// so that getFlatIdx() can correctly get the cell index of a discretized point
		// TODO better approach?
		stride = getStride(dsize);
		this.m = new HashMap<T, Double2D>();

		int volume = 1;
		for (int i = 0; i < this.dsize.length; i++) { // <- size of 2
			volume *= this.dsize[i];
		}
		HashSet[] set = new HashSet[volume];
		for (int i = 0; i < volume; i++) {
			set[i] = new HashSet();
		}
		return set;
	}

	public String toString() {
		final StringBuffer string = new StringBuffer(String.format("ContStorage-%s\n", shape));

//		TODO: Should we use StringBuilder here?
//		StringBuffer uses synchronization and is generally slower than StringBuilder
//		final StringBuilder string = new StringBuilder(String.format("ContStorage-%s\n", shape));

//		for (final Int2D dp : IntPointGenerator.getBlock(dsize))
//			if (getCelldp(dp).size() > 0)
//				string.append("Cell " + dp + ":\t" + getCelldp(dp) + "\n");

		for (int x = 0; x < dsize[0]; x++) 
			{
			for (int y = 0; y < dsize[1]; y++) 
				{
				HashSet<T> cell = getCelldp(x, y);
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
				objs.add(m.get(obj).rshift(shape.ul().toArray()).rshift(rect.ul().toArray()));
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
						.shift(mp.rects.get(k).ul().toArray()).shift(shape.ul().toArray()));

//		return objs.stream().mapToInt(x -> x.size()).sum();
		int sum = 0;
		for (int i = 0; i < objs.size(); i++) {
			sum += objs.get(i).size();
		}
		return sum;
	}

	public Int2D discretize(final NumberND p) {

		final double[] offsets = shape.ul().getOffsetsDouble(p);
		
		int[] ans = new int[2];
		for (int i = 0; i < offsets.length; i++) {
			ans[i] = -(int) (offsets[i] / discretization);
		}
		return new Int2D(ans);
	}

	public void clear() {
		storage = allocate(shape.getArea());
	}

	// Get the corresponding cell given a continuous point
	public HashSet<T> getCell(final Double2D p) {
		return getCelldp(discretize(p));
	}

	// Get the corresponding cell given a discretized point
	public HashSet<T> getCelldp(final Int2D p) {
		return ((HashSet<T>[]) storage)[getFlatIdx(p)];
	}

	public HashSet<T> getCelldp(int x, int y) {
		return ((HashSet<T>[]) storage)[getFlatIdx(x, y)];
	}

	// Put the object to the given point
	public void addToLocation(final T obj, final Double2D p) {
		final Double2D old = m.put(obj, p);
		if (old != null)
			getCell(old).remove(obj);
		getCell(p).add(obj);
	}

	// Get the location of the given location
	public Double2D getLocation(final T obj) {
		return m.get(obj);
	}

	// Get all the objects at the given point
	public ArrayList<T> getObjects(final Double2D p) {
		final ArrayList<T> objects = new ArrayList<>();
		for (final T t : getCell(p)) {
			if (m.get(t).equals(p))
				objects.add(t);
		}
		return objects;
	}

	// Get all the objects inside the given rectangle
	public List<T> getObjects(final IntRect2D r) {
		final ArrayList<T> objs = new ArrayList<T>();

		final Int2D ul = discretize(r.ul()), br = discretize(r.br()).shift(1);

//		for (final Int2D dp : IntPointGenerator.getBlock(ul, br)) {
// //			getCelldp(dp)
// //				.stream()
// //				.filter(obj -> r.contains(m.get(obj)))	// 
// //				.forEach(obj -> objs.add(obj));
//			for (T obj : getCelldp(dp)) {
//				if (r.contains(m.get(obj))) {
//					objs.add(obj);
//				}
//			}
//		}

// I believe this code is just doing:

	for(int x = ul.x; x < br.x; x++)
		{
		for(int y = ul.y; y < br.y; y++)
			{
			for(T obj: getCelldp(x, y))
				{
				if (r.contains(m.get(obj))) 
					{
					objs.add(obj);
					}
				}
			}
		}

		return objs;
	}

	// Remove the object from the storage
	public void removeObject(final T obj) {
		getCell(m.remove(obj)).remove(obj);
	}

	public void removeObject(final T obj, Double2D p) {
		getCell(m.remove(obj)).remove(obj);
	}

	// Remove all the objects at the given point
	public void removeObjects(final Double2D p) {
//		getObjects(p).stream().forEach(obj -> removeObject(obj));
		for (T obj : getObjects(p)) {
			removeObject(obj);
		}
	}

	// Remove all the objects inside the given rectangle
	public void removeObjects(final IntRect2D r) {
//		getObjects(r).stream().forEach(obj -> removeObject(obj));
		for (T obj : getObjects(r)) {
			removeObject(obj);
		}
	}

/**
	// Return a list of k nearest neighbors sorted by their distances
	// to the query obj, if that many exists
	public List<T> getNearestNeighbors(final T obj, final int need) {
		final Double2D loc = m.get(obj);
		final Int2D dloc = discretize(loc);
//		final int maxLayer = IntStream.range(0, 2)	// 0, ..., 2 - 1		// 2 = num dimensions
//				.map(i -> Math.max(dloc.c(i), dsize[i] - dloc.c(i)))
//				.max()
//				.getAsInt();
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < 2; i++) 		// 2 = num dimensions
			{
			int mapI = Math.max(dloc.c(i), dsize[i] - dloc.c(i));
			if (mapI > max) {
				max = mapI;
			}
		}
		final int maxLayer = max;
		
		final ArrayList<T> objs = new ArrayList<T>();
		final ArrayList<T> candidates = new ArrayList<T>(getCelldp(dloc));
		candidates.remove(obj); // remove self

		int currLayer = 1;

		while (objs.size 

		while (objs.size() < need && currLayer <= maxLayer) {
			for (final Int2D dp : IntPointGenerator.getLayer(dloc, currLayer)) {
				boolean flag = true;
				for (int i = 0; i < 2; i++) 			// 2 = num dimensions
					{
					if (!(dp.c(i) >= 0 && dp.c(i) < dsize[i])) {
						flag = false;
						break;
					}
				}
				if (flag) {
					candidates.addAll(getCelldp(dp));
				}
//				if (IntStream.range(0, 2)					// 2 = num dimensions
//						.allMatch(i -> dp.c(i) >= 0 && dp.c(i) < dsize[i])) {
//					candidates.addAll(getCelldp(dp));
//				}
			}

			candidates.sort(Comparator.comparingDouble(o -> m.get(o).getDistanceSq(loc)));
			objs.addAll(candidates.subList(0, Math.min(candidates.size(), need - objs.size())));
			candidates.clear();

			currLayer++;
		}

		return objs;
	}
*/


	// Return a list of neighbors of the given object within the given radius
	public List<T> getNeighborsWithin(final T obj, final double radius) {
		Double2D tmp = null;
		try {
			tmp = m.get(obj);
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
		for (int i = 0; i < offsets.length; i++)
			{
			offsets[i] = (int) Math.ceil(radius / discretization);
		}

		// Generate the start/end point subject to the boundaries
		int[] ansUl = new int[2];						// 2 = num dimensions
		ansUl[0] = Math.max(dloc.x - offsets[0], 0);
		ansUl[1] = Math.max(dloc.y - offsets[1], 0);
		final Int2D ul = new Int2D(ansUl);
		
		int[] ansBr = new int[2];						// 2 = num dimensions
		ansBr[0] = Math.min(dloc.x + offsets[0] + 1, dsize[0]);
		ansBr[1] = Math.min(dloc.y + offsets[1] + 1, dsize[1]);

		final Int2D br = new Int2D(ansBr);

//		// Collect all the objects that are not obj itself and within the given radius
//		for (final Int2D dp : IntPointGenerator.getBlock(ul, br)) {
// //			getCelldp(dp).stream()
// //					.filter(x -> x != obj && m.get(x).getDistanceSq(loc) <= radius * radius)
// //					.forEach(x -> objs.add(x));
//			for (T x : getCelldp(dp)) {
//				if (x != obj && m.get(x).getDistanceSq(loc) <= radius * radius) {
//					objs.add(x);
//				}
//			}
//		}

		for(int x = ul.x; x < br.x; x++)
			{
			for(int y = ul.y; y < br.y; y++)
				{
				for(T foo: getCelldp(x, y))
					{
					if (foo != obj && m.get(foo).getDistanceSq(loc) <= radius * radius) 
						{
						objs.add(foo);
						}
					}
				}
			}

		return objs;
	}

	public HashSet[] getStorageArray() {
		return (HashSet[]) getStorage();
	}

	public HashMap<T, Double2D> getStorageObjects() {
		return m;
	}

	public double getDiscretization() {
		return discretization;
	}



//	public static void main(final String[] args) throws mpi.MPIException {
//		mpi.MPI.Init(args);
//
//		final Int2D ul = new Int2D(10, 20), br = new Int2D(50, 80);
//		final IntRect2D rect = new IntRect2D(1, ul, br);
//		final double[] discretize = new double[] { 10, 10 };
//
//		final ContStorage<TestObj> f = new ContStorage<TestObj>(rect, discretize);
//
//		final TestObj obj1 = new TestObj(1);
//		final Double2D loc1 = new Double2D(23.4, 30.2);
//		final TestObj obj2 = new TestObj(2);
//		final Double2D loc2 = new Double2D(29.99, 39.99);
//		final TestObj obj3 = new TestObj(3);
//		final Double2D loc3 = new Double2D(31, 45.6);
//		final TestObj obj4 = new TestObj(4);
//		final Double2D loc4 = new Double2D(31, 45.6);
//		final TestObj obj5 = new TestObj(5);
//		final Double2D loc5 = new Double2D(31, 45.60001);
//
//		f.setLocation(obj1, loc1);
//		f.setLocation(obj2, loc2);
//		f.setLocation(obj3, loc3);
//		f.setLocation(obj4, loc4);
//		f.setLocation(obj5, loc5);
//
//		System.out.println("get objects at " + loc1);
//		for (final TestObj obj : f.getObjects(loc1))
//			System.out.println(obj);
//
//		System.out.println("get objects at " + loc4);
//		for (final TestObj obj : f.getObjects(loc4))
//			System.out.println(obj);
//
//		System.out.println("get objects at " + loc5);
//		for (final TestObj obj : f.getObjects(loc5))
//			System.out.println(obj);
//
//		final IntRect2D area = new IntRect2D(1, new Int2D(25, 35), new Int2D(35, 47));
//		System.out.println("get objects in " + area);
//		for (final TestObj obj : f.getObjects(area))
//			System.out.println(obj);
//
//		System.out.println("Move " + obj4 + " from " + loc4 + " to " + loc5 + ", get objects at " + loc4);
//		f.setLocation(obj4, loc5);
//		for (final TestObj obj : f.getObjects(loc4))
//			System.out.println(obj);
//		System.out.println("get objects at " + loc5);
//		for (final TestObj obj : f.getObjects(loc5))
//			System.out.println(obj);
//
//		final IntRect2D r1 = new IntRect2D(-1, new Int2D(20, 30), new Int2D(31, 41));
//		System.out.println("get objects in " + r1);
//		for (final TestObj obj : f.getObjects(r1))
//			System.out.println(obj);
//
//		for (int count = 1; count <= 5; count++) {
//			System.out.println("get " + count + " neighbors of " + obj2);
//			for (final TestObj obj : f.getNearestNeighbors(obj2, count))
//				System.out.println(obj);
//		}
//
//		double r = 9;
//		System.out.println("get objects within " + r + " from " + obj2);
//		for (final TestObj obj : f.getNeighborsWithin(obj2, r))
//			System.out.println(obj);
//
//		r = 12;
//		System.out.println("get objects within " + r + " from " + obj2);
//		for (final TestObj obj : f.getNeighborsWithin(obj2, r))
//			System.out.println(obj);
//
//		final IntRect2D r2 = new IntRect2D(-1, new Int2D(20, 30), new Int2D(31, 41));
//		System.out.println("after remove " + obj1 + ", get objects in " + r2);
//		f.removeObject(obj1);
//		for (final TestObj obj : f.getObjects(r2))
//			System.out.println(obj);
//
//		System.out.println("after remove object at " + loc3 + ", get objects in " + rect);
//		f.removeObjects(loc3);
//		for (final TestObj obj : f.getObjects(rect))
//			System.out.println(obj);
//
//		f.setLocation(obj1, loc1);
//		f.setLocation(obj3, loc3);
//		f.setLocation(obj4, loc4);
//		System.out.println("after putting " + obj1 + " " + obj3 + " " + obj4 + " " + "back, get objects in " + rect);
//		System.out.println(f);
//
//		final Int2D ul2 = new Int2D(20, 30), br2 = new Int2D(30, 40);
//		final IntRect2D rect2 = new IntRect2D(2, ul2, br2);
//		f.reshape(rect2);
//		System.out.println("after reshaping from " + rect + " to " + rect2 + ", get objects in " + rect2);
//		System.out.println(f);
//
//		r = 12;
//		System.out.println("get objects within " + r + " from " + obj1);
//		for (final TestObj obj : f.getNeighborsWithin(obj1, r))
//			System.out.println(obj);
//
//		System.out.println("get objects at " + loc1);
//		for (final TestObj obj : f.getObjects(loc1))
//			System.out.println(obj);
//
//		mpi.MPI.Finalize();
//	}
}
