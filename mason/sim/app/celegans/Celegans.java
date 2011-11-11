/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.celegans;
import sim.engine.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.util.*;


public class Celegans extends SimState
    {
    private static final long serialVersionUID = 1;

    public Continuous3D cells;
    public Continuous3D neurons;
    public Network synapses;
        
    Cells database;

    public Celegans(long seed)
        {
        super(seed);
        }

    public void start()
        {
        super.start();
        if (database == null) database = new Cells();  // only load at start() time, and only ONCE

        cells = new Continuous3D(100, 100, 100, 100);
        neurons = new Continuous3D(100, 100, 100, 100);
        synapses = new Network();
        
        Cell p0 = database.P0;
                
        p0.stopper = schedule.scheduleRepeating(p0);
        p0.step(this);  // have p0 add himself to the continuous3D.  Yes, I know this is unusual, but it's kosher.  Sort of.
                
        schedule.scheduleOnce(1000, -1, new Steppable() { public void step(SimState state) { state.kill(); } });   // kill the simulation at around 600, before the cells all die at 1000
        }

    public static void main(String[] args)
        {
        doLoop(Celegans.class, args);
        System.exit(0);
        }    
    }
