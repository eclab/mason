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
    Object[] elements = null;
    Collection elements2 = null;
    
    Object[] getElements() 
        {
        if (elements != null) return elements;
        else return elements2.toArray();
        }
        
    public void setElements(Object[] elts) { if (elts != null) elts = (Object[])(elts.clone()); elements = elts; elements2 = null; values = null; labels = null;}
    public void setElements(Collection elts) { if (elts != null) elts = new ArrayList(elts); elements2 = elts; elements = null; values = null; labels = null;}
    
    double[] values; 
    public double[] getValues() { return values; }
    public void setValues(double[] vals) { if (vals != null) vals = (double[])(vals.clone()); values = vals; elements = null; elements2 = null; }
    
    String[] labels;
    public String[] getLabels() { return labels; }
    public void setLabels(String[] labs) { if (labs != null) labs = (String[])(labs.clone()); labels = labs; }
    
    public PieChartSeriesAttributes(ChartGenerator generator, String name, int index, SeriesChangeListener stoppable)  // , boolean includeMargin)
        { 
        super(generator, name, index, stoppable);
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
