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
import org.jfree.data.general.*;
import org.jfree.chart.title.*;
import org.jfree.data.xy.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/*  // looks like we'll have to move to these soon
    import com.itextpdf.text.*;
    import com.itextpdf.text.pdf.*;
*/

/**
   An abstract subclass of ChartGenerator for charts which involve X-Y data, such
   as Time Series, Histograms, and Scatter Plots.                
*/

public abstract class XYChartGenerator extends ChartGenerator
    {
    /** The global attributes domain axis field. */
    PropertyField xLabel;
    /** The global attributes range axis field. */
    PropertyField yLabel;
    
    /** The global attributes logarithmic range axis check box. */
    JCheckBox yLog;
    /** The global attributes logarithmic domain axis check box. */
    JCheckBox xLog;
    
    public void setXAxisLogScaled(boolean isLogScaled){xLog.setSelected(isLogScaled);}
    public boolean isXAxisLogScaled(){return xLog.isSelected();}
    public void setYAxisLogScaled(boolean isLogScaled){yLog.setSelected(isLogScaled);}
    public boolean isYAxisLogScaled(){return yLog.isSelected();}
        
    public Dataset getSeriesDataset() { return ((XYPlot)(chart.getPlot())).getDataset(); }
    public void setSeriesDataset(Dataset obj) { ((XYPlot)(chart.getPlot())).setDataset((XYDataset)obj); }





    /** Assumes that the underlying Dataset is an XYDataset.  Override this for other datasets. */
    public int getSeriesCount()
        {
        return ((XYDataset)getSeriesDataset()).getSeriesCount();
        }



    /** @deprecated
        Sets the name of the Range Axis label -- usually this is the Y axis. */
    public void setRangeAxisLabel(String val) { setYAxisLabel(val); }
        
    /** Sets the name of the Y Axis label. */
    public void setYAxisLabel(String val)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getRangeAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getRangeAxis()));
        yLabel.setValue(val);
        }
                
    /** @deprecated
        Returns the name of the Range Axis Label -- usually this is the Y axis. */
    public String getRangeAxisLabel() { return getYAxisLabel(); }
        
    /** Returns the name of the Y Axis label. */
    public String getYAxisLabel()
        {
        return ((XYPlot)(chart.getPlot())).getRangeAxis().getLabel();
        }
                
    /** @deprecated
        Sets the name of the Domain Axis label  -- usually this is the X axis. */
    public void setDomainAxisLabel(String val) { setXAxisLabel(val); }
        
    /** Sets the name of the X Axis label. */
    public void setXAxisLabel(String val)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getDomainAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getDomainAxis()));
        xLabel.setValue(val);
        }
                
    /** @deprecated Returns the name of the Domain Axis label -- usually this is the X axis. */
    public String getDomainAxisLabel() { return getXAxisLabel(); } 

    /** Returns the name of the X Axis label. */
    public String getXAxisLabel()
        {
        return ((XYPlot)(chart.getPlot())).getDomainAxis().getLabel();
        }
    
    /** Returns the underlying chart. **/
    public JFreeChart getChart()
        {
        return chart;
        }

    protected void buildGlobalAttributes(LabelledList list)
        {
        
        // create the chart
        ((XYPlot)(chart.getPlot())).setDomainGridlinesVisible(false);
        ((XYPlot)(chart.getPlot())).setRangeGridlinesVisible(false);
        ((XYPlot)(chart.getPlot())).setDomainGridlinePaint(new Color(200,200,200));
        ((XYPlot)(chart.getPlot())).setRangeGridlinePaint(new Color(200,200,200));

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
        
        xLog = new JCheckBox();
        xLog.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e)
                {
                if(xLog.isSelected())
                    {
                    LogarithmicAxis logAxis = new LogarithmicAxis(xLabel.getValue());
                    logAxis.setStrictValuesFlag(false);
                    chart.getXYPlot().setDomainAxis(logAxis);
                    }
                else
                    chart.getXYPlot().setDomainAxis(new NumberAxis(xLabel.getValue()));
                }
            });

        yLog = new JCheckBox();
        yLog.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e)
                {
                if(yLog.isSelected())
                    {
                    LogarithmicAxis logAxis = new LogarithmicAxis(yLabel.getValue());
                    logAxis.setStrictValuesFlag(false);
                    chart.getXYPlot().setRangeAxis(logAxis);
                    }
                else
                    chart.getXYPlot().setRangeAxis(new NumberAxis(yLabel.getValue()));
                }
            });

        Box box = Box.createHorizontalBox();
        box.add(new JLabel("X"));
        box.add(xLog);
        box.add(new JLabel(" Y"));
        box.add(yLog);
        box.add(Box.createGlue());
        list.add(new JLabel("Log Axis"), box);

        final JCheckBox xgridlines = new JCheckBox();
        xgridlines.setSelected(false);
        ItemListener il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                    chart.getXYPlot().setDomainGridlinesVisible(true);
                    }
                else
                    {
                    chart.getXYPlot().setDomainGridlinesVisible(false);
                    }
                }
            };
        xgridlines.addItemListener(il);

        final JCheckBox ygridlines = new JCheckBox();
        ygridlines.setSelected(false);
        il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                    chart.getXYPlot().setRangeGridlinesVisible(true);
                    }
                else
                    {
                    chart.getXYPlot().setRangeGridlinesVisible(false);
                    }
                }
            };
        ygridlines.addItemListener(il);


        box = Box.createHorizontalBox();
        box.add(new JLabel("X"));
        box.add(xgridlines);
        box.add(new JLabel(" Y"));
        box.add(ygridlines);
        box.add(Box.createGlue());
        list.add(new JLabel("Grid Lines"), box);
        }
    
    /** @deprecated */
    public void setRangeAxisRange(double lower, double upper) { setYAxisRange(lower, upper); }

    public void setYAxisRange(double lower, double upper)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getRangeAxis().setRange(lower, upper);
        }
                
    /** @deprecated */
    public void setDomainAxisRange(double lower, double upper) { setXAxisRange(lower, upper); }
        
    public void setXAxisRange(double lower, double upper)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getDomainAxis().setRange(lower, upper);
        }
    }

        
