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

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
   TimeSeriesChartGenerator is a ChartGenerator which displays a time-series chart using the JFreeChart library.
   The generator uses the XYSeriesCollection as its dataset, and thus holds some N XYSeriesDataset objects, each
   representing a time series displayed on the chart.  You add series to the generator with the <tt>addSeries</tt>
   method.
   
   <p>TimeSeriesChartGenerator creates attributes components in the form of TimeSeriesAttributes, which work with
   the generator to properly update the chart to reflect changes the user has made to its display.
*/

public class TimeSeriesChartGenerator extends ChartGenerator
    {
    /** The dataset.  Generated in buildChart(). */
    protected XYSeriesCollection dataset;
    /** A list of SeriesChangeListeners, one per element in the dataset, and indexed in the same way.
        When an element is removed from the dataset and deleted from the chart, its corresponding
        SeriesChangeListener will be removed and have seriesChanged(...) called. */
    protected ArrayList stoppables = new ArrayList();
        
    public AbstractSeriesDataset getSeriesDataset() { return dataset; }

    DatasetChangeEvent updateEvent;
    // We issue a datset change event because various changes may have been made by our attributes
    // objects and they haven't informed the graph yet.   That way we can bulk up lots of changes
    // before we do a redraw.
    public void update()
        {
        if (updateEvent == null)
            updateEvent = new DatasetChangeEvent(chart.getPlot(), null);
        chart.getPlot().datasetChanged(updateEvent);
        }

    public void removeSeries(int index)
        {
        // stop the inspector....
        Object tmpObj = stoppables.remove(index);
        if( ( tmpObj != null ) && ( tmpObj instanceof SeriesChangeListener ) )
            ((SeriesChangeListener)tmpObj).seriesChanged(new SeriesChangeEvent(this));
        
        // remove from the dataset.  This is easier done in some JFreeChart plots than others, dang coders
        dataset.removeSeries(index);
                
        // remove the attribute
        seriesAttributes.remove(index);
                
        // shift all the seriesAttributes' indices down so they know where they are
        Component[] c = seriesAttributes.getComponents();
        for(int i = index; i < c.length; i++)  // do for just the components >= index in the seriesAttributes
            {
            if (i >= index) 
                {
                SeriesAttributes csa = (SeriesAttributes)(c[i]);
                csa.setSeriesIndex(csa.getSeriesIndex() - 1);
                csa.rebuildGraphicsDefinitions();
                }
            }
        revalidate();
        }
                
    public void moveSeries(int index, boolean up)
        {
        java.util.List allSeries = dataset.getSeries();
        int count = allSeries.size();
        
        if ((index > 0 && up) || (index < count-1 && !up))  // it's not the first or the last given the move
            {
            // this requires removing everything from the dataset and resinserting, duh
            ArrayList items = new ArrayList(allSeries);
            dataset.removeAllSeries();
            
            int delta = up? -1:1;
            // now rearrange
            items.add(index + delta, items.remove(index));
            
            // rebuild the dataset
            for(int i = 0; i < count; i++)
                dataset.addSeries(((XYSeries)(items.get(i))));
                    
            
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

        }

    protected void buildChart()
        {
        dataset = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart("Untitled Chart","Untitled X Axis","Untitled Y Axis",dataset,
            PlotOrientation.VERTICAL, false, true, false);
        ((XYLineAndShapeRenderer)(((XYPlot)(chart.getPlot())).getRenderer())).setDrawSeriesLineAsPath(true);

        chart.setAntiAlias(false);
        chartPanel = new ChartPanel(chart, true);
        chartPanel.setPreferredSize(new java.awt.Dimension(640,480));
        chartPanel.setMinimumDrawHeight(10);
        chartPanel.setMaximumDrawHeight(2000);
        chartPanel.setMinimumDrawWidth(20);
        chartPanel.setMaximumDrawWidth(2000);
        chartHolder.getViewport().setView(chartPanel);
        }


    /** Adds a series, plus a (possibly null) SeriesChangeListener which will receive a <i>single</i>
        event if/when the series is deleted from the chart by the user.  The series should have a key
        in the form of a String.  Returns the series attributes. */
    public TimeSeriesAttributes addSeries( final XYSeries series, final org.jfree.data.general.SeriesChangeListener stopper)
        {
        int i = dataset.getSeriesCount();
        dataset.addSeries(series);
        TimeSeriesAttributes csa = new TimeSeriesAttributes(this, series, i); 
        seriesAttributes.add(csa);
        stoppables.add( stopper );
        revalidate();
        return csa;
        }
        
    
    
    protected JCheckBox useCullingCheckBox;
    protected NumberTextField maxPointsPerSeriesTextField;
    protected DataCuller  dataCuller;
    public DataCuller getDataCuller(){return dataCuller;}
    public void setDataCuller(DataCuller dataCuller){this.dataCuller = dataCuller;}
    
    public TimeSeriesChartGenerator()
        {
        super();
        LabelledList globalAttribList = (LabelledList) getGlobalAttribute(-2);
        useCullingCheckBox = new JCheckBox();
        
        globalAttribList.add(new JLabel("Cull Data"), useCullingCheckBox);
        maxPointsPerSeriesTextField = new NumberTextField(1000)
            {
            public double newValue(final double val)
                {
                int max = (int)val;
                if(val<2)
                    return (int)getValue();
                dataCuller = new MinGapDataCuller(max);
                return max;
                }
            };
        useCullingCheckBox.setSelected(true);
        globalAttribList.add(new JLabel("... Over"),maxPointsPerSeriesTextField);
        maxPointsPerSeriesTextField.setToolTipText("The maximum number of data points in a series before data culling gets triggered.");

        dataCuller = new MinGapDataCuller((int)maxPointsPerSeriesTextField.getValue());

        
        useCullingCheckBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                if(useCullingCheckBox.isSelected())
                    {
                    maxPointsPerSeriesTextField.setEnabled(true);
                    int maxPoints = (int)maxPointsPerSeriesTextField.getValue();
                    dataCuller = new MinGapDataCuller(maxPoints);
                    }
                else
                    {
                    maxPointsPerSeriesTextField.setEnabled(false);
                    dataCuller = null;
                    }
                }
            }); 

        }

    }
