/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims3d;

import sim.field.continuous.*;
import sim.engine.*;
import sim.display3d.*;
import sim.util.*;
import javax.swing.*;

public class WoimsDemo3D extends SimState
    {

    public static final double XMIN = 0;
    public static final double XMAX = 200;
    public static final double YMIN = 0;
    public static final double YMAX = 200;
    public static final double ZMIN = 0;
    public static final double ZMAX = 200;
    
    public static final double DIAMETER = 1;

    public static final double[][] obstInfo = { {40, 40, 40, 40}, {60, 135, 135, 135} };

    public static final int NUM_WOIMS = 40;

    public static final double TIMESTEP = 30;

    public Display3D display;
    public JFrame displayFrame;

    public Continuous3D environment = new Continuous3D( 2*DIAMETER, XMAX-XMIN, YMAX-YMIN, ZMAX-ZMIN );
    public Continuous3D woimEnvironment = null;
    public Continuous3D obstacles = null;

    public WoimsDemo3D(long seed)
        {
        super(seed);
        }

    public final static double EXTRA_SPACE = 10;

    public void setObjectLocation( final Woim3D woim, Double3D location )
        {
        // toroidal world!
        double x = location.x;
        while( x < XMIN - EXTRA_SPACE )
            x += XMAX-XMIN+EXTRA_SPACE+EXTRA_SPACE;
        while( x > XMAX + EXTRA_SPACE )
            x -= XMAX-XMIN+EXTRA_SPACE+EXTRA_SPACE;
        double y = location.y;
        while( y < YMIN - EXTRA_SPACE )
            y += YMAX-YMIN+EXTRA_SPACE+EXTRA_SPACE;
        while( y > YMAX + EXTRA_SPACE )
            y -= YMAX-YMIN+EXTRA_SPACE+EXTRA_SPACE;
        double z = location.z;
        while( z < ZMIN - EXTRA_SPACE )
            z += ZMAX-ZMIN+EXTRA_SPACE+EXTRA_SPACE;
        while( z > ZMAX + EXTRA_SPACE )
            z -= ZMAX-ZMIN+EXTRA_SPACE+EXTRA_SPACE;
        location = new Double3D( x, y, z );
        environment.setObjectLocation( woim, location );
        woimEnvironment.setObjectLocation( woim, location );
        woim.x = location.x;
        woim.y = location.y;
        woim.z = location.z;
        }

    public void start()
        {
        super.start();  // clear out the schedule

        environment = new Continuous3D( 2*DIAMETER, XMAX-XMIN, YMAX-YMIN, ZMAX-ZMIN );
        woimEnvironment = new Continuous3D( Woim3D.MAX_DISTANCE,XMAX-XMIN, YMAX-YMIN, ZMAX-ZMIN );
        obstacles = new Continuous3D( Math.max(XMAX-XMIN,Math.max(YMAX-YMIN,ZMAX-ZMIN)),XMAX-XMIN, YMAX-YMIN, ZMAX-ZMIN );

        // Schedule the Woims -- we could instead use a RandomSequence, which would be faster,
        // but this is a good test of the scheduler
        for(int x=0;x<NUM_WOIMS;x++)
            {
            Double3D loc = null;
            Woim3D woim = null;
            int caz = random.nextInt(6);
            switch( caz )
                {
                case 0: loc = new Double3D( XMIN-EXTRA_SPACE,
                    random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2,
                    random.nextDouble()*(ZMAX-ZMIN-DIAMETER)+ZMIN+DIAMETER/2 );
                    break;
                case 1: loc = new Double3D( XMAX+EXTRA_SPACE,
                    random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2,
                    random.nextDouble()*(ZMAX-ZMIN-DIAMETER)+ZMIN+DIAMETER/2 );
                    break;
                case 2: loc = new Double3D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                    YMIN-EXTRA_SPACE,
                    random.nextDouble()*(ZMAX-ZMIN-DIAMETER)+ZMIN+DIAMETER/2 );
                    break;
                case 3: loc = new Double3D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                    YMAX+EXTRA_SPACE,
                    random.nextDouble()*(ZMAX-ZMIN-DIAMETER)+ZMIN+DIAMETER/2 );
                    break;
                case 4: loc = new Double3D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                    random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2,
                    ZMIN-EXTRA_SPACE );
                    break;
                case 5: loc = new Double3D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                    random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2,
                    ZMAX+EXTRA_SPACE );
                    break;
                }
            woim = new Woim3D();
            environment.setObjectLocation( woim, loc );
            woimEnvironment.setObjectLocation( woim, loc );
            woim.x = loc.x;
            woim.y = loc.y;
            woim.z = loc.z;
            schedule.scheduleRepeating(woim);
            }

        for( int i = 0 ; i < obstInfo.length ; i++ )
            {
            environment.setObjectLocation(new Obstacle3D(obstInfo[i][0]),
                new Double3D(obstInfo[i][1],obstInfo[i][2],obstInfo[i][3]));
            }
        }
    
    public static void main(String[] args)
        {
        doLoop(WoimsDemo3D.class, args);
        System.exit(0);
        }    
    }
