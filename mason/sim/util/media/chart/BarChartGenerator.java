/*
  Copyright 2013 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.image.*;
import java.util.*;
import java.text.Format.*;

import sim.util.gui.*;
import sim.util.gui.Utilities;
import sim.util.*;
import sim.display.*;
import sim.util.media.*;

// From JFreeChart
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.general.*;
import org.jfree.data.statistics.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.category.*;
import org.jfree.chart.title.*;
import org.jfree.ui.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/*  // looks like we'll have to move to these soon
    import com.itextpdf.text.*;
    import com.itextpdf.text.pdf.*;
*/


/**
A ChartGenerator for Bar Charts.  Similar enough to PieChartGenerator that it subclasses from it to share methods.
*/ 

public class BarChartGenerator extends PieChartGenerator
    {
    /** The global attributes domain axis field. */
    PropertyField xLabel;
    /** The global attributes range axis field. */
    PropertyField yLabel;
    

    /** Sets the name of the Y Axis label. */
    public void setYAxisLabel(String val)
        {
        CategoryPlot xyplot = (CategoryPlot)(chart.getPlot());
        xyplot.getRangeAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getRangeAxis()));
        yLabel.setValue(val);
        }
                
    /** Returns the name of the Y Axis label. */
    public String getYAxisLabel()
        {
        return ((CategoryPlot)(chart.getPlot())).getRangeAxis().getLabel();
        }
                
    /** Sets the name of the X Axis label. */
    public void setXAxisLabel(String val)
        {
        CategoryPlot xyplot = (CategoryPlot)(chart.getPlot());
        xyplot.getDomainAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getDomainAxis()));
        xLabel.setValue(val);
        }
                
    /** Returns the name of the X Axis label. */
    public String getXAxisLabel()
        {
        return ((CategoryPlot)(chart.getPlot())).getDomainAxis().getLabel();
        }
        
    BarRenderer barRenderer;
    StackedBarRenderer stackedBarRenderer;
    StackedBarRenderer percentageRenderer;
    
    BarRenderer getBarRenderer() { return barRenderer; }
    BarRenderer getStackedBarRenderer() { return stackedBarRenderer; }
    BarRenderer getPercentageRenderer() { return percentageRenderer; }
    
    void reviseRenderer(BarRenderer renderer)
    	{
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setBaseOutlineStroke(new BasicStroke(2.0f));
        renderer.setDrawBarOutline(true);
		renderer.setBaseItemLabelGenerator(
    		new StandardCategoryItemLabelGenerator(
        		"{0}", java.text.NumberFormat.getInstance())); // {0} is the row key, {1} is the column key, {2} is the value
		renderer.setBaseItemLabelsVisible(true);
    	}
    
    protected void buildGlobalAttributes(LabelledList list)
        {
        // create the chart
        CategoryPlot plot = (CategoryPlot)(chart.getPlot());
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		plot.setDomainGridlinePaint(new Color(200,200,200));
		plot.setRangeGridlinePaint(new Color(200,200,200));
		
        // define the renderers
        barRenderer = new BarRenderer();
        reviseRenderer(barRenderer);
        
        stackedBarRenderer = new StackedBarRenderer(false);
        reviseRenderer(stackedBarRenderer);
        
        percentageRenderer = new StackedBarRenderer(true);
        reviseRenderer(percentageRenderer);
                
		plot.setRenderer(barRenderer);
        
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

        final JCheckBox gridlines = new JCheckBox();
        gridlines.setSelected(false);
        ItemListener il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                hasgridlines = (e.getStateChange() == ItemEvent.SELECTED);
                updateGridLines();
                }
            };
        gridlines.addItemListener(il);

		list.add(new JLabel("Grid Lines"), gridlines);
		
        final JCheckBox labels = new JCheckBox();
        labels.setSelected(true);
        il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {                    
                    getBarRenderer().setBaseItemLabelsVisible(true);
                    getStackedBarRenderer().setBaseItemLabelsVisible(true);
                    getPercentageRenderer().setBaseItemLabelsVisible(true);
                    }
                else
                    {
                    getBarRenderer().setBaseItemLabelsVisible(false);
                    getStackedBarRenderer().setBaseItemLabelsVisible(false);
                    getPercentageRenderer().setBaseItemLabelsVisible(false);
                    }
                }
            };
        labels.addItemListener(il);
		list.add(new JLabel("Labels"), labels);

        final JComboBox barType = new JComboBox();
        barType.setEditable(false);
        barType.setModel(new DefaultComboBoxModel(new java.util.Vector(Arrays.asList(
                        new String[] { "Separate", "Stacked", "Percentage" }))));
        barType.setSelectedIndex(0);
        barType.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
		        CategoryPlot plot = (CategoryPlot)(chart.getPlot());
                int type = barType.getSelectedIndex();
                
                if (type == 0) // separate
                	{
					plot.setRenderer(getBarRenderer());
                	}
                else if (type == 1)  // stacked
                	{
					plot.setRenderer(getStackedBarRenderer());
                	}
                else				// percentage
                	{
					plot.setRenderer(getPercentageRenderer());
                	}
                }
            });
        list.add(new JLabel("Bars"), barType);


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
                    ishorizontal = true;                 
                    }
                else
                    {
                    plot.setOrientation(PlotOrientation.VERTICAL);
                    ishorizontal = false;                 
                    }
                updateGridLines();
                }
            };
        horizontal.addItemListener(il);

		list.add(new JLabel("Horizontal"), horizontal);
        }    
    
    boolean hasgridlines = false;
    boolean ishorizontal = false;
    void updateGridLines()
    	{
    	if (hasgridlines)
    		{
    		if (ishorizontal)
    			{
    			chart.getCategoryPlot().setRangeGridlinesVisible(true);
    			chart.getCategoryPlot().setDomainGridlinesVisible(false);
    			}
    		else
    			{
    			chart.getCategoryPlot().setRangeGridlinesVisible(true);
    			chart.getCategoryPlot().setDomainGridlinesVisible(false);
    			}
    		}
    	else
    		{
    		chart.getCategoryPlot().setRangeGridlinesVisible(false);
    		chart.getCategoryPlot().setDomainGridlinesVisible(false);
    		}
    	}
    
    public static final int MAXIMUM_BAR_CHART_ITEMS = 20;
    final DefaultCategoryDataset emptyDataset = new DefaultCategoryDataset();
    public Dataset getSeriesDataset() { return ((CategoryPlot)(chart.getPlot())).getDataset(); }
    public void setSeriesDataset(Dataset obj) 
    	{
    	// here we will interrupt things if they're too big
    	if (((CategoryDataset)obj).getRowCount() > MAXIMUM_BAR_CHART_ITEMS)
    		{
    		((CategoryPlot)(chart.getPlot())).setDataset(emptyDataset);
    		setInvalidChartTitle("[[ Dataset has too many items. ]]");
    		}
    	else
    		{
    		((CategoryPlot)(chart.getPlot())).setDataset((DefaultCategoryDataset)obj);
    		if (invalidChartTitle != null)
    			setInvalidChartTitle(null);
    		}
    	}

    protected void buildChart()
        {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        chart = ChartFactory.createBarChart("Untitled Chart", "Category", "Value", dataset,
            PlotOrientation.VERTICAL, false, true, false);
        chart.setAntiAlias(true);
        chartPanel = new ScrollableChartPanel(chart, true);            
        chartHolder.getViewport().setView(chartPanel);
        		
        // this must come last because the chart must exist for us to set its dataset
        setSeriesDataset(dataset);
        }
 
    protected void update()
        {
        // We have to rebuild the dataset from scratch (deleting and replacing it) because JFreeChart's
        // piechart facility doesn't have a way to move series.  Just like the histogram system: stupid stupid stupid.

        SeriesAttributes[] sa = getSeriesAttributes();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for(int i=0; i < sa.length; i++)
        	if (sa[i].isPlotVisible())
				{
				BarChartSeriesAttributes attributes = (BarChartSeriesAttributes)(sa[i]);
				double[] values = attributes.getValues();
				String[] labels = attributes.getLabels();
				UniqueString seriesName = new UniqueString(attributes.getSeriesName());
	
				for(int j = 0; j < values.length; j++)
					dataset.addValue(values[j], labels[j], seriesName);  // ugh
				}
                        
        setSeriesDataset(dataset);
        }

    public BarChartGenerator()
        {
        // buildChart is called by super() first
        }


    /** Adds a series, plus a (possibly null) SeriesChangeListener which will receive a <i>single</i>
        event if/when the series is deleted from the chart by the user. Returns the series attributes. */
 	SeriesAttributes addSeries(double[] amounts, String[] labels, String name, SeriesChangeListener stopper)
        {
        int i = getSeriesCount();
        
        // need to have added the dataset BEFORE calling this since it'll try to change the name of the series
        BarChartSeriesAttributes csa = new BarChartSeriesAttributes(this, name, i, amounts, labels, stopper);
        seriesAttributes.add(csa);
                
        revalidate();  // display the new series panel
        update();
                
        // won't update properly unless I force it here by letting all the existing scheduled events to go through.  Dumb design.  :-(
        SwingUtilities.invokeLater(new Runnable() { public void run() { update(); } });
                
        return csa;
        }

    /** Adds a series, plus a (possibly null) SeriesChangeListener which will receive a <i>single</i>
        event if/when the series is deleted from the chart by the user. Returns the series attributes. */
    public SeriesAttributes addSeries(Object[] objs, String name, SeriesChangeListener stopper)
        {
        HashMap map = convertIntoAmountsAndLabels(objs);
        String[] labels = revisedLabels(map);
        double[] amounts = amounts(map, labels);
        return addSeries(amounts, labels, name, stopper);
        }
        
    public void updateSeries(int index, Object[] objs)
        {
        if (index < 0) // this happens when we're a dead chart but the inspector doesn't know
            return;
            
        if (index >= getNumSeriesAttributes())  // this can happen when we close a window if we use the Histogram in a display
            return;

        HashMap map = convertIntoAmountsAndLabels(objs);
        String[] labels = revisedLabels(map);
        double[] amounts = amounts(map, labels);

		updateSeries(index, amounts, labels);
        }
    
    void updateSeries(int index, double[] amounts, String[] labels)
    	{
        if (index < 0) // this happens when we're a dead chart but the inspector doesn't know
            return;
            
        if (index >= getNumSeriesAttributes())  // this can happen when we close a window if we use the Histogram in a display
            return;

        BarChartSeriesAttributes hsa = (BarChartSeriesAttributes)(getSeriesAttribute(index));
        hsa.setValues(amounts);
        hsa.setLabels(labels);
    	}    	
    }

