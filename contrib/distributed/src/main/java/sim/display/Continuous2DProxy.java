package sim.display;

import sim.field.continuous.*;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import java.util.Map.Entry;

import sim.util.*;

@SuppressWarnings("rawtypes")
public class Continuous2DProxy extends Continuous2D implements UpdatableProxy {
	private static final long serialVersionUID = 1L;

	public Continuous2DProxy(double discretization, double width, double height) {
		super(discretization, width, height);
	}

	public void update(SimStateProxy stateProxy, int proxyIndex, int[] quad_tree_partitions) throws RemoteException, NotBoundException {
		

		
		/*
		IntRect2D bounds = stateProxy.worldBounds;
		//System.out.println(bounds);

		int width = bounds.br().x - bounds.ul().x;
		int height = bounds.br().y - bounds.ul().y;
		

		//if (width != this.width || height != this.height)
		reshape(width, height);
		*/
		

		int halo_size = 0;

		
		IntRect2D[] rect_list = new IntRect2D[quad_tree_partitions.length];
		for (int p_ind = 0; p_ind < quad_tree_partitions.length; p_ind++) {
			int p = quad_tree_partitions[p_ind];
			VisualizationProcessor vp1 = stateProxy.visualizationProcessor(p);
			halo_size = vp1.getAOI();

			rect_list[p_ind] = vp1.getStorageBounds();
		    
		}
		
		IntRect2D fullBounds = IntRect2D.getBoundingRect(rect_list);
		Int2D new_ul = fullBounds.ul().add(halo_size); //remove halo
		Int2D new_br = fullBounds.br().add(-1 * halo_size); //remove halo
		fullBounds = new IntRect2D(new_ul, new_br);
		
		Int2D fullBounds_offset = fullBounds.ul();
		
		int width = fullBounds.br().x - fullBounds.ul().x;
		int height = fullBounds.br().y - fullBounds.ul().y;
		

		//if (width != this.width || height != this.height)
		reshape(width, height);
		
		//for (int p = 0; p < stateProxy.numProcessors; p++) {
		for (int p : quad_tree_partitions) {

			VisualizationProcessor vp1 = stateProxy.visualizationProcessor(p);
			//int halo_size = vp1.getAOI();
		    IntRect2D partBound = vp1.getStorageBounds();
		    
		    
		    
			//remove halo bounds using bounds.ul offset, assumption is offset from 0,0 is halo size
		    
            int partition_width_low_ind = partBound.ul().getX()+halo_size;  //partition bounds, subtract to remove halo
            int partition_width_high_ind = partBound.br().getX()-halo_size;  //partition bounds, add to remove halo
            int partition_height_low_ind =  partBound.ul().getY()+halo_size;  //partition bounds
            int partition_height_high_ind =  partBound.br().getY()-halo_size;   //partition bounds 

            
			// load storage, add this to field!
            ContinuousStorage storage = (ContinuousStorage)(stateProxy.storage(proxyIndex));
    		HashMap<Long, Double2D> map = storage.getStorageMap();
    		discretization = storage.getDiscretization();

    		clear();

    		Int2D origin = fullBounds.ul();
    		
    		for (Entry<Long, Double2D> entry : map.entrySet()) {
    			Double2D loc = entry.getValue();
    			setObjectLocation(entry.getKey(), loc);
    		}
		}
    		

	}

}