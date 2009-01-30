/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import sim.display.*;

/**
   A wrapper for other Portrayal2Ds which provides some kind of pointing object (typically a line)
   along the object's specified orientation angle. This is a very simple way to show orientation.
   The underlying Portrayal2D may also be automatically rotated if autoRotate() is turned on (it's off by default,
   as rotation is expensive.
   
   <p>For the line to be drawn, the underlying object must adhere to the Oriented2D interface,
   which provides the orientation2D() method.  The line starts at the origin and is of length:

   <pre><tt>
   length:     (int)(or * max(info.draw.width,info.draw.height)) + dr;
   </tt></pre>

   <p>... that is, or is a value which scales when you zoom in, and dr adds 
   additional fixed pixels.  The default is or = 0.5, dr = 0, with a red color.
   
   <p>You can specify other shapes than a simple line.  We provide two others: kites and compasses. 

   <p><b>Note:  </b> One oddity of OrientedPortrayal2D is due to the fact that the line is only
   drawn if the object is being drawn.  While most FieldPortrayals ask objects just off-screen
   to draw themselves just to be careful, if an object is significantly off-screen, it may not
   be asked to draw itself, and so the orientation line will not be drawn -- even though part 
   of the orientation line could be on-screen at the time!  C'est la vie.
*/

public class OrientedPortrayal2D extends SimplePortrayal2D
    {
    public static final double DEFAULT_OR = 0.5;
    public static final int DEFAULT_DR = 0;
    
    public static final int SHAPE_LINE = 0;
    public static final int SHAPE_KITE = 1;
    public static final int SHAPE_COMPASS = 2;

    /** The type of the oriented shape */
    int shape = SHAPE_LINE;
    
    /** The pre-scaling length */
    public double or;
    
    /** The post-scaling length offset */   
    public int dr;
    
    /** The Paint or Color of the line */
    public Paint paint;
    
    public SimplePortrayal2D child;
    
    /** Overrides all drawing. */
    boolean showLine = true;
    
    public void setShape(int val) { if (val >= SHAPE_LINE && val <= SHAPE_COMPASS) shape = val; }
    public int getShape() { return shape; }
    public boolean isLineShowing() { return showLine; }
    public void setLineShowing(boolean val) { showLine = val; }
    
    public OrientedPortrayal2D(SimplePortrayal2D child, int dr, double or, Paint paint, int shape)
        {
        this.dr = dr; this.or = or; this.child = child;
        this.paint = paint; this.shape = shape;
        }
    
    /** If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public OrientedPortrayal2D(SimplePortrayal2D child, int dr, double or, Paint paint)
        {
        this(child,dr,or,paint,SHAPE_LINE);
        }
    
    /** Draw a line of length or = 0.5 dr = 0, in red.
        If child is null, then the underlying model object is presumed to be a Portrayal2D and will be used. */
    public OrientedPortrayal2D(SimplePortrayal2D child)
        {
        this(child, DEFAULT_DR, DEFAULT_OR, Color.red);
        }
        
    /** Draw a line of the given length in red.
        If child is null, then the underlying model object is presumed to be a Portrayal2D and will be used. */
    public OrientedPortrayal2D(SimplePortrayal2D child, int dr, double or)
        {
        this(child, dr, or, Color.red);
        }

    /** Draw a line of length or = 0.5, dr = 0.
        If child is null, then the underlying model object is presumed to be a Portrayal2D and will be used. */
    public OrientedPortrayal2D(SimplePortrayal2D child, Paint paint)
        {
        this(child, DEFAULT_DR, DEFAULT_OR, paint);
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
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        getChild(object).draw(object,graphics,info);

        if (showLine && (object!=null) && (object instanceof Oriented2D))
            {
            final double theta = ((Oriented2D)object).orientation2D();
            final int length = ((int)(or * (info.draw.width < info.draw.height ? 
                        info.draw.width : info.draw.height)) + dr);  // fit in smallest dimension
            
            final double lenx = Math.cos(theta)*length;
            final double leny = Math.sin(theta)*length;
            final int x = (int)(info.draw.x + lenx);
            final int y = (int)(info.draw.y + leny);

            graphics.setPaint(paint);
            switch(shape)
                {
                default:
                case SHAPE_LINE:
                {
                graphics.drawLine((int)info.draw.x,(int)info.draw.y,x,y);
                } break;
                case SHAPE_KITE:
                {
                simplePolygonX[0] = x;
                simplePolygonY[0] = y;
                simplePolygonX[1] = (int)(info.draw.x + -leny + -lenx);
                simplePolygonY[1] = (int)(info.draw.y + lenx + -leny);
                simplePolygonX[2] = (int)(info.draw.x + -lenx/2);
                simplePolygonY[2] = (int)(info.draw.y + -leny/2);
                simplePolygonX[3] = (int)(info.draw.x + leny + -lenx);
                simplePolygonY[3] = (int)(info.draw.y + -lenx + -leny);
                graphics.fillPolygon(simplePolygonX, simplePolygonY, 4);
                } break;
                case SHAPE_COMPASS:
                {
                simplePolygonX[0] = (int)(info.draw.x + lenx);
                simplePolygonY[0] = (int)(info.draw.y + leny);
                simplePolygonX[1] = (int)(info.draw.x + -leny/2);
                simplePolygonY[1] = (int)(info.draw.y + lenx/2);
                simplePolygonX[2] = (int)(info.draw.x + -lenx/2);
                simplePolygonY[2] = (int)(info.draw.y + -leny/2);
                simplePolygonX[3] = (int)(info.draw.x + leny/2);
                simplePolygonY[3] = (int)(info.draw.y + -lenx/2);
                graphics.fillPolygon(simplePolygonX, simplePolygonY, 4);
                } break;
                }
            }
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

        if (showLine && (object!=null) && (object instanceof Oriented2D))
            {
            final double theta = ((Oriented2D)object).orientation2D();
            final int length = ((int)(or * (range.draw.width < range.draw.height ? 
                        range.draw.width : range.draw.height)) + dr);  // fit in smallest dimension
            
            final double lenx = Math.cos(theta)*length;
            final double leny = Math.sin(theta)*length;
            final int x = (int)(range.draw.x + lenx);
            final int y = (int)(range.draw.y + leny);

            switch(shape)
                {
                default: case SHAPE_LINE: { break; }  // hard to hit a line
                case SHAPE_KITE:
                {
                simplePolygonX[0] = x;
                simplePolygonY[0] = y;
                simplePolygonX[1] = (int)(range.draw.x + -leny + -lenx);
                simplePolygonY[1] = (int)(range.draw.y + lenx + -leny);
                simplePolygonX[2] = (int)(range.draw.x + -lenx/2);
                simplePolygonY[2] = (int)(range.draw.y + -leny/2);
                simplePolygonX[3] = (int)(range.draw.x + leny + -lenx);
                simplePolygonY[3] = (int)(range.draw.y + -lenx + -leny);
                return new Polygon(simplePolygonX,simplePolygonY,4).intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
                //break;
                }
                case SHAPE_COMPASS:
                {
                simplePolygonX[0] = (int)(range.draw.x + lenx);
                simplePolygonY[0] = (int)(range.draw.y + leny);
                simplePolygonX[1] = (int)(range.draw.x + -leny/2);
                simplePolygonY[1] = (int)(range.draw.y + lenx/2);
                simplePolygonX[2] = (int)(range.draw.x + -lenx/2);
                simplePolygonY[2] = (int)(range.draw.y + -leny/2);
                simplePolygonX[3] = (int)(range.draw.x + leny/2);
                simplePolygonY[3] = (int)(range.draw.y + -lenx/2);
                return new Polygon(simplePolygonX,simplePolygonY,4).intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
                // break;
                }
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
    }
    
    
    
