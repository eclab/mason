package sim.util;

import java.io.Serializable;

public class Pair<A, B> implements Serializable {
	private static final long serialVersionUID = 1L;

	public final A a;
	public final B b;

	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}
}
