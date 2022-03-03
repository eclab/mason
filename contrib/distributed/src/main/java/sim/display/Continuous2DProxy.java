package sim.display;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map.Entry;

import sim.engine.DObject;
import sim.field.continuous.Continuous2D;
import sim.field.storage.ContinuousStorage;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntRect2D;

@SuppressWarnings("rawtypes")
public class Continuous2DProxy extends Continuous2D implements UpdatableProxy
{
	private static final long serialVersionUID = 1L;

	public Continuous2DProxy(double discretization, double width, double height)
	{
		super(discretization, width, height);
	}

	public void update(SimStateProxy stateProxy, int proxyIndex, int[] quad_tree_partitions) throws RemoteException, NotBoundException
	{
		/*
		IntRect2D bounds = stateProxy.worldBounds;
		//System.out.println(bounds);

		int width = bounds.br().x - bounds.ul().x;
		int height = bounds.br().y - bounds.ul().y;
		

		//if (width != this.width || height != this.height)
		reshape(width, height);
		*/

		int halo_size = 0;

		//calculate position in quadtree that encompasses all desired partitions
		VisualizationProcessor vp2 = stateProxy.visualizationProcessor(quad_tree_partitions[0]); //pick 1
		int[] extended_partition_list = vp2.getMinimumNeighborhood(quad_tree_partitions);

		//IntRect2D[] rect_list = new IntRect2D[quad_tree_partitions.length];
		IntRect2D[] rect_list = new IntRect2D[extended_partition_list.length];
		for (int p_ind = 0; p_ind < extended_partition_list.length; p_ind++)
		{
			int p = extended_partition_list[p_ind];
			VisualizationProcessor vp1 = stateProxy.visualizationProcessor(p);
			halo_size = vp1.getAOI();

			rect_list[p_ind] = vp1.getStorageBounds();
		    
		}
		
		IntRect2D fullBounds = IntRect2D.getBoundingRect(rect_list); //I want this to be based on quadtree
		
		/// SEAN -- double check add
		Int2D new_ul = fullBounds.ul().add(halo_size, halo_size); //remove halo
		Int2D new_br = fullBounds.br().add(-1 * halo_size, -1 * halo_size); //remove halo
		fullBounds = new IntRect2D(new_ul, new_br);
		// ^ private area bounds
		
		Int2D fullBounds_offset = fullBounds.ul();
		
		int width = fullBounds.br().x - fullBounds.ul().x;
		int height = fullBounds.br().y - fullBounds.ul().y;
		
		int max_w_h = width;
		
		if (width < height)
		{
			max_w_h = height;

		}
		
		//reshape(width, height);
		reshape(max_w_h, max_w_h);

		
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
            
            IntRect2D privateBounds = new IntRect2D(new Int2D(partition_width_low_ind, partition_height_low_ind), new Int2D(partition_width_high_ind,partition_height_high_ind));

			// load storage, add this to field!
            ContinuousStorage storage = (ContinuousStorage)(stateProxy.storage(proxyIndex));
            HashMap<Long, DObject>[] data = storage.storage;
            HashMap<Long, Double2D> locations = storage.getLocations();

//            HashMap<Long, DObject>[] privateData = (HashMap<Long, DObject>[]) new Object[data.length];

//			for (int x = partition_width_low_ind; x < partition_width_high_ind; x++) {
//				for (int y = partition_height_low_ind; y < partition_height_high_ind; y++) {
//					Int2D local_p = storage.toLocalPoint(new Int2D(x, y)); //convert to local storage to access partition storage correctly
////					if (local_p.x * partBound.getHeight() + local_p.y < 0 || local_p.x * partBound.getHeight() + local_p.y >= data.length) {
////						System.err.println("IndexOutOfBoundsException: " + local_p.x * partBound.getHeight() + local_p.y + " for bounds " + data.length);
////						continue;
////					}
//					HashMap<Long, DObject> privateArea = storage.getCell(local_p);
////					HashMap<Long, DObject> privateArea = data[local_p.x * partBound.getHeight() + local_p.y];
////					HashMap<Long, DObject> privateArea = data[GridStorage.getFlatIdx(local_p, partBound.getHeight())];
//	    			for (Entry<Long, DObject> entry : privateArea.entrySet()) {
//	    				Double2D loc = locations.get(entry.getKey());
//	    				DObject obj = entry.getValue();
//	    				setObjectLocation(obj, loc);
//	    			}
//				}
//			}

            HashMap<Long, Double2D> map = storage.getLocations();
    		for (Entry<Long, Double2D> entry : map.entrySet())
    		{
    			Double2D loc = entry.getValue();
    			Double2D new_loc = loc.subtract(new Double2D(fullBounds_offset));
    			
    			if (privateBounds.contains(loc))
    			{
    				for (int i=0; i<data.length; i++) {
    					
    					if (data[i].containsKey(entry.getKey())) {
    					
    					
        			    setObjectLocation(data[i].get(entry.getKey()), new_loc);
        			    
        			    break;

    					
    					}
    				}
    				//System.out.println("set obj loc: " + loc + " -> " + new_loc);
    			    //setObjectLocation(entry.getKey(), new_loc);
                    //setObjectLocation(entry.get)

    			    //setObjectLocation(entry.getValue(), new_loc);

    			}
    		}
		}
		
		System.out.println("--");
	}
}