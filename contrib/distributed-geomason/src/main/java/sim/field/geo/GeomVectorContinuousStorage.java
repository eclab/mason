package sim.field.geo;

import java.io.Serializable;


import java.util.ArrayList;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Coordinate;

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


	public GeomVectorContinuousStorage(final IntRect2D shape, int discretization)
	{
		super(shape, discretization);
		this.geomVectorField = new GeomVectorField(shape.getWidth(), shape.getHeight());
		this.geomVectorField.clear();
	}


	//Override to also add to GeomVectorField

	// Put the object to the given point
	public void addObject(Number2D p, T obj)
	{
		Double2D p_double = buildDouble2D(p);
//		System.out.println("add Object: " + m + "; " + obj);
		final Double2D old = locations.put(obj.ID(), p_double);

		if (old != null)
			getCell(old).remove(obj.ID());
		getCell(p_double).put(obj.ID(), obj);
		
		int oldsize = this.getGeomVectorField().getGeometries().size(); //remove this
		

		MasonGeometry m = obj.getMasonGeometry();
		
		
		int newsize = this.getGeomVectorField().getGeometries().size(); //remove this
		
        String s = "";
        
        for (Object o : this.getGeomVectorField().getGeometries()) {
        	s = s+" "+o;
        }
        
        System.out.println("before "+s);
		
		

		this.geomVectorField.addGeometry(m);
		
		int newsize2 = this.getGeomVectorField().getGeometries().size(); //remove this
        
        if (newsize2 > newsize + 1) {
        	System.out.println("added multiple incorrectly");
        	System.exit(-1);
        	
        }
        
        if (newsize2 > oldsize+1) {
        	System.out.println("added multiple incorrectly2");
        	System.exit(-1);


        }
        
        System.out.println("adding "+obj+" at "+p);
        
        System.out.println("----");
        
        s = "";
        
        for (Object o : this.getGeomVectorField().getGeometries()) {
        	s = s+" "+o;
        }
        
        System.out.println("after "+s);

		
	}



	public boolean removeObject(Number2D p, long id)
	{
		// p is ignored.

		Double2D loc = locations.remove(id);
		if (loc == null)
			return false;
		T obj = getCell(loc).remove(id);
		
		int oldsize = this.getGeomVectorField().getGeometries().size(); //remove this

		
		this.geomVectorField.removeGeometry(obj.getMasonGeometry());

		int newsize = this.getGeomVectorField().getGeometries().size(); //remove this
		
		/*
        if (oldsize == newsize) {
        	System.out.println("removeObj 119 not removing correctly");
        	System.exit(-1);
        }
        */
		
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
				
				int oldsize = this.getGeomVectorField().getGeometries().size(); //remove this

				this.geomVectorField.removeGeometry(obj.getMasonGeometry());

				int newsize = this.getGeomVectorField().getGeometries().size(); //remove this
				
				/*
		        if (oldsize == newsize) {
		        	System.out.println("clear 130 not removing correctly");
		        	System.exit(-1);
		        }
		        */
				

			}
	}



	/// METHODS FOR PACKING AND UNPACKING

	void removeObject(long id)
	{
		T obj = getCell(locations.remove(id)).remove(id);
		
		
		int oldsize = this.getGeomVectorField().getGeometries().size(); //remove this

		this.geomVectorField.removeGeometry(obj.getMasonGeometry());
		
		int newsize = this.getGeomVectorField().getGeometries().size(); //remove this
		
		
		/*
		System.out.println("before "+oldsize+" "+" after: "+newsize);
		//System.exit(-1);
		
		
		if (newsize == oldsize) {
			System.out.println("SAME: before "+oldsize+" "+" after: "+newsize);
			System.exit(-1);


		}
		*/
		

		

	}

	// Remove all the objects inside the given rectangle
	void removeObjects(final IntRect2D r)
	{
		

		for (T obj : getObjects(r))
		{
			removeObject(obj.ID());
			
			
			//already done in removeObject
			//this.geomVectorField.removeGeometry(obj.getMasonGeometry());

			
			


		}
		

	}
	
	public GeomVectorField getGeomVectorField() {
		
		return geomVectorField;
	}
	
	
	
	public Serializable pack(final MPIParam mp)
	{
		
		final ArrayList<ArrayList<Serializable>> ret = new ArrayList<>();

		for (final IntRect2D rect : mp.rects)
		{
			//System.out.println("pack : "+rect);
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
		
		//return null;
	}

	public void unpack(final MPIParam mp, final Serializable buf)
	{
		
		
		int oldsize = this.getGeomVectorField().getGeometries().size(); //remove this
        int oldlocsize = this.locations.size();
        int halo_objs = 0;
        int new_halo_objs = 0;
        
        int newsize=0;
        int newlocsize = 0;
        int newsize2 = 0;
        int newlocsize2 = 0;
        int addObjCalls = 0;

		final ArrayList<ArrayList<Serializable>> objs = (ArrayList<ArrayList<Serializable>>) buf;

		// Remove any objects that are in the unpack area (overwrite the area)
		// shift the rect with local coordinates back to global coordinates
		for (final IntRect2D rect : mp.rects) {
			//System.out.println("unpack : "+rect);
			halo_objs = halo_objs + getObjects(rect.add(shape.ul())).size();
			removeObjects(rect.add(shape.ul()));
			//System.out.println("removing "+rect.add(shape.ul()));
		}
		
		newsize = this.getGeomVectorField().getGeometries().size(); //remove this
        newlocsize = this.locations.size();

        
        //check if now emptpy!------------------------------------------------------------------
		for (int k = 0; k < mp.rects.size(); k++) {
			IntRect2D rect = mp.rects.get(k);

			for (final T obj : getObjects(rect.add(shape.ul())))
			{
               System.out.println("obj still here !");
               System.exit(-1);
			}
		
		
		
			for (int i = 0; i < objs.get(k).size(); i += 2) {
				
				Number2D p = ((Double2D) objs.get(k).get(i + 1)).add(mp.rects.get(k).ul()).add(shape.ul());
				
				if (!rect.contains(p)) {
					System.out.println(p+" not in "+rect);
					System.exit(-1);
				}
			}
		}
		//-------------------------------------------------------------------------------------------		
				
        
        
        
		for (int k = 0; k < mp.rects.size(); k++) {
			for (int i = 0; i < objs.get(k).size(); i += 2) {
				
				Number2D p = ((Double2D) objs.get(k).get(i + 1)).add(mp.rects.get(k).ul()).add(shape.ul());
				T t = (T) objs.get(k).get(i);
				System.out.println(t);
				addObject(p, t);
				addObjCalls = addObjCalls + 1;
			}
			
			new_halo_objs = new_halo_objs + getObjects(mp.rects.get(k).add(shape.ul())).size();
			
		}
		
		
		newsize2 = this.getGeomVectorField().getGeometries().size(); //remove this
        newlocsize2 = this.locations.size();

		System.out.println("old size "+oldsize+" new size1 "+newsize+" new size2 "+newsize2+ "addObjCalls " +addObjCalls);
		
		if (addObjCalls > 0) {
	       // System.exit(-1);

		}
		
       


	}




}
