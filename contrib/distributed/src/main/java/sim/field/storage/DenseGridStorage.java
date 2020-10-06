package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.IntFunction;

import sim.field.partitioning.IntHyperRect;
import sim.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public class DenseGridStorage<T extends Serializable> extends GridStorage<T, Int2D> {

	IntFunction<ArrayList<T>[]> alloc; // Lambda function which accepts the size as its argument and returns a T array

	public DenseGridStorage(final IntHyperRect shape, final IntFunction<ArrayList<T>[]> allocator) {
		super(shape);

		alloc = allocator;
		storage = allocate(shape.getArea());
	}

	public GridStorage<T, Int2D> getNewStorage(final IntHyperRect shape) {
		return new DenseGridStorage<T>(shape, alloc);
	}

	protected Object[] allocate(final int size) {
		return alloc.apply(size);
	}

	public String toString() {
		final int[] size = shape.getSize();
		final ArrayList<T>[] array = (ArrayList[]) storage;
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
		final ArrayList<T>[] objs = alloc.apply(mp.size);
		final ArrayList<T>[] stor = (ArrayList<T>[]) storage;
		int curr = 0;

		for (final IntHyperRect rect : mp.rects)
			for (final Int2D p : rect)
				objs[curr++] = stor[getFlatIdx(p)];

		return objs;
	}

	public int unpack(final MPIParam mp, final Serializable buf) {
//		System.out.println(buf);
		final ArrayList<T>[] stor = (ArrayList<T>[]) storage;
		final ArrayList<T>[] objs = (ArrayList<T>[]) buf;
		int curr = 0;

		for (final IntHyperRect rect : mp.rects)
			for (final Int2D p : rect)
				stor[getFlatIdx(p)] = objs[curr++];

		return curr;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<T>[] getStorageArray() {
		return (ArrayList[]) getStorage();
	}

	public void addToLocation(T obj, Int2D p) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = getFlatIdx(p);

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(obj);
	}

//	public NumberND getLocation(T obj) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public void removeObject(T obj, Int2D p) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = getFlatIdx(p);

		if (array[idx] != null)
			array[idx].remove(obj);
	}

	public void removeObjects(Int2D p) {
		final ArrayList<T>[] array = getStorageArray();
		final int idx = getFlatIdx(p);

		if (array[idx] != null)
			array[idx].clear();
	}

	public ArrayList<T> getObjects(Int2D p) {
		return getStorageArray()[getFlatIdx(p)];
	}

}
