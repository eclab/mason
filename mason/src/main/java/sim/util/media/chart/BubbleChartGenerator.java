/*
  Copyright 2014 by Sean Luke and George Mason University
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

public class BubbleChartGenerator extends XYChartGenerator
    {
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
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        chart = ChartFactory.createBubbleChart("Untitled Chart","Untitled X Axis","Untitled Y Axis",dataset,
            PlotOrientation.VERTICAL, false, true, false);
        chart.setAntiAlias(true);
        chartPanel = buildChartPanel(chart);           
        setChartPanel(chartPanel);
        
        // most irritating: you can't change the scale type once you've
        // constructed the renderer.  :-(
        chart.getXYPlot().setRenderer(new XYBubbleRenderer(XYBubbleRenderer.SCALE_ON_DOMAIN_AXIS));

        // this must come last because the chart must exist for us to set its dataset
        setSeriesDataset(dataset);
        }


    protected void update()
        {
        // we'll rebuild the plot from scratch
                
        SeriesAttributes[] sa = getSeriesAttributes();
        //XYPlot xyplot = (XYPlot)(chart.getPlot());
        DefaultXYZDataset dataset = new DefaultXYZDataset();
                
        for(int i=0; i < sa.length; i++)
            {
            BubbleChartSeriesAttributes attributes = (BubbleChartSeriesAttributes)(sa[i]);
            double scale = attributes.getScale();
            
            // copy over values, and square-root the z-value.
            // A bug in JFreeChart means that z-values are not shown by
            // area but rather by (ugh) diameter.
            // Also we'll take advantage of this situation to allow
            // for user-defined scaling of the bubbles on a per-series basis.
            
            double[][] values = attributes.getValues();
            double[][] v2 = new double[values.length][values[0].length];
            for(int k = 0; k < v2.length; k++)
                for(int j = 0; j < v2[k].length; j++)
                    v2[k][j] = values[k][j];
            for(int j = 0; j < v2[2].length; j++)
                v2[2][j] = Math.sqrt(scale * v2[2][j]);
            
            dataset.addSeries(new UniqueString(attributes.getSeriesName()), v2);
            }

        setSeriesDataset(dataset);
        }

    public SeriesAttributes addSeries(double[][] values, String name, final org.jfree.data.general.SeriesChangeListener stopper)
        {
        DefaultXYZDataset dataset = (DefaultXYZDataset)(getSeriesDataset());
        int i = dataset.getSeriesCount();
        dataset.addSeries(new UniqueString(name), values);
                
        // need to have added the dataset BEFORE calling this since it'll try to change the name of the series
        BubbleChartSeriesAttributes csa = new BubbleChartSeriesAttributes(this, name, i, values, stopper);
        seriesAttributes.add(csa, name);
       
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

        BubbleChartSeriesAttributes series = (BubbleChartSeriesAttributes)(getSeriesAttribute(index));
        series.setValues(vals);
        }
    }
