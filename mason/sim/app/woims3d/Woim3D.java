/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims3d;

import sim.util.*;
import sim.engine.*;
import sim.portrayal3d.*;
import sim.portrayal3d.simple.*;

import javax.media.j3d.*;
import javax.vecmath.*;

public class Woim3D extends SimplePortrayal3D implements Steppable 
    {

    public static final double CENTROID_DISTANCE = 20 * WoimsDemo3D.DIAMETER;
    public static final double AVOID_DISTANCE = 16 * WoimsDemo3D.DIAMETER;
    public static final double COPY_SPEED_DISTANCE = 40 * WoimsDemo3D.DIAMETER;

    public static final double OBSTACLE_AVOID_COEF = 1.05;
    public static final double OBSTACLE_FAST_AVOID_COEF = 1.5;

    public static final double MAX_DISTANCE = Math.max( CENTROID_DISTANCE,
        Math.max( AVOID_DISTANCE,
            COPY_SPEED_DISTANCE ) );

    public static final double ADJUSTMENT_RATE = 0.025;
    public static final double MIN_VELOCITY = 0.25;
    public static final double MAX_VELOCITY = 0.75;

    public Woim3D() 
        {
        ond = Math.random()*6.2832;
        ondSpeed = 0.05 + Math.random()*0.15;
        for( int i = 0 ; i < colors.length ; i++ )
            colors[i] = new java.awt.Color(63 + (int)(192*(colors.length-i)/colors.length),0,0);
        //((float)(63f+(192.0*(colors.length-i))/colors.length)/255.0f, 0f, 0f );
        velocity = new Vector3D(0.05,0.05, 0.05);
        computePositions();
        }

    public final double distanceSquared( final Vector3D loc1, final Vector3D loc2 )
        {
        return( (loc1.x-loc2.x)*(loc1.x-loc2.x)+(loc1.y-loc2.y)*(loc1.y-loc2.y)+(loc1.z-loc2.z)*(loc1.z-loc2.z) );
        }

    public final double distanceSquared( final Vector3D loc1, final Double3D loc2 )
        {
        return( (loc1.x-loc2.x)*(loc1.x-loc2.x)+(loc1.y-loc2.y)*(loc1.y-loc2.y)+(loc1.z-loc2.z)*(loc1.z-loc2.z) );
        }

    public final double distanceSquared( final double x1, final double y1, final double z1, final double x2, final double y2, final double z2 )
        {
        return ((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) + (z1-z2)*(z1-z2));
        }

    Bag nearbyWoims;
    double[] distSqrTo;
    void preprocessWoims( final WoimsDemo3D state, Double3D pos, double distance )
        {
        nearbyWoims = state.woimEnvironment.getObjectsWithinDistance( pos, distance );
        if( nearbyWoims == null )
            return;
        distSqrTo = new double[nearbyWoims.numObjs];
        for( int i = 0 ; i < nearbyWoims.numObjs ; i++ )
            {
            Woim3D p = (Woim3D)(nearbyWoims.objs[i]);
            distSqrTo[i] = distanceSquared(
                pos.x,pos.y,pos.z,p.x,p.y,p.z);
            }
        }

    public Vector3D towardsFlockCenterOfMass( final WoimsDemo3D state )
        {
        if( nearbyWoims == null )
            return new Vector3D( 0, 0, 0 );
        Vector3D mean = new Vector3D( 0, 0, 0 );
        int n = 0;
        for( int i = 0 ; i < nearbyWoims.numObjs ; i++ )
            {
            if( nearbyWoims.objs[i] != this &&
                distSqrTo[i] <= CENTROID_DISTANCE * CENTROID_DISTANCE &&
                distSqrTo[i] > AVOID_DISTANCE * AVOID_DISTANCE )
                {
                Woim3D p = (Woim3D)(nearbyWoims.objs[i]);
                mean = mean.add(new Double3D(p.x,p.y,p.z));
                n++;
                }
            }
        if( n == 0 )
            return new Vector3D( 0, 0, 0 );
        else
            {
            mean = mean.amplify( 1.0 / n );
            mean = mean.subtract( woimPosition );
            return mean.normalize();
            }
        }


    public Vector3D awayFromCloseBys( final WoimsDemo3D state )
        {
        if( nearbyWoims == null )
            return new Vector3D( 0, 0, 0 );
        Vector3D away = new Vector3D( 0, 0, 0 );
        for( int i = 0 ; i < nearbyWoims.numObjs ; i++ )
            {
            if( nearbyWoims.objs[i] != this &&
                distSqrTo[i] <= AVOID_DISTANCE * AVOID_DISTANCE )
                {
                Woim3D p = (Woim3D)(nearbyWoims.objs[i]);
                Vector3D temp = woimPosition.subtract(new Double3D(p.x,p.y,p.z));
                temp = temp.normalize();
                away = away.add( temp ); 
                }
            }
        return away.normalize();
        }

    public Vector3D matchFlockSpeed( final SimState state )
        {
        if( nearbyWoims == null )
            return new Vector3D( 0, 0, 0 );
        Vector3D mean = new Vector3D( 0, 0, 0 );
        int n = 0;
        for( int i = 0 ; i < nearbyWoims.numObjs ; i++ )
            {
            if( nearbyWoims.objs[i] != this &&
                distSqrTo[i] <= COPY_SPEED_DISTANCE * COPY_SPEED_DISTANCE &&
                distSqrTo[i] > AVOID_DISTANCE * AVOID_DISTANCE )
                {
                mean = mean.add( ((Woim3D)(nearbyWoims.objs[i])).velocity );
                n++;
                }
            }
        if( n == 0 )
            return new Vector3D( 0, 0, 0 );
        else
            {
            mean = mean.amplify( 1.0 / n );
            return mean.normalize();
            }
        }

    public Vector3D randomDirection( final SimState state )
        {
        Vector3D temp = new Vector3D( 1.0 - 2.0 * state.random.nextDouble(),
            1.0 - 2.0 * state.random.nextDouble(),
            1.0 - 2.0 * state.random.nextDouble() );
        return temp.setLength( MIN_VELOCITY + state.random.nextDouble()*(MAX_VELOCITY-MIN_VELOCITY) );
        }

    double ond;
    double ondSpeed;
    public Vector3D niceOndulation( final SimState state )
        {
        ond += ondSpeed;
        if( ond > 7 )
            ond -= 6.2832;
        double angle = Math.cos( ond );
        Vector3D temp = velocity;
        double velA = Math.atan2( temp.y, temp.x );
        velA = velA + 1.5708*angle;
        return new Vector3D( Math.cos(velA), Math.sin(velA), 0 );
        }

    public Vector3D avoidObstacles( final SimState state )
        {
        double[][] info = WoimsDemo3D.obstInfo;
        if( info == null || info.length == 0 )
            return new Vector3D( 0, 0, 0 );
            
        Vector3D away = new Vector3D( 0, 0, 0 );
        for( int i = 0 ; i < info.length ; i++ )
            {
            double dist = Math.sqrt( (woimPosition.x-info[i][1])*(woimPosition.x-info[i][1]) +
                (woimPosition.y-info[i][2])*(woimPosition.y-info[i][2]) +
                (woimPosition.z-info[i][3])*(woimPosition.z-info[i][3]) );
            if( dist <= info[i][0]+AVOID_DISTANCE )
                {
                Vector3D temp = woimPosition.subtract( new Vector3D( info[i][1], info[i][2], info[i][3] ) );
                temp = temp.normalize();
                away = away.add( temp ); 
                }
            }
        return away.normalize();
        }

    protected Vector3D woimPosition = new Vector3D( 0, 0, 0 );

    public void step( final SimState state )
        {
        WoimsDemo3D bd = (WoimsDemo3D)state;
                {
                Double3D temp = new Double3D(x,y,z);  //bd.environment.getObjectLocation( this );
                woimPosition.x = x;
                woimPosition.y = y;
                woimPosition.z = z;
                preprocessWoims( bd, temp, MAX_DISTANCE );
                }
        Vector3D vel = new Vector3D( 0, 0, 0 );
        vel = vel.add( avoidObstacles(bd).amplify( 1.5 ) );
        vel = vel.add( towardsFlockCenterOfMass(bd).amplify(0.5) );
        vel = vel.add( matchFlockSpeed(bd).amplify(0.5) );
        vel = vel.add( awayFromCloseBys(bd).amplify(1.5) );
        if( vel.length() <= 1.0 )
            {
            vel = vel.add( niceOndulation(bd).amplify(0.5) );
            vel = vel.add( randomDirection(bd).amplify(0.25) );
            }

        double vl = vel.length();
        if( vl < MIN_VELOCITY )
            vel = vel.setLength( MIN_VELOCITY );
        else if( vl > MAX_VELOCITY )
            vel = vel.setLength( MAX_VELOCITY );
        vel = new Vector3D( (1-ADJUSTMENT_RATE)*velocity.x + ADJUSTMENT_RATE*vel.x,
            (1-ADJUSTMENT_RATE)*velocity.y + ADJUSTMENT_RATE*vel.y,
            (1-ADJUSTMENT_RATE)*velocity.z + ADJUSTMENT_RATE*vel.z );
        velocity = vel;
        Double3D desiredPosition = new Double3D( woimPosition.x+vel.x*WoimsDemo3D.TIMESTEP,
            woimPosition.y+vel.y*WoimsDemo3D.TIMESTEP,
            woimPosition.z+vel.z*WoimsDemo3D.TIMESTEP );
        bd.setObjectLocation( this, desiredPosition );
        }

    public double x;
    public double y;
    public double z;
    
    final static int numLinks = 7;
    Vector3d[] lastPos = new Vector3d[numLinks];
    Vector3d[] lastPosRel = new Vector3d[numLinks];
    java.awt.Color[] colors = new java.awt.Color[numLinks];

    protected double orientation;
    protected Vector3D velocity = new Vector3D( 0, 0, 0 );
    protected Vector3D acceleration = new Vector3D( 0, 0, 0 );
    



    public void computePositions()
        {
        double centerx, centery, centerz;

        // the head!
        centerx = x + 1.0/2.0;
        centery = y + 1.0/2.0;
        centerz = z + 1.0/2.0;
//        drawLink( graphics, centerx, centery, rx, ry, colors[0] );
        lastPos[0] = new Vector3d( centerx, centery, centerz );
        Vector3d temp  = new Vector3d();
        Vector3d velocity3d = new Vector3d(velocity.x, velocity.y, velocity.z);
        for( int i = 1 ; i < numLinks ; i++ )
            {
            if( lastPos[i] == null )
                {
                temp.scale(-1.0, velocity3d);
                temp.normalize();
                centerx = lastPos[i-1].x+1.0*temp.x;
                centery = lastPos[i-1].y+1.0*temp.y;
                centerz = lastPos[i-1].z+1.0*temp.z;
//                drawLink( graphics, centerx, centery, rx, ry, colors[i] );
                lastPos[i] = new Vector3d( centerx, centery, centerz );
                }
            else
                {
                temp.sub(lastPos[i-1], lastPos[i] );
                temp.scale(1.0/temp.length());
                temp.sub(lastPos[i-1], temp);
//                drawLink( graphics, temp.x, temp.y, rx, ry, colors[i] );
                lastPos[i] = new Vector3d( temp.x, temp.y, temp.z );
                }
//                      lastPos[i].negate();
            }
        for( int i = 0 ; i < lastPosRel.length ; i++ )
            {
            lastPosRel[i] = new Vector3d( lastPos[i].x-lastPos[0].x,
                lastPos[i].y-lastPos[0].y,
                lastPos[i].z-lastPos[0].z );
            }
        }
        
    public TransformGroup createModel(Object obj)
        {
        TransformGroup globalTG = new TransformGroup();
//              globalTG.addChild(new ColorCube());
        for(int i=0; i<numLinks; ++i)
            {
            // we set the number of divisions to 6 and it's quite a bit faster and
            // less memory-hungry.  The default is 15.
            SpherePortrayal3D s = new SpherePortrayal3D(colors[i],1.0f,6);
            s.setParentPortrayal(parentPortrayal);
            TransformGroup localTG = s.getModel(obj, null);
            
            localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            globalTG.addChild(localTG);
            }
        globalTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        return globalTG;
        }
        
//      Transform3D tmpT3d = new Transform3D();
        
    public TransformGroup getModel(Object obj, TransformGroup transf)
        {
        computePositions();
        if(transf==null) return createModel(obj);
        for(int i=0; i<transf.numChildren(); ++i)
            {
            Transform3D tmpT3d = new Transform3D();
            tmpT3d.setTranslation(lastPosRel[i]);
            ((TransformGroup)transf.getChild(i)).setTransform(tmpT3d);
            }
        return transf;
        }
    }
