/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.networktest;

import sim.engine.*;
import sim.util.Double2D;
import sim.portrayal.*;
import java.awt.geom.*;
import java.awt.*;

public /*strictfp*/ class CustomNode extends SimplePortrayal2D implements Steppable
    {

    public String id;
    public String getID() { return id; }
    public void setID( final String id ) { this.id = id; }

    public CustomNode( String id )
        {
        this.id = id;
        }

    Double2D desiredLocation = null;
    Double2D suggestedLocation = null;
    int steps = 0;

    public void step( final SimState state )
        {
        NetworkTest nt = (NetworkTest)state;
        Double2D location = nt.environment.getObjectLocation(this);

        steps--;
        if( desiredLocation == null || steps <= 0 )
            {
            desiredLocation = new Double2D((state.random.nextDouble()-0.5)*((NetworkTest.XMAX-NetworkTest.XMIN)/5-NetworkTest.DIAMETER) + location.x,
                (state.random.nextDouble()-0.5)*((NetworkTest.YMAX-NetworkTest.YMIN)/5-NetworkTest.DIAMETER) + location.y);
            steps = 50+state.random.nextInt(50);
            }

        double dx = desiredLocation.x - location.x;
        double dy = desiredLocation.y - location.y;

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

        if( ! nt.acceptablePosition( this, new Double2D( location.x + dx, location.y + dy ) ) )
            {
            steps = 0;
            }
        else
            {
            nt.environment.setObjectLocation(this, new Double2D(location.x + dx, location.y + dy));
            }

        }

    public Font nodeFont = new Font("SansSerif", Font.PLAIN, 12);
    
    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        double diamx = info.draw.width*NetworkTest.DIAMETER;
        double diamy = info.draw.height*NetworkTest.DIAMETER;

        graphics.setColor( Color.red );
        graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
        graphics.setFont(nodeFont.deriveFont(nodeFont.getSize2D()*(float)info.draw.width));
        graphics.setColor( Color.blue );
        graphics.drawString( id, (int)(info.draw.x-diamx/2), (int)(info.draw.y-diamy/2) );
        }

    public boolean hitObject(Object object, DrawInfo2D info)
        {
        double diamx = info.draw.width*NetworkTest.DIAMETER;
        double diamy = info.draw.height*NetworkTest.DIAMETER;

        Ellipse2D.Double ellipse = new Ellipse2D.Double( (int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy) );
        return ( ellipse.intersects( info.clip.x, info.clip.y, info.clip.width, info.clip.height ) ); 
        }

    }
