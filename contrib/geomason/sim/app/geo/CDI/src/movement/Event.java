package CDI.src.movement;


import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import sim.field.grid.DoubleGrid2D;

/**
 * 
 * @author Ermo Wei
 *	This class represent a event that happen during a time
 */



public class Event {
	enum Type
	{
		ADD,
		MULTIPLY
	};
	
	
	private int start;
	private int end;
	private Type type;
	private double value;
	private ArrayList<Point> cells;
	private DoubleGrid2D grid;
	
	public Event(int start, int end, ArrayList<Point> polygon, Type type, double value, DoubleGrid2D grid)
	{
		// note, start should always less than end
		this.start = start;
		this.end = end;
		this.type = type;
		this.value = value;
		this.grid = grid;
		this.cells = new ArrayList<Point>();
		getCells(polygon);
	}
	
	// get a list of cell in this polygon
	private void getCells(List<Point> polygon) {
		// let's assume that all polygon are square
		// this need to be replaced
		int minX = Integer.MAX_VALUE,minY = Integer.MAX_VALUE,
				maxX = Integer.MIN_VALUE,maxY = Integer.MIN_VALUE;
		for(Point p:polygon)
		{
			if(p.x < minX)
			{
				minX = p.x;
			}
			if(p.x > maxX)
			{
				maxX = p.x;
			}
			if(p.y < minY)
			{
				minY = p.y;
			}
			if(p.y > maxY)
			{
				maxY = p.y;
			}
		}
		
		for(int i = minX;i<=maxX;++i)
		{
			for(int j = minY;j<=maxY;++j)
			{
				this.cells.add(new Point(i,j));
			}
		}
		
	}

	public void startEvent()
	{
		for(Point point:cells)
		{
			if(this.type==Type.ADD)
			{
				grid.field[point.x][point.y] += value;
			}
			else if(this.type==Type.MULTIPLY)
			{
				grid.field[point.x][point.y] *= value;
			}
		}
	}
	
	public void endEvent()
	{
		for(Point point:cells)
		{
			if(this.type==Type.ADD)
			{
				grid.field[point.x][point.y] -= value;
			}
			else if(this.type==Type.MULTIPLY)
			{
				grid.field[point.x][point.y] /= value;
			}
		}
	}

	public void doEvent(int time, WorldAgent world) {
		if(time==this.start)
		{
			this.startEvent();
			world.updateRouletteWheel();
			world.updateCumulativeProbs();
		}
		else if(time==this.end)
		{
			this.endEvent();
			world.updateRouletteWheel();
			world.updateCumulativeProbs();
		}
	}
	
}

