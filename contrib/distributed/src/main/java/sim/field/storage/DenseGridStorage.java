package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;

import sim.engine.DObject;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.MPIParam;
import sim.util.Number2D;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public class DenseGridStorage<T extends DObject> extends GridStorage<T>
{
	private static final long serialVersionUID = 1L;

	public ArrayList<T>[] storage;

	/**
	 * Should we remove bags in the field if they have been emptied, and let them
	 * GC, or should we keep them around?
	 */
	public boolean removeEmptyBags = true;
	
	

	public DenseGridStorage(final IntRect2D shape)
	{
		super(shape);
		clear();
	}

	public String toString()
	{
		int width = shape.getWidth();
		int height = shape.getHeight();
		final ArrayList<T>[] array = storage;
		final StringBuffer buf = new StringBuffer(
				String.format("ObjectGridStorage<%s>-%s\n", array.getClass().getSimpleName(), shape));

		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
				buf.append(String.format(" %8s ", array[i * height + j]));
			buf.append("\n");
		}

		return buf.toString();
	}

	public Serializable pack(final MPIParam mp)
	{
		final ArrayList<T>[] objs = new ArrayList[mp.size]; // alloc.apply(mp.size);
		final ArrayList<T>[] stor = storage;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
		{
			for (final Int2D p : rect.getPointList())
			{
				objs[curr++] = stor[getFlatIndex(p)];
			}
		}

		return objs;
	}

	public void unpack(final MPIParam mp, final Serializable buf)
	{
		final ArrayList<T>[] stor = (ArrayList<T>[]) storage;
		final ArrayList<T>[] objs = (ArrayList<T>[]) buf;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
		{
			for (final Int2D p : rect.getPointList())
			{
				stor[getFlatIndex(p)] = objs[curr++];
			}
		}
	}

	public ArrayList<T> get(Int2D p)
	{
		return storage[getFlatIndex((Int2D) p)];
	}

	public void set(Int2D p, ArrayList<T> t)
	{
		storage[getFlatIndex((Int2D) p)] = t;
	}

	public ArrayList<T> get(int x, int y)
	{
		return storage[getFlatIndex(x, y)];
	}

	public void set(int x, int y, ArrayList<T> t)
	{
		storage[getFlatIndex(x, y)] = t;
	}




	///// GRIDSTORAGE GUNK

	public void addObject(Number2D p, T t)
	{
		//System.out.println("adding obj "+t+" to"+p);
		
		Int2D localP = toLocalPoint((Int2D) p);
		final ArrayList<T>[] array = storage;
		final int idx = getFlatIndex(localP);

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(t);
				
		if (!getAllObjects(p).contains(t))
		{
			System.out.println("not added to correct location");
			System.exit(-1);
		}
		
	}

	public T getObject(Number2D p, long id)
	{
		Int2D localP = toLocalPoint((Int2D) p);
		ArrayList<T> ts = storage[getFlatIndex(localP)];
		if (ts != null)
			{
			for (T t : ts)
				if (t.ID() == id)
					return t;
			}
		return null;
	}

	public ArrayList<T> getAllObjects(Number2D p)
	{
		Int2D localP = toLocalPoint((Int2D) p);
		return storage[getFlatIndex(localP)];
	}

	//Does this need to be adapted to convert to local???
	boolean removeFast(ArrayList<T> list, int pos)
		{
		int top = list.size() - 1;
		if (top != pos)
			list.set(pos, list.get(top));
		return list.remove(top) != null;
		}

	public boolean removeObject(Number2D p, long id)
	{
		Int2D localP = toLocalPoint((Int2D) p);
		final ArrayList<T>[] array = storage;
		final int idx = getFlatIndex(localP);
		boolean result = false;

		if (array[idx] != null)
			{
			for (int i = 0; i < array[idx].size(); i++) 
				{
				T t = array[idx].get(i);
				if (t.ID() == id) 
					{
					result = removeFast(array[idx], i);
					if (array[idx].size() == 0 && removeEmptyBags)
						array[idx] = null;
					break;
					}
				}
			}
		return result;
	}

	public void clear(Number2D p)
	{
		Int2D localP = toLocalPoint((Int2D) p);
		final ArrayList<T>[] array = storage;
		final int idx = getFlatIndex(localP);

		if (array[idx] != null)
		{
			if (removeEmptyBags)
				array[idx] = null;
			else
				array[idx].clear();
		}
	}

	public void clear()
	{
		storage = new ArrayList[shape.getArea()];
	}
}
