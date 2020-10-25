package sim.field.proxy;
import sim.field.grid.*;
import sim.engine.*;
import sim.field.storage.*;
import sim.field.partitioning.*;
import java.rmi.*;

public class ObjectGrid2DProxy extends ObjectGrid2D implements UpdatableProxy
	{
	public ObjectGrid2DProxy(int width, int height) { super(width, height); }

	public void update(SimStateProxy stateProxy, int proxyIndex) throws RemoteException, NotBoundException
		{
		// reshape if needed
		IntRect2D bounds = stateProxy.getBounds();
		int width = bounds.br().x - bounds.ul().x;
		int height = bounds.br().y - bounds.ul().y;
		if (width != this.width || height != this.height)
			reshape(width, height);
		
		// load storage
		ObjectGridStorage storage = (ObjectGridStorage)(stateProxy.getStorage(proxyIndex));
		Object[] data = (Object[])(storage.getStorageArray());	
		for(int x = 0; x < width; x++)
			{
			Object[] fieldx = field[x];
			for(int y = 0; y < height; y++)
				{
				fieldx[y] = data[x * height + y];
				}
			}		
		}
	}