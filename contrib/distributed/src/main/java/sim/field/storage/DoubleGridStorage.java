package sim.field.storage;

import java.io.Serializable;
import java.util.Arrays;

import mpi.*;

import sim.field.partitioning.IntHyperRect;
import sim.util.*;

public class DoubleGridStorage extends GridStorage<Double> {

	final double initVal;

	public DoubleGridStorage(IntHyperRect shape, double initVal) {
		super(shape);
		baseType = MPI.DOUBLE;
		storage = allocate(shape.getArea());
		this.initVal = initVal;
		Arrays.fill((double[]) storage, initVal);
	}

	public GridStorage getNewStorage(IntHyperRect shape) {
		return new DoubleGridStorage(shape, 0);
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
		int[] size = shape.getSize();
		double[] array = (double[]) storage;
		StringBuffer buf = new StringBuffer(String.format("DoubleGridStorage-%s\n", shape));

		if (shape.getNd() == 2)
			for (int i = 0; i < size[0]; i++) {
				for (int j = 0; j < size[1]; j++)
					buf.append(String.format(" %4.2f ", array[i * size[1] + j]));
				buf.append("\n");
			}

		return buf.toString();
	}

	protected Object allocate(int size) {
		return new double[size];
	}

	public double[] getStorageArray() {
		return (double[]) getStorage();
	}

	public void addToLocation(Double t, NumberND p) {
		getStorageArray()[getFlatIdx((Int2D) p)] = t;
	}

	public void addToLocation(double t, NumberND p) {
		getStorageArray()[getFlatIdx((Int2D) p)] = t;
	}

	public void removeObject(Double t, NumberND p) {
		addToLocation(initVal, p);
	}

	public void removeObject(double t, NumberND p) {
		addToLocation(initVal, p);
	}

	public void removeObjects(NumberND p) {
		addToLocation(initVal, p);
	}

	public Serializable getObjects(NumberND p) {
		return getStorageArray()[getFlatIdx((Int2D) p)];
	}

}
