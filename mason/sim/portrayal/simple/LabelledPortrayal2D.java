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
   A wrapper for other Portrayal2Ds which also draws a textual label.  When you create this
   LabelledPortrayal2D, you will pass in an underlying Portrayal2D which is supposed to draw
   the actual object; LabelledPortrayal2D will then add on an underlying label.  If the object
   will draw itself (it's its own Portrayal2D), you can signify this by passing in null for the
   underlying Portrayal2D.
   
   <p>You provide a string label at construction time.  The LabelledPortrayal2D will retrieve the label
   by calling getLabel(Object obj, DrawInfo2D info) at draw time to get the expected label.
   By default the getLabel function will return the label
   you had provided at construction; if your provided label was null, then getLabel will by default
   return the string name of the object.  You are free to override this function to provide more
   specialized label information.
   
   <p>There are certain guidelines you can specify for when the label is to be drawn.  At construction
   time you can state that the label should <i>only</i> be drawn when the object is selected.
   Additionally if you call the setLabelShowing(...) function, you can turn off or on label
   drawing entirely for this LabelledPortrayal2D.
   
   <p>You may specify a color or paint for the label (the default is blue) and an alignment (the default is align left).  

   <p>The label is drawn at:

   <pre><tt>   
   x:     (int)(info.draw.x + scalex * info.draw.width + offsetx);
   y:     (int)(info.draw.y + scaley * info.draw.height + offsety);
   </tt></pre>

   <p>... that is, scalex and scaley are values which scale when you zoom in, and
   offsetx and offsety are values which add additional fixed pixels.  The default
   is scalex = 0, scaley = 0.5, offsetx = 0, offsety = 10.  This draws the label ten
   pixels below the outer rectangular edge of the bounds rect for the
   portrayal.
   
   <p>The label can be set to scale when zoomed in or out (by default it does not scale).

   <p><b>Note:  </b> One oddity of LabelledPortrayal2D is due to the fact that the label is only
   drawn if the object is being drawn.  While most FieldPortrayals ask objects just off-screen
   to draw themselves just to be careful, if an object is significantly off-screen, it may not
   be asked to draw itself, and so the label will not be drawn -- even though part of the label 
   could be on-screen at the time!  C'est la vie.
*/

public class LabelledPortrayal2D extends SimplePortrayal2D
    {
    public static final double DEFAULT_SCALE_X = 0;
    public static final double DEFAULT_SCALE_Y = 0.5;
    public static final double DEFAULT_OFFSET_X = 0;
    public static final double DEFAULT_OFFSET_Y = 10;
    public static final int ALIGN_CENTER = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = -1;
    
    /** The pre-scaling offset from the object's origin. */
    public double scalex;
    
    /** The pre-scaling offset from the object's origin. */
    public double scaley;
    
    /** The post-scaling offset from the object's origin. */
    public double offsetx;
    
    /** The post-scaling offset from the object's origin. */
    public double offsety;
    
    /** One of ALIGN_CENTER, ALIGN_LEFT, or ALIGN_RIGHT */
    public int align;
    
    /** The font of the text. */
    public Font font;
    /** The Paint or Color of the text */
    public Paint paint;
    
    public String label;
    public SimplePortrayal2D child;
    
    /** Overrides all drawing. */
    boolean showLabel = true;
    
    public boolean onlyLabelWhenSelected;
    
    boolean isSelected = false;
        
    public boolean isLabelShowing() { return showLabel; }
    public void setLabelShowing(boolean val) { showLabel = val; }
    
    Font scaledFont;
    int labelScaling;
    public static final int NEVER_SCALE = 0;
    public static final int SCALE_WHEN_SMALLER = 1;
    public static final int ALWAYS_SCALE = 2;

    public int getLabelScaling() { return labelScaling; }
    public void setLabelScaling(int val) { if (val>= NEVER_SCALE && val <= ALWAYS_SCALE) labelScaling = val; }

    /** Draws [x=offsetx, y=offsety] pixels away from the [dx=scalex, dy=scaley] prescaled position of the Portrayal2D, 
        using the SansSerif 10pt font, blue, and left alignment.  If label is null, 
        then object.toString() is used. Labelling occurs if onlyLabelWhenSelected is true.  
        If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public LabelledPortrayal2D(SimplePortrayal2D child, double offsetx, double offsety, double scalex, double scaley, Font font, int align, String label, Paint paint, boolean onlyLabelWhenSelected)
        {
        this.offsetx = offsetx; this.offsety = offsety; this.scalex = scalex; this.scaley = scaley;
        this.font = font; this.align = align; this.label = label; this.child = child;
        this.paint = paint;  this.onlyLabelWhenSelected = onlyLabelWhenSelected;
        }

    /** Draws 10 pixels down from the [dx=0, dy=0.5] prescaled position of the Portrayal2D, 
        using the SansSerif 10pt font, blue, and left alignment.  If label is null, 
        then object.toString() is used. Labelling will always occur.  
        If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public LabelledPortrayal2D(SimplePortrayal2D child, String label)
        {
        this(child, label, Color.blue, false);
        }

    /** Draws 10 pixels down from the [dx=0, dy=scale] prescaled position of the Portrayal2D, 
        using the SansSerif 10pt font, blue, and left alignment.  If label is null, 
        then object.toString() is used. Labelling occurs if onlyLabelWhenSelected is true.    
        If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public LabelledPortrayal2D(SimplePortrayal2D child, double scale, String label, Paint paint, boolean onlyLabelWhenSelected)
        {
        this(child, DEFAULT_OFFSET_X, DEFAULT_OFFSET_Y , DEFAULT_SCALE_X, scale, new Font("SansSerif",Font.PLAIN, 10), ALIGN_LEFT, label, paint, onlyLabelWhenSelected);
        }
        
    /** Draws 10 pixels down from the [dx=0, dy=0.5] prescaled position of the Portrayal2D, 
        using the SansSerif 10pt font, and left alignment.  If label is null, 
        then object.toString() is used. Labelling occurs if onlyLabelWhenSelected is true.  
        If child is null, then the underlying model object 
        is presumed to be a Portrayal2D and will be used. */
    public LabelledPortrayal2D(SimplePortrayal2D child, String label, Paint paint, boolean onlyLabelWhenSelected)
        {
        this(child, DEFAULT_OFFSET_X, DEFAULT_OFFSET_Y, DEFAULT_SCALE_X, DEFAULT_SCALE_Y, new Font("SansSerif",Font.PLAIN, 10), ALIGN_LEFT, label, paint, onlyLabelWhenSelected);
        }

    public SimplePortrayal2D getChild(Object object)
        {
        if (child!=null) return child;
        else
            {
            if (!(object instanceof SimplePortrayal2D))
                throw new RuntimeException("Object provided to LabelledPortrayal2D is not a SimplePortrayal2D: " + object);
            return (SimplePortrayal2D) object;
            }
        }
        
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        getChild(object).draw(object,graphics,info);

        if (showLabel && (isSelected || !onlyLabelWhenSelected))
            {
            // some locals
            Font labelFont = this.font;
            Font scaledFont = this.scaledFont;

            // build font
            float size = (labelScaling == ALWAYS_SCALE ||
                (labelScaling == SCALE_WHEN_SMALLER && info.draw.width < 1)) ?
                (float)(info.draw.width * labelFont.getSize2D()) :
                labelFont.getSize2D();
            if (scaledFont == null || 
                scaledFont.getSize2D() != size || 
                scaledFont.getFamily() != labelFont.getFamily() ||
                scaledFont.getStyle() != labelFont.getStyle())
                scaledFont = this.scaledFont = labelFont.deriveFont(size);

            String s = getLabel(object,info);
            int x = (int)(info.draw.x + scalex * info.draw.width + offsetx);
            int y = (int)(info.draw.y + scaley * info.draw.height + offsety);
            graphics.setPaint(paint);
            graphics.setFont(scaledFont);
    
            if (align == ALIGN_CENTER)
                {
                x -= graphics.getFontMetrics().stringWidth(s)/2;
                }
            else if (align == ALIGN_RIGHT)
                {
                x -= graphics.getFontMetrics().stringWidth(s);
                }
            graphics.drawString(s,x,y);
            }
        }
        
    public boolean hitObject(Object object, DrawInfo2D range)
        {
        return getChild(object).hitObject(object,range);
        }

    public boolean setSelected(LocationWrapper wrapper, boolean selected)
        {
        isSelected = selected;
        return getChild(wrapper.getObject()).setSelected(wrapper, selected);
        }

    public Inspector getInspector(LocationWrapper wrapper, GUIState state)
        {
        return getChild(wrapper.getObject()).getInspector(wrapper,state);
        }
    
    /** Returns a name appropriate for the object.  By default, this returns
        the label, or if label is null, then returns ("" + object).
        Override this to make a more customized label to display for the object
        on-screen. */
    public String getLabel(Object object, DrawInfo2D info)
        {
        if (label==null) return ("" + object);
        else return label;
        }
    
    public String getName(LocationWrapper wrapper)
        {
        return getChild(wrapper.getObject()).getName(wrapper);
        }
    }
    
    
    
