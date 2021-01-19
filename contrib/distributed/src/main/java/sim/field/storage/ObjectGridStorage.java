package sim.field.storage;

import java.io.Serializable;
import java.util.function.IntFunction;

import sim.field.partitioning.IntRect2D;
import sim.util.MPIParam;
import sim.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public class ObjectGridStorage<T extends Serializable> extends GridStorage<T, Int2D> {

	public T[] storage;

	public ObjectGridStorage(final IntRect2D shape) {
		super(shape);
		clear();
		// storage = allocate(shape.getArea());
	}

	/*
	 * public GridStorage<T, Int2D> getNewStorage(final IntRect2D shape) { return
	 * new ObjectGridStorage<T>(shape); }
	 */

// This is "weak", perhaps we should change it to "strong" checking
// See https://stackoverflow.com/questions/529085/how-to-create-a-generic-array-in-java
	public void clear() {
// We don't really need this to compile:    @SuppressWarnings("unchecked") 	
		storage = (T[]) new Object[shape.getArea()]; // alloc.apply(size);
	}

	public String toString() {
		final int[] size = shape.getSizes();
		final StringBuffer buf = new StringBuffer(
				String.format("ObjectGridStorage<%s>-%s\n", storage.getClass().getSimpleName(), shape));

		for (int i = 0; i < size[0]; i++) {
			for (int j = 0; j < size[1]; j++)
				buf.append(String.format(" %8s ", storage[i * size[1] + j]));
			buf.append("\n");
		}

		return buf.toString();
	}

	public Serializable pack(final MPIParam mp) {
		final T[] objs = (T[]) new Object[mp.size];
		final T[] stor = storage;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList())
				objs[curr++] = stor[getFlatIdx(p)];

		return objs;
	}

	public int unpack(final MPIParam mp, final Serializable buf) {
//		System.out.println(buf);
		final T[] stor = (T[]) storage;
		final T[] objs = (T[]) buf;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList())
				stor[getFlatIdx(p)] = objs[curr++];

		return curr;
	}

	public void addToLocation(T obj, Int2D p) {
		storage[getFlatIdx(p)] = obj;
	}

//	public NumberND getLocation(T obj) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public void removeObject(Int2D p, T obj) {
		addToLocation(null, p);
	}

	public void removeObjects(Int2D p) {
		addToLocation(null, p);
	}

	public T getObjects(Int2D p) {
		return (T) storage[getFlatIdx(p)];
	}

//	public static void main(final String[] args) throws MPIException {
//		MPI.Init(args);
//
//		final Int2D p1 = new Int2D(new int[] { 0, 0 });
//		final Int2D p2 = new Int2D(new int[] { 5, 5 });
//		final Int2D p3 = new Int2D(new int[] { 1, 1 });
//		final Int2D p4 = new Int2D(new int[] { 4, 4 });
//		final IntRect2D r1 = new IntRect2D(0, p1, p2);
//		final IntRect2D r2 = new IntRect2D(1, p3, p4);
//		final ObjectGridStorage<TestObj> s1 = new ObjectGridStorage<TestObj>(r1, size -> new TestObj[size]);
//		final ObjectGridStorage<TestObj> s2 = new ObjectGridStorage<TestObj>(r1, size -> new TestObj[size]);
//
//		final TestObj[] stor = (TestObj[]) s1.getStorage();
//		for (final int i : new int[] { 6, 12, 18 })
//			stor[i] = new TestObj(i);
//
//		final MPIParam mp = new MPIParam(r2, r1, s1.getMPIBaseType());
//		s2.unpack(mp, s1.pack(mp));
//
//		final TestObj[] objs = (TestObj[]) s2.getStorage();
//		for (final TestObj obj : objs)
//			System.out.print(obj + " ");
//		System.out.println("");
//
//		MPI.Finalize();
//	}
}
