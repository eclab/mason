package sim.field.storage;

import java.util.Arrays;
import java.util.function.*;
import java.io.*;

import mpi.*;
import static mpi.MPI.slice;

import sim.util.IntHyperRect;
import sim.util.IntPoint;
import sim.util.MPIParam;

public class ObjectGridStorage<T> extends GridStorage {

	IntFunction<T[]> alloc; // Lambda function which accepts the size as its argument and returns a T array

	public ObjectGridStorage(IntHyperRect shape, IntFunction<T[]> allocator) {
		super(shape);
		
		alloc = allocator;
		storage = allocate(shape.getArea());
	}

	public GridStorage getNewStorage(IntHyperRect shape) {
		return new ObjectGridStorage(shape, alloc);
	}

	protected Object allocate(int size) {
		return alloc.apply(size);
	}

	public String toString() {
		int[] size = shape.getSize();
		T[] array = (T[])storage;
		StringBuffer buf = new StringBuffer(String.format("ObjectGridStorage<%s>-%s\n", array.getClass().getSimpleName(), shape));

		if (shape.getNd() == 2)
			for (int i = 0; i < size[0]; i++) {
				for (int j = 0; j < size[1]; j++)
					buf.append(String.format(" %8s ", array[i * size[1] + j]));
				buf.append("\n");
			}

		return buf.toString();
	}

	public Serializable pack(MPIParam mp) {
		T[] objs = alloc.apply(mp.size), stor = (T[])storage;
		int curr = 0;

		for (IntHyperRect rect : mp.rects)
			for (IntPoint p : rect)
				objs[curr++] = stor[getFlatIdx(p)];

		return objs;
	}

	public int unpack(MPIParam mp, Serializable buf) {
		T[] stor = (T[])storage, objs = (T[])buf;
		int curr = 0;

		for (IntHyperRect rect : mp.rects)
			for (IntPoint p : rect)
				stor[getFlatIdx(p)] = (T)objs[curr++];

		return curr;
	}

	public static void main(String[] args) throws MPIException {
		MPI.Init(args);

		IntPoint p1 = new IntPoint(new int[] {0, 0});
		IntPoint p2 = new IntPoint(new int[] {5, 5});
		IntPoint p3 = new IntPoint(new int[] {1, 1});
		IntPoint p4 = new IntPoint(new int[] {4, 4});
		IntHyperRect r1 = new IntHyperRect(0, p1, p2);
		IntHyperRect r2 = new IntHyperRect(1, p3, p4);
		ObjectGridStorage<TestObj> s1 = new ObjectGridStorage<TestObj>(r1, size -> new TestObj[size]);
		ObjectGridStorage<TestObj> s2 = new ObjectGridStorage<TestObj>(r1, size -> new TestObj[size]);

		TestObj[] stor = (TestObj[])s1.getStorage();
		for (int i : new int[] {6, 12, 18})
			stor[i] = new TestObj(i);

		MPIParam mp = new MPIParam(r2, r1, s1.getMPIBaseType());
		s2.unpack(mp, s1.pack(mp));

		TestObj[] objs = (TestObj[])s2.getStorage();
		for (TestObj obj : objs)
			System.out.print(obj + " ");
		System.out.println("");

		MPI.Finalize();
	}
}