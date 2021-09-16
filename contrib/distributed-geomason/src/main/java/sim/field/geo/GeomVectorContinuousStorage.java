package sim.field.geo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import sim.field.storage.ContinuousStorage;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.MPIParam;
import sim.util.Number2D;
import sim.util.geo.MasonGeometryWrapper;

public class GeomVectorContinuousStorage extends ContinuousStorage<MasonGeometryWrapper> {
	private static final long serialVersionUID = 1L;

	// note that this is the DISCRETIZED WIDTH (and the DISCRETIZED HEIGHT is height in GridStorage)
	
	//This is private, as we don't want to call these methods directly.  This is because we want the storage to update with this one and vice versa
	private GeomVectorField geomVectorField;


	public GeomVectorContinuousStorage(final IntRect2D shape, int discretization)
	{
		super(shape, discretization);
		this.geomVectorField = new GeomVectorField(shape.getWidth(), shape.getHeight());
		clear();
	}


	//Override to also add to GeomVectorField

	// Put the object to the given point
	public void addObject(Number2D p, MasonGeometryWrapper obj)
	{
		Double2D p_double = buildDouble2D(p);
//		System.out.println("add Object: " + m + "; " + obj);
		final Double2D old = locations.put(obj.ID(), p_double);

		if (old != null)
			getCell(old).remove(obj.ID());
		getCell(p_double).put(obj.ID(), obj);
		
		this.geomVectorField.addGeometry(obj.getMasonGeometry());
	}



	public boolean removeObject(Number2D p, long id)
	{
		// p is ignored.

		Double2D loc = locations.remove(id);
		if (loc == null)
			return false;
		MasonGeometryWrapper mgw = getCell(loc).remove(id);
		this.geomVectorField.removeGeometry(mgw.getMasonGeometry());

		
		return true;
	}


	// Remove all the objects at the given point
	public void clear(Number2D p)
	{
		Double2D p_double = buildDouble2D(p);
		HashMap<Long, MasonGeometryWrapper> cell = getCell(p_double);

		for (Long key : cell.keySet())
			if (locations.get(key).equals(p_double)) {
				MasonGeometryWrapper mgw = cell.remove(key);
				this.geomVectorField.removeGeometry(mgw.getMasonGeometry());

			}
	}

	@SuppressWarnings("unchecked")
	public void clear()
	{
		super.clear();
		//clear geomVectorField
		this.geomVectorField.clear();
				
		
	}

	/// METHODS FOR PACKING AND UNPACKING

	void removeObject(long id)
	{
		MasonGeometryWrapper mgw = getCell(locations.remove(id)).remove(id);
		this.geomVectorField.removeGeometry(mgw.getMasonGeometry());

	}

	// Remove all the objects inside the given rectangle
	void removeObjects(final IntRect2D r)
	{
		for (MasonGeometryWrapper obj : getObjects(r))
		{
			removeObject(obj.ID());
			this.geomVectorField.removeGeometry(obj.getMasonGeometry());

		}
	}







}
