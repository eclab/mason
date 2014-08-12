/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;

import sim.util.*;
import java.util.*;

/**
   A concrete implementation of the Grid3D methods; used by several subclasses.
   Note that you should avoid calling these methods from an object of type Grid3D; instead
   try to call them from something more concrete (AbstractGrid3D or SparseGrid3D).  
   Otherwise they will not get inlined.  For example,

   <pre><tt>
   Grid3D foo = ... ;
   foo.tx(4);  // will not get inlined

   AbstractGrid3D bar = ...;
   bar.tx(4);  // WILL get inlined
   </tt></pre>

*/

public abstract class AbstractGrid3D implements Grid3D
    {
    private static final long serialVersionUID = 1;

    // this should never change except via setTo
    protected int width;
    // this should never change except via setTo
    protected int height;
    // this should never change except via setTo
    protected int length;

    public final int getWidth() { return width; }
    
    public final int getHeight() { return height; }
    
    public final int getLength() { return length; }
    
    public Map buildMap(Map other) { return new HashMap(other); }
    public Map buildMap(int size) 
        {
        if (size <= ANY_SIZE) return new HashMap();
        else return new HashMap(size);
        }

    /*
      public final int tx(final int x) 
      { 
      final int width = this.width; 
      if (x >= 0) return (x % width); 
      final int width2 = (x % width) + height;
      if (width2 < width) return width2;
      return 0;
      }
    */

    // slight revision for more efficiency
    public final int tx(int x) 
        { 
        final int width = this.width;
        if (x >= 0 && x < width) return x;  // do clearest case first
        x = x % width;
        if (x < 0) x = x + width;
        return x;
        }
        
    /*
      public final int ty(final int y) 
      { 
      final int height = this.height; 
      if (y >= 0) return (y % height); 
      final int height2 = (y % height) + height;
      if (height2 < height) return height2;
      return 0;
      }
    */
        
    // slight revision for more efficiency
    public final int ty(int y) 
        { 
        final int height = this.height;
        if (y >= 0 && y < height) return y;  // do clearest case first
        y = y % height;
        if (y < 0) y = y + height;
        return y;
        }

/*
  public final int tz(final int z) 
  { 
  final int length = this.length; 
  if (z >= 0) return (z % length); 
  final int length2 = (z % length) + length;
  if (length2 < length) return length2;
  return 0;
  }
*/

    // slight revision for more efficiency
    public final int tz(int z) 
        { 
        final int length = this.length;
        if (z >= 0 && z < length) return z;  // do clearest case first
        z = z % length;
        if (z < 0) z = z + height;
        return z;
        }

    public final int stx(final int x) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
    
    public final int sty(final int y) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    public final int stz(final int z) 
        { if (z >= 0) { if (z < length) return z ; return z - length; } return z + length; }

    // faster version
    final int stx(final int x, final int width) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
        
    // faster version
    final int sty(final int y, final int height) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    // faster version
    public final int stz(final int z, final int length) 
        { if (z >= 0) { if (z < length) return z ; return z - length; } return z + length; }



    // this internal version of tx is arranged to be 34 bytes.  It first tries stx, then tx.
    int tx(int x, int width, int widthtimestwo, int xpluswidth, int xminuswidth) 
        {
        if (x >= -width && x < widthtimestwo)
            {
            if (x < 0) return xpluswidth;
            if (x < width) return x;
            return xminuswidth;
            }
        return tx2(x, width);
        }

    // used internally by the internal version of tx above.  Do not call directly.
    int tx2(int x, int width)
        {
        x = x % width;
        if (x < 0) x = x + width;
        return x;
        }

    // this internal version of ty is arranged to be 34 bytes.  It first tries sty, then ty.
    int ty(int y, int height, int heighttimestwo, int yplusheight, int yminusheight) 
        {
        if (y >= -height && y < heighttimestwo)
            {
            if (y < 0) return yplusheight;
            if (y < height) return y;
            return yminusheight;
            }
        return ty2(y, height);
        }
        
    // used internally by the internal version of ty above.  Do not call directly.
    int ty2(int y, int height)
        {
        y = y % height;
        if (y < 0) y = y + height;
        return y;
        }
        
    // this internal version of tz is arranged to be 34 bytes.  It first tries stz, then tz.
    int tz(int z, int length, int lengthtimestwo, int zpluslength, int zminuslength) 
        {
        if (z >= -length && z < lengthtimestwo)
            {
            if (z < 0) return zpluslength;
            if (z < length) return z;
            return zminuslength;
            }
        return tz2(z, length);
        }
        
    // used internally by the internal version of ty above.  Do not call directly.
    int tz2(int z, int length)
        {
        z = z % length;
        if (z < 0) z = z + length;
        return z;
        }
        

    protected void removeOrigin(int x, int y, int z, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        int size = xPos.size();
        for(int i = 0; i <size; i++)
            {
            if (xPos.get(i) == x && yPos.get(i) == y && zPos.get(i) == z)
                {
                xPos.remove(i);
                yPos.remove(i);
                zPos.remove(i);
                return;
                }
            }
        }
        
    // only removes the first occurence
    protected void removeOriginToroidal(int x, int y, int z, IntBag xPos, IntBag yPos, IntBag zPos)
        {
        int size = xPos.size();
        x = tx(x, width, width*2, x+width, x-width);
        y = ty(y, height, height*2, y+height, y-height);
        z = tz(z, length, length*2, z+length, z-length);
        
        for(int i = 0; i <size; i++)
            {
            if (tx(xPos.get(i), width, width*2, x+width, x-width) == x && 
                ty(yPos.get(i), height, height*2, y+height, y-height) == y &&
                tz(zPos.get(i), length, length*2, z+length, z-length) == z)
                {
                xPos.remove(i);
                yPos.remove(i);
                zPos.remove(i);
                return;
                }
            }
        }



    /** @deprecated */
    public void getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        getMooreLocations(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos, zPos);
        }

    public void getMooreLocations( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);

        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid3D.BOUNDED, Grid3D.UNBOUNDED, or Grid3D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null || zPos == null)
            {
            throw new RuntimeException( "xPos and yPos and zPos should not be null" );
            }

        xPos.clear();
        yPos.clear();
        zPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;
        final int length = this.length;

        // for toroidal environments the code will be different because of wrapping arround
        if( toroidal )
            {
            // compute xmin and xmax for the neighborhood
            int xmin = x - dist;
            int xmax = x + dist;

            // next: is xmax - xmin humongous?  If so, no need to continue wrapping around
            if (xmax - xmin >= width)  // too wide
                xmax = xmin + width - 1;
            
            // compute ymin and ymax for the neighborhood
            int ymin = y - dist;
            int ymax = y + dist;
                        
            // next: is ymax - ymin humongous?  If so, no need to continue wrapping around
            if (ymax - ymin >= height)  // too wide
                ymax = ymin + height - 1;

            // compute zmin and zmax for the neighborhood
            int zmin = z - dist;
            int zmax = z + dist;

            // next: is zmax - zmin humongous?  If so, no need to continue wrapping around
            if (zmax - zmin >= length)  // too wide
                zmax = zmin + length - 1;
                        

            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                final int x_0 = tx(x0, width, width*2, x0+width, x0-width);
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    final int y_0 = ty(y0, height, height*2, y0+height, y0-height);
                    for( int z0 = zmin ; z0 <= zmax ; z0++ )
                        {
                        final int z_0 = tz(z0, length, length*2, z0+length, z0-length);
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,z,xPos,yPos,zPos); 
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmin = ((x-dist>=0 || !bounded)?x-dist:0);
            final int xmax =((x+dist<=width-1 || !bounded)?x+dist:width-1);
            // compute ymin and ymax for the neighborhood such that they are within boundaries
            final int ymin = ((y-dist>=0 || !bounded)?y-dist:0);
            final int ymax = ((y+dist<=height-1 || !bounded)?y+dist:height-1);
                        
            final int zmin = ((z-dist>=0 || !bounded)?z-dist:0);
            final int zmax = ((z+dist<=length-1 || !bounded)?z+dist:length-1);

            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    for( int z0 = zmin ; z0 <= zmax ; z0++ )
                        {
                        xPos.add( x0 );
                        yPos.add( y0 );
                        zPos.add( z0 );
                        }
                    }
                }
            if (!includeOrigin) removeOrigin(x,y,z,xPos,yPos,zPos); 
            }
        }


    /** @deprecated */
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        getVonNeumannLocations(x, y, z, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos, zPos);
        }


    public void getVonNeumannLocations( final int x, final int y, final int z, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);

        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid3D.BOUNDED, Grid3D.UNBOUNDED, or Grid3D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null || zPos == null)
            {
            throw new RuntimeException( "xPos and yPos and zPos should not be null" );
            }

        xPos.clear();
        yPos.clear();
        zPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;
        final int length = this.length;

        // for toroidal environments the code will be different because of wrapping arround
        if( toroidal )
            {
            // compute xmin and xmax for the neighborhood
            final int xmax = x+dist;
            final int xmin = x-dist;
            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                final int x_0 = tx(x0, width, width*2, x0+width, x0-width);
                // compute ymin and ymax for the neighborhood; they depend on the curreny x0 value
                final int ymax = y+(dist-((x0-x>=0)?x0-x:x-x0));
                final int ymin = y-(dist-((x0-x>=0)?x0-x:x-x0));
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = ty(y0, height, height*2, y0+height, y0-height);
                    final int zmax = z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0));
                    final int zmin = z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0));
                    for( int z0 = zmin; z0 <= zmax; z0++ )
                        {
                        final int z_0 = tz(z0, length, length*2, z0+length, z0-length);
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            if (dist * 2 >= width || dist * 2 >= height || dist * 2 >= length)  // too big, will have to remove duplicates
                {
                int sz = xPos.size();
                Map map = buildMap(sz);
                for(int i = 0 ; i < sz; i++)
                    {
                    Double3D elem = new Double3D(xPos.get(i), yPos.get(i), zPos.get(i));
                    if (map.containsKey(elem)) // already there
                        {
                        xPos.remove(i);
                        yPos.remove(i);
                        zPos.remove(i);
                        i--;
                        sz--;
                        }
                    else
                        {
                        map.put(elem, elem);
                        }
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,z,xPos,yPos,zPos); 
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmax = ((x+dist<=width-1 || !bounded)?x+dist:width-1);
            final int xmin = ((x-dist>=0 || !bounded)?x-dist:0);
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                final int x_0 = x0;
                // compute ymin and ymax for the neighborhood such that they are within boundaries
                // they depend on the curreny x0 value
                final int ymax = ((y+(dist-((x0-x>=0)?x0-x:x-x0))<=height-1 || !bounded)?y+(dist-((x0-x>=0)?x0-x:x-x0)):height-1);
                final int ymin = ((y-(dist-((x0-x>=0)?x0-x:x-x0))>=0 || !bounded)?y-(dist-((x0-x>=0)?x0-x:x-x0)):0);
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = y0;
                    final int zmin = ((z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0))>=0 || !bounded)?z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0)):0);
                    final int zmax = ((z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0))<=length-1 || !bounded)?z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0)):length-1) ;
                    for( int z0 = zmin; z0 <= zmax; z0++ )
                        {
                        final int z_0 = z0;
                        xPos.add( x_0 );
                        yPos.add( y_0 );
                        zPos.add( z_0 );
                        }
                    }
                }
            if (!includeOrigin) removeOrigin(x,y,z,xPos,yPos,zPos); 
            }
        }



    double ds(double d1x, double d1y, double d1z, double d2x, double d2y, double d2z)
        {
        return ((d1x - d2x) * (d1x - d2x) + (d1y - d2y) * (d1y - d2y) + (d1z - d2z) * (d1z - d2z));
        }
    
    boolean within(double d1x, double d1y, double d1z, double d2x, double d2y, double d2z, double distanceSquared, boolean closed)
        {
        double d= ds(d1x, d1y, d1z, d2x, d2y, d2z);
        return (d < distanceSquared || (d == distanceSquared && closed));
        }
        
    public void getRadialLocations( final int x, final int y, final int z, final double dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        getRadialLocations(x, y, z, dist, mode, includeOrigin, Grid3D.ANY, true, xPos, yPos, zPos);
        }
        

    public void getRadialLocations( final int x, final int y, final int z, final double dist, int mode, boolean includeOrigin, int measurementRule, boolean closed, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        boolean toroidal = (mode == TOROIDAL);

        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }
            
        if (measurementRule != Grid3D.ANY && measurementRule != Grid3D.ALL && measurementRule != Grid3D.CENTER)
            {
            throw new RuntimeException(" Measurement rule must be one of ANY, ALL, or CENTER" );
            }
                
        // grab the rectangle
        getMooreLocations(x,y, z, (int) Math.ceil(dist + 0.5), mode, includeOrigin, xPos, yPos, zPos);
        int len = xPos.size();
        double distsq = dist * dist;
        

        int width = this.width;
        int height = this.height;
        int length = this.length;
        int widthtimestwo = width * 2;
        int heighttimestwo = height * 2;
        int lengthtimestwo = length * 2;
        

        for(int i = 0; i < len; i++)
            {
            int xp = xPos.get(i);
            int yp = yPos.get(i);
            int zp = zPos.get(i);
            boolean remove = false;
                
            if (measurementRule == Grid3D.ANY)
                {
                if (z == zp)
                    {
                    if (x == xp)
                        {
                        if (y < yp)
                            {
                            double d = (yp - 0.5) -  y;
                            remove = !(d < dist || (d == dist && closed));
                            }
                        else
                            {
                            double d = -((yp - 0.5) - y);
                            remove = !(d < dist || (d == dist && closed));
                            }
                        }
                    else if (y == yp)
                        {
                        if (x < xp)
                            {
                            double d = (xp - 0.5) - x;
                            remove = !(d < dist || (d == dist && closed));
                            }
                        else
                            {
                            double d = -((xp - 0.5) - x);
                            remove = !(d < dist || (d == dist && closed));
                            }
                        }
                    }
                else if (x == xp)
                    {
                    if (y == yp)
                        {
                        if (z  < zp)
                            {
                            double d = (zp - 0.5) -  z;
                            remove = !(d < dist || (d == dist && closed));
                            }
                        else
                            {
                            double d = -((zp - 0.5) - z);
                            remove = !(d < dist || (d == dist && closed));
                            }
                        }
                    }
                else if (z < zp)
                    {
                    if (x < xp)
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp-0.5,yp-0.5,zp-0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp-0.5,yp+0.5,zp-0.5,distsq,closed);
                        }
                    else
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp+0.5,yp-0.5,zp-0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp+0.5,yp+0.5,zp-0.5,distsq,closed);
                        }
                    }
                else
                    {
                    if (x < xp)
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp-0.5,yp-0.5,zp+0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp-0.5,yp+0.5,zp+0.5,distsq,closed);
                        }
                    else
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp+0.5,yp-0.5,zp+0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp+0.5,yp+0.5,zp+0.5,distsq,closed);
                        }
                    }
                }
            else if (measurementRule == Grid3D.ALL)
                {
                if (z < zp)
                    {
                    if (x < xp)
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp+0.5,yp+0.5,zp+0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp+0.5,yp-0.5,zp+0.5,distsq,closed);
                        }
                    else
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp-0.5,yp+0.5,zp+0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp-0.5,yp-0.5,zp+0.5,distsq,closed);
                        }
                    }
                else
                    {
                    if (x < xp)
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp+0.5,yp+0.5,zp-0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp+0.5,yp-0.5,zp-0.5,distsq,closed);
                        }
                    else
                        {
                        if (y < yp)
                            remove = !within(x,y,z,xp-0.5,yp+0.5,zp-0.5,distsq,closed);
                        else
                            remove = !within(x,y,z,xp-0.5,yp-0.5,zp-0.5,distsq,closed);
                        }
                    }

                }
            else // (measurementRule == Grid3D.CENTER)
                {
                remove = !within(x,y,z,xp,yp,zp,distsq,closed);
                }
                
            if (remove)
                { xPos.remove(i); yPos.remove(i); zPos.remove(i); i--; len--; }
            else if (toroidal) // need to convert to toroidal position
                { 
                int _x = xPos.get(i);
                int _y = yPos.get(i);
                int _z = zPos.get(i);
                xPos.set(i, tx(_x, width, widthtimestwo, _x + width, _x - width));
                yPos.set(i, ty(_y, height, heighttimestwo, _y + width, _y - width));
                zPos.set(i, tz(_z, length, lengthtimestwo, _z + length, _z - length));
                }

            }
        }


    protected void checkBounds(Grid3D other)
        {
        if (getHeight() != other.getHeight() || getWidth() != other.getWidth() || getLength() != other.getLength())
            throw new IllegalArgumentException("Grids must be the same dimensions.");
        }
    

    }

