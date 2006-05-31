/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import java.awt.*;
import java.awt.geom.*;

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
*/

public class DrawInfo2D
    {
    public Rectangle2D.Double draw;
    public Rectangle2D.Double clip;
    
    public DrawInfo2D(Rectangle2D.Double draw, Rectangle2D.Double clip)
        {
        this.draw = draw; this.clip = clip;
        }
        
    public DrawInfo2D(Rectangle draw, Rectangle clip)
        {
        this.draw = new Rectangle2D.Double(draw.x, draw.y, draw.width, draw.height);
        this.clip = new Rectangle2D.Double(clip.x, clip.y, clip.width, clip.height);
        } 

    public DrawInfo2D(RectangularShape draw, RectangularShape clip)
        {
        this.draw = new Rectangle2D.Double();
        this.draw.setRect(draw.getFrame());
        this.clip = new Rectangle2D.Double();
        this.clip.setRect(clip.getFrame());
        }
        
    public DrawInfo2D(DrawInfo2D other, double translateX, double translateY)
        {
        Rectangle2D.Double odraw = other.draw;
        draw = new Rectangle2D.Double(odraw.x+translateX,odraw.y+translateY,odraw.width,odraw.height);
        Rectangle2D.Double oclip = other.clip;
        clip = new Rectangle2D.Double(oclip.x+translateX,oclip.y+translateY,oclip.width,oclip.height);
        }
        
    public String toString() { return "DrawInfo2D[ Draw: " + draw + " Clip: " + clip + "]"; }
    }
    
    
