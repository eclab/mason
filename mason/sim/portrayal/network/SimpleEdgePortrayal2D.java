/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network;
import sim.portrayal.*;
import java.awt.*;
import sim.field.network.*;
import java.awt.geom.*;

/*
  A simple portrayal for edges in a network field.
  
  <p>The label can be set to scale when zoomed in or out (by default it does not scale).

*/

public class SimpleEdgePortrayal2D extends SimplePortrayal2D
    {
    public Paint fromPaint;
    public Paint toPaint;
    public Paint labelPaint;
    public Font labelFont;
    Font scaledFont;
    int labelScaling;
    public static final int NEVER_SCALE = 0;
    public static final int SCALE_WHEN_SMALLER = 1;
    public static final int ALWAYS_SCALE = 2;
    
    /** Draws a single-color, undirected black line with no label. */
    public SimpleEdgePortrayal2D()
        {
        this(Color.black, Color.black, null);
        }
    
    /** If fromPaint == toPaint, one single color line will be drawn, and if labelPaint is null, no label is drawn. */
    public SimpleEdgePortrayal2D(Paint fromPaint, Paint toPaint, Paint labelPaint)
        {
        this(fromPaint, toPaint, labelPaint, new Font("SansSerif", Font.PLAIN, 12));
        }

    /** If fromPaint == toPaint, one single color line will be drawn, and if labelPaint is null, no label is drawn. */
    public SimpleEdgePortrayal2D(Paint fromPaint, Paint toPaint, Paint labelPaint, Font labelFont)
        {
        this.fromPaint = fromPaint;
        this.toPaint = toPaint;
        this.labelPaint = labelPaint;
        this.labelFont = labelFont;
        }
    
    public int getLabelScaling() { return labelScaling; }
    public void setLabelScaling(int val) { if (val>= NEVER_SCALE && val <= ALWAYS_SCALE) labelScaling = val; }
    
    /** Returns a name appropriate for the edge.  By default, this returns 
        (edge.info == null ? "" : "" + edge.info).
        Override this to make a more customized label to display for the edge on-screen. */
    public String getLabel(Edge edge, EdgeDrawInfo2D info)
        {
        Object obj = edge.info;
        if (obj == null) return "";
        return "" + obj;
        }
    

    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        if (!(info instanceof EdgeDrawInfo2D))
            throw new RuntimeException("Expected this to be an EdgeDrawInfo2D: " + info);
        EdgeDrawInfo2D e = (EdgeDrawInfo2D) info;
        
        final int startX = (int)e.draw.x;
        final int startY = (int)e.draw.y;
        final int endX = (int)e.secondPoint.x;
        final int endY = (int)e.secondPoint.y;
        final int midX = (int)((e.draw.x+e.secondPoint.x) / 2);
        final int midY = (int)((e.draw.y+e.secondPoint.y) / 2);
        
        // draw lines
        if (fromPaint == toPaint)
            {
            graphics.setPaint (fromPaint);
            graphics.drawLine (startX, startY, endX, endY);
            }
        else
            {
            graphics.setPaint( fromPaint );
            graphics.drawLine(startX,startY,midX,midY);
            graphics.setPaint( toPaint );
            graphics.drawLine(midX,midY,endX,endY);
            }
                
        // draw label
        if (labelPaint != null)
            {
            // some locals
            Font labelFont = this.labelFont;
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

            //Object infoval = ((Edge)object).info;
            String information = getLabel((Edge)object, e);
            if( /* infoval != null && */ information.length() > 0 )
                {
                graphics.setPaint(labelPaint);
                graphics.setFont(scaledFont);
                int width = graphics.getFontMetrics().stringWidth(information);
                graphics.drawString( information, 
                                     midX - width/2, midY );
                }
            }
        }

    public boolean hitObject(Object object, DrawInfo2D range)
        {
        if (!(range instanceof EdgeDrawInfo2D))
            throw new RuntimeException("Expected this to be an EdgeDrawInfo2D: " + range);
        EdgeDrawInfo2D e = (EdgeDrawInfo2D) range;
        
        Line2D.Double line = new Line2D.Double( e.draw.x, e.draw.y, e.secondPoint.x, e.secondPoint.y );
        final double SLOP = 5;  // allow some imprecision -- click 6 away from the line
        return (line.intersects(range.clip.x - SLOP, range.clip.y - SLOP, range.clip.width + SLOP*2, range.clip.height + SLOP*2));
        //        return ( line.ptSegDist( range.clip.x, range.clip.y ) < 4 );  // allow some imprecision
        }
            
    public String getName(LocationWrapper wrapper)
        {
        // indicate it's an edge
        return "Edge: " + super.getName(wrapper);
        }
    }
