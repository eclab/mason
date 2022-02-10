package sim.display;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.field.grid.DenseGrid2D;
import sim.field.storage.DenseGridStorage;
import sim.util.Bag;
import sim.util.Int2D;
import sim.util.IntRect2D;

public class DenseGrid2DProxy extends DenseGrid2D implements UpdatableProxy
	{
	private static final long serialVersionUID = 1L;

	public DenseGrid2DProxy(int width, int height)
	{
		super(width, height);
	}
	
	public void update(SimStateProxy stateProxy, int proxyIndex, int[] quad_tree_partitions) throws RemoteException, NotBoundException
		{

		
		//stitch together with all partition storages
		
		//rebuild bounds by combining each processor?
		
		//IntRect2D bounds = stateProxy.worldBounds;
		//System.out.println(bounds);

		//int width = bounds.br().x - bounds.ul().x;
		//int height = bounds.br().y - bounds.ul().y;
		
		int halo_size = 0;

		//calculate position in quadtree that encompasses all desired partitions
		VisualizationProcessor vp2 = stateProxy.visualizationProcessor(quad_tree_partitions[0]); //pick 1
		int[] extended_partition_list = vp2.getMinimumNeighborhood(quad_tree_partitions);
		
		IntRect2D[] rect_list = new IntRect2D[extended_partition_list.length];
		for (int p_ind = 0; p_ind < extended_partition_list.length; p_ind++)
		{
			int p = extended_partition_list[p_ind];
			VisualizationProcessor vp1 = stateProxy.visualizationProcessor(p);
			halo_size = vp1.getAOI();

			rect_list[p_ind] = vp1.getStorageBounds();
		    
		}
		
		IntRect2D fullBounds = IntRect2D.getBoundingRect(rect_list);
		/// SEAN -- double check add
		Int2D new_ul = fullBounds.ul().add(halo_size, halo_size); //remove halo
		Int2D new_br = fullBounds.br().add(-1 * halo_size, -1 * halo_size); //remove halo
		fullBounds = new IntRect2D(new_ul, new_br);
		
		Int2D fullBounds_offset = fullBounds.ul();
		
		int width = fullBounds.br().x - fullBounds.ul().x;
		int height = fullBounds.br().y - fullBounds.ul().y;
		
		int max_w_h = width;
		
		if (width < height) {
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
		    
		    
		    
			//remove halo bounds using bounds.ul offset, assumption is offset from 0,0 is halo size
		    
            int partition_width_low_ind = partBound.ul().getX()+halo_size;  //partition bounds, subtract to remove halo
            int partition_width_high_ind = partBound.br().getX()-halo_size;  //partition bounds, add to remove halo
            int partition_height_low_ind =  partBound.ul().getY()+halo_size;  //partition bounds
            int partition_height_high_ind =  partBound.br().getY()-halo_size;   //partition bounds 
            
            System.out.println(partition_width_low_ind);
            System.out.println(partition_width_high_ind);
            System.out.println(partition_height_low_ind);
            System.out.println(partition_height_high_ind);
            
			
            
            
			// load storage, add this to field!
			DenseGridStorage storage = (DenseGridStorage)(stateProxy.storage(proxyIndex));
			ArrayList[] data = (ArrayList[])(storage.storage);	
			for(int x = partition_width_low_ind; x < partition_width_high_ind; x++)
				{
				
				
				Bag[] fieldx = field[x - fullBounds_offset.getX()];
				for(int y = partition_height_low_ind; y < partition_height_high_ind; y++)
					{
					

					
					Int2D local_p = storage.toLocalPoint(new Int2D(x, y)); //convert to local storage to access partiton storage correctly
					ArrayList list = data[local_p.x * partBound.getHeight() + local_p.y];
					if (list == null) 
						{
						fieldx[y - fullBounds_offset.getY()] = null;
						}
					else 
						{
						int sz = list.size();
						fieldx[y - fullBounds_offset.getY()] = new Bag(sz);
						for(int i = 0; i < sz; i++)
							{
							fieldx[y - fullBounds_offset.getY()].add(list.get(i));
							
							

							
							}
						}
					}
				}

		
		}
	  }
	}