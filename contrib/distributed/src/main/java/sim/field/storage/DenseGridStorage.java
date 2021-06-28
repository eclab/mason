package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.IntFunction;

import sim.app.dheatbugs.DHeatBug;
import sim.app.dheatbugs.DHeatBugs;
import sim.engine.DObject;
import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.*;

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
			//System.out.println("packing "+rect);
			for (final Int2D p : rect.getPointList())
			{
				
				//local_p = toLocalPoint(p);

				
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
			//System.out.println("unpacking "+rect);
			for (final Int2D p : rect.getPointList())
			{
				

				

				stor[getFlatIndex(p)] = objs[curr++];
				
				/*
				stor[getFlatIndex(p)] = objs[curr];
				
				if (stor[getFlatIndex(p)] != null) {
					for (T t : stor[getFlatIndex(p)]) {
						if (t instanceof Stoppable) {
							((Stoppable)t).stop();
						}
					}
				}
				curr++;
				*/
				
				
				

			}
		}

		//return curr;
	}



	///// GRIDSTORAGE GUNK

	public void addObject(NumberND p, T t)
	{
		//System.out.println("adding obj "+t+" to"+p);
		
		Int2D local_p = toLocalPoint((Int2D) p);
		final ArrayList<T>[] array = storage;
		final int idx = getFlatIndex(local_p);

		if (array[idx] == null)
			array[idx] = new ArrayList<T>();

		array[idx].add(t);
		
		//DSimState.loc_disagree((Int2D)p, (DHeatBug) t, null, "addObject");
		
		if (!getAllObjects(p).contains(t))
		{
			System.out.println("not added to correct location");
			System.exit(-1);
		}
		
	}

	public T getObject(NumberND p, long id)
	{
		Int2D local_p = toLocalPoint((Int2D) p);
		ArrayList<T> ts = storage[getFlatIndex(local_p)];
		if (ts != null)
			{
			for (T t : ts)
				if (t.ID() == id)
					return t;
			}
		return null;
	}

	public ArrayList<T> getAllObjects(NumberND p)
	{
		Int2D local_p = toLocalPoint((Int2D) p);
		return storage[getFlatIndex(local_p)];
	}

	//Does this need to be adapted to convert to local???
	boolean removeFast(ArrayList<T> list, int pos)
		{
		int top = list.size() - 1;
		if (top != pos)
			list.set(pos, list.get(top));
		return list.remove(top) != null;
		}

/*
	boolean removeFast(ArrayList<T> list, T t)
		{
		int pos = list.indexOf(t);
		if (pos >= 0)
			return removeFast(list, pos);
		else return (pos >= 0);
		}
*/

	public boolean removeObject(NumberND p, long id)
	{
		Int2D local_p = toLocalPoint((Int2D) p);
		final ArrayList<T>[] array = storage;
		final int idx = getFlatIndex(local_p);
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

	public void clear(NumberND p)
	{
		Int2D local_p = toLocalPoint((Int2D) p);
		final ArrayList<T>[] array = storage;
		final int idx = getFlatIndex(local_p);

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
