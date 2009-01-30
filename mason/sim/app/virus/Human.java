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

public /*strictfp*/ class Human extends Agent
    {

    protected boolean infected = false;
    public final boolean isInfected() { return infected; }
    public final void setInfected( boolean b ) { infected = b; }

    public Human( String id, Double2D location ) 
        {
        super( id, location );
        try
            {
            intID = Integer.parseInt( id.substring(5) ); // "Human"
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

        steps--;
        if( desiredLocation == null || steps <= 0 )
            {
            desiredLocation = new Double2D((state.random.nextDouble()-0.5)*((VirusInfectionDemo.XMAX-VirusInfectionDemo.XMIN)/5-VirusInfectionDemo.DIAMETER) +
                //VirusInfectionDemo.XMIN
                agentLocation.x 
                //+VirusInfectionDemo.DIAMETER/2
                ,
                (state.random.nextDouble()-0.5)*((VirusInfectionDemo.YMAX-VirusInfectionDemo.YMIN)/5-VirusInfectionDemo.DIAMETER) +
                agentLocation.y
                //VirusInfectionDemo.YMIN
                //+VirusInfectionDemo.DIAMETER/2
                );
            steps = 50+state.random.nextInt(50);
            }

        double dx = desiredLocation.x - agentLocation.x;
        double dy = desiredLocation.y - agentLocation.y;

                {
                double temp = /*Strict*/Math.sqrt(dx*dx+dy*dy);
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

        if( ! hb.acceptablePosition( this, new Double2D( agentLocation.x + dx, agentLocation.y + dy ) ) )
            {
            steps = 0;
            }
        else
            {
            agentLocation = new Double2D(agentLocation.x + dx, agentLocation.y + dy);
            hb.environment.setObjectLocation(this,agentLocation);
            }

        }

    protected Color humanColor = new Color(192,128,128);
    protected Color infectedColor = new Color(128,255,128);
    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        double diamx = info.draw.width*VirusInfectionDemo.DIAMETER;
        double diamy = info.draw.height*VirusInfectionDemo.DIAMETER;
    
        if (isInfected())
            graphics.setColor( infectedColor );
        else graphics.setColor ( humanColor ); 
        graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
        }


    public String getType()
        {
        if( isInfected() )
            return "Infected Human";
        else
            return "Healthy Human";
        }
    }
