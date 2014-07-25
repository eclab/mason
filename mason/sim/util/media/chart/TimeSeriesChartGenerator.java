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
import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/*  // looks like we'll have to move to these soon
    import com.itextpdf.text.*;
    import com.itextpdf.text.pdf.*;
*/

/**
   TimeSeriesChartGenerator is a ChartGenerator which displays a time-series chart using the JFreeChart library.
   The generator uses the XYSeriesCollection as its dataset, and thus holds some N XYSeriesDataset objects, each
   representing a time series displayed on the chart.  You add series to the generator with the <tt>addSeries</tt>
   method.
   
   <p>TimeSeriesChartGenerator creates attributes components in the form of TimeSeriesAttributes, which work with
   the generator to properly update the chart to reflect changes the user has made to its display.
*/

public class TimeSeriesChartGenerator extends XYChartGenerator
    {
    public void clearAllSeries()
        {
        SeriesAttributes[] c = getSeriesAttributes();
        for (int i = 0; i < c.length; i++)
            {
            ((TimeSeriesAttributes)(c[i])).clear();
            }
        }
        
    public void removeSeries(int index)
        {
        super.removeSeries(index);
        XYSeriesCollection xysc = (XYSeriesCollection) getSeriesDataset();
        xysc.removeSeries(index);
        }
                
    public void moveSeries(int index, boolean up)
        {
        super.moveSeries(index, up);
                
        if ((index > 0 && up) || (index < getSeriesCount() - 1 && !up))  // it's not the first or the last given the move
            {
            XYSeriesCollection xysc = (XYSeriesCollection) getSeriesDataset();
            // this requires removing everything from the dataset and resinserting, duh
            ArrayList items = new ArrayList(xysc.getSeries());
            xysc.removeAllSeries();
            
            int delta = up? -1:1;
            // now rearrange
            items.add(index + delta, items.remove(index));
            
            // rebuild the dataset
            for(int i = 0; i < items.size(); i++)
                xysc.addSeries(((XYSeries)(items.get(i))));
            }
        }

    /** Adds a series, plus a (possibly null) SeriesChangeListener which will receive a <i>single</i>
        event if/when the series is deleted from the chart by the user.  The series should have a key
        in the form of a String.  Returns the series attributes. */
    public SeriesAttributes addSeries( final XYSeries series, final SeriesChangeListener stopper)
        {
        XYSeriesCollection xysc = (XYSeriesCollection) getSeriesDataset();

        int i = xysc.getSeriesCount();
        series.setKey(new ChartGenerator.UniqueString(series.getKey()));
        xysc.addSeries(series);
        TimeSeriesAttributes csa = new TimeSeriesAttributes(this, series, i, stopper); 
        seriesAttributes.add(csa);
        revalidate();
        return csa;
        }
        
    
    protected void buildChart()
        {
        XYSeriesCollection collection = new XYSeriesCollection();
                
        chart = ChartFactory.createXYLineChart("Untitled Chart","Untitled X Axis","Untitled Y Axis", collection,
            PlotOrientation.VERTICAL, false, true, false);
        ((XYLineAndShapeRenderer)(((XYPlot)(chart.getPlot())).getRenderer())).setDrawSeriesLineAsPath(true);

        chart.setAntiAlias(true);
        //chartPanel = new ScrollableChartPanel(chart, true); 
        chartPanel = buildChartPanel(chart);           
        setChartPanel(chartPanel);           
//        chartHolder.getViewport().setView(chartPanel);
                
        // this must come last because the chart must exist for us to set its dataset
        setSeriesDataset(collection);
        }


    JCheckBox useCullingCheckBox;
    NumberTextField maxPointsPerSeriesTextField;
    DataCuller dataCuller;
    public DataCuller getDataCuller() {return dataCuller;}
    public void setDataCuller(DataCuller dataCuller) {this.dataCuller = dataCuller;}
    
    public TimeSeriesChartGenerator()
        {
        super();
        LabelledList globalAttribList = (LabelledList) (((DisclosurePanel)getGlobalAttribute(-2)).getDisclosedComponent());
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
