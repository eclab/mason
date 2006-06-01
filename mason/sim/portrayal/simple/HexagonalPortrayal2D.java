/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import java.awt.geom.*;

/**
   A simple portrayal for 2D visualization of hexagons. It extends the SimplePortrayal2D and
   it manages the drawing and hit-testing for hexagonal shapes. If the DrawInfo2D parameter
   received by draw and hitObject functions is an instance of HexaDrawInfo2D, better information
   is extracted and used to make everthing look better. Otherwise, hexagons may be created from
   information stored in simple DrawInfo2D objects, but overlapping or extra empty spaces may be
   observed (especially when increasing the scale).
*/

public class HexagonalPortrayal2D extends SimplePortrayal2D
    {
    public Paint paint;

    private int[] xPoints = new int[6];
    private int[] yPoints = new int[6];

    public boolean drawFrame = true;

    public HexagonalPortrayal2D() { this(Color.gray,false); }
    public HexagonalPortrayal2D(Paint paint)  { this(paint,false); }
    public HexagonalPortrayal2D(boolean drawFrame) { this(Color.gray,drawFrame); }
    public HexagonalPortrayal2D(Paint paint, boolean drawFrame)  { this.paint = paint; this.drawFrame = drawFrame; }
        
    // assumes the graphics already has its color set
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        graphics.setPaint(paint);
        GeneralPath generalPath = createGeneralPath( info );
        //graphics.fill(generalPath);
        graphics.fillPolygon(xPoints,yPoints,6);
        if( drawFrame )
            {
            graphics.setColor( Color.black );
            graphics.draw( generalPath);
            }
        }

    protected GeneralPath generalPath = new GeneralPath();

    /** Creates a general path for the bounding hexagon */
    GeneralPath createGeneralPath( DrawInfo2D info )
        {
        generalPath.reset();
        // we are doing a simple draw, so we ignore the info.clip
        if( info instanceof HexaDrawInfo2D )
            {
            System.out.println("yo");
            final HexaDrawInfo2D temp = (HexaDrawInfo2D)info;
                        
            for(int i=0; i < 6; i++)
                { xPoints[i] = temp.xPoints[i]; yPoints[i] = temp.yPoints[i]; }
            return null;
            //generalPath.moveTo( temp.xPoints[0], temp.yPoints[0] );
            //for( int i = 1 ; i < 6 ; i++ )
            //    generalPath.lineTo( temp.xPoints[i], temp.yPoints[i] );
            //generalPath.closePath();
            }
        else
            {
            final double infodrawx = info.draw.x;
            final double infodrawy = info.draw.y;
            final double infodrawwidth = info.draw.width;
            final double infodrawheight = info.draw.height;
            xPoints[0] = (int)(infodrawx+infodrawwidth/2.0);
            yPoints[0] = (int)(infodrawy);
            xPoints[1] = (int)(infodrawx+infodrawwidth/4.0);
            yPoints[1] = (int)(infodrawy-infodrawheight/2.0);
            xPoints[2] = (int)(infodrawx-infodrawwidth/4.0);
            yPoints[2] = (int)(infodrawy-infodrawheight/2.0);
            xPoints[3] = (int)(infodrawx-infodrawwidth/2.0);
            yPoints[3] = (int)(infodrawy);
            xPoints[4] = (int)(infodrawx-infodrawwidth/4.0);
            yPoints[4] = (int)(infodrawy+infodrawheight/2.0);
            xPoints[5] = (int)(infodrawx+infodrawwidth/4.0);
            yPoints[5] = (int)(infodrawy+infodrawheight/2.0);
            /*
              generalPath.moveTo( xPoints[0], yPoints[0] );
              for( int i = 1 ; i < 6 ; i++ )
              generalPath.lineTo( xPoints[i], yPoints[i] );
              generalPath.closePath();
            */
            }
        return generalPath;
        }

    /** If drawing area intersects selected area, add last portrayed object to the bag */
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        GeneralPath generalPath = createGeneralPath( range );
        Area area = new Area( generalPath );
        return ( area.intersects( range.clip.x, range.clip.y, range.clip.width, range.clip.height ) );
        }
    }
