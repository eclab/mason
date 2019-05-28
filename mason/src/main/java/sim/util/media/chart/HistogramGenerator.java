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
import org.jfree.chart.renderer.xy.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/*  // looks like we'll have to move to these soon
    import com.itextpdf.text.*;
    import com.itextpdf.text.pdf.*;
*/

/**
   HistogramGenerator is a ChartGenerator which displays a histogram using the JFreeChart library.
   The generator uses the HistoramDataset as its dataset, which holds histogram elements consisting of
   a name, an array of doubles (the samples), and an integer (the number of bins).  
   representing a time series displayed on the chart.  You add series to the generator with the <tt>addSeries</tt>
   method.
   
   <p>HistogramGenerator creates attributes components in the form of HistogramSeriesAttributes, which work with
   the generator to properly update the chart to reflect changes the user has made to its display.
*/

public class HistogramGenerator extends XYChartGenerator
    {
    HistogramType histogramType = HistogramType.FREQUENCY;

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
        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.FREQUENCY);  // when buildChart() is called, histogramType hasn't been set yet.

        chart = ChartFactory.createHistogram("Untitled Chart","Untitled X Axis","Untitled Y Axis",dataset,
            PlotOrientation.VERTICAL, false, true, false);
        chart.setAntiAlias(true);
        //chartPanel = new ScrollableChartPanel(chart, true);         
        chartPanel = buildChartPanel(chart);           
        setChartPanel(chartPanel);   
        //chartHolder.getViewport().setView(chartPanel);
        ((XYBarRenderer)(chart.getXYPlot().getRenderer())).setShadowVisible(false);
        ((XYBarRenderer)(chart.getXYPlot().getRenderer())).setBarPainter(new StandardXYBarPainter());

        // this must come last because the chart must exist for us to set its dataset
        setSeriesDataset(dataset);
        }
 
    protected void update()
        {
        // We have to rebuild the dataset from scratch (deleting and replacing it) because JFreeChart's
        // histogram facility doesn't have a way to remove or move elements.  Stupid stupid stupid.

        SeriesAttributes[] sa = getSeriesAttributes();
        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(histogramType);
                
        for(int i=0; i < sa.length; i++)
            {
            HistogramSeriesAttributes attributes = (HistogramSeriesAttributes)(sa[i]);
            dataset.addSeries(new UniqueString(attributes.getSeriesName()), attributes.getValues(), attributes.getNumBins());
            }
                        
        setSeriesDataset(dataset);
        }

    public HistogramGenerator()
        {
        // buildChart is called by super() first
                
        LabelledList list = new LabelledList("Show Histogram...");
        DisclosurePanel pan1 = new DisclosurePanel("Show Histogram...", list);
                
        final HistogramType[] styles = new HistogramType[] 
            { HistogramType.FREQUENCY, HistogramType.RELATIVE_FREQUENCY, HistogramType.SCALE_AREA_TO_1 };
        final JComboBox style = new JComboBox(new String[] {"By Frequency", "By Relative Frequency", "With Area = 1.0"});
        style.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                histogramType = styles[style.getSelectedIndex()];
                HistogramDataset dataset = (HistogramDataset)(getSeriesDataset());
                dataset.setType(histogramType);
                }
            });
        list.add(style);
        addGlobalAttribute(pan1);
        }


    /** Adds a series, plus a (possibly null) SeriesChangeListener which will receive a <i>single</i>
        event if/when the series is deleted from the chart by the user. Returns the series attributes. */
    public SeriesAttributes addSeries(double[] vals, int bins, String name, SeriesChangeListener stopper)
        {
        if (vals == null || vals.length == 0) vals = new double[] { 0 };  // ya gotta have at least one val
        HistogramDataset dataset = (HistogramDataset)(getSeriesDataset());
        int i = dataset.getSeriesCount();
        dataset.setType(histogramType);  // It looks like the histograms reset
        dataset.addSeries(new UniqueString(name), vals, bins);
                
        // need to have added the dataset BEFORE calling this since it'll try to change the name of the series
        HistogramSeriesAttributes csa = new HistogramSeriesAttributes(this, name, i, vals, bins, stopper);
        seriesAttributes.add(csa);
                
        revalidate();  // display the new series panel
        update();
                
        // won't update properly unless I force it here by letting all the existing scheduled events to go through.  Dumb design.  :-(
        SwingUtilities.invokeLater(new Runnable() { public void run() { update(); } });
                
        return csa;
        }


    public void updateSeries(int index, double[] vals)
        {
        if (index < 0) // this happens when we're a dead chart but the inspector doesn't know
            return;
            
        if (index >= getNumSeriesAttributes())  // this can happen when we close a window if we use the Histogram in a display
            return;

        if (vals == null || vals.length == 0) vals = new double[] { 0 };  // ya gotta have at least one val
        HistogramSeriesAttributes hsa = (HistogramSeriesAttributes)(getSeriesAttribute(index));
        hsa.setValues(vals);
        }
    
    public void setHistogramType(HistogramType type) 
        {
        histogramType = type;
        }
                
    public HistogramType getHistogramType() 
        {
        return histogramType;
        }
    }
