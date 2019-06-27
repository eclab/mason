package sim.util;

import java.util.Arrays;
import java.util.stream.IntStream;

public class IntPoint extends NdPoint {
	private static final long serialVersionUID = 1L;

	public final int[] c;

	public IntPoint(final int c[]) {
		super(c.length);
		this.c = Arrays.copyOf(c, nd);
	}

	public IntPoint(final int x, final int y) {
		super(2);
		c = new int[] { x, y };
	}

	public IntPoint(final int x, final int y, final int z) {
		super(3);
		c = new int[] { x, y, z };
	}

	// Make a copy of array so that IntPoint can remain immutable
	public int[] getArray() {
		return Arrays.copyOf(c, nd);
	}

	public double[] getArrayInDouble() {
		return Arrays.stream(c).mapToDouble(x -> x).toArray();
	}

	// TODO Move into NdRectangle
	public int getRectArea(final IntPoint that) {
		assertEqualDim(that);
		return nd == 0 ? 0 : Math.abs(Arrays.stream(getOffset(that)).reduce(1, (x, y) -> x * y));
	}

	public IntPoint shift(final int dim, final int offset) {
		assertEqualDim(dim);

		final int[] a = getArray();
		a[dim] += offset;

		return new IntPoint(a);
	}

	public IntPoint shift(final int offset) {
		return new IntPoint(IntStream.range(0, nd).map(i -> c[i] + offset).toArray());
	}

	public IntPoint shift(final int[] offsets) {
		assertEqualDim(offsets);
		return new IntPoint(IntStream.range(0, nd).map(i -> c[i] + offsets[i]).toArray());
	}

	public IntPoint rshift(final int[] offsets) {
		assertEqualDim(offsets);
		return new IntPoint(IntStream.range(0, nd).map(i -> c[i] - offsets[i]).toArray());
	}

	// TODO make these return DoublePoint
	public NdPoint shift(final double offset) {
		throw new IllegalArgumentException("IntPoint cannot be shifted with double offsets");
	}

	public NdPoint shift(final int dim, final double offset) {
		throw new IllegalArgumentException("IntPoint cannot be shifted with double offsets");
	}

	public NdPoint shift(final double[] offsets) {
		throw new IllegalArgumentException("IntPoint cannot be shifted with double offsets");
	}

	public NdPoint rshift(final double[] offsets) {
		throw new IllegalArgumentException("IntPoint cannot be shifted with double offsets");
	}

	// TODO remove
	public int[] getOffset(final IntPoint that) {
		return getOffsetsInt(that);
	}

	// Get the distances in each dimension between self and the given point
	public int[] getOffsetsInt(final NdPoint that) {
		assertEqualDim(that);

		if (!(that instanceof IntPoint))
			throw new IllegalArgumentException("Cannot get int offsets between IntPoint and "
					+ that.getClass().getSimpleName());

		final int[] array = (int[]) that.getArray();
		return IntStream.range(0, nd).map(i -> c[i] - array[i]).toArray();
	}

	public double[] getOffsetsDouble(final NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			final int[] array = (int[]) that.getArray();
			return IntStream.range(0, nd).mapToDouble(i -> c[i] - array[i]).toArray();
		} else if (that instanceof DoublePoint) {
			final double[] array = (double[]) that.getArray();
			return IntStream.range(0, nd).mapToDouble(i -> c[i] - array[i]).toArray();
		} else
			throw new IllegalArgumentException("Cannot get double offsets between IntPoint and "
					+ that.getClass().getSimpleName());
	}

	// Reduce dimension by removing the value at the dimth dimension
	public IntPoint reduceDim(final int dim) {
		assertEqualDim(dim);

		final int[] newc = Arrays.copyOf(c, nd - 1);
		for (int i = dim; i < nd - 1; i++)
			newc[i] = c[i + 1];

		return new IntPoint(newc);
	}

	public boolean equals(final NdPoint that) {
		assertEqualDim(that);

		final Object a = that.getArray();
		if (a instanceof int[])
			return Arrays.equals(c, (int[]) a);
		else if (a instanceof double[]) {
			final double[] d = (double[]) a;
			return IntStream.range(0, nd).allMatch(i -> equals(c[i], d[i]));
		} else
			throw new IllegalArgumentException("Unknown type " + that.getClass().getSimpleName());
	}

	// TODO use NdRectangle
	public IntPoint toToroidal(final IntHyperRect bound) {
		final int[] size = bound.getSize(), offsets = new int[nd];

		for (int i = 0; i < nd; i++)
			if (c[i] >= bound.br.c[i])
				offsets[i] = -size[i];
			else if (c[i] < bound.ul.c[i])
				offsets[i] = size[i];

		return shift(offsets);
	}

	// // Increase the dimension by inserting the val into the dimth dimension
	// public IntPoint increaseDim(int dim, int val) {
	// if (dim < 0 || dim > nd)
	// throw new IllegalArgumentException("Illegal dimension: " + dim);

	// int[] newc = Arrays.copyOf(c, nd + 1);
	// for(int i = dim; i < nd; i++)
	// newc[i + 1] = c[i];
	// newc[dim] = val;

	// return new IntPoint(newc);
	// }

	// Sort the points by their components
	@Override
	public int compareTo(final NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			final int[] b = (int[]) that.getArray();
			for (int i = 0; i < nd; i++) {
				if (c[i] == b[i])
					continue;
				return c[i] - b[i];
			}
		} else if (that instanceof DoublePoint) {
			final double[] b = (double[]) that.getArray();
			for (int i = 0; i < nd; i++) {
				if (equals(c[i], b[i]))
					continue;
				return c[i] - b[i] > 0 ? 1 : -1;
			}
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
					+ that.getClass().getSimpleName());

		return 0;
	}

	public boolean geq(final NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			final int[] b = (int[]) that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] >= b[i]);
		} else if (that instanceof DoublePoint) {
			final double[] b = (double[]) that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] > b[i] || equals(c[i], b[i]));
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
					+ that.getClass().getSimpleName());
	}

	public boolean gt(final NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			final int[] b = (int[]) that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] > b[i]);
		} else if (that instanceof DoublePoint) {
			final double[] b = (double[]) that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] > b[i] && !equals(c[i], b[i]));
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
					+ that.getClass().getSimpleName());
	}

	public boolean leq(final NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			final int[] b = (int[]) that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] <= b[i]);
		} else if (that instanceof DoublePoint) {
			final double[] b = (double[]) that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] < b[i] || equals(c[i], b[i]));
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
					+ that.getClass().getSimpleName());
	}

	public boolean lt(final NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			final int[] b = (int[]) that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] < b[i]);
		} else if (that instanceof DoublePoint) {
			final double[] b = (double[]) that.getArray();
			return IntStream.range(0, nd).allMatch(i -> c[i] < b[i] && !equals(c[i], b[i]));
		} else
			throw new IllegalArgumentException("Cannot compare IntPoint with "
					+ that.getClass().getSimpleName());
	}

	public NdPoint max(final NdPoint that) {
		assertEqualDim(that);

		if (that instanceof IntPoint) {
			final int[] b = (int[]) that.getArray();
			return new IntPoint(IntStream.range(0, nd).map(i -> Math.max(c[i], b[i])).toArray());
		} else if (that instanceof DoublePoint) {
			final double[] b = (double[]) that.getArray();
			return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> Math.max(c[i], b[i])).toArray());
		} else
			throw new IllegalArgumentException("Cannot get max of IntPoint and "
					+ that.getClass().getSimpleName());
	}

	public NdPoint min(final NdPoint that) {
		if (that instanceof IntPoint) {
			final int[] b = (int[]) that.getArray();
			return new IntPoint(IntStream.range(0, nd).map(i -> Math.min(c[i], b[i])).toArray());
		} else if (that instanceof DoublePoint) {
			final double[] b = (double[]) that.getArray();
			return new DoublePoint(IntStream.range(0, nd).mapToDouble(i -> Math.min(c[i], b[i])).toArray());
		} else
			throw new IllegalArgumentException("Cannot get min of IntPoint and "
					+ that.getClass().getSimpleName());
	}

	public String toString() {
		return Arrays.toString(c);
	}

	public static void main(final String[] args) {
		final IntPoint pa = new IntPoint(new int[] { 1, 1 });
		final IntPoint pb = new IntPoint(new int[] { 4, 4 });
		final IntHyperRect r = new IntHyperRect(0, pa, pb);

		final IntPoint p1 = new IntPoint(new int[] { 4, 4 });
		final IntPoint p2 = new IntPoint(new int[] { 4, 5 });
		final IntPoint p3 = new IntPoint(new int[] { 5, 4 });
		final IntPoint p4 = new IntPoint(new int[] { 1, 1 });
		final IntPoint p5 = new IntPoint(new int[] { 2, 3 });
		final IntPoint p6 = new IntPoint(new int[] { 1, 0 });
		final IntPoint p7 = new IntPoint(new int[] { -1, 0 });

		for (final IntPoint p : new IntPoint[] { p1, p2, p3, p4, p5, p6, p7 })
			System.out.println("toToroidal " + p.toToroidal(r));
	}
}
