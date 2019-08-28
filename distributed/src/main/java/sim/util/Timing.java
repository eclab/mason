package sim.util;

import java.util.HashMap;
import java.util.NoSuchElementException;

public class Timing {

    public static final String LB_RUNTIME = "_MASON_LOAD_BALANCING_RUNTIME";
    public static final String LB_OVERHEAD = "_MASON_LOAD_BALANCING_OVERHEAD";
    public static final String MPI_SYNC_OVERHEAD = "_MASON_MPI_SYNC_OVERHEAD";

    private static int window = 100;
    private static HashMap<String, TimingStat> m = new HashMap<String, TimingStat>();
    private static NanoClock clock = new NanoClock() {
            public long nanoTime() { return System.nanoTime(); }
            public void advance(long val) { throw new UnsupportedOperationException("Cannot set nano time for real clock"); }
        };

    private static class FakeClock implements NanoClock {
        public long val = 0;
        public long nanoTime() { return val; }
        public void advance(long val) { this.val += val; }
    }

    private interface NanoClock {
        long nanoTime();
        void advance(long val);
    }

    public static void useFakeClock() {
        clock = new FakeClock();
    }

    public static void advanceFakeClock(long time) {
        clock.advance(time);
    }

    public static void advanceFakeClockMS(int time) {
        clock.advance(time * 1000000L);
    }

    public static void setWindow(int win) {
        window = win;
    }

    public static void start(String ... ids) {
        for (String id : ids) {
            m.putIfAbsent(id, new TimingStat(window));
            m.get(id).start(clock.nanoTime());
        }
    }

    public static void stop(String ... ids) {
        for (String id : ids) {
            check(id);
            m.get(id).stop(clock.nanoTime());
        }
    }

    public static void reset(String ... ids) {
        for (String id : ids) {
            check(id);
            m.get(id).reset();
        }
    }

    public static TimingStat get(String id) {
        check(id);
        return m.get(id);
    }

    public static double getLast(String id) {
        check(id);
        return m.get(id).last();
    }

    private static void check(String id) {
        if (!m.containsKey(id))
            throw new NoSuchElementException("Timer for " + id + " does not exist");
    }

    public static void main(String[] args) {
        useFakeClock();

        System.out.println("Name  \tCount \tMinimum \tMaximum \tOverall Mean \tOverall Stdev \tMoving Average \tMoving Stdev \tUnit");

        start("Test1");
        advanceFakeClock(5000000L);
        stop("Test1");
        System.out.println("Test1\t" + get("Test1"));

        start("Test1");
        advanceFakeClock(6000000L);
        stop("Test1");
        System.out.println("Test1\t" + get("Test1"));

        start("Test1");
        advanceFakeClock(7000000L);
        stop("Test1");
        System.out.println("Test1\t" + get("Test1"));

        start("Test1");
        advanceFakeClock(3000000L);
        stop("Test1");
        System.out.println("Test1\t" + get("Test1"));

        start("Test1");
        advanceFakeClock(4000000L);
        stop("Test1");
        System.out.println("Test1\t" + get("Test1"));

        start("Test1", "Test2");
        advanceFakeClock(4000000L);
        stop("Test1");
        advanceFakeClock(4000000L);
        stop("Test2");
        System.out.println("Test1\t" + get("Test1"));
        System.out.println("Test2\t" + get("Test2"));

        reset("Test1");

        start("Test1", "Test2");
        advanceFakeClock(4000000L);
        stop("Test2", "Test1");
        System.out.println("Test1\t" + get("Test1"));
        System.out.println("Test2\t" + get("Test2"));

        start("Test1", "Test2");
        advanceFakeClock(100000000L);
        stop("Test2", "Test1");
        System.out.println("Test1\t" + get("Test1"));
        System.out.println("Test2\t" + get("Test2"));

        start("Test1", "Test2");
        advanceFakeClock(40000000L);
        stop("Test2", "Test1");
        System.out.println("Test1\t" + get("Test1"));
        System.out.println("Test2\t" + get("Test2"));
    }
}
