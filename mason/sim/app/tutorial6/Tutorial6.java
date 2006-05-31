/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial6;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

public class Tutorial6 extends SimState
    {
    static final int PLUTO = 9;  // Furthest-out body
    public Continuous2D bodies;

    public Tutorial6(long seed)
        {
        super(seed);
        bodies = new Continuous2D(DISTANCE[PLUTO],DISTANCE[PLUTO],DISTANCE[PLUTO]);
        }

    // distance from sun in 10^5 km
    public static final double[] DISTANCE = new double[]
    {0, 579, 1082, 1496, 2279, 7786, 14335, 28725, 44951, 58700}; 

    // diameters in 10 km
    public static final double[] DIAMETER = new double[] 
    {139200.0, 487.9, 1210.4, 1275.6, 679.4, 14298.4, 12053.6, 5111.8, 4952.8, 239.0};
 
    // period in days 
    public static final double[] PERIOD = new double[] 
    {1 /* don't care :-) */, 88.0, 224.7, 365.2, 687.0, 4331, 10747, 30589, 59800, 90588 };

    public void start()
        {
        super.start();
        
        bodies = new Continuous2D(DISTANCE[PLUTO],DISTANCE[PLUTO],DISTANCE[PLUTO]);
        
        // make the bodies  -- stick them out the x axis, sweeping towards the y axis.
        for(int i=0; i<10;i++)
            {
            Body b = new Body((2*Math.PI*DISTANCE[i]) / PERIOD[i], DISTANCE[i]);
            bodies.setObjectLocation(b, new Double2D(DISTANCE[i],0)); 
            schedule.scheduleRepeating(b);
            }
        }

    public static void main(String[] args)
        {
        doLoop(Tutorial6.class, args);
        System.exit(0);
        }    
    }
