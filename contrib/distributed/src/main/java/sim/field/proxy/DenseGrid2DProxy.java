package sim.field.proxy;
import sim.field.grid.*;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;
import java.util.*;
import sim.util.*;

public class DenseGrid2DProxy extends DenseGrid2D implements UpdatableProxy
	{
	public DenseGrid2DProxy(int width, int height) { super(width, height); }
	
	public void update(SimStateProxy stateProxy, int proxyIndex) throws RemoteException, NotBoundException
		{
		// reshape if needed
		IntRect2D bounds = stateProxy.getBounds();
		int width = bounds.br().x - bounds.ul().x;
		int height = bounds.br().y - bounds.ul().y;

		if (width != this.width || height != this.height)
			reshape(width, height);
		
		// load storage
		DenseGridStorage storage = (DenseGridStorage)(stateProxy.storage(proxyIndex));
		ArrayList[] data = (ArrayList[])(storage.getStorageArray());	
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
		}
	}