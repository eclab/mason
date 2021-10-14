/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers.display;
import sim.display.Continuous2DProxy;
import sim.display.SimStateProxy;

public class FlockersProxy extends SimStateProxy
    {
    private static final long serialVersionUID = 1;

    public Continuous2DProxy flockers = new Continuous2DProxy(1, 1, 1);
    //public double width = 300;
    //public double height = 300;
    //public int numFlockers = 200;

    /** Creates a Flockers simulation with the given random number seed. */
    public FlockersProxy(long seed)
        {
        super(seed);
		setRegistryHost("localhost");
		//setRegistryPort(5000);
        }
    
    public void start()
        {
        super.start();
		registerFieldProxy(flockers, 0);
        }
	}
