package sim.field.storage;

import java.io.Serializable;
import java.util.stream.IntStream;

import mpi.Datatype;
import mpi.MPI;
import mpi.MPIException;
import sim.util.IntHyperRect;
import sim.util.IntPoint;
import sim.util.MPIParam;

public abstract class GridStorage {
	Object storage;
	IntHyperRect shape;
	Datatype baseType = MPI.BYTE;

	int[] stride;

	public GridStorage(final IntHyperRect shape) {
		this.shape = shape;
		stride = getStride(shape.getSize());
	}

	public GridStorage(final Object storage, final IntHyperRect shape) {
		super();
		this.storage = storage;
		this.shape = shape;
		stride = shape.getSize();
	}

	public GridStorage(final Object storage, final IntHyperRect shape, final Datatype baseType) {
		super();
		this.storage = storage;
		this.shape = shape;
		this.baseType = baseType;
		stride = shape.getSize();
	}

	public Object getStorage() {
		return storage;
	}

	public Datatype getMPIBaseType() {
		return baseType;
	}

	public IntHyperRect getShape() {
		return shape;
	}

	// Return a new instance of the subclass (IntStorage/DoubleStorage/etc...)
	public abstract GridStorage getNewStorage(IntHyperRect shape);

	public abstract String toString();

	public abstract Serializable pack(MPIParam mp) throws MPIException;

	public abstract int unpack(MPIParam mp, Serializable buf) throws MPIException;

	// Method that allocates an array of objects of desired type
	// This method will be called after the new shape has been set
	protected abstract Object allocate(int size);

	private void reload(final IntHyperRect newShape) {
		shape = newShape;
		stride = getStride(newShape.getSize());
		storage = allocate(newShape.getArea());
	}

	public void reshape(final IntHyperRect newShape) {
		if (newShape.equals(shape))
			return;

		if (newShape.isIntersect(shape)) {
			final IntHyperRect overlap = newShape.getIntersection(shape);
			final MPIParam fromParam = new MPIParam(overlap, shape, baseType);
			final MPIParam toParam = new MPIParam(overlap, newShape, baseType);

			try {
				final Serializable buf = pack(fromParam);
				reload(newShape);
				unpack(toParam, buf);

				fromParam.type.free();
				toParam.type.free();
			} catch (final MPIException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		} else
			reload(newShape);
	}

	public int getFlatIdx(final IntPoint p) {
		return IntStream.range(0, p.nd).map(i -> p.c[i] * stride[i]).sum();
	}

	// Get the flatted index with respect to the given size
	public static int getFlatIdx(final IntPoint p, final int[] wrtSize) {
		final int[] s = getStride(wrtSize);
		return IntStream.range(0, p.nd).map(i -> p.c[i] * s[i]).sum();
	}

	protected static int[] getStride(final int[] size) {
		final int[] ret = new int[size.length];

		ret[size.length - 1] = 1;
		for (int i = size.length - 2; i >= 0; i--)
			ret[i] = ret[i + 1] * size[i + 1];

		return ret;
	}
}
