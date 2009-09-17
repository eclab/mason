/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import java.awt.*;
import java.awt.geom.*;
import sim.util.*;

/** 
    Superclass of all Field Portrayals in 2D.  Implements default versions of the
    necessary methods, which you should feel free to override (especially portray,
    hit, and draw!).
    
    <p>The default version of the setField(...) method sets the field without checking
    to see if it's a valid field or not; you'll want to override this to check.  Also,
    the default version of getDefaultPortrayal() returns an empty SimplePortrayal: you'll
    might want to override that as well.  These defaults enable you to use FieldPortrayal2D
    as an "empty field portrayal" to be attached to a Display2D and draw arbitrary things
    when its draw(...) method is called.  In that case, all you need to do is override
    the draw(...) method and you're set.  
    
    <p>The default versions of hitObjects and draw call a protected but empty method
    called hitOrDraw.  It's very common that a field's hitObjects and draw methods will
    be identical save for a single line which calls hitObjects or draw on an underlying
    SimplePortrayal.  So most fields unify both methods into this single method
    and unify the line as something like this:
    
    <pre><tt>
    if (graphics == null)
    {
    if (portrayal.hitObject(portrayedObject, newinfo))
    putInHere.add(getWrapper(portrayedObject, portrayedObjectLocation));
    }
    else
    portrayal.draw(portrayedObject, graphics, newinfo);
    </tt></pre>
    
    <p>...where <i>getWrapper(...)</i> returns a LocationWrapper appropriate to the Field.
*/

public abstract class FieldPortrayal2D extends FieldPortrayal implements Portrayal2D
    {
    /** Draws the field with its origin at [info.draw.x,info.draw.y], relative to the 
        scaled coordinate system defined by [info.draw.width,info.draw.height].  The
        only parts that need be drawn are those which fall within the [info.clip] rectangle.
        Since your draw and hitObjects methods are likely to be nearly identical, you can choose
        instead to override the hitOrDraw method to handle both of them.  By default this
        method simply calls hitOrDraw.  FieldPortrayals will receive null for the object;
        they should just draw their own fields. */
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        hitOrDraw(graphics, info, null);
        }
        
    /** Adds to the provided Bag LocationWrappers for any objects which
        overlap the provided hit range.  The hit range will usually
        define a single point, but COULD be a range. The object 
        should perceive itself as located at the (range.draw.x,range.draw.y) origin and drawn
        relative to the (range.draw.width,range.draw.height) scale.  */
    public void hitObjects(DrawInfo2D range, Bag putInHere)
        {
        hitOrDraw(null, range, putInHere);
        }

    
    /** Instead of overriding the draw and hitObjects methods, you can optionally override
        this method to provide <i>both</i> the draw(...) and hitObjects(...)
        functionality in a single method, as it's common that these two methods have nearly
        identical code.  You should test which operation to do
        based on whether or not graphics is null (if it is, you're hitting, else you're drawing).  */
    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        }
    
    SimplePortrayal2D simple = new SimplePortrayal2D();
    public Portrayal getDefaultPortrayal()
        {
        return simple;
        }
        
    public void setField(Object field) { this.field = field; dirtyField = true; }
    
    /** Returns the location of the given object were it to be drawn on the screen.  Negative locations are acceptable.
        If null is returned, then the portrayal is unable to determine the location of the object.  The default implementation
        returns null. */
    public Point2D.Double getPositionInFieldPortrayal(Object object, DrawInfo2D fieldPortrayalInfo)
        {
        return null;
        }
        
    /**
       Moves the Bag of LocationWrappers by the provided amount.
    */
    // Hmmm, Point2D has a Point, Point2D.Float, and Point2D.Double.
    // Rectangle2D has a Rectangle, Rectangle2D.Float, and Rectangle2D.Double.
    // But Dimension2D *only* has a Dimension (int).  You have to make your
    // own Dimension2D double subclass!!
    public void move(Bag locationWrappers, Dimension2D distance)
        {
        for(int x=0;x<locationWrappers.numObjs;x++)
            {
            LocationWrapper wrapper = (LocationWrapper)(locationWrappers.objs[x]);
            ((SimplePortrayal2D)(getPortrayalForObject(wrapper.getObject()))).move(wrapper, distance);
            }
        }

    /** Default buffering: let the program decide on its own (typically in a platform-dependent fashion) */
    public static final int DEFAULT = 0;
    /** Use a buffer */
    public static final int USE_BUFFER = 1;
    /** Don't use a buffer */
    public static final int DONT_USE_BUFFER = 2;

    int buffering = DEFAULT;
    Object bufferingLock = new Object();

    /** Returns whether or not the FieldPortrayal2D will use a buffering "trick" to draw quickly.  This optional
        property is in FieldPortrayal2D but is only taken advantage of by one or two subclasses.  Some FieldPortrayal2Ds
        primarily draw lots of rectangles in a grid.  Certain ones can draw this in one of two ways: either by drawing
        each rectangle separately, or by filling a buffer (an image) with individual points, then stretching the buffer
        over the area, causing the points to enlarge into rectangles.  The second is often faster, especially if given
        lots of memory (and always faster on Macs regardless).  The FieldPortrayal2D's behavior in this regard will
        depend on the value of the buffering property.  The property can take on one of three values: 
        DEFAULT (let the machine decide on its own in a platform-dependent fashion -- the default), 
        USE_BUFFER, or DONT_USE_BUFFER. */  
    public int getBuffering() { synchronized(bufferingLock) { return buffering; } }

    /** Sets whether or not the FieldPortrayal2D will use a buffering "trick" to draw quickly.  This optional
        property is in FieldPortrayal2D but is only taken advantage of by one or two subclasses.  Some FieldPortrayal2Ds
        primarily draw lots of rectangles in a grid.  Certain ones can draw this in one of two ways: either by drawing
        each rectangle separately, or by filling a buffer (an image) with individual points, then stretching the buffer
        over the area, causing the points to enlarge into rectangles.  The second is often faster, especially if given
        lots of memory (and always faster on Macs regardless).  The FieldPortrayal2D's behavior in this regard will
        depend on the value of the buffering property.  The property can take on one of three values: 
        DEFAULT (let the machine decide on its own in a platform-dependent fashion -- the default), 
        USE_BUFFER, or DONT_USE_BUFFER. */  
    public void setBuffering(int val) { synchronized(bufferingLock) { buffering = val; } }
    }
