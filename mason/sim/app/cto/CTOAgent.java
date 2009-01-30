/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.cto;

import sim.util.*;
import sim.engine.*;
import java.awt.*;

public /*strictfp*/ class CTOAgent extends sim.portrayal.simple.OvalPortrayal2D implements Steppable
    {
    public String id;

    public int intID = -1;

    public CTOAgent( final Double2D location, final int state, String id ) 
        {
        super(CooperativeObservation.DIAMETER);

        this.agentLocation = location;
        this.setState( state );
        this.id = id;

        if( id.startsWith( "A" ) )
            {
            try
                {
                intID = Integer.parseInt( id.substring(5) ); // "AGENT"
                }
            catch( IndexOutOfBoundsException e )
                {
                System.err.println( "Exception generated: " + e );
                System.exit(1);
                }
            catch( NumberFormatException e )
                {
                System.err.println( "Exception generated: " + e );
                System.exit(1);
                }
            }
        else
            {
            try
                {
                intID = Integer.parseInt( id.substring(6) ); // "TARGET"
                }
            catch( IndexOutOfBoundsException e )
                {
                System.err.println( "Exception generated: " + e );
                System.exit(1);
                }
            catch( NumberFormatException e )
                {
                System.err.println( "Exception generated: " + e );
                System.exit(1);
                }
            }
        }

    Double2D desiredLocation = null;
    Double2D suggestedLocation = null;
    int steps = 0;

    public void step( final SimState state )
        {

        CooperativeObservation hb = (CooperativeObservation)state;

        Double2D location = agentLocation;//hb.environment.getObjectLocation(this);

        if( agentState == AGENT )
            {
            hb.agentPos[intID] = location;
            }
        else
            {
            hb.targetPos[intID] = location;
            }

        if( agentState == AGENT )
            {
            suggestedLocation = hb.kMeansEngine.getGoalPosition( intID );
            if( suggestedLocation != null )
                {
                desiredLocation = suggestedLocation;
                }
            else
                {
                steps--;
                if( steps <= 0 )
                    {
                    desiredLocation = new Double2D( state.random.nextDouble()*(CooperativeObservation.XMAX-CooperativeObservation.XMIN-CooperativeObservation.DIAMETER)+CooperativeObservation.XMIN+CooperativeObservation.DIAMETER/2,
                        state.random.nextDouble()*(CooperativeObservation.YMAX-CooperativeObservation.YMIN-CooperativeObservation.DIAMETER)+CooperativeObservation.YMIN+CooperativeObservation.DIAMETER/2 );
                    steps = 100;
                    }
                }
            }
        else
            {
            steps--;
            if( desiredLocation == null || steps <= 0 )
                {
                desiredLocation = new Double2D( state.random.nextDouble()*(CooperativeObservation.XMAX-CooperativeObservation.XMIN-CooperativeObservation.DIAMETER)+CooperativeObservation.XMIN+CooperativeObservation.DIAMETER/2,
                    state.random.nextDouble()*(CooperativeObservation.YMAX-CooperativeObservation.YMIN-CooperativeObservation.DIAMETER)+CooperativeObservation.YMIN+CooperativeObservation.DIAMETER/2 );
                steps = 100;
                }
            }

        double dx = desiredLocation.x - location.x;
        double dy = desiredLocation.y - location.y;
        if( dx > 0.5 )
            dx = 0.5;
        else if( dx < -0.5 )
            dx = -0.5;
        if( dy > 0.5 )
            dy = 0.5;
        else if( dy < -0.5 )
            dy = -0.5;
        if( dx < 0.5 && dx > -0.5 && dy < 0.5 && dy > -0.5 )
            steps = 0;

        if( agentState == AGENT )
            {
            dx *= 2.0;
            dy *= 2.0;
            }

        if( ! hb.acceptablePosition( this, new Double2D( location.x + dx, location.y + dy ) ) )
            {
            steps = 0;
            }
        else
            {
            agentLocation = new Double2D(location.x + dx, location.y + dy);
            hb.environment.setObjectLocation(this,agentLocation);
            }

        }

    // application specific variables
    public static final int AGENT = 0;
    public static final int TARGET = 1;

    protected int agentState;
    public int getState() { return agentState; }
        
    // not public so it doesn't appear in the inspector -- if a user changed it in the
    // inspector, various exceptions would occur.
    void setState( final int agentState )
        {
        if( agentState != AGENT && agentState != TARGET )
            throw new RuntimeException("Unknown state desired to be set (command ignored!): " + agentState );
        else
            this.agentState = agentState;
        
        // set the oval's color
        if (agentState == AGENT) paint = agentColor;
        else paint = targetColor;
        }

    protected Color agentColor = new Color(0,0,0);
    protected Color targetColor = new Color(255,0,0);
    
    // for Object2D
    public Double2D agentLocation = null;

    }
