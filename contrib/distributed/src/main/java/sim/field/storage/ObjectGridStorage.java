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
public class ObjectGridStorage<T extends DObject> extends GridStorage<T>
{
	private static final long serialVersionUID = 1L;

	public T[] storage;

	public ObjectGridStorage(final IntRect2D shape)
	{
		super(shape);
		clear();
	}

	public String toString()
	{
		int width = shape.getWidth();
		int height = shape.getHeight();
		final StringBuffer buf = new StringBuffer(
				String.format("ObjectGridStorage<%s>-%s\n", storage.getClass().getSimpleName(), shape));

		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
				buf.append(String.format(" %8s ", storage[i * height + j]));
			buf.append("\n");
		}

		return buf.toString();
	}

	public Serializable pack(final MPIParam mp)
	{
		final T[] objs = (T[]) new Object[mp.size];
		final T[] stor = storage;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList())
				objs[curr++] = stor[getFlatIndex(p)];

		return objs;
	}

	public void unpack(final MPIParam mp, final Serializable buf)
	{
		final T[] stor = (T[]) storage;
		final T[] objs = (T[]) buf;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList())
				stor[getFlatIndex(p)] = objs[curr++];
	}

	public T get(Int2D p)
	{
		return storage[getFlatIndex((Int2D) p)];
	}

	public void set(Int2D p, T t)
	{
		storage[getFlatIndex((Int2D) p)] = t;
	}

	public T get(int x, int y)
	{
		return storage[getFlatIndex(x, y)];
	}

	public void set(int x, int y, T t)
	{
		storage[getFlatIndex(x, y)] = t;
	}


	public void addObject(NumberND p, T t)
	{
		Int2D local_p = toLocalPoint((Int2D) p);

		set(local_p, t);
	}

	public T getObject(NumberND p, long id)
	{
		Int2D local_p = toLocalPoint((Int2D) p);
		
		return storage[getFlatIndex(local_p)];
	}

	// Don't call this method, it'd be foolish
	public ArrayList<T> getAllObjects(NumberND p)
	{
		Int2D local_p = toLocalPoint((Int2D) p);

		
		ArrayList<T> list = new ArrayList<T>();
		list.add(storage[getFlatIndex(local_p)]);
		return list;
	}

	public boolean removeObject(NumberND p, long id)
	{
		Int2D local_p = toLocalPoint((Int2D) p);
		set(local_p, null);
		return true;
	}

	public void clear(NumberND p)
	{
		Int2D local_p = toLocalPoint((Int2D) p);
		set(local_p, null);
	}

	public void clear()
	{
		storage = (T[]) new Object[shape.getArea()];
	}
}
