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
import java.util.*;
import sim.util.gui.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.grid.*;
import sim.engine.*;
import sim.portrayal.network.*;
import java.awt.event.*;

/**
   <p>TrailedPortrayal2D is a special SimplePortrayal wrapper which enables you to draw "trails" or
   "mouse tails" that drag behind objects and show where the've recently been.  Unlike other "wrapper"
   style SimplePortrayals, like CircledPortrayal2D or LabelledPortrayal2D, TrailedPortrayal2D generally requires
   its own separate FieldPortrayal in which to draw the trails properly.
          
   <p>Let's say you have a 2D field called "field" (we strongly suggest only
   using trails in SparseGrid2D or in Continuous2D because of the costs involved in other spaces).  That
   field is presently being portrayed by a FieldPortrayal2D called, say, "fieldPortrayal".  fieldPortrayal2D
   is presently using a SimplePortrayal2D (we'll call it "simple") to draw those objects.  It's possible
   for the objects to draw themselves if you like.  fieldPortrayal is presently attached to a Display2D called
   "display".
          
   <p>To add trails, what you'll do is create a second FieldPortrayal2D, identical to fieldPortrayal, and
   which portrays the same field.  Let's call this one trailfieldportrayal.  Attach trailfieldportrayal to
   the display immediately *before* fieldPortrayal is attached, so it's drawn immediately before and thus
   the objects in fieldPortrayal get drawn on top of the trails.
           
   <p>Next you'll need to specify the SimplePortrayal2D for trailfieldportrayal.  To do this, you'll use
   a TrailPortrayal2D wrapped around a child SimplePortrayal2D, specifically, the same kind of SimplePortrayal2D as "simple" was.
   In fact you can use "simple" itself if you like.  If your objects portray themselves, pass in null
   as the child portrayal instead.  This child portrayal won't be drawn and won't be inspected: it exists solely to enable the TrailPortrayal2D
   to determine whether an object has been SELECTED and if it should start drawing the trail.
   That is, it's used for hit testing.
                
   <p>It's important that you provide a SEPARATE TrailPortrayal2D for EVERY SINGLE object in trailfieldportrayal.
   That is, you register it using <b>FieldPortrayal.setPortrayalForObject</b> rather than <b>FieldPortrayal.setPortrayalForAll</b> or some-such.
   This is because TrailPortrayal2D only maintains one trail at a time, so if you select another object, the previous
   trail appears to be attached to that other object (which is wrong).  If you use separate TrailPortrayal2Ds,
   each will manage the trail for the various objects in the field (which is right, but memory expensive and a bit slow).
        
   <p>Alternatively you can provide a single TrailPortrayal2D via <b>FieldPortrayal.setPortrayalForAll</b> but 
   set <b>onlyGrowTrailWhenSelected</b> to FALSE.  This will cause the TrailPortrayal2D to *begin* growing the trail
   each time a new object is selected.  It uses a lot less memory and is faster but may not produce the effect you desire.
        
   <p>You can also cause all the TrailPortrayal2Ds to draw trails regardless of selection: just set <b>onlyShowTrailWhenSelected</b> to FALSE 
   (and make sure you have separate TrailPortrayal2Ds for each object in the field).  This is a rare need.
        
   <p>To draw the actual trail segements, TrailPortrayal2D relies on a SECOND subsidiary SimplePortrayal2D called "trail".
   You can either provide your own custom SimplePortrayal2D or use the default one, which draws half-thick line segments of different colors. 
   Either way, this "trail" SimplePortrayal2D
   will be passed a TrailedPortrayal2D.TrailDrawInfo2D, which is a special version of DrawInfo2D which
   contains the previous point and also a value from 0.0 to 1.0 indicating how far back in time the
   segement is supposed to be -- you can use that to figure out what color to draw with, for example.

   <p>To handle big jumps (particularly toroidal wrap-arounds) elegantly, TrailedPortrayal2D has
   a maximumJump percentage.  For a jump to be drawn, it must be LESS than this percentage of
   the total width or height of the underlying field.
*/

public class TrailedPortrayal2D extends SimplePortrayal2D
    {
    /** A special version of DrawInfo2D which adds additional information useful for drawing your own trails.
        Instances of this class are what will be sent to the "trail" SimplePortrayal2D object when it is
        called upon to draw a trail segment.
                
        <p>The TrailDrawInfo2D adds two additional items beyond the standard DrawInfo2D stuff:
        <ul>
        <p><li><b>Point2D.Double <i>secondPoint</i></b>: (inherited from EdgeDrawInfo2D) this provides one end of the 
        trail segment to be drawn.  <tt>draw.x</tt> and <tt>draw.y</tt> provide the other end of the segment.
        <p><li><b>double <i>value</i></b>: a number between 0.0 and 1.0, representing how far back in time the segment represents.
        If 1.0, the segment is maximally far back in time and won't be drawn next time.  If 0.0, the segment is nearly brand
        new and probably quite close to the current location of the object.
        </ul>
    */
        
    public static class TrailDrawInfo2D extends EdgeDrawInfo2D
        {
        /** A value from 1.0 to 0.0 indicating how far "back in time" this segment is supposed to be. */
        public double value = 0.0;
        public TrailDrawInfo2D(GUIState gui, FieldPortrayal2D fieldPortrayal, RectangularShape draw, RectangularShape clip, Point2D.Double secondPoint) { super(gui, fieldPortrayal, draw, clip, secondPoint); }
        public TrailDrawInfo2D(DrawInfo2D other, double translateX, double translateY, Point2D.Double secondPoint) { super(other, translateX, translateY, secondPoint); }
        //public TrailDrawInfo2D(Rectangle2D.Double draw, Rectangle2D.Double clip, Point2D.Double secondPoint) { super(draw, clip, secondPoint); }
        public TrailDrawInfo2D(DrawInfo2D other, Point2D.Double secondPoint) { super(other, secondPoint); }
        public TrailDrawInfo2D(EdgeDrawInfo2D other) { super(other); }
        }
        
        
    // Holds the location (in the Field) and timestamp of when the object was at that location.
    static class Place
        {
        Object location;
        double timestamp;
        Place(Object location, double timestamp) { this.location = location; this.timestamp = timestamp; }
        }
        
    // is the object selected?  We'll use this auxiliary variable in the case that we're using a MovablePortrayal2D
    // and so we didn't get selected.
    boolean isSelected = false;

    boolean onlyGrowTrailWhenSelected = false;
    /** Set this to grow the trail only after the objet has been selected, and delete it when the object has been deselected.  By default this is FALSE.
        If you set this to TRUE, you can use the same TrailedPortrayal2D repeatedly for all objects in your field rather than providing separate
        ones for separate objects; furthermore only one trail will exist at a time, reducing memory costs.*/
    public void setOnlyGrowTrailWhenSelected(boolean val) { onlyGrowTrailWhenSelected = val; }
    /** @deprecated use setOnlyGrowTrailWhenSelected */
    public void setGrowTrailOnlyWhenSelected(boolean val) { onlyGrowTrailWhenSelected = val; }
    /** Returns whether or not to grow the trail only after the objet has been selected, and delete it when the object has been deselected.  By default this is FALSE.
        If you set this to TRUE, you can use the same TrailedPortrayal2D repeatedly for all objects in your field rather than providing separate
        ones for separate objects; furthermore only one trail will exist at a time, reducing memory costs.*/
    public boolean getOnlyGrowTrailWhenSelected() { return onlyGrowTrailWhenSelected; }
    /** @deprecated use getOnlyGrowTrailWhenSelected */
    public boolean getGrowTrailOnlyWhenSelected() { return onlyGrowTrailWhenSelected; }

    boolean onlyShowTrailWhenSelected = true;
    /** Set this to draw the trail only when the object has been selected (or not).  By default this is TRUE. */
    public void setOnlyShowTrailWhenSelected(boolean val) { onlyShowTrailWhenSelected = val; }
    /** Returns whether or not to draw the trail only when the object has been selected (or not).  By default this is TRUE. */
    public boolean getOnlyShowTrailWhenSelected() { return onlyShowTrailWhenSelected; }

    // A collection of all places the object has been to recently.  These will be used to determine the line segments of the trail.
    LinkedList places = new LinkedList();

    // The default color map used by the default trail SimplePortrayal2D
    SimpleColorMap defaultMap;

    // The default Trail SimplePortrayal2D.  Draws line segements by drawing lines using the default color map.
    class DefaultTrail extends SimpleEdgePortrayal2D
        {
        BasicStroke stroke = new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
            {
            info.precise = true;  // force non-awt graphics
            float linewidth = stroke.getLineWidth();
            if (linewidth * 2 != info.draw.width)
                stroke = new BasicStroke((float)(info.draw.width / 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            graphics.setStroke(stroke);
            TrailDrawInfo2D t = (TrailDrawInfo2D) info;
            toPaint = fromPaint = defaultMap.getColor(t.value);
            super.draw(object, graphics, info);
            }
        }

    /** The Child portrayal of this portrayal: a SimplePortrayal2D used solely for determining hit testing. */
    public SimplePortrayal2D child;

    /** The SimplePortrayal2D used to draw line segments in the trail.  */
    public SimplePortrayal2D trail;

    double length;
    /** Sets the length of the trail in TIME.  If an object was at a location further back than the length, the segment for that location won't be drawn any more. */
    public void setLength(double val) { if (val >= 0) length = val; }
    /** Returns the length of the trail in TIME.  If an object was at a location further back than the length, the segment for that location won't be drawn any more. */
    public double getLength() { return length; }

    // The currrent GUIState, duh
    GUIState state;
        
    // The current fieldPortrayal, duh
    FieldPortrayal2D fieldPortrayal;
        
    public static final double DEFAULT_MAXIMUM_JUMP = 0.75;
    public double maximumJump = DEFAULT_MAXIMUM_JUMP;
    /** Sets the maximum percentage of either the width or height of the field that can be 
        jumped between two successive object locations before it's considered to be a huge leap and that segment won't be drawn.  
        Huge leaps usually happen because of toroidal wrap-around.  By default the value is 0.75.  If you'd like all jumps to be drawn
        regardless of their size, set this to 1.0. */
    public void setMaximumJump(double val) { if (val >= 0.0 && val <= 1.0) maximumJump = val; }
    /** Returns the maximum percentage of either the width or height of the field that can be 
        jumped between two successive object locations before it's considered to be a huge leap and that segment won't be drawn.  
        Huge leaps usually happen because of toroidal wrap-around.  By default the value is 0.75.  If you'd like all jumps to be drawn
        regardless of their size, set this to 1.0. */
    public double getMaximumJump() { return maximumJump; }

    // Default "minimum" ("most recent in time") color is opaque gray
    public static final Color DEFAULT_MIN_COLOR = new Color(128,128,128,255);
    // Default "maximum" ("furthest back in time") color is transparent gray
    public static final Color DEFAULT_MAX_COLOR = new Color(128,128,128,0);
        
    /** Creates a TrailedPortrayal2D for a given child portrayal, field portrayal for the trail, trail portrayal, and length in time. */
    public TrailedPortrayal2D(GUIState state, SimplePortrayal2D child, FieldPortrayal2D fieldPortrayal, SimplePortrayal2D trail, double length)
        {
        this.state = state;
        this.child = child;
        this.trail = trail;
        this.length = length;
        this.fieldPortrayal = fieldPortrayal;
        defaultMap = new SimpleColorMap(0.0, 1.0, DEFAULT_MIN_COLOR, DEFAULT_MAX_COLOR);
        }

    /** Creates a TrailedPortrayal2D for a given child portrayal, field portrayal for the trail, length in time, using a default trail portrayal going from minColor to maxColor through time.  */
    public TrailedPortrayal2D(GUIState state, SimplePortrayal2D child, FieldPortrayal2D fieldPortrayal, double length, Color minColor, Color maxColor)
        {
        // all this repeating because I can't call this(...) because of complaints about DefaultTrail being built before the supertype constructor.  Java's constructor semantics are stupid.  Stupid stupid stupid.
        this.state = state;
        this.child = child;
        this.trail = new DefaultTrail();
        this.length = length;
        this.fieldPortrayal = fieldPortrayal;
        defaultMap = new SimpleColorMap(0, 1.0, minColor, maxColor);  // yeah yeah, this overwrites...
        }
        
    /** Creates a TrailedPortrayal2D for a given child portrayal, field portrayal for the trail, length in time, using a default trail portrayal with default settings.  */
    public TrailedPortrayal2D(GUIState state, SimplePortrayal2D child, FieldPortrayal2D fieldPortrayal, double length)
        {
        this(state, child, fieldPortrayal, length, DEFAULT_MIN_COLOR, DEFAULT_MAX_COLOR);
        }
        
    // returns the child if there is one.  If there isn't one and the underlying object itself is a SimplePortrayal2D, that can be used too.
    SimplePortrayal2D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal2D))
                throw new RuntimeException("Object provided to TransformedPortrayal2D is not a SimplePortrayal2D: " + object);
            return (SimplePortrayal2D) object;
            }
        }
    
    // Converts a timestamp into a value from 0..1 for color purpospes.
    double valueForTimestep(double timestamp, double currentTime)
        {
        if (length == 0) return 0;
        double val = (currentTime - timestamp) / length;
        if (val < 0) return 0;
        if (val > 1)  return 1;  // huh?
        return val;
        }
        


    final static Object NO_OBJ = new Object();  // for use in currentObjectLocation
    final static Object NO_OBJ2 = new Object(); // for use in selectedObj

    Object lastObj;  // what was the last object that was growing a trail
    Object selectedObj;  // what was the object selected in the primary fieldPortrayal?
    boolean locked = false;  // have we settled on a selected object?
        
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {               
        // I am probably added to more than one field portrayal, but should only
        // be drawing in one of them.  So let's first double check that.
        if (info.fieldPortrayal != fieldPortrayal)
            {
            if (info.selected && !locked)  // not settled on one yet
                { 
                selectedObj = object; 
                if (selectedObj == lastObj)  // DEFINITELY want this one, lock it, no one else may be the selected object
                    locked = true;
                }
            else if (selectedObj == object)  // deselected
                selectedObj = NO_OBJ2;  // impossible to be in a field
                         
            getChild(object).draw(object, graphics, info);
            return;  // don't draw me.
            }
                
        // locals are faster
        Object selectedObj = this.selectedObj;
        Object lastObj = this.lastObj;
        boolean onlyShowTrailWhenSelected = this.onlyShowTrailWhenSelected;
        boolean onlyGrowTrailWhenSelected = this.onlyGrowTrailWhenSelected;
                
                
        // unlock so next time around I have to search for an object again
        locked = false;
                
        // am I a new object that's been selected?  If so, clear the trail
        if (object == selectedObj || !onlyShowTrailWhenSelected)
            {
            if (object != lastObj && onlyGrowTrailWhenSelected)
                { 
                places.clear();
                this.lastObj = object;
                }
            }
                
        Object currentObjectLocation = NO_OBJ;
                
        // should I update my location?
        if (object == selectedObj || !onlyGrowTrailWhenSelected)
            {
            double currentTime = state.state.schedule.getTime();

            // delete old stuff from front
            ListIterator iterator = places.listIterator();
            while(iterator.hasNext())
                {
                Place p = (Place)(iterator.next());

                // First remove old stuff
                if (p.timestamp <= currentTime - length)
                    {
                    iterator.remove();
                    }
                else break;
                }
                        
            // add new stuff to back
            int size = places.size();
            currentObjectLocation = fieldPortrayal.getObjectLocation(object, info.gui);
                
            if (size == 0 && currentTime > Schedule.BEFORE_SIMULATION && currentTime < Schedule.AFTER_SIMULATION)  // first time!
                {
                places.add(new Place(currentObjectLocation, currentTime));
                }
            else if (size > 0)
                {
                Place lastPlace = (Place)(places.getLast());
                if (lastPlace.timestamp < currentTime)  // something new!
                    {
                    places.add(new Place(currentObjectLocation, currentTime));
                    }
                }
            }
                
                
        // am I being drawn?
        if (object == selectedObj || !onlyShowTrailWhenSelected) 
            {
            if (currentObjectLocation == NO_OBJ) // haven't determined this yet
                currentObjectLocation = fieldPortrayal.getObjectLocation(object, info.gui);

            double currentTime = state.state.schedule.getTime();
            ListIterator iterator = places.listIterator();
            Place lastPlace = null;
            Point2D.Double lastPosition = null;
            TrailDrawInfo2D temp = new TrailDrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(info.draw.x, info.draw.y, info.draw.width, info.draw.height), // make a copy, we'll modify it
                info.clip, null);
            while(iterator.hasNext())
                {
                Place p = (Place)(iterator.next());

                // first figure out where to draw the first point.  We'll do this by computing the position relative to a known
                // position of a known object (namely the object that was just passed in along with its DrawInfo).
                Point2D.Double position = fieldPortrayal.getRelativeObjectPosition(p.location, currentObjectLocation, info);
                                                                
                // now determine whether or not to draw.
                if (lastPosition != null)  // we had a previous position to use as our second point
                    {
                    // compute whether this was a big jump
                    boolean jump = false;
                    Object field = fieldPortrayal.getField();
                    double width = 0;
                    double height = 0;
                    if (field instanceof Grid2D) 
                        {
                        Grid2D grid = (Grid2D) field;
                        width = grid.getWidth();
                        height = grid.getHeight();
                        Int2D loc1 = (Int2D)(p.location);
                        Int2D loc2 = (Int2D)(lastPlace.location);
                        jump = Math.abs(loc1.x - loc2.x) > width * maximumJump ||
                            Math.abs(loc1.y - loc2.y) > height * maximumJump;
                        }
                    else if (field instanceof Continuous2D) 
                        {
                        Continuous2D grid = (Continuous2D) field;
                        width = grid.getWidth();
                        height = grid.getHeight();
                        Double2D loc1 = (Double2D)(p.location);
                        Double2D loc2 = (Double2D)(lastPlace.location);
                        jump = Math.abs(loc1.x - loc2.x) > width * maximumJump ||
                            Math.abs(loc1.y - loc2.y) > height * maximumJump;
                        }
                                                                                
                    // don't draw if it's a jump
                    if (!jump)
                        {
                        temp.value = valueForTimestep(p.timestamp, currentTime);
                        temp.draw.x = position.x;
                        temp.draw.y = position.y;
                        temp.secondPoint = lastPosition;
                        trail.draw(object, graphics, temp);
                        }
                    }
                                                                
                lastPlace = p;
                lastPosition = position;
                }
            }
        }
        
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        // always do a hit so I can receive a setSelected call
        return getChild(object).hitObject(object, range);
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        // always do a setSelected if the child is cool with it.
        Object object = wrapper.getObject();
        boolean returnval = getChild(object).setSelected(wrapper, selected);
        //isSelected = (selected && returnval);  // sometimes a child will return true regardless: we want to check for that.
        return returnval;
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        // do not return the inspector unless it's not my field portrayal
        Object object = wrapper.getObject();
        if (wrapper.getFieldPortrayal() != fieldPortrayal)
            return getChild(object).getInspector(wrapper, state);
        else return null;
        }
    
    public String getName(LocationWrapper wrapper)
        {
        // do not return a name unless it's not my field portrayal
        Object object = wrapper.getObject();
        if (wrapper.getFieldPortrayal() != fieldPortrayal)
            return getChild(object).getName(wrapper);
        else return null;
        }

    public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper,
        MouseEvent event, DrawInfo2D fieldPortrayalDrawInfo, int type)
        {
        return getChild(wrapper.getObject()).handleMouseEvent(guistate, manipulating, wrapper, event, fieldPortrayalDrawInfo, type);  // let someone else have it
        }
    }
    
    
    
