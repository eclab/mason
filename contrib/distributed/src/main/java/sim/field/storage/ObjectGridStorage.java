package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;

import sim.engine.DObject;
import sim.util.GenericArray;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.MPIParam;
import sim.util.Number2D;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public class ObjectGridStorage<T extends DObject> extends GridStorage<T>
{
	private static final long serialVersionUID = 1L;

	//public T[] storage;
	public GenericArray<T> storage;

	public ObjectGridStorage(final IntRect2D shape)
	{
		super(shape);
		clear();
	}

	public String toString()
	{
		int width = shape.getWidth();
		int height = shape.getHeight();
		StringBuffer buf = new StringBuffer(String.format("ObjectGridStorage<%s>-%s\n", storage.getClass().getSimpleName(), shape));

		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{
				//buf.append(String.format(" %8s ", storage[i * height + j]));
				buf.append(String.format(" %8s ", storage.get(i * height + j)));
			}
			buf.append("\n");
		}

		return buf.toString();
	}

	public Serializable pack(final MPIParam mp)
	{
		//final T[] objs = (T[]) new Object[mp.size];
		//final T[] stor = storage;
		
		GenericArray<T> objs = new GenericArray<T>(mp.size);
		GenericArray<T> stor = storage;
		
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList()) 
			{
				//objs[curr++] = stor[getFlatIndex(p)];
				objs.set(curr++, stor.get(getFlatIndex(p)));
			}

		return objs;
	}

	public void unpack(final MPIParam mp, final Serializable buf)
	{
		//final T[] stor = (T[]) storage;
		//final T[] objs = (T[]) buf;

		 GenericArray<T> stor = storage;
		 GenericArray<T> objs = (GenericArray<T>) buf;
		int curr = 0;

		for (final IntRect2D rect : mp.rects)
			for (final Int2D p : rect.getPointList())
				{
				//stor[getFlatIndex(p)] = objs[curr++];
				stor.set(getFlatIndex(p),objs.get(curr++));
				}
	}

	public T get(Int2D p)
	{
		//return storage[getFlatIndex((Int2D) p)];
		return storage.get(getFlatIndex((Int2D) p));
	}

	public void set(Int2D p, T t)
	{
		//storage[getFlatIndex((Int2D) p)] = t;
		storage.set(getFlatIndex((Int2D) p), t);
	}

	public T get(int x, int y)
	{
		//return storage[getFlatIndex(x, y)];
		return storage.get(getFlatIndex(x, y));
	}

	public void set(int x, int y, T t)
	{
		//storage[getFlatIndex(x, y)] = t;
		storage.set(getFlatIndex(x, y), t);
	}


	public void addObject(Number2D p, T t)
	{
		Int2D localP = toLocalPoint((Int2D) p);

		set(localP, t);
	}

	public T getObject(Number2D p, long id)
	{
		Int2D localP = toLocalPoint((Int2D) p);
		
		//return storage[getFlatIndex(localP)];
		return storage.get(getFlatIndex(localP));
	}

	// Don't call this method, it'd be foolish
	public ArrayList<T> getAllObjects(Number2D p)
	{
		Int2D localP = toLocalPoint((Int2D) p);

		
		ArrayList<T> list = new ArrayList<T>();
		//list.add(storage[getFlatIndex(localP)]);
		list.add(storage.get(getFlatIndex(localP)));
		return list;
	}

	public boolean removeObject(Number2D p, long id)
	{
		Int2D localP = toLocalPoint((Int2D) p);
		set(localP, null);
		return true;
	}

	public void clear(Number2D p)
	{
		Int2D localP = toLocalPoint((Int2D) p);
		set(localP, null);
	}

	public void clear()
	{
		//storage = (T[]) new Object[shape.getArea()]; //this leads to a runtime error
		//storage = new T[shape.getArea()]; //this leads to a runtime error
		
		storage = new GenericArray<T>(shape.getArea());

	}
	

	
}




