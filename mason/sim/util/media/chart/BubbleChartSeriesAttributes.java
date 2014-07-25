/*
  Copyright 2014 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import sim.util.gui.*;

// From JFreeChart
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;
import org.jfree.chart.plot.*;

public class BubbleChartSeriesAttributes extends SeriesAttributes
    {
    double[][] values; 
    public double[][] getValues() { return values; }
    public void setValues(double[][] vals) 
        { 
        if (vals != null)
            {
            vals = (double[][]) (vals.clone());
            for(int i = 0; i < vals.length; i++)
                vals[i] = (double[]) (vals[i].clone());
            }
        values = vals; 
        }

    Color color;
    ColorWell colorWell;
    double opacity;
    NumberTextField opacityField;
    double scale;
    NumberTextField scaleField;
        
    public void setOpacity(double value) { opacityField.setValue(opacityField.newValue(value));  }
    public double getOpacity() { return opacityField.getValue(); }
    
    public void setColor(Color value) { colorWell.setColor(color = value); }
    public Color getColor() { return color; }
    
    public void setScale(double scale) { this.scale = scale; }
    public double getScale() { return scale; }

    /** Produces a BubbleChartSeriesAttributes object with the given generator, series name, series index,
        and desire to display margin options. */
    public BubbleChartSeriesAttributes(ChartGenerator generator, String name, int index, double[][] values, SeriesChangeListener stoppable)
        { 
        super(generator, name, index, stoppable);
                
        setValues(values);
        super.setSeriesName(name);  // just set the name, don't update.  Bypasses standard method below.
        }

    public void setSeriesName(String val) 
        {
        super.setSeriesName(val);
        ((BubbleChartGenerator)generator).update();
        }
                        
    public void rebuildGraphicsDefinitions()
        {
        XYBubbleRenderer renderer = (XYBubbleRenderer)(((XYPlot)getPlot()).getRenderer());
        renderer.setSeriesPaint(getSeriesIndex(), reviseColor(color, opacity));
        repaint();
        }
        
    public void buildAttributes()
        {
        // The following variables aren't defined until AFTER construction if
        // you just define them above.  So we define them below here instead.
        opacity = 0.5;  // so the bubbles overlap prettily
        
        scale = 1.0;

        // NOTE:
        // Paint paint = renderer.getSeriesPaint(getSeriesIndex());        
        // In JFreeChart 1.0.6 getSeriesPaint returns null!!!
        // You need lookupSeriesPaint(), but that's not backward compatible.
        // The only thing consistent in all versions is getItemPaint 
        // (which looks like a gross miss-use, but gets the job done)
                
        color = (Color) ((((XYPlot)getPlot()).getRenderer()).getItemPaint(getSeriesIndex(), -1));
        
        colorWell = new ColorWell(color)
            {
            public Color changeColor(Color c) 
                {
                color = c;
                rebuildGraphicsDefinitions();
                return c;
                }
            };

        addLabelled("Color", colorWell);

        opacityField = new NumberTextField("Opacity ", opacity,1.0,0.125)
            {
            public double newValue(double newValue) 
                {
                if (newValue < 0.0 || newValue > 1.0) 
                    newValue = currentValue;
                opacity = (float)newValue;
                rebuildGraphicsDefinitions();
                return newValue;
                }
            };
        addLabelled("",opacityField);

        scaleField = new NumberTextField("", scale,2.0, 0.0)
            {
            public double newValue(double newValue) 
                {
                if (newValue <= 0.0) 
                    newValue = currentValue;
                scale = newValue;
                generator.update();  // so it reloads and resets the data
                rebuildGraphicsDefinitions();
                return newValue;
                }
            };
        addLabelled("Scale",scaleField);
        }
    }

