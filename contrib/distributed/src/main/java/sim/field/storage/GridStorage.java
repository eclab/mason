package sim.field.storage;

import java.io.Serializable;

import mpi.*;
import sim.util.*;
import java.util.*;

/**
 * internal local storage for distributed grids.
 *
 * @param <T> Type of objects to store
 */
public abstract class GridStorage<T extends Serializable> implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;

	IntRect2D shape;
	transient Datatype baseType = MPI.BYTE; // something by default
	int height; // this is the same as shape.getHeight(), to save a bit of computation

	Int2D offset; // moved here

	//// NOTE: Subclasses are responsible for allocating the storage
	//// and setting the base type
	public GridStorage(IntRect2D shape)
	{
		this.shape = shape;
		height = shape.getHeight(); // getHeight(shape.getSizes());
	}

	public Datatype getMPIBaseType()
	{
		return baseType;
	}

	public IntRect2D getShape()
	{
		return shape;
	}

	public abstract String toString();

	public abstract Serializable pack(MPIParam mp) throws MPIException;

	public abstract int unpack(MPIParam mp, Serializable buf) throws MPIException;

	/**
	 * Adds or sets the given object at the given point. Dense and Continuous
	 * storage add the object. Int, Object, and Double grid storage set it.
	 */
	public abstract void addObject(NumberND p, final T obj);

	/**
	 * Object, Int, and Double grid storage ignore the id and return whatever is
	 * currently present.
	 * 
	 * @throws Exception
	 */
	public abstract T getObject(NumberND p, long id);

	/**
	 * Returns an ArrayList consisting of all the elements at a given location.
	 * 
	 * @throws Exception
	 */
	public abstract ArrayList<T> getAllObjects(NumberND p);

	/**
	 * Returns true if the object is at this location and was removed. Continuous
	 * storage ignores the location and simply removes the object, returning true if
	 * the object was successfully remeoved. Int and Double grid storage ignore the
	 * id and set the value to 0, always returning true. Object grid storage sets
	 * the value to null, always returning true.
	 * 
	 * @throws Exception
	 */
	public abstract boolean removeObject(NumberND p, long id);

	/**
	 * Clears all objects at the given point. Int and Double grid storage set all
	 * values to 0. Object grid storage sets all values to null.
	 * 
	 * @throws Exception
	 */
	public abstract void clear(NumberND p);

	/**
	 * Clears all objects from the storage entirely. Int and Double grid storage set
	 * all values to 0. Object grid storage sets all values to null.
	 */
	public abstract void clear();

	// Method that allocates an array of objects of desired type
	// This method will be called after the new shape has been set
	// protected abstract Object allocate(int size);

	/**
	 * Reset the shape, height, and storage w.r.t. newShape
	 * 
	 * @param newShape
	 */
	void reload(final IntRect2D newShape)
	{
		shape = newShape;
		height = newShape.getHeight();
		clear();
	}

	/**
	 * Reshapes HyperRect to a newShape
	 * 
	 * @param newShape
	 */
	public void reshape(final IntRect2D newShape)
	{
		if (newShape.equals(shape))
			return;

		if (newShape.intersects(shape))
		{
			final IntRect2D overlap = newShape.getIntersection(shape);

			final MPIParam fromParam = new MPIParam(overlap, shape, baseType);
			final MPIParam toParam = new MPIParam(overlap, newShape, baseType);

			try
			{
				final Serializable buf = pack(fromParam);
				reload(newShape);
				unpack(toParam, buf);

				fromParam.type.free();
				toParam.type.free();
			}
			catch (final MPIException e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else
			reload(newShape);
	}

	/**
	 * @param p
	 * 
	 * @return flattened index
	 */
	public int getFlatIdx(final Int2D p)
	{
		return getFlatIdx(p.x, p.y);
	}

	/**
	 * @param p
	 * 
	 * @return flattened index
	 */
	public int getFlatIdx(int x, int y)
	{
		return x * height + y;
	}

/*
	public static int getFlatIdx(final Int2D p, final int[] wrtSize) {
		return p.x * wrtSize[1] + p.y; // [1] is height
	}
*/

	public static int getFlatIdx(final Int2D p, int height)
	{
		return p.x * height + p.y;
	}

	public void setOffSet(Int2D offset)
	{
		this.offset = offset;
	}

	public Int2D toLocalPoint(final Int2D p)
	{
		return p.subtract(offset);
	}
}
