package sim.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class NdPoint implements Comparable<NdPoint>, Serializable {

	private static final long serialVersionUID = 1L;

	public final int nd;

	public int getNd() {
		return nd;
	}

	public NdPoint(final int nd) {
		super();
		this.nd = nd;
	}

	public abstract Object getArray();

	public abstract double[] getArrayInDouble();

	public abstract String toString();

	public abstract NdPoint shift(int offset);

	public abstract NdPoint shift(int dim, int offset);

	public abstract NdPoint shift(int[] offsets);

	public abstract NdPoint rshift(int[] offsets);

	public abstract NdPoint shift(double offset);

	public abstract NdPoint shift(int dim, double offset);

	public abstract NdPoint shift(double[] offsets);

	public abstract NdPoint rshift(double[] offsets);

	public abstract NdPoint reduceDim(int dim);

	public abstract boolean geq(NdPoint that);

	public abstract boolean gt(NdPoint that);

	public abstract boolean leq(NdPoint that);

	public abstract boolean lt(NdPoint that);

	public abstract NdPoint max(NdPoint that);

	public abstract NdPoint min(NdPoint that);

	public abstract boolean equals(NdPoint that);

	// TODO Use NdRectangle
	public abstract NdPoint toToroidal(IntHyperRect bound);

	// TODO better design?
	public abstract int[] getOffsetsInt(NdPoint that);

	public abstract double[] getOffsetsDouble(NdPoint that);

	public double getDistance(final NdPoint that, final int l) {
		final double[] a = that.getArrayInDouble();
		final double[] c = this.getArrayInDouble();
		return Math.pow(IntStream.range(0, nd).mapToDouble(i -> Math.pow(Math.abs(a[i] - c[i]), l)).sum(), 1.0 / l);
	}

	protected static boolean equals(final double a, final double b) {
		return Math.abs(a - b) < Math.ulp(1.0);
	}

	// Utility functions for sanity checks
	protected void assertEqualDim(final int d) {
		if (d < 0 || d >= nd)
			throw new IllegalArgumentException(String.format("Illegal dimension %d given to %s", d, this.toString()));
	}

	protected void assertEqualDim(final int[] a) {
		if (nd != a.length)
			throw new IllegalArgumentException(String.format("%s and %s got different dimensions: %d, %d",
					this.toString(), Arrays.toString(a), nd, a.length));
	}

	protected void assertEqualDim(final double[] a) {
		if (nd != a.length)
			throw new IllegalArgumentException(String.format("%s and %s got different dimensions: %d, %d",
					this.toString(), Arrays.toString(a), nd, a.length));
	}

	protected void assertEqualDim(final NdPoint p) {
		if (nd != p.getNd())
			throw new IllegalArgumentException(String.format("%s and %s got different dimensions: %d, %d",
					this.toString(), p.toString(), nd, p.getNd()));
	}

	protected void assertEqualDim(final NdRectangle r) {
		if (nd != r.getNd())
			throw new IllegalArgumentException(String.format("%s and %s got different dimensions: %d, %d",
					this.toString(), r.toString(), nd, r.getNd()));
	}
}
