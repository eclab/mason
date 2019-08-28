package sim.util;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.IntStream;

public class NdRectangle implements Comparable<NdRectangle>, Iterable<IntPoint> {
    int nd, id, proc;
    NdPoint ul, br;

    public NdRectangle(int id, int proc, NdPoint ul, NdPoint br) {
        this.id = id;
        this.proc = proc;

        if (ul.getNd() != br.getNd())
            throw new IllegalArgumentException("Number of dimensions must be the same. Got " + ul.getNd() + " and " + br.getNd());

        this.nd = ul.getNd();

        if (br.compareTo(ul) < 0)
            throw new IllegalArgumentException("All p2's components should be greater than or equal to p1's corresponding one");

        this.ul = ul;
        this.br = br;
    }

    public NdRectangle(int id, NdPoint ul, NdPoint br) {
        this(id, -1, ul, br);
    }

    public NdRectangle(int[] size) {
        this(-1, -1, new IntPoint(new int[size.length]), new IntPoint(size));
    }

    public NdRectangle(double[] size) {
        this(-1, -1, new DoublePoint(new double[size.length]), new DoublePoint(size));
    }

    public int getLPId() {
        return proc;
    }

    public int getId() {
        return id;
    }

    public int getNd() {
        return nd;
    }

    public NdPoint ul() {
        return ul;
    }

    public NdPoint br() {
        return br;
    }

    public int getAreaInt() {
        return nd == 0 ? 0 : Math.abs(Arrays.stream(getSizeInt()).reduce(1, (x, y) -> x * y));
    }

    public double getAreaDouble() {
        return nd == 0 ? 0 : Math.abs(Arrays.stream(getSizeDouble()).reduce(1, (x, y) -> x * y));
    }

    public int[] getSizeInt() {
        return br.getOffsetsInt(ul);
    }

    public double[] getSizeDouble() {
        return br.getOffsetsDouble(ul);
    }

    public boolean contains(NdPoint p) {
        return p.geq(ul) && p.lt(br);
    }

    public boolean contains(NdRectangle r) {
        return ul.leq(r.ul()) && br.geq(r.br());
    }

    public boolean isIntersect(NdRectangle r) {
        return ul.lt(r.br()) && br.gt(r.ul());
    }

    public NdRectangle getIntersection(NdRectangle r) {
        if (!isIntersect(r))
            throw new IllegalArgumentException(this + " does not intersect with " + r);

        return new NdRectangle(-1, -1, ul.max(r.ul()), br.min(r.br()));
    }

    // Symmetric resize
    public NdRectangle resize(int dim, int val) {
        return new NdRectangle(id, proc, ul.shift(dim, -val), br.shift(dim, val));
    }

    public NdRectangle resize(int dim, double val) {
        return new NdRectangle(id, proc, ul.shift(dim, -val), br.shift(dim, val));
    }

    // Symmetric resize at all dimension
    public NdRectangle resize(int[] vals) {
        return new NdRectangle(id, proc, ul.rshift(vals), br.shift(vals));
    }

    public NdRectangle resize(double[] vals) {
        return new NdRectangle(id, proc, ul.rshift(vals), br.shift(vals));
    }

    // One-sided resize
    public NdRectangle resize(int dim, int dir, int val) {
        if (dir > 0)
            return new NdRectangle(id, proc, ul.shift(dim, 0), br.shift(dim, val));
        return new NdRectangle(id, proc, ul.shift(dim, -val), br.shift(dim, 0));
    }

    public NdRectangle resize(int dim, int dir, double val) {
        if (dir > 0)
            return new NdRectangle(id, proc, ul.shift(dim, 0), br.shift(dim, val));
        return new NdRectangle(id, proc, ul.shift(dim, -val), br.shift(dim, 0));
    }

    // One-sided resize at all dimension
    public NdRectangle resize(int dir, int[] vals) {
        if (dir > 0)
            return new NdRectangle(id, proc, ul.shift(0, 0), br.shift(vals));
        return new NdRectangle(id, proc, ul.rshift(vals), br.shift(0, 0));
    }

    public NdRectangle resize(int dir, double[] vals) {
        if (dir > 0)
            return new NdRectangle(id, proc, ul.shift(0, 0), br.shift(vals));
        return new NdRectangle(id, proc, ul.rshift(vals), br.shift(0, 0));
    }

    // Move the rect by offset in the dimth dimension
    public NdRectangle shift(int dim, int offset) {
        return new NdRectangle(id, proc, ul.shift(dim, offset), br.shift(dim, offset));
    }

    public NdRectangle shift(int dim, double offset) {
        return new NdRectangle(id, proc, ul.shift(dim, offset), br.shift(dim, offset));
    }

    // Move the rect by the given offsets
    public NdRectangle shift(int[] offsets) {
        return new NdRectangle(id, proc, ul.shift(offsets), br.shift(offsets));
    }

    public NdRectangle shift(double[] offsets) {
        return new NdRectangle(id, proc, ul.shift(offsets), br.shift(offsets));
    }

    // Move the rect by the given offsets in reverse direction
    public NdRectangle rshift(int[] offsets) {
        return new NdRectangle(id, proc, ul.rshift(offsets), br.rshift(offsets));
    }

    public NdRectangle rshift(double[] offsets) {
        return new NdRectangle(id, proc, ul.rshift(offsets), br.rshift(offsets));
    }

    public NdPoint[] getVertices() {
        return new NdPoint[] {ul, br};
    }

    public NdPoint[] getAllVertices() {
        if ((ul instanceof IntPoint) && (br instanceof IntPoint)) {
            int[] a = (int[]) ul.getArray(), b = (int[]) br.getArray();
            return IntStream.range(0, 1 << nd)
                .mapToObj(k -> new IntPoint(
                                            IntStream.range(0, nd)
                                            .map(i -> ((k >> i) & 1) == 1 ? a[i] : b[i])
                                            .toArray()))
                .toArray(size -> new NdPoint[size]);
        }

        double[] a = ul.getArrayInDouble(), b = br.getArrayInDouble();
        return IntStream.range(0, 1 << nd)
            .mapToObj(k -> new DoublePoint(
                                           IntStream.range(0, nd)
                                           .mapToDouble(i -> ((k >> i) & 1) == 1 ? a[i] : b[i])
                                           .toArray()))
            .toArray(size -> new NdPoint[size]);
    }

    public Segment getSegment(int dim) {
        return new Segment(ul.getArrayInDouble()[dim], br.getArrayInDouble()[dim], id);
    }

    public boolean isAligned(NdRectangle that, int dim) {
        return this.reduceDim(dim).equals(that.reduceDim(dim));
    }

    public boolean equals(NdRectangle that) {
        return this.ul.equals(that.ul) && this.br.equals(that.br);
    }

    public int compareTo(NdRectangle that) {
        int ret;

        if ((ret = this.ul.compareTo(that.ul)) != 0)
            return ret;
        if ((ret = this.br.compareTo(that.br)) != 0)
            return ret;

        return this.id - that.id;
    }

    public NdRectangle reduceDim(int dim) {
        return new NdRectangle(id, proc, ul.reduceDim(dim), br.reduceDim(dim));
    }

    public boolean isIntRectangle() {
        return (ul instanceof IntPoint) && (br instanceof IntPoint);
    }

    public ArrayList<NdRectangle> split(NdPoint[] ps) {
        if (!isIntRectangle())
            throw new IllegalArgumentException("Split NdRectangle with vertices other than IntPoint are not supported yet");

        if (!Arrays.stream(ps).allMatch(p -> p instanceof IntPoint))
            throw new IllegalArgumentException("Split points other than IntPoint are not supported yet");

        if (!Arrays.stream(ps).allMatch(p -> this.contains(p)))
            throw new IllegalArgumentException("Given points must be inside the rectangle");

        return splitInt(ps);
    }

    public ArrayList<NdRectangle> splitInt(NdPoint[] ps) {
        ArrayList<NdRectangle> ret = new ArrayList<NdRectangle>();
        final int numDelims = ps.length + 2;
        final int numRects = (int)Math.pow(numDelims - 1, nd);

        int[][] delims = new int[nd][numDelims];
        int[] a = (int[]) ul.getArray(), b = (int[]) br.getArray();

        for (int i = 0; i < nd; i++) {
            delims[i][0] = a[i];
            delims[i][1] = b[i];

            for (int j = 2; j < numDelims; j++)
                delims[i][j] = ((int[])(ps[j - 2].getArray()))[i];

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
                ret.add(new NdRectangle(id, proc, new IntPoint(ul), new IntPoint(br)));
        }

        return ret;
    }

    public ArrayList<NdRectangle> toToroidal(NdRectangle bound) {
        if (!this.isIntRectangle() || !bound.isIntRectangle())
            throw new IllegalArgumentException("Only IntRectangles are supported");

        ArrayList<NdRectangle> ret = new ArrayList<NdRectangle>();
        int[] size = bound.getSizeInt();
        int[] bul = (int[])bound.ul().getArray(), bbr = (int[])bound.br().getArray();

        for (NdRectangle rect : this.split(this.getIntersection(bound).getVertices())) {
            int[] offsets = new int[nd];
            int[] rul = (int[])rect.ul().getArray(), rbr = (int[])rect.br().getArray();
            for (int i = 0; i < nd; i++)
                if (rbr[i] > bbr[i])
                    offsets[i] = -size[i];
                else if (rul[i] < bul[i])
                    offsets[i] = size[i];
            ret.add(rect.shift(offsets));
        }

        return ret;
    }

    public Iterator<IntPoint> iterator() {
        if (!(ul instanceof IntPoint) || !(br instanceof IntPoint))
            throw new UnsupportedOperationException("Can only iterate when both ul and br are IntPoint - got "
                                                    + ul.getClass().getSimpleName() + " " + br.getClass().getSimpleName());
        return new NdRectIter();
    }

    private class NdRectIter implements Iterator<IntPoint> {
        int[] coords, st, sp;
        int curr, ub;

        public NdRectIter() {
            st = (int[])ul.getArray();
            sp = (int[])br.getArray();
            coords = Arrays.copyOf(st, nd);
            ub = getAreaInt();
            curr = 0;
        }

        public boolean hasNext() {
            return curr < ub;
        }

        public IntPoint next() {
            IntPoint ret = new IntPoint(coords);

            for (int i = nd - 1; i >= 0; i--) {
                coords[i]++;
                if (coords[i] == sp[i])
                    coords[i] = st[i];
                else
                    break;
            }

            curr++;
            return ret;
        }
    }

    public String toString() {
        return String.format("%s<%d, %s, %s>", this.getClass().getSimpleName(), id, ul.toString(), br.toString());
    }

    // // TODO temporary solution - remove after all related structures has been converted to use NdRectangle
    // public IntHyperRect toIntRect(int newId) {
    //      IntPoint nul, nbr;

    //      if (ul instanceof IntPoint)
    //              nul = ul;
    //      else
    //              nul = new IntPoint(Arrays.stream(((DoublePoint)ul).c).mapToInt(x -> (int)x).toArray());

    //      if (br instanceof IntPoint)
    //              nbr = br;
    //      else
    //              nbr = new IntPoint(Arrays.stream(((DoublePoint)br).c).mapToInt(x -> (int)x).toArray());

    //      return new IntHyperRect(newId, nul, nr);
    // }

    public static void main(String[] args) {
        IntPoint p1 = new IntPoint(1, 1);
        IntPoint p2 = new IntPoint(0, 3);
        IntPoint p3 = new IntPoint(4, 4);
        IntPoint p4 = new IntPoint(2, 6);
        NdRectangle r1 = new NdRectangle(0, 0, p1, p3);
        NdRectangle r2 = new NdRectangle(1, 0, p2, p4);

        System.out.println(r1.getIntersection(r2));

        IntPoint p5 = new IntPoint(0, 4);
        NdRectangle r3 = new NdRectangle(1, 0, p5, p4);

        System.out.println(r1.isIntersect(r3));

        System.out.println(r1.contains(r2));
        System.out.println(r1.contains(r1.getIntersection(r2)));

        for (NdPoint p : r1.getAllVertices())
            System.out.println("vertices: " + p);

        IntPoint pp1 = new IntPoint(0, 1);
        IntPoint pp2 = new IntPoint(5, 0);
        IntPoint pp3 = new IntPoint(10, 11);
        IntPoint pp4 = new IntPoint(12, 12);
        NdRectangle rr1 = new NdRectangle(0, 0, pp1, pp3);
        NdRectangle rr2 = new NdRectangle(1, 0, pp2, pp4);
        for (NdRectangle r : rr2.toToroidal(rr1))
            System.out.println("toroidal: " + r);

        IntPoint p6 = new IntPoint(1, 2, 3);
        IntPoint p7 = new IntPoint(4, 7, 9);
        NdRectangle r4 = new NdRectangle(0, 0, p6, p7);
        for (IntPoint p : r4)
            System.out.println(r4 + " Iterating points " + p);
    }
}
