/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.cto;
import sim.field.continuous.*;
import sim.engine.*;
import sim.util.*;

public /*strictfp*/ class CooperativeObservation extends SimState
    {
    public static final double XMIN = 0;
    public static final double XMAX = 400;
    public static final double YMIN = 0;
    public static final double YMAX = 400;
    
    public static final double DIAMETER = 8;
    
    public static final int NUM_TARGETS = 40;
    public static final int NUM_AGENTS = 10;

    Double2D[] agentPos;
    Double2D[] targetPos;
    public Continuous2D environment = null;

    KMeansEngine kMeansEngine;

    /** Creates a CooperativeObservation simulation with the given random number seed. */
    public CooperativeObservation(long seed)
        {
        super(seed);
        }

    boolean conflict( final Object agent1, final Double2D a, final Object agent2, final Double2D b )
        {
        if( ( ( a.x > b.x && a.x < b.x+DIAMETER ) ||
                ( a.x+DIAMETER > b.x && a.x+DIAMETER < b.x+DIAMETER ) ) &&
                ( ( a.y > b.y && a.y < b.y+DIAMETER ) ||
                ( a.y+DIAMETER > b.y && a.y+DIAMETER < b.y+DIAMETER ) ) )
            {
            return true;
            }
        return false;
        }

    boolean acceptablePosition( final Object agent, final Double2D location )
        {
        if( location.x < DIAMETER/2 || location.x > (XMAX-XMIN)/*environment.getXSize()*/-DIAMETER/2 ||
            location.y < DIAMETER/2 || location.y > (YMAX-YMIN)/*environment.getYSize()*/-DIAMETER/2 )
            return false;
        Bag misteriousObjects = environment.getObjectsWithinDistance( location, /*Strict*/Math.max( 2*DIAMETER, 2*DIAMETER ) );
        if( misteriousObjects != null )
            {
            for( int i = 0 ; i < misteriousObjects.numObjs ; i++ )
                {
                if( misteriousObjects.objs[i] != null && misteriousObjects.objs[i] != agent )
                    {
                    Object ta = (CTOAgent)(misteriousObjects.objs[i]);
                    if( conflict( agent, location, ta, environment.getObjectLocation(ta) ) )
                        return false;
                    }
                }
            }
        return true;
        }

    public void start()
        {
        super.start();  // clear out the schedule

        agentPos = new Double2D[ NUM_AGENTS ];
        for( int i = 0 ; i < NUM_AGENTS ; i++ )
            agentPos[i] = new Double2D();

        targetPos = new Double2D[ NUM_TARGETS ];
        for( int i = 0 ; i < NUM_TARGETS ; i++ )
            targetPos[i] = new Double2D();

        kMeansEngine = new KMeansEngine( this );

        environment = new Continuous2D(8.0, XMAX-XMIN, YMAX-YMIN);

        // Schedule the agents -- we could instead use a RandomSequence, which would be faster,
        // but this is a good test of the scheduler
        for(int x=0;x<NUM_AGENTS+NUM_TARGETS;x++)
            {
            Double2D loc = null;
            CTOAgent agent = null;
            int times = 0;
            do
                {
                loc = new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                    random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 );
                if( x < NUM_AGENTS )
                    {
                    agent = new CTOAgent( loc, CTOAgent.AGENT, "Agent"+x );
                    }
                else
                    {
                    agent = new CTOAgent( loc, CTOAgent.TARGET, "Target"+(x-NUM_AGENTS) );
                    }
                times++;
                if( times == 1000 )
                    {
                    System.err.println( "Cannot place agents. Exiting...." );
                    System.exit(1);
                    }
                } while( !acceptablePosition( agent, loc ) );
            environment.setObjectLocation(agent,loc);
            schedule.scheduleRepeating(agent);
            }

        }


    public static void main(String[] args)
        {
        doLoop(CooperativeObservation.class, args);
        System.exit(0);
        }    
    }
