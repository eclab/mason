package sim.field.storage;

import java.io.Serializable;
import java.util.Arrays;

import mpi.*;
import sim.util.*;

public class IntGridStorage extends GridStorage<Integer, Int2D> {
	private static final long serialVersionUID = 1L;


	public int[] storage;
//	final int initVal;

	public IntGridStorage(IntRect2D shape) {
		super(shape);
		baseType = MPI.INT;
		clear();
//		storage = allocate(shape.getArea());
//		this.initVal = initVal;
//		Arrays.fill((int[]) storage, initVal);
	}

	/*
	 * public GridStorage getNewStorage(IntRect2D shape) { return new
	 * IntGridStorage(shape, 0); }
	 */

	public byte[] pack(MPIParam mp) throws MPIException {
		byte[] buf = new byte[MPI.COMM_WORLD.packSize(mp.size, baseType)];
		MPI.COMM_WORLD.pack(MPI.slice((int[]) storage, mp.idx), 1, mp.type, buf, 0);
		return buf;
	}

	public int unpack(MPIParam mp, Serializable buf) throws MPIException {
		return MPI.COMM_WORLD.unpack((byte[]) buf, 0, MPI.slice((int[]) storage, mp.idx), 1, mp.type);
	}

	public String toString() {
		int[] size = shape.getSizes();
		int[] array = (int[]) storage;
		StringBuffer buf = new StringBuffer(String.format("IntGridStorage-%s\n", shape));

		for (int i = 0; i < size[0]; i++) {
			for (int j = 0; j < size[1]; j++)
				buf.append(String.format(" %4d ", array[i * size[1] + j]));
			buf.append("\n");
		}

		return buf.toString();
	}

	public void clear() {
		storage = new int[shape.getArea()];
	}

	public void addToLocation(Integer t, Int2D p) {
		storage[getFlatIdx((Int2D) p)] = t;
	}

	public void addToLocation(int t, Int2D p) {
		storage[getFlatIdx((Int2D) p)] = t;
	}

	public void removeObject(Int2D p, Integer t) {
		addToLocation(0, p);
	}

	public void removeObject(int t, Int2D p) {
		addToLocation(0, p);
	}

	public void removeObjects(Int2D p) {
		addToLocation(0, p);
	}

	public Serializable getObjects(Int2D p) {
		return storage[getFlatIdx(p)];
	}

}
