package sim.display;
import sim.field.grid.*;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import sim.util.*;

public class DenseGrid2DProxy extends DenseGrid2D implements UpdatableProxy
	{
	private static final long serialVersionUID = 1L;

	public DenseGrid2DProxy(int width, int height) { super(width, height); }
	
	public void update(SimStateProxy stateProxy, int proxyIndex) throws RemoteException, NotBoundException
		{
		/*
		//original 
		
		// reshape if needed
		IntRect2D bounds = stateProxy.bounds();
		int width = bounds.br().x - bounds.ul().x;
		int height = bounds.br().y - bounds.ul().y;

		if (width != this.width || height != this.height)
			reshape(width, height);
		
		// load storage
		DenseGridStorage storage = (DenseGridStorage)(stateProxy.storage(proxyIndex));
		ArrayList[] data = (ArrayList[])(storage.storage);	
		for(int x = 0; x < width; x++)
			{
			Bag[] fieldx = field[x];
			for(int y = 0; y < height; y++)
				{
				ArrayList list = data[x * height + y];
				if (list == null) 
					{
					fieldx[y] = null;
					}
				else 
					{
					int sz = list.size();
					fieldx[y] = new Bag(sz);
					for(int i = 0; i < sz; i++)
						{
						fieldx[y].add(list.get(i));
						}
					}
				}
			}
				
		
		*/
		
		//stitch together with all partition storages
		
		//rebuild bounds by combining each processor?
		//IntRect2D bounds = stateProxy.bounds(); //HERE USE worldbounds instead! 
		
		IntRect2D bounds = stateProxy.worldBounds;
		System.out.println(bounds);

		int width = bounds.br().x - bounds.ul().x;
		int height = bounds.br().y - bounds.ul().y;
		

		if (width != this.width || height != this.height)
			reshape(width, height);
		
		for (int p = 0; p < stateProxy.numProcessors; p++) {
			VisualizationProcessor vp1 = stateProxy.visualizationProcessor(p);
			int halo_size = vp1.getAOI();
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
				
				
				Bag[] fieldx = field[x];
				for(int y = partition_height_low_ind; y < partition_height_high_ind; y++)
					{
					

					
					Int2D local_p = storage.toLocalPoint(new Int2D(x, y)); //convert to local storage to access partiton storage correctly
					ArrayList list = data[local_p.x * partBound.getHeight() + local_p.y];
					if (list == null) 
						{
						fieldx[y] = null;
						}
					else 
						{
						int sz = list.size();
						fieldx[y] = new Bag(sz);
						for(int i = 0; i < sz; i++)
							{
							fieldx[y].add(list.get(i));
							}
						}
					}
				}

		
		}
	  }
	}