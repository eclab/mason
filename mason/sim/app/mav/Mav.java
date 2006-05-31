/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.mav;

import sim.portrayal.*;
import sim.engine.*;
import sim.util.*;

// we extend OvalPortrayal2D to steal its hitObjects() code -- but 
// we override the draw(...) code to draw our own oval with a little line...

public /*strictfp*/ class Mav implements Steppable, Oriented2D
    {
    public final static double[] theta = new double[/* 8 */]
    {
    0*(/*Strict*/Math.PI/180),
    45*(/*Strict*/Math.PI/180),
    90*(/*Strict*/Math.PI/180),
    135*(/*Strict*/Math.PI/180),
    180*(/*Strict*/Math.PI/180),
    225*(/*Strict*/Math.PI/180),
    270*(/*Strict*/Math.PI/180),
    315*(/*Strict*/Math.PI/180)
    };
        
    public final static double[] xd = new double[/* 8 */]
    {
    /*Strict*/Math.cos(theta[0]),
    /*Strict*/Math.cos(theta[1]),
    /*Strict*/Math.cos(theta[2]),
    /*Strict*/Math.cos(theta[3]),
    /*Strict*/Math.cos(theta[4]),
    /*Strict*/Math.cos(theta[5]),
    /*Strict*/Math.cos(theta[6]),
    /*Strict*/Math.cos(theta[7]),
    };
        
    public final static double[] yd = new double[/* 8 */]
    {
    /*Strict*/Math.sin(theta[0]),
    /*Strict*/Math.sin(theta[1]),
    /*Strict*/Math.sin(theta[2]),
    /*Strict*/Math.sin(theta[3]),
    /*Strict*/Math.sin(theta[4]),
    /*Strict*/Math.sin(theta[5]),
    /*Strict*/Math.sin(theta[6]),
    /*Strict*/Math.sin(theta[7]),
    };

    public int orientation = 0;
    public double x;
    public double y;

    public double orientation2D() { return theta[orientation]; }

    public Mav(int orientation, double x, double y)
        {
        this.orientation = orientation; this.x = x; this.y = y;
        }

    public void step(SimState state)
        {
        final MavDemo mavdemo = (MavDemo)state;
        orientation += mavdemo.random.nextInt(3) - 1;
        if (orientation > 7) orientation = 0;
        if (orientation < 0) orientation = 7;
        x += xd[orientation];
        y += yd[orientation];
        if (x >= mavdemo.width) x = mavdemo.width - 1;
        else if (x < 0) x = 0;
        if (y >= mavdemo.height) y = mavdemo.height - 1;
        else if (y < 0) y = 0;
        mavdemo.mavs.setObjectLocation(this,new Double2D(x,y));
        act(nearbyMAVs(mavdemo), currentSurface(mavdemo));
        }
    
    public void act(double[] sensorReading, int currentSurface)
        {
        if (currentSurface == 100) System.out.println("Acting");
        }
        
    double[] proximitySensors = new double[8];  // all squared values

    /** Re-uses the double[], so don't hang onto it */
    public double[] nearbyMAVs(MavDemo mavdemo)
        {
        for(int i=0;i<8;i++) proximitySensors[i] = Double.MAX_VALUE;
        
        final double d = mavdemo.sensorRangeDistance * mavdemo.sensorRangeDistance;
        
        final Bag nearbyMavs = mavdemo.mavs.getObjectsWithinDistance(new Double2D(x,y),16,false,false);
        for(int i=0;i<nearbyMavs.numObjs;i++)
            {
            final Mav mav = (Mav)(nearbyMavs.objs[i]);
            final double mavDistance = (mav.x-x)*(mav.x-x)+(mav.y-y)*(mav.y-y);
            if (mavDistance < d)  // it's within our range
                {
                final int octant = sensorForPoint(mav.x,mav.y);  // figure the octant
                proximitySensors[octant] = /*Strict*/Math.min(proximitySensors[octant],mavDistance);
                }
            }
        return proximitySensors;
        }
    
    /** 0 is the default surface */
    public int currentSurface(MavDemo mavdemo)
        {
        for(int i = 0; i < mavdemo.region.length;i++)
            if (mavdemo.region[i].area.contains(
                    x-mavdemo.region[i].originx,y-mavdemo.region[i].originy))
                return mavdemo.region[i].surface;
        return 0;
        }
    

    // in order to rotate 45/2 degrees counterclockwise around origin
    final double sinTheta = /*Strict*/Math.sin(45.0/2*/*Strict*/Math.PI/180);
    final double cosTheta = /*Strict*/Math.cos(45.0/2*/*Strict*/Math.PI/180);

    public int sensorForPoint(double px, double py)
        {
        int o = 0;
        // translate to origin
        px -= x; py -= y;

        // rotate 45/2 degrees counterclockwise about the origin
        final double xx = px * cosTheta + py * (-sinTheta);
        final double yy = px * sinTheta + py * cosTheta;
        
        // Now we've divided it into octants of 0--45, 45--90, etc.
        // for each sensor area.  The border between octants is
        // arbitrarily, not evenly, assigned to the octants, because
        // it results in fewer if/then statements/
        
        if (!(xx == 0.0 && yy == 0.0))
            {
            if (xx > 0)         // right side
                {
                if (yy > 0)     // quadrant 1
                    {
                    if (xx > yy)  o = 0;
                    else o = 1;
                    }
                else            // quadrant 4
                    {
                    if (xx > -yy) o = 7;
                    else o = 6;
                    }
                }
            else                // left side
                {
                if (yy > 0)     // quadrant 2
                    {
                    if (-xx > yy)  o = 3;
                    else o = 2;
                    }
                else            // quadrant 3
                    {
                    if (-xx > -yy) o = 4;
                    else o = 5;
                    }
                }  // hope I got that right!
            }
            
        // now rotate to be relative to MAV's orientation
        o += orientation; 
        if (o >= 8) o = o % 8;
        return o;
        }
        
    }
    
