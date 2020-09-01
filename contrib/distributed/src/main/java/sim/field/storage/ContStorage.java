package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import mpi.MPI;
import mpi.MPIException;
import sim.field.partitioning.IntHyperRect;
import sim.field.partitioning.IntPointGenerator;
import sim.util.MPIParam;
import sim.util.*;

public class ContStorage<T extends Serializable> extends GridStorage<T> {

	int[] dsize;
	double[] discretizations;
	public HashMap<T, NumberND> m;

	public ContStorage(final IntHyperRect shape, final double[] discretizations) {
		super(shape);

		this.discretizations = discretizations;
		storage = allocate(shape.getArea());
	}

	public GridStorage getNewStorage(final IntHyperRect shape) {
		return new ContStorage<>(shape, discretizations);
	}

	protected Object allocate(final int size) {
		this.dsize = IntStream.range(0, shape.getNd())
				.map(i -> (int) Math.ceil(shape.getSize()[i] / discretizations[i]) + 1).toArray();
		// Overwrite the original stride with the new stride of dsize;
		// so that getFlatIdx() can correctly get the cell index of a discretized point
		// TODO better approach?
		stride = getStride(dsize);
		this.m = new HashMap<T, NumberND>();
		return IntStream.range(0, Arrays.stream(this.dsize).reduce(1, (x, y) -> x * y))
				.mapToObj(i -> new HashSet<>()).toArray(s -> new HashSet[s]);
	}

	public String toString() {
		final StringBuffer buf = new StringBuffer(String.format("ContStorage-%s\n", shape));

		for (final Int2D dp : IntPointGenerator.getBlock(dsize))
			if (getCelldp(dp).size() > 0)
				buf.append("Cell " + dp + ":\t" + getCelldp(dp) + "\n");

		return buf.toString();
	}

	// This returns a list of a list of dissimilar Objects
	// They are either of type T or of type NumberND
	public Serializable pack(final MPIParam mp) {
		final ArrayList<ArrayList<Serializable>> ret = new ArrayList<>();

		for (final IntHyperRect rect : mp.rects) {
			final ArrayList<Serializable> objs = new ArrayList<>();
			for (final T obj : getObjects(rect.shift(shape.ul().getArray()))) {
				objs.add(obj);
				// Append the object's location relative to the rectangle
				objs.add(m.get(obj).rshift(shape.ul().getArray()).rshift(rect.ul().getArray()));
			}
			ret.add(objs);
		}

		return ret;
	}

	public int unpack(final MPIParam mp, final Serializable buf) {
		final ArrayList<ArrayList<Serializable>> objs = (ArrayList<ArrayList<Serializable>>) buf;

		// Remove any objects that are in the unpack area (overwrite the area)
		// shift the rect with local coordinates back to global coordinates
		for (final IntHyperRect rect : mp.rects)
			removeObjects(rect.shift(shape.ul().getArray()));

		for (int k = 0; k < mp.rects.size(); k++)
			for (int i = 0; i < objs.get(k).size(); i += 2)
				setLocation((T) objs.get(k).get(i), ((NumberND) objs.get(k).get(i + 1))
						.shift(mp.rects.get(k).ul().getArray()).shift(shape.ul().getArray()));

		return objs.stream().mapToInt(x -> x.size()).sum();
	}

	public Int2D discretize(final NumberND p) {
		final double[] offsets = shape.ul().getOffsetsDouble(p);
		return new Int2D(IntStream.range(0, offsets.length)
				.map(i -> -(int) (offsets[i] / discretizations[i]))
				.toArray());
	}

	public void clear() {
		storage = allocate(shape.getArea());
	}

	// Get the corresponding cell given a continuous point
	public HashSet<T> getCell(final NumberND p) {
		return getCelldp(discretize(p));
	}

	// Get the corresponding cell given a discretized point
	public HashSet<T> getCelldp(final Int2D p) {
		return ((HashSet<T>[]) storage)[getFlatIdx(p)];
	}

	// Put the object to the given point
	public void setLocation(final T obj, final NumberND p) {
		final NumberND old = m.put(obj, p);
		if (old != null)
			getCell(old).remove(obj);
		getCell(p).add(obj);
	}

	// Get the location of the given location
	public NumberND getLocation(final T obj) {
		return m.get(obj);
	}

	// Get all the objects at the given point
	public ArrayList<T> getObjects(final NumberND p) {
		final ArrayList<T> objects = new ArrayList<>();
		for (final T t : getCell(p)) {
			if (m.get(t).equals(p))
				objects.add(t);
		}
		return objects;
	}

	// Get all the objects inside the given rectangle
	public List<T> getObjects(final IntHyperRect r) {
		final ArrayList<T> objs = new ArrayList<T>();

		final Int2D ul = discretize(r.ul()), br = discretize(r.br()).shift(1);
		for (final Int2D dp : IntPointGenerator.getBlock(ul, br))
			getCelldp(dp).stream().filter(obj -> r.contains(m.get(obj))).forEach(obj -> objs.add(obj));

		return objs;
	}

	// Remove the object from the storage
	public void removeObject(final T obj) {
		getCell(m.remove(obj)).remove(obj);
	}

	// Remove all the objects at the given point
	public void removeObjects(final NumberND p) {
		getObjects(p).stream().forEach(obj -> removeObject(obj));
	}

	// Remove all the objects inside the given rectangle
	public void removeObjects(final IntHyperRect r) {
		getObjects(r).stream().forEach(obj -> removeObject(obj));
	}

	// Return a list of k nearest neighbors sorted by their distances
	// to the query obj, if that many exists
	public List<T> getNearestNeighbors(final T obj, final int need) {
		final NumberND loc = m.get(obj);
		final Int2D dloc = discretize(loc);
		final int maxLayer = IntStream.range(0, shape.getNd())
				.map(i -> Math.max(dloc.c(i), dsize[i] - dloc.c(i)))
				.max().getAsInt();
		final ArrayList<T> objs = new ArrayList<T>();
		final ArrayList<T> candidates = new ArrayList<T>(getCelldp(dloc));
		candidates.remove(obj); // remove self

		int currLayer = 1;

		while (objs.size() < need && currLayer <= maxLayer) {
			for (final Int2D dp : IntPointGenerator.getLayer(dloc, currLayer))
				if (IntStream.range(0, shape.getNd()).allMatch(i -> dp.c(i) >= 0 && dp.c(i) < dsize[i]))
					candidates.addAll(getCelldp(dp));

			candidates.sort(Comparator.comparingDouble(o -> m.get(o).getDistanceSq(loc)));
			objs.addAll(candidates.subList(0, Math.min(candidates.size(), need - objs.size())));
			candidates.clear();

			currLayer++;
		}

		return objs;
	}

	// Return a list of neighbors of the given object within the given radius
	public List<T> getNeighborsWithin(final T obj, final double radius) {
		NumberND tmp = null;
		try {
			tmp = m.get(obj);
		}catch (Exception e) {
			//System.out.println(  );
			//System.out.println(storage);
			System.exit(-1);
		}
		final NumberND loc = tmp;
		Int2D tmp2 = null;
		try {
			tmp2 = discretize(loc);
		}catch (Exception e) {
			
			//System.out.println(this.toString());
			//System.out.println("m: "+ m.keySet());
			//System.out.println("object "+obj+" loc "+loc);
			e.printStackTrace();
		}
		final Int2D dloc = tmp2;
		final ArrayList<T> objs = new ArrayList<T>();

		// Calculate how many discretized cells we need to search
		final int[] offsets = Arrays.stream(discretizations).mapToInt(x -> (int) Math.ceil(radius / x)).toArray();

		// Generate the start/end point subject to the boundaries
		final Int2D ul = new Int2D(
				IntStream.range(0, shape.getNd()).map(i -> Math.max(dloc.c(i) - offsets[i], 0)).toArray());
		final Int2D br = new Int2D(
				IntStream.range(0, shape.getNd()).map(i -> Math.min(dloc.c(i) + offsets[i] + 1, dsize[i])).toArray());

		// Collect all the objects that are not obj itself and within the given radius
		for (final Int2D dp : IntPointGenerator.getBlock(ul, br))
			getCelldp(dp).stream()
					.filter(x -> x != obj && m.get(x).getDistanceSq(loc) <= radius * radius)
					.forEach(x -> objs.add(x));

		return objs;
	}

//	public static void main(final String[] args) throws mpi.MPIException {
//		mpi.MPI.Init(args);
//
//		final Int2D ul = new Int2D(10, 20), br = new Int2D(50, 80);
//		final IntHyperRect rect = new IntHyperRect(1, ul, br);
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
//		final IntHyperRect area = new IntHyperRect(1, new Int2D(25, 35), new Int2D(35, 47));
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
//		final IntHyperRect r1 = new IntHyperRect(-1, new Int2D(20, 30), new Int2D(31, 41));
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
//		final IntHyperRect r2 = new IntHyperRect(-1, new Int2D(20, 30), new Int2D(31, 41));
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
//		final IntHyperRect rect2 = new IntHyperRect(2, ul2, br2);
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
