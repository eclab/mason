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
  A simple portrayal for directed and undirected edges in a network field.  The portrayal can draw edges as lines or as thin triangles with their points
  pointing to the "to" node.  the edges can also be drawn with an optional label.  By default lines are drawn: you can use triangles instead by setting the
  shape to SHAPE_TRIANGLE.
  
  <p>If the portrayal draws edges as lines, you can specify both a "to" and "from" color.  Half the line will be drawn with one
  color and the other half will be drawn with the other color.
  
  <p>If the portrayal draws edges as thin triangles, the "to" color is ignored.  The baseWidth (a positive value) defines the width
  of the "base" of the triangle at its "from"-node end.  You can define the triangle to scale (that is, get wider when you zoom in), 
  to never do so, or to only scale when 'zoomed out' (SCALE_WHEN_SMALLER).  By default it always scales.
  
  <p>You can specify the both a label color and a label font.   If the label color is null, the label will not be drawn.  
  You can define the label to scale (that is, increase in font size when you zoom in), to never do so, or to only scale 
  when 'zoomed out' (SCALE_WHEN_SMALER).  By default it always scales.
*/

public class SimpleEdgePortrayal2D extends SimplePortrayal2D
    {
    public Paint fromPaint;
    public Paint toPaint;
    public Paint labelPaint;
    public Font labelFont;
    Font scaledFont;
    int labelScaling = ALWAYS_SCALE;
    int scaling = ALWAYS_SCALE;
    public static final int NEVER_SCALE = 0;
    public static final int SCALE_WHEN_SMALLER = 1;
    public static final int ALWAYS_SCALE = 2;
    public double baseWidth;
        
    public static final int SHAPE_LINE = 0;
    public static final int SHAPE_TRIANGLE = 1;
    public int shape;
    
    /** Draws a single-color, undirected black line (or triangle) with no label. */
    public SimpleEdgePortrayal2D()
        {
        this(Color.black, null);
        }
    
    /** One single color line will be drawn, and if labelPaint is null, no label is drawn. */
    public SimpleEdgePortrayal2D(Paint edgePaint, Paint labelPaint)
        {
        this(edgePaint, edgePaint, labelPaint);
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
    
    /** Returns the shape of the edge.  At present there are two shapes: a straight line (SHAPE_LINE) and a triangle (SHAPE_TRIANGLE). */
    public int getShape() { return shape; }
    /** Sets the shape of the edge.   At present there are two shapes: a straight line (SHAPE_LINE) and a triangle (SHAPE_TRIANGLE) */
    public void setShape(int shape) { this.shape = shape; }
        
    public double getBaseWidth() { return baseWidth; } 
    /** Sets the width of the base of the triangle used in drawing the directed edge -- by default, this is 0 (a simple line is drawn).
        The triangle is drawn with its base at the "from" node and its point at the "to" node. */
    public void setBaseWidth(double val) { baseWidth = val; }
        
    public int getScaling() { return labelScaling; }
    public void setScaling(int val) { if (val>= NEVER_SCALE && val <= ALWAYS_SCALE) labelScaling = val; }
        
    public int getLabelScaling() { return labelScaling; }
    public void setLabelScaling(int val) { if (val>= NEVER_SCALE && val <= ALWAYS_SCALE) labelScaling = val; }
    
    
    Line2D.Double preciseLine = new Line2D.Double();
    GeneralPath precisePoly = new GeneralPath();
    
    /** Returns a name appropriate for the edge.  By default, this returns 
        (edge.info == null ? "" : "" + edge.info).
        Override this to make a more customized label to display for the edge on-screen. */
    public String getLabel(Edge edge, EdgeDrawInfo2D info)
        {
        Object obj = edge.info;
        if (obj == null) return "";
        return "" + obj;
        }
    
    int[] xPoints = new int[3];
    int[] yPoints = new int[3];
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        if (!(info instanceof EdgeDrawInfo2D))
            throw new RuntimeException("Expected this to be an EdgeDrawInfo2D: " + info);
        EdgeDrawInfo2D e = (EdgeDrawInfo2D) info;
        
        double startXd = e.draw.x;
        double startYd = e.draw.y;
        final double endXd = e.secondPoint.x;
        final double endYd = e.secondPoint.y;
        final double midXd = ((startXd+endXd) / 2);
        final double midYd = ((startYd+endYd) / 2);     
        final int startX = (int)startXd;
        final int startY = (int)startYd;
        final int endX = (int)endXd;
        final int endY = (int)endYd;
        final int midX = (int)midXd;
        final int midY = (int)midYd;
        
        // draw lines
        if (shape == SHAPE_TRIANGLE)
            {
            graphics.setPaint (fromPaint);
            double len = Math.sqrt((startXd - endXd)*(startXd - endXd) + (startYd - endYd)*(startYd - endYd));
            double vecX = ((startXd - endXd) * baseWidth * 0.5) / len;
            double vecY = ((startYd - endYd) * baseWidth * 0.5) / len;
            double scaleWidth = info.draw.width;
            double scaleHeight = info.draw.height;
            xPoints[0] = endX;  yPoints[0] = endY;
                        
            if (scaling == SCALE_WHEN_SMALLER && info.draw.width >= 1 || scaling == NEVER_SCALE)  // no scaling
                { scaleWidth = 1; scaleHeight = 1; }
            if (info.precise)
                {
                precisePoly.reset();
                precisePoly.moveTo((float)endXd, (float)endYd);
                precisePoly.lineTo((float)(startXd + (vecY)*scaleWidth), (float)(startYd + (-vecX)*scaleHeight));
                precisePoly.lineTo((float)(startXd + (-vecY)*scaleWidth), (float)(startYd + (vecX)*scaleHeight));
                precisePoly.lineTo((float)endXd, (float)endYd);
                graphics.fill(precisePoly);
                }
            else
                {
                xPoints[1] = (int)(startXd + (vecY)*scaleWidth); yPoints[1] = (int)(startYd + (-vecX)*scaleHeight);
                xPoints[2] = (int)(startXd + (-vecY)*scaleWidth); yPoints[2] = (int)(startYd + (vecX)*scaleHeight); // rotate 180 degrees
                graphics.fillPolygon(xPoints,yPoints,3);
                graphics.drawPolygon(xPoints,yPoints,3);  // when you scale out, fillPolygon stops drawing anything at all.  Stupid.
                }
            }
        else // shape == SHAPE_LINE
            {
            if (fromPaint == toPaint)
                {
                graphics.setPaint (fromPaint);
                if (info.precise)
                    { preciseLine.setLine(startXd, startYd, endXd, endYd); graphics.draw(preciseLine); }
                else graphics.drawLine (startX, startY, endX, endY);
                }
            else
                {
                graphics.setPaint( fromPaint );
                if (info.precise)
                    { 
                    preciseLine.setLine(startXd, startYd, midXd, midYd); 
                    graphics.draw(preciseLine); 
                    graphics.setPaint(toPaint);
                    preciseLine.setLine(midXd, midYd, endXd, endYd); 
                    graphics.draw(preciseLine); 
                    }
                else
                    {
                    graphics.drawLine(startX,startY,midX,midY);
                    graphics.setPaint( toPaint );
                    graphics.drawLine(midX,midY,endX,endY);
                    }
                }
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
                graphics.drawString( information, midX - width/2, midY );
                }
            }
        }

    public boolean hitObject(Object object, DrawInfo2D range)
        {
        if (!(range instanceof EdgeDrawInfo2D))
            throw new RuntimeException("Expected this to be an EdgeDrawInfo2D: " + range);
        EdgeDrawInfo2D e = (EdgeDrawInfo2D) range;

        double startXd = e.draw.x;
        double startYd = e.draw.y;
        final double endXd = e.secondPoint.x;
        final double endYd = e.secondPoint.y;
        
        final double SLOP = 5;  // allow some imprecision -- click 6 away from the line
        if (baseWidth == 0)
            {
            Line2D.Double line = new Line2D.Double( startXd, startYd, endXd, endYd );
            return (line.intersects(range.clip.x - SLOP, range.clip.y - SLOP, range.clip.width + SLOP*2, range.clip.height + SLOP*2));
            //        return ( line.ptSegDist( range.clip.x, range.clip.y ) < 4 );  // allow some imprecision
            }
        else
            {
            double len = Math.sqrt((startXd - endXd)*(startXd - endXd) + (startYd - endYd)*(startYd - endYd));
            double vecX = ((startXd - endXd) * baseWidth * 0.5) / len;
            double vecY = ((startYd - endYd) * baseWidth * 0.5) / len;
            double scaleWidth = range.draw.width;
            double scaleHeight = range.draw.height;
            xPoints[0] = (int)endXd ;  yPoints[0] = (int)endYd; 
                        
            if (scaling == SCALE_WHEN_SMALLER && range.draw.width >= 1 || scaling == NEVER_SCALE)  // no scaling
                { scaleWidth = 1; scaleHeight = 1; }
            xPoints[1] = (int)(startXd + (vecY)*scaleWidth); yPoints[1] = (int)(startYd + (-vecX)*scaleHeight);
            xPoints[2] = (int)(startXd + (-vecY)*scaleWidth); yPoints[2] = (int)(startYd + (vecX)*scaleHeight); // rotate 180 degrees
            Polygon poly = new Polygon(xPoints,yPoints,3);
            return (poly.intersects(range.clip.x - SLOP, range.clip.y - SLOP, range.clip.width + SLOP*2, range.clip.height + SLOP*2));
            }
        }
            
    public String getName(LocationWrapper wrapper)
        {
        // indicate it's an edge
        return "Edge: " + super.getName(wrapper);
        }
    }
