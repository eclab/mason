package sim.field.partitioning;

import java.util.Arrays;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import sim.util.*;

// TODO Move to NdRectangle
public class IntHyperRect implements Comparable<IntHyperRect>, Iterable<Int2D>, Serializable {
	private static final long serialVersionUID = 1L;
	public int id;
	public Int2D ul, br;

	public IntHyperRect(int id, Int2D ul, Int2D br) {
		this.id = id;

		// Refactor 20201026
		int[] d = new int[] {br.x, br.y};
		if (br.x < ul.x) {
			throw new IllegalArgumentException("All br's components " + Arrays.toString(new int[] {br.x, br.y})
					+ " should be greater than or equal to ul's " + Arrays.toString(new int[] {ul.x, ul.y}));
		}
		if (br.y < ul.y) {
			throw new IllegalArgumentException("All br's components " + Arrays.toString(new int[] {br.x, br.y})
					+ " should be greater than or equal to ul's " + Arrays.toString(new int[] {ul.x, ul.y}));
		}
//		for (int i = 0; i < 2; i++)
//			if (br.c(i) < ul.c(i))
//				throw new IllegalArgumentException("All br's components " + Arrays.toString(br.c())
//						+ " should be greater than or equal to ul's " + Arrays.toString(ul.c()));
		// >>>>>>>>>>>>>>>>>
		
		this.ul = ul;
		this.br = br;
	}

	public IntHyperRect(int[] size) {
		this(-1, new Int2D(new int[size.length]), new Int2D(size));
	}

	public int getId() {
		return id;
	}

	public void setId(int newId) {
		id = newId;
	}

	public Int2D ul() {
		return ul;
	}

	public Int2D br() {
		return br;
	}

	// Return the area of the hyper rectangle
	public int getArea() {
		return Math.abs((br.x - ul.x) * (br.y - ul.y));
//        return br.getRectArea(ul);
	}

	// Return the size of the hyper rectangle in each dimension
	public int[] getSize() {
		return new int[] { br.x - ul.x, br.y - ul.y };
		// return br.getOffset(ul);
	}

	// Return whether the rect contains p
	// Noted that the rect is treated as half-inclusive (ul) and half-exclusive (br)
	public boolean contains(Int2D p) {
//        ul.assertEqualDim(p);
		return (ul.x <= p.x && ul.y <= p.y && br.x > p.x && br.y > p.y);
//        return IntStream.range(0, p.nd).allMatch(i -> ul.c(i) <= p.c(i) && p.c(i) < br.c(i));
	}

	// Return whether the rect contains p
	// Noted that the rect is treated as half-inclusive (ul) and half-exclusive (br)
	public boolean contains(NumberND p) {
//        ul.assertEqualDim(p);
		double x = p.getVal(0);
		double y = p.getVal(1);
		return (ul.x <= x && ul.y <= y && br.x > x && br.y > y);
//        return IntStream.range(0, p.nd).allMatch(i -> ul.c(i) <= p.c(i) && p.c(i) < br.c(i));
	}

	/*
	 * public boolean contains(NumberND p) { // return ul.leq(p) && br.gt(p); }
	 */

	// Return whether the given rect is inside this rectangle
	public boolean contains(IntHyperRect that) {
//        ul.assertEqualDim(that.ul);
//        return IntStream.range(0, nd).allMatch(i -> this.ul.c(i) <= that.ul.c(i) && this.br.c(i) >= that.br.c(i));
		return (ul.x <= that.ul.x && ul.y <= that.ul.y && br.x >= that.br.x && br.y >= that.br.y);
	}

	// Return whether the given rect intersects with self
	public boolean isIntersect(IntHyperRect that) {
//        ul.assertEqualDim(that.ul);
//        return IntStream.range(0, nd).allMatch(i -> this.ul.c(i) < that.br.c(i) && this.br.c(i) > that.ul.c(i));
		return (ul.x < that.br.x && ul.y < that.br.y && br.x > that.ul.x && br.y > that.ul.y);
	}

	// Return the intersection of the given rect and self
	public IntHyperRect getIntersection(IntHyperRect that) {
		if (!isIntersect(that))
			throw new IllegalArgumentException(this + " does not intersect with " + that);

//        int[] c1 = IntStream.range(0, nd).map(i -> Math.max(this.ul.c(i), that.ul.c(i))).toArray();
//        int[] c2 = IntStream.range(0, nd).map(i -> Math.min(this.br.c(i), that.br.c(i))).toArray();
//
//        return new IntHyperRect(-1, new Int2D(c1), new Int2D(c2));
		return new IntHyperRect(-1, new Int2D(Math.max(ul.x, that.ul.x), Math.max(ul.y, that.ul.y)),
				new Int2D(Math.min(br.x, that.br.x), Math.min(br.y, that.br.y)));
	}

	// Symmetric resize
	public IntHyperRect resize(int dim, int val) {
		return new IntHyperRect(id, ul.shift(dim, -val), br.shift(dim, val));
	}

	// Symmetric resize at all dimension
	public IntHyperRect resize(int[] vals) {
		return new IntHyperRect(id, ul.rshift(vals), br.shift(vals));
	}

	// One-sided resize
	public IntHyperRect resize(int dim, int dir, int val) {
		if (dir > 0)
			return new IntHyperRect(id, ul.shift(dim, 0), br.shift(dim, val));
		return new IntHyperRect(id, ul.shift(dim, -val), br.shift(dim, 0));
	}

	// One-sided resize at all dimension
	public IntHyperRect resize(int dir, int[] vals) {
		if (dir > 0)
			return new IntHyperRect(id, ul.shift(0, 0), br.shift(vals));
		return new IntHyperRect(id, ul.rshift(vals), br.shift(0, 0));
	}

	// Move the rect by offset in the dimth dimension
	public IntHyperRect shift(int dim, int offset) {
		return new IntHyperRect(id, ul.shift(dim, offset), br.shift(dim, offset));
	}

	// Move the rect by the given offsets
	public IntHyperRect shift(int[] offsets) {
		return new IntHyperRect(id, ul.shift(offsets), br.shift(offsets));
	}

	public IntHyperRect rshift(int[] offsets) {
		return new IntHyperRect(id, ul.rshift(offsets), br.rshift(offsets));
	}

	// Get the upper left and bottom right points
	public Int2D[] getVertices() {
		return new Int2D[] { this.ul, this.br };
	}

	// Get all the vertices in order
	public Int2D[] getAllVertices() {
		// Refactor 20201026 >>>>>>>>>>>>>
		Int2D[] vertices = new Int2D[4]; // 4 =  1*2^(2) = 1 << 2
		for (int k = 0; k < vertices.length; k++) {
			int[] array = new int[2];
			array[0] = ((k >> 0) & 1) == 1 ? this.ul.x : this.br.x;
			array[1] = ((k >> 1) & 1) == 1 ? this.ul.y : this.br.y;
			Int2D d = new Int2D(array);
			vertices[k] = d;
		}
		return vertices;
//		return IntStream.range(0, 1 << 2)
//				.mapToObj(k -> new Int2D(
//						IntStream.range(0, 2)
//								.map(i -> ((k >> i) & 1) == 1 ? this.ul.c(i) : this.br.c(i))
//								.toArray()))
//				.toArray(size -> new Int2D[size]);
		// <<<<<<<<<<<<<<< Refactor 20201026
	}

	public Int2D getCenter() {
		// Refactor 20201026 >>>>>>>>>>>>>
		int[] center = new int[2];
		center[0] = (br.x - ul.x) / 2 + ul.x;
		center[1] = (br.y - ul.y) / 2 + ul.y;
		return new Int2D(center);
//		Int2D d = new Int2D(
//				IntStream.range(0, 2)
//				.map(i -> (br.c(i) - ul.c(i)) / 2 + ul.c(i))
//				.toArray());
//		
//		return new Int2D(IntStream.range(0, 2).map(i -> (br.c(i) - ul.c(i)) / 2 + ul.c(i)).toArray());
	    // <<<<<<<<<<<<<<<<<<<<	Refactor 20201026
	}

	// Return whether two hyper rectangles align along the given dimension
	/*
	 * public boolean isAligned(IntHyperRect that, int dim) { return
	 * this.reduceDim(dim).equals(that.reduceDim(dim)); }
	 */

	// Return whether two hyper rectangles equal (the two vertexs equal)
	public boolean equals(IntHyperRect that) {
		return this.ul.equals(that.ul) && this.br.equals(that.br);
	}

	// Sort the rectangles based on its upper left corner first and then
	// bottom-right corner and then id
	@Override
	public int compareTo(IntHyperRect that) {
		int ret;

		if ((ret = this.ul.compareTo(that.ul)) != 0)
			return ret;
		if ((ret = this.br.compareTo(that.br)) != 0)
			return ret;

		return this.id - that.id;
	}

	public static IntHyperRect getBoundingRect(IntHyperRect[] rects) {
		if (rects.length == 0)
			return null;

		Int2D ul = rects[0].ul, br = rects[0].br;
		for (IntHyperRect rect : rects) {
			ul = (Int2D) ul.min(rect.ul);
			br = (Int2D) br.max(rect.ul);
		}

		return new IntHyperRect(-1, ul, br);
	}

	public Iterator<Int2D> iterator() {
		// return IntPointGenerator.getBlock(this).iterator();
		return new Iterator<Int2D>() {
			int x = ul.x;
			int y = ul.y;

			public boolean hasNext() {
				return y < br.y;
			}

			public Int2D next() {
				if (hasNext()) {
					Int2D val = new Int2D(x, y);
					x++;
					if (x >= br.x) {
						x = ul.x;
						y++;
					}
					return val;
				} else {
					return null;
				}
			}
		};
	}

	public String toString() {
		return String.format("%s<%d, %s, %s>", this.getClass().getSimpleName(), id, ul.toString(), br.toString());
	}

	// Return a copy of the hyper rectangle with the given dimension removed
	/*
	 * public IntHyperRect reduceDim(int dim) { return new IntHyperRect(id,
	 * ul.reduceDim(dim), br.reduceDim(dim)); }
	 */

	// Split the rect into multiple rectangles based on the given array of points
	public ArrayList<IntHyperRect> split(Int2D[] ps) {
		if (!Arrays.stream(ps).allMatch(p -> this.contains(p)))
			throw new IllegalArgumentException("Given points must be inside the rectangle");

		ArrayList<IntHyperRect> ret = new ArrayList<IntHyperRect>();
		final int numDelims = ps.length + 2;
//        final int numRects = (int)Math.pow(numDelims - 1, 2);
		final int numRects = (numDelims - 1) * (numDelims - 1);
		int[][] delims = new int[2][numDelims];

		// Refactor 20201026 >>>>>>>>>>>>>
		delims[0][0] = this.ul.x;
		delims[0][1] = this.br.x;
		delims[1][0] = this.ul.y;
		delims[1][1] = this.br.y;
//		when i = 0;
		for (int j = 2; j < numDelims; j++)
			delims[0][j] = ps[j - 2].x;
		Arrays.sort(delims[0]);
//		when i = 1;
		for (int j = 2; j < numDelims; j++)
			delims[1][j] = ps[j - 2].y;
		Arrays.sort(delims[1]);
//		for (int i = 0; i < 2; i++) {
//			delims[i][0] = this.ul.c(i);
//			delims[i][1] = this.br.c(i);
//
//			for (int j = 2; j < numDelims; j++)
//				delims[i][j] = ps[j - 2].c(i);
//
//			Arrays.sort(delims[i]);
//		}
	    // <<<<<<<<<<<<<<<<<<<<	Refactor 20201026

		for (int k = 0; k < numRects; k++) {
			boolean empty = false;
			int[] ul = new int[2], br = new int[2];

			for (int i = 0; i < 2; i++) {
//                int stride = (int)Math.pow(numDelims - 1, 2 - i - 1);
				int stride = (i == 0) ? numDelims - 1 : 1;
				int idx = k / stride % (numDelims - 1);
				ul[i] = delims[i][idx];
				br[i] = delims[i][idx + 1];
				if (ul[i] == br[i]) {
					empty = true;
					break;
				}
			}

			if (!empty)
				ret.add(new IntHyperRect(id, new Int2D(ul), new Int2D(br)));
		}

		return ret;
	}

	// Convert the rect to rects under a toroidal field represented by bound
	// assuming the exceeding portion is small, i.e., within the (1 x size) of the
	// field
	public ArrayList<IntHyperRect> toToroidal(IntHyperRect bound) {
		ArrayList<IntHyperRect> ret = new ArrayList<IntHyperRect>();
		int[] size = bound.getSize();

		for (IntHyperRect rect : this.split(this.getIntersection(bound).getVertices())) {
			int[] offsets = new int[2];
			// Refactor 20201026 >>>>>>>>>>>>>
			if (rect.br.x > bound.br.x)
				offsets[0] = -size[0];
			else if (rect.ul.x < bound.ul.x)
				offsets[0] = size[0];
			if (rect.br.y > bound.br.y)
				offsets[1] = -size[1];
			else if (rect.ul.y < bound.ul.y)
				offsets[1] = size[1];
//			for (int i = 0; i < 2; i++)
//				if (rect.br.c(i) > bound.br.c(i))
//					offsets[i] = -size[i];
//				else if (rect.ul.c(i) < bound.ul.c(i))
//					offsets[i] = size[i];
		    // <<<<<<<<<<<<<<<<<<<<	Refactor 20201026
			ret.add(rect.shift(offsets));
		}

		return ret;
	}

	// fix for bugs
	public Int2D toToroidal(Int2D p) {
		int x = p.x;
		int y = p.y;
		int width = br.x - ul.x;
		int height = br.y - ul.y;

		return new Int2D(tx(x, width), ty(y, height));
	}

	// slight revision for more efficiency
	final int tx(int x, int width) {
		if (x >= 0 && x < width)
			return x; // do clearest case first
		x = x % width;
		if (x < 0)
			x = x + width;
		return x;
	}

	// slight revision for more efficiency
	final int ty(int y, int height) {
		if (y >= 0 && y < height)
			return y; // do clearest case first
		y = y % height;
		if (y < 0)
			y = y + height;
		return y;
	}

	public static void main(String[] args) {
		Int2D p1 = new Int2D(new int[] { 1, 1 });
		Int2D p2 = new Int2D(new int[] { 0, 3 });
		Int2D p3 = new Int2D(new int[] { 4, 4 });
		Int2D p4 = new Int2D(new int[] { 2, 6 });
		IntHyperRect r1 = new IntHyperRect(0, p1, p3);
		IntHyperRect r2 = new IntHyperRect(1, p2, p4);

		System.out.println(r1.getIntersection(r2));

		Int2D p5 = new Int2D(new int[] { 0, 4 });
		IntHyperRect r3 = new IntHyperRect(1, p5, p4);

		System.out.println(r1.isIntersect(r3));

		System.out.println(r1.contains(r2));
		System.out.println(r1.contains(r1.getIntersection(r2)));

		for (Int2D p : r1.getAllVertices())
			System.out.println("vertices: " + p);

		Int2D pp1 = new Int2D(new int[] { 0, 1 });
		Int2D pp2 = new Int2D(new int[] { 5, 0 });
		Int2D pp3 = new Int2D(new int[] { 10, 11 });
		Int2D pp4 = new Int2D(new int[] { 12, 12 });
		IntHyperRect rr1 = new IntHyperRect(0, pp1, pp3);
		IntHyperRect rr2 = new IntHyperRect(1, pp2, pp4);
		for (IntHyperRect r : rr2.toToroidal(rr1))
			System.out.println("toroidal: " + r);

		Int2D p6 = new Int2D(new int[] { 1, 2, 3 });
		Int2D p7 = new Int2D(new int[] { 4, 7, 9 });
		IntHyperRect r4 = new IntHyperRect(0, p6, p7);
		for (Int2D p : r4)
			System.out.println(r4 + " Iterating points " + p);
	}
}
