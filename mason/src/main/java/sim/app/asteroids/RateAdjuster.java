/*
  Copyright 2009 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.asteroids;
import sim.engine.*;
import sim.display.*;


/** A simple class which maintains a frame rate fixed to no more than a given number of ticks per second.
    Typically this is used in games or visual demonstrations, where it is best to add it not to the Schedule
    but to the GUIState's minischedule.  For example, you could do this when start() and load(...) are called:
        
    <p><tt>
    myGUIState.scheduleRepeatingImmediatelyAfter(new RateAdjuster(60));  // I want 60 frames per second maximum
    </tt>
*/

public class RateAdjuster implements Steppable
    {
    private static final long serialVersionUID = 1;

    long initialTime;
    long totalTics;
    boolean started = false;
    double rate;
        
    public RateAdjuster(double targetRate)
        {
        rate = targetRate;
        }
        
    public void step(SimState state)
        {
        if (!started)
            {
            initialTime = System.currentTimeMillis();
            started = true;
            }
        else
            {
            long currentTime = System.currentTimeMillis();
            long time = currentTime - initialTime;
            totalTics++;
                        
            long expectedTime = (long)(totalTics / rate * 1000);
            if (time < expectedTime)  // too fast, need to slow down
                try
                    { 
                    Thread.currentThread().sleep(expectedTime - time); 
                    }
                catch (InterruptedException e) { } 
            else    // we lost time, but don't try to make up for it. We do this by resetting the clock
                {
                initialTime = currentTime;
                totalTics = 0;
                }
            }
        }
    }

