/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import java.awt.geom.*;
import sim.display.*;

/**
   A simple portrayal for 2D visualization of ovals. It extends the SimplePortrayal2D and
   it manages the drawing and hit-testing for oval shapes.
*/

public class OvalPortrayal2D extends SimplePortrayal2D
    {
    public Paint paint;
    public double scale;
    //boolean drawSmaller = Display2D.isMacOSX && !Display2D.javaVersion.startsWith("1.3"); // fix a bug in OS X
    
    public OvalPortrayal2D() { this(Color.gray,1.0); }
    public OvalPortrayal2D(Paint paint) { this(paint,1.0); }
    public OvalPortrayal2D(double scale) { this(Color.gray,scale); }
    
    public OvalPortrayal2D(Paint paint, double scale)
        {
        this.paint = paint;
        this.scale = scale;
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
        int w = (int)(width);
        int h = (int)(height);
        //if (drawSmaller) { --w; --h; }
                
        // draw centered on the origin
        graphics.fillOval(x,y,w, h);
        }

    /** If drawing area intersects selected area, add last portrayed object to the bag */
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        final double SLOP = 1.0;  // need a little extra area to hit objects
        final double width = range.draw.width*scale;
        final double height = range.draw.height*scale;
        Ellipse2D.Double ellipse = new Ellipse2D.Double( range.draw.x-width/2-SLOP, range.draw.y-height/2-SLOP, width+SLOP*2,height+SLOP*2 );
        return ( ellipse.intersects( range.clip.x, range.clip.y, range.clip.width, range.clip.height ) );
        }
    }
