/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;

import sim.util.IntBag;

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
    // this should never change except via setTo
    protected int width;
    // this should never change except via setTo
    protected int height;
    // this should never change except via setTo
    protected int length;

    public final int getWidth() { return width; }
    
    public final int getHeight() { return height; }
    
    public final int getLength() { return length; }
    
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

    /*
     * Gets all neighbors of a location that satisfy max( abs(x-X) , abs(y-Y), abs(z-Z) ) <= d
     * Returns the x, y and z positions of the neighbors.
     */
    public void getNeighborsMaxDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsMaxDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null || zPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsMaxDistance: xPos and yPos should not be null" );
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
            final int xmin = x - dist;
            final int xmax = x + dist;
            // compute ymin and ymax for the neighborhood
            final int ymin = y - dist;
            final int ymax = y + dist;
                        
            final int zmin = z - dist;
            final int zmax = z + dist;
                        

            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                final int x_0 = stx(x0, width);
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    final int y_0 = sty(y0, height);
                    for( int z0 = zmin ; z0 <= zmax ; z0++ )
                        {
                        final int z_0 = stz(z0, length);
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmin = ((x-dist>=0)?x-dist:0);
            final int xmax =((x+dist<=width-1)?x+dist:width-1);
            // compute ymin and ymax for the neighborhood such that they are within boundaries
            final int ymin = ((y-dist>=0)?y-dist:0);
            final int ymax = ((y+dist<=height-1)?y+dist:height-1);
                        
            final int zmin = ((z-dist>=0)?z-dist:0);
            final int zmax = ((z+dist<=length-1)?z+dist:length-1);
                        
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    for( int z0 = zmin ; z0 <= zmax ; z0++ )
                        {
                        if( x0 != x || y0 != y || z0 != z )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            zPos.add( z0 );
                            }
                        }
                    }
                }
            }
        }


    /*
     * Gets all neighbors of a location that satisfy abs(x-X) + abs(y-Y) + abs(z-Z) <= d
     * Returns the x, y and z positions of the neighbors.
     */
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int z, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos, IntBag zPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null || zPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: xPos and yPos should not be null" );
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
                final int x_0 = stx(x0, width);
                // compute ymin and ymax for the neighborhood; they depend on the curreny x0 value
                final int ymax = y+(dist-((x0-x>=0)?x0-x:x-x0));
                final int ymin = y-(dist-((x0-x>=0)?x0-x:x-x0));
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = sty(y0, height);
                    final int zmax = z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0));
                    final int zmin = z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0));
                    for( int z0 = zmin; z0 <= zmax; z0++ )
                        {
                        final int z_0 = stz(z0, length);
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmax = ((x+dist<=width-1)?x+dist:width-1);
            final int xmin = ((x-dist>=0)?x-dist:0);
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                final int x_0 = x0;
                // compute ymin and ymax for the neighborhood such that they are within boundaries
                // they depend on the curreny x0 value
                final int ymax = ((y+(dist-((x0-x>=0)?x0-x:x-x0))<=height-1)?y+(dist-((x0-x>=0)?x0-x:x-x0)):height-1);
                final int ymin = ((y-(dist-((x0-x>=0)?x0-x:x-x0))>=0)?y-(dist-((x0-x>=0)?x0-x:x-x0)):0);
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = y0;
                    final int zmin = ((z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0))>=0)?z-(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0)):0);
                    final int zmax = ((z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0))<=length-1)?z+(dist-((x0-x>=0)?x0-x:x-x0)-((y0-y>=0)?y0-y:y-y0)):length-1) ;
                    for( int z0 = zmin; z0 <= zmax; z0++ )
                        {
                        final int z_0 = z0;
                        if( x_0 != x || y_0 != y || z_0 != z )
                            {
                            xPos.add( x_0 );
                            yPos.add( y_0 );
                            zPos.add( z_0 );
                            }
                        }
                    }
                }
            }
        }

    }

