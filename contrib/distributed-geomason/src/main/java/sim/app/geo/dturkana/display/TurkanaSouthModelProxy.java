package sim.app.geo.dturkana.display;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import sim.display.Continuous2DProxy;
import sim.display.DenseGrid2DProxy;
import sim.display.DoubleGrid2DProxy;
import sim.display.SimStateProxy;
import sim.field.geo.GeomGridField;
import sim.portrayal.grid.FastValueGridPortrayal2D;

public class TurkanaSouthModelProxy extends SimStateProxy{
	
    private static final long serialVersionUID = 1;

    DenseGrid2DProxy populationdensgrid = new DenseGrid2DProxy(1, 1);

    public DoubleGrid2DProxy raingrid = new DoubleGrid2DProxy(1,1);
    public DoubleGrid2DProxy veggrid = new DoubleGrid2DProxy(1,1);

    public DenseGrid2DProxy turkanians = new DenseGrid2DProxy(1, 1);

    //public double width = 300;
    //public double height = 300;
    //public int numFlockers = 200;

    /** Creates a Flockers simulation with the given random number seed. */
    public TurkanaSouthModelProxy(long seed)
        {
        super(seed);
		setRegistryHost("localhost");
		//setRegistryPort(5000);
        }
    
    public void start()
        {
        super.start();

		//registerFieldProxy(populationdensgrid, 0);
		registerFieldProxy(raingrid, 1);
		registerFieldProxy(veggrid, 2);
		registerFieldProxy(turkanians, 0);
        }


}
