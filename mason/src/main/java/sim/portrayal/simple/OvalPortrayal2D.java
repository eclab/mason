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

public class OvalPortrayal2D extends AbstractShapePortrayal2D
    {
    protected double offset = 0.0;  // used only by CircledPortrayal2D
    
    public OvalPortrayal2D() { this(Color.gray,1.0, true); }
    public OvalPortrayal2D(Paint paint) { this(paint,1.0, true); }
    public OvalPortrayal2D(double scale) { this(Color.gray,scale, true); }
    public OvalPortrayal2D(Paint paint, double scale) { this(paint, scale, true); }
    public OvalPortrayal2D(Paint paint, boolean filled) { this(paint,1.0, filled); }
    public OvalPortrayal2D(double scale, boolean filled) { this(Color.gray,scale, filled); }

    public OvalPortrayal2D(Paint paint, double scale, boolean filled)
        {
        this.paint = paint;
        this.scale = scale;
        this.filled = filled;
        }

    // we must be transient because Ellipse2D.Double is not serializable.
    // We also check to see if it's null elsewhere (because it's transient).
    transient Ellipse2D.Double preciseEllipse = new Ellipse2D.Double();
        
    // assumes the graphics already has its color set
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        Rectangle2D.Double draw = info.draw;
        final double width = draw.width*scale + offset;
        final double height = draw.height*scale + offset;

        graphics.setPaint(paint);
        // we are doing a simple draw, so we ignore the info.clip

        if (info.precise)
            {
            if (preciseEllipse == null) preciseEllipse = new Ellipse2D.Double();    // could get reset because it's transient
            preciseEllipse.setFrame(info.draw.x - width/2.0, info.draw.y - height/2.0, width, height);
            if (filled) graphics.fill(preciseEllipse);
            else graphics.draw(preciseEllipse);
            return;
            }
            
        final int x = (int)(draw.x - width / 2.0);
        final int y = (int)(draw.y - height / 2.0);
        int w = (int)(width);
        int h = (int)(height);
                
        // draw centered on the origin
        if (filled)
            graphics.fillOval(x,y,w,h);
        else
            graphics.drawOval(x,y,w,h);
        }

    /** If drawing area intersects selected area, add last portrayed object to the bag */
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        if (preciseEllipse == null) preciseEllipse = new Ellipse2D.Double();
        final double SLOP = 1.0;  // need a little extra area to hit objects
        final double width = range.draw.width*scale;
        final double height = range.draw.height*scale;
        preciseEllipse.setFrame( range.draw.x-width/2-SLOP, range.draw.y-height/2-SLOP, width+SLOP*2,height+SLOP*2 );
        return ( preciseEllipse.intersects( range.clip.x, range.clip.y, range.clip.width, range.clip.height ) );
        }
    }
