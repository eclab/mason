package sim.field;

import sim.engine.DSimState;
import sim.field.grid.*;

import sim.field.partitioning.Partition;
import sim.util.*;

/**
 * A abstract distributed grid 2d. It wraps all methods of distributed grid.
 * 
 * @author Carmine Spagnuolo
 */

public abstract class DAbstractGrid2D extends AbstractGrid2D {

	protected DSimState state;

	public DAbstractGrid2D(DSimState state) {
		int[] fieldSize = state.getPartition().getFieldSize();
		width = fieldSize[0];
		height = fieldSize[1];
		this.state = state;
	}

	protected void throwNotLocalException(NumberND p) {
		throw new RuntimeException("Point: " + p + ", is Not Local");
	}
	
	public abstract HaloGrid2D getHaloGrid();

	public IntRect2D getLocalBounds()  { return getHaloGrid().getLocalBounds(); }
	public IntRect2D getHaloBounds()  { return getHaloGrid().getHaloBounds(); }
	public boolean isLocal(Int2D p) { return getHaloGrid().inLocal(p); }
	public boolean isHalo(Int2D p) { return getHaloGrid().inLocalAndHalo(p); }
	
/*
	public abstract RemoteFulfillable get(Int2D p);

	public abstract RemoteFulfillable get(Int2D p, T t);

	public void add(Int2D p, T t) 

	public void remove(Int2D p, T t) 

	public boolean removeAllLocal(Int2D p) 

	public boolean removeMultiplyLocal(Int2D p, T t) 

	public boolean removeLocal(Int2D p, T t) 

	public void addLocal(Int2D p, T t) 

	public void setLocal(Int2D p, ArrayList<T> t) 

	public ArrayList<T> getLocal(Int2D p) 

	public boolean containsLocal(Int2D p, T t) 

	public void removeAll(Int2D p) 

	public void addAgent(Int2D p, T agent, double time, int ordering) 

	public void addAgent(Int2D p, T agent, double time, int ordering, double interval) 

	public void removeAgent(Int2D p, T agent) 

	public void removeAllAgentsAndObjects(Int2D p) 

	public void moveAgent(Int2D from, Int2D to, T agent) 

	public int getLocal(Int2D p) 
	public void setLocal(Int2D p, int t) 

	public void set(Int2D p, int val) 
*/



}
