/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pacman;
import sim.engine.*;
import sim.display.*;


/** A simple class which maintains an approximate framerate despite the speed of the machine.
    A simple p-controller with momentum, nothing special.  */

public class RateAdjuster implements Steppable
    {
    // how long we should sleep -- a variable with momentum.
    double sleep = 1;
        
    // our current frame rate -- also a variable with momentum
    double rate;
        
    // The last timestamp.  If it's -1 or -2, we've not gathered enough information yet.  Thus we take two ticks to get up to speed
    double last = -2;
        
    // only used for debugging
    int tic = 0;

    // The desired frame rate, in frames per second
    public static final double DESIRED_RATE = 60;  // fps
        
    // The most we'll sleep (in ms) despite the desired frame rate.  Right now this is 10, but we may
    // need to make it larger.
    public static final double MAXIMUM_SLEEP = 10;  // ms
        
    // The learning rate for our momentum.
    public static final double A = 0.7;
        
    public void step(SimState state)
        {
        // first time around, just gather the current time
        if (last == -2)  // can't compute yet
            {
            last = System.currentTimeMillis();
            last = -1;
            }
                
        // next time around gather a frame rate
        else if (last == -1)
            {
            long cur = System.currentTimeMillis();
            rate = cur - last;
            last = cur;
            }
                
        // third time around we *technically* have enough data to go on though it's probably pretty rough
        else
            {
            long cur = System.currentTimeMillis();
            rate = (1 - A) * rate + A * (1000 / (cur - last));
            last = cur;

            // Move sleep so that it's closer to the desired rate  (yes, we *divide* by desired rate, it's inverted remember)
            sleep = (1 - A) * sleep + A * sleep * rate / DESIRED_RATE;
                        
            // now we need to make sure sleep isn't ridiculous -- this happens initially when the rates are bad samples
            if (sleep > MAXIMUM_SLEEP)
                sleep = MAXIMUM_SLEEP;
                        
            // we can't sleep for fractional milliseconds.  So instead we sleep for AT LEAST
            // that many milliseconds, and with a certain probability we sleep for the next
            // millisecond up.  This will handle sleep values less than 1.0 as well.
                        
            long s = (long) sleep;
            if (state.random.nextBoolean(sleep - s)) s++;
            try { Thread.currentThread().sleep(s); }
            catch (InterruptedException e) { } 
                                                
            /*
              if (++tic % 20 == 0)
              System.out.println("rate: " + rate + "   sleep: " + sleep + "   actual: " + s);
            */
            }
        }
    }

