package sim.field.storage;

import java.io.Serializable;
import java.util.*;
import sim.engine.*;
import sim.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public class ObjectGridStorage<T extends DObject> extends GridStorage<T> {
	private static final long serialVersionUID = 1L;

	public T[] storage;

	public ObjectGridStorage(final IntRect2D shape) {
		super(shape);
		clear();
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
		final T[] stor = (T[]) storage;
		final T[] objs = (T[]) buf;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList())
				stor[getFlatIdx(p)] = objs[curr++];

		return curr;
	}





	public void set(Int2D p, T t) {
		storage[getFlatIdx((Int2D) p)] = t;
	}

	public void addObject(Int2D p, T t) {
		set(p, t);
	}

	public T getObject(Int2D p, long id) {
		return storage[getFlatIdx((Int2D) p)];
	}

	// Don't call this method, it'd be foolish
	public ArrayList<T> getAllObjects(Int2D p) {
		ArrayList<T> list = new ArrayList<T>();
		list.add(storage[getFlatIdx(p)]);
		return list;
	}

	public boolean removeObject(Int2D p, long id) {
		set(p, null);
		return true;
	}

	public void clear(Int2D p) {
		set(p, null);
	}

	public void clear() {
		storage = (T[]) new Object[shape.getArea()];
	}


}
