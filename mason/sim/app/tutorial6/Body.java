/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial6;
import sim.engine.*;
import sim.util.*;

public class Body implements Steppable
    {
    public double velocity; 
    public double distanceFromSun; 
    
    public double getVelocity() { return velocity; }
    public double getDistanceFromSun() { return distanceFromSun; } 
        
    public Body(double vel, double d) 
        { 
        velocity = vel;  distanceFromSun = d; 
        }
        
    public void step(SimState state)
        {
        Tutorial6 tut = (Tutorial6) state;
        if (distanceFromSun > 0)  // the sun's at 0, and you can't divide by 0
            {
            double theta = ((velocity / distanceFromSun) * state.schedule.getSteps())%(2*Math.PI) ;  
            tut.bodies.setObjectLocation(this, 
                new Double2D(distanceFromSun*Math.cos(theta), distanceFromSun*Math.sin(theta)));
            }
        }
    }    
 
