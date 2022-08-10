/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
        
package sim.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * TIMING is used to time simulation steps in order to compute model load
 * for purposes of load balancing by the partition scheme.
 */
 
public class Timing 
    {
    public static final String LB_RUNTIME = "_MASON_LOAD_BALANCING_RUNTIME";
    public static final String LB_OVERHEAD = "_MASON_LOAD_BALANCING_OVERHEAD";
    public static final String MPI_SYNC_OVERHEAD = "_MASON_MPI_SYNC_OVERHEAD";

    private static int window = 100;
    // hashmap containing the different timer for each operation
    private static HashMap<String, TimingStat> timers = new HashMap<String, TimingStat>();

    public static void setWindow(int win) 
        {
        window = win;
        }

    /**
     * for each operation
     * create the timer if does not exist and put it in the hasmap
     * then start the timer
     **/
    public static void start(String... ids) 
        {
        for (String id : ids) 
            {
            timers.putIfAbsent(id, new TimingStat(window));
            timers.get(id).start(System.nanoTime());
            }
        }

    /**
     * for each operation check if it exist in the timers hashmap
     * then stop the timer
     **/
    public static void stop(String... ids) 
        {
        for (String id : ids) 
            {
            if (!timers.containsKey(id))
                throw new NoSuchElementException("Timer for " + id + " does not exist");
            timers.get(id).stop(System.nanoTime());
            }
        }

    /**
     * check if the timer exist in the timers hashmap
     * then get the total time
     */
    public static TimingStat get(String id) 
        {
        if (!timers.containsKey(id))
            throw new NoSuchElementException("Timer for " + id + " does not exist");
        return timers.get(id);
        }

    /**
     *
     * Internal Class used by Timing.java
     */
    public static class TimingStat 
        {
        int window;
        long count, conversion, timeStart;
        double avg, min, max, var, last;
        MovingAverage mav;
        TimeUnit unit;

        TimingStat(int window) 
            {
            this.window = window;
            this.unit = TimeUnit.MILLISECONDS;
            this.conversion = TimeUnit.NANOSECONDS.convert(1L, TimeUnit.MILLISECONDS);
            this.mav = new MovingAverage(this.window);
            this.min = Double.MAX_VALUE;
            this.timeStart = -1L;
            }

        void add(double val) 
            {
            last = val;
            min = Math.min(min, val);
            max = Math.max(max, val);

            double avgOld = avg;
            avg += (val - avg) / ++count;
            var += (val - avgOld) * (val - avg);

            mav.next(val);
            }

        void start(long curr) 
            {
            if (timeStart != -1L)
                throw new IllegalStateException("Timer is already started");
            timeStart = curr;
            }

        void stop(long current) 
            {
            add((double) (current - timeStart));
            timeStart = -1L;
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
                unit);
            }

        public long getCount() 
            {
            return count;
            }

        public double getMean() 
            {
            return avg / conversion;
            }

        public double getMin() 
            {
            return min / conversion;
            }

        public double getMax() 
            {
            return max / conversion;
            }

        public double getStdev() 
            {
            if (count > 1)
                return Math.sqrt(var / (count - 1)) / conversion;
            return 0;
            }

        public double getMovingAverage() 
            {
            return mav.average() / conversion;
            }

        public double getMovingStdev() 
            {
            return mav.stdev() / conversion;
            }

        }

    /**
     * Internal utility class used to calculate moving average. Used by TimingStat.
     */
    static class MovingAverage 
        {
        double average;
        double[] queue;
        int count, startTime;
        final int window;

        public MovingAverage(int window) 
            {
            this.queue = new double[window];
            this.window = window;
            }

        public double next(double value) 
            {
            if (count < window) 
                {
                queue[startTime + count] = value;
                average += (value - average) / ++count;
                } 
            else 
                {
                average += (value - queue[startTime]) / window;
                queue[startTime] = value;
                startTime = (startTime + 1) % window;
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
        }
    }
