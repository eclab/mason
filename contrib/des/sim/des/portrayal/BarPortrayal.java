/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.portrayal;
import sim.des.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import sim.display.*;
import java.awt.*;

/** An extension of LabelledPortrayal2D which also draws various current numerical information
    if the underlying object has implemented ProvidesBarData */
         
public class BarPortrayal extends LabelledPortrayal2D
    {
    public Paint emptyPaint = Color.BLACK;
    public Paint barPaint = Color.RED;
    public static final int BAR_WIDTH = 20;
    public static final int GUTTER = 4;
         
    public BarPortrayal(SimplePortrayal2D child, double offsetx, double offsety, double scalex, double scaley, Font font, int align, String label, Paint paint, boolean onlyLabelWhenSelected)
        {
        super(child, offsetx, offsety, scalex, scaley, font, align, label, paint, onlyLabelWhenSelected);
        }
                
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        super.draw(object, graphics, info);
        
        if (object instanceof ProvidesBarData)
            { 
            graphics.setPaint(paint);
            graphics.setFont(scaledFont);
            FontMetrics fm = graphics.getFontMetrics(scaledFont);
            int height = fm.getHeight();
            int leading = fm.getLeading();
            int ascent = fm.getAscent();
            if ((ascent & 0x01) == 0x00)    // even
                ascent -= 1;
                        
            int x = (int)(info.draw.x + scalex * info.draw.width + offsetx);
            int y = (int)(info.draw.y + scaley * info.draw.height + offsety);
            int origy = y;
            int origx = x;

            // some locals
            double[] bars = ((ProvidesBarData)object).getDataBars();
            String[] values = ((ProvidesBarData)object).getDataValues();
            String[] labels = ((ProvidesBarData)object).getDataLabels();
            
            // Draw Labels
            for(int i = 0; i < labels.length; i++)
                {
                y += fm.getHeight();
                graphics.drawString(labels[i],x,y);
                }
                        
            // Draw Bars
            y = origy;
            graphics.setPaint(emptyPaint);
            for(int i = 0; i < labels.length; i++)
                {
                int barStart = x + fm.stringWidth(labels[i]) + GUTTER;
                y += height;
                if (bars[i] < 0) continue;
                graphics.drawLine(barStart, y - ascent / 2, barStart + BAR_WIDTH, y - ascent / 2);
                }

            y = origy;
            graphics.setPaint(barPaint);
            for(int i = 0; i < labels.length; i++)
                {
                int barStart = x + fm.stringWidth(labels[i]) + GUTTER;
                y += height;
                if (bars[i] < 0) continue;
                int w = (int)(BAR_WIDTH * bars[i]);
                graphics.fillRect(barStart, y - (ascent * 3) / 4, w, ascent / 2);
                }

            y = origy;
            graphics.setPaint(barPaint);
            for(int i = 0; i < labels.length; i++)
                {
                y += height;
                if (values[i] == null) continue;                
                //if (bars[i] < 0)
                    {
                    graphics.drawString(values[i],x - fm.stringWidth(values[i]) - GUTTER,y);
                    }
                /*
                  else
                  {
                  graphics.drawString(values[i],x - fm.stringWidth(values[i]) - GUTTER - BAR_WIDTH - GUTTER,y);
                  }
                */
                }
            }
        }
    }               
