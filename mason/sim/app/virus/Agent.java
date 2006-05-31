/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.virus;

import sim.engine.*;
import sim.util.Double2D;
import sim.portrayal.*;
import java.awt.geom.*;

public abstract /*strictfp*/ class Agent extends SimplePortrayal2D implements Steppable
    {

    public String id;

    public Double2D agentLocation; 

    public int intID = -1;

    public Agent( String id, Double2D location )
        {
        this.id = id;
        this.agentLocation = location;
        }

    double distanceSquared( final Double2D loc1, Double2D loc2 )
        {
        return( (loc1.x-loc2.x)*(loc1.x-loc2.x)+(loc1.y-loc2.y)*(loc1.y-loc2.y) );
        }

    // Returns "Human", "Evil", or "Good"
    public abstract String getType();  

    public boolean hitObject(Object object, DrawInfo2D info)
        {
        double diamx = info.draw.width*VirusInfectionDemo.DIAMETER;
        double diamy = info.draw.height*VirusInfectionDemo.DIAMETER;

        Ellipse2D.Double ellipse = new Ellipse2D.Double( (int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy) );
        return ( ellipse.intersects( info.clip.x, info.clip.y, info.clip.width, info.clip.height ) );
        }
    }
