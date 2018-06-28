package sim.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class NdPoint implements Comparable<NdPoint>, Serializable {
	// TODO make this private
	public int nd;

	public int getNd() {
		return nd;
	}

	public abstract Object getArray();
	public abstract double[] getArrayInDouble();
	public abstract String toString();

	public abstract NdPoint shift(int dim, int offset);
	public abstract NdPoint shift(int[] offsets);
	public abstract NdPoint rshift(int[] offsets);

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
	// TODO Complete this in subclass
	//public abstract NdPoint toToroidal(NdRectangle bound);

	// TODO better design?
	public abstract int[] getOffsetsInt(NdPoint that);
	public abstract double[] getOffsetsDouble(NdPoint that);

	public double getDistance(NdPoint that, int l) {
		double[] a = that.getArrayInDouble();
		double[] c = this.getArrayInDouble();
		return Math.pow(IntStream.range(0, nd).mapToDouble(i -> Math.pow(Math.abs(a[i] - c[i]), l)).sum(), 1.0 / l);
	}

	protected static boolean equals(double a, double b) {
		return Math.abs(a - b) < Math.ulp(1.0);
	}

	// Utility functions for sanity checks
	protected void assertEqualDim(int d) {
		if (d < 0 || d >= this.nd)
			throw new IllegalArgumentException(String.format("Illegal dimension %d given to %s", d, this.toString()));
	}

	protected void assertEqualDim(int[] a) {
		if (this.nd != a.length)
			throw new IllegalArgumentException(String.format("%s and %s got different dimensions: %d, %d", this.toString(), Arrays.toString(a), this.nd, a.length));
	}

	protected void assertEqualDim(double[] a) {
		if (this.nd != a.length)
			throw new IllegalArgumentException(String.format("%s and %s got different dimensions: %d, %d", this.toString(), Arrays.toString(a), this.nd, a.length));
	}

	protected void assertEqualDim(NdPoint p) {
		if (this.nd != p.getNd())
			throw new IllegalArgumentException(String.format("%s and %s got different dimensions: %d, %d", this.toString(), p.toString(), this.nd, p.getNd()));
	}

	protected void assertEqualDim(NdRectangle r) {
		if (this.nd != r.getNd())
			throw new IllegalArgumentException(String.format("%s and %s got different dimensions: %d, %d", this.toString(), r.toString(), this.nd, r.getNd()));
	}
}