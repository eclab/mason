package sim.app.geo.masoncsc.util;

import java.util.LinkedList;

public class RollingAverage
{
	private LinkedList<Double> queue = new LinkedList<Double>();
	private double total = 0;
	private int count = 0;
	
	private int windowSize = 1000;
	public int getWindowSize() { return windowSize; }
	public void setWindowSize(int val) {
		if (val < windowSize) {
		    while (queue.size() > val) {
		    	total -= queue.removeLast();
		    } 
		    if (count > val)
		    	count = val;
		}
		windowSize = val;
	}
	
	public RollingAverage() {
	}
	
	public RollingAverage(int windowSize) {
		this.windowSize = windowSize;		
	}
	
	public void clear() {
		queue.clear();
		total = 0;
		count = 0;
	}	

	public void add(double x) {
		total += x;
	    queue.addFirst(x);
	    if (queue.size() > windowSize) {
	    	total -= queue.removeLast();
	    } 
	    else {
	    	count++;
	    }
	}
	
	public double getAverage() {
		return total / count;
	}
	
	public double[] getData() {
		double[] data = new double[queue.size()];
		
		for (int i = 0; i < queue.size(); i++)
			data[i] = queue.get(i);

		return data;
	}

}
