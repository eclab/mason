package sim.app.dpso.display;

import sim.display.Continuous2DProxy;
import sim.display.SimStateProxy;

public class PSOProxy extends SimStateProxy {
	
    private static final long serialVersionUID = 1;

    //public Continuous2DProxy flockers = new Continuous2DProxy(1, 1, 1);
    public Continuous2DProxy space = new Continuous2DProxy(1, 1, 1);


    /** Creates a DPSO simulation with the given random number seed. */
    public PSOProxy(long seed)
        {
        super(seed);
		setRegistryHost("localhost");
		//setRegistryPort(5000);
        }
    
    public void start()
        {
        super.start();
		registerFieldProxy(space, 0);
        }
	

}
