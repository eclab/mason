package sim.util;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.Iterator;
import java.util.stream.Stream;

public abstract class IntPointGenerator implements Supplier<IntPoint>, Iterable<IntPoint> {
    final int nd;
    int remaining;

    protected IntPointGenerator(int nd, int remaining) {
        this.nd = nd;
        this.remaining = remaining;
    }

    protected abstract IntPoint getNext();

    public int getNumRemaining() {
        return remaining;
    }

    @Override
    public IntPoint get() {
        --remaining;
        return getNext();
    }

    @Override
    public Iterator<IntPoint> iterator() {
        return new Iterator<IntPoint>() {
            public boolean hasNext() {
                return remaining > 0;
            }

            public IntPoint next() {
                return get();
            }
        };
    }

    public static IntPointGenerator getLayer(IntPoint st, int fromLayer, int toLayer) {
        return new LayerGenerator(st, fromLayer, toLayer);
    }

    public static IntPointGenerator getLayer(IntPoint st, int thisLayer) {
        return getLayer(st, thisLayer, thisLayer);
    }

    public static IntPointGenerator getLayer(int nd, int toLayer) {
        return getLayer(new IntPoint(new int[nd]), 0, toLayer);
    }

    public static IntPointGenerator getBlock(IntHyperRect rect) {
        return new BlockGenerator(rect);
    }

    public static IntPointGenerator getBlock(IntPoint st, IntPoint ed) {
        return getBlock(new IntHyperRect(-1, st, ed));
    }

    public static IntPointGenerator getBlock(int[] size) {
        return getBlock(new IntPoint(new int[size.length]), new IntPoint(size));
    }

    public static void main(String[] args) {
        System.out.println("Test LayerGenerator with stream...");
        IntPointGenerator gl = getLayer(new IntPoint(0, 0, 0), 2);
        Stream.generate(gl).limit(gl.getNumRemaining()).forEach(System.out::println);

        System.out.println("Test BlockGenerator with stream...");
        IntPointGenerator gb = getBlock(new int[] {3, 3, 3});
        Stream.generate(gb).limit(gb.getNumRemaining()).forEach(System.out::println);

        System.out.println("Test LayerGenerator with iteration...");
        IntPointGenerator gliter = getLayer(new IntPoint(0, 0, 0), 2);
        gliter.forEach(System.out::println);

        System.out.println("Test BlockGenerator with iteration...");
        IntPointGenerator gbiter = getBlock(new int[] {3, 3, 3});
        gbiter.forEach(System.out::println);

        System.out.println("Test LayerGenerator with iteration from 2 to 2...");
        IntPointGenerator gliter2 = getLayer(new IntPoint(0, 0, 0), 2, 2);
        gliter2.forEach(System.out::println);
    }

    protected static class LayerGenerator extends IntPointGenerator {
        final IntPoint p;
        final int ml;
        final int[] offsets;
        int cl, idx, ub;

        // Generate all the points layer by layer around the given points
        // Starting from the fromLayer to toLayer, both inclusive
        public LayerGenerator(IntPoint p, int fromLayer, int toLayer) {
            super(p.nd, (int)Math.pow(toLayer * 2 + 1, p.nd)
                  - (int)Math.pow(fromLayer > 0 ? fromLayer * 2 - 1 : 0, p.nd));

            this.p = p;
            this.ml = toLayer;
            this.offsets = new int[nd];

            setLayer(fromLayer);
        }

        private void setLayer(int k) {
            cl = k;
            idx = 0;
            ub = (int)Math.pow(2 * cl + 1, nd);
            Arrays.fill(offsets, -cl);
        }

        public IntPoint getNext() {
            IntPoint ret = p.shift(offsets);

            // check if we need to expand to an outer layer
            if (++idx == ub)
                setLayer(cl + 1);
            else {
                for (int i = nd - 1; i >= 0; i--)
                    if (++offsets[i] == cl + 1)
                        offsets[i] = -cl;
                    else
                        break;
                // skip the points that are not on the current layer
                if (Arrays.stream(offsets).allMatch(x -> x < cl && x > -cl)) {
                    offsets[nd - 1] = cl;
                    idx += 2 * cl - 1;
                }
            }

            return ret;
        }
    }

    protected static class BlockGenerator extends IntPointGenerator {
        final int[] c;
        final IntPoint ul, br;

        public BlockGenerator(IntHyperRect rect) {
            super(rect.nd, rect.getArea());

            ul = rect.ul;
            br = rect.br;
            c = Arrays.copyOf(ul.c, nd);
        }

        public IntPoint getNext() {
            IntPoint ret = new IntPoint(c);

            for (int i = nd - 1; i >= 0; i--)
                if (++c[i] == br.c[i])
                    c[i] = ul.c[i];
                else
                    break;

            return ret;
        }
    }
}
