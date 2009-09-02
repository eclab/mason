/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;

import sim.util.IntBag;

/**
   A concrete implementation of the Grid2D methods; used by several subclasses.
   Note that you should avoid calling these methods from an object of type Grid2D; instead
   try to call them from something more concrete (AbstractGrid2D or SparseGrid2D).
   Otherwise they will not get inlined.  For example,

   <pre><tt>
   Grid2D foo = ... ;
   foo.tx(4);  // will not get inlined

   AbstractGrid2D bar = ...;
   bar.tx(4);  // WILL get inlined
   
   ObjectGrid2D baz = ...;  // (assuming we're an ObjectGrid2D)
   baz.tx(4);   // WILL get inlined
   </tt></pre>

*/

public abstract class AbstractGrid2D implements Grid2D
    {
    // this should never change except via setTo
    protected int width;
    // this should never change except via setTo
    protected int height;

    public final int getWidth() { return width; }
    
    public final int getHeight() { return height; }
    
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
        
    public final int stx(final int x) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
        
    public final int sty(final int y) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }
        
    // faster internal version
    final int stx(final int x, final int width) 
        { if (x >= 0) { if (x < width) return x; return x - width; } return x + width; }
        
    // faster internal version
    final int sty(final int y, final int height) 
        { if (y >= 0) { if (y < height) return y ; return y - height; } return y + height; }

    public final int ulx(final int x, final int y) { return x - 1; }

    public final int uly(final int x, final int y) { if ((x & 1) == 0) return y - 1; return y; }

    public final int urx(final int x, final int y) { return x + 1; }

    public final int ury(final int x, final int y) { if ((x & 1) == 0) return y - 1; return y; }
    
    public final int dlx(final int x, final int y) { return x - 1; }

    public final int dly(final int x, final int y) { if ((x & 1) == 0) return y ; return y + 1; }
    
    public final int drx(final int x, final int y) { return x + 1; }

    public final int dry(final int x, final int y) { if ((x & 1) == 0) return y ; return y + 1; }

    public final int upx(final int x, final int y) { return x; }

    public final int upy(final int x, final int y) { return y - 1; }

    public final int downx(final int x, final int y) { return x; }

    public final int downy(final int x, final int y) { return y + 1; }

    public boolean trb(final int x, final int y) { return ((x + y) & 1) == 1; }
    
    public boolean trt(final int x, final int y) { return ((x + y) & 1) == 0; }

    public void getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsMaxDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsMaxDistance: xPos and yPos should not be null" );
            }

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;

        // for toroidal environments the code will be different because of wrapping arround
        if( toroidal )
            {
            // compute xmin and xmax for the neighborhood
            final int xmin = x - dist;
            final int xmax = x + dist;
            // compute ymin and ymax for the neighborhood
            final int ymin = y - dist;
            final int ymax = y + dist;
                
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                final int x_0 = stx(x0, width);
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    final int y_0 = sty(y0, height);
                    xPos.add( x_0 );
                    yPos.add( y_0 );
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
            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    xPos.add( x0 );
                    yPos.add( y0 );
                    }
                }
            }
        }


    public void getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: xPos and yPos should not be null" );
            }

        xPos.clear();
        yPos.clear();
                
        // local variables are faster
        final int height = this.height;
        final int width = this.width;

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
                    xPos.add( x_0 );
                    yPos.add( y_0 );
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
                // compute ymin and ymax for the neighborhood such that they are within boundaries
                // they depend on the curreny x0 value
                final int ymax = ((y+(dist-((x0-x>=0)?x0-x:x-x0))<=height-1)?y+(dist-((x0-x>=0)?x0-x:x-x0)):height-1);
                final int ymin = ((y-(dist-((x0-x>=0)?x0-x:x-x0))>=0)?y-(dist-((x0-x>=0)?x0-x:x-x0)):0);
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    xPos.add( x0 );
                    yPos.add( y0 );
                    }
                }
            }
        }

    public void getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHexagonalDistance: Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "Runtime exception in method getNeighborsHamiltonianDistance: xPos and yPos should not be null" );
            }

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;
                
        if( toroidal && width%2==1 )
            throw new RuntimeException( "Runtime exception in getNeighborsHexagonalDistance: toroidal hexagonal environment should have an even width" );

        if( toroidal )
            {
            // compute ymin and ymax for the neighborhood
            int ymin = y - dist;
            int ymax = y + dist;
            for( int y0 = ymin ; y0 <= ymax ; y0 = downy(x,y0) )
                {
                xPos.add( stx(x, width) );
                yPos.add( sty(y0, height) );
                }
            int x0 = x;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dly( x0, ymin );
                ymax = uly( x0, ymax );
                x0 = dlx( x0, temp_ymin );
                for( int y0 = ymin ; y0 <= ymax ; y0 = downy(x0,y0) )
                    {
                    xPos.add( stx(x0, width) );
                    yPos.add( sty(y0, height) );
                    }
                }
            x0 = x;
            ymin = y-dist;
            ymax = y+dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dry( x0, ymin );
                ymax = ury( x0, ymax );
                x0 = drx( x0, temp_ymin );
                for( int y0 = ymin ; y0 <= ymax ; y0 = downy(x0,y0) )
                    {
                    xPos.add( stx(x0, width) );
                    yPos.add( sty(y0, height) );
                    }
                }
            }
        else // not toroidal
            {
            if( x < 0 || x >= width || y < 0 || y >= height )
                throw new RuntimeException( "Runtime exception in method getNeighborsHexagonalDistance: invalid initial position" );

            // compute ymin and ymax for the neighborhood
            int ylBound = (y-dist<0)?0:y-dist;
            int yuBound = ((y+dist<height)?y+dist:height-1);

            for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x,y0) )
                {
                xPos.add( x );
                yPos.add( y0 );
                }

            int x0 = x;
            int ymin = y-dist;
            int ymax = y+dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dly( x0, ymin );
                ymax = uly( x0, ymax );
                x0 = dlx( x0, temp_ymin );
                ylBound = (ymin>=0)?ymin:0;
                yuBound =  (ymax<height)?ymax:(height-1);
                if( x0 >= 0 )
                    for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x0,y0) )
                        {
                        if( y0 >= 0 )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            }
                        }
                }

            x0 = x;
            ymin = y-dist;
            ymax = y+dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dry( x0, ymin );
                ymax = ury( x0, ymax );
                x0 = drx( x0, temp_ymin );
                ylBound = (ymin>=0)?ymin:0;
                yuBound =  (ymax<height)?ymax:(height-1);
                if( x0 < width )
                    for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x0,y0) )
                        {
                        if( y0 >= 0 )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            }
                        }
                }
            }
        }

    }
