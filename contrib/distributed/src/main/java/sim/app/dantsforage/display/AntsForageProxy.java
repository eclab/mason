package sim.app.dantsforage.display;

import sim.app.dantsforage.DAnt;
import sim.display.Continuous2DProxy;
import sim.display.DenseGrid2DProxy;
import sim.display.DoubleGrid2DProxy;
import sim.display.IntGrid2DProxy;
import sim.display.SimStateProxy;
import sim.field.grid.DDenseGrid2D;
import sim.field.grid.DDoubleGrid2D;
import sim.field.grid.DIntGrid2D;

public class AntsForageProxy extends SimStateProxy
{
private static final long serialVersionUID = 1;



IntGrid2DProxy sitesGrid = new IntGrid2DProxy(1,1);
DoubleGrid2DProxy toFoodGridGrid = new DoubleGrid2DProxy(1,1);	// width and height don't matter, they'll be changed
DoubleGrid2DProxy toHomeGridGrid = new DoubleGrid2DProxy(1,1);	// width and height don't matter, they'll be changed
DenseGrid2DProxy antGrid = new DenseGrid2DProxy(1,1);		// width and height don't matter, they'll be changed
IntGrid2DProxy obstaclesGrid = new IntGrid2DProxy(1,1);




//public double width = 300;
//public double height = 300;
//public int numFlockers = 200;

/** Creates a Flockers simulation with the given random number seed. */
public AntsForageProxy(long seed)
    {
    super(seed);
	setRegistryHost("localhost");
	//setRegistryPort(5000);
    }

public void start()
    {
    super.start();
	registerFieldProxy(sitesGrid, 0);
	registerFieldProxy(toFoodGridGrid, 1);
	registerFieldProxy(toHomeGridGrid, 2);
	registerFieldProxy(antGrid, 3);
	registerFieldProxy(obstaclesGrid, 4);
	
	

    }
}
