/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.mav;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

public /*strictfp*/ class MavDemo extends SimState
    {
    public Continuous2D ground;
    public Continuous2D mavs;
    
    public double width = 500;
    public double height = 500;
    public double crashDistance = 8;
    public double sensorRangeDistance = 50; 
    public int numMavs = 30;
    
    // shapes on the ground
    public Region[] region = new Region[]
    {
    new Region(0, 1, 50,50),
    new Region(1,  2, 200, 200),
    new Region(2, 3, 200,450)
    };
    
    public MavDemo(long seed)
        {
        super(seed);
        }
    
    public void start()
        {
        super.start();

        // We'll use a Continuous2D field for the ground regions -- but in fact there are only
        // a relatively few ground regions and so when we do hit testing etc. (see surfaceAtPoint(...)),
        // we'll just scan through the region[] array rather than go through the overhead of the
        // field.  Why dump them in the field then?  Simply so we can use a field portrayal.
        // So we make a field portrayal with one big discretization -- so it surely will draw ALL objects
        // objects during every redraw.  This also lets us just set the location of the objects to 0,0,
        // and use the objects' internal shape coordinates for handling their drawing.
        ground = new Continuous2D(width > height ? width : height, width, height);
        for(int i = 0 ; i < region.length; i++)
            ground.setObjectLocation(region[i], new Double2D(region[i].originx, region[i].originy));

        // Use a Continuous2D for the MAVs.  We need to
        // compute a good discretization: such that the width of the buckets
        // is twice the width of the sensors, plus a little bit more for overlap.
        // Since the MAVs are schedulable, we'll load them into the schedule to be
        // fired each time as well.
        mavs = new Continuous2D(sensorRangeDistance * 2, width, height);        
        for(int i = 0 ; i < numMavs; i++)
            {
            // put the mav in a random location and random orientation.  We'll give them
            // some N steps to get away from one another before we start crashing them into each other
            Mav mav = new Mav(4,random.nextDouble()*width,random.nextDouble()*height);
            mavs.setObjectLocation(mav, new Double2D(mav.x,mav.y));
            schedule.scheduleRepeating(mav);
            }
        }
    
    public static void main(String[] args)
        {
        doLoop(MavDemo.class, args);
        System.exit(0);
        }    

    }
