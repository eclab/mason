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
import sim.util.*;

/*
  A simple portrayal for directed and undirected edges in a network field.  The portrayal can draw edges as lines or as thin triangles with their points
  pointing to the "to" node.  the edges can also be drawn with an optional label.  By default lines are drawn: you can use triangles instead by setting the
  shape to SHAPE_TRIANGLE.
  
  <p>If the portrayal draws edges as lines, you can specify both a "to" and "from" color.  Half the line will be drawn with one
  color and the other half will be drawn with the other color.
  
  <p>If the portrayal draws edges as thin triangles, the "to" color is ignored.  The baseWidth (a positive value) defines the width
  of the "base" of the triangle at its "from"-node end.  You must set this value in order to see a triangle: try 1.0.
  
  <p>The baseWidth also affects drawing lines: if it's 0.0 a single-pixel thin line is drawn, else a line is drawn of the given thickness.
  
  <p>You can also define the triangle or line thickness to scale (that is, get wider when you zoom in), 
  to never do so, or to only scale when 'zoomed out' (SCALE_WHEN_SMALLER).  By default it always scales.
  
  <p>You can specify the both a label color and a label font.   If the label color is null, the label will not be drawn.  
  You can define the label to scale (that is, increase in font size when you zoom in), to never do so, or to only scale 
  when 'zoomed out' (SCALE_WHEN_SMALER).  By default it always scales.
*/

public class SimpleEdgePortrayal2D extends SimplePortrayal2D
    {
    public Paint fromPaint = Color.black;
    public Paint toPaint = Color.black;
    public Paint labelPaint = null;  // indicates no label
    public Font labelFont;
    Font scaledFont;
    int labelScaling = ALWAYS_SCALE;
    int scaling = ALWAYS_SCALE;
    public static final int NEVER_SCALE = 0;
    public static final int SCALE_WHEN_SMALLER = 1;
    public static final int ALWAYS_SCALE = 2;
    public double baseWidth = 1.0;
        
    public static final int SHAPE_THIN_LINE = 0;
    /** @deprecated Use SHAPE_LINE_ROUND_ENDS */
    public static final int SHAPE_LINE = 0;
    public static final int SHAPE_LINE_ROUND_ENDS = 1;
    public static final int SHAPE_LINE_SQUARE_ENDS = 2;
    public static final int SHAPE_LINE_BUTT_ENDS = 3;
    public static final int SHAPE_TRIANGLE = 4;
    public int shape = SHAPE_THIN_LINE;
        
    boolean adjustsThickness;
    
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
    
    public boolean getAdjustsThickness() { return adjustsThickness; }
    public void setAdjustsThickness(boolean val) { adjustsThickness = val; }
        
    /** Returns the shape of the edge.  At present there are two shapes: a straight line (SHAPE_LINE) and a triangle (SHAPE_TRIANGLE). */
    public int getShape() { return shape; }
    /** Sets the shape of the edge.   At present there are two shapes: a straight line (SHAPE_LINE) and a triangle (SHAPE_TRIANGLE) */
    public void setShape(int shape) { this.shape = shape; }
        
    public double getBaseWidth() { return baseWidth; } 
    /** Sets the width of the base of the triangle used in drawing the directed edge -- by default, this is 0 (a simple line is drawn).
        The triangle is drawn with its base at the "from" node and its point at the "to" node.*/
    public void setBaseWidth(double val) { baseWidth = val; }
        
    public int getScaling() { return scaling; }
    public void setScaling(int val) { if (val>= NEVER_SCALE && val <= ALWAYS_SCALE) scaling = val; }
        
    public int getLabelScaling() { return labelScaling; }
    public void setLabelScaling(int val) { if (val>= NEVER_SCALE && val <= ALWAYS_SCALE) labelScaling = val; }
    
    
    Line2D.Double preciseLine = new Line2D.Double();
    GeneralPath precisePoly = new GeneralPath();
    
    /** Returns a weight appropriate to scale the edge.  This weight must be >= 0.
        By default, this returns 1.0 of adjustsThickness() is false or if edge.info
        cannot be converted into a weight, else converts edge.info and returns the absolute value. */
    protected double getPositiveWeight(Object edge, EdgeDrawInfo2D info)
        {
        if (getAdjustsThickness())              
            {
            Object obj = edge;              // it's possible for the SimpleEdgePortrayal to be used for non-edges, as in TrailedPortrayal
            if (edge instanceof Edge) obj = ((Edge)edge).info;
            if (obj instanceof Number)
                return Math.abs(((Number)obj).doubleValue());
            else if (obj instanceof Valuable)
                return Math.abs(((Valuable)obj).doubleValue());
            }
        return 1.0;
        }

    /** Returns a name appropriate for the edge.  By default, this returns 
        (edge.info == null ? "" : "" + edge.info).
        Override this to make a more customized label to display for the edge on-screen. */
    public String getLabel(Edge edge, EdgeDrawInfo2D info)
        {
        Object obj = edge.info;
        if (obj == null) return "";
        return "" + obj;
        }
    
    BasicStroke getBasicStroke(float thickness)
        {
        return new BasicStroke(thickness, 
                (shape == SHAPE_LINE_ROUND_ENDS ? 
                BasicStroke.CAP_ROUND :
                    (shape == SHAPE_LINE_SQUARE_ENDS ? 
                    BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT)),
            BasicStroke.JOIN_MITER);
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
            double weight = getPositiveWeight(object, e);
            double width = getBaseWidth();
            graphics.setPaint (fromPaint);
            double len = Math.sqrt((startXd - endXd)*(startXd - endXd) + (startYd - endYd)*(startYd - endYd));
            double vecX = ((startXd - endXd) * width * 0.5 * weight) / len;
            double vecY = ((startYd - endYd) * width * 0.5 * weight) / len;
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
        else if (shape == SHAPE_THIN_LINE)
            {
            if (fromPaint == toPaint)
                {
                graphics.setPaint (fromPaint);
                graphics.drawLine(startX, startY, endX, endY);
                }
            else
                {
                graphics.setPaint( fromPaint );
                graphics.drawLine(startX,startY,midX,midY);
                graphics.setPaint( toPaint );
                graphics.drawLine(midX,midY,endX,endY);
                }
            }
        else // shape == SHAPE_LINE etc.
            {
            if (fromPaint == toPaint)
                {
                graphics.setPaint (fromPaint);
                double width = getBaseWidth();
                //if (info.precise || width != 0.0)
                //    { 
                double scale = info.draw.width;
                if (scaling == SCALE_WHEN_SMALLER && info.draw.width >= 1 || scaling == NEVER_SCALE)  // no scaling
                    scale = 1;

                Stroke oldstroke = graphics.getStroke();
                double weight = getPositiveWeight(object, e);
                graphics.setStroke(getBasicStroke((float)(width * weight * scale)));  // duh, can't reset a stroke, have to make it new each time :-(
                preciseLine.setLine(startXd, startYd, endXd, endYd);
                graphics.draw(preciseLine);
                graphics.setStroke(oldstroke);
                //    }
                //else graphics.drawLine(startX, startY, endX, endY);
                }
            else
                {
                graphics.setPaint( fromPaint );
                double width = getBaseWidth();
                //if (info.precise || width != 0.0)
                //    { 
                double scale = info.draw.width;
                if (scaling == SCALE_WHEN_SMALLER && info.draw.width >= 1 || scaling == NEVER_SCALE)  // no scaling
                    scale = 1;

                Stroke oldstroke = graphics.getStroke();
                double weight = getPositiveWeight(object, e);
                graphics.setStroke(getBasicStroke((float)(width * weight * scale)));  // duh, can't reset a stroke, have to make it new each time :-(
                preciseLine.setLine(startXd, startYd, midXd, midYd); 
                graphics.draw(preciseLine); 
                graphics.setPaint(toPaint);
                preciseLine.setLine(midXd, midYd, endXd, endYd); 
                graphics.draw(preciseLine); 
                graphics.setStroke(oldstroke);
                //    }
                //else
                //    {
                //    graphics.drawLine(startX,startY,midX,midY);
                //    graphics.setPaint( toPaint );
                //    graphics.drawLine(midX,midY,endX,endY);
                //    }
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
                !scaledFont.getFamily().equals(labelFont.getFamily()) ||
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
        
        double weight = getPositiveWeight(object, e);
        double width = getBaseWidth();

        final double SLOP = 5;  // allow some imprecision -- click 6 away from the line
        if (shape == SHAPE_LINE)
            {
            double scale = range.draw.width;
            if (scaling == SCALE_WHEN_SMALLER && range.draw.width >= 1 || scaling == NEVER_SCALE)  // no scaling
                scale = 1;

            Line2D.Double line = new Line2D.Double( startXd, startYd, endXd, endYd );
            if (width == 0)
                return (line.intersects(range.clip.x - SLOP, range.clip.y - SLOP, range.clip.width + SLOP*2, range.clip.height + SLOP*2));
            else
                return new BasicStroke((float)(width * weight * scale), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER).createStrokedShape(line).intersects(
                    range.clip.x - SLOP, range.clip.y - SLOP, range.clip.width + SLOP*2, range.clip.height + SLOP*2);
            }
        else
            {
            double len = Math.sqrt((startXd - endXd)*(startXd - endXd) + (startYd - endYd)*(startYd - endYd));
            double vecX = ((startXd - endXd) * width * 0.5 * weight) / len;
            double vecY = ((startYd - endYd) * width * 0.5 * weight) / len;
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
