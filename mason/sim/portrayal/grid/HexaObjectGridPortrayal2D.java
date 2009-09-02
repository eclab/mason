/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;
import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;
import java.awt.geom.*;
import sim.util.*;

/**
   Portrayal for hexagonal grids (each cell has six equally-distanced neighbors) containing objects.

   <p>By default this portrayal describes objects as gray ovals (that's what getDefaultPortrayal() returns)
   and null values as empty regions (that's what getDefaultNullPortrayal() returns).  You may wish to override this
   for your own purposes.
*/

public class HexaObjectGridPortrayal2D extends ObjectGridPortrayal2D
    {
    int[] xPoints = new int[6];
    int[] yPoints = new int[6];

    double[] xyC = new double[2];
    double[] xyC_ul = new double[2];
    double[] xyC_up = new double[2];
    double[] xyC_ur = new double[2];
    
    final static void getxyC( final int x, final int y, final double xScale, final double yScale, final double tx, final double ty, final double[] xyC )
        {
        xyC[0] = tx + xScale * (1.5 * x + 1);
        xyC[1] = ty + yScale * (1.0 + 2.0 * y + (x<0?(-x)%2:x%2) );
        }

    public HexaObjectGridPortrayal2D()
        {
        super();
        defaultPortrayal = new HexagonalPortrayal2D();
        }
        
    /** The ratio of the width of a hexagon to its height: 1 / Sin(60 degrees), otherwise known as 2 / Sqrt(3) */
    public static final double HEXAGONAL_RATIO = 2/Math.sqrt(3);

    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final ObjectGrid2D field = (ObjectGrid2D) this.field;
        if (field==null) return;
        
        // Scale graphics to desired shape -- according to p. 90 of Java2D book,
        // this will change the line widths etc. as well.  Maybe that's not what we
        // want.
        
        // first question: determine the range in which we need to draw.
        // We assume that we will fill exactly the info.draw rectangle.
        // We can do the item below because we're an expensive operation ourselves
        
        final int maxX = field.getWidth();
        final int maxY = field.getHeight();
        if (maxX == 0 || maxY == 0) return;
        
        final double divideByX = ((maxX%2==0)?(3.0*maxX/2.0+0.5):(3.0*maxX/2.0+2.0));
        final double divideByY = (1.0+2.0*maxY);

        final double xScale = info.draw.width / divideByX;
        final double yScale = info.draw.height / divideByY;
        int startx = (int)(((info.clip.x - info.draw.x)/xScale-0.5)/1.5)-2;
        int starty = (int)((info.clip.y - info.draw.y)/(yScale*2.0))-2;
        int endx = /*startx +*/ (int)(((info.clip.x - info.draw.x + info.clip.width)/xScale-0.5)/1.5) + 4;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height)/(yScale*2.0)) + 4;  // with rounding, height be as much as 1 off

//        double precomputedWidth = -1;  // see discussion further below
//        double precomputedHeight = -1;  // see discussion further below

        //
        //
        // CAUTION!
        //
        // At some point we should triple check the math for rounding such
        // that the margins are drawn properly
        //
        //

        // Horizontal hexagons are staggered.  This complicates computations.  Thus
        // if  you have a M x N grid scaled to SCALE, then
        // your height is (N + 0.5) * SCALE
        // and your width is ((M - 1) * (3/4) + 1) * HEXAGONAL_RATIO * SCALE
        // we invert these calculations here to compute the rough width and height
        // for the newinfo here.  Additionally, because the original screen sizes were likely
        // converted from floats to ints, there's a round down there, so we round up to
        // compensate.  This usually results in nice circles.

//        final Rectangle clip = (graphics==null ? null : graphics.getClipBounds());

        DrawInfo2D newinfo = new DrawInfo2D(new Rectangle2D.Double(0,0, 
                Math.ceil(info.draw.width / (HEXAGONAL_RATIO * ((maxX - 1) * 3.0 / 4.0 + 1))),
                Math.ceil(info.draw.height / (maxY + 0.5))),
            info.clip/*, xPoints, yPoints*/);  // we don't do further clipping 

        if( startx < 0 ) startx = 0;
        if( starty < 0 ) starty = 0;
        if (endx > maxX) endx = maxX;
        if (endy > maxY) endy = maxY;

        for(int y=starty;y<endy;y++)
            for(int x=startx;x<endx;x++)
                {
                Object obj = field.field[x][y];
                Portrayal p = getPortrayalForObject(obj);
                if (!(p instanceof SimplePortrayal2D))
                    throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                        obj + " -- expected a SimplePortrayal2D");
                SimplePortrayal2D portrayal = (SimplePortrayal2D) p;
                
                getxyC( x, y, xScale, yScale, info.draw.x, info.draw.y, xyC );
                getxyC( field.ulx(x,y), field.uly(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_ul );
                getxyC( field.upx(x,y), field.upy(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_up );
                getxyC( field.urx(x,y), field.ury(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_ur );

                xPoints[0] = (int)(xyC_ur[0]-0.5*xScale);
                //yPoints[0] = (int)(xyC_ur[1]+yScale);
                //xPoints[1] = (int)(xyC_up[0]+0.5*xScale);
                yPoints[1] = (int)(xyC_up[1]+yScale);
                //xPoints[2] = (int)(xyC_up[0]-0.5*xScale);
                //yPoints[2] = (int)(xyC_up[1]+yScale);
                xPoints[3] = (int)(xyC_ul[0]+0.5*xScale);
                //yPoints[3] = (int)(xyC_ul[1]+yScale);
                //xPoints[4] = (int)(xyC[0]-0.5*xScale);
                yPoints[4] = (int)(xyC[1]+yScale);
                //xPoints[5] = (int)(xyC[0]+0.5*xScale);
                //yPoints[5] = (int)(xyC[1]+yScale);

                // compute the width of the object -- we tried computing the EXACT width each time, but
                // it results in weird-shaped circles etc, so instead we precomputed a standard width
                // and height, and just compute the x values here.
                newinfo.draw.x = xPoints[3];
                newinfo.draw.y = yPoints[1];
                
                // adjust drawX and drawY to center
                newinfo.draw.x +=(xPoints[0]-xPoints[3]) / 2.0;
                newinfo.draw.y += (yPoints[4]-yPoints[1]) / 2.0;
            
                if (graphics == null)
                    {
                    if (portrayal.hitObject(obj, newinfo))
                        putInHere.add(getWrapper(obj, new Int2D(x,y)));
                    }
                else
                    {
                    // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                    //                    graphics.setClip(clip);
                    portrayal.draw(obj, graphics, newinfo);
                    }
                }
        }
    }
