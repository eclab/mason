package sim.field.storage;

import java.io.Serializable;

import mpi.Datatype;
import mpi.MPI;
import mpi.MPIException;
import sim.field.partitioning.IntRect2D;
import sim.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public abstract class GridStorage<T extends Serializable, P extends NumberND> implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// Object storage;
	IntRect2D shape;
	transient Datatype baseType = MPI.BYTE; // something by default
	int height; // this is the same as shape.getHeight(), pulled out just in case inlining
				// doesn't work

	/* Abstract Method of generic storage based on N-dimensional Point */
	public abstract void addToLocation(final T obj, final P p);

//	public abstract P getLocation(final T obj);

//	public abstract void removeObject(final T obj);

	public abstract void removeObjects(final P p);

	public abstract void removeObject(P p, final T obj);

	public void removeObject(P p, long id) {
		// TODO: what to do for int grid storage etc?
		throw new UnsupportedOperationException(
				"A getObjects method which searches for an id is not implemented for this storage");
	}

	public abstract Serializable getObjects(final P p);

	public T getObjects(final P p, long id) {
		// TODO: what to do for int grid storage etc?
		throw new UnsupportedOperationException(
				"A getObjects method which searches for an id is not implemented for this storage");
	}

	public abstract void clear();

	//// NOTE: Subclasses are responsible for allocating the storage
	//// and setting the base type
	public GridStorage(final IntRect2D shape) {
		this.shape = shape;
		height = shape.getHeight(); // getHeight(shape.getSizes());
	}

	/*
	 * public GridStorage(final Object storage, final IntRect2D shape) {
	 * this(shape); this.storage = storage; }
	 * 
	 * /* public GridStorage(final Object storage, final IntRect2D shape, final
	 * Datatype baseType) { this(storage, shape); this.baseType = baseType; }
	 */

	/*
	 * public Object getStorage() { return storage; }
	 */

	public Datatype getMPIBaseType() {
		return baseType;
	}

	public IntRect2D getShape() {
		return shape;
	}

	// Return a new instance of the subclass (IntStorage/DoubleStorage/etc...)
	// public abstract GridStorage getNewStorage(IntRect2D shape);

	public abstract String toString();

	public abstract Serializable pack(MPIParam mp) throws MPIException;
	

	public abstract int unpack(MPIParam mp, Serializable buf) throws MPIException;

	// Method that allocates an array of objects of desired type
	// This method will be called after the new shape has been set
	// protected abstract Object allocate(int size);

	/**
	 * Reset the shape, height, and storage w.r.t. newShape
	 * 
	 * @param newShape
	 */
	void reload(final IntRect2D newShape) {
		shape = newShape;
		height = newShape.getHeight(); // getHeight(newShape.getSizes());
		clear();
		// storage = allocate(newShape.getArea());
	}

	/**
	 * Reshapes HyperRect to a newShape
	 * 
	 * @param newShape
	 */
	public void reshape(final IntRect2D newShape) {
		if (newShape.equals(shape))
			return;

		if (newShape.intersects(shape)) {

			final IntRect2D overlap = newShape.getIntersection(shape);

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

	/**
	 * @param p
	 * 
	 * @return flattened index
	 */
	public int getFlatIdx(final Int2D p) {
		return p.x * height + p.y;
//		int sum = 0;
//		for (int i = 0; i < p.getNumDimensions(); i++) {
//			sum += p.c(i) * height[i];
//		}
//		return sum;
	}

	/**
	 * @param p
	 * 
	 * @return flattened index
	 */
	public int getFlatIdx(int x, int y) {
		return x * height + y;
	}

	/**
	 * @param p
	 * @param height
	 * 
	 * @return flattened index with respect to the given height
	 */
	public static int getFlatIdx(final Int2D p, final int[] wrtSize) {
		return p.x * wrtSize[1] + p.y; // [1] is height //return p.x * getHeight(wrtSize) + p.y;

//		final int s = getHeight(wrtSize);
//		int sum = 0;
//		for (int i = 0; i < p.getNumDimensions(); i++) {
//			sum += p.c(i) * s[i];
//		}
//		return sum;
	}

	//public abstract void check_m_and_storage_match(String s); //test method
	//public abstract void same_agent_multiple_cells(String string); //test method

	/**
	 * @param size
	 * @return height
	 */
//	protected static int getHeight(final int[] size) {
//		return size[1];
//	}

//	protected static int[] getHeight(final int[] size) {
//	final int[] ret = new int[size.length];
//
//	ret[size.length - 1] = 1;
//	for (int i = size.length - 2; i >= 0; i--)
//		ret[i] = ret[i + 1] * size[i + 1];
//
//	return ret;
//}

}
