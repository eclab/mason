package sim.field;

import java.io.Serializable;

import sim.util.DoublePoint;

public class Transportee<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	public T wrappedObject;

	public int destination; // this is pId
	public int ordering;
	public boolean migrate;
	// this is null if the object is not associated with a field
	public DoublePoint loc;
	public int fieldIndex;

	public Transportee(final int dst, final T wrappedObject, final DoublePoint loc, final boolean migrate,
			final int ordering, final int fieldIndex) {
		this.destination = dst;
		this.wrappedObject = wrappedObject;
		this.migrate = migrate;
		this.loc = loc;
		this.ordering = ordering;
		this.fieldIndex = fieldIndex;
	}

	public Transportee(final int dst, final T wrappedObject, final DoublePoint loc, final boolean migrate,
			final int fieldIndex) {
		this(dst, wrappedObject, loc, migrate, 1, fieldIndex);
	}

	public Transportee(final int dst, final T wrappedObject, final DoublePoint loc, final int fieldIndex) {
		this(dst, wrappedObject, loc, false, 1, fieldIndex);
	}

	public Transportee(final int dst, final T wrappedObject, final int ordering) {
		this(dst, wrappedObject, null, false, ordering, -1);
	}

	public Transportee(final int dst, final T wrappedObject) {
		this(dst, wrappedObject, null, false, 1, -1);
	}

}
