package sim.util;

import java.util.Arrays;
import java.util.stream.IntStream;

public class DoublePoint extends NdPoint {
	// TODO make these private
	public double[] c;

	public DoublePoint(double c[]) {
		this.nd = c.length;
		this.c = Arrays.copyOf(c, nd);
	}

	public DoublePoint(double x, double y) {
		this.nd = 2;
		this.c = new double[] {x, y};
	}

	public DoublePoint(double x, double y, double z) {
		this.nd = 3;
		this.c = new double[] {x, y, z};
	}

	public double[] getArray() {
		return Arrays.copyOf(c, nd);
	}

	public double[] getArrayInDouble() {
		return getArray();
	}

	// TODO Move into NdRectangle
	public double getRectArea(DoublePoint that) {
		assertEqualDim(that);
		return nd == 0 ? 0 : Math.abs(Arrays.stream(getOffsetsDouble(that)).reduce(1, (x, y) -> x * y));
	}

	public DoublePoint shift(int dim, double offset) {
		assertEqualDim(dim);

		double[] newc = Arrays.copyOf(c, nd);
		newc[dim] += offset;

		return new DoublePoint(newc);
	}

	public DoublePoint shift(double offset) {
		return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> c[i] + offset).toArray());
	}

	public DoublePoint shift(double[] offsets) {
		assertEqualDim(offsets);
		return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> c[i] + offsets[i]).toArray());
	}

	public DoublePoint rshift(double[] offsets) {
		assertEqualDim(offsets);
		return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> c[i] - offsets[i]).toArray());
	}

	public DoublePoint shift(int dim, int offset) {
		return shift(dim, (double)offset);
	}

	public DoublePoint shift(int offset) {
		return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> c[i] + offset).toArray());
	}

	public DoublePoint shift(int[] offsets) {
		return shift(Arrays.stream(offsets).mapToDouble(x -> x).toArray());
	}

	public DoublePoint rshift(int[] offsets) {
		return rshift(Arrays.stream(offsets).mapToDouble(x -> x).toArray());
	}

	// Get the distances in each dimension between self and the given point
	public int[] getOffsetsInt(NdPoint that) {
		throw new IllegalArgumentException("Cannot get int offsets between DoublePoint and "
		                                   + that.getClass().getSimpleName());
	}

	public double[] getOffsetsDouble(NdPoint that) {
		assertEqualDim(that);
		double[] a = that.getArrayInDouble();
		return IntStream.range(0, nd).mapToDouble(i -> this.c[i] - a[i]).toArray();
	}

	// Reduce dimension by removing the value at the dimth dimension
	public DoublePoint reduceDim(int dim) {
		assertEqualDim(dim);

		double[] newc = Arrays.copyOf(c, nd - 1);
		for (int i = dim; i < nd - 1; i++)
			newc[i] = c[i + 1];

		return new DoublePoint(newc);
	}

	public boolean equals(NdPoint that) {
		assertEqualDim(that);

		Object a = that.getArray();
		if (a instanceof double[])
			return Arrays.equals(c, (double[])a);
		else if (a instanceof int[]) {
			int[] d = (int[])a;
			return IntStream.range(0, nd).allMatch(i -> equals(c[i], d[i]));
		} else
			throw new IllegalArgumentException("Unknown type " + that.getClass().getSimpleName());
	}

	// TODO use NdRectangle
	public DoublePoint toToroidal(IntHyperRect bound) {
		int[] size = bound.getSize(), offsets = new int[nd];

		for (int i = 0; i < nd; i++)
			if (c[i] >= bound.br.c[i])
				offsets[i] = -size[i];
			else if (c[i] < bound.ul.c[i])
				offsets[i] = size[i];

		return shift(offsets);
	}

	// Sort the points by their components
	@Override
	public int compareTo(NdPoint that) {
		double[] a = that.getArrayInDouble();

		for (int i = 0; i < nd; i++) {
			if (equals(c[i], a[i]))
				continue;
			return c[i] - a[i] > 0 ? 1 : -1;
		}

		return 0;
	}

	public boolean geq(NdPoint that) {
		assertEqualDim(that);
		double[] a = that.getArrayInDouble();
		return IntStream.range(0, nd).allMatch(i -> c[i] > a[i] || equals(c[i], a[i]));
	}

	public boolean gt(NdPoint that) {
		assertEqualDim(that);
		double[] a = that.getArrayInDouble();
		return IntStream.range(0, nd).allMatch(i -> c[i] > a[i] && !equals(c[i], a[i]));
	}

	public boolean leq(NdPoint that) {
		assertEqualDim(that);
		double[] a = that.getArrayInDouble();
		return IntStream.range(0, nd).allMatch(i -> c[i] < a[i] || equals(c[i], a[i]));
	}

	public boolean lt(NdPoint that) {
		assertEqualDim(that);
		double[] a = that.getArrayInDouble();
		return IntStream.range(0, nd).allMatch(i -> c[i] < a[i] && !equals(c[i], a[i]));
	}

	public NdPoint max(NdPoint that) {
		assertEqualDim(that);
		double[] a = that.getArrayInDouble();
		return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> Math.max(c[i], a[i])).toArray());
	}

	public NdPoint min(NdPoint that) {
		assertEqualDim(that);
		double[] a = that.getArrayInDouble();
		return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> Math.min(c[i], a[i])).toArray());
	}

	public String toString() {
		return Arrays.toString(c);
	}

	public static void main(String[] args) {
		IntPoint pa = new IntPoint(new int[] {1, 1});
		IntPoint pb = new IntPoint(new int[] {4, 4});
		IntHyperRect r = new IntHyperRect(0, pa, pb);

		DoublePoint p1 = new DoublePoint(new double[] {4, 4});
		DoublePoint p2 = new DoublePoint(new double[] {4, 5});
		DoublePoint p3 = new DoublePoint(new double[] {5, 4});
		DoublePoint p4 = new DoublePoint(new double[] {1, 1});
		DoublePoint p5 = new DoublePoint(new double[] {2, 3});
		DoublePoint p6 = new DoublePoint(new double[] {1, 0});
		DoublePoint p7 = new DoublePoint(new double[] { -1, 0});

		for (DoublePoint p : new DoublePoint[] {p1, p2, p3, p4, p5, p6, p7})
			System.out.println("toToroidal " + p.toToroidal(r));
	}
}