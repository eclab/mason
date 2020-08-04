package sim.field;

import sim.field.partitioning.PartitionInterface;

/**
 * A abstract distributed grid 2d. It wraps all methods of distributed grid.
 * 
 * @author Carmine Spagnuolo
 */

public abstract class DAbstractGrid2D {

	protected int[] fieldSize;

	public DAbstractGrid2D(final PartitionInterface ps) {
		fieldSize = ps.getFieldSize();
	}

	/* UTILS METHODS */

	/**
	 * Wraps around a toroidal field
	 * 
	 * @param x
	 * @param dim
	 * @return wrapped around value for value x in this field for dimention dim.
	 */
	public int toToroidal(final int x, final int dim) {
		final int s = fieldSize[dim];
		if (x >= s)
			return x - s;
		else if (x < 0)
			return x + s;
		return x;
	}

	/**
	 * Wraps around a toroidal field
	 * 
	 * @param x
	 * @param dim
	 * @return wrapped around value for value x in this field for dimention dim.
	 */
	public double toToroidal(final double x, final int dim) {
		final int s = fieldSize[dim];
		if (x >= s)
			return x - s;
		else if (x < 0)
			return x + s;
		return x;
	}

	/**
	 * Difference in a toroidal field
	 * 
	 * @param x1
	 * @param x2
	 * @param dim
	 * @return difference between two value x1 and x2 in this field for dimention
	 *         dim.
	 */
	public double toToroidalDiff(final double x1, final double x2, final int dim) {
		final int s = fieldSize[dim];
		if (Math.abs(x1 - x2) <= s / 2)
			return x1 - x2; // no wraparounds -- quick and dirty check

		final double dx = toToroidal(x1, dim) - toToroidal(x2, dim);
		if (dx * 2 > s)
			return dx - s;
		if (dx * 2 < -s)
			return dx + s;
		return dx;
	}

	public int stx(final int x) {
		return toToroidal(x, 0);
	}

	public int sty(final int y) {
		return toToroidal(y, 1);
	}

	public double stx(final double x) {
		return toToroidal(x, 0);
	}

	public double sty(final double y) {
		return toToroidal(y, 1);
	}

	public double tdx(final double x1, final double x2) {
		return toToroidalDiff(x1, x2, 0);
	}

	public double tdy(final double y1, final double y2) {
		return toToroidalDiff(y1, y2, 1);
	}

	public int getWidth() {
		return fieldSize[0];
	}

	public int getHeight() {
		return fieldSize[1];
	}

}
