package sim.field.partitioning;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.Iterator;
import java.util.stream.Stream;
import sim.util.*;

public abstract class IntPointGenerator implements Supplier<Int2D>, Iterable<Int2D> {
    int remaining;

// used to have nd
    protected IntPointGenerator(int remaining) {
        this.remaining = remaining;
    }

	public int getNd() { return 2; }
	
    protected abstract Int2D getNext();

    public int getNumRemaining() {
        return remaining;
    }

    @Override
    public Int2D get() {
        --remaining;
        return getNext();
    }

    @Override
    public Iterator<Int2D> iterator() {
        return new Iterator<Int2D>() {
            public boolean hasNext() {
                return remaining > 0;
            }

            public Int2D next() {
                return get();
            }
        };
    }

    public static IntPointGenerator getLayer(Int2D st, int fromLayer, int toLayer) {
        return new LayerGenerator(st, fromLayer, toLayer);
    }

    public static IntPointGenerator getLayer(Int2D st, int thisLayer) {
        return getLayer(st, thisLayer, thisLayer);
    }

	// I think this is just centered at 0?
    public static IntPointGenerator getLayer(int nd, int toLayer) {
        return getLayer(new Int2D(0, 0), 0, toLayer);
    }

    public static IntPointGenerator getBlock(IntHyperRect rect) {
        return new BlockGenerator(rect);
    }

    public static IntPointGenerator getBlock(Int2D st, Int2D ed) {
        return getBlock(new IntHyperRect(-1, st, ed));
    }

    public static IntPointGenerator getBlock(int[] size) {
        return getBlock(new Int2D(new int[size.length]), new Int2D(size));
    }

    public static void main(String[] args) {
        System.out.println("Test LayerGenerator with stream...");
        IntPointGenerator gl = getLayer(new Int2D(0, 0), 2);
        Stream.generate(gl).limit(gl.getNumRemaining()).forEach(System.out::println);

        System.out.println("Test BlockGenerator with stream...");
        IntPointGenerator gb = getBlock(new int[] {3, 3, 3});
        Stream.generate(gb).limit(gb.getNumRemaining()).forEach(System.out::println);

        System.out.println("Test LayerGenerator with iteration...");
        IntPointGenerator gliter = getLayer(new Int2D(0, 0), 2);
        gliter.forEach(System.out::println);

        System.out.println("Test BlockGenerator with iteration...");
        IntPointGenerator gbiter = getBlock(new int[] {3, 3, 3});
        gbiter.forEach(System.out::println);

        System.out.println("Test LayerGenerator with iteration from 2 to 2...");
        IntPointGenerator gliter2 = getLayer(new Int2D(0, 0), 2, 2);
        gliter2.forEach(System.out::println);
    }

    protected static class LayerGenerator extends IntPointGenerator {
        final Int2D p;
        final int ml;
        final int[] offsets;
        int cl, idx, ub;

        // Generate all the points layer by layer around the given points
        // Starting from the fromLayer to toLayer, both inclusive
        public LayerGenerator(Int2D p, int fromLayer, int toLayer) {
            super((int)Math.pow(toLayer * 2 + 1, p.getNd())
                  - (int)Math.pow(fromLayer > 0 ? fromLayer * 2 - 1 : 0, p.getNd()));

            this.p = p;
            this.ml = toLayer;
            this.offsets = new int[getNd()];

            setLayer(fromLayer);
        }

        private void setLayer(int k) {
            cl = k;
            idx = 0;
            ub = (int)Math.pow(2 * cl + 1, getNd());
            Arrays.fill(offsets, -cl);
        }

        public Int2D getNext() {
            // check if we need to expand to an outer layer
            if (++idx == ub)
                setLayer(cl + 1);
            else {
                for (int i = getNd() - 1; i >= 0; i--)
                    if (++offsets[i] == cl + 1)
                        offsets[i] = -cl;
                    else
                        break;
                // skip the points that are not on the current layer
                if (Arrays.stream(offsets).allMatch(x -> x < cl && x > -cl)) {
                    offsets[getNd() - 1] = cl;
                    idx += 2 * cl - 1;
                }
            }

            Int2D ret = p.shift(offsets);
            return ret;
        }
    }

    protected static class BlockGenerator extends IntPointGenerator {
        final int[] c;
        final Int2D ul, br;

        public BlockGenerator(IntHyperRect rect) {
            super(rect.getArea());

            ul = rect.ul;
            br = rect.br;
            c = Arrays.copyOf(ul.c(), getNd());
        }

        public Int2D getNext() {
            for (int i = getNd() - 1; i >= 0; i--)
                if (++c[i] == br.c(i))
                    c[i] = ul.c(i);
                else
                    break;

            Int2D ret = new Int2D(c);
            return ret;
        }
    }
}
