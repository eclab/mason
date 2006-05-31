/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial5;
import sim.field.network.*;
import sim.portrayal.network.*;
import sim.portrayal.*;
import java.awt.*;

public class BandPortrayal2D extends SimpleEdgePortrayal2D
    {
    // how our strength should look
    java.text.NumberFormat strengthFormat;
    public BandPortrayal2D()
        {
        strengthFormat = java.text.NumberFormat.getInstance();
        strengthFormat.setMinimumIntegerDigits(1);
        strengthFormat.setMaximumFractionDigits(2);
        }
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        // this better be an EdgeDrawInfo2D!  :-)
        EdgeDrawInfo2D ei = (EdgeDrawInfo2D) info;
        // likewise, this better be an Edge!
        Edge e = (Edge) object;

        // our start (x,y), ending (x,y), and midpoint (for drawing the label)
        final int startX = (int)ei.draw.x;
        final int startY = (int)ei.draw.y;
        final int endX = (int)ei.secondPoint.x;
        final int endY = (int)ei.secondPoint.y;
        final int midX = (int)((ei.draw.x+ei.secondPoint.x) / 2);
        final int midY = (int)((ei.draw.y+ei.secondPoint.y) / 2);

        // draw line.
        graphics.setColor(Color.black);
        graphics.drawLine (startX, startY, endX, endY);
        
        // draw label in blue
        graphics.setColor(Color.blue);
        graphics.setFont(labelFont);  // default font for Edge labels
        String information = strengthFormat.format(((Band)(e.info)).strength);
        int width = graphics.getFontMetrics().stringWidth(information);
        graphics.drawString( information, midX - width / 2, midY );
        }
    
    // use the default hitObject -- don't bother writing that one, it works fine
    }
