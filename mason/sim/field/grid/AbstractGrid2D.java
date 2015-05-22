/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.field.grid;

import sim.util.*;
import java.util.*;

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
    private static final long serialVersionUID = 1;

    // this should never change except via setTo
    protected int width;
    // this should never change except via setTo
    protected int height;

    public final int getWidth() { return width; }
    
    public final int getHeight() { return height; }
    
    public Map buildMap(Map other) { return new HashMap(other); }
    public Map buildMap(int size) 
        {
        if (size <= ANY_SIZE) return new HashMap();
        else return new HashMap(size);
        }
    
    // slight revision for more efficiency
    public final int tx(int x) 
        { 
        final int width = this.width;
        if (x >= 0 && x < width) return x;  // do clearest case first
        x = x % width;
        if (x < 0) x = x + width;
        return x;
        }
        
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
        
    protected void removeOrigin(int x, int y, IntBag xPos, IntBag yPos)
        {
        int size = xPos.size();
        for(int i = 0; i <size; i++)
            {
            if (xPos.get(i) == x && yPos.get(i) == y)
                {
                xPos.remove(i);
                yPos.remove(i);
                return;
                }
            }
        }
        
    // only removes the first occurence
    protected void removeOriginToroidal(int x, int y, IntBag xPos, IntBag yPos)
        {
        int size = xPos.size();
        x = tx(x, width, width*2, x+width, x-width);
        y = ty(y, height, height*2, y+height, y-height);
        
        for(int i = 0; i <size; i++)
            {
            if (tx(xPos.get(i), width, width*2, x+width, x-width) == x && ty(yPos.get(i), height, height*2, y+height, y-height) == y)
                {
                xPos.remove(i);
                yPos.remove(i);
                return;
                }
            }
        }

    /** @deprecated */
    public void getNeighborsMaxDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        getMooreLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos);
        }

    public void getMooreLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);

        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid2D.BOUNDED, Grid2D.UNBOUNDED, or Grid2D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "xPos and yPos should not be null" );
            }

        if( ( x < 0 || x >= width || y < 0 || y >= height ) && !bounded)
            throw new RuntimeException( "Invalid initial position" );

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;


        // for toroidal environments the code will be different because of wrapping arround
        if( toroidal )
            {
            // compute xmin and xmax for the neighborhood
            int xmin = x - dist;
            int xmax = x + dist;

            // next: is xmax - xmin humongous?  If so, no need to continue wrapping around
            if (xmax - xmin >= width)  // too wide, just use whole neighborhood
                { xmin = 0; xmax = width - 1; }
                
            // compute ymin and ymax for the neighborhood
            int ymin = y - dist;
            int ymax = y + dist;
                
            // next: is ymax - ymin humongous?  If so, no need to continue wrapping around
            if (ymax - ymin >= height)  // too wide, just use whole neighborhood
                { ymin = 0; ymax = width - 1; }
                
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                final int x_0 = tx(x0, width, width*2, x0+width, x0-width);
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    final int y_0 = ty(y0, height, height*2, y0+height, y0-height);
                    xPos.add( x_0 );
                    yPos.add( y_0 );
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,xPos,yPos); 
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmin = ((x-dist>=0) || !bounded ?x-dist:0);
            final int xmax =((x+dist<=width-1) || !bounded ?x+dist:width-1);
            // compute ymin and ymax for the neighborhood such that they are within boundaries
            final int ymin = ((y-dist>=0) || !bounded ?y-dist:0);
            final int ymax = ((y+dist<=height-1) || !bounded ?y+dist:height-1);
            for( int x0 = xmin; x0 <= xmax ; x0++ )
                {
                for( int y0 = ymin ; y0 <= ymax ; y0++ )
                    {
                    xPos.add( x0 );
                    yPos.add( y0 );
                    }
                }
            if (!includeOrigin) removeOrigin(x,y,xPos,yPos); 
            }
        }


    /** @deprecated */
    public void getNeighborsHamiltonianDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        getVonNeumannLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos);
        }


    public void getVonNeumannLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);
        
        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid2D.BOUNDED, Grid2D.UNBOUNDED, or Grid2D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "xPos and yPos should not be null" );
            }

        if( ( x < 0 || x >= width || y < 0 || y >= height ) && !bounded)
            throw new RuntimeException( "Invalid initial position" );

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
                final int x_0 = tx(x0, width, width*2, x0+width, x0-width);
                // compute ymin and ymax for the neighborhood; they depend on the current x0 value
                final int ymax = y+(dist-((x0-x>=0)?x0-x:x-x0));
                final int ymin = y-(dist-((x0-x>=0)?x0-x:x-x0));
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    final int y_0 = ty(y0, height, height*2, y0+height, y0-height);
                    xPos.add( x_0 );
                    yPos.add( y_0 );
                    }
                }
                
            if (dist * 2 >= width || dist * 2 >= height)  // too big, will have to remove duplicates
                {
                int sz = xPos.size();
                Map map = buildMap(sz);
                for(int i = 0 ; i < sz; i++)
                    {
                    Double2D elem = new Double2D(xPos.get(i), yPos.get(i));
                    if (map.containsKey(elem)) // already there
                        {
                        xPos.remove(i);
                        yPos.remove(i);
                        i--;
                        sz--;
                        }
                    else
                        {
                        map.put(elem, elem);
                        }
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,xPos,yPos); 
            }
        else // not toroidal
            {
            // compute xmin and xmax for the neighborhood such that they are within boundaries
            final int xmax = ((x+dist<=width-1) || !bounded ?x+dist:width-1);
            final int xmin = ((x-dist>=0) || !bounded ?x-dist:0);
            for( int x0 = xmin ; x0 <= xmax ; x0++ )
                {
                // compute ymin and ymax for the neighborhood such that they are within boundaries
                // they depend on the curreny x0 value
                final int ymax = ((y+(dist-((x0-x>=0)?x0-x:x-x0))<=height-1) || !bounded ?y+(dist-((x0-x>=0)?x0-x:x-x0)):height-1);
                final int ymin = ((y-(dist-((x0-x>=0)?x0-x:x-x0))>=0) || !bounded ?y-(dist-((x0-x>=0)?x0-x:x-x0)):0);
                for( int y0 =  ymin; y0 <= ymax; y0++ )
                    {
                    xPos.add( x0 );
                    yPos.add( y0 );
                    }
                }
            if (!includeOrigin) removeOrigin(x,y,xPos,yPos);
            }
        }


    /** @deprecated */
    public void getNeighborsHexagonalDistance( final int x, final int y, final int dist, final boolean toroidal, IntBag xPos, IntBag yPos )
        {
        getHexagonalLocations(x, y, dist, toroidal ? TOROIDAL : BOUNDED, true, xPos, yPos);
        }

    public void getHexagonalLocations( final int x, final int y, final int dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos )
        {
        boolean toroidal = (mode == TOROIDAL);
        boolean bounded = (mode == BOUNDED);
        
        if (mode != BOUNDED && mode != UNBOUNDED && mode != TOROIDAL)
            {
            throw new RuntimeException("Mode must be either Grid2D.BOUNDED, Grid2D.UNBOUNDED, or Grid2D.TOROIDAL");
            }
        
        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }

        if( xPos == null || yPos == null )
            {
            throw new RuntimeException( "xPos and yPos should not be null" );
            }

        if( ( x < 0 || x >= width || y < 0 || y >= height ) && !bounded)
            throw new RuntimeException( "Invalid initial position" );

        xPos.clear();
        yPos.clear();

        // local variables are faster
        final int height = this.height;
        final int width = this.width;

        if( toroidal && height%2==1 )
            throw new RuntimeException( "toroidal hexagonal environment should have even heights" );

        if( toroidal )
            {
            // compute ymin and ymax for the neighborhood
            int ymin = y - dist;
            int ymax = y + dist;
            for( int y0 = ymin ; y0 <= ymax ; y0 = downy(x,y0) )
                {
                xPos.add( tx(x, width, width*2, x+width, x-width) );
                yPos.add( ty(y0, height, height*2, y0+height, y0-height) );
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
                    xPos.add( tx(x0, width, width*2, x0+width, x0-width) );
                    yPos.add( ty(y0, height, height*2, y0+height, y0-height) );
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
                    xPos.add( tx(x0, width, width*2, x0+width, x0-width) );
                    yPos.add( ty(y0, height, height*2, y0+height, y0-height) );
                    }
                }

            if (dist * 2 >= width || dist * 2 >= height)  // too big, will have to remove duplicates
                {
                int sz = xPos.size();
                Map map = buildMap(sz);
                for(int i = 0 ; i < sz; i++)
                    {
                    Double2D elem = new Double2D(xPos.get(i), yPos.get(i));
                    if (map.containsKey(elem)) // already there
                        {
                        xPos.remove(i);
                        yPos.remove(i);
                        i--;
                        sz--;
                        }
                    else
                        {
                        map.put(elem, elem);
                        }
                    }
                }
            if (!includeOrigin) removeOriginToroidal(x,y,xPos,yPos); 
            }
        else // not toroidal
            {
            int ymin = y - dist;
            int ymax = y + dist;
            
            // compute ymin and ymax for the neighborhood
            int ylBound = ((ymin >= 0 || !bounded) ? ymin : 0);
            int yuBound = ((ymax < height || !bounded) ? ymax : height-1);

            // add vertical center line of hexagon
            for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x,y0) )
                {
                xPos.add( x );
                yPos.add( y0 );
                }
            
            // add right half of hexagon
            int x0 = x;
            ymin = y - dist;
            ymax = y + dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dly( x0, ymin );
                ymax = uly( x0, ymax );
                x0 = dlx( x0, temp_ymin );
                
                ylBound = ((ymin >= 0 || !bounded) ? ymin : 0);
                yuBound = ((ymax < height || !bounded) ? ymax : height-1);
    
                // yuBound =  (( ymax<height  || !bounded) ? ymax : height-1);

                if( x0 >= 0 )
                    for( int y0 = ylBound ; y0 <= yuBound ; y0 = downy(x0,y0) )
                        {
                        if( y0 >= 0 || !bounded )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            }
                        }
                }

            x0 = x;
            ymin = y - dist;
            ymax = y + dist;
            for( int i = 1 ; i <= dist ; i++ )
                {
                final int temp_ymin = ymin;
                ymin = dry( x0, ymin );
                ymax = ury( x0, ymax );
                x0 = drx( x0, temp_ymin );

                ylBound = ((ymin >= 0 || !bounded) ? ymin : 0);
                yuBound = ((ymax < height || !bounded) ? ymax : height-1);
                
                // yuBound =  ((ymax<height) || !bounded ?ymax:height);
                
                if( x0 < width )
                    for( int y0 = ymin ; y0 <= yuBound; y0 = downy(x0,y0) )
                        {
                        if( y0 >= 0 || !bounded )
                            {
                            xPos.add( x0 );
                            yPos.add( y0 );
                            }
                        }
                }
            if (!includeOrigin) removeOrigin(x,y,xPos,yPos); 
            }
        }
        
    double ds(double d1x, double d1y, double d2x, double d2y)
        {
        return ((d1x - d2x) * (d1x - d2x) + (d1y - d2y) * (d1y - d2y));
        }
    
    boolean within(double d1x, double d1y, double d2x, double d2y, double distanceSquared, boolean closed)
        {
        double d = ds(d1x, d1y, d2x, d2y);
        return (d < distanceSquared || (d == distanceSquared && closed));
        }
        
    public void getRadialLocations( final int x, final int y, final double dist, int mode, boolean includeOrigin, IntBag xPos, IntBag yPos )
        {
        getRadialLocations(x, y, dist, mode, includeOrigin, Grid2D.ANY, true, xPos, yPos);
        }
        
    public void getRadialLocations( final int x, final int y, final double dist, int mode, boolean includeOrigin, int measurementRule, boolean closed, IntBag xPos, IntBag yPos )
        {
        boolean toroidal = (mode == TOROIDAL);

        // won't work for negative distances
        if( dist < 0 )
            {
            throw new RuntimeException( "Distance must be positive" );
            }
            
        if (measurementRule != Grid2D.ANY && measurementRule != Grid2D.ALL && measurementRule != Grid2D.CENTER)
            {
            throw new RuntimeException(" Measurement rule must be one of ANY, ALL, or CENTER" );
            }
                
        // grab the rectangle
        if (toroidal)
            getMooreLocations(x,y, (int) Math.ceil(dist + 0.5), UNBOUNDED, includeOrigin, xPos, yPos);
        else
            getMooreLocations(x,y, (int) Math.ceil(dist + 0.5), mode, includeOrigin, xPos, yPos);
        int len = xPos.size();
        double distsq = dist * dist;
        
        int width = this.width;
        int height = this.height;
        int widthtimestwo = width * 2;
        int heighttimestwo = height * 2;
        
        for(int i = 0; i < len; i++)
            {
            int xp = xPos.get(i);
            int yp = yPos.get(i);
            boolean remove = false;
                
            if (measurementRule == Grid2D.ANY)
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
                if (x < xp)
                    {
                    if (y < yp)
                        remove = !within(x,y,xp-0.5,yp-0.5,distsq,closed);
                    else
                        remove = !within(x,y,xp-0.5,yp+0.5,distsq,closed);
                    }
                else
                    {
                    if (y < yp)
                        remove = !within(x,y,xp+0.5,yp-0.5,distsq,closed);
                    else
                        remove = !within(x,y,xp+0.5,yp+0.5,distsq,closed);
                    }
                }
            else if (measurementRule == Grid2D.ALL)
                {
                if (x < xp)
                    {
                    if (y < yp)
                        remove = !within(x,y,xp+0.5,yp+0.5,distsq,closed);
                    else
                        remove = !within(x,y,xp+0.5,yp-0.5,distsq,closed);
                    }
                else
                    {
                    if (y < yp)
                        remove = !within(x,y,xp-0.5,yp+0.5,distsq,closed);
                    else
                        remove = !within(x,y,xp-0.5,yp-0.5,distsq,closed);
                    }
                }
            else // (measurementRule == Grid2D.CENTER)
                {
                remove = !within(x,y,xp,yp,distsq,closed);
                }
                
            if (remove)
                { xPos.remove(i); yPos.remove(i); i--;  len--; }
            else if (toroidal) // need to convert to toroidal position
                { 
                int _x = xPos.get(i);
                int _y = yPos.get(i);
                xPos.set(i, tx(_x, width, widthtimestwo, _x + width, _x - width));
                yPos.set(i, tx(_y, height, heighttimestwo, _y + width, _y - width));
                }
            }
        }


    protected void checkBounds(Grid2D other)
        {
        if (getHeight() != other.getHeight() || getWidth() != other.getWidth())
            throw new IllegalArgumentException("Grids must be the same dimensions.");
        }
    




/*
  double dxForRadius(double dy, double radius)  // may return NaN
  {
  return Math.sqrt(radius*radius - dy*dy);
  }

  // still in real-valued space
  double dxForAngle(double dy, double xa, double ya)
  {
  if (ya == 0 && dy == 0)  // horizontal line, push dx way out to the far edges of space, even if we're not in line with it
  { return xa > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY ; }
  else if ((dy >= 0 && ya >= 0) || (dy <= 0 && ya <= 0))          // same side
  {
  // dx : dy :: xa : ya
  return (xa * dy) / ya;
  }
  else return Double.NaN;
  }

  // a is next to b (<) and c and d are not between them
  boolean nextTo(double a, double b, double c, double d)
  {
  return (a <= b &&
  !(c >= a && c <= b) &&
  !(d >= a && d <= b));
  }
        
  int pushLeft(double x, double y, double slope, double radiusSq)
  {
  System.err.println("<---- " + x + " " + y + " " + slope);
  double xa = x;
  double ya = y;
  int xi = (int)xa;
  int yi = (int)ya;
                
  while( 
  ((slope >= 0 && xi * slope >= yi) || 
  (slope < 0 && xi * slope < (yi + 1))) &&
  xi * xi + (xi * slope) * (xi * slope) < radiusSq)
  {
  xi--;
  }
  return xi;
  }

  int pushRight(double x, double y, double slope, double radiusSq)  // radius limits our distance
  {
  System.err.println("----> " + x + " " + y + " " + slope);
  double xa = x;
  double ya = y;
  int xi = (int)xa;
  int yi = (int)ya;

  while(
  ((slope <= 0 && (xi + 1) * slope >= yi) ||
  (slope > 0 && (xi + 1) * slope < (yi + 1))) &&
  (xi + 1) * (xi + 1) * ((xi + 1) * slope) * ((xi + 1) * slope) < radiusSq)
  {
  xi++;
  }
  return xi;
  }

  boolean scan(int x, int y, int dy, double radius, double xa, double ya, double xb, double yb, 
  boolean crossesZero, boolean crossesPi, boolean toroidal, IntBag xPos, IntBag yPos)
  {
  // too high?
  if (dy > radius || dy < -radius) return false;

  double r = dxForRadius(dy, radius);
  double l = - r;
  double s = dxForAngle(dy, xa, ya);
  double e = dxForAngle(dy, xb, yb);

  int min = 0;
  int max = 0;

  System.err.println("dy=" + dy + " radius=" + radius + " zero=" + crossesZero + " Pi=" + crossesPi);
  System.err.println("xa " + xa + " ya " + ya + " xb " + xb + " yb " + yb);
  System.err.println("r " + r + " l " + l + " s " + s + " e " + e);
                
  //if (dy == 0) // special case dy == 0
  //  {
  //  if (crossesPi) min = (int) Math.round(l);
  //  if (crossesZero) max = (int) Math.round(r);
  //  System.err.println("MIN " + min + " MAX " + max);
  //  if (toroidal)
  //  for(int i = min; i <= max; i++) { xPos.add(stx(x + i)); yPos.add(sty(y + dy));  }
  //  else
  //  for(int i = min; i <= max; i++) { xPos.add(x + i); yPos.add(y + dy); }
  //  return true;
  //  }
  //  else 
                
  if ((s==Double.POSITIVE_INFINITY && e == Double.POSITIVE_INFINITY) || (s == Double.NEGATIVE_INFINITY && e==Double.NEGATIVE_INFINITY))  // straight line in the same direction, handle specially
  { return false; }
  else if (dy >= 0) 
  {
  if (l!=l)  // NaN, signifies out of bounds
  return false;
  // six cases:   (L = negativeDXForRadius, R = positiveDXForRadius, S = start (Xa, Ya), E = end(Xb, Yb)
  if (s == e)  // line
  {
  System.err.println("S==E");
  if (s * s + dy * dy > radius * radius) return false;
  min = pushLeft(s, dy, ya / xa, radius * radius);
  max = pushRight(s, dy, ya / xa, radius * radius);
  }
  // L S E R
  else if (l <= s && s <= e && e <= r) 
  {
  System.err.println("LSER");
  // draw first line
  min = (int)Math.floor(l); max = (int)Math.ceil(s);
  if (toroidal)
  for(int i = min; i <= max; i++) { xPos.add(stx(x + i)); yPos.add(sty(y + dy));  }
  else
  for(int i = min; i <= max; i++) { xPos.add(x + i); yPos.add(y + dy); }
  // set up second line
  min = (int)Math.floor(e); max = (int)Math.ceil(r);
  }
  // L R
  else if ((e <= l && l <= r && r <= s) || 
  (l <= r && r <= s && s <= e))
                        
//                      nextTo(l, r, e, s) && e <= s)  // end must be less than start for this to be valid
{
System.err.println("LR");
min = (int)Math.floor(l); max = (int)Math.ceil(r);
}
// E S
else if //(nextTo(e, s, l, r))
(
(l <= e && e <= s && s <= r) 
//||
//(l <= e && e <= s && r <= e) ||
//(l >= e && e <= s && r >= e)
)
{
System.err.println("ES");
min = (int)Math.floor(e); max = (int)Math.ceil(s);
}
// E R
else if (nextTo(e, r, l, s))
{
System.err.println("ER");
min = (int)Math.floor(e); max = (int)Math.ceil(r);
}
// L S
else if (nextTo(l, s, e, r))
{
System.err.println("LS");
min = (int)Math.floor(l); max = (int)Math.ceil(s);
}
else return false;
                        
System.err.println("MIN " + min + " MAX " + max);
// draw line
if (toroidal)
for(int i = min; i <= max; i++) { xPos.add(stx(x + i)); yPos.add(sty(y + dy));  }
else
for(int i = min; i <= max; i++) { xPos.add(x + i); yPos.add(y + dy); }
                                
return true;
}
else // (dy < 0) 
{
if (l!=l)  // out of bounds
return false;
//double s = dxForAngle(dy, xa, ya);
//double e = dxForAngle(dy, xb, yb);
                
// five cases:  (L = negativeDXForRadius, R = positiveDXForRadius, S = start (Xa, Ya), E = end(Xb, Yb)
// L E S R
if (l <= e && e <= s && s <= r) 
{
// draw first line
min = (int)Math.round(l); max = (int)Math.round(e);
if (toroidal)
for(int i = min; i <= max; i++) { xPos.add(stx(x + i)); yPos.add(sty(y + dy));  }
else
for(int i = min; i <= max; i++) { xPos.add(x + i); yPos.add(y + dy); }
// set up second line
min = (int)Math.round(s); max = (int)Math.round(r);
}
// L R
else if (nextTo(l, r, e, s) && s <= e)  // end must be greater than start for this to be valid
{
min = (int)Math.round(l); max = (int)Math.round(r);
}
// S E
else if (nextTo(s, e, l, r))
{
min = (int)Math.round(s); max = (int)Math.round(e);
}
// L E
else if (nextTo(l, e, r, s))
{
min = (int)Math.round(l); max = (int)Math.round(e);
}
// S R
else if (nextTo(s, r, l, e))
{
min = (int)Math.round(s); max = (int)Math.round(r);
}
else return false;
                        
// draw line
if (toroidal)
for(int i = min; i <= max; i++) { xPos.add(stx(x + i)); yPos.add(sty(y));  }
else
for(int i = min; i <= max; i++) { xPos.add(x + i); yPos.add(y); }
                                
return true;
}
}

//
//  public void getNeighborsWithinArc(int x, int y, double radius, double startAngle, double endAngle, IntBag xPos, IntBag yPos)
//  { getNeighborsWithinArc(x,y,radius,startAngle,endAngle,false,xPos,yPos); }
//                
//  public void getNeighborsWithinArc(int x, int y, double radius, double startAngle, double endAngle, boolean toroidal, IntBag xPos, IntBag yPos)
//  {
//  if (radius < 0)
//  throw new RuntimeException("Radius must be positive");
//  xPos.clear();
//  yPos.clear();
//                
//  // move angles into [0...2 PI)
//  if (startAngle < 0) startAngle += Math.PI * 2; 
//  if (startAngle < 0) startAngle = ((startAngle % Math.PI * 2) + Math.PI * 2);
//  if (startAngle >= Math.PI * 2) startAngle = startAngle % Math.PI * 2;
//                
//  if (endAngle < 0) endAngle += Math.PI * 2; 
//  if (endAngle < 0) endAngle = ((endAngle % Math.PI * 2) + Math.PI * 2);
//  if (endAngle >= Math.PI * 2) endAngle = endAngle % Math.PI * 2;
//
//  // compute slopes -- avoid atan2
//  double xa = Math.cos(startAngle);
//  double ya = Math.sin(startAngle);
//  double xb = Math.cos(endAngle);
//  double yb = Math.sin(endAngle);
//                
//  // compute crossings
//  boolean crossesZero = false;
//  boolean crossesPi = false;
//  if (startAngle > endAngle || startAngle == 0 || endAngle == 0)  // crosses zero for sure
//  crossesZero = true;
//  else if (startAngle <= Math.PI && endAngle >= Math.PI)
//  crossesPi = true;
//                
//  // scan up
//  int dy = 0;
//  while(scan(x, y, dy, radius, xa, ya, xb, yb, crossesZero, crossesPi, toroidal, xPos, yPos))
//  dy++;
//  // scan down (not including zero)
//  dy = -1;
//  while(scan(x, y, dy, radius, xa, ya, xb, yb, crossesZero, crossesPi, toroidal, xPos, yPos))
//  dy--;
//  }


*/

    }
