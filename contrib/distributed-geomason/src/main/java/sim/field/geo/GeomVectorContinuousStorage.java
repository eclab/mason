package sim.field.geo;

import java.io.Serializable;


import java.util.ArrayList;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import sim.app.geo.dcampusworld.DAgent;
import sim.engine.DObject;
import sim.engine.DSimState;
import sim.field.storage.ContinuousStorage;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.MPIParam;
import sim.util.Number2D;
import sim.util.geo.DGeomObject;
import sim.util.geo.DGeomSteppable;
import sim.util.geo.MasonGeometry;

public class GeomVectorContinuousStorage<T extends DGeomObject> extends ContinuousStorage<T>{
	private static final long serialVersionUID = 1L;

	// note that this is the DISCRETIZED WIDTH (and the DISCRETIZED HEIGHT is height in GridStorage)
	
	//This is private, as we don't want to call these methods directly.  This is because we want the storage to update with this one and vice versa
	private GeomVectorField geomVectorField;
    public Envelope globalEnvelope;

	public GeomVectorContinuousStorage(final IntRect2D shape, int discretization)
	{
		super(shape, discretization);
		this.geomVectorField = new GeomVectorField(shape.getWidth(), shape.getHeight());
		this.geomVectorField.clear();
		
		globalEnvelope = null;
		

	}

	//If we include static objects, we may need to extend each envelope!
	/*
	public void expandEnvelope(Envelope e) {
		this.geomVectorField.getMBR().expandToInclude(e);
	}
	*/
	

	//Override to also add to GeomVectorField

	// Put the object to the given point
	public void addObject(Number2D p, T obj)
	{
		//geomvecAndContinuousStorageMatch("addObject before");
		
		
		
		
		Double2D p_double = buildDouble2D(p);


		
		final Double2D old = locations.put(obj.ID(), p_double);

		
		if (old != null) {
			
			T retrieved_obj = getCell(old).get(obj.ID());
			if (retrieved_obj != null) {
				MasonGeometry old_m = retrieved_obj.getMasonGeometry();
				this.geomVectorField.removeGeometry(old_m);
			}


			
			getCell(old).remove(obj.ID());


		}
		getCell(p_double).put(obj.ID(), obj);
		
		


		MasonGeometry m = obj.getMasonGeometry();
		
		
	

		
    	this.geomVectorField.addGeometry(m);


		
	}



	public boolean removeObject(Number2D p, long id)
	{
		// p is ignored.

		Double2D loc = locations.remove(id);
		if (loc == null)
			return false;
		T obj = getCell(loc).remove(id);
		

		
		this.geomVectorField.removeGeometry(obj.getMasonGeometry());


		
		return true;
	}
	
	


	// Remove all the objects at the given point
	public void clear(Number2D p)
	{
		Double2D p_double = buildDouble2D(p);
		HashMap<Long, T> cell = getCell(p_double);

		for (Long key : cell.keySet())
			if (locations.get(key).equals(p_double)) {
				T obj = cell.remove(key);
				


				this.geomVectorField.removeGeometry(obj.getMasonGeometry());

	


			}
		
	}

	public void clear()
	{
		
        super.clear();
        
        
        //Need to check for null because clear is called in constructor (super) before geomVectorField is initialized
        if (this.geomVectorField != null) {
        	


        	this.geomVectorField.clear();
        	


        }
        
        
        
	}


	/// METHODS FOR PACKING AND UNPACKING

	void removeObject(long id)
	{
		T obj = getCell(locations.remove(id)).remove(id);
		
		

		this.geomVectorField.removeGeometry(obj.getMasonGeometry());
		

		
		

		

	}

	// Remove all the objects inside the given rectangle
	void removeObjects(final IntRect2D r)
	{
		

		for (T obj : getObjects(r))
		{
			removeObject(obj.ID());
			
	

			
			


		}
		

	}
	
	public GeomVectorField getGeomVectorField() {
		
		return geomVectorField;
	}
	
	public void setGeomVectorField(GeomVectorField tempGeomVectorField) {
		// TODO Auto-generated method stub
		this.geomVectorField = tempGeomVectorField;
	}
	
	
	
	public Serializable pack(final MPIParam mp)
	{
		//geomvecAndContinuousStorageMatch("pack beginning");


		final ArrayList<ArrayList<Serializable>> ret = new ArrayList<>();

		for (final IntRect2D rect : mp.rects)
		{
			final ArrayList<Serializable> objs = new ArrayList<>();
			for (final T obj : getObjects(rect.add(shape.ul())))
			{
				objs.add(obj);
				// Append the object's location relative to the rectangle
				objs.add(locations.get(obj.ID()).subtract(shape.ul()).subtract(rect.ul()));
			}
			ret.add(objs);
		}
		


		return ret;
		
	}

	public void unpack(final MPIParam mp, final Serializable buf)
	{
	
        

		final ArrayList<ArrayList<Serializable>> objs = (ArrayList<ArrayList<Serializable>>) buf;


		

       
        
        
        
		for (int k = 0; k < mp.rects.size(); k++) {
			
			
			removeObjects(mp.rects.get(k).add(shape.ul()));
			


			
			IntRect2D aaa = mp.rects.get(k).add(shape.ul());
			
			for (int i = 0; i < objs.get(k).size(); i += 2) {
				
				Number2D p = ((Double2D) objs.get(k).get(i + 1)).add(mp.rects.get(k).ul()).add(shape.ul());
				T t = (T) objs.get(k).get(i);
				
				
				addObject(p, t);


				
			}

			
		}
		


		
       


	}


	

	




}
