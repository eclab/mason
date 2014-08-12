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
import javax.swing.event.*;

import sim.util.gui.*;

// From JFreeChart
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.data.statistics.*;
import org.jfree.data.general.*;
import org.jfree.chart.title.*;
import org.jfree.data.xy.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.*;
import org.jfree.data.category.*;
import org.jfree.chart.labels.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/*  // looks like we'll have to move to these soon
    import com.itextpdf.text.*;
    import com.itextpdf.text.pdf.*;
*/

/**
   BoxPlotGenerator is a ChartGenerator which displays a BoxPlot using the JFreeChart library.
   The generator uses the HistoramDataset as its dataset, which holds BoxPlot elements consisting of
   a name, an array of doubles (the samples), and an integer (the number of bins).  
   representing a time series displayed on the chart.  You add series to the generator with the <tt>addSeries</tt>
   method.
   
   <p>BoxPlotChartGenerator creates attributes components in the form of BoxPlotAttributes, which work with
   the generator to properly update the chart to reflect changes the user has made to its display.
*/

public class BoxPlotGenerator extends ChartGenerator
    {
    /** The global attributes range axis field. */
    PropertyField yLabel;
    
    /** The global attributes domain axis field. */
    PropertyField xLabel;
    
    /** The global attributes logarithmic range axis check box. */
    JCheckBox yLog;
    
    JCheckBox mean;
    JCheckBox median;
    
    NumberTextField maximumWidthField;
    
    public void setMaximumWidth(double value) { maximumWidthField.setValue(maximumWidthField.newValue(value));  }
    public double getMaximumWidth() { return maximumWidthField.getValue(); }
    
    public void setYAxisLogScaled(boolean isLogScaled){yLog.setSelected(isLogScaled);}
    public boolean isYAxisLogScaled(){return yLog.isSelected();}

    public void setMeanShown(boolean val){mean.setSelected(val);}
    public boolean isMeanShown(){return mean.isSelected();}
    public void setMedianShown(boolean val){median.setSelected(val);}
    public boolean isMedianShown(){return median.isSelected();}
        
    /** Returns the name of the Y Axis label. */
    public String getYAxisLabel()
        {
        return ((CategoryPlot)(chart.getPlot())).getRangeAxis().getLabel();
        }
                
    /** Returns the name of the X Axis label. */
    public String getXAxisLabel()
        {
        return ((CategoryPlot)(chart.getPlot())).getDomainAxis().getLabel();
        }
                

    public Dataset getSeriesDataset() { return ((CategoryPlot)(chart.getPlot())).getDataset(); }

    public void setSeriesDataset(Dataset obj)
        {
        ((CategoryPlot)(chart.getPlot())).setDataset((DefaultBoxAndWhiskerCategoryDataset)obj);
        if (invalidChartTitle != null)
            setInvalidChartTitle(null);
        }
        
    public int getSeriesCount()
        {
        DefaultBoxAndWhiskerCategoryDataset dataset = (DefaultBoxAndWhiskerCategoryDataset)(getSeriesDataset());
        return dataset.getRowCount();
        }
        
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
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

        // we build the chart manually rather than using ChartFactory
        // because we need to customize the getDataRange method below

        CategoryAxis categoryAxis = new CategoryAxis("");
        NumberAxis valueAxis = new NumberAxis("Untitled Y Axis");
        valueAxis.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer)
            {
            // Customizing this method in order to provide a bit of
            // vertical buffer.  Otherwise the bar chart box gets drawn
            // slightly off-chart, which looks really bad.
                
            public Range getDataRange(ValueAxis axis)
                {
                Range range = super.getDataRange(axis);
                if (range == null) return null;
                final double EXTRA_PERCENTAGE = 0.02;
                return Range.expand(range, EXTRA_PERCENTAGE, EXTRA_PERCENTAGE);
                }
            };
                
        chart = new JFreeChart("Untitled Chart", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        ChartFactory.getChartTheme().apply(chart);

        chart.setAntiAlias(true);
        chartPanel = buildChartPanel(chart);           
        setChartPanel(chartPanel);   

        // this must come last because the chart must exist for us to set its dataset
        setSeriesDataset(dataset);
        }
 
    ArrayList buildList(double[] vals)
        {
        ArrayList list = new ArrayList();
        for(int i = 0; i < vals.length; i++)
            list.add(new Double(vals[i]));
        return list;
        }
 
    protected void update()
        {
        // We have to rebuild the dataset from scratch (deleting and replacing it) because JFreeChart's
        // BoxPlot facility doesn't have a way to remove or move elements.  Stupid stupid stupid.

        SeriesAttributes[] sa = getSeriesAttributes();
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
                 
        for(int i=0; i < sa.length; i++)
            {
            BoxPlotSeriesAttributes attributes = (BoxPlotSeriesAttributes)(sa[i]);
            double[][] values = attributes.getValues();
            String[] labels = attributes.getLabels();
            //UniqueString series = new UniqueString(attributes.getSeriesName());
            String series = attributes.getSeriesName();
            for(int j = 0; j < values.length; j++)
                {
                dataset.add(buildList(values[j]), series, labels[j]);
                }
            }

        ((BoxAndWhiskerRenderer)(((CategoryPlot)(chart.getPlot())).getRenderer())).setMaximumBarWidth(getMaximumWidth());
                        
        setSeriesDataset(dataset);
        }



    public SeriesAttributes addSeries(double[] vals, String name, SeriesChangeListener stopper)
        {
        double[][] vvals = new double[1][];
        vvals[0] = vals;
        return addSeries(vvals, name, stopper);
        }

    /** Adds a series, plus a (possibly null) SeriesChangeListener which will receive a <i>single</i>
        event if/when the series is deleted from the chart by the user. Returns the series attributes. */

    SeriesAttributes addSeries(double[][] vals, String name, SeriesChangeListener stopper)
        {
        if (vals == null || vals.length == 0) vals = new double[0][0];
        int i = getSeriesCount();
                
        // need to have added the dataset BEFORE calling this since it'll try to change the name of the series
        BoxPlotSeriesAttributes csa = new BoxPlotSeriesAttributes(this, name, i, vals, stopper);
        seriesAttributes.add(csa);
                
        revalidate();  // display the new series panel
        update();
                
        // won't update properly unless I force it here by letting all the existing scheduled events to go through.  Dumb design.  :-(
        SwingUtilities.invokeLater(new Runnable() { public void run() { update(); } });
                
        return csa;
        }


    public SeriesAttributes addSeries(double[][] vals, String[] labels, String name, SeriesChangeListener stopper)
        {
        if (vals == null || vals.length == 0) vals = new double[0][0];
        int i = getSeriesCount();
                
        // need to have added the dataset BEFORE calling this since it'll try to change the name of the series
        BoxPlotSeriesAttributes csa = new BoxPlotSeriesAttributes(this, name, i, vals, labels, stopper);
        seriesAttributes.add(csa);
                
        revalidate();  // display the new series panel
        update();
                
        // won't update properly unless I force it here by letting all the existing scheduled events to go through.  Dumb design.  :-(
        SwingUtilities.invokeLater(new Runnable() { public void run() { update(); } });
                
        return csa;
        }
        



    /** Sets the name of the Y Axis label. */
    public void setYAxisLabel(String val)
        {
        CategoryPlot xyplot = (CategoryPlot)(chart.getPlot());
        xyplot.getRangeAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getRangeAxis()));
        yLabel.setValue(val);
        }

    /** Sets the name of the X Axis label. */
    public void setXAxisLabel(String val)
        {
        CategoryPlot xyplot = (CategoryPlot)(chart.getPlot());
        xyplot.getDomainAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getDomainAxis()));
        xLabel.setValue(val);
        }

    public void updateSeries(int index, double[] vals)
        {
        double[][] vvals = new double[1][];
        vvals[0] = vals;
        updateSeries(index, vvals);
        }

    public void updateSeries(int index, double[][] vals)
        {
        if (index < 0) // this happens when we're a dead chart but the inspector doesn't know
            return;
            
        if (index >= getNumSeriesAttributes())  // this can happen when we close a window if we use the BoxPlot in a display
            return;

        if (vals == null || vals.length == 0) vals = new double[0][0];
        BoxPlotSeriesAttributes hsa = (BoxPlotSeriesAttributes)(getSeriesAttribute(index));
        hsa.setValues(vals);
        hsa.setLabels(null);
        }

    public void updateSeries(int index, double[][] vals, String[] labels)
        {
        if (index < 0) // this happens when we're a dead chart but the inspector doesn't know
            return;
            
        if (index >= getNumSeriesAttributes())  // this can happen when we close a window if we use the BoxPlot in a display
            return;

        if (vals == null || vals.length == 0) vals = new double[0][0];
        if (labels == null || labels.length == 0) labels = new String[0];
        if (vals.length != labels.length)  // uh oh
            return;
                
        BoxPlotSeriesAttributes hsa = (BoxPlotSeriesAttributes)(getSeriesAttribute(index));
        hsa.setValues(vals);
        hsa.setLabels(labels);
        }
        
    protected void buildGlobalAttributes(LabelledList list)
        {
        // create the chart
        ((CategoryPlot)(chart.getPlot())).setRangeGridlinesVisible(false);
        ((CategoryPlot)(chart.getPlot())).setRangeGridlinePaint(new Color(200,200,200));

        xLabel = new PropertyField()
            {
            public String newValue(String newValue)
                {
                setXAxisLabel(newValue);
                getChartPanel().repaint();
                return newValue;
                }
            };
        xLabel.setValue(getXAxisLabel());
        
        list.add(new JLabel("X Label"), xLabel);

        yLabel = new PropertyField()
            {
            public String newValue(String newValue)
                {
                setYAxisLabel(newValue);
                getChartPanel().repaint();
                return newValue;
                }
            };
        yLabel.setValue(getYAxisLabel());
        
        list.add(new JLabel("Y Label"), yLabel);
        
        yLog = new JCheckBox();
        yLog.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e)
                {
                if(yLog.isSelected())
                    {
                    LogarithmicAxis logAxis = new LogarithmicAxis(yLabel.getValue());
                    logAxis.setStrictValuesFlag(false);
                    ((CategoryPlot)(chart.getPlot())).setRangeAxis(logAxis);
                    }
                else
                    ((CategoryPlot)(chart.getPlot())).setRangeAxis(new NumberAxis(yLabel.getValue()));
                }
            });

        list.add(new JLabel("Y Log Axis"), yLog);

        final JCheckBox ygridlines = new JCheckBox();
        ygridlines.setSelected(false);
        ItemListener il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                    ((CategoryPlot)(chart.getPlot())).setRangeGridlinesVisible(true);
                    }
                else
                    {
                    ((CategoryPlot)(chart.getPlot())).setRangeGridlinesVisible(false);
                    }
                }
            };
        ygridlines.addItemListener(il);

        // JFreeChart's Box Plots look awful when wide because the mean
        // circle is based on the width of the bar to the exclusion of all
        // else.  So I've restricted the width to be no more than 0.4, and 0.1
        // is the suggested default.
                
        final double INITIAL_WIDTH = 0.1;
        final double MAXIMUM_RATIONAL_WIDTH = 0.4;
                
        maximumWidthField = new NumberTextField(INITIAL_WIDTH, 2.0, 0)
            {
            public double newValue(double newValue) 
                {
                if (newValue <= 0.0 || newValue > MAXIMUM_RATIONAL_WIDTH) 
                    newValue = currentValue;
                ((BoxAndWhiskerRenderer)(((CategoryPlot)(chart.getPlot())).getRenderer())).setMaximumBarWidth(newValue);
                //update();
                return newValue;
                }
            };
        list.addLabelled("Max Width",maximumWidthField);

        Box box = Box.createHorizontalBox();
        box.add(new JLabel(" Y"));
        box.add(ygridlines);
        box.add(Box.createGlue());
        list.add(new JLabel("Y Grid Lines"), ygridlines);
        
        mean = new JCheckBox();
        mean.setSelected(true);
        il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                BoxAndWhiskerRenderer renderer = ((BoxAndWhiskerRenderer)((CategoryPlot)(chart.getPlot())).getRenderer());
                renderer.setMeanVisible(mean.isSelected());
                }
            };
        mean.addItemListener(il);

        median = new JCheckBox();
        median.setSelected(true);
        il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                BoxAndWhiskerRenderer renderer = ((BoxAndWhiskerRenderer)((CategoryPlot)(chart.getPlot())).getRenderer());
                renderer.setMedianVisible(median.isSelected());
                }
            };
        median.addItemListener(il);
        
        list.add(new JLabel("Mean"), mean);
        list.add(new JLabel("Median"), median);

        final JCheckBox horizontal = new JCheckBox();
        horizontal.setSelected(false);
        il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                CategoryPlot plot = (CategoryPlot)(chart.getPlot());                                
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                    plot.setOrientation(PlotOrientation.HORIZONTAL);
                    }
                else
                    {
                    plot.setOrientation(PlotOrientation.VERTICAL);
                    }
                //updateGridLines();
                }
            };
        horizontal.addItemListener(il);

        list.add(new JLabel("Horizontal"), horizontal);


        final JCheckBox whiskersUseFillColorButton = new JCheckBox();
        whiskersUseFillColorButton.setSelected(false);
        whiskersUseFillColorButton.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e)
                {
                BoxAndWhiskerRenderer renderer = ((BoxAndWhiskerRenderer)((CategoryPlot)(chart.getPlot())).getRenderer());
                renderer.setUseOutlinePaintForWhiskers(!whiskersUseFillColorButton.isSelected());
                }
            });

        box = Box.createHorizontalBox();
        box.add(new JLabel(" Colored"));
        box.add(whiskersUseFillColorButton);
        box.add(Box.createGlue());
        list.add(new JLabel("Whiskers"), box);
        }
    }
