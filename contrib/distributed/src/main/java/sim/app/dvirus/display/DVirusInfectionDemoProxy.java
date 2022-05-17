/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
        
package sim.app.dvirus.display;

import sim.display.Continuous2DProxy;

import sim.display.SimStateProxy;
import sim.field.continuous.DContinuous2D;

public class DVirusInfectionDemoProxy extends SimStateProxy{
        
    private static final long serialVersionUID = 1;

    public DVirusInfectionDemoProxy(long seed)
        {
        super(seed);
        setRegistryHost("localhost");
        //setRegistryPort(5000);
        }
        

    public Continuous2DProxy envgrid = new Continuous2DProxy(1, 1, 1);

    public void start()
        {
        super.start();
        registerFieldProxy(envgrid, 0);
        }

    }
