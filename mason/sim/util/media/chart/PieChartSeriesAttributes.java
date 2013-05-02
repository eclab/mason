/*
  Copyright 2013 by Sean Luke and George Mason University
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

/** 
    A SeriesAttributes used for user control of pie chart series created with PieChartGenerator.
    Like HistogramSeriesAttributes, modifying this series is costly because JFreeChart must rebuild everything.
*/


public class PieChartSeriesAttributes extends SeriesAttributes
    {
    double[] values; 
    double[] getValues() { return values; }
    void setValues(double[] vals) { values = vals; }
    
    String[] labels;
    String[] getLabels() { return labels; }
    void setLabels(String[] labs) { labels = labs; }
    
    public PieChartSeriesAttributes(ChartGenerator generator, String name, int index, double[] values, String[] labels, SeriesChangeListener stoppable)  // , boolean includeMargin)
        { 
        super(generator, name, index, stoppable);
        setValues(values);
        setLabels(labels);
        super.setSeriesName(name);  // just set the name, don't update
        }

    /** It's very expensive to call this function (O(n)) because JFreeChart has no way of changing the
        name of a pie chart dataset series, and so we must rebuild all of it from scratch. */
    public void setSeriesName(String val) 
        {
        super.setSeriesName(val); // call this first to set it
        ((PieChartGenerator)generator).update();
        }

    public void rebuildGraphicsDefinitions()
        {
        repaint();  // probably unneeded if we're not changing anything here
        }
        
    public void buildAttributes()
        {
        // No attributes for now
        }
    
    public void setPlotVisible(boolean val)
        {
        plotVisible = val;
        generator.update();
        }
        
    }
