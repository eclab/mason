package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;

import mpi.MPI;
import mpi.MPIException;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.MPIParam;
import sim.util.Number2D;

public class IntGridStorage extends GridStorage<Integer>
{
	private static final long serialVersionUID = 1L;

	public int[] storage;

	public IntGridStorage(final IntRect2D shape)
	{
		super(shape);
		clear();
	}

	public byte[] pack(MPIParam mp) throws MPIException
	{
		byte[] buf = new byte[MPI.COMM_WORLD.packSize(mp.size, MPI.INT)];
		MPI.COMM_WORLD.pack(MPI.slice((int[]) storage, mp.idx), 1, mp.type, buf, 0);
		return buf;
	}

	public void unpack(MPIParam mp, Serializable buf) throws MPIException
	{
		MPI.COMM_WORLD.unpack((byte[]) buf, 0, MPI.slice((int[]) storage, mp.idx), 1, mp.type);
	}

	public String toString()
	{
		int width = shape.getWidth();
		int height = shape.getHeight();
		int[] array = (int[]) storage;
		StringBuffer buf = new StringBuffer(String.format("IntGridStorage-%s\n", shape));

		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
				buf.append(String.format(" %4d ", array[i * height + j]));
			buf.append("\n");
		}

		return buf.toString();
	}

	public int get(Int2D p)
	{
		return storage[getFlatIndex((Int2D) p)];
	}

	public void set(Int2D p, int t)
	{
		storage[getFlatIndex((Int2D) p)] = t;
	}

	public int get(int x, int y)
	{
		return storage[getFlatIndex(x, y)];
	}

	public void set(int x, int y, int t)
	{
		storage[getFlatIndex(x, y)] = t;
	}

	public void addObject(Number2D p, Integer t)
	{
		Int2D localP = toLocalPoint((Int2D) p);

		set(localP, t);
	}

	public Integer getObject(Number2D p, long id)
	{
		Int2D localP = toLocalPoint((Int2D) p);

		return storage[getFlatIndex(localP)];
	}

	// Don't call this method, it'd be foolish
	public ArrayList<Integer> getAllObjects(Number2D p)
	{
		
		Int2D localP = toLocalPoint((Int2D) p);

		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(storage[getFlatIndex(localP)]);
		return list;
	}

	public boolean removeObject(Number2D p, long id)
	{
		Int2D localP = toLocalPoint((Int2D) p);

		set(localP, 0);
		return true;
	}

	public void clear(Number2D p)
	{
		Int2D localP = toLocalPoint((Int2D) p);

		set(localP, 0);
	}

	public void clear()
	{
		storage = new int[shape.getArea()];
	}
}
