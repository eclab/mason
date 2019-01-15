/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial1and2.ec;

import sim.engine.*;
import sim.field.grid.*;
import ec.util.*;
import sim.app.tutorial1and2.*;

public class ECExample extends SimState
    {
    private static final long serialVersionUID = 1;

    public ECExample(long seed)
        {
        super(seed);
        }

    public ECExample(MersenneTwisterFast gen)
        {
        super(gen);
        }
    
    // our own parameters for setting the grid size later on
    public IntGrid2D grid;
    
    public int gridWidth = 100;
    public int gridHeight = 100;
    
    public void start()
        {
        super.start();
        grid = new IntGrid2D(gridWidth, gridHeight);
        schedule.scheduleRepeating(new CA2());
        }

    public static void main(String[] args)
        {
        doLoop(ECExample.class, args);
        System.exit(0);
        }    
    }
