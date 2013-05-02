/*
  Copyright 2013 by Sean Luke and George Mason University
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

/** A property inspector which generates various bar charts of data.  The bar charts update in real-time as
    requested by the user.  Data properties for which
    the BarChartChartingPropertyInspector will operate include:
        
    <ul>
    <li>Any array of Objects
    </ul>
        
    <p>BarChartChartingPropertyInspector registers itself with the property menu option "Make Bar Chart".
*/

public class BarChartChartingPropertyInspector extends ChartingPropertyInspector
    {
    Object[] previousValues = new Object[] {  };  // sacrificial
        
    protected boolean validChartGenerator(ChartGenerator generator) { return generator instanceof BarChartGenerator; }
        
    protected boolean includeAggregationMethodAttributes() { return false; }

    public static String name() { return "Make Bar Chart"; }
    public static Class[] types() 
        {
        return new Class[]
            {
            new Object[0].getClass()
            };
        }

    public BarChartChartingPropertyInspector(Properties properties, int index, Frame parent, final GUIState simulation)
        {
        super(properties,index,parent,simulation);
        setupSeriesAttributes(properties, index);
        }
    
    public BarChartChartingPropertyInspector(Properties properties, int index, final GUIState simulation, ChartGenerator generator)
        {
        super(properties, index, simulation, generator);
        setupSeriesAttributes(properties, index);
        }
    
    // I isolated this code from the constructor into this method because I have two constructors now. 
    private void setupSeriesAttributes(Properties properties, int index)
        {
        if (isValidInspector())
            {
            if (getGenerator().getNumSeriesAttributes() == 0)  // recall that we've not been added yet
                {
                // take control
                getGenerator().setTitle("" + properties.getName(index) + " of " + properties.getObject());
                }

            // add our series
            seriesAttributes = ((BarChartGenerator)generator).addSeries(previousValues, properties.getName(index), 
                new SeriesChangeListener()
                    {
                    public void seriesChanged(SeriesChangeEvent event) { getStopper().stop(); }
                    });
            }
        }
                
    protected ChartGenerator createNewGenerator()
        {
        return new BarChartGenerator()
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
        Object[] vals = previousValues;  // set it to something in case we don't get anything new.
                
        //if (cls.isArray())
                {
                Class comp = cls.getComponentType();
                if (comp.equals(Object.class))
                    {
                    Object[] array = (Object[]) obj;
                    vals = new Object[array.length];
                    for(int i=0;i<array.length;i++)
                        vals[i] = array[i];
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

        ((BarChartGenerator)generator).updateSeries(seriesAttributes.getSeriesIndex(), vals); 
        }
    }
