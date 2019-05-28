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
    A SeriesAttributes used for user control of bar chart series created with BarChartGenerator.  Similar to PieChartSeriesAttributes.
*/


public class BarChartSeriesAttributes extends PieChartSeriesAttributes
    {
    /** Produces a PieChartSeriesAttributes object */
    public BarChartSeriesAttributes(ChartGenerator generator, String name, int index, SeriesChangeListener stoppable)  // , boolean includeMargin)
        { 
        super(generator, name, index, stoppable);
        }       
    }
