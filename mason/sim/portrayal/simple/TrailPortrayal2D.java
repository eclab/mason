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

/**
   A special wrapper portrayal which maintains trails behind objects.
   
   <p>TrailPortrayal2D is really designed to be used with its own FieldPortrayal.  The best strategy is
	  to create an identical field portrayal to the one presently portraying your objects.  Attach this
	  new field portrayal IMMEDIATELY BEFORE attaching the original one, which causes it to draw the
	  trails first, then all the objects on top of it (which looks nice).  Both field portrayals will
	  point to the same field.
	  
   <p>In this new field portrayal, ideally you'll add a SEPARATE TrailPortrayal2D for each and every
	  object in the field for which you want trails to appear.  Wrap the TrailPortrayal2D around a
	  SimplePortrayal2D representing the object, possibly the original one being used in the 
	  original field portrayal.  This SimplePortrayal2D has only one purpose: proper hit-testing 
	  to determine whether to draw the trail when selected.  It's not used for any drawing.
	  
	<p>To draw the trail, TrailPortrayal2D maintains <i>another</i> SimplePortrayal2D to draw each
	line segment.  The default version draws half-thick line segments of different colors.  If you
	want to do something else, you can customize the SimplePortrayal2D yourself.  The SimplePortrayal2D
	will be passed a TrailPortrayal2D.TrailDrawInfo2D, which is a special version of DrawInfo2D which
	contains the previous point and also a value from 0.0 to 1.0 indicating how far back in time the
	segement is supposed to be -- you can use that to figure out what color to draw with, for example.
	  
	<p>TrailPortrayal2D by default only draws trails for objects which have been selected. 
		However for the moment the TrailPortrayal2D maintains trails for all objects even if they're not all being drawn,
		which can be a bit slow and memory consumptive if you have lots of objects.
	
	<p>To handle big jumps (particularly toroidal wrap-arounds) elegantly, TrailPortrayal2D has
		a maximumJump percentage.  For a jump to be drawn, it must be LESS than this percentage of
		the total width or height of the underlying field.
*/

public class TrailPortrayal2D extends SimplePortrayal2D
    {
	public static class TrailDrawInfo2D extends sim.portrayal.network.EdgeDrawInfo2D
		{
		/** A value from 1.0 to 0.0 indicating how far "back in time" this segment is supposed to be. */
		public double value = 0.0;
		public TrailDrawInfo2D(Rectangle2D.Double draw, Rectangle2D.Double clip, Point2D.Double secondPoint) { super(draw, clip, secondPoint); }
		public TrailDrawInfo2D(DrawInfo2D other, Point2D.Double secondPoint) { super(other, secondPoint); }
		public TrailDrawInfo2D(EdgeDrawInfo2D other) { super(other); }
		public String toString() { return "From: " + draw + " To: " + secondPoint; }
		}
	
    boolean onlyShowTrailWhenSelected = true;
    boolean isSelected = false;
	    
    public void setOnlyShowTrailWhenSelected(boolean val) { onlyShowTrailWhenSelected = val; }
    public boolean getOnlyShowTrailWhenSelected() { return onlyShowTrailWhenSelected; }

	static class Place
		{
		public Object location;
		public double timestamp;
		public Place(Object location, double timestamp) { this.location = location; this.timestamp = timestamp; }
		}
	
	LinkedList places = new LinkedList();
    public SimplePortrayal2D child;
	public SimpleColorMap defaultMap; 
	public SimplePortrayal2D trail;

	double length;
    public void setLength(double val) { if (val >= 0) length = val; }
    public double getLength() { return length; }

	GUIState state;
	double currentTime;
	FieldPortrayal2D fieldPortrayal;
	
	/** Converts a timestamp into a value from 0..1 for color purpospes.
		Only call this method if you are the trail SimplePortrayal2D and only from within your draw() method. */
	public double valueForTimestep(double timestamp)
		{
		if (length == 0) return 0;
		double val = (currentTime - timestamp) / length;
		if (val < 0) return 0;
		if (val > 1)  return 1;  // huh?
		return val;
		}
	
	class DefaultTrail extends sim.portrayal.network.SimpleEdgePortrayal2D
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


	public double maximumJump = .75;
    public void setMaximumJump(double val) { if (val >= 0.0 && val <= 1.0) maximumJump = val; }
    public double getMaximumJump() { return maximumJump; }

	static final Color DEFAULT_MIN_COLOR = new Color(128,128,128,255);
	static final Color DEFAULT_MAX_COLOR = new Color(128,128,128,0);
	
	/** Creates a TrailPortrayal2D for a given child portrayal, field portrayal for the trail, trail portrayal, and length in time. */
    public TrailPortrayal2D(GUIState state, SimplePortrayal2D child, FieldPortrayal2D fieldPortrayal, SimplePortrayal2D trail, double length)
        {
		this.state = state;
        this.child = child;
		this.trail = trail;
		this.length = length;
		this.fieldPortrayal = fieldPortrayal;
 		defaultMap = new SimpleColorMap(0.0, 1.0, DEFAULT_MIN_COLOR, DEFAULT_MAX_COLOR);
       }

	/** Creates a TrailPortrayal2D for a given child portrayal, field portrayal for the trail, length in time, using a default trail portrayal going from minColor to maxColor through time.  */
    public TrailPortrayal2D(GUIState state, SimplePortrayal2D child, FieldPortrayal2D fieldPortrayal, double length, Color minColor, Color maxColor)
        {
		// all this repeating because I can't call this(...) because of complaints about DefaultTrail being built before the supertype constructor.  Java's constructor semantics are stupid.  Stupid stupid stupid.
		this.state = state;
        this.child = child;
		this.trail = new DefaultTrail();
		this.length = length;
		this.fieldPortrayal = fieldPortrayal;
 		defaultMap = new SimpleColorMap(0, 1.0, minColor, maxColor);  // yeah yeah, this overwrites...
        }
	
	/** Creates a TrailPortrayal2D for a given child portrayal, field portrayal for the trail, length in time, using a default trail portrayal with default settings.  */
    public TrailPortrayal2D(GUIState state, SimplePortrayal2D child, FieldPortrayal2D fieldPortrayal, double length)
        {
		this(state, child, fieldPortrayal, length, DEFAULT_MIN_COLOR, DEFAULT_MAX_COLOR);
        }
        
    public SimplePortrayal2D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal2D))
                throw new RuntimeException("Object provided to TransformedPortrayal2D is not a SimplePortrayal2D: " + object);
            return (SimplePortrayal2D) object;
            }
        }
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
		currentTime = state.state.schedule.time();
		
		int size = places.size();
		
		Object currentObjectLocation = fieldPortrayal.getObjectLocation(object);
		
		// Add in new timestamp if appropriate
		if ((size == 0 && currentTime > Schedule.BEFORE_SIMULATION && currentTime < Schedule.AFTER_SIMULATION))  // first time!
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
		
		ListIterator iterator = places.listIterator();
		Place lastPlace = null;
		Point2D.Double lastPosition = null;
		TrailDrawInfo2D temp = new TrailDrawInfo2D(new Rectangle2D.Double(info.draw.x, info.draw.y, info.draw.width, info.draw.height), // make a copy, we'll modify it
													info.clip, null);
		while(iterator.hasNext())
			{
			Place p = (Place)(iterator.next());

			// First remove old stuff
			if (p.timestamp <= currentTime - length)
				{
				iterator.remove();
				lastPlace = null;
				}
			// now break out if we're not selected -- at this stage we've removed everything relevant anyway
			else if (!isSelected && onlyShowTrailWhenSelected)
				break;
			// else draw if we see fit
			else
				{
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
						temp.value = valueForTimestep(p.timestamp);
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
        return getChild(object).hitObject(object, range);
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        isSelected = selected;
		return true;
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
		return null;
        }
    
    public String getName(LocationWrapper wrapper)
        {
		return null;
        }
    }
    
    
    
