/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.lsystem;
import sim.engine.*;
import sim.field.continuous.*;

public /*strictfp*/ class LSystem extends SimState
    {
    /** @todo handle realocation of grids when these two are changed */
    public double xMin = 0;
    public double xMax = 100;
    public double yMin = 0;
    public double yMax = 100;
    
    public LSystemData l = new LSystemData();
    
    public Continuous2D drawEnvironment;
    

    public LSystem(long seed)
        {
        this(seed, 100, 100);
        }
        
    public LSystem(long seed, int width, int height)
        {
        super(seed);
        xMax = width; yMax = height;
        
        createGrids();
        }

    void createGrids()
        {       
        drawEnvironment = new Continuous2D(5, (xMax - xMin), (yMax - yMin));
        }
    
    /** Resets and starts a simulation */
    public void start()
        {
        super.start();  // clear out the schedule
        createGrids();

        LSystemDrawer ld = new LSystemDrawer(l);
        ld.stopper = schedule.scheduleRepeating(ld);
        }
    
    public static void main(String[] args)
        {
        doLoop(LSystem.class, args);
        System.exit(0);
        }    
    }
    
    
    
    
    
