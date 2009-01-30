/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

public /*strictfp*/ class WoimsDemo extends SimState
    {

    // dimensions of the environment
    public static final double XMIN = 0;
    public static final double XMAX = 200;
    public static final double YMIN = 0;
    public static final double YMAX = 200;

    // the diameter of each link
    public static final double DIAMETER = 1;

    // where the obstacles are located (diameter, xpos, ypos)
    public static final double[][] obstInfo = { {20, 40, 40}, {30, 135, 135} };

    // number of woims
    public static final int NUM_WOIMS = 40;

    // the difference between simulation time and woims time. it is used to compute by how much they moved. can be eliminated, but the speed
    // would need to be increased to maintain the same simulation quality
    public static final double TIMESTEP = 2;

    // for nice displaying, extra space is allocated arround the visible area
    public final static double EXTRA_SPACE = 25;

    // the maximum number of links per woim
    public final static int MAX_LINKS = 1000;

    // the woims and obstacle environments
    public Continuous2D woimsEnvironment = null;
    public Continuous2D obstaclesEnvironment = null;

    /** Creates a WoimsDemo simulation with the given random number seed. */
    public WoimsDemo(long seed)
        {
        super(seed);
        }

    public void setObjectLocation( final Woim woim, Double2D location )
        {
        // toroidal world!
        double x = (((location.x + EXTRA_SPACE - XMIN) + (XMAX-XMIN  + 2*EXTRA_SPACE)) % (XMAX-XMIN + 2*EXTRA_SPACE)) + XMIN - EXTRA_SPACE;
        double y = (((location.y + EXTRA_SPACE - YMIN) + (YMAX-YMIN  + 2*EXTRA_SPACE)) % (YMAX-YMIN + 2*EXTRA_SPACE)) + YMIN - EXTRA_SPACE;
        
        location = new Double2D( x, y );

        woimsEnvironment.setObjectLocation( woim, location );

        // to speed up the simulation, each woims knows where it is located (gets rid of a hash get call)
        woim.x = location.x;
        woim.y = location.y;
        }

    public void start()
        {
        super.start();  // clear out the schedule

        woimsEnvironment = new Continuous2D( Woim.MAX_DISTANCE, (XMAX-XMIN), (YMAX-YMIN) );
        obstaclesEnvironment = new Continuous2D( /*Math.max(Woim.numLinks*DIAMETER,30)*/30, (XMAX-XMIN), (YMAX-YMIN) );

        // Schedule the Woims -- we could instead use a RandomSequence, which would be faster,
        // but this is a good test of the scheduler
        for(int x=0;x<NUM_WOIMS;x++)
            {
            Double2D loc = null;
            Woim woim = null;
            int caz = random.nextInt(4);
            switch( caz )
                {
                case 0: loc = new Double2D( XMIN-EXTRA_SPACE,
                    random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 );
                    break;
                case 1: loc = new Double2D( XMAX+EXTRA_SPACE,
                    random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 );
                    break;
                case 2: loc = new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                    YMIN-EXTRA_SPACE );
                    break;
                case 3: loc = new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                    YMAX+EXTRA_SPACE );
                    break;
                }
            woim = new Woim();
            woimsEnvironment.setObjectLocation( woim, loc );
            woim.x = loc.x;
            woim.y = loc.y;
            schedule.scheduleRepeating(woim);
            }

        // add the obstacles to the simulation
        for( int i = 0 ; i < obstInfo.length ; i++ )
            {
            Obstacle obst = new Obstacle( obstInfo[i][0] );
            obstaclesEnvironment.setObjectLocation( obst, new Double2D( obstInfo[i][1], obstInfo[i][2] ) );
            }
        
        }

    public static void main(String[] args)
        {
        doLoop(WoimsDemo.class, args);
        System.exit(0);
        }    
    }
