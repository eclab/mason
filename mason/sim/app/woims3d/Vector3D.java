/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims3d;

import sim.util.Double3D;

public class Vector3D implements java.io.Serializable
    {

    public double x;
    public double y;
    public double z;

    public Vector3D( double x, double y, double z )
        {
        this.x = x;
        this.y = y;
        this.z = z;
        }

    public Vector3D( final Double3D d )
        {
        this.x = d.x;
        this.y = d.y;
        this.z = d.z;
        }

    public final Vector3D add( final Vector3D b )
        {
        return new Vector3D( x + b.x, y + b.y, z + b.z );
        }

    public final Vector3D add( final Double3D b )
        {
        return new Vector3D( x + b.x, y + b.y, z + b.z );
        }

    public final Vector3D subtract( final Vector3D b )
        {
        return new Vector3D( x - b.x, y - b.y, z - b.z );
        }

    public final Vector3D subtract( final Double3D b )
        {
        return new Vector3D( x - b.x, y - b.y, z - b.z );
        }

    public final Vector3D amplify( double alpha )
        {
        return new Vector3D( x * alpha, y * alpha, z * alpha );
        }

    public final Vector3D normalize()
        {
        if( x != 0 || y != 0 || z != 0)
            {
            double temp = Math.sqrt( x*x+y*y+z*z );
            return new Vector3D( x/temp, y/temp, z/temp );
            }
        else
            return new Vector3D( 0, 0, 0 );
        }

    public final double length()
        {
        return Math.sqrt( x*x+y*y+z*z );
        }

    public final Vector3D setLength( double dist )
        {
        if( dist == 0 )
            return new Vector3D( 0, 0, 0 );
        if( x == 0 && y == 0 && z == 0 )
            return new Vector3D( 0, 0, 0 );
        double temp = Math.sqrt( x*x+y*y+z*z );
        return new Vector3D( x * dist / temp, y * dist / temp, z * dist / temp );
        }

    }
