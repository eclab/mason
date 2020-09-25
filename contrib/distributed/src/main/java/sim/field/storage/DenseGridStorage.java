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
public class DenseGridStorage<T extends Serializable> extends GridStorage {

	IntFunction<ArrayList<T>[]> alloc; // Lambda function which accepts the size as its argument and returns a T array

	public DenseGridStorage(final IntHyperRect shape, final IntFunction<ArrayList<T>[]> allocator) {
		super(shape);

		alloc = allocator;
		storage = allocate(shape.getArea());
	}

	public GridStorage getNewStorage(final IntHyperRect shape) {
		return new DenseGridStorage(shape, alloc);
	}

	protected Object[] allocate(final int size) {
		return alloc.apply(size);
	}

	public String toString() {
		final int[] size = shape.getSize();
		final ArrayList[] array = (ArrayList[]) storage;
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
		final ArrayList[] objs = alloc.apply(mp.size), stor = (ArrayList[]) storage;
		int curr = 0;

		for (final IntHyperRect rect : mp.rects)
			for (final Int2D p : rect)
				objs[curr++] = stor[getFlatIdx(p)];

		return objs;
	}

	public int unpack(final MPIParam mp, final Serializable buf) {
//		System.out.println(buf);
		final ArrayList[] stor = (ArrayList[]) storage;
		final ArrayList[] objs = (ArrayList[]) buf;
		int curr = 0;

		for (final IntHyperRect rect : mp.rects)
			for (final Int2D p : rect)
				stor[getFlatIdx(p)] = objs[curr++];

		return curr;
	}

	@SuppressWarnings("unchecked")
	public ArrayList[] getStorageArray() {
		return (ArrayList[]) getStorage();
	}

	public void addToLocation(Serializable obj, NumberND p) {
		final ArrayList[] array = getStorageArray();
		final int idx = getFlatIdx((Int2D) p);

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(obj);

//		getStorageArray()[getFlatIdx((Int2D) p)] = obj;
	}

//	public NumberND getLocation(T obj) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public void removeObject(Serializable obj, NumberND p) {
		addToLocation(null, p);
	}

	public void removeObjects(NumberND p) {
		addToLocation(null, p);
	}

	public ArrayList getObjects(NumberND p) {
		return (ArrayList) getStorageArray()[getFlatIdx((Int2D) p)];
	}
}
