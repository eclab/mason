package sim.app.physicsTutorial2;

import sim.engine.*;
import sim.field.continuous.*;
import ec.util.*;
import sim.physics2D.util.*;
import sim.physics2D.constraint.*;

import java.awt.*;

import sim.physics2D.*;

public class PhysicsTutorial2 extends SimState
    {
    public Continuous2D fieldEnvironment;
                                
    public PhysicsTutorial2(long seed)
        {
        super(seed);
        createGrids();
        }
                
    void createGrids()
        {       
        fieldEnvironment = new Continuous2D(25, 200, 200);
        }
                
    // Resets and starts a simulation
    public void start()
        {
        super.start();  // clear out the schedule
        createGrids();

        // Add physics specific code here
        }
        
    public static void main(String[] args)
        {
        doLoop(PhysicsTutorial2.class, args);
        System.exit(0);
        }
    }
