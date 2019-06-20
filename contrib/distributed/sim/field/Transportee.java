package sim.field;

import java.io.Serializable;

import sim.util.DoublePoint;

public class Transportee<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	public T wrappedObject;

	public int destination; // this is pId
	public int ordering;
	public boolean migrate;
	public DoublePoint loc;

	public Transportee(final int dst, final T wrappedObject, final DoublePoint loc, final boolean migrate,
			final int ordering) {
		this.destination = dst;
		this.wrappedObject = wrappedObject;
		this.migrate = migrate;
		this.loc = loc;
		this.ordering = ordering;
	}

	public Transportee(final int dst, final T wrappedObject, final DoublePoint loc, final boolean migrate) {
		this(dst, wrappedObject, loc, migrate, 1);
	}

	public Transportee(final int dst, final T wrappedObject, final DoublePoint loc) {
		this(dst, wrappedObject, loc, false, 1);
	}

	public String toString() {
		return "Transportee [wrappedObject=" + wrappedObject + ", destination=" + destination + ", ordering=" + ordering
				+ ", migrate=" + migrate + ", loc=" + loc + "]";
	}
}
