package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.IntFunction;

import sim.field.partitioning.IntRect2D;
import sim.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public class DenseGridStorage<T extends Serializable> extends GridStorage<T, Int2D> {
	public ArrayList<T>[] storage;
	
	/// SEAN -- for the time being we're just doing removeEmptyBags, not replaceLargeBags, though we really oughta do that too...
	/// See DenseGrid2D
	
    /** Should we remove bags in the field if they have been emptied, and let them GC, or should
        we keep them around? */
    public boolean removeEmptyBags = true;

	public DenseGridStorage(final IntRect2D shape) {
		super(shape);
		clear();
		//storage = allocate(shape.getArea());
	}

/*
	public GridStorage<T, Int2D> getNewStorage(final IntRect2D shape) {
		return new DenseGridStorage<T>(shape);
	}
*/

	public void clear() {
// We don't really need this to compile:    @SuppressWarnings("unchecked") 	
    storage = (ArrayList<T>[]) new Object[shape.getArea()];		//alloc.apply(size);
	}

	public String toString() {
		final int[] size = shape.getSizes();
		final ArrayList<T>[] array = (ArrayList[]) storage;
		final StringBuffer buf = new StringBuffer(
				String.format("ObjectGridStorage<%s>-%s\n", array.getClass().getSimpleName(), shape));

			for (int i = 0; i < size[0]; i++) {
				for (int j = 0; j < size[1]; j++)
					buf.append(String.format(" %8s ", array[i * size[1] + j]));
				buf.append("\n");
			}

		return buf.toString();
	}

	public Serializable pack(final MPIParam mp) {
		final ArrayList<T>[] objs = (ArrayList<T>[]) new Object[mp.size]; 	// alloc.apply(mp.size);
		final ArrayList<T>[] stor = (ArrayList<T>[]) storage;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList())
				objs[curr++] = stor[getFlatIdx(p)];

		return objs;
	}

	public int unpack(final MPIParam mp, final Serializable buf) {
//		System.out.println(buf);
		final ArrayList<T>[] stor = (ArrayList<T>[]) storage;
		final ArrayList<T>[] objs = (ArrayList<T>[]) buf;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList())
				stor[getFlatIdx(p)] = objs[curr++];

		return curr;
	}

	public void addToLocation(T obj, Int2D p) {
		final ArrayList<T>[] array = storage;
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
		final ArrayList<T>[] array = storage;
		final int idx = getFlatIdx(p);

		if (array[idx] != null)
			{
			array[idx].remove(obj);
			if (array[idx].size() == 0 && removeEmptyBags)
				array[idx] = null;
			}
	}

	public void removeObjects(Int2D p) {
		final ArrayList<T>[] array = storage;
		final int idx = getFlatIdx(p);

		if (array[idx] != null)
			{
			if (removeEmptyBags)
				array[idx] = null;
			else
				array[idx].clear();
			}
	}

	public ArrayList<T> getObjects(Int2D p) {
		return storage[getFlatIdx(p)];
	}

}
