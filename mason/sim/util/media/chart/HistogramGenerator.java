/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

// From MASON (cs.gmu.edu/~eclab/projects/mason/)
import sim.util.gui.LabelledList;
import sim.util.gui.NumberTextField;

// From JFreeChart (jfreechart.org)
import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.data.general.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;
import org.jfree.data.statistics.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
   TimeSeriesChartGenerator is a ChartGenerator which displays a histogram using the JFreeChart library.
   The generator uses the HistoramDataset as its dataset, which holds histogram elements consisting of
   a name, an array of doubles (the samples), and an integer (the number of bins).  
   representing a time series displayed on the chart.  You add series to the generator with the <tt>addSeries</tt>
   method.
   
   <p>TimeSeriesChartGenerator creates attributes components in the form of TimeSeriesAttributes, which work with
   the generator to properly update the chart to reflect changes the user has made to its display.
*/

public class HistogramGenerator extends ChartGenerator
    {
    HistogramDataset dataset;
    ArrayList stoppables = new ArrayList();
    HistogramType histogramType = HistogramType.FREQUENCY;
                
    public class HistogramSeries 
        {
        double[] values; 
        int bins;
        String name;
        public HistogramSeries(String name, double[] values, int bins) 
            { this.name = name; this.values = values; this.bins = bins; }
        public void setValues(double[] v) { values = v; }
        public double[] getValues() { return values; }
        public void setBins(int b) { bins = b; }
        public int getBins() { return bins; }
        public String getName() { return name; }
        public void setName(String val) { name = val; }
        }
                
    ArrayList histogramSeries = new ArrayList();
        
    public AbstractSeriesDataset getSeriesDataset() { return dataset; }

    public void removeSeries(int index)
        {
        // stop the inspector....
        Object tmpObj = stoppables.remove(index);
        if( ( tmpObj != null ) && ( tmpObj instanceof SeriesChangeListener ) )
            ((SeriesChangeListener)tmpObj).seriesChanged(new SeriesChangeEvent(this));

        // remove from the dataset.  This is very hard to do in Histograms, stupid JFreeChart design.  Basicaly
        // we have to make a new dataset
        histogramSeries.remove(index);
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        dataset = new HistogramDataset();
        for(int i=0; i < histogramSeries.size(); i++)
            {
            HistogramSeries series = (HistogramSeries)(histogramSeries.get(i));
            dataset.addSeries(series.getName(),series.getValues(), series.getBins());
            }
        xyplot.setDataset(dataset);
        dataset.setType(histogramType);  // It looks like the histograms reset
                
        // remove the attribute
        seriesAttributes.remove(index);
                
        // shift all the seriesAttributes' indices down so they know where they are             
        Component[] c = seriesAttributes.getComponents();
        for(int i = 0; i < c.length; i++)  // do for just the components >= index in the seriesAttributes
            {
            SeriesAttributes csa = (SeriesAttributes)(c[i]);
            if (i >= index) 
                csa.setSeriesIndex(csa.getSeriesIndex() - 1);

            csa.rebuildGraphicsDefinitions();  // they've ALL just been deleted and changed, must update
            }
        revalidate();
        }
                

    public void moveSeries(int index, boolean up)
        {
        if ((index == 0 && up) || (index == histogramSeries.size()-1 && !up))
            //first one can't move up, last one can't move down
            return;
        int delta = up? -1:1;
        // move the series
        histogramSeries.add(index + delta, histogramSeries.remove(index));
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        dataset = new HistogramDataset();
        for(int i=0; i < histogramSeries.size(); i++)
            {
            HistogramSeries series = (HistogramSeries)(histogramSeries.get(i));
            dataset.addSeries(series.getName(),series.getValues(), series.getBins());
            }
        xyplot.setDataset(dataset);
        dataset.setType(histogramType);  // It looks like the histograms reset
                    
        // adjust the seriesAttributes' indices         
        Component[] c = seriesAttributes.getComponents();
        SeriesAttributes csa;
        (csa = (SeriesAttributes)c[index]).setSeriesIndex(index+delta);
        csa.rebuildGraphicsDefinitions();
        (csa = (SeriesAttributes)c[index+delta]).setSeriesIndex(index);
        csa.rebuildGraphicsDefinitions();
                
        seriesAttributes.remove(index+delta);
        //seriesAttributes.add((SeriesAttributes)(c[index+delta]), index);
        seriesAttributes.add(csa, index);

        revalidate();
            
        // adjust the stoppables, too
        stoppables.add(index+delta, stoppables.remove(index));
        }
                

    protected void buildChart()
        {
        dataset = new HistogramDataset();
        dataset.setType(HistogramType.FREQUENCY);  // when buildChart() is called, histogramType hasn't been set yet.
        chart = ChartFactory.createHistogram("Untitled Chart","Untitled X Axis","Untitled Y Axis",dataset,
            PlotOrientation.VERTICAL, false, true, false);
        chart.setAntiAlias(false);
        chartPanel = new ChartPanel(chart, true);
        chartPanel.setPreferredSize(new java.awt.Dimension(640,480));
        chartPanel.setMinimumDrawHeight(10);
        chartPanel.setMaximumDrawHeight(2000);
        chartPanel.setMinimumDrawWidth(20);
        chartPanel.setMaximumDrawWidth(2000);
        chartHolder.getViewport().setView(chartPanel);
        }


    //I need this so I can override this later when going for unit-wide bins
    //(chose the values for min, max and # bins).
    protected void addSeriesToDataSet(HistogramSeries series)
        {
        dataset.addSeries(series.getName(),series.getValues(), series.getBins());
        }
    
    public void update()
        {
        // We have to rebuild the whole stupid dataset.  Dumb design, JFreeCharters!
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        dataset = new HistogramDataset();
        for(int i=0; i < histogramSeries.size(); i++)
            {
            HistogramSeries series = (HistogramSeries)(histogramSeries.get(i));
            addSeriesToDataSet(series);
            }
        xyplot.setDataset(dataset);
        dataset.setType(histogramType);  // It looks like the histograms reset
                
        // tell all the seriesAttributes they need to rebuild, just for a single series.  Stupid.         O(n) when it should be O(1).
        Component[] c = seriesAttributes.getComponents();
        for(int i = 0; i < c.length; i++)
            {
            SeriesAttributes csa = (SeriesAttributes)(c[i]);
            csa.rebuildGraphicsDefinitions();
            }
        revalidate();
        }


    public HistogramGenerator()
        {
        // buildChart is called by super() first
                
        LabelledList list = new LabelledList("Show Histograms...");
                
        final HistogramType[] styles = new HistogramType[] 
            { HistogramType.FREQUENCY, HistogramType.RELATIVE_FREQUENCY, HistogramType.SCALE_AREA_TO_1 };
        final JComboBox style = new JComboBox(new String[] {"By Frequency", "By Relative Frequency", "With Area = 1.0"});
        style.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                histogramType = styles[style.getSelectedIndex()];
                dataset.setType(histogramType);
                }
            });
        list.add(style);
        addGlobalAttribute(list);
        }

    /** Changes the name in the histogram but not in the seriesAttributes.  Typically called FROM the seriesAttributes' setName() method. */
    void updateName(int index, String name, boolean waitUntilUpdate)
        {
        ((HistogramSeries)(histogramSeries.get(index))).setName(name);
        if (!waitUntilUpdate) update();
        }

    /** Adds a series, plus a (possibly null) SeriesChangeListener which will receive a <i>single</i>
        event if/when the series is deleted from the chart by the user.  If values is null, then the series is added in the seriesAttributes
        but not in the chart: the expectation is that you will then do an update() which will load the series properly.  This is a hack
        to get around the fact that you HAVE to provide values to a series even if you don't know what they are yet because JFreeChart dies
        on a series of length 0.
        Returns the series attributes. */
    public HistogramSeriesAttributes addSeries(double[] values, int bins, String name, final org.jfree.data.general.SeriesChangeListener stopper)
        {
        int i = dataset.getSeriesCount();
        if (values != null)  dataset.addSeries(name, values, bins);
        dataset.setType(histogramType);  // It looks like the histograms reset
        histogramSeries.add(new HistogramSeries(name,values,bins));  // histogram dataset gives us no way to hold onto these, so we must do so ourselves
        HistogramSeriesAttributes csa = new HistogramSeriesAttributes(this, name, i, false);
        seriesAttributes.add(csa);
        stoppables.add( stopper );
        revalidate();
        return csa;
        }

    public void updateSeries(int index, double[] vals, boolean waitUntilUpdate)
        {
        if (histogramSeries.size() > index)
            updateSeries(index, vals, ((HistogramSeries)(histogramSeries.get(index))).getBins(),waitUntilUpdate);
        }
                
    public void updateSeries(int index, int bins, boolean waitUntilUpdate)
        {
        if (histogramSeries.size() > index)
            updateSeries(index, ((HistogramSeries)(histogramSeries.get(index))).getValues(), bins, waitUntilUpdate);
        }
                    
    public void updateSeries(int index, double[] vals, int bins, boolean waitUntilUpdate)
        {
        if (histogramSeries.size() > index)
            {
            HistogramSeries series = (HistogramSeries)(histogramSeries.get(index));
            series.setValues(vals);
            series.setBins(bins);
            if (!waitUntilUpdate) update();
            }
        }

    public int getNumBins(int index)
        {
        return ((HistogramSeries)(histogramSeries.get(index))).getBins();
        }

    public String getName(int index)
        {
        return ((HistogramSeries)(histogramSeries.get(index))).getName();
        }

    public double[] getValues(int index)
        {
        return ((HistogramSeries)(histogramSeries.get(index))).getValues();
        }

    }
