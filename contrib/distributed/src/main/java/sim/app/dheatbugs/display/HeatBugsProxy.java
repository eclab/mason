/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dheatbugs.display;

import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;
import sim.display.*;

public class HeatBugsProxy extends SimStateProxy
    {
    private static final long serialVersionUID = 1;

    public HeatBugsProxy(long seed)
        {
        super(seed);
		setRegistryHost("localhost");
		//setRegistryPort(5000);
        }
        
	DoubleGrid2DProxy valgrid = new DoubleGrid2DProxy(1,1);	// width and height don't matter, they'll be changed
	DenseGrid2DProxy buggrid = new DenseGrid2DProxy(1,1);		// width and height don't matter, they'll be changed

    public void start()
        {
        super.start();
		registerFieldProxy(valgrid, 0);
		registerFieldProxy(buggrid, 2);
        }
    }
    
    
    
    
    
