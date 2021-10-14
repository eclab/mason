package sim.display;

import sim.field.continuous.*;
import sim.field.geo.DGeomVectorField;
import sim.field.geo.GeomVectorContinuousStorage;
import sim.field.geo.GeomVectorField;
import sim.app.geo.dcampusworld.display.CampusWorldProxy;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import java.util.Map.Entry;

import sim.util.*;
import sim.util.geo.MasonGeometry;

@SuppressWarnings("rawtypes")
public class GeomVectorContinuousStorageProxy extends GeomVectorField implements UpdatableProxy
{
	private static final long serialVersionUID = 1L;

    public GeomVectorContinuousStorageProxy()
    {
        super();
    }
    
    public GeomVectorContinuousStorageProxy(int w, int h)
    {
    	super(w, h);
    }

	@Override
	public void update(SimStateProxy stateProxy, int proxyIndex, int[] quad_tree_partitions)
			throws RemoteException, NotBoundException
	{		
		int halo_size = 0;

		IntRect2D[] rect_list = new IntRect2D[quad_tree_partitions.length];
		for (int p_ind = 0; p_ind < quad_tree_partitions.length; p_ind++)
		{
			int p = quad_tree_partitions[p_ind];
			VisualizationProcessor vp1 = stateProxy.visualizationProcessor(p);
			halo_size = vp1.getAOI();

			rect_list[p_ind] = vp1.getStorageBounds();
		    
		}
		
		IntRect2D fullBounds = IntRect2D.getBoundingRect(rect_list);
		Int2D new_ul = fullBounds.ul().add(halo_size,halo_size); //remove halo
		Int2D new_br = fullBounds.br().add(-1 * halo_size, -1 * halo_size); //remove halo
		fullBounds = new IntRect2D(new_ul, new_br);
		
		Int2D fullBounds_offset = fullBounds.ul();
		
		int width = fullBounds.br().x - fullBounds.ul().x;
		int height = fullBounds.br().y - fullBounds.ul().y;
		

		//if (width != this.width || height != this.height)
		reshape(width, height);
		
		System.out.println("objs : "+this.getGeometries().size());
		
		//for (int p = 0; p < stateProxy.numProcessors; p++) {
		for (int p : quad_tree_partitions)
		{

			VisualizationProcessor vp1 = stateProxy.visualizationProcessor(p);
			//int halo_size = vp1.getAOI();
		    IntRect2D partBound = vp1.getStorageBounds();
		    
		    System.out.println(partBound);
		    
		    
			//remove halo bounds using bounds.ul offset, assumption is offset from 0,0 is halo size
		    
            int partition_width_low_ind = partBound.ul().getX()+halo_size;  //partition bounds, subtract to remove halo
            int partition_width_high_ind = partBound.br().getX()-halo_size;  //partition bounds, add to remove halo
            int partition_height_low_ind =  partBound.ul().getY()+halo_size;  //partition bounds
            int partition_height_high_ind =  partBound.br().getY()-halo_size;   //partition bounds 
            
            
            GeomVectorContinuousStorage storage = (GeomVectorContinuousStorage)(stateProxy.storage(proxyIndex));
            
            System.out.println(p+" : "+storage.getGeomVectorField().getGeometries().size());

            for (Object a : storage.getGeomVectorField().getGeometries()) {
            	
            	//System.out.println(p);

            	
            	
            	this.addGeometry((MasonGeometry)a);
            	
            }
            
            System.out.println(p+" : "+"hashmap size :"+storage.locations.keySet().size()); 
            	
            	
            
            
	
            
		}
		
		System.out.println("objs 2 : "+this.getGeometries().size());

		
		
		
		
		
	}
		
	public void reshape(int w, int h)
	{
		   setFieldWidth(w);
		   setFieldHeight(h);
		   this.clear();
	}

//	public void update(SimStateProxy stateProxy, int proxyIndex) throws RemoteException, NotBoundException {
////		System.out.println("stateProxy: " + stateProxy);
////		System.out.println("proxyIndex: " + proxyIndex);
////		System.out.println("stateProxy.storage(proxyIndex): " + stateProxy.storage(proxyIndex));
////		System.out.println("stateProxy.storage(proxyIndex).getClass(): " + stateProxy.storage(proxyIndex).getClass());
////		
//////		CampusWorldProxy cwp = (CampusWorldProxy) stateProxy;
//////		stateProxy.
////		
////		//TODO
////		// Get the latest geomvector data that has changed and update this one to match it
////		// but only update the dynamic geomvectors, not the static ones
////		// and from the dynamic ones, only update the ones that have changed...
////
////		ContinuousStorage storage = (ContinuousStorage) (stateProxy.storage(proxyIndex));
////		HashMap<Long, Double2D> map = storage.getStorageMap();
////		
////		// Clear all geometries and MBR
////		clear();
////
////		//TODO ...
////		// Reconstruct geometries? How? We're just storing locations in storage, no?
////		// Although, currently, they're just points, but still, they need to be GeomVectorFields
////		// Quadtree? index? etc...
//	}
}