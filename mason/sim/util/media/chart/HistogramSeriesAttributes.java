/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.*;
import sim.util.gui.*;

// From JFreeChart (jfreechart.org)
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
    /** Border thickness */
    float thickness;
    NumberTextField thicknessField;
    /** Whether or not to include the margin as a GUI option.  */
    boolean includeMargin;
    /** The margin: the percentage of available space that a histogram bar will actually take up. 
        Turned off by default. */
    float margin;
    NumberTextField marginField;
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
    
    public void setLineOpacity(double value) { lineOpacityField.setValue(lineOpacityField.newValue(value));  }
    public double getLineOpacity() { return lineOpacityField.getValue(); }

    public void setThickness(float value) { thicknessField.setValue(thicknessField.newValue(value));  }
    public float getThickness() { return (float)(thicknessField.getValue()); }

    public void setMargin(float value) { marginField.setValue(marginField.newValue(value));  }
    public float getMargin() { return (float)(marginField.getValue()); }
    
    public void setNumBins(int value) { numBinsField.setValue(numBinsField.newValue(value));  }
    public int getNumBins() { return (int)(numBinsField.getValue()); }

    public void setFillColor(Color value) { fillColorWell.changeColor(fillColor = value); }
    public Color getFillColor() { return fillColor; }

    public void setStrokeColor(Color value) { strokeColorWell.changeColor(strokeColor = value); }
    public Color getStrokeColor() { return strokeColor; }

    /** Produces a HistogramSeriesAttributes object with the given generator, series name, series index,
        and desire to display margin options. */
    public HistogramSeriesAttributes(ChartGenerator generator, String name, int index, boolean includeMargin)
        { 
        super(generator, name, index);
        setName(name);//I need this for the remove series confirmation dialog.
        this.includeMargin = includeMargin;
        }

    public void setSeriesName(String val) 
        {
        setName(val);
        ((HistogramGenerator)generator).updateName(seriesIndex,val,false);
        }
                        
    public String getSeriesName() { return getName(); }

    public void rebuildGraphicsDefinitions()
        {
        XYBarRenderer renderer = (XYBarRenderer)getRenderer();
            
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
        if (includeMargin) renderer.setMargin(margin);
        repaint();
        }
        
    public void buildAttributes()
        {
        // The following three variables aren't defined until AFTER construction if
        // you just define them above.  So we define them below here instead.
        thickness = 2.0f;
        margin = 0.5f;
        fillOpacity = 1.0;
        lineOpacity = 1.0;

        numBinsField = new NumberTextField("", ((HistogramGenerator)generator).getNumBins(seriesIndex),true)
            {
            public double newValue(double newValue) 
                {
                newValue = (int)newValue;
                if (newValue < 1) 
                    newValue = currentValue;
                ((HistogramGenerator)generator).updateSeries(seriesIndex, (int)newValue, false);
                rebuildGraphicsDefinitions();  // forces a repaint
                return newValue;
                }
            };
        addLabelled("Bins",numBinsField);

        // fillColor = (Color)(getRenderer().getSeriesPaint(getSeriesIndex()));
        // this returns null, cause getSeriesPaint returns whatever was set through setSeriesPaint;
        // for the default colors, you need "lookupSeriesPaint()".
        //fillColor = (Color) (getRenderer().lookupSeriesPaint(getSeriesIndex()));
        // getRenderer returns an object implementing the XYItemRenderer interface.
        // either you cast that object to AbstractRenderer, and call lookupSeriesPaint()
        // or you call getItemPaint() on it directly; all getItemPaint does is call lookupSeriesPaint(),
        // but that looks bad, cause getItemPaint() seems to be meant for category data).
        //On the other hand, lookupSeriesPaint() does not show up before 1.0.6, so 
        // in the interest of backward compatibility:
        fillColor = (Color) (getRenderer().getItemPaint(getSeriesIndex(), -1));
        // second argument does not matter

        fillColor = (Color)(getRenderer().getSeriesPaint(getSeriesIndex()));
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

        strokeColor = Color.black; //(Color)(getRenderer().getSeriesOutlinePaint(getSeriesIndex()));
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

        thicknessField = new NumberTextField(thickness,false)
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
        addLabelled("Width",thicknessField);
                        
        if (includeMargin)
            {
            marginField = new NumberTextField(0.5,1.0,0.125)
                {
                public double newValue(double newValue) 
                    {
                    if (newValue < 0.0 || newValue > 1.0) 
                        newValue = currentValue;
                    margin = (float)newValue;
                    rebuildGraphicsDefinitions();
                    return newValue;
                    }
                };
            addLabelled("Space",marginField);
            }
        }
    }
