package masoncsc.util;

import java.util.ArrayList;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DoubleGrid2D;
import sim.util.MutableDouble2D;

public class TestModel extends SimState
{
	private static final long serialVersionUID = 1L;
	
	public int width = 100;
	public int getWidth() {	return width; }
	public void setWidth(int width) { this.width = width; }

	public int height = 100;
	public int getHeight() { return height; }
	public void setHeight(int height) {	this.height = height; }
	

	public DoubleGrid2D grid;
	
	public TestModel(long seed) {
		super(seed);
	}

	@Override
	public void start() {
		super.start();
		
		grid = new DoubleGrid2D(width, height);

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				grid.set(i, j, -1 + (j / (double)height)*2.0);
			}
		}
		
		schedule.scheduleRepeating(new Steppable() {
			@Override
			public void step(SimState state) {
				// do nothing
			}
		});
	}
	
	public static void setVar(MutableDouble2D point) {
//		point.x = 10;
//		point.y = 10;
		point = new MutableDouble2D(10, 10);
	}

	public static void main(String[] args) {
		
//		MutableDouble2D point = new MutableDouble2D(5, 5);
//		setVar(point);
//		System.out.println("point:" + point);
		
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		list.add(1);
		list.add(2);
		list.add(3);
		
		System.out.println(list.toString());
		
		ArrayList<Integer> list2 = new ArrayList<Integer>(list);
		
		list2.remove(0);

		System.out.println(list2.toString());
		System.out.println(list.toString());
	
	}
	
	

}
