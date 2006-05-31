/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.keepaway;
import java.awt.*;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

public /*strictfp*/ class Keepaway extends SimState
    {
    /** @todo handle realocation of grids when these two are changed */
    public double xMin = 0;
    public double xMax = 100;
    public double yMin = 0;
    public double yMax = 100;
    
    public Continuous2D fieldEnvironment;
    

    /** Creates a Keepaway simulation with the given random number seed. */
    public Keepaway(long seed)
        {
        this(seed, 100, 100);
        }
        
    public Keepaway(long seed, int width, int height)
        {
        super(seed);
        xMax = width; yMax = height;
        createGrids();
        }

    void createGrids()
        {       
        fieldEnvironment = new Continuous2D(25, (xMax - xMin), (yMax - yMin));
        }
    
    /** Resets and starts a simulation */
    public void start()
        {
        super.start();  // clear out the schedule
        createGrids();

        Bot b;
        double x,y;
        
        // bot 1-1  
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        b = new Bot(x, y, Color.red);
        b.cap = 0.65;
        fieldEnvironment.setObjectLocation(b, new Double2D(x,y));
        schedule.scheduleRepeating(b);
        
        // bot 2-1   
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        b = new Bot(x, y, Color.blue);
        b.cap = 0.5;
        fieldEnvironment.setObjectLocation(b, new Double2D(x,y));
        schedule.scheduleRepeating(b);
        
        
        // bot 2-2  
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        b = new Bot(x, y, Color.blue);
        b.cap = 0.5;
        fieldEnvironment.setObjectLocation(b, new Double2D(x,y));
        schedule.scheduleRepeating(b);
        
        // bot 2-3  
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        b = new Bot(x, y, Color.blue);
        b.cap = 0.5;
        fieldEnvironment.setObjectLocation(b, new Double2D(x,y));
        schedule.scheduleRepeating(b);
        
        // ball
        Ball ba;
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        ba = new Ball(x, y);
        fieldEnvironment.setObjectLocation(ba, new Double2D(x,y));
        schedule.scheduleRepeating(ba);
        }

    public static void main(String[] args)
        {
        doLoop(Keepaway.class, args);
        System.exit(0);
        }    

    
    }
    
    
    
    
    
