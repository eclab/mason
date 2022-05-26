package sim.display;

import sim.field.continuous.*;
import sim.field.geo.DGeomVectorField;
import sim.field.geo.GeomVectorContinuousStorage;
import sim.field.geo.GeomVectorField;
import sim.app.geo.dcampusworld.display.CampusWorldProxy;
import sim.engine.*;
import sim.engine.rmi.RemoteProcessorRMI;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.Envelope;

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

		//setViewRect();
		//port.getViewRect()
		
		
		/*
		IntRect2D[] rect_list = new IntRect2D[quad_tree_partitions.length];
		for (int p_ind = 0; p_ind < quad_tree_partitions.length; p_ind++)
		{
			int p = quad_tree_partitions[p_ind];
			VisualizationProcessor vp1 = stateProxy.visualizationProcessor(p);
			halo_size = vp1.getAOI();

			rect_list[p_ind] = vp1.getStorageBounds();  //use getWorldBounds() 
		    
		}
		
		
		IntRect2D fullBounds = IntRect2D.getBoundingRect(rect_list);
		*/
		
		int p0 = quad_tree_partitions[0];
		RemoteProcessorRMI vp0 = stateProxy.RemoteProcessorRMI(p0);
		halo_size = vp0.getAOI();

		IntRect2D fullBounds = vp0.getWorldBounds();
		

		
		Int2D new_ul = fullBounds.ul().add(halo_size,halo_size); //remove halo
		Int2D new_br = fullBounds.br().add(-1 * halo_size, -1 * halo_size); //remove halo
		fullBounds = new IntRect2D(new_ul, new_br);
		
		Int2D fullBounds_offset = fullBounds.ul();
		
		int width = fullBounds.br().x - fullBounds.ul().x;
		int height = fullBounds.br().y - fullBounds.ul().y;
		
		System.out.println("width : "+width);
		System.out.println("height : "+height);

		//if (width != this.width || height != this.height)
		
		reshape(width, height);

		
		
		GeomVectorContinuousStorage st = (GeomVectorContinuousStorage)(stateProxy.storage(proxyIndex));
		
		System.out.println("objs : "+this.getGeometries().size());
	
		
		//for (int p = 0; p < stateProxy.numProcessors; p++) {
		for (int p : quad_tree_partitions)
		{
			

			RemoteProcessorRMI vp1 = stateProxy.RemoteProcessorRMI(p);
		    IntRect2D partBound = vp1.getStorageBounds();
		    
		    System.out.println("partBound : "+partBound);
		    

            
            
            GeomVectorContinuousStorage storage = (GeomVectorContinuousStorage)(stateProxy.storage(proxyIndex));
            

            
            //this.getMBR().expandToInclude(storage.getGeomVectorField().MBR);
            System.out.println("env : "+this.getMBR().getMinX()+" "+this.getMBR().getMaxX()+" "+this.getMBR().getMinY()+" "+this.getMBR().getMaxY());
            
            System.out.println(storage.getGeomVectorField().getGeometries().numObjs);
            //System.exit(-1);

            for (Object a : storage.getGeomVectorField().getGeometries()) {
            	

            	MasonGeometry b = (MasonGeometry)a;
            	
            	System.out.println("point "+b.getGeometry().getEnvelopeInternal());

            	this.addGeometry(b);
              // }
            	
            }
            
            System.out.println(this.getGeometries().size());

    		//if (this.getGeometries().size() > 0)
    		//     System.exit(-1);	
            
            //System.out.println(p+" : "+"hashmap size :"+storage.locations.keySet().size()); 
            	
            
			System.out.println("aaa "+this.getMBR());
            	
			System.out.println("this parts env: "+storage.getGeomVectorField().MBR);
            //this.getMBR().expandToInclude(storage.getGeomVectorField().MBR);
			
			if (storage.globalEnvelope != null) {
	            this.getMBR().expandToInclude(storage.globalEnvelope);

			}
			
			
			System.out.println("bbb "+this.getMBR());
            //System.exit(-1);
            
	
            
		}
		
		//this.setMBR(st.getGeomVectorField().getMBR()); 
		
		System.out.println("objs 2 : "+this.getGeometries().size());

		//System.exit(-1);
		
		
		
		
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