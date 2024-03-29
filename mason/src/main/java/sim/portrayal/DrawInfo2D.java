/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import java.awt.*;
import java.awt.geom.*;
import java.awt.geom.Rectangle2D.Double;

import sim.display.*;
import sim.portrayal.network.*;

/**
   The DrawInfo2D class provides two Rectangles which define a simple drawing situation.

   <p>The <b>draw</b> rectangle describes a box which should be considered
   to define the coordinates and scale for the object being drawn.  That is, the
   object should imagine that it's located at the origin [the box's <x,y> coordinate],
   and is being drawn relative to the scale [the box's <width,height> values].
   While continuous objects will probably center themselves on the origin, 
   Discrete 2D objects typically will draw themselves to fill the box (the
   box effectively defines the [0,0] to [1,1] range).  You may assume that 
   the coordinates will never be fipped or zeroed (that is, the width and 
   height will not be negative or 0).

   <p>Why is this rectangle being provided instead of just using an affine transform
   on the grahics object to scale and translate the space?  Two reasons.  First, 
   affine transforms are expensive in Java2D.  Second, if you need to draw auxillary
   information (like readable text), the text would also get transformed (scaled), which
   is not what we want.  Line thickness is also likewise transformed, which might or
   might not be desirable.  

   <p>The <b>clip</b> rectangle describes a box defining a region that must be drawn.
   This region will always intersect at least partially with the <b>draw</b> rectangle.

   <p>Why provide this clip rectangle?  Because to my knowledge there's no standard way to
   tell objects that only part of them needs to be updated in Java2D and Swing -- a failure
   of the system design.
   
   <p>The <i>precise</i> flag hints to the underlying portrayals that the drawing should be
   done precisely rather than rapidly: this is primarily for generating PDF images.  It may
   be ignored.

   <p>The <i>selected</i> flag indicates to the underlying portrayals that the object in question
   is in selected mode.  This flag is only set by FieldPortrayals.
   
   <p>The <i>location</i> object <i>may</i> store the location of the item in the outer Field.
   Fields are free to not store anything here if they see fit.  Further, this
   object may not be the actual kind of object used to store the location (for example,
   it might be a MutableDouble2D, even though the object is associated with a Double2D). 

   <p>The <i>gui</i> object stores the current GUIState. 

   <p>If this is a DrawInfo2D for a SimplePortrayal2D, then the <i>parent</i> object stores 
   the parent DrawInfo2D for the outer FieldPortrayal2D.  Otherwise, it stores null.
        
   <p>The <i>scale</i> is the current zoom level of the Display2D.  It is by default 1.0.  
   If you zoom in, the scale increases.
*/

public class DrawInfo2D
    {
    public GUIState gui;
    public FieldPortrayal2D fieldPortrayal;
    public Rectangle2D.Double draw;
    public Rectangle2D.Double clip;
    public DrawInfo2D parent;
    public boolean selected;
    public boolean precise;
    public Object location;
    public double scale;
    
    public DrawInfo2D(RectangularShape draw, RectangularShape clip, GUIState gui, FieldPortrayal2D fieldPortrayal, 
        boolean precise, double scale)
        {
        this.draw = new Rectangle2D.Double();
        this.draw.setRect(draw.getFrame());
        this.clip = new Rectangle2D.Double();
        this.clip.setRect(clip.getFrame());
        
        this.parent = parent;
        this.selected = false;
        this.precise = precise;
        this.gui = gui;
        this.fieldPortrayal = fieldPortrayal;
        this.scale = scale;
        this.location = null;
        }

    public DrawInfo2D(RectangularShape draw, RectangularShape clip, DrawInfo2D parent)
        {
        this.draw = new Rectangle2D.Double();
        this.draw.setRect(draw.getFrame());
        this.clip = new Rectangle2D.Double();
        this.clip.setRect(clip.getFrame());
        
        this.parent = parent;
        selected = false;
        precise = parent.precise;
        gui = parent.gui;
        fieldPortrayal = parent.fieldPortrayal;
        this.scale = parent.scale;
        this.location = null;
        // this.location = parent.location;                     // may not be valid
        }

    public DrawInfo2D(RectangularShape draw, DrawInfo2D parent)
        {
        this(draw, parent.clip, parent);
        }

    public DrawInfo2D(DrawInfo2D other, double translateX, double translateY)
        {
        Rectangle2D.Double odraw = other.draw;
        draw = new Rectangle2D.Double(odraw.x+translateX,odraw.y+translateY,odraw.width,odraw.height);
        Rectangle2D.Double oclip = other.clip;
        clip = new Rectangle2D.Double(oclip.x+translateX,oclip.y+translateY,oclip.width,oclip.height);
        
        precise = other.precise;
        gui = other.gui;
        fieldPortrayal = other.fieldPortrayal;
        selected = other.selected;
        parent = other.parent;
        location = other.location;
        parent = other.parent;
        scale = other.scale;
        }
        
    public DrawInfo2D(DrawInfo2D other)
        {
        this(other, 0, 0);
        }

    public DrawInfo2D(GUIState gui, FieldPortrayal2D fieldPortrayal, RectangularShape draw, RectangularShape clip)
        {
        this(gui, fieldPortrayal, draw, clip, null);
        }
    
    public DrawInfo2D(GUIState gui, FieldPortrayal2D fieldPortrayal, RectangularShape draw, RectangularShape clip, DrawInfo2D parent)
        {
        this.draw = new Rectangle2D.Double();
        this.draw.setRect(draw.getFrame());
        this.clip = new Rectangle2D.Double();
        this.clip.setRect(clip.getFrame());
        precise = false;
        this.gui = gui;
        this.parent = parent;
        this.fieldPortrayal = fieldPortrayal;
        }
    

    public String toString() { return "DrawInfo2D[ Draw: " + draw + " Clip: " + clip + " Precise: " + precise + " Location : " + location + " portrayal: " + fieldPortrayal + " Scale : " + scale + " ]"; }
    }
    
    
