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
   A simple portrayal for 2D visualization of java.awt.Shapes. It extends the SimplePortrayal2D and
   it manages the drawing and hit-testing for shapes.
*/

public class ShapePortrayal2D extends SimplePortrayal2D
    {
    public Paint paint;
    public double scale;
    public Shape shape;
    AffineTransform transform = new AffineTransform();

    double bufferedWidth;
    double bufferedHeight;
    Shape bufferedShape;
    
    public ShapePortrayal2D(Shape shape) { this(shape,Color.gray,1.0); }
    public ShapePortrayal2D(Shape shape, Paint paint) { this(shape,paint,1.0); }
    public ShapePortrayal2D(Shape shape, double scale) { this(shape,Color.gray,scale); }
    
    public ShapePortrayal2D(Shape shape, Paint paint, double scale)
        {
        this.shape = shape;
        this.paint = paint;
        this.scale = scale;
        }
    
    // assumes the graphics already has its color set
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        final double width = info.draw.width*scale;
        final double height = info.draw.height*scale;
        if (bufferedShape == null || width != bufferedWidth || height != bufferedHeight)
            {
            transform.setToScale(bufferedWidth = width, bufferedHeight = height);
            bufferedShape = transform.createTransformedShape(shape);
            }

        graphics.setPaint(paint);
        // we are doing a simple draw, so we ignore the info.clip

        // draw centered on the origin
        transform.setToTranslation(info.draw.x,info.draw.y);
        graphics.fill(transform.createTransformedShape(bufferedShape));
        }

    public boolean hitObject(Object object, DrawInfo2D range)
        {
        final double width = range.draw.width*scale;
        final double height = range.draw.height*scale;
        if (bufferedShape == null || width != bufferedWidth || height != bufferedHeight)
            {
            transform.setToScale(bufferedWidth = width, bufferedHeight = height);
            bufferedShape = transform.createTransformedShape(shape);
            }
        // center on the origin
        transform.setToTranslation(range.draw.x,range.draw.y);
                
        // now hit-test
        return new Area(transform.createTransformedShape(bufferedShape)).intersects(
            range.clip.x, range.clip.y, range.clip.width, range.clip.height);
        }
    }
