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

/** A property inspector which generates histograms of data.  The histograms update in real-time as
    requested by the user.  Data properties for which
    the HistogramChartingPropertyInspector will operate include:
        
    <ul>
    <li>Any array of numerical values (byte[], int[], double[], etc.) 
    <li>Any array of sim.util.Valuable
    <li>Any array of Numbers (Double[], Integer[], etc.)
    <li>Any sim.util.IntBag
    <li>Any sim.util.DoubleBag
    </ul>
        
    <p>HistogramChartingPropertyInspector registers itself with the property menu option "Make Histogram".
*/

public class HistogramChartingPropertyInspector extends ChartingPropertyInspector
    {
    /** The initial number of bins in histograms. */
    public static final int DEFAULT_BINS = 8;
    double[] previousValues = new double[] { 0 };  // sacrificial
        
    protected boolean validChartGenerator(ChartGenerator generator) { return generator instanceof HistogramGenerator; }
        
    protected boolean includeAggregationMethodAttributes() { return false; }

    public static String name() { return "Make Histogram"; }
    public static Class[] types() 
        {
        return new Class[]
            {
            new byte[0].getClass(), new short[0].getClass(), new int[0].getClass(), new long[0].getClass(),
            new float[0].getClass(), new double[0].getClass(), new boolean[0].getClass(), new Valuable[0].getClass(),
            new Number[0].getClass(), IntBag.class, DoubleBag.class
            };
        }

    public HistogramChartingPropertyInspector(Properties properties, int index, Frame parent, final GUIState simulation)
        {
        super(properties,index,parent,simulation);
        setupSeriesAttributes();
        }
    
    public HistogramChartingPropertyInspector(Properties properties, int index, final GUIState simulation, ChartGenerator generator)
        {
        super(properties, index, simulation, generator);
        setupSeriesAttributes();
        }
    
    //I isolated this code from the constructor into this method because I have two constructors now. 
    private void setupSeriesAttributes()
        {
        if (validInspector)
            {
            // add our series
            seriesAttributes = ((HistogramGenerator)generator).addSeries(previousValues, DEFAULT_BINS, properties.getName(index), 
                new SeriesChangeListener()
                    {
                    public void seriesChanged(SeriesChangeEvent event) { getStopper().stop(); }
                    });
                        
            // force an update to get it right.  See the documentation for addSeries(...)
            updateInspector();
            repaint();
            }
        }
                
    protected ChartGenerator createNewGenerator()
        {
        return new HistogramGenerator()
            {
            public void quit()
                {
                super.quit();
                Stoppable stopper = getStopper();
                if (stopper!=null) stopper.stop();

                // remove the chart from the GUIState's guiObjects
                if( simulation.guiObjects != null )
                    simulation.guiObjects.remove(this);
                }
            };
        }

    public void updateSeries(double time, double lastTime)
        {
        Object obj = properties.getValue(index);
        if (obj==null) return;
        Class cls = obj.getClass();
        double[] vals = previousValues;  // set it to something in case we don't get anything new.
                
        if (cls.isArray())
            {
            Class comp = cls.getComponentType();
            // this is gonna be long
            if (comp.equals(Byte.TYPE))
                {
                byte[] array = (byte[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i];
                }
            else if (comp.equals(Short.TYPE))
                {
                short[] array = (short[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i];
                }
            else if (comp.equals(Integer.TYPE))
                {
                int[] array = (int[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i];
                }
            else if (comp.equals(Long.TYPE))
                {
                long[] array = (long[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i];
                }
            else if (comp.equals(Float.TYPE))
                {
                float[] array = (float[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i];
                }
            else if (comp.equals(Double.TYPE))
                {
                // yeah, yeah, yeah, double->double...
                double[] array = (double[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i];
                }
            else if (comp.equals(Boolean.TYPE))
                {
                boolean[] array = (boolean[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = (array[i] ? 1 : 0);
                }
            else if (comp.equals(Valuable.class))
                {
                Valuable[] array = (Valuable[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i].doubleValue();
                }
            else if (comp.equals(Number.class))
                {
                Number[] array = (Number[]) obj;
                vals = new double[array.length];
                for(int i=0;i<array.length;i++)
                    vals[i] = array[i].doubleValue();
                }
            }
        else if (obj instanceof IntBag)
            {
            IntBag bag = (IntBag)(obj);
            vals = new double[bag.numObjs];
            for(int i=0; i < bag.numObjs; i++)
                vals[i] = (bag.objs[i]);
            }
        else if (obj instanceof DoubleBag)
            {
            DoubleBag bag = (DoubleBag)(obj);
            vals = new double[bag.numObjs];
            for(int i=0; i < bag.numObjs; i++)
                vals[i] = (bag.objs[i]);
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
                
        // I'm worried this will update every time, perhaps very expensive.  It's not clear from the JFreeChart docs.
        // if this is the case, then we need to delay the update by overloading the update method
        ((HistogramGenerator)generator).updateSeries(seriesAttributes.getSeriesIndex(), vals, true); 
        }
    }
