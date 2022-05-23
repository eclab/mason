/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.portrayal;

import sim.field.network.*;
import sim.util.*;
import sim.engine.*;
import java.util.*;
import sim.des.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import sim.field.network.*;
import sim.portrayal.network.*;
import sim.portrayal.*;
import java.awt.geom.*;

/**
   A subclass of SimpleEdgePortrayal2D which scales the edges appropriately to the 
   receint offers accepted between nodes.
**/

public class DelayedEdgePortrayal extends SimpleEdgePortrayal2D
    {
    private static final long serialVersionUID = 1;
    
    public DelayedEdgePortrayal()
        {
        super(Color.BLUE, Color.RED, Color.BLACK, new Font("SansSerif", Font.PLAIN, 10));
        setShape(triangle ? SimpleEdgePortrayal2D.SHAPE_TRIANGLE : SimpleEdgePortrayal2D.SHAPE_LINE_BUTT_ENDS);
        setAdjustsThickness(true);
        setScaling(SimpleEdgePortrayal2D.ALWAYS_SCALE);
        setLabelScaling(SimpleEdgePortrayal2D.SCALE_WHEN_SMALLER);
        }
    
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
			if (!(info instanceof EdgeDrawInfo2D))
				throw new RuntimeException("Expected this to be an EdgeDrawInfo2D: " + info);
			EdgeDrawInfo2D e = (EdgeDrawInfo2D) info;
		
		    double width = getBaseWidth();

                double scale = info.scale;
                if (getScaling() == SCALE_WHEN_SMALLER && info.draw.width >= 1 || getScaling() == NEVER_SCALE)  // no scaling
                    scale = 1;

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
        
        	final double TRIANGLE_WIDTH = 10.0;

            	double alpha = Math.atan2(startYd - endYd, startXd - endXd);
            	
            	// This is the length of the line from center to center of two objects
        		double len = Math.sqrt((startXd - endXd) * (startXd - endXd) + (startYd - endYd) * (startYd - endYd));
        		
        		// we're hard-setting the scale
        		double objectScale = DESPortrayalParameters.getPortrayalScale();

        		// This is the total offset on both sides
        		double offsetStart = 
        			1.5 / 2 * info.draw.width * objectScale; 	// this is the distance from the center of the object to the outer circular border

        		double offsetEnd = 
        			1.5 / 2 * info.draw.width * objectScale 					// this is the distance from the center of the object to the outer circular border
        		 		+ TRIANGLE_WIDTH * width * scale;						// this is the additional offset to include the arrowhead
        		
        		double sXd = startXd;
        		double sYd = startYd;
        		double eXd = endXd;
        		double eYd = endYd;

        		if (len - offsetStart - offsetEnd > info.draw.width * objectScale * 0.5)		// If the length of the line, minus the offset, is less than one half a full object's distance
        			{
					 sXd -= offsetStart * Math.cos(alpha);
					 sYd -= offsetStart * Math.sin(alpha);
					 eXd += offsetEnd * Math.cos(alpha);
					 eYd += offsetEnd * Math.sin(alpha);
					 }
				else if (len > 0)
					{
					 double beta = len / (info.draw.width * objectScale * 0.5 + offsetStart + offsetEnd);		// fraction of original offset we should shrink the offset to
					 sXd -= offsetStart * Math.cos(alpha) * beta;
					 sYd -= offsetStart * Math.sin(alpha) * beta;
					 eXd += offsetEnd * Math.cos(alpha) * beta;
					 eYd += offsetEnd * Math.sin(alpha) * beta;
					}
        		
        		graphics.setPaint(fromPaint);

				if (len > TRIANGLE_WIDTH * width * scale)	// draw line
					{
					Stroke oldstroke = graphics.getStroke();
					double weight = getPositiveWeight(object, e);
					graphics.setStroke(getBasicStroke((float)(width * weight * scale)));  // duh, can't reset a stroke, have to make it new each time :-(
					Line2D.Double preciseLine = new Line2D.Double();
					preciseLine.setLine(sXd, sYd, eXd, eYd);
					graphics.draw(preciseLine);
					graphics.setStroke(oldstroke);
                	}

				Path2D.Double head = new Path2D.Double();
				head.moveTo((1.0 - TRIANGLE_WIDTH) * width * scale, 0 * width * scale);
				head.lineTo(1.0 * width * scale, -(TRIANGLE_WIDTH / 2.0) * width * scale);
				head.lineTo(1.0 * width * scale, (TRIANGLE_WIDTH / 2.0) * width * scale);
				head.closePath();
				AffineTransform trans = new AffineTransform(graphics.getTransform());
				trans.translate(eXd, eYd);
				trans.rotate(alpha);
				AffineTransform old = graphics.getTransform();
				graphics.setTransform(trans);
			    graphics.fill(head);
			    graphics.setTransform(old);
			    
			    // Draw the little circles
			    graphics.setColor(Color.BLACK);
			    if (object != null &&
			    	object instanceof Edge)
			    	{
			    	Object to = ((Edge)object).to();
			    	if (to != null &&
			    		to instanceof SimpleDelay)
			    		{
			    		SimpleDelay delay = (SimpleDelay)to;
			    		DelayNode[] delayed = delay.getDelayedResources();
						double delayTime = delay.getDelayTime();
						double time = info.gui.state.schedule.getTime();
						for(int i = 0; i < delayed.length; i++)
							{
							double pos = 1.0 - (delayed[i].getTimestamp() - time) / delayTime;
							if (pos >= 0 && pos <= 1)
								{
								double centerX = sXd + pos * (eXd - sXd);
								double centerY = sYd + pos * (eYd - sYd);
								graphics.fill(new Ellipse2D.Double(centerX - TRIANGLE_WIDTH/2, centerY - TRIANGLE_WIDTH/2, TRIANGLE_WIDTH, TRIANGLE_WIDTH));
								}
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
            float size = (getLabelScaling() == ALWAYS_SCALE ||
                (getLabelScaling() == SCALE_WHEN_SMALLER && info.scale < 1)) ?
                (float)(info.scale * labelFont.getSize2D()) :
                labelFont.getSize2D();
            if (scaledFont == null || 
                scaledFont.getSize2D() != size || 
                !scaledFont.getFamily().equals(labelFont.getFamily()) ||
                scaledFont.getStyle() != labelFont.getStyle())
                scaledFont = this.scaledFont = labelFont.deriveFont(size);
            
            String information = getLabel((Edge)object, e);
            if( information.length() > 0 )
                {
                graphics.setPaint(labelPaint);
                graphics.setFont(scaledFont);
                int labelWidth = graphics.getFontMetrics().stringWidth(information);
                graphics.drawString( information, midX - labelWidth/2, midY );
                }
            }

		}

    protected double getPositiveWeight(Object edge, EdgeDrawInfo2D info)
        {
        ResourceEdge e = (ResourceEdge)edge;
        Provider provider = (Provider)(e.getFrom());
        Receiver receiver = (Receiver)(e.getTo());
        if (provider.getState().schedule.getTime() == provider.getLastAcceptedOfferTime())
            {
            ArrayList<Resource> offers = provider.getLastAcceptedOffers();
            ArrayList<Receiver> receivers = provider.getLastAcceptedOfferReceivers();
            int loc = receivers.indexOf(receiver);
            if (loc >= 0)
                {
                return (offers.get(loc).getAmount()) * scale;
                }
            else 
                {
                return 0.0;
                }
            }
        else
            {
            return 0.0;
            }
        }
    }
        
        
