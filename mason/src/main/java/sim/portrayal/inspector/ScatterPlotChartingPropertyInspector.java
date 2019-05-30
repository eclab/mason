/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;
import java.awt.*;
import java.awt.event.*;
import sim.util.*;
import sim.display.*;
import sim.engine.*;
import javax.swing.*;
import sim.util.gui.*;
import sim.util.media.chart.*;
import org.jfree.data.xy.*;
import org.jfree.data.general.*;

public class ScatterPlotChartingPropertyInspector extends ChartingPropertyInspector
    {
    double[][] previousValues = new double[2][0]; // sacrificial
            
    protected boolean validChartGenerator(ChartGenerator generator) { return generator instanceof ScatterPlotGenerator; }
        
    protected boolean includeAggregationMethodAttributes() { return false; }

    public static String name() { return "Make Scatter Plot"; }
    public static Class[] types() 
        {
        return new Class[]
            {
            new Double2D[0].getClass(), new Int2D[0].getClass(), ChartUtilities.ProvidesDoubleDoubles.class
            };
        }

    public ScatterPlotChartingPropertyInspector(Properties properties, int index, Frame parent, final GUIState simulation)
        {
        super(properties,index,parent,simulation);
        setupSeriesAttributes(properties, index);
        }
    
    public ScatterPlotChartingPropertyInspector(Properties properties, int index, final GUIState simulation, ChartGenerator generator)
        {
        super(properties, index, simulation, generator);
        setupSeriesAttributes(properties, index);
        }
    
    //I isolated this code from the constructor into this method because I have two constructors now. 
    private void setupSeriesAttributes(Properties properties, int index)
        {
        if (isValidInspector())
            {
            if (getGenerator().getNumSeriesAttributes() == 0)  // recall that we've not been added yet
                {
                // take control
                getGenerator().setTitle("" + properties.getName(index) + " of " + properties.getObject());
                ((XYChartGenerator)getGenerator()).setYAxisLabel("Y " + properties.getName(index));
                ((XYChartGenerator)getGenerator()).setXAxisLabel("X " + properties.getName(index));
                }

            // add our series
            seriesAttributes = ((ScatterPlotGenerator)generator).addSeries(previousValues, properties.getName(index), 
                new SeriesChangeListener()
                    {
                    public void seriesChanged(SeriesChangeEvent event) { getStopper().stop(); }
                    });
            }
        }
                
    protected ChartGenerator createNewGenerator()
        {
        return new ScatterPlotGenerator()
            {
            public void quit()
                {
                super.quit();
                Stoppable stopper = getStopper();
                if (stopper!=null) stopper.stop();

                // remove the chart from the GUIState's charts
                getCharts(simulation).remove(this);
                }
            };
        }

    public void updateSeries(double time, double lastTime)
        {
        Object obj = properties.getValue(index);
        if (obj==null) return;
        Class cls = obj.getClass();
        double[][] vals = previousValues;  // set it to something in case we don't get anything new.
                
        if (cls.isArray())
            {
            Class comp = cls.getComponentType();
            // this is gonna be long
            if (comp.equals(Double2D.class))
                {
                Double2D[] array = (Double2D[]) obj;
                vals = new double[2][array.length];
                for(int i=0;i<array.length;i++)
                    { vals[0][i] = array[i].x; vals[1][i] = array[i].y; }
                }
            else if (comp.equals(Int2D.class))
                {
                Int2D[] array = (Int2D[]) obj;
                vals = new double[2][array.length];
                for(int i=0;i<array.length;i++)
                    { vals[0][i] = array[i].x; vals[1][i] = array[i].y; }
                }
            else if (obj instanceof ChartUtilities.ProvidesDoubleDoubles)
                {
                double[][] array = ((ChartUtilities.ProvidesDoubleDoubles) obj).provide();
                vals = new double[2][array.length];
                for(int i=0;i<array.length;i++)
                    { vals[0][i] = array[0][i]; vals[1][i] = array[1][i]; }
                }
            }
                                
        boolean same = true;
        if (previousValues != null && vals.length == previousValues.length)
            {
            for(int i=0;i < vals.length; i++)
                if (vals[i] != previousValues[i])
                    { same = false; break; }
            }
        else same = false;
                
        if (same) return;  // they're identical

        // at this point we're committed to do an update
        previousValues = vals;
                
        ((ScatterPlotGenerator)generator).updateSeries(seriesAttributes.getSeriesIndex(), vals); 
        }
    }
