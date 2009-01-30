/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims;

import sim.util.*;
import sim.engine.*;
import java.awt.*;
import sim.portrayal.*;
import java.awt.geom.*;

public /*strictfp*/ class Woim extends SimplePortrayal2D implements Steppable
    {

    public static final double CENTROID_DISTANCE = 20 * WoimsDemo.DIAMETER;
    public static final double AVOID_DISTANCE = 16 * WoimsDemo.DIAMETER;
    public static final double COPY_SPEED_DISTANCE = 40 * WoimsDemo.DIAMETER;

    public static final double OBSTACLE_AVOID_COEF = 1.05;
    public static final double OBSTACLE_FAST_AVOID_COEF = 1.5;

    public static final double MAX_DISTANCE = /*Strict*/Math.max( CENTROID_DISTANCE,
        /*Strict*/Math.max( AVOID_DISTANCE,
            COPY_SPEED_DISTANCE ) );

    public static final double ADJUSTMENT_RATE = 0.025;
    public static final double MIN_VELOCITY = 0.25;
    public static final double MAX_VELOCITY = 0.75;

    // initialize the woim
    public Woim() 
        {
        ond = /*Strict*/Math.random()*6.2832;
        ondSpeed = 0.05 + /*Strict*/Math.random()*0.15;
        setNumberOfLinks( numLinks );
        }

    // squared distance between two points
    public final double distanceSquared( final Vector2D loc1, final Vector2D loc2 )
        {
        return( (loc1.x-loc2.x)*(loc1.x-loc2.x)+(loc1.y-loc2.y)*(loc1.y-loc2.y) );
        }

    // squared distance between two points
    public final double distanceSquared( final Vector2D loc1, final Double2D loc2 )
        {
        return( (loc1.x-loc2.x)*(loc1.x-loc2.x)+(loc1.y-loc2.y)*(loc1.y-loc2.y) );
        }

    // squared distance between two points
    public final double distanceSquared( final double x1, final double y1, final double x2, final double y2 )
        {
        return ((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
        }

    // dot product between two vectors
    public final double dotproduct( final Vector2D loc1, final Vector2D loc2 )
        {
        return loc1.x*loc2.x+loc1.y*loc2.y;
        }

    // initializes distances to closeby woims. it should be called a single time in the step function at each timestep.
    Bag nearbyWoims;
    double[] distSqrTo;
    void preprocessWoims( final WoimsDemo state, Double2D pos, double distance )
        {
        nearbyWoims = state.woimsEnvironment.getObjectsWithinDistance( pos, distance );
        if( nearbyWoims == null )
            {
            return;
            }
        distSqrTo = new double[nearbyWoims.numObjs];
        for( int i = 0 ; i < nearbyWoims.numObjs ; i++ )
            {
            Woim p = (Woim)(nearbyWoims.objs[i]);
            distSqrTo[i] = distanceSquared(
                pos.x,pos.y,p.x,p.y);
            }
        }

    // returns a vector towards the center of the flock
    public Vector2D towardsFlockCenterOfMass( final WoimsDemo state )
        {
        if( nearbyWoims == null )
            return new Vector2D( 0, 0 );
        Vector2D mean = new Vector2D( 0, 0 );
        int n = 0;
        for( int i = 0 ; i < nearbyWoims.numObjs ; i++ )
            {
            if( nearbyWoims.objs[i] != this &&
                distSqrTo[i] <= CENTROID_DISTANCE * CENTROID_DISTANCE &&
                distSqrTo[i] > AVOID_DISTANCE * AVOID_DISTANCE )
                {
                Woim p = (Woim)(nearbyWoims.objs[i]);
                mean = mean.add(new Double2D(p.x,p.y));
                n++;
                }
            }
        if( n == 0 )
            return new Vector2D( 0, 0 );
        else
            {
            mean = mean.amplify( 1.0 / n );
            mean = mean.subtract( woimPosition );
            return mean.normalize();
            }
        }

    // returns a vector away from woims that are too close
    public Vector2D awayFromCloseBys( final WoimsDemo state )
        {
        if( nearbyWoims == null )
            return new Vector2D( 0, 0 );
        Vector2D away = new Vector2D( 0, 0 );
        for( int i = 0 ; i < nearbyWoims.numObjs ; i++ )
            {
            if( nearbyWoims.objs[i] != this &&
                distSqrTo[i] <= AVOID_DISTANCE * AVOID_DISTANCE )
                {
                Woim p = (Woim)(nearbyWoims.objs[i]);
                Vector2D temp = woimPosition.subtract(new Double2D(p.x,p.y));
                temp = temp.normalize();
                away = away.add( temp ); 
                }
            }
        return away.normalize();
        }

    // returns the mean speed of the nearby woims
    public Vector2D matchFlockSpeed( final SimState state )
        {
        if( nearbyWoims == null )
            return new Vector2D( 0, 0 );
        Vector2D mean = new Vector2D( 0, 0 );
        int n = 0;
        for( int i = 0 ; i < nearbyWoims.numObjs ; i++ )
            {
            if( nearbyWoims.objs[i] != this &&
                distSqrTo[i] <= COPY_SPEED_DISTANCE * COPY_SPEED_DISTANCE &&
                distSqrTo[i] > AVOID_DISTANCE * AVOID_DISTANCE )
                {
                mean = mean.add( ((Woim)(nearbyWoims.objs[i])).velocity );
                n++;
                }
            }
        if( n == 0 )
            return new Vector2D( 0, 0 );
        else
            {
            mean = mean.amplify( 1.0 / n );
            return mean.normalize();
            }
        }

    // returns a random directions
    public Vector2D randomDirection( final SimState state )
        {
        Vector2D temp = new Vector2D( 1.0 - 2.0 * state.random.nextDouble(),
            1.0 - 2.0 * state.random.nextDouble() );
        return temp.setLength( MIN_VELOCITY + state.random.nextDouble()*(MAX_VELOCITY-MIN_VELOCITY) );
        }

    // returns the oscilation vector
    double ond;
    double ondSpeed;
    public Vector2D niceUndulation( final SimState state )
        {
        ond += ondSpeed;
        if( ond > 7 )
            ond -= 6.2832;
        double angle = /*Strict*/Math.cos( ond );
        Vector2D temp = velocity;
        double velA = /*Strict*/Math.atan2( temp.y, temp.x );
        velA = velA + 1.5708*angle;
        return new Vector2D( /*Strict*/Math.cos(velA), /*Strict*/Math.sin(velA) );
        }

    // returns a direction away from obstacles
    public Vector2D avoidObstacles( final SimState state )
        {
        double[][] info = WoimsDemo.obstInfo;
        if( info == null || info.length == 0 )
            return new Vector2D( 0, 0 );
            
        Vector2D away = new Vector2D( 0, 0 );
        for( int i = 0 ; i < info.length ; i++ )
            {
            double dist = /*Strict*/Math.sqrt( (woimPosition.x-info[i][1])*(woimPosition.x-info[i][1]) +
                (woimPosition.y-info[i][2])*(woimPosition.y-info[i][2]) );
            if( dist <= info[i][0]+AVOID_DISTANCE )
                {
                Vector2D temp = woimPosition.subtract( new Vector2D( info[i][1], info[i][2] ) );
                temp = temp.normalize();
                away = away.add( temp ); 
                }
            }
        return away.normalize();
        }

    protected Vector2D woimPosition = new Vector2D( 0, 0 );

    public void step( final SimState state )
        {
        WoimsDemo bd = (WoimsDemo)state;
                {
                Double2D temp = new Double2D(x,y);  //bd.environment.getObjectLocation( this );
                woimPosition.x = x;
                woimPosition.y = y;
                preprocessWoims( bd, temp, MAX_DISTANCE );
                }

        Vector2D vel = new Vector2D( 0, 0 );
        vel = vel.add( avoidObstacles(bd).amplify( 1.5 ) );
        vel = vel.add( towardsFlockCenterOfMass(bd).amplify(0.5) );
        vel = vel.add( matchFlockSpeed(bd).amplify(0.5) );
        vel = vel.add( awayFromCloseBys(bd).amplify(1.5) );
        if( vel.length() <= 1.0 )
            {
            vel = vel.add( niceUndulation(bd).amplify(0.5) );
            vel = vel.add( randomDirection(bd).amplify(0.25) );
            }

        double vl = vel.length();
        if( vl < MIN_VELOCITY )
            vel = vel.setLength( MIN_VELOCITY );
        else if( vl > MAX_VELOCITY )
            vel = vel.setLength( MAX_VELOCITY );
        vel = new Vector2D( (1-ADJUSTMENT_RATE)*velocity.x + ADJUSTMENT_RATE*vel.x,
            (1-ADJUSTMENT_RATE)*velocity.y + ADJUSTMENT_RATE*vel.y );
        velocity = vel;
        Double2D desiredPosition = new Double2D( woimPosition.x+vel.x*WoimsDemo.TIMESTEP,
            woimPosition.y+vel.y*WoimsDemo.TIMESTEP );
        bd.setObjectLocation( this, desiredPosition );
        updateLinkPosition();
        }

    public double x;
    public double y;
    
    Vector2D[] lastPos;
    Color[] colors;
    int numLinks = 7;
    public int getNumberOfLinks() { return numLinks; }
    public void setNumberOfLinks( int n )
        {
        if( numLinks == n && colors != null )
            return;
        if( n <= 0 )
            return;
        if( n > WoimsDemo.MAX_LINKS )
            n = WoimsDemo.MAX_LINKS;
        numLinks = n;
        lastPos = new Vector2D[numLinks];
        colors = new Color[numLinks];
        for( int i = 0 ; i < colors.length ; i++ )
            colors[i] = new Color( (int) (63+(192.0*(colors.length-i))/colors.length), 0, 0 );
        updateLinkPosition();
        }

    protected double orientation;
    protected Vector2D velocity = new Vector2D( 0, 0 );
    protected Vector2D acceleration = new Vector2D( 0, 0 );

    // drawing graphics
    void drawLink( final Graphics2D graphics, double x, double y, double rx, double ry, final Color color)
        {
        graphics.setColor( color );
        graphics.fillOval( (int)(x-rx/2.0), (int)(y-ry/2.0), (int)(rx), (int)(ry));
        }

    public void updateLinkPosition()
        {
        double centerx, centery;


        // the head!
        centerx = x;
        centery = y;
        lastPos[0] = new Vector2D( centerx, centery );
        for( int i = 1 ; i < numLinks ; i++ )
            {
            if( lastPos[i] == null )
                {
                Vector2D temp = velocity.normalize().amplify(-1.0);
                centerx = lastPos[i-1].x+1.0*temp.x;
                centery = lastPos[i-1].y+1.0*temp.y;
                lastPos[i] = new Vector2D( centerx, centery );
                }
            else
                {
                Vector2D temp = lastPos[i-1].subtract( lastPos[i] );
                temp = temp.setLength( 1.0 );
                temp = lastPos[i-1].subtract( temp );
                lastPos[i] = temp;
                }
            }
        }

    public final void draw(Object object,  final Graphics2D graphics, final DrawInfo2D info)
        {
        if( lastPos == null )
            return;
        for( int i = 0 ; i < numLinks ; i++ )
            if( lastPos[i] != null )
                drawLink( graphics,
                    info.draw.x+info.draw.width*(lastPos[i].x-lastPos[0].x),
                    info.draw.y+info.draw.height*(lastPos[i].y-lastPos[0].y),
                    info.draw.width,
                    info.draw.height,
                    colors[i] );
        }

    /** If drawing area intersects selected area, add last portrayed object to the bag */
    public boolean hitObject(Object object, DrawInfo2D info)
        {
        if( lastPos == null )
            return false;
        for( int i = 0 ; i < numLinks ; i++ )
            if( lastPos[i] != null )
                {
                Ellipse2D.Double ellipse = new Ellipse2D.Double(
                    info.draw.x+info.draw.width*(lastPos[i].x-lastPos[0].x),
                    info.draw.y+info.draw.height*(lastPos[i].y-lastPos[0].y),
                    info.draw.width,
                    info.draw.height );
                if( ellipse.intersects( info.clip.x, info.clip.y, info.clip.width, info.clip.height ) )
                    {
                    return true;
                    }
                }
        return false;
        }
    }
