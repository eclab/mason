package sim.field;

import sim.engine.DSimState;
import sim.field.grid.AbstractGrid2D;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.Number2D;

/**
 * A abstract distributed Grid2D. It wraps all methods of distributed grid.
 * 
 * @author Carmine Spagnuolo
 */

public abstract class DAbstractGrid2D extends AbstractGrid2D
{

	private static final long serialVersionUID = 1L;

	protected DSimState state;

	public DAbstractGrid2D(DSimState state)
	{
		width = state.getPartition().getWorldWidth();
		height = state.getPartition().getWorldHeight();
		this.state = state;
	}

	protected void throwNotLocalException(Number2D p)
	{
		throw new RuntimeException("Point: " + p + ", is Not Local");
	}
	
	public abstract HaloGrid2D getHaloGrid();
	public IntRect2D getLocalBounds()
		{
		return getHaloGrid().getLocalBounds();
		}
	
	public IntRect2D getHaloBounds()
		{
		return getHaloGrid().getHaloBounds();
		}
	
	public boolean isLocal(Int2D p)
		{
		return getHaloGrid().inLocal(p);
		}
	
	public boolean isHalo(Int2D p)
		{
		return getHaloGrid().inHalo(p);
		}
	
	public boolean isHaloToroidal(Number2D p)
		{
		return getHaloGrid().inHaloToroidal(p);
		}
	
	public Double2D toHaloToroidal(Double2D p)
		{
		return getHaloGrid().toHaloToroidal(p);
		}
	
	public Int2D toHaloToroidal(Int2D p)
		{
		return getHaloGrid().toHaloToroidal(p);
		}
	
	/** Returns true if the square centered at x, y and going out to distance
		is entirely within the halo toroidal region. */
	public boolean isHaloToroidal(double x, double y, double distance) 
		{
		HaloGrid2D grid = getHaloGrid();
		return  grid.inHaloToroidal(x + distance, y + distance) &&
				grid.inHaloToroidal(x + distance, y - distance) &&
				grid.inHaloToroidal(x - distance, y + distance) &&
				grid.inHaloToroidal(x - distance, y - distance);
		}
}
