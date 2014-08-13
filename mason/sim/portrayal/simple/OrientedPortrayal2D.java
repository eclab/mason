/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import sim.display.*;
import java.awt.geom.*;
import java.awt.event.*;

/**
   A wrapper for other Portrayal2Ds which provides some kind of pointing object (typically a line)
   along the object's specified orientation angle. This is a very simple way to show orientation.
   
   <p>For the line to be drawn, the underlying object must adhere to the Oriented2D interface,
   which provides the orientation2D() method.  The line starts at the origin and is of length:

   <pre><tt>
   length:     (int)(scale * max(info.draw.width,info.draw.height)) + offset;
   </tt></pre>

   <p>... that is, or is a value which scales when you zoom in, and dr adds 
   additional fixed pixels.  The default is scale = 0.5, offset = 0, with a red color.
   
   <p>You can specify other shapes than a simple line.  We provide two others: kites and compasses. 

   <p><b>Note:  </b> One oddity of OrientedPortrayal2D is due to the fact that the line is only
   drawn if the object is being drawn.  While most FieldPortrayals ask objects just off-screen
   to draw themselves just to be careful, if an object is significantly off-screen, it may not
   be asked to draw itself, and so the orientation line will not be drawn -- even though part 
   of the orientation line could be on-screen at the time!  C'est la vie.
*/

public class OrientedPortrayal2D extends SimplePortrayal2D
    {
    public static final double DEFAULT_SCALE = 0.5;
    public static final int DEFAULT_OFFSET = 0;
    
    public static final int SHAPE_LINE = 0;
    public static final int SHAPE_KITE = 1;
    public static final int SHAPE_COMPASS = 2;

    /** The type of the oriented shape */
    int shape = SHAPE_LINE;
    
    /** The pre-scaling length */
    public double scale;
    
    /** The post-scaling length offset */   
    public int offset;
    
    /** The Paint or Color of the line */
    public Paint paint;
    
    public SimplePortrayal2D child;
    
    /** Overrides all drawing. */
    boolean showOrientation = true;
    
    public boolean drawFilled = true;
    public void setDrawFilled(boolean val) { drawFilled = val; }
    public boolean isDrawFilled() { return drawFilled; }
            
    public void setShape(int val) { if (val >= SHAPE_LINE && val <= SHAPE_COMPASS) shape = val; path = null; }
    public int getShape() { return shape; }
        
    public boolean isOrientationShowing() { return showOrientation; }
    public void setOrientationShowing(boolean val) { showOrientation = val; }

    /** @deprecated use isOrientationShowing() */
    public boolean isLineShowing() { return showOrientation; }
        
    /** @deprecated use setOrientationShowing() */
    public void setLineShowing(boolean val) { showOrientation = val; }
        
    Shape path = null;
        
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
        
    boolean onlyDrawWhenSelected = false;
        
    public void setOnlyDrawWhenSelected(boolean val) { onlyDrawWhenSelected = val; }
    public boolean getOnlyDrawWhenSelected() { return onlyDrawWhenSelected; }
    
    public OrientedPortrayal2D(SimplePortrayal2D child, int offset, double scale, Paint paint, int shape)
        {
        this.offset = offset; this.scale = scale; this.child = child;
        this.paint = paint; setShape(shape);
        }
    
    /** If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public OrientedPortrayal2D(SimplePortrayal2D child, int offset, double scale, Paint paint)
        {
        this(child,offset,scale,paint,SHAPE_LINE);
        }
    
    /** Draw a line of length scale = 0.5, offset = 0, in red.
        If child is null, then the underlying model object is presumed to be a Portrayal2D and will be used. */
    public OrientedPortrayal2D(SimplePortrayal2D child)
        {
        this(child, DEFAULT_OFFSET, DEFAULT_SCALE, Color.red);
        }
        
    /** Draw a line of the given length in red.
        If child is null, then the underlying model object is presumed to be a Portrayal2D and will be used. */
    public OrientedPortrayal2D(SimplePortrayal2D child, int offset, double scale)
        {
        this(child, offset, scale, Color.red);
        }

    /** Draw a line of length scale = 0.5, offset = 0.
        If child is null, then the underlying model object is presumed to be a Portrayal2D and will be used. */
    public OrientedPortrayal2D(SimplePortrayal2D child, Paint paint)
        {
        this(child, DEFAULT_OFFSET, DEFAULT_SCALE, paint);
        }

    public SimplePortrayal2D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal2D))
                throw new RuntimeException("Object provided to OrientedPortrayal2D is not a SimplePortrayal2D: " + object);
            return (SimplePortrayal2D) object;
            }
        }
    
    int[] simplePolygonX = new int[4];
    int[] simplePolygonY = new int[4];
    double[] simplePolygonXd = new double[4];
    double[] simplePolygonYd = new double[4];
    double lastLength = Double.NaN;
    AffineTransform transform = new AffineTransform();
    Stroke stroke = new BasicStroke();
    
    /** Returns the orientation of the underlying object, or NaN if there is no such orientation. 
        The default implementation assumes that the object is non-null and is an instance of Oriented2D,
        and calls orientation2D() on it; else it returns NaN. */
        
    public double getOrientation(Object object, DrawInfo2D info)
        {
        if (object != null && object instanceof Oriented2D)
            return ((Oriented2D)object).orientation2D();
        else return Double.NaN;
        }
        
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        // draw the underlying object first?
        if (shape == SHAPE_LINE || !drawFilled)
            getChild(object).draw(object,graphics,info);

        if (showOrientation && (info.selected || !onlyDrawWhenSelected))
            {
            double theta = getOrientation(object, info);
            if (theta == theta)  // NaN != NaN
                {
                double length = (scale * (info.draw.width < info.draw.height ? 
                        info.draw.width : info.draw.height)) + offset;  // fit in smallest dimension
                if (length != lastLength) 
                    { lastLength = length; path = null; }  // redo shape
            
                graphics.setPaint(paint);
                        
                if (info.precise)               // real-valued drawing, slightly slower
                    {
                    transform.setToTranslation(info.draw.x, info.draw.y);
                    transform.rotate(theta);
                                                                
                    final double lenx = 1.0 * length;               // oriented forwards
                    final double leny = 0.0 * length;
                    switch(shape)
                        {
                        default:                        // NOTE FALL THRU
                        case SHAPE_LINE:
                            if (path == null)
                                {
                                path = new Line2D.Double(0,0,0,length);
                                }
                            graphics.setStroke(stroke);
                            graphics.draw(transform.createTransformedShape(path));
                            break;
                        case SHAPE_KITE:
                            if (path == null)
                                {
                                simplePolygonXd[0] = (0 + lenx);
                                simplePolygonYd[0] = (0 + leny);
                                simplePolygonXd[1] = (0 + -leny + -lenx);
                                simplePolygonYd[1] = (0 + lenx + -leny);
                                simplePolygonXd[2] = (0 + -lenx/2);
                                simplePolygonYd[2] = (0 + -leny/2);
                                simplePolygonXd[3] = (0 + leny + -lenx);
                                simplePolygonYd[3] = (0 + -lenx + -leny);
                                path = buildPolygon(simplePolygonXd, simplePolygonYd);
                                }
                            if (drawFilled) 
                                graphics.fill(transform.createTransformedShape(path));
                            else 
                                {
                                graphics.setStroke(stroke);
                                graphics.draw(transform.createTransformedShape(path));
                                }
                            break;
                        case SHAPE_COMPASS:
                            if (path == null)
                                {
                                simplePolygonXd[0] = (0 + lenx);
                                simplePolygonYd[0] = (0 + leny);
                                simplePolygonXd[1] = (0 + -leny/2);
                                simplePolygonYd[1] = (0 + lenx/2);
                                simplePolygonXd[2] = (0 + -lenx/2);
                                simplePolygonYd[2] = (0 + -leny/2);
                                simplePolygonXd[3] = (0 + leny/2);
                                simplePolygonYd[3] = (0 + -lenx/2);
                                path = buildPolygon(simplePolygonXd, simplePolygonYd);
                                }
                            if (drawFilled) 
                                graphics.fill(transform.createTransformedShape(path));  
                            else 
                                {
                                graphics.setStroke(stroke);
                                graphics.draw(transform.createTransformedShape(path));
                                }
                            break;
                        }
                    }
                else                    // integer drawing
                    {
                    final double lenx = Math.cos(theta)*length;
                    final double leny = Math.sin(theta)*length;
                    switch(shape)
                        {
                        default:                // NOTE FALL THRU
                        case SHAPE_LINE:
                            graphics.drawLine((int)info.draw.x,
                                (int)info.draw.y,
                                (int)(info.draw.x + lenx),
                                (int)(info.draw.y + leny));
                            break;
                        case SHAPE_KITE:
                            simplePolygonX[0] = (int)(info.draw.x + lenx);
                            simplePolygonY[0] = (int)(info.draw.y + leny);
                            simplePolygonX[1] = (int)(info.draw.x + -leny + -lenx);
                            simplePolygonY[1] = (int)(info.draw.y + lenx + -leny);
                            simplePolygonX[2] = (int)(info.draw.x + -lenx/2);
                            simplePolygonY[2] = (int)(info.draw.y + -leny/2);
                            simplePolygonX[3] = (int)(info.draw.x + leny + -lenx);
                            simplePolygonY[3] = (int)(info.draw.y + -lenx + -leny);
                            if (drawFilled) graphics.fillPolygon(simplePolygonX, simplePolygonY, 4);
                            else graphics.drawPolygon(simplePolygonX, simplePolygonY, 4);
                            break;
                        case SHAPE_COMPASS:
                            simplePolygonX[0] = (int)(info.draw.x + lenx);
                            simplePolygonY[0] = (int)(info.draw.y + leny);
                            simplePolygonX[1] = (int)(info.draw.x + -leny/2);
                            simplePolygonY[1] = (int)(info.draw.y + lenx/2);
                            simplePolygonX[2] = (int)(info.draw.x + -lenx/2);
                            simplePolygonY[2] = (int)(info.draw.y + -leny/2);
                            simplePolygonX[3] = (int)(info.draw.x + leny/2);
                            simplePolygonY[3] = (int)(info.draw.y + -lenx/2);
                            if (drawFilled) graphics.fillPolygon(simplePolygonX, simplePolygonY, 4);
                            else graphics.drawPolygon(simplePolygonX, simplePolygonY, 4);
                            break;
                        }
                    }
                }
            }
            
        // draw the underlying object last?
        if (shape != SHAPE_LINE && drawFilled)
            getChild(object).draw(object,graphics,info);
        }
        
        
    boolean orientationHittable = true;
    /** Returns true if the orientation marker can be hit as part of the object.  By default the answer is YES. */ 
    public boolean isOrientationHittable() { return orientationHittable; }
    /** Sets whether or not the orientation marker can be hit as part of the object. */ 
    public void setOrientationHittable(boolean val) { orientationHittable = val; }
        
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        if (getChild(object).hitObject(object,range)) return true;
        if (!orientationHittable) return false;
                
        // now additionally determine if I was hit

        if (showOrientation && (object!=null) && (object instanceof Oriented2D))
            {
            final double theta = ((Oriented2D)object).orientation2D();
            final double length = ((scale * (range.draw.width < range.draw.height ? 
                        range.draw.width : range.draw.height)) + offset);  // fit in smallest dimension
            
            // we'll always do precise hitting
                        
            transform.setToTranslation(range.draw.x, range.draw.y);
            transform.rotate(theta);
                                                        
            final double lenx = 1.0 * length;               // oriented forwards
            final double leny = 0.0 * length;
            switch(shape)
                {
                default:                // NOTE FALL THRU
                case SHAPE_LINE: { break; }  // hard to hit a line
                case SHAPE_KITE:
                    if (path == null)
                        {
                        simplePolygonXd[0] = (0 + lenx);
                        simplePolygonYd[0] = (0 + leny);
                        simplePolygonXd[1] = (0 + -leny + -lenx);
                        simplePolygonYd[1] = (0 + lenx + -leny);
                        simplePolygonXd[2] = (0 + -lenx/2);
                        simplePolygonYd[2] = (0 + -leny/2);
                        simplePolygonXd[3] = (0 + leny + -lenx);
                        simplePolygonYd[3] = (0 + -lenx + -leny);
                        path = buildPolygon(simplePolygonXd, simplePolygonYd);
                        }
                    return transform.createTransformedShape(path).intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
                    //break;
                case SHAPE_COMPASS:
                    if (path == null)
                        {
                        simplePolygonXd[0] = (0 + lenx);
                        simplePolygonYd[0] = (0 + leny);
                        simplePolygonXd[1] = (0 + -leny/2);
                        simplePolygonYd[1] = (0 + lenx/2);
                        simplePolygonXd[2] = (0 + -lenx/2);
                        simplePolygonYd[2] = (0 + -leny/2);
                        simplePolygonXd[3] = (0 + leny/2);
                        simplePolygonYd[3] = (0 + -lenx/2);
                        path = buildPolygon(simplePolygonXd, simplePolygonYd);
                        }
                    return transform.createTransformedShape(path).intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
                    //break;
                }
            }
        return false;
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        return getChild(wrapper.getObject()).setSelected(wrapper, selected);
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        return getChild(wrapper.getObject()).getInspector(wrapper,state);
        }
    
    public String getName(LocationWrapper wrapper)
        {
        return getChild(wrapper.getObject()).getName(wrapper);
        }

    public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper,
        MouseEvent event, DrawInfo2D fieldPortrayalDrawInfo, int type)
        {
        return getChild(wrapper.getObject()).handleMouseEvent(guistate, manipulating, wrapper, event, fieldPortrayalDrawInfo, type);  // let someone else have it
        }
    }
    
    
    
