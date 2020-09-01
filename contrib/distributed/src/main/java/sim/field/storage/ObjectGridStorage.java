package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.IntFunction;

import sim.field.partitioning.IntHyperRect;
import sim.util.MPIParam;
import sim.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public class ObjectGridStorage<T extends Serializable> extends GridStorage<T> {

	IntFunction<T[]> alloc; // Lambda function which accepts the size as its argument and returns a T array

	public ObjectGridStorage(final IntHyperRect shape, final IntFunction<T[]> allocator) {
		super(shape);

		alloc = allocator;
		storage = allocate(shape.getArea());
	}

	public GridStorage getNewStorage(final IntHyperRect shape) {
		return new ObjectGridStorage<T>(shape, alloc);
	}

	protected Object allocate(final int size) {
		return alloc.apply(size);
	}

	public String toString() {
		final int[] size = shape.getSize();
		final T[] array = (T[]) storage;
		final StringBuffer buf = new StringBuffer(
				String.format("ObjectGridStorage<%s>-%s\n", array.getClass().getSimpleName(), shape));

		if (shape.getNd() == 2)
			for (int i = 0; i < size[0]; i++) {
				for (int j = 0; j < size[1]; j++)
					buf.append(String.format(" %8s ", array[i * size[1] + j]));
				buf.append("\n");
			}

		return buf.toString();
	}

	public Serializable pack(final MPIParam mp) {
		final T[] objs = alloc.apply(mp.size), stor = (T[]) storage;
		int curr = 0;

		for (final IntHyperRect rect : mp.rects)
			for (final Int2D p : rect)
				objs[curr++] = stor[getFlatIdx(p)];

		return objs;
	}

	public int unpack(final MPIParam mp, final Serializable buf) {
//		System.out.println(buf);
		final T[] stor = (T[]) storage;
		final T[] objs = (T[]) buf;
		int curr = 0;

		for (final IntHyperRect rect : mp.rects)
			for (final Int2D p : rect)
				stor[getFlatIdx(p)] = objs[curr++];

		return curr;
	}

	@Override
	public void setLocation(T obj, NumberND p) {
		// TODO Auto-generated method stub

	}

	@Override
	public NumberND getLocation(T obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeObject(T obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeObjects(NumberND p) {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<T> getObjects(NumberND p) {
		// TODO Auto-generated method stub
		return null;
	}

//	public static void main(final String[] args) throws MPIException {
//		MPI.Init(args);
//
//		final Int2D p1 = new Int2D(new int[] { 0, 0 });
//		final Int2D p2 = new Int2D(new int[] { 5, 5 });
//		final Int2D p3 = new Int2D(new int[] { 1, 1 });
//		final Int2D p4 = new Int2D(new int[] { 4, 4 });
//		final IntHyperRect r1 = new IntHyperRect(0, p1, p2);
//		final IntHyperRect r2 = new IntHyperRect(1, p3, p4);
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
