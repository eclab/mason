/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import sim.util.gui.*;

// From JFreeChart
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.general.*;
import org.jfree.data.statistics.*;
import org.jfree.data.xy.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.util.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/*  // looks like we'll have to move to these soon
    import com.itextpdf.text.*;
    import com.itextpdf.text.pdf.*;
*/

public class ScatterPlotGenerator extends XYChartGenerator
    {
    // used by ScatterPlotSeriesAttributes
    int shapeCounter = -1;
        
    public void removeSeries(int index)
        {
        super.removeSeries(index);
        update();
        }
                
    public void moveSeries(int index, boolean up)
        {
        super.moveSeries(index, up);
        update();
        }

    protected void buildChart()
        {
        DefaultXYDataset dataset = new DefaultXYDataset();
        chart = ChartFactory.createScatterPlot("Untitled Chart","Untitled X Axis","Untitled Y Axis",dataset,
            PlotOrientation.VERTICAL, false, true, false);
        chart.setAntiAlias(true);
        //chartPanel = new ScrollableChartPanel(chart, true); 
        chartPanel = buildChartPanel(chart);           
        //chartHolder.getViewport().setView(chartPanel);
        setChartPanel(chartPanel);
        chart.getXYPlot().setRenderer(new XYLineAndShapeRenderer(false, true));
//              ((StandardLegend) chart.getLegend()).setDisplaySeriesShapes(true);

        // this must come last because the chart must exist for us to set its dataset
        setSeriesDataset(dataset);
        }


    protected void update()
        {
        // we'll rebuild the plot from scratch
                
        SeriesAttributes[] sa = getSeriesAttributes();
        //XYPlot xyplot = (XYPlot)(chart.getPlot());
        DefaultXYDataset dataset = new DefaultXYDataset();
                
        for(int i=0; i < sa.length; i++)
            {
            ScatterPlotSeriesAttributes attributes = (ScatterPlotSeriesAttributes)(sa[i]);
            dataset.addSeries(new UniqueString(attributes.getSeriesName()), attributes.getValues());
            }

        setSeriesDataset(dataset);
        }

    public SeriesAttributes addSeries(double[][] values, String name, final org.jfree.data.general.SeriesChangeListener stopper)
        {
        DefaultXYDataset dataset = (DefaultXYDataset)(getSeriesDataset());
        int i = dataset.getSeriesCount();
        dataset.addSeries(new UniqueString(name), values);
                
        // need to have added the dataset BEFORE calling this since it'll try to change the name of the series
        ScatterPlotSeriesAttributes csa = new ScatterPlotSeriesAttributes(this, name, i, values, stopper);
        seriesAttributes.add(csa);
       
        revalidate();
        update();
                
        // won't update properly unless I force it here by letting all the existing scheduled events to go through.  Dumb design.  :-(
        SwingUtilities.invokeLater(new Runnable() { public void run() { update(); } });

        return csa;
        }

    public void updateSeries(int index, double[][] vals)
        {
        if (index < 0) // this happens when we're a dead chart but the inspector doesn't know
            return;

        if (index >= getNumSeriesAttributes())  // this can happen when we close a window if we use the Histogram in a display
            return;

        ScatterPlotSeriesAttributes series = (ScatterPlotSeriesAttributes)(getSeriesAttribute(index));
        series.setValues(vals);
        }
    }
