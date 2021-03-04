package sim.field.storage;

import java.io.Serializable;
import java.util.*;
import mpi.*;
import sim.util.*;

public class DoubleGridStorage extends GridStorage<Double> {
	private static final long serialVersionUID = 1L;

	public double[] storage;

	public DoubleGridStorage(IntRect2D shape) {
		super(shape);
		baseType = MPI.DOUBLE;
		clear();
	}

	public byte[] pack(MPIParam mp) throws MPIException {
		byte[] buf = new byte[MPI.COMM_WORLD.packSize(mp.size, baseType)];
		MPI.COMM_WORLD.pack(MPI.slice((double[]) storage, mp.idx), 1, mp.type, buf, 0);
		return buf;
	}

	public int unpack(MPIParam mp, Serializable buf) throws MPIException {
		return MPI.COMM_WORLD.unpack((byte[]) buf, 0, MPI.slice((double[]) storage, mp.idx), 1, mp.type);
	}

	public String toString() {
		int[] size = shape.getSizes();
		double[] array = (double[]) storage;
		StringBuffer buf = new StringBuffer(String.format("DoubleGridStorage-%s\n", shape));

		for (int i = 0; i < size[0]; i++) {
			for (int j = 0; j < size[1]; j++)
				buf.append(String.format(" %4.2f ", array[i * size[1] + j]));
			buf.append("\n");
		}

		return buf.toString();
	}

	public void set(Int2D p, double t) {
		storage[getFlatIdx((Int2D) p)] = t;
	}

	public void addObject(Int2D p, Double t) {
		set(p, t);
	}

	public Double getObject(Int2D p, long id) {
		return storage[getFlatIdx((Int2D) p)];
	}

	// Don't call this method, it'd be foolish
	public ArrayList<Double> getAllObjects(Int2D p) {
		ArrayList<Double> list = new ArrayList<Double>();
		list.add(storage[getFlatIdx(p)]);
		return list;
	}

	public boolean removeObject(Int2D p, long id) {
		set(p, 0);
		return true;
	}

	public void clear(Int2D p) {
		set(p, 0);
	}

	public void clear() {
		storage = new double[shape.getArea()];
	}
}
