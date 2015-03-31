/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;
import sim.portrayal.*;
import sim.field.grid.*;
import java.awt.*;
import java.awt.geom.*;
import sim.util.*;

/**
   Portrayal for hexagonal grids (each cell has six equally-distanced neighbors) with double-precision real values.
*/

public class HexaValueGridPortrayal2D extends ValueGridPortrayal2D
    {
    int[] xPoints = new int[6];
    int[] yPoints = new int[6];
    float[] xPointsf = new float[6];
    float[] yPointsf = new float[6];
        
    double[] xyC = new double[2];
    double[] xyC_ul = new double[2];
    double[] xyC_up = new double[2];
    double[] xyC_ur = new double[2];
    
    public HexaValueGridPortrayal2D()
        {
        super();
        }

    public HexaValueGridPortrayal2D(String valueName)
        {
        super(valueName);
        }

    final static void getxyC( final int x, final int y, final double xScale, final double yScale, final double tx, final double ty, final double[] xyC )
        {
        xyC[0] = tx + xScale * (1.5 * x + 1);
        xyC[1] = ty + yScale * (1.0 + 2.0 * y + (x<0?(-x)%2:x%2) );
        }


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
                
        int startx = (int)(((position.getX() - info.draw.x)/xScale-0.5)/1.5);
        int starty = (int)((position.getY() - info.draw.y)/(yScale*2.0));

        return new Int2D(startx, starty);
        }


    static final double HEXAGONAL_RATIO = 2/Math.sqrt(3);

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
            //int startx = (int)(((info.clip.x - info.draw.x)/xScale-0.5)/1.5)-2;
            //int starty = (int)((info.clip.y - info.draw.y)/(yScale*2.0))-2;
            //int endx = /*startx +*/ (int)(((info.clip.x - info.draw.x + info.clip.width)/xScale-0.5)/1.5) + 4;  // with rounding, width be as much as 1 off
            //int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height)/(yScale*2.0)) + 4;  // with rounding, height be as much as 1 off

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
        }


    // our object to pass to the portrayal
    //final MutableDouble valueToPass = new MutableDouble(0);

    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final Grid2D field = (Grid2D)(this.field);
        if (field==null) return;
        
        // first question: determine the range in which we need to draw.
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

        //
        //
        // CAUTION!
        //
        // At some point we should triple check the math for rounding such
        // that the margins are drawn properly
        //
        //
        
        // next we determine if this is a DoubleGrid2D or an IntGrid2D
        
//        final Rectangle clip = (graphics==null ? null : graphics.getClipBounds());

        final boolean isDoubleGrid2D = (field instanceof DoubleGrid2D);
        final double[][] doubleField = (isDoubleGrid2D ? ((DoubleGrid2D) field).field : null);
        final int[][] intField = (isDoubleGrid2D ? null : ((IntGrid2D) field).field);

        double xyC_x, xyC_y, xyC_ulx, xyC_uly, xyC_upx, xyC_upy, xyC_urx, xyC_ury, x0, y0, tx, ty;

        if( startx < 0 ) startx = 0;
        if( starty < 0 ) starty = 0;
        if (endx > maxX) endx = maxX;
        if (endy > maxY) endy = maxY;

        for(int y=starty;y<endy;y++)
            for(int x=startx;x<endx;x++)
                {
                //getxyC( x, y, xScale, yScale, info.draw.x, info.draw.y, xyC );
                //getxyC( field.ulx(x,y), field.uly(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_ul );
                //getxyC( field.upx(x,y), field.upy(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_up );
                //getxyC( field.urx(x,y), field.ury(x,y), xScale, yScale, info.draw.x, info.draw.y, xyC_ur );

                x0 = x; y0 = y; tx = info.draw.x; ty = info.draw.y;
                xyC_x = tx + xScale * (1.5 * x0 + 1);
                xyC_y = ty + yScale * (1.0 + 2.0 * y0 + (x0<0?(-x0)%2:x0%2) );

                x0 = field.ulx(x,y); y0 = field.uly(x,y); tx = info.draw.x; ty = info.draw.y;
                xyC_ulx = tx + xScale * (1.5 * x0 + 1);
                xyC_uly = ty + yScale * (1.0 + 2.0 * y0 + (x0<0?(-x0)%2:x0%2) );

                x0 = field.upx(x,y); y0 = field.upy(x,y); tx = info.draw.x; ty = info.draw.y;
                xyC_upx = tx + xScale * (1.5 * x0 + 1);
                xyC_upy = ty + yScale * (1.0 + 2.0 * y0 + (x0<0?(-x0)%2:x0%2) );

                x0 = field.urx(x,y); y0 = field.ury(x,y); tx = info.draw.x; ty = info.draw.y;
                xyC_urx = tx + xScale * (1.5 * x0 + 1);
                xyC_ury = ty + yScale * (1.0 + 2.0 * y0 + (x0<0?(-x0)%2:x0%2) );


                if (graphics == null)
                    {
                    xPointsf[0] = (float)(xyC_urx-0.5*xScale);
                    yPointsf[0] = (float)(xyC_ury+yScale);
                    xPointsf[1] = (float)(xyC_upx+0.5*xScale);
                    yPointsf[1] = (float)(xyC_upy+yScale);
                    xPointsf[2] = (float)(xyC_upx-0.5*xScale);
                    yPointsf[2] = (float)(xyC_upy+yScale);
                    xPointsf[3] = (float)(xyC_ulx+0.5*xScale);
                    yPointsf[3] = (float)(xyC_uly+yScale);
                    xPointsf[4] = (float)(xyC_x-0.5*xScale);
                    yPointsf[4] = (float)(xyC_y+yScale);
                    xPointsf[5] = (float)(xyC_x+0.5*xScale);
                    yPointsf[5] = (float)(xyC_y+yScale);

                    generalPath.reset();
                    generalPath.moveTo( xPointsf[0], yPointsf[0] );
                    for( int i = 1 ; i < 6 ; i++ )
                        generalPath.lineTo( xPointsf[i], yPointsf[i] );
                    generalPath.closePath();
                    Area area = new Area( generalPath );
                    if( area.intersects( info.clip.x, info.clip.y, info.clip.width, info.clip.height ) )
                        {
                        valueToPass.val = isDoubleGrid2D ?  doubleField[x][y] : intField[x][y];
                        putInHere.add( getWrapper(valueToPass.val, new Int2D(x, y)) );
                        }
                    }
                else if (info.precise)
                    {
                    xPointsf[0] = (float)(xyC_urx-0.5*xScale);
                    yPointsf[0] = (float)(xyC_ury+yScale);
                    xPointsf[1] = (float)(xyC_upx+0.5*xScale);
                    yPointsf[1] = (float)(xyC_upy+yScale);
                    xPointsf[2] = (float)(xyC_upx-0.5*xScale);
                    yPointsf[2] = (float)(xyC_upy+yScale);
                    xPointsf[3] = (float)(xyC_ulx+0.5*xScale);
                    yPointsf[3] = (float)(xyC_uly+yScale);
                    xPointsf[4] = (float)(xyC_x-0.5*xScale);
                    yPointsf[4] = (float)(xyC_y+yScale);
                    xPointsf[5] = (float)(xyC_x+0.5*xScale);
                    yPointsf[5] = (float)(xyC_y+yScale);
                                        
                    Color c = map.getColor(isDoubleGrid2D ?  doubleField[x][y] : intField[x][y]);
                    if (c.getAlpha() == 0) continue;
                    graphics.setColor(c);
                                        
                    generalPath.reset();
                    generalPath.moveTo( xPointsf[0], yPointsf[0] );
                    for( int i = 1 ; i < 6 ; i++ )
                        generalPath.lineTo( xPointsf[i], yPointsf[i] );
                    generalPath.closePath();
                    graphics.fill(generalPath);
                    }
                else
                    {                    
                    xPoints[0] = (int)(xyC_urx-0.5*xScale);
                    yPoints[0] = (int)(xyC_ury+yScale);
                    xPoints[1] = (int)(xyC_upx+0.5*xScale);
                    yPoints[1] = (int)(xyC_upy+yScale);
                    xPoints[2] = (int)(xyC_upx-0.5*xScale);
                    yPoints[2] = (int)(xyC_upy+yScale);
                    xPoints[3] = (int)(xyC_ulx+0.5*xScale);
                    yPoints[3] = (int)(xyC_uly+yScale);
                    xPoints[4] = (int)(xyC_x-0.5*xScale);
                    yPoints[4] = (int)(xyC_y+yScale);
                    xPoints[5] = (int)(xyC_x+0.5*xScale);
                    yPoints[5] = (int)(xyC_y+yScale);
                                
                    Color c = map.getColor(isDoubleGrid2D ?  doubleField[x][y] : intField[x][y]);
                    if (c.getAlpha() == 0) continue;
                    graphics.setColor(c);

                    // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                    //                    graphics.setClip(clip);
                    graphics.fillPolygon(xPoints,yPoints,6);
                    }
                }
        
        }

    GeneralPath generalPath = new GeneralPath();


    /** This is not supported by hexagonal portrayals.  Throws an exception. */
    public void setBorder(boolean on) { throw new RuntimeException("Border drawing is not supported by hexagonal portrayals."); }

    /** This is not supported by hexagonal portrayals.  Throws an exception. */
    public void setGridLines(boolean on) { throw new RuntimeException("Grid line drawing is not supported by hexagonal portrayals."); }

    }
