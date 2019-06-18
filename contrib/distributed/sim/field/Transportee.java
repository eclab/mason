package sim.field;

import java.io.Serializable;

import sim.util.DoublePoint;

public class Transportee implements Serializable {
	private static final long serialVersionUID = 1L;
	public int destination;
	public boolean migrate;
	public Object wrappedObject;

	public DoublePoint loc;
	public int identityHashcode;

	public Transportee(final int dst, final Object wrappedObject, final DoublePoint loc, final boolean migrate) {
		this.destination = dst;
		this.wrappedObject = wrappedObject;
		this.migrate = migrate;
		this.loc = loc;
	}

	public Transportee(final int dst, final Object wrappedObject, final DoublePoint loc) {
		this(dst, wrappedObject, loc, false);
	}
}
