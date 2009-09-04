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
   either continuous and descrete sparse fields.
*/

public class HexaSparseGridPortrayal2D extends SparseGridPortrayal2D
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

    public HexaSparseGridPortrayal2D()
        {
        super();
        defaultPortrayal = new HexagonalPortrayal2D();
        }

    public HexaSparseGridPortrayal2D (DrawPolicy policy)
        {
        super(policy);
        defaultPortrayal = new HexagonalPortrayal2D();
        }

    
    /** The ratio of the width of a hexagon to its height: 1 / Sin(60 degrees), otherwise known as 2 / Sqrt(3) */
    public static final double HEXAGONAL_RATIO = 2/Math.sqrt(3);
    
    
    public Int2D getLocation(DrawInfo2D info)
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
        int startx = (int)(((info.clip.x - info.draw.x)/xScale-0.5)/1.5)-2;
        int starty = (int)((info.clip.y - info.draw.y)/(yScale*2.0))-2;

        return new Int2D(startx, starty);
        }


    public Point2D.Double getPositionInFieldPortrayal(Object object, DrawInfo2D info)
        {
        final SparseGrid2D field = (SparseGrid2D) this.field;
        if (field==null) return null;

        int maxX = field.getWidth(); 
        int maxY = field.getHeight();
        if (maxX == 0 || maxY == 0) return null;

        final double divideByX = ((maxX%2==0)?(3.0*maxX/2.0+0.5):(3.0*maxX/2.0+2.0));
        final double divideByY = (1.0+2.0*maxY);

        final double xScale = info.draw.width / divideByX;
        final double yScale = info.draw.height / divideByY;
        int startx = (int)(((info.clip.x - info.draw.x)/xScale-0.5)/1.5)-2;
        int starty = (int)((info.clip.y - info.draw.y)/(yScale*2.0))-2;
        int endx = /*startx +*/ (int)(((info.clip.x - info.draw.x + info.clip.width)/xScale-0.5)/1.5) + 4;  // with rounding, width be as much as 1 off
        int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height)/(yScale*2.0)) + 4;  // with rounding, height be as much as 1 off

        DrawInfo2D newinfo = new DrawInfo2D(new Rectangle2D.Double(0,0, 
                Math.ceil(info.draw.width / (HEXAGONAL_RATIO * ((maxX - 1) * 3.0 / 4.0 + 1))),
                Math.ceil(info.draw.height / (maxY + 0.5))),
            info.clip/*, xPoints, yPoints*/);  // we don't do further clipping 

        Int2D loc = field.getObjectLocation(object);
        if (loc == null) return null;

        final int x = loc.x;
        final int y = loc.y;

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

        return new Point2D.Double(newinfo.draw.x, newinfo.draw.y);
        }
    
    
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final SparseGrid2D field = (SparseGrid2D) this.field;
        if (field==null) return;

        boolean objectSelected = !selectedWrappers.isEmpty();

        int maxX = field.getWidth(); 
        int maxY = field.getHeight();
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

        // If the person has specified a policy, we have to iterate through the
        // bags.  At present we have to do this by using a hash table iterator
        // (yuck -- possibly expensive, have to search through empty locations).
        //
        // We never use the policy to determine hitting.  hence this only works if graphics != null
        if (policy != null && graphics != null)
            {
            Bag policyBag = new Bag();
            Iterator iterator = field.locationBagIterator();
            while(iterator.hasNext())
                {
                Bag objects = (Bag)(iterator.next());
                
                // restrict the number of objects to draw
                policyBag.clear();  // fast
                if (policy.objectToDraw(objects,policyBag))  // if this function returns FALSE, we should use objects as is, else use the policy bag.
                    objects = policyBag;  // returned TRUE, so we're going to use the modified policyBag instead.

                // draw 'em
                for(int xO=0;xO<objects.numObjs;xO++)
                    {
                    final Object portrayedObject = objects.objs[xO];
                    Int2D loc = field.getObjectLocation(portrayedObject);
                    final int x = loc.x;
                    final int y = loc.y;

                    // here we only draw the object if it's within our range.  However objects
                    // might leak over to other places, so I dunno...  I give them the benefit
                    // of the doubt that they might be three times the size they oughta be, hence the -2 and +2's
                    if (loc.x >= startx -2 && loc.x < endx + 4 &&
                        loc.y >= starty -2 && loc.y < endy + 4)
                        {
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
                        
                        if (graphics == null)
                            {
                            if (portrayal.hitObject(portrayedObject, newinfo))
                                putInHere.add(getWrapper(portrayedObject));
                            }
                        else
                            {
                            // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                            //                    graphics.setClip(clip);
                            if (objectSelected &&  // there's something there
                                selectedWrappers.get(portrayedObject) != null)
                                {
                                LocationWrapper wrapper = (LocationWrapper)(selectedWrappers.get(portrayedObject));
                                portrayal.setSelected(wrapper,true);
                                portrayal.draw(portrayedObject, graphics, newinfo);
                                portrayal.setSelected(wrapper,false);
                                }
                            else portrayal.draw(portrayedObject, graphics, newinfo);
                            }
                        }
                    }
                }
            }
        else            // the easy way -- draw the objects one by one
            {
            Bag objects = field.getAllObjects();
            for(int xO=0;xO<objects.numObjs;xO++)
                {
                final Object portrayedObject = objects.objs[xO];
                Int2D loc = field.getObjectLocation(portrayedObject);
                final int x = loc.x;
                final int y = loc.y;

                // here we only draw the object if it's within our range.  However objects
                // might leak over to other places, so I dunno...  I give them the benefit
                // of the doubt that they might be three times the size they oughta be, hence the -2 and +2's
                if (loc.x >= startx -2 && loc.x < endx + 4 &&
                    loc.y >= starty -2 && loc.y < endy + 4)
                    {
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

                    if (graphics == null)
                        {
                        if (portrayal.hitObject(portrayedObject, newinfo))
                            putInHere.add(getWrapper(portrayedObject));
                        }
                    else
                        {
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        //                        graphics.setClip(clip);                        
                        if (objectSelected &&  // there's something there
                            selectedWrappers.get(portrayedObject) != null)
                            {
                            LocationWrapper wrapper = (LocationWrapper)(selectedWrappers.get(portrayedObject));
                            portrayal.setSelected(wrapper,true);
                            portrayal.draw(portrayedObject, graphics, newinfo);
                            portrayal.setSelected(wrapper,false);
                            }
                        else portrayal.draw(portrayedObject, graphics, newinfo);
                        }
                    }
                }
            }
        }
    }
    
    
