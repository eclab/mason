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
   A simple portrayal for 2D visualization of rectangles. It extends the SimplePortrayal2D and
   it manages the drawing and hit-testing for rectangular shapes.
*/

public class RectanglePortrayal2D extends AbstractShapePortrayal2D
    {
    public RectanglePortrayal2D() { this(Color.gray,1.0, true); }
    public RectanglePortrayal2D(Paint paint) { this(paint,1.0, true); }
    public RectanglePortrayal2D(double scale) { this(Color.gray,scale, true); }
    public RectanglePortrayal2D(Paint paint, double scale) { this(paint, scale, true); }
    public RectanglePortrayal2D(Paint paint, boolean filled) { this(paint, 1.0, filled); }
    public RectanglePortrayal2D(double scale, boolean filled) { this(Color.gray, scale, filled); }
    
    public RectanglePortrayal2D(Paint paint, double scale, boolean filled)
        {
        this.paint = paint;
        this.scale = scale;
        this.filled = filled;
        }
                
    /** If drawing area intersects selected area, add last portrayed object to the bag */
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        final double width = range.draw.width*scale;
        final double height = range.draw.height*scale;
        return( range.clip.intersects( range.draw.x-width/2, range.draw.y-height/2, width, height ) );
        }

    // we must be transient because Rectangle2D.Double is not serializable.
    // We also check to see if it's null elsewhere (because it's transient).
    transient Rectangle2D.Double preciseRectangle = new Rectangle2D.Double();
        
    // assumes the graphics already has its color set
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        Rectangle2D.Double draw = info.draw;
        final double width = draw.width*scale;
        final double height = draw.height*scale;

        graphics.setPaint(paint);
        // we are doing a simple draw, so we ignore the info.clip

        if (info.precise)
            {
            if (preciseRectangle == null) preciseRectangle= new Rectangle2D.Double();  // could get reset because it's transient
            preciseRectangle.setFrame(info.draw.x - width/2.0, info.draw.y - height/2.0, width, height);
            if (filled) graphics.fill(preciseRectangle);
            else graphics.draw(preciseRectangle);
            return;
            }
                
        final int x = (int)(draw.x - width / 2.0);
        final int y = (int)(draw.y - height / 2.0);
        final int w = (int)(width);
        final int h = (int)(height);

        // draw centered on the origin
        if (filled)
            graphics.fillRect(x,y,w,h);
        else
            graphics.drawRect(x,y,w,h);
        }
        
    }
