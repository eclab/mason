/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dHeatBugs.visualization;

import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;
import sim.field.proxy.*;

public class HeatBugsProxy extends SimStateProxy
    {
    private static final long serialVersionUID = 1;

    public HeatBugsProxy(long seed)
        {
        super(seed);
		setRegistryHost("my.host.org");
		setRegistryPort(21242);
        }
        
	DoubleGrid2DProxy valgrid = new DoubleGrid2DProxy(1,1);	// width and height don't matter, they'll be changed
	DenseGrid2DProxy buggrid = new DenseGrid2DProxy(1,1);		// width and height don't matter, they'll be changed

    public void start()
        {
        super.start();
		registerFieldProxy(valgrid);
		registerFieldProxy(buggrid);
        }
    
    public static void main(String[] args)
        {
        doLoop(HeatBugsProxy.class, args);
        System.exit(0);
        }    
    }
    
    
    
    
    
