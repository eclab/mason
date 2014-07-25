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

/** A property inspector which generates pie charts of data.  The pie charts update in real-time as
    requested by the user.  Data properties for which
    the PieChartChartingPropertyInspector will operate include:
        
    <ul>
    <li>Any array of Objects
    </ul>
        
    <p>PieChartChartingPropertyInspector registers itself with the property menu option "Make Pie Chart".
*/

public class PieChartChartingPropertyInspector extends ChartingPropertyInspector
    {
    Object[] previousValues = new Object[] {  };  // sacrificial
    
    // this double-instanceof hack is a simple way of only locking onto PieChartGenerator
    // but still permitting anonymous subclasses (which are critical) but NOT BarChartGenerator.
    // The alternative would be to have each class emit some kind of key and you check for that
    // key, where anonymous subclasses don't override the emitting function.  But for now since
    // there are so few generators, we're doing it this ugly way instead.
    protected boolean validChartGenerator(ChartGenerator generator) { return generator instanceof PieChartGenerator && 
            !(generator instanceof BarChartGenerator); }
        
    protected boolean includeAggregationMethodAttributes() { return false; }

    public static String name() { return "Make Pie Chart"; }
    public static Class[] types() 
        {
        return new Class[]
            {
            new Object[0].getClass(), java.util.Collection.class,
            ChartUtilities.ProvidesDoublesAndLabels.class,
            ChartUtilities.ProvidesObjects.class,
            ChartUtilities.ProvidesCollection.class,
            };
        }

    public PieChartChartingPropertyInspector(Properties properties, int index, Frame parent, final GUIState simulation)
        {
        super(properties,index,parent,simulation);
        setupSeriesAttributes(properties, index);
        }
    
    public PieChartChartingPropertyInspector(Properties properties, int index, final GUIState simulation, ChartGenerator generator)
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
            seriesAttributes = ((PieChartGenerator)generator).addSeries(previousValues, properties.getName(index), 
                new SeriesChangeListener()
                    {
                    public void seriesChanged(SeriesChangeEvent event) { getStopper().stop(); }
                    });
            }
        }
                
    protected ChartGenerator createNewGenerator()
        {
        return new PieChartGenerator()
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
                
        if (cls.isArray())
            {
            Class comp = cls.getComponentType();
            if (Object.class.isAssignableFrom(comp))
                {
                Object[] array = (Object[]) obj;
                vals = new Object[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i];
                }
            }
        else if (java.util.Collection.class.isAssignableFrom(cls))
            {
            Object[] array = ((java.util.Collection) obj).toArray();
            vals = new Object[array.length];
            for(int i=0;i<array.length;i++)
                vals[i] = array[i];
            }
        else if (obj instanceof ChartUtilities.ProvidesObjects)
            {
            Object[] array = ((ChartUtilities.ProvidesObjects) obj).provide();
            vals = new Object[array.length];
            for(int i=0;i<array.length;i++)
                vals[i] = array[i];
            }
        else if (obj instanceof ChartUtilities.ProvidesCollection)
            {
            Object[] array = ((ChartUtilities.ProvidesCollection) obj).provide().toArray();
            vals = new Object[array.length];
            for(int i=0;i<array.length;i++)
                vals[i] = array[i];
            }
        else if (obj instanceof ChartUtilities.ProvidesDoublesAndLabels)  // Handled Specially
            {
            double[] array = ((ChartUtilities.ProvidesDoublesAndLabels) obj).provide();
            String[] labels = ((ChartUtilities.ProvidesDoublesAndLabels) obj).provideLabels();
            previousValues = null;
            ((PieChartGenerator)generator).updateSeries(seriesAttributes.getSeriesIndex(), array, labels); 
            return;
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

        ((PieChartGenerator)generator).updateSeries(seriesAttributes.getSeriesIndex(), vals); 
        }
    }
