package sim.util;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.IntStream;

// TODO Move to NdRectangle
public class IntHyperRect implements Comparable<IntHyperRect>, Iterable<IntPoint> {
    int nd, id;
    IntPoint ul, br;

    public IntHyperRect(int id, IntPoint ul, IntPoint br) {
        this.id = id;

        if (ul.nd != br.nd)
            throw new IllegalArgumentException("Number of dimensions must be the same. Got " + ul.nd + " and " + br.nd);

        this.nd = ul.nd;

        for (int i = 0; i < nd; i++)
            if (br.c[i] < ul.c[i])
                throw new IllegalArgumentException("All br's components " + Arrays.toString(br.c) + " should be greater than or equal to ul's " + Arrays.toString(ul.c));

        this.ul = ul;
        this.br = br;
    }

    public IntHyperRect(int[] size) {
        this(-1, new IntPoint(new int[size.length]), new IntPoint(size));
    }

    public int getNd() {
        return nd;
    }

    public int getId() {
        return id;
    }

    public void setId(int newId) {
        id = newId;
    }

    public IntPoint ul() {
        return ul;
    }

    public IntPoint br() {
        return br;
    }

    // Return the area of the hyper rectangle
    public int getArea() {
        return br.getRectArea(ul);
    }

    // Return the size of the hyper rectangle in each dimension
    public int[] getSize() {
        return br.getOffset(ul);
    }

    // Return whether the rect contains p
    // Noted that the rect is treated as half-inclusive (ul) and half-exclusive (br)
    public boolean contains(IntPoint p) {
        ul.assertEqualDim(p);
        return IntStream.range(0, p.nd).allMatch(i -> ul.c[i] <= p.c[i] && p.c[i] < br.c[i]);
    }

    public boolean contains(NdPoint p) {
        return ul.leq(p) && br.gt(p);
    }

    // Return whether the given rect is inside this rectangle
    public boolean contains(IntHyperRect that) {
        ul.assertEqualDim(that.ul);
        return IntStream.range(0, nd).allMatch(i -> this.ul.c[i] <= that.ul.c[i] && this.br.c[i] >= that.br.c[i]);
    }

    // Return whether the given rect intersects with self
    public boolean isIntersect(IntHyperRect that) {
        ul.assertEqualDim(that.ul);
        return IntStream.range(0, nd).allMatch(i -> this.ul.c[i] < that.br.c[i] && this.br.c[i] > that.ul.c[i]);
    }

    // Return the intersection of the given rect and self
    public IntHyperRect getIntersection(IntHyperRect that) {
        if (!isIntersect(that))
            throw new IllegalArgumentException(this + " does not intersect with " + that);

        int[] c1 = IntStream.range(0, nd).map(i -> Math.max(this.ul.c[i], that.ul.c[i])).toArray();
        int[] c2 = IntStream.range(0, nd).map(i -> Math.min(this.br.c[i], that.br.c[i])).toArray();

        return new IntHyperRect(-1, new IntPoint(c1), new IntPoint(c2));
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
    public IntPoint[] getVertices() {
        return new IntPoint[] {this.ul, this.br};
    }

    // Get all the vertices in order
    public IntPoint[] getAllVertices() {
        return IntStream.range(0, 1 << nd)
            .mapToObj(k -> new IntPoint(
                                        IntStream.range(0, nd)
                                        .map(i -> ((k >> i) & 1) == 1 ? this.ul.c[i] : this.br.c[i])
                                        .toArray()))
            .toArray(size -> new IntPoint[size]);
    }

    // Return the segment of the hyper rectangle on the given dimension
    public Segment getSegment(int dim) {
        ul.assertEqualDim(dim);
        return new Segment((double)ul.c[dim], (double)br.c[dim], id);
    }

    public IntPoint getCenter() {
        return new IntPoint(IntStream.range(0, nd).map(i -> (br.c[i] - ul.c[i]) / 2 + ul.c[i]).toArray());
    }

    // Return whether two hyper rectangles align along the given dimension
    public boolean isAligned(IntHyperRect that, int dim) {
        return this.reduceDim(dim).equals(that.reduceDim(dim));
    }

    // Return whether two hyper rectangles equal (the two vertexs equal)
    public boolean equals(IntHyperRect that) {
        return this.ul.equals(that.ul) && this.br.equals(that.br);
    }

    // Sort the rectangles based on its upper left corner first and then bottom-right corner and then id
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

        IntPoint ul = rects[0].ul, br = rects[0].br;
        for (IntHyperRect rect : rects) {
            ul = (IntPoint)ul.min(rect.ul);
            br = (IntPoint)br.max(rect.ul);
        }

        return new IntHyperRect(-1, ul, br);
    }

    public Iterator<IntPoint> iterator() {
        return IntPointGenerator.getBlock(this).iterator();
    }

    public String toString() {
        return String.format("%s<%d, %s, %s>", this.getClass().getSimpleName(), id, ul.toString(), br.toString());
    }

    // Return a copy of the hyper rectangle with the given dimension removed
    public IntHyperRect reduceDim(int dim) {
        return new IntHyperRect(id, ul.reduceDim(dim), br.reduceDim(dim));
    }

    // Split the rect into multiple rectangles based on the given array of points
    public ArrayList<IntHyperRect> split(IntPoint[] ps) {
        if (!Arrays.stream(ps).allMatch(p -> this.contains(p)))
            throw new IllegalArgumentException("Given points must be inside the rectangle");

        ArrayList<IntHyperRect> ret = new ArrayList<IntHyperRect>();
        final int numDelims = ps.length + 2;
        final int numRects = (int)Math.pow(numDelims - 1, nd);
        int[][] delims = new int[nd][numDelims];

        for (int i = 0; i < nd; i++) {
            delims[i][0] = this.ul.c[i];
            delims[i][1] = this.br.c[i];

            for (int j = 2; j < numDelims; j++)
                delims[i][j] = ps[j - 2].c[i];

            Arrays.sort(delims[i]);
        }

        for (int k = 0; k < numRects; k++) {
            boolean nonEmpty = true;
            int[] ul = new int[nd], br = new int[nd];

            for (int i = 0; i < nd; i++) {
                int stride = (int)Math.pow(numDelims - 1, nd - i - 1);
                int idx = k / stride % (numDelims - 1);
                ul[i] = delims[i][idx];
                br[i] = delims[i][idx + 1];
                if (ul[i] == br[i]) {
                    nonEmpty = false;
                    break;
                }
            }

            if (nonEmpty)
                ret.add(new IntHyperRect(id, new IntPoint(ul), new IntPoint(br)));
        }

        return ret;
    }

    // Convert the rect to rects under a toroidal field represented by bound
    // assuming the exceeding portion is small, i.e., within the (1 x size) of the field
    public ArrayList<IntHyperRect> toToroidal(IntHyperRect bound) {
        ArrayList<IntHyperRect> ret = new ArrayList<IntHyperRect>();
        int[] size = bound.getSize();

        for (IntHyperRect rect : this.split(this.getIntersection(bound).getVertices())) {
            int[] offsets = new int[nd];
            for (int i = 0; i < nd; i++)
                if (rect.br.c[i] > bound.br.c[i])
                    offsets[i] = -size[i];
                else if (rect.ul.c[i] < bound.ul.c[i])
                    offsets[i] = size[i];
            ret.add(rect.shift(offsets));
        }

        return ret;
    }

    public static void main(String[] args) {
        IntPoint p1 = new IntPoint(new int[] {1, 1});
        IntPoint p2 = new IntPoint(new int[] {0, 3});
        IntPoint p3 = new IntPoint(new int[] {4, 4});
        IntPoint p4 = new IntPoint(new int[] {2, 6});
        IntHyperRect r1 = new IntHyperRect(0, p1, p3);
        IntHyperRect r2 = new IntHyperRect(1, p2, p4);

        System.out.println(r1.getIntersection(r2));

        IntPoint p5 = new IntPoint(new int[] {0, 4});
        IntHyperRect r3 = new IntHyperRect(1, p5, p4);

        System.out.println(r1.isIntersect(r3));

        System.out.println(r1.contains(r2));
        System.out.println(r1.contains(r1.getIntersection(r2)));

        for (IntPoint p : r1.getAllVertices())
            System.out.println("vertices: " + p);

        IntPoint pp1 = new IntPoint(new int[] {0, 1});
        IntPoint pp2 = new IntPoint(new int[] {5, 0});
        IntPoint pp3 = new IntPoint(new int[] {10, 11});
        IntPoint pp4 = new IntPoint(new int[] {12, 12});
        IntHyperRect rr1 = new IntHyperRect(0, pp1, pp3);
        IntHyperRect rr2 = new IntHyperRect(1, pp2, pp4);
        for (IntHyperRect r : rr2.toToroidal(rr1))
            System.out.println("toroidal: " + r);

        IntPoint p6 = new IntPoint(new int[] {1, 2, 3});
        IntPoint p7 = new IntPoint(new int[] {4, 7, 9});
        IntHyperRect r4 = new IntHyperRect(0, p6, p7);
        for (IntPoint p : r4)
            System.out.println(r4 + " Iterating points " + p);
    }
}
