package sim.app.geo.dturkana.display;

import sim.display.Continuous2DProxy;
import sim.display.DenseGrid2DProxy;
import sim.display.SimStateProxy;
import sim.field.geo.GeomGridField;

public class TurkanaSouthModelProxy extends SimStateProxy{
	
    private static final long serialVersionUID = 1;

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
		registerFieldProxy(turkanians, 1);
        }


}
