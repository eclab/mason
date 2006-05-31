/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;

/**
   A simple portrayal for 2D visualization of rectangles. It extends the SimplePortrayal2D and
   it manages the drawing and hit-testing for rectangular shapes.
*/

public class RectanglePortrayal2D extends SimplePortrayal2D
    {
    public Paint paint;
    public double scale;

    public RectanglePortrayal2D() { this(Color.gray,1.0); }
    public RectanglePortrayal2D(Paint paint) { this(paint,1.0); }
    public RectanglePortrayal2D(double scale) { this(Color.gray,scale); }
    
    public RectanglePortrayal2D(Paint paint, double scale)
        {
        this.paint = paint;
        this.scale = scale;
        }
                
    /** If drawing area intersects selected area, add last portrayed object to the bag */
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        final double width = range.draw.width*scale;
        final double height = range.draw.height*scale;
        return( range.clip.intersects( range.draw.x-width/2, range.draw.y-height/2, width, height ) );
        }

    // assumes the graphics already has its color set
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        final double width = info.draw.width*scale;
        final double height = info.draw.height*scale;

        graphics.setPaint(paint);
        // we are doing a simple draw, so we ignore the info.clip

        final int x = (int)(info.draw.x - width / 2.0);
        final int y = (int)(info.draw.y - height / 2.0);
        final int w = (int)(width);
        final int h = (int)(height);

        // draw centered on the origin
        graphics.fillRect(x,y,w,h);
        }
        
    }
