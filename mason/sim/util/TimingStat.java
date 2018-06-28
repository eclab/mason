package sim.util;

import java.util.concurrent.TimeUnit;

public class TimingStat {

	int cap;
	long cnt, conv, ts;
	double avg, min, max, var, last;
	MovingAverage mav;
	TimeUnit u;

	public TimingStat(int cap) {
		this.cap = cap;
		this.setUnit(TimeUnit.MILLISECONDS);
		reset();
	}

	public void setUnit(TimeUnit u) {
		this.u = u;
		this.conv = TimeUnit.NANOSECONDS.convert(1L, u);
	}

	public void add(double val) {
		last = val;
		min = Math.min(min, val);
		max = Math.max(max, val);

		double avg_old = avg;
		avg += (val - avg) / ++cnt;
		var += (val - avg_old) * (val - avg);

		mav.next(val);
	}

	public void reset() {
		mav = new MovingAverage(cap);
		cnt = 0;
		min = Double.MAX_VALUE;
		max = 0;
		avg = 0;
		var = 0;
		ts = -1L;
	}

	public void start(long curr) {
		if (ts != -1L)
			throw new IllegalStateException("Timer is already started");
		ts = curr;
	}

	public void stop(long curr) {
		if (ts == -1L)
			throw new IllegalStateException("Timer is not started");
		add((double)(curr - ts));
		ts = -1L;
	}

	public double last() {
		return last / conv;
	}

	public long getCount() {
		return cnt;
	}

	public double getMean() {
		return avg / conv;
	}

	public double getMin() {
		return min / conv;
	}

	public double getMax() {
		return max / conv;
	}

	public double getStdev() {
		if (cnt > 1)
			return Math.sqrt(var / (cnt - 1)) / conv;
		return 0;
	}

	public double getMovingAverage() {
		return mav.average() / conv;
	}

	public double getMovingStdev() {
		return mav.stdev() / conv;
	}

	public String toString() {
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