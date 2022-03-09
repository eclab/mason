package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;

import mpi.Datatype;
import mpi.MPI;
import mpi.MPIException;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.MPIParam;
import sim.util.Number2D;

public class DoubleGridStorage extends GridStorage<Double>
{
	private static final long serialVersionUID = 1L;

	public double[] storage;

	public DoubleGridStorage(final IntRect2D shape)
	{
		super(shape);
		clear();
	}

	public Datatype getMPIBaseType()
		{
		return MPI.DOUBLE;
		}
		
	public byte[] pack(MPIParam mp) throws MPIException
	{
		byte[] buf = new byte[MPI.COMM_WORLD.packSize(mp.size, MPI.DOUBLE)];
		MPI.COMM_WORLD.pack(MPI.slice((double[]) storage, mp.idx), 1, mp.type, buf, 0);
		return buf;
	}

	public void unpack(MPIParam mp, Serializable buf) throws MPIException
	{
		MPI.COMM_WORLD.unpack((byte[]) buf, 0, MPI.slice((double[]) storage, mp.idx), 1, mp.type);
	}

	public String toString()
	{
		int width = shape.getWidth();
		int height = shape.getHeight();
		double[] array = (double[]) storage;
		StringBuffer buf = new StringBuffer(String.format("DoubleGridStorage-%s\n", shape));

		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
				buf.append(String.format(" %4.2f ", array[i * height + j]));
			buf.append("\n");
		}

		return buf.toString();
	}

	public double get(Int2D p)
	{
		return storage[getFlatIndex((Int2D) p)];
	}

	public void set(Int2D p, double t)
	{
		storage[getFlatIndex((Int2D) p)] = t;
	}

	public double get(int x, int y)
	{
		return storage[getFlatIndex(x, y)];
	}

	public void set(int x, int y, double t)
	{
		storage[getFlatIndex(x, y)] = t;
	}


	public void addObject(Number2D p, Double t)
	{
		Int2D localP = toLocalPoint((Int2D) p);
		set(localP, t);
	}

	public Double getObject(Number2D p, long id)
	{
		Int2D localP = toLocalPoint((Int2D) p);

		return storage[getFlatIndex(localP)];
	}

	// Don't call this method, it'd be foolish
	public ArrayList<Double> getAllObjects(Number2D p)
	{
		Int2D localP = toLocalPoint((Int2D) p);

		ArrayList<Double> list = new ArrayList<Double>();
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
		storage = new double[shape.getArea()];
	}
}
