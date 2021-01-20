package sim.field.storage;

import java.io.Serializable;
import mpi.*;
import sim.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public abstract class GridStorage<T extends Serializable, P extends NumberND> implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	IntRect2D shape;
	transient Datatype baseType = MPI.BYTE; // something by default
	int height; // this is the same as shape.getHeight(), to save a bit of computation

	public abstract String toString();

	public abstract Serializable pack(MPIParam mp) throws MPIException;

	public abstract int unpack(MPIParam mp, Serializable buf) throws MPIException;

	/* Abstract Method of generic storage based on N-dimensional Point */
	public abstract void addToLocation(final T obj, final P p);

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

	public Datatype getMPIBaseType() {
		return baseType;
	}

	public IntRect2D getShape() {
		return shape;
	}

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
		height = newShape.getHeight();
		clear();
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
		return p.x * wrtSize[1] + p.y; // [1] is height
	}
}
