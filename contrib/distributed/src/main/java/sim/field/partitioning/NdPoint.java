package sim.field.partitioning;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Parent class for n-dimensional points
 */
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

	/**
	 * Reduce dimension by removing the value at the dimth dimension
	 * 
	 * @param dim
	 */
	public abstract NdPoint reduceDim(int dim);

	/**
	 * @param that
	 * @return true if that is greater than or equal to this
	 */
	public abstract boolean geq(NdPoint that);

	/**
	 * @param that
	 * @return true if that is greater than this
	 */
	public abstract boolean gt(NdPoint that);

	/**
	 * @param that
	 * @return true if that is less than or equal to this
	 */
	public abstract boolean leq(NdPoint that);

	/**
	 * @param that
	 * @return true if that is less than this
	 */
	public abstract boolean lt(NdPoint that);

	/**
	 * @param that
	 * @return max of that and this
	 */
	public abstract NdPoint max(NdPoint that);

	/**
	 * @param that
	 * @return min of that and this
	 */
	public abstract NdPoint min(NdPoint that);

	public abstract boolean equals(NdPoint that);

	// TODO Use NdRectangle
	public abstract NdPoint toToroidal(IntHyperRect bound);

	// TODO better design?
	/**
	 * @param that
	 * @return the distances in each dimension between self and the given point
	 */
	public abstract int[] getOffsetsInt(NdPoint that);

	/**
	 * @param that
	 * @return the distances in each dimension between self and the given point
	 */
	public abstract double[] getOffsetsDouble(NdPoint that);

	/**
	 * @param that
	 * @param l
	 * @return the distance between this and that (according to the given l, as in
	 *         L1, L2 ...)
	 */
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

}
