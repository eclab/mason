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
   it manages the drawing and hit-testing for shapes.  Various X and Y point arrays for constructing
   different default shapes are also provided.
*/

public class ShapePortrayal2D extends AbstractShapePortrayal2D
    {
    public Shape shape;
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
    
    public static final double[] X_POINTS_TRIANGLE_DOWN = new double[] {-0.5, 0, 0.5};
    public static final double[] Y_POINTS_TRIANGLE_DOWN = new double[] {-0.5, 0.5, -0.5};
    public static final double[] X_POINTS_TRIANGLE_UP = new double[] {-0.5, 0, 0.5};
    public static final double[] Y_POINTS_TRIANGLE_UP = new double[] {0.5, -0.5, 0.5};
    public static final double[] X_POINTS_TRIANGLE_RIGHT = new double[] {-0.5, -0.5, 0.5};
    public static final double[] Y_POINTS_TRIANGLE_RIGHT = new double[] {-0.5, 0.5, 0};
    public static final double[] X_POINTS_TRIANGLE_LEFT = new double[] {-0.5, 0.5, 0.5};
    public static final double[] Y_POINTS_TRIANGLE_LEFT = new double[] {0, 0.5, -0.5};
    public static final double[] X_POINTS_POINTER_RIGHT = new double[] {-0.5, 0.0, 0.5, 0.0, -0.5};
    public static final double[] Y_POINTS_POINTER_RIGHT = new double[] {-0.5, -0.5, 0,  0.5, 0.5};
    public static final double[] X_POINTS_POINTER_LEFT = new double[] {0.5, 0.0, -0.5, 0.0, 0.5};
    public static final double[] Y_POINTS_POINTER_LEFT = new double[] {-0.5, -0.5, 0,  0.5, 0.5};
    public static final double[] X_POINTS_POINTER_DOWN = new double[] {-0.5, -0.5, 0,  0.5, 0.5};
    public static final double[] Y_POINTS_POINTER_DOWN = new double[] {-0.5, 0.0, 0.5, 0.0, -0.5};
    public static final double[] X_POINTS_POINTER_UP = new double[] {-0.5, -0.5, 0,  0.5, 0.5};
    public static final double[] Y_POINTS_POINTER_UP = new double[] {0.5, 0.0, -0.5, 0.0, 0.5};
    public static final double[] X_POINTS_DIAMOND = new double[] {-0.5, 0, 0.5, 0};
    public static final double[] Y_POINTS_DIAMOND = new double[] {0, 0.5, 0, -0.5};
    public static final double[] X_POINTS_SQUARE = new double[] {-0.5, -0.5, 0.5, 0.5};
    public static final double[] Y_POINTS_SQUARE = new double[] {-0.5, 0.5, 0.5, -0.5};
    public static final double[] X_POINTS_BOWTIE = new double[] {-0.5, 0.5, 0.5, -0.5};
    public static final double[] Y_POINTS_BOWTIE = new double[] {-0.5, 0.5, -0.5, 0.5};
    public static final double[] X_POINTS_HOURGLASS = new double[] {-0.5, 0.5, -0.5, 0.5};
    public static final double[] Y_POINTS_HOURGLASS = new double[] {-0.5, 0.5, 0.5, -0.5};
    public static final double[] X_POINTS_COMPASS = new double[] {-0.5, -0.125, 0, 0.125, 0.5, 0.125, 0, -0.125};
    public static final double[] Y_POINTS_COMPASS = new double[] {0, -0.125, -0.5, -0.125, 0, 0.125, 0.5, 0.125};
    public static final double[] X_POINTS_STAR = new double[] {-0.5, 0, 0.5, 0.25, 0.5, 0, -0.5, -0.25};
    public static final double[] Y_POINTS_STAR = new double[] {-0.5, -0.25, -0.5, 0, 0.5, 0.25, 0.5, 0};
    public static final double[] X_POINTS_PARALLELOGRAM = new double[] {-0.5, 0.25, 0.5, -0.25, -0.5};
    public static final double[] Y_POINTS_PARALLELOGRAM = new double[] {0.5, 0.5, -0.5, -0.5, 0.5};
    
    static final double OCT_COORD = (1.0 / (1.0 + Math.sqrt(2))) / 2.0;  // About .2071067811, derived from Wikipedia's Octogon article :-)
    public static final double[] X_POINTS_OCTAGON = new double[] {-0.5, -0.5, -OCT_COORD, OCT_COORD, 0.5, 0.5, OCT_COORD, -OCT_COORD};
    public static final double[] Y_POINTS_OCTAGON = new double[] {-OCT_COORD, OCT_COORD, 0.5, 0.5, OCT_COORD, -OCT_COORD, -0.5, -0.5};

    // This hexagon, unlike HexagonalPortrayal2D, fits inside a 1x1 square centered at (0,0) and so looks somewhat stretched
    public static final double[] X_POINTS_HEXAGON = new double[] {-0.5, -0.25, 0.25, 0.5, 0.25, -0.25};
    public static final double[] Y_POINTS_HEXAGON = new double[] {0, 0.5, 0.5, 0, -0.5, -0.5};
    public static final double[] X_POINTS_HEXAGON_ROTATED = new double[] {0, 0.5, 0.5, 0, -0.5, -0.5};
    public static final double[] Y_POINTS_HEXAGON_ROTATED = new double[] {-0.5, -0.25, 0.25, 0.5, 0.25, -0.25};
    
    static final double[][] XPOINTS =
        {
        X_POINTS_TRIANGLE_UP, X_POINTS_TRIANGLE_DOWN, X_POINTS_TRIANGLE_LEFT, X_POINTS_TRIANGLE_RIGHT,
        X_POINTS_POINTER_UP, X_POINTS_POINTER_DOWN, X_POINTS_POINTER_LEFT, X_POINTS_POINTER_RIGHT,
        X_POINTS_DIAMOND, X_POINTS_SQUARE, X_POINTS_BOWTIE, X_POINTS_HOURGLASS, 
        X_POINTS_COMPASS, X_POINTS_STAR, X_POINTS_PARALLELOGRAM, X_POINTS_OCTAGON, 
        X_POINTS_HEXAGON, X_POINTS_HEXAGON_ROTATED
        };

    static final double[][] YPOINTS =
        {
        Y_POINTS_TRIANGLE_UP, Y_POINTS_TRIANGLE_DOWN, Y_POINTS_TRIANGLE_LEFT, Y_POINTS_TRIANGLE_RIGHT,
        Y_POINTS_POINTER_UP, Y_POINTS_POINTER_DOWN, Y_POINTS_POINTER_LEFT, Y_POINTS_POINTER_RIGHT,
        Y_POINTS_DIAMOND, Y_POINTS_SQUARE, Y_POINTS_BOWTIE, Y_POINTS_HOURGLASS, 
        Y_POINTS_COMPASS, Y_POINTS_STAR, Y_POINTS_PARALLELOGRAM, Y_POINTS_OCTAGON, 
        Y_POINTS_HEXAGON, Y_POINTS_HEXAGON_ROTATED
        };
    
    public static final int POLY_TRIANGLE_UP = 0;
    public static final int POLY_TRIANGLE_DOWN = 1;
    public static final int POLY_TRIANGLE_LEFT = 2;
    public static final int POLY_TRIANGLE_RIGHT = 3;
    public static final int POLY_POINTER_UP = 4;
    public static final int POLY_POINTER_DOWN = 5;
    public static final int POLY_POINTER_LEFT = 6;
    public static final int POLY_POINTER_RIGHT = 7;
    public static final int POLY_DIAMOND = 8;
    public static final int POLY_SQUARE = 9;
    public static final int POLY_BOWTIE = 10;
    public static final int POLY_HOURGLASS = 11;
    public static final int POLY_COMPASS = 12;
    public static final int POLY_STAR = 13;
    public static final int POLY_PARALLELOGRAM = 14;
    public static final int POLY_OCTAGON = 15;
    public static final int POLY_HEXAGON = 16;
    public static final int POLY_HEXAGON_ROTATED = 17;
    
    public static final Shape SHAPE_CIRCLE = new Ellipse2D.Float(-0.5f, -0.5f, 1.0f, 1.0f);
    public static final Shape SHAPE_ROUND_SQUARE = new RoundRectangle2D.Float(-0.5f, -0.5f, 1.0f, 1.0f, 0.1f, 0.1f);
    public static final Shape SHAPE_VERY_ROUND_SQUARE = new RoundRectangle2D.Float(-0.5f, -0.5f, 1.0f, 1.0f, 0.3f, 0.3f);
    public static final Area SHAPE_DELAY;
    public static final Area SHAPE_REVERSE_DELAY;
    public static final Area SHAPE_CHOMP;
    public static final Area SHAPE_PILL;
    public static final Area SHAPE_STORAGE;
        
    
    static
        {
        SHAPE_DELAY = new Area(new Rectangle2D.Float(-0.5f, -0.5f, 0.5f, 1.0f));
        SHAPE_DELAY.add(new Area(SHAPE_CIRCLE));
        SHAPE_REVERSE_DELAY = new Area(new Rectangle2D.Float(0f, -0.5f, 0.5f, 1.0f));
        SHAPE_REVERSE_DELAY.add(new Area(SHAPE_CIRCLE));
        SHAPE_CHOMP = new Area(new Rectangle2D.Float(-0.5f, -0.5f, 1.0f, 1.0f));
        SHAPE_CHOMP.subtract(new Area(new Ellipse2D.Float(0f, -0.5f, 1.0f, 1.0f)));
        Shape bigCircleLeft = new Ellipse2D.Float(-0.5f, -1f, 2f, 2f);
        Shape bigCircleRight = new Ellipse2D.Float(-1.5f, -1f, 2f, 2f);
        Shape farCircleRight = new Ellipse2D.Float(0.33f, -1f, 2f, 2f);         // this is an ESTIMATE
        SHAPE_PILL = new Area(bigCircleLeft);
        SHAPE_PILL.intersect(new Area(bigCircleRight));
        SHAPE_PILL.intersect(new Area(new Rectangle2D.Float(-0.5f, -0.5f, 1.0f, 1.0f))); 
        SHAPE_STORAGE = new Area(bigCircleLeft);
        SHAPE_STORAGE.subtract(new Area(farCircleRight));
        SHAPE_STORAGE.intersect(new Area(new Rectangle2D.Float(-0.5f, -0.5f, 1.0f, 1.0f))); 
        }
        
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

    /** New-style constructors.  Rather than having a "filled" flag which determines whether we
        stroke versus fill, we can do BOTH.  We do this by specifying a fill paint and a stroke
        paint, either of which can be NULL.  We also provide a stroke width and a scale. */
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, Paint fillPaint, Paint strokePaint, double strokeWidth, double scale)
        {
        this(xpoints, ypoints, fillPaint, strokePaint, new BasicStroke((float) strokeWidth), scale);
        }

    /** New-style constructors.  Rather than having a "filled" flag which determines whether we
        stroke versus fill, we can do BOTH.  We do this by specifying a fill paint and a stroke
        paint, either of which can be NULL.  We also provide a stroke and a scale. */
    public ShapePortrayal2D(double[] xpoints, double[] ypoints, Paint fillPaint, Paint strokePaint, Stroke stroke, double scale)
        {
        this.shape = buildPolygon(xpoints, ypoints);
        this.paint = fillPaint;
        this.strokePaint = strokePaint;
        this.fillPaint = fillPaint;
        this.scale = scale;
        this.filled = (fillPaint != null);
        setStroke(stroke);
        }

    /** New-style constructors.  Rather than having a "filled" flag which determines whether we
        stroke versus fill, we can do BOTH.  We do this by specifying a fill paint and a stroke
        paint, either of which can be NULL.  We also provide a stroke width and a scale. */
    public ShapePortrayal2D(int polygon, Paint fillPaint, Paint strokePaint, double strokeWidth, double scale)
        {
        this(polygon, fillPaint, strokePaint, new BasicStroke((float) strokeWidth), scale);
        }

    /** New-style constructors.  Rather than having a "filled" flag which determines whether we
        stroke versus fill, we can do BOTH.  We do this by specifying a fill paint and a stroke
        paint, either of which can be NULL.  We also provide a stroke and a scale. */
    public ShapePortrayal2D(int polygon, Paint fillPaint, Paint strokePaint, Stroke stroke, double scale)
        {
        this(XPOINTS[polygon], YPOINTS[polygon], fillPaint, strokePaint, stroke, scale);
        }


    /** New-style constructors.  Rather than having a "filled" flag which determines whether we
        stroke versus fill, we can do BOTH.  We do this by specifying a fill paint and a stroke
        paint, either of which can be NULL.  We also provide a stroke width and a scale. */
    public ShapePortrayal2D(Shape shape, Paint fillPaint, Paint strokePaint, double strokeWidth, double scale)
        {
        this(shape, fillPaint, strokePaint, new BasicStroke((float) strokeWidth), scale);
        }

    /** New-style constructors.  Rather than having a "filled" flag which determines whether we
        stroke versus fill, we can do BOTH.  We do this by specifying a fill paint and a stroke
        paint, either of which can be NULL.  We also provide a stroke and a scale. */
    public ShapePortrayal2D(Shape shape, Paint fillPaint, Paint strokePaint, Stroke stroke, double scale)
        {
        this.shape = shape;
        this.paint = fillPaint;
        this.strokePaint = strokePaint;
        this.fillPaint = fillPaint;
        this.scale = scale;
        this.filled = (fillPaint != null);
        setStroke(stroke);
        }
        
    public void setShape(Shape shape)
        {
        this.shape = shape;
        }

    public void setShape(double[] xpoints, double[] ypoints)
        {
        this.shape = buildPolygon(xpoints, ypoints);
        }
        
    public void setStroke(Stroke s)
        {
        stroke = s;
        }

    public void setStroke(double width)
        {
        setStroke(new BasicStroke((float) width));
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
        Shape sh = transform.createTransformedShape(bufferedShape);
                
        if (fillPaint != null || strokePaint != null)           // New Style
            {
            if (fillPaint != null)
                {
                graphics.setPaint(fillPaint);
                graphics.fill(sh);
                }
            if (strokePaint != null)
                {
                Stroke oldStroke = graphics.getStroke();
                graphics.setPaint(strokePaint);
                graphics.setStroke(stroke == null ? defaultStroke : stroke);
                graphics.draw(sh);
                graphics.setStroke(oldStroke);
                }
            }
        else if (filled)                // old style
            {
            graphics.fill(sh);
            }
        else            // old style
            {
            Stroke oldStroke = graphics.getStroke();
            graphics.setStroke(stroke == null ? defaultStroke : stroke);
            graphics.draw(sh);
            graphics.setStroke(oldStroke);
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
