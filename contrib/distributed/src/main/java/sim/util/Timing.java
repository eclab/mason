package sim.util;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

/**
 * Internal distributed MASON class to time simulation steps
 *
 */
public class Timing 
	{
	private static final long serialVersionUID = 1L;

	public static final String LB_RUNTIME = "_MASON_LOAD_BALANCING_RUNTIME";
	public static final String LB_OVERHEAD = "_MASON_LOAD_BALANCING_OVERHEAD";
	public static final String MPI_SYNC_OVERHEAD = "_MASON_MPI_SYNC_OVERHEAD";

	private static int window = 100;
	private static HashMap<String, TimingStat> m = new HashMap<String, TimingStat>();
	private static NanoClock clock = new NanoClock()
	{
		public long nanoTime()
		{
			return System.nanoTime();
		}

		public void advance(long val)
		{
			throw new UnsupportedOperationException("Cannot set nano time for real clock");
		}
	};

	private static class FakeClock implements NanoClock
	{
		public long val = 0;

		public long nanoTime()
		{
			return val;
		}

		public void advance(long val)
		{
			this.val += val;
		}
	}

	private interface NanoClock
	{
		long nanoTime();

		void advance(long val);
	}

	public static void useFakeClock()
	{
		clock = new FakeClock();
	}

	public static void advanceFakeClock(long time)
	{
		clock.advance(time);
	}

	public static void advanceFakeClockMS(int time)
	{
		clock.advance(time * 1000000L);
	}

	public static void setWindow(int win)
	{
		window = win;
	}

	public static void start(String... ids)
	{
		for (String id : ids)
		{
			m.putIfAbsent(id, new TimingStat(window));
			m.get(id).start(clock.nanoTime());
		}
	}

	public static void stop(String... ids)
	{
		for (String id : ids)
		{
			check(id);
			m.get(id).stop(clock.nanoTime());
		}
	}

	public static void reset(String... ids)
	{
		for (String id : ids)
		{
			check(id);
			m.get(id).reset();
		}
	}

	public static TimingStat get(String id)
	{
		check(id);
		return m.get(id);
	}

	public static double getLast(String id)
	{
		check(id);
		return m.get(id).last();
	}

	private static void check(String id)
	{
		if (!m.containsKey(id))
			throw new NoSuchElementException("Timer for " + id + " does not exist");
	}

	public static void main(String[] args)
	{
		useFakeClock();

		System.out.println(
				"Name  \tCount \tMinimum \tMaximum \tOverall Mean \tOverall Stdev \tMoving Average \tMoving Stdev \tUnit");

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
	
	

/**
 * Internal utility class used to calculate moving average. Used by TimingStat.
 */
static class MovingAverage
{
	double average;
	double var;
	double[] queue;
	int count, st;
	final int capacity;

	public MovingAverage(int capacity)
	{
		this.queue = new double[capacity];
		this.capacity = capacity;
	}

	public double next(double val)
	{
		if (count < capacity)
		{
			queue[st + count] = val;
			average += (val - average) / ++count;
		}
		else
		{
			average += (val - queue[st]) / capacity;
			queue[st] = val;
			st = (st + 1) % capacity;
		}

		return average;
	}

	public double average()
	{
		return average;
	}

	public double stdev()
	{
		if (count < 2)
			return 0;
		return Math.sqrt(Arrays.stream(queue)
				.limit(count)
				.map(x -> (x - average) * (x - average))
				.sum() / (count - 1));
	}

	public static void main(String args[])
	{
		MovingAverage a = new MovingAverage(4);
		System.out.printf("1 \t Want %g \t Got %g stdev %g\n", 10.0, a.next(10), a.stdev());
		System.out.printf("2 \t Want %g \t Got %g stdev %g\n", 15.0, a.next(20), a.stdev());
		System.out.printf("3 \t Want %g \t Got %g stdev %g\n", 15.0, a.next(15), a.stdev());
		System.out.printf("4 \t Want %g \t Got %g stdev %g\n", 12.5, a.next(5), a.stdev());
		System.out.printf("5 \t Want %g \t Got %g stdev %g\n", 10.0, a.next(0), a.stdev());
		System.out.printf("6 \t Want %g \t Got %g stdev %g\n", 12.5, a.next(30), a.stdev());
		System.out.printf("7 \t Want %g \t Got %g stdev %g\n", 11.25, a.next(10), a.stdev());
		System.out.printf("8 \t Want %g \t Got %g stdev %g\n", 15.0, a.next(20), a.stdev());
		System.out.printf("9 \t Want %g \t Got %g stdev %g\n", 27.5, a.next(50), a.stdev());
		System.out.printf("10 \t Want %g \t Got %g stdev %g\n", 22.5, a.next(10), a.stdev());
		System.out.printf("11 \t Want %g \t Got %g stdev %g\n", 25.0, a.next(20), a.stdev());
		System.out.printf("12 \t Want %g \t Got %g stdev %g\n", 23.75, a.next(15), a.stdev());
		System.out.printf("13 \t Want %g \t Got %g stdev %g\n", 12.5, a.next(5), a.stdev());
	}
}

	

/**
 *
 *Internal Class used by Timing.java
 */
public static class TimingStat
{

    int cap;
    long cnt, conv, ts;
    double avg, min, max, var, last;
    MovingAverage mav;
    TimeUnit u;

     TimingStat(int cap)
     {
        this.cap = cap;
        this.setUnit(TimeUnit.MILLISECONDS);
        reset();
    }

     void setUnit(TimeUnit u)
     {
        this.u = u;
        this.conv = TimeUnit.NANOSECONDS.convert(1L, u);
    }

     void add(double val)
     {
        last = val;
        min = Math.min(min, val);
        max = Math.max(max, val);

        double avg_old = avg;
        avg += (val - avg) / ++cnt;
        var += (val - avg_old) * (val - avg);

        mav.next(val);
    }

     void reset()
     {
        mav = new MovingAverage(cap);
        cnt = 0;
        min = Double.MAX_VALUE;
        max = 0;
        avg = 0;
        var = 0;
        ts = -1L;
    }

     void start(long curr)
     {
        if (ts != -1L)
            throw new IllegalStateException("Timer is already started");
        ts = curr;
    }

    /**
     * @deprecated
     * hatfolk (13 Feb 2019):
     * This isn't actually deprecated but more of a
     * "BOO! LOOK AT ME! BEWARE! SPOOKY!" kind of warning.
     *
     * In order to get the distributed applications to work, I ended up
     * commenting out the timestamp check below and the thrown exception.
     * We, as of Feb. 2019, hereby say that this isn't a bad idea /yet/.
     * But if you, traveler of the future, find it to be problematic,
     * please change the code as needed. 
     *
     * The fundamental question is "Is it problematic to stop a stopped timer?"
     *
     * Our answer seems to be that it probably isn't and the timer is being used
     * as part of a lamport schema. That and doing this currently works.
     *
     */
    @Deprecated
     void stop(long curr)
    {
        /*if (ts == -1L)
            throw new IllegalStateException("Timer is not started"); */
        add((double)(curr - ts));
        ts = -1L;
    }

    public double last()
    {
        return last / conv;
    }

    public long getCount()
    {
        return cnt;
    }

    public double getMean()
    {
        return avg / conv;
    }

    public double getMin()
    {
        return min / conv;
    }

    public double getMax()
    {
        return max / conv;
    }

    public double getStdev()
    {
        if (cnt > 1)
            return Math.sqrt(var / (cnt - 1)) / conv;
        return 0;
    }

    public double getMovingAverage()
    {
        return mav.average() / conv;
    }

    public double getMovingStdev()
    {
        return mav.stdev() / conv;
    }

    public String toString()
    {
        return String.format("%d\t%f\t%f\t%f\t%f\t%f\t%f\t%s",
                             getCount(),
                             getMin(),
                             getMax(),
                             getMean(),
                             getStdev(),
                             getMovingAverage(),
                             getMovingStdev(),
                             u
                             );
    }
    
    
}
	
}