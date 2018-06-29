package sim.util;

import java.util.Arrays;
import java.util.stream.IntStream;

public class IntPoint extends NdPoint {
	// TODO make these private
	public int[] c;

	public IntPoint(int c[]) {
		this.nd = c.length;
		this.c = Arrays.copyOf(c, nd);
	}

	public IntPoint(int x, int y) {
		this.nd = 2;
		this.c = new int[] {x, y};
	}

	public IntPoint(int x, int y, int z) {
		this.nd = 3;
		this.c = new int[] {x, y, z};
	}

	// Make a copy of array so that IntPoint can remain immutable
	public int[] getArray() {
		return Arrays.copyOf(c, nd);
	}

	public double[] getArrayInDouble() {
		return Arrays.stream(c).mapToDouble(x -> x).toArray();
	}

	// TODO Move into NdRectangle
	public int getRectArea(IntPoint that) {
		assertEqualDim(that);
		return nd == 0 ? 0 : Math.abs(Arrays.stream(getOffset(that)).reduce(1, (x, y) -> x * y));
	}

	public IntPoint shift(int dim, int offset) {
		assertEqualDim(dim);

		int[] a = getArray();
		a[dim] += offset;

		return new IntPoint(a);
	}

	public IntPoint shift(int offset) {
		return new IntPoint(IntStream.range(0, nd).map(i -> c[i] + offset).toArray());
	}

	public IntPoint shift(int[] offsets) {
		assertEqualDim(offsets);
		return new IntPoint(IntStream.range(0, nd).map(i -> c[i] + offsets[i]).toArray());
	}

	public IntPoint rshift(int[] offsets) {
		assertEqualDim(offsets);
		return new IntPoint(IntStream.range(0, nd).map(i -> c[i] - offsets[i]).toArray());
	}

	// TODO make these return DoublePoint
	public NdPoint shift(double offset) {
		throw new IllegalArgumentException("IntPoint cannot be shifted with double offsets");
	}

	public NdPoint shift(int dim, double offset) {
		throw new IllegalArgumentException("IntPoint cannot be shifted with double offsets");
	}

	public NdPoint shift(double[] offsets) {
		throw new IllegalArgumentException("IntPoint cannot be shifted with double offsets");
	}

	public NdPoint rshift(double[] offsets) {
		throw new IllegalArgumentException("IntPoint cannot be shifted with double offsets");
	}

	// TODO remove
	public int[] getOffset(IntPoint that) {
		return getOffsetsInt(that);
	}

	// Get the distances in each dimension between self and the given point
	public int[] getOffsetsInt(NdPoint that) {
		assertEqualDim(that);

		if (!(that instanceof IntPoint))
			throw new IllegalArgumentException("Cannot get int offsets between IntPoint and "
			                                   + that.getClass().getSimpleName());

		int[] array = (int[]) that.getArray();
		return IntStream.range(0, nd).map(i -> this.c[i] - array[i]).toArray();
	}

	public double[] getOffsetsDouble(NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			int[] array = (int[])that.getArray();
			return IntStream.range(0, nd).mapToDouble(i -> this.c[i] - array[i]).toArray();
		} else if (that instanceof DoublePoint) {
			double[] array = (double[])that.getArray();
			return IntStream.range(0, nd).mapToDouble(i -> this.c[i] - array[i]).toArray();
		} else
			throw new IllegalArgumentException("Cannot get double offsets between IntPoint and "
			                                   + that.getClass().getSimpleName());
	}

	// Reduce dimension by removing the value at the dimth dimension
	public IntPoint reduceDim(int dim) {
		assertEqualDim(dim);

		int[] newc = Arrays.copyOf(c, nd - 1);
		for (int i = dim; i < nd - 1; i++)
			newc[i] = c[i + 1];

		return new IntPoint(newc);
	}

	public boolean equals(NdPoint that) {
		assertEqualDim(that);

		Object a = that.getArray();
		if (a instanceof int[])
			return Arrays.equals(c, (int[])a);
		else if (a instanceof double[]) {
			double[] d = (double[])a;
			return IntStream.range(0, nd).allMatch(i -> equals(c[i], d[i]));
		} else
			throw new IllegalArgumentException("Unknown type " + that.getClass().getSimpleName());
	}

	// TODO use NdRectangle
	public IntPoint toToroidal(IntHyperRect bound) {
		int[] size = bound.getSize(), offsets = new int[nd];

		for (int i = 0; i < nd; i++)
			if (c[i] >= bound.br.c[i])
				offsets[i] = -size[i];
			else if (c[i] < bound.ul.c[i])
				offsets[i] = size[i];

		return shift(offsets);
	}

	// // Increase the dimension by inserting the val into the dimth dimension
	// public IntPoint increaseDim(int dim, int val) {
	// 	if (dim < 0 || dim > nd)
	// 		throw new IllegalArgumentException("Illegal dimension: " + dim);

	// 	int[] newc = Arrays.copyOf(c, nd + 1);
	// 	for(int i = dim; i < nd; i++)
	// 		newc[i + 1] = c[i];
	// 	newc[dim] = val;

	// 	return new IntPoint(newc);
	// }

	// Sort the points by their components
	@Override
	public int compareTo(NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			int[] b = (int[])that.getArray();
			for (int i = 0; i < nd; i++) {
				if (this.c[i] == b[i])
					continue;
				return this.c[i] - b[i];
			}
		} else if (that instanceof DoublePoint) {
			double[] b = (double[])that.getArray();
			for (int i = 0; i < nd; i++) {
				if (equals(this.c[i], b[i]))
					continue;
				return this.c[i] - b[i] > 0 ? 1 : -1;
			}
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
			                                   + that.getClass().getSimpleName());

		return 0;
	}

	public boolean geq(NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			int[] b = (int[])that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] >= b[i]);
		} else if (that instanceof DoublePoint) {
			double[] b = (double[])that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] > b[i] || equals(c[i], b[i]));
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
			                                   + that.getClass().getSimpleName());
	}

	public boolean gt(NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			int[] b = (int[])that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] > b[i]);
		} else if (that instanceof DoublePoint) {
			double[] b = (double[])that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] > b[i] && !equals(c[i], b[i]));
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
			                                   + that.getClass().getSimpleName());
	}

	public boolean leq(NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			int[] b = (int[])that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] <= b[i]);
		} else if (that instanceof DoublePoint) {
			double[] b = (double[])that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] < b[i] || equals(c[i], b[i]));
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
			                                   + that.getClass().getSimpleName());
	}

	public boolean lt(NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			int[] b = (int[])that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] < b[i]);
		} else if (that instanceof DoublePoint) {
			double[] b = (double[])that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] < b[i] && !equals(c[i], b[i]));
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
			                                   + that.getClass().getSimpleName());
	}

	public NdPoint max(NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			int[] b = (int[])that.getArray();
			return new IntPoint(IntStream.range(0, nd).map(i -> Math.max(c[i], b[i])).toArray());
		} else if (that instanceof DoublePoint) {
			double[] b = (double[])that.getArray();
			return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> Math.max(c[i], b[i])).toArray());
		} else
			throw new IllegalArgumentException("Cannot get max of IntPoint and "
			                                   + that.getClass().getSimpleName());
	}

	public NdPoint min(NdPoint that) {
		if (that instanceof IntPoint) {
			int[] b = (int[])that.getArray();
			return new IntPoint(IntStream.range(0, nd).map(i -> Math.min(c[i], b[i])).toArray());
		} else if (that instanceof DoublePoint) {
			double[] b = (double[])that.getArray();
			return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> Math.min(c[i], b[i])).toArray());
		} else
			throw new IllegalArgumentException("Cannot get min of IntPoint and "
			                                   + that.getClass().getSimpleName());
	}


	public String toString() {
		return Arrays.toString(c);
	}

	public static void main(String[] args) {
		IntPoint pa = new IntPoint(new int[] {1, 1});
		IntPoint pb = new IntPoint(new int[] {4, 4});
		IntHyperRect r = new IntHyperRect(0, pa, pb);

		IntPoint p1 = new IntPoint(new int[] {4, 4});
		IntPoint p2 = new IntPoint(new int[] {4, 5});
		IntPoint p3 = new IntPoint(new int[] {5, 4});
		IntPoint p4 = new IntPoint(new int[] {1, 1});
		IntPoint p5 = new IntPoint(new int[] {2, 3});
		IntPoint p6 = new IntPoint(new int[] {1, 0});
		IntPoint p7 = new IntPoint(new int[] { -1, 0});

		for (IntPoint p : new IntPoint[] {p1, p2, p3, p4, p5, p6, p7})
			System.out.println("toToroidal " + p.toToroidal(r));
	}
}