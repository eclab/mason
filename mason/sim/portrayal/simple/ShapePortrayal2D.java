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
   A simple portrayal for 2D visualization of java.awt.Shapes and java.awt.Polygons. It extends the SimplePortrayal2D and
   it manages the drawing and hit-testing for shapes.
*/

public class ShapePortrayal2D extends SimplePortrayal2D
    {
    public Paint paint;
    public double scale;
    public Shape shape;
    public Stroke stroke;
    public boolean filled;
    AffineTransform transform = new AffineTransform();

    double[] xPoints = null;
    double[] yPoints = null;
    double[] scaledXPoints = null;
    double[] scaledYPoints = null;
    int[] translatedXPoints = null;
    int[] translatedYPoints = null;
    double scaling;

    double bufferedWidth;
    double bufferedHeight;
    Shape bufferedShape;
    
    Shape buildPolygon(double[] xpoints, double[] ypoints)
        {
        GeneralPath path = new GeneralPath();
        // general paths are only floats and not doubles in Java 1.4, 1.5
        // in 1.6 it's been changed to doubles finally but we're not there yet.
        if (xpoints.length > 0) path.moveTo((float)xpoints[0], (float)ypoints[0]);
        for(int i=xpoints.length-1; i >= 0; i--)
            path.lineTo((float)xpoints[i], (float)ypoints[i]);
        return path;
        }
    
    public ShapePortrayal2D(double[] xpoints, double[] ypoints) { this(xpoints, ypoints, Color.gray,1.0,true); }
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, Paint paint) { this(xpoints, ypoints,paint,1.0,true); }
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, double scale) { this(xpoints, ypoints,Color.gray,scale,true); }
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, Paint paint, double scale) { this(xpoints, ypoints, paint,scale,true); }
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, boolean filled) { this(xpoints, ypoints,Color.gray,1.0,filled); }
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, Paint paint, boolean filled) { this(xpoints, ypoints,paint,1.0,filled); }
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, double scale, boolean filled) { this(xpoints, ypoints,Color.gray,scale,filled); }
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, Paint paint, double scale, boolean filled)
        {
        this(null, paint, scale, filled);
        this.shape = buildPolygon(xpoints,ypoints);
        this.xPoints = xpoints;
        this.yPoints = ypoints;
        this.scaledXPoints = new double[xpoints.length];
        this.scaledYPoints = new double[ypoints.length];
        this.translatedXPoints = new int[xpoints.length];
        this.translatedYPoints = new int[ypoints.length];
        }

    public ShapePortrayal2D(Shape shape) { this(shape,Color.gray,1.0,true); }
    public ShapePortrayal2D(Shape shape, Paint paint) { this(shape,paint,1.0,true); }
    public ShapePortrayal2D(Shape shape, double scale) { this(shape,Color.gray,scale,true); }
    public ShapePortrayal2D(Shape shape, Paint paint, double scale) { this(shape, paint,scale,true); }
    public ShapePortrayal2D(Shape shape, boolean filled) { this(shape,Color.gray,1.0,filled); }
    public ShapePortrayal2D(Shape shape, Paint paint, boolean filled) { this(shape,paint,1.0,filled); }
    public ShapePortrayal2D(Shape shape, double scale, boolean filled) { this(shape,Color.gray,scale,filled); }
    public ShapePortrayal2D(Shape shape, Paint paint, double scale, boolean filled)
        {
        this.shape = shape;
        this.paint = paint;
        this.scale = scale;
        this.filled = filled;
        setStroke(null);
        }
    
    boolean strokeSet = false;
    public void setStroke(Stroke s)
        {
        stroke = s;
        if (stroke == null)
            {
            stroke = new BasicStroke();
            strokeSet = false;
            }
        else strokeSet = true;
        }
        
    // assumes the graphics already has its color set
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        graphics.setPaint(paint);
        if (true)//info.precise || xPoints == null || strokeSet)
            {   
            final double width = info.draw.width*scale;
            final double height = info.draw.height*scale;
            if (bufferedShape == null || width != bufferedWidth || height != bufferedHeight)
                {
                transform.setToScale(bufferedWidth = width, bufferedHeight = height);
                bufferedShape = transform.createTransformedShape(shape);
                }

            // we are doing a simple draw, so we ignore the info.clip

            // draw centered on the origin
            transform.setToTranslation(info.draw.x,info.draw.y);
            if (filled)
                {
                graphics.fill(transform.createTransformedShape(bufferedShape));
                }
            else
                {
                graphics.setStroke(stroke);
                graphics.draw(transform.createTransformedShape(bufferedShape));
                }
            }
        else   // faster by far         // NOTE:  Not any more.  On the Mac it's about 1% faster, not enough to worry about.
            {
            int len = xPoints.length;
            double[] scaledXPoints = this.scaledXPoints;
            double[] scaledYPoints = this.scaledYPoints;
            int[] translatedXPoints = this.translatedXPoints;
            int[] translatedYPoints = this.translatedYPoints;
            double x = info.draw.x;
            double y = info.draw.y;
            double width = scale * info.draw.width;

            // do we need to scale?
            if (scaling != width)
                {
                double[] xPoints = this.xPoints;
                double[] yPoints = this.yPoints;
                double height = scale * info.draw.height;
                for(int i=0;i<len;i++)
                    {
                    scaledXPoints[i] = xPoints[i] * width;
                    scaledYPoints[i] = yPoints[i] * height;
                    }
                scaling = width;
                }
                
            // always translate
            for(int i=0;i<len;i++)
                {
                translatedXPoints[i] = (int)(scaledXPoints[i] + x);
                translatedYPoints[i] = (int)(scaledYPoints[i] + y);
                }
            if (filled) graphics.fillPolygon(translatedXPoints, translatedYPoints,translatedXPoints.length);
            else graphics.drawPolygon(translatedXPoints, translatedYPoints,translatedXPoints.length);
            }
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
