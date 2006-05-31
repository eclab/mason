/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims;
import java.io.Serializable;
import sim.util.Double2D;

public /*strictfp*/ class Vector2D implements Serializable
    {

    public double x;
    public double y;

    public Vector2D( double x, double y )
        {
        this.x = x;
        this.y = y;
        }

    public Vector2D( final Double2D d )
        {
        this.x = d.x;
        this.y = d.y;
        }

    public final Vector2D add( final Vector2D b )
        {
        return new Vector2D( x + b.x, y + b.y );
        }

    public final Vector2D add( final Double2D b )
        {
        return new Vector2D( x + b.x, y + b.y );
        }

    public final Vector2D subtract( final Vector2D b )
        {
        return new Vector2D( x - b.x, y - b.y );
        }

    public final Vector2D subtract( final Double2D b )
        {
        return new Vector2D( x - b.x, y - b.y );
        }

    public final Vector2D amplify( double alpha )
        {
        return new Vector2D( x * alpha, y * alpha );
        }

    public final Vector2D normalize()
        {
        if( x != 0 || y != 0 )
            {
            double temp = /*Strict*/Math.sqrt( x*x+y*y );
            return new Vector2D( x/temp, y/temp );
            }
        else
            return new Vector2D( 0, 0 );
        }

    public final double length()
        {
        return /*Strict*/Math.sqrt( x*x+y*y );
        }

    public final Vector2D setLength( double dist )
        {
        if( dist == 0 )
            return new Vector2D( 0, 0 );
        if( x == 0 && y == 0 )
            return new Vector2D( 0, 0 );
        double temp = /*Strict*/Math.sqrt( x*x+y*y );
        return new Vector2D( x * dist / temp, y * dist / temp );
        }

    }
