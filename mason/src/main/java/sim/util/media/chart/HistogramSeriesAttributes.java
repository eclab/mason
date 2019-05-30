/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import sim.util.gui.*;

// From JFreeChart
import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.data.general.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;

/** A SeriesAttributes used for user control of histogram series created with HistogramGenerator.
    Unfortunately JFreeChart doesn't have nearly
    as well-designed a histogram chart facility as its time series charts.  There is no HistogramSeries object to
    encapsulate a series, and no facilities for deleting or moving series relative to one another.  
*/

public class HistogramSeriesAttributes extends SeriesAttributes
    {
    double[] values; 
    public double[] getValues() { return values; }
    public void setValues(double[] vals) 
        { if (vals != null) vals = (double[])(vals.clone()); values = vals; }
                
    /** Border thickness */
    float thickness;
    NumberTextField thicknessField;
    /** The color of the histogram bar. */
    Color fillColor;
    ColorWell fillColorWell;
    /** The color of the histogram bar border. */
    Color strokeColor;
    ColorWell strokeColorWell;
    /** The opacity of the histogram bar.  Sadly this must be separate than the color because
        Sun doesn't have a proper color selector.  */
    double fillOpacity;
    NumberTextField fillOpacityField;
        
    /** The opacity of the histogram bar border.  Sadly this must be separate than the color because
        Sun doesn't have a proper color selector.  */
    double lineOpacity;
    NumberTextField lineOpacityField;
    NumberTextField numBinsField;
                
    public void setFillOpacity(double value) { fillOpacityField.setValue(fillOpacityField.newValue(value));  }
    public double getFillOpacity() { return fillOpacityField.getValue(); }
    
    public void setStrokeOpacity(double value) { lineOpacityField.setValue(lineOpacityField.newValue(value));  }
    public double getStrokeOpacity() { return lineOpacityField.getValue(); }

    public void setThickness(double value) { thicknessField.setValue(thicknessField.newValue(value));  }
    public double getThickness() { return (double)(thicknessField.getValue()); }
    
    // we need to store numBins here so update() gets it properly, see later
    int numBins;
    public void setNumBins(int value) { numBinsField.setValue(numBinsField.newValue(value));  numBins = (int)(numBinsField.getValue()); }
    public int getNumBins() { return numBins; }
        
    public void setFillColor(Color value) { fillColorWell.setColor(fillColor = value); }
    public Color getFillColor() { return fillColor; }

    public void setStrokeColor(Color value) { strokeColorWell.setColor(strokeColor = value); }
    public Color getStrokeColor() { return strokeColor; }

    /** Produces a HistogramSeriesAttributes object with the given generator, series name, series index,
        and desire to display margin options. */
    public HistogramSeriesAttributes(ChartGenerator generator, String name, int index, double[] values, int bins, SeriesChangeListener stoppable)  // , boolean includeMargin)
        { 
        super(generator, name, index, stoppable);
        setValues(values);
        super.setSeriesName(name);  // just set the name, don't update
        numBins = bins;
        numBinsField.setValue(bins);  // otherwise it'd call newValue(), which would in turn try to update the series, which hasn't been made yet
        numBinsField.setInitialValue(bins);  // make this the default
        }

    /** It's very expensive to call this function (O(n)) because JFreeChart has no way of changing the
        name of a histogram dataset series, and so we must rebuild all of it from scratch. */
    public void setSeriesName(String val) 
        {
        super.setSeriesName(val); // call this first to set it
        ((HistogramGenerator)generator).update();
        }

    public void rebuildGraphicsDefinitions()
        {
        XYBarRenderer renderer = (XYBarRenderer)(((XYPlot)getPlot()).getRenderer());
            
        if (thickness == 0.0)
            renderer.setDrawBarOutline(false);
        else
            {
            renderer.setSeriesOutlineStroke(getSeriesIndex(),
                new BasicStroke(thickness));
            renderer.setDrawBarOutline(true);
            }

        renderer.setSeriesPaint(getSeriesIndex(),reviseColor(fillColor, fillOpacity));
        renderer.setSeriesOutlinePaint(getSeriesIndex(),reviseColor(strokeColor, lineOpacity));
        repaint();
        }
        
    static final int DEFAULT_BINS = 8;  // to have enough space  -- it'll get changed soon
    public void buildAttributes()
        {
        // The following three variables aren't defined until AFTER construction if
        // you just define them above.  So we define them below here instead.
        thickness = 2.0f;
        fillOpacity = 1.0;
        lineOpacity = 1.0;

        numBinsField = new NumberTextField("", DEFAULT_BINS ,true)
            {
            public double newValue(double newValue) 
                {
                newValue = (int)newValue;
                if (newValue < 1) 
                    newValue = currentValue;
                                
                // we now set the number of bins directly so update() gets it when it calls getNumBins()
                numBins = (int)newValue;
                ((HistogramGenerator)generator).update();
                return newValue;
                }
            };
        addLabelled("Bins",numBinsField);

        // NOTES:
        // fillColor = (Color)((((XYPlot)getPlot()).getRenderer()).getSeriesPaint(getSeriesIndex()));
        // this returns null, cause getSeriesPaint returns whatever was set through setSeriesPaint;
        // for the default colors, you need "lookupSeriesPaint()".
        // fillColor = (Color) ((((XYPlot)getPlot()).getRenderer()).lookupSeriesPaint(getSeriesIndex()));
        // getRenderer returns an object implementing the XYItemRenderer interface.
        // either you cast that object to AbstractRenderer, and call lookupSeriesPaint()
        // or you call getItemPaint() on it directly; all getItemPaint does is call lookupSeriesPaint(),
        // but that looks bad, cause getItemPaint() seems to be meant for category data).
        // On the other hand, lookupSeriesPaint() does not show up before 1.0.6, so 
        // in the interest of backward compatibility:
        fillColor = (Color) ((((XYPlot)getPlot()).getRenderer()).getItemPaint(getSeriesIndex(), -1));
        // second argument does not matter

        fillColor = (Color)((((XYPlot)getPlot()).getRenderer()).getSeriesPaint(getSeriesIndex()));
        fillColorWell = new ColorWell(fillColor)
            {
            public Color changeColor(Color c) 
                {
                fillColor = c;
                rebuildGraphicsDefinitions();
                return c;
                }
            };

        addLabelled("Fill",fillColorWell);

        fillOpacityField = new NumberTextField("Opacity ", fillOpacity,1.0,0.125)
            {
            public double newValue(double newValue) 
                {
                if (newValue < 0.0 || newValue > 1.0) 
                    newValue = currentValue;
                fillOpacity = (float)newValue;
                rebuildGraphicsDefinitions();
                return newValue;
                }
            };
        addLabelled("",fillOpacityField);

        strokeColor = Color.black;
        strokeColorWell = new ColorWell(strokeColor)
            {
            public Color changeColor(Color c) 
                {
                strokeColor = c;
                rebuildGraphicsDefinitions();
                return c;
                }
            };

        addLabelled("Line",strokeColorWell);

        lineOpacityField = new NumberTextField("Opacity ", lineOpacity,1.0,0.125)
            {
            public double newValue(double newValue) 
                {
                if (newValue < 0.0 || newValue > 1.0) 
                    newValue = currentValue;
                lineOpacity = (float)newValue;
                rebuildGraphicsDefinitions();
                return newValue;
                }
            };
        addLabelled("",lineOpacityField);

        thicknessField = new NumberTextField("Width ", thickness,false)
            {
            public double newValue(double newValue) 
                {
                if (newValue < 0.0) 
                    newValue = currentValue;
                thickness = (float)newValue;
                rebuildGraphicsDefinitions();
                return newValue;
                }
            };
        addLabelled("",thicknessField);
        }
    }
