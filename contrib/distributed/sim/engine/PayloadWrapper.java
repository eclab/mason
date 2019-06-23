package sim.field;

import java.io.Serializable;

import sim.util.NdPoint;

/**
 * Wrapper for transporting objects to remote processors
 *
 * @param <T> The Class of the Object to wrap
 * @param <P> The Type of NdPoint to use
 */
public class Transportee<T extends Serializable, P extends NdPoint> implements Serializable {
	private static final long serialVersionUID = 1L;
	public T wrappedObject;

	/**
	 * The is the pId of the destination
	 */
	public int destination;

	/**
	 * Ordering for the scheduler <br>
	 * Optional field, only needed if the wrappedObject is to be scheduled as well
	 * <br>
	 * <br>
	 * <b>Default:</b> 1
	 */
	public int ordering = 1;

	/**
	 * Internal flag, do not set it explicitly <br>
	 * <br>
	 * <b>Default:</b> false
	 */
	public boolean migrate = false;

	/**
	 * Location of the Object in the field <br>
	 * Optional field, only needed it the wrappedObject is to be added to a field as
	 * well <br>
	 * <br>
	 * <b>Default:</b> null
	 */
	public P loc = null;

	/**
	 * Internal field, do not set it explicitly <br>
	 * It is set by the field, if a field is used to migrate an Object <br>
	 * <br>
	 * <b>Default:</b> -1
	 */
	public int fieldIndex = -1;

	public Transportee(final int dst, final T wrappedObject, final P loc, final boolean migrate,
			final int ordering, final int fieldIndex) {
		this.destination = dst;
		this.wrappedObject = wrappedObject;
		this.loc = loc;
		this.migrate = migrate;
		this.ordering = ordering;
		this.fieldIndex = fieldIndex;
	}

	public Transportee(final int dst, final T wrappedObject, final P loc, final boolean migrate,
			final int fieldIndex) {
		this.destination = dst;
		this.wrappedObject = wrappedObject;
		this.loc = loc;
		this.migrate = migrate;
		this.fieldIndex = fieldIndex;
	}

	public Transportee(final int dst, final T wrappedObject, final P loc, final int fieldIndex) {
		this.destination = dst;
		this.wrappedObject = wrappedObject;
		this.loc = loc;
		this.fieldIndex = fieldIndex;
	}

	public Transportee(final int dst, final T wrappedObject, final int ordering) {
		this.destination = dst;
		this.wrappedObject = wrappedObject;
		this.ordering = ordering;
	}

	public Transportee(final int dst, final T wrappedObject) {
		this.destination = dst;
		this.wrappedObject = wrappedObject;
	}

}
