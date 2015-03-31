/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;
import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import sim.util.*;
import java.awt.*;
import java.util.*;
import java.awt.geom.*;

/**
   Portrayal for hexagonal grids (each cell has six equally-distanced neighbors). It can draw
   either continuous and descrete Dense fields.

   The 'location' passed
   into the DrawInfo2D handed to the SimplePortryal2D is an Int2D.
*/

public class HexaDenseGridPortrayal2D extends DenseGridPortrayal2D
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

    public HexaDenseGridPortrayal2D()
        {
        super();
        defaultPortrayal = new HexagonalPortrayal2D();
        }

    /** @deprecated Use setDrawPolicy. */
    public HexaDenseGridPortrayal2D (DrawPolicy policy)
        {
        super(policy);
        defaultPortrayal = new HexagonalPortrayal2D();
        }

    
    /** The ratio of the width of a hexagon to its height: 1 / Sin(60 degrees), otherwise known as 2 / Sqrt(3) */
    static final double HEXAGONAL_RATIO = 2/Math.sqrt(3);
    
    
    public Double2D getScale(DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Grid2D field = (Grid2D) this.field;
            if (field==null) return null;

            int maxX = field.getWidth(); 
            int maxY = field.getHeight();
            if (maxX == 0 || maxY == 0) return null;

            final double divideByX = ((maxX%2==0)?(3.0*maxX/2.0+0.5):(3.0*maxX/2.0+2.0));
            final double divideByY = (1.0+2.0*maxY);

            final double xScale = info.draw.width / divideByX;
            final double yScale = info.draw.height / divideByY;
            return new Double2D(xScale, yScale);
            }
        }
                
                
    public Object getPositionLocation(Point2D.Double position, DrawInfo2D info)
        {
        Double2D scale = getScale(info);
        double xScale = scale.x;
        double yScale = scale.y;
                
        int startx = (int)Math.floor(((position.getX() - info.draw.x)/xScale-0.5)/1.5);
        int starty = (int)Math.floor((position.getY() - info.draw.y)/(yScale*2.0));

        return new Int2D(startx, starty);
        }


    public Point2D.Double getLocationPosition(Object location, DrawInfo2D info)
        {
        synchronized(info.gui.state.schedule)
            {
            final Grid2D field = (Grid2D) this.field;
            if (field==null) return null;

            int maxX = field.getWidth(); 
            int maxY = field.getHeight();
            if (maxX == 0 || maxY == 0) return null;

            final double divideByX = ((maxX%2==0)?(3.0*maxX/2.0+0.5):(3.0*maxX/2.0+2.0));
            final double divideByY = (1.0+2.0*maxY);

            final double xScale = info.draw.width / divideByX;
            final double yScale = info.draw.height / divideByY;
            //int startx = (int)Math.floor(((info.clip.x - info.draw.x)/xScale-0.5)/1.5)-2;
            //int starty = (int)Math.floor((info.clip.y - info.draw.y)/(yScale*2.0))-2;
            //int endx = /*startx +*/ (int)Math.floor(((info.clip.x - info.draw.x + info.clip.width)/xScale-0.5)/1.5) + 4;  // with rounding, width be as much as 1 off
            //int endy = /*starty +*/ (int)Math.floor((info.clip.y - info.draw.y + info.clip.height)/(yScale*2.0)) + 4;  // with rounding, height be as much as 1 off

            DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, 
                    Math.ceil(info.draw.width / (HEXAGONAL_RATIO * ((maxX - 1) * 3.0 / 4.0 + 1))),
                    Math.ceil(info.draw.height / (maxY + 0.5))),
                info.clip/*, xPoints, yPoints*/);  // we don't do further clipping 
            newinfo.precise = info.precise;

            Int2D loc = (Int2D) location;
            if (loc == null) return null;

            final int x = loc.x;
            final int y = loc.y;

            getxyC( x, y, xScale, yScale, info.draw.x, info.draw.y, xyC );
            getxyC( field.ulx(x,y), field.uly(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_ul );
            getxyC( field.upx(x,y), field.upy(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_up );
            getxyC( field.urx(x,y), field.ury(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_ur );

            xPoints[0] = (int)Math.floor(xyC_ur[0]-0.5*xScale);
            //yPoints[0] = (int)Math.floor(xyC_ur[1]+yScale);
            //xPoints[1] = (int)Math.floor(xyC_up[0]+0.5*xScale);
            yPoints[1] = (int)Math.floor(xyC_up[1]+yScale);
            //xPoints[2] = (int)Math.floor(xyC_up[0]-0.5*xScale);
            //yPoints[2] = (int)Math.floor(xyC_up[1]+yScale);
            xPoints[3] = (int)Math.floor(xyC_ul[0]+0.5*xScale);
            //yPoints[3] = (int)Math.floor(xyC_ul[1]+yScale);
            //xPoints[4] = (int)Math.floor(xyC[0]-0.5*xScale);
            yPoints[4] = (int)Math.floor(xyC[1]+yScale);
            //xPoints[5] = (int)Math.floor(xyC[0]+0.5*xScale);
            //yPoints[5] = (int)Math.floor(xyC[1]+yScale);

            // compute the width of the object -- we tried computing the EXACT width each time, but
            // it results in weird-shaped circles etc, so instead we precomputed a standard width
            // and height, and just compute the x values here.
            newinfo.draw.x = xPoints[3];
            newinfo.draw.y = yPoints[1];
                    
            // adjust drawX and drawY to center
            newinfo.draw.x +=(xPoints[0]-xPoints[3]) / 2.0;
            newinfo.draw.y += (yPoints[4]-yPoints[1]) / 2.0;

            return new Point2D.Double(newinfo.draw.x, newinfo.draw.y);
            }
        }
    
    
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final DenseGrid2D field = (DenseGrid2D) this.field;
        Bag policyBag = new Bag();

        if (field==null) return;

        boolean objectSelected = !selectedWrappers.isEmpty();

        int maxX = field.getWidth(); 
        int maxY = field.getHeight();
        if (maxX == 0 || maxY == 0) return;

        final double divideByX = ((maxX%2==0)?(3.0*maxX/2.0+0.5):(3.0*maxX/2.0+2.0));
        final double divideByY = (1.0+2.0*maxY);

        final double xScale = info.draw.width / divideByX;
        final double yScale = info.draw.height / divideByY;
        int startx = (int)Math.floor(((info.clip.x - info.draw.x)/xScale-0.5)/1.5)-2;
        int starty = (int)Math.floor((info.clip.y - info.draw.y)/(yScale*2.0))-2;
        int endx = /*startx +*/ (int)Math.floor(((info.clip.x - info.draw.x + info.clip.width)/xScale-0.5)/1.5) + 4;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)Math.floor((info.clip.y - info.draw.y + info.clip.height)/(yScale*2.0)) + 4;  // with rounding, height be as much as 1 off

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

        DrawInfo2D newinfo = new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0,0, 
                Math.ceil(info.draw.width / (HEXAGONAL_RATIO * ((maxX - 1) * 3.0 / 4.0 + 1))),
                Math.ceil(info.draw.height / (maxY + 0.5))),
            info.clip/*, xPoints, yPoints*/);  // we don't do further clipping 
        newinfo.precise = info.precise;
        newinfo.fieldPortrayal = this;

        // If the person has specified a policy, we have to iterate through the
        // bags.  At present we have to do this by using a hash table iterator
        // (yuck -- possibly expensive, have to search through empty locations).
        //
        // We never use the policy to determine hitting.  hence this only works if graphics != null
 
                
        if( startx < 0 ) startx = 0;
        if( starty < 0 ) starty = 0;
        if (endx > maxX) endx = maxX;
        if (endy > maxY) endy = maxY;

        for(int y=starty;y<endy;y++)
            for(int x=startx;x<endx;x++)
                {
                Bag objects = field.field[x][y];
                                
                if (objects == null) continue;

                if (policy != null & graphics != null)
                    {
                    policyBag.clear();  // fast
                    if (policy.objectToDraw(objects,policyBag))  // if this function returns FALSE, we should use objects as is, else use the policy bag.
                        objects = policyBag;  // returned TRUE, so we're going to use the modified policyBag instead.
                    }
                
                for(int i=0;i<objects.numObjs;i++)
                    {
                    final Object portrayedObject = objects.objs[i];
                                        
                    Portrayal p = getPortrayalForObject(portrayedObject);
                    if (!(p instanceof SimplePortrayal2D))
                        throw new RuntimeException("Unexpected Portrayal " + p + " for object " + 
                            portrayedObject + " -- expected a SimplePortrayal2D");
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
            
                    locationToPass.x = x;
                    locationToPass.y = y;
                
                    if (graphics == null)
                        {
                        if (portrayal.hitObject(portrayedObject, newinfo))
                            putInHere.add(getWrapper(portrayedObject, new Int2D(x,y)));
                        }
                    else
                        {
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        //                    graphics.setClip(clip);
                        portrayal.draw(portrayedObject, graphics, newinfo);
                        }
                    }
                }
        }


    /** This is not supported by hexagonal portrayals.  Throws an exception. */
    public void setBorder(boolean on) { throw new RuntimeException("Border drawing is not supported by hexagonal portrayals."); }

    /** This is not supported by hexagonal portrayals.  Throws an exception. */
    public void setGridLines(boolean on) { throw new RuntimeException("Grid line drawing is not supported by hexagonal portrayals."); }

    }
    
    
