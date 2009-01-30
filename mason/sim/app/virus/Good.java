/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.virus;

import sim.util.*;
import sim.engine.*;
import java.awt.*;
import sim.portrayal.*;

public /*strictfp*/ class Good extends Agent
    {

    protected boolean greedy = false;
    public final boolean getIsGreedy() { return greedy; }
    public final void setIsGreedy( final boolean b ) { greedy = b; }

    public Good( String id, Double2D location ) 
        {
        super( id, location );
        try
            {
            intID = Integer.parseInt( id.substring(4) ); // "Good"
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

    Double2D desiredLocation = null;
    Double2D suggestedLocation = null;
    int steps = 0;

    public void step( final SimState state )
        {
        VirusInfectionDemo hb = (VirusInfectionDemo)state;

        desiredLocation = null;
        double distance2DesiredLocation = 1e30;

        Bag mysteriousObjects = hb.environment.getObjectsWithinDistance( agentLocation, 50.0 * VirusInfectionDemo.HEALING_DISTANCE );
        if( mysteriousObjects != null )
            {
            for( int i = 0 ; i < mysteriousObjects.numObjs ; i++ )
                {
                if( mysteriousObjects.objs[i] != null &&
                    mysteriousObjects.objs[i] != this )
                    {
                    // if agent is not human, wasted time....
                    if( ! (((Agent)mysteriousObjects.objs[i]) instanceof Human ))
                        continue;
                    Human ta = (Human)(mysteriousObjects.objs[i]);
                    // if agent is already healthy, wasted time....
                    if( !ta.isInfected() )
                        continue;
                    if( hb.withinHealingDistance( this, agentLocation, ta, ta.agentLocation ) )
                        ta.setInfected( false );
                    else
                        {
                        if( getIsGreedy() )
                            {
                            double tmpDist = distanceSquared( agentLocation, ta.agentLocation );
                            if( tmpDist <  distance2DesiredLocation )
                                {
                                desiredLocation = ta.agentLocation;
                                distance2DesiredLocation = tmpDist;
                                }
                            }
                        }
                    }
                }
            }

        steps--;
        if( desiredLocation == null || !getIsGreedy() )
            {
            if(  steps <= 0 )
                {
                suggestedLocation = new Double2D((state.random.nextDouble()-0.5)*((VirusInfectionDemo.XMAX-VirusInfectionDemo.XMIN)/5-VirusInfectionDemo.DIAMETER) +
                    //VirusInfectionDemo.XMIN
                    agentLocation.x 
                    //+VirusInfectionDemo.DIAMETER/2
                    ,
                    (state.random.nextDouble()-0.5)*((VirusInfectionDemo.YMAX-VirusInfectionDemo.YMIN)/5-VirusInfectionDemo.DIAMETER) +
                    agentLocation.y
                    //VirusInfectionDemo.YMIN
                    //+VirusInfectionDemo.DIAMETER/2
                    );
                steps = 100;
                }
            desiredLocation = suggestedLocation;
            }

        double dx = desiredLocation.x - agentLocation.x;
        double dy = desiredLocation.y - agentLocation.y;

                {
                double temp = 0.5 * /*Strict*/Math.sqrt(dx*dx+dy*dy);
                if( temp < 1 )
                    {
                    steps = 0;
                    }
                else
                    {
                    dx /= temp;
                    dy /= temp;
                    }
                }

        if( ! hb.acceptablePosition( this, new Double2D(agentLocation.x + dx, agentLocation.y + dy) ) )
            {
            steps = 0;
            }
        else
            {
            agentLocation = new Double2D(agentLocation.x + dx, agentLocation.y + dy);
            hb.environment.setObjectLocation(this,agentLocation);
            }
        }

    protected Color goodColor = new Color(0,0,0);
    protected Color goodMarkColor = new Color(255,0,0);
    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        double diamx = info.draw.width*VirusInfectionDemo.DIAMETER;
        double diamy = info.draw.height*VirusInfectionDemo.DIAMETER;

        graphics.setColor( goodColor );            
        graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
        graphics.setColor( goodMarkColor );
        graphics.fillRect((int)(info.draw.x-diamx/3),(int)(info.draw.y-diamy/16),(int)(diamx/1.5),(int)(diamy/8));
        graphics.fillRect((int)(info.draw.x-diamx/16),(int)(info.draw.y-diamy/3),(int)(diamx/8),(int)(diamy/1.5));
        }
    
    public String getType() { return "Good"; }
    }
