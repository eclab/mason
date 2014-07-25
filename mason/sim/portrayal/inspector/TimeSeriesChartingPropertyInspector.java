/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;
import java.awt.*;
import java.util.Iterator;
import sim.util.*;
import sim.display.*;
import sim.engine.*;
import sim.util.media.chart.*;
import org.jfree.data.xy.*;
import org.jfree.data.general.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.*;

/** A property inspector which generates time series of data.  Time series are extended in real-time
    as requested by the user.  Data properties for which
    the TimeSeriesChartingPropertyInspector will operate include:
        
    <ul>
    <li>Any numerical value (byte, int, double, etc.)
    <li>Any sim.util.Valuable object
    <li>Any Number (Double, Integer, etc.)
    </ul>
        
    <p>TimeSeriesChartingPropertyInspector registers itself with the property menu option "Chart".
*/
        
public class TimeSeriesChartingPropertyInspector extends ChartingPropertyInspector
    {
    XYSeries chartSeries = null;
    XYSeries aggregateSeries = new XYSeries("ChartingPropertyInspector.temp", false);

    protected boolean validChartGenerator(ChartGenerator generator) { return generator instanceof TimeSeriesChartGenerator; }

    public static String name() { return "Chart"; }
    public static Class[] types() 
        {
        return new Class[]
            {
            Number.class, Boolean.TYPE, Byte.TYPE, Short.TYPE,
            Integer.TYPE, Long.TYPE, Float.TYPE,
            Double.TYPE, Valuable.class
            };
        }

    public TimeSeriesChartingPropertyInspector(Properties properties, int index, Frame parent, final GUIState simulation)
        {
        super(properties,index,parent,simulation); 
        setupSeriesAttributes(properties, index); 
        }
    
    public TimeSeriesChartingPropertyInspector(Properties properties, int index, final GUIState simulation, ChartGenerator generator)
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
                ((XYChartGenerator)getGenerator()).setYAxisLabel("" + properties.getName(index));
                ((XYChartGenerator)getGenerator()).setXAxisLabel("Time");
                }
                        
            chartSeries = new XYSeries( properties.getName(index), false );

            // add our series
            seriesAttributes = ((TimeSeriesChartGenerator)generator).addSeries(chartSeries, new SeriesChangeListener()
                {
                public void seriesChanged(SeriesChangeEvent event) { getStopper().stop(); }
                });
            }
        }

    protected  ChartGenerator createNewGenerator()
        {
        return new TimeSeriesChartGenerator()
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

    protected double valueFor(Object o)
        {
        if (o instanceof java.lang.Number)  // compiler complains unless I include the full classname!!! Huh?
            return ((Number)o).doubleValue();
        else if (o instanceof Valuable)
            return ((Valuable)o).doubleValue();
        else if (o instanceof Boolean)
            return ((Boolean)o).booleanValue() ? 1 : 0;
        else return Double.NaN;  // unknown
        }
    
    void addToMainSeries(double x, double y, boolean notify)
        {
        chartSeries.add(x, y, false);
        TimeSeriesAttributes attributes = (TimeSeriesAttributes)(seriesAttributes);
        if (!attributes.possiblyCull())
            {
            if (notify)     // do a notification anyway
                chartSeries.fireSeriesChanged();
            }
        }

    protected void updateSeries(double time, double lastTime)
        {
        double d = 0;
                
        GlobalAttributes globalAttributes = getGlobalAttributes();
                
        // FIRST, load the aggregate series with the items
        aggregateSeries.add(time, d = valueFor(properties.getValue(index)), false);
        int len = aggregateSeries.getItemCount();
                                        
        // SECOND, determine if it's time to dump stuff into the main series
        long interval = globalAttributes.interval;
        double intervalMark = time % interval;
        if (!
            // I think these are the three cases for when we may need to update because
            // we've exceeded the next interval
                (intervalMark == 0 || 
                (time - lastTime >= interval) ||
                lastTime % interval > intervalMark))
            return;  // not yet
                                        
        // THIRD determine how and when to dump stuff into the main series
        double y = 0;  // make compiler happy
        double temp;
        switch(globalAttributes.aggregationMethod)
            {
            case AGGREGATIONMETHOD_CURRENT:  // in this case the aggregateSeries is sort of worthless
                addToMainSeries(time, d, false);
                break;
            case AGGREGATIONMETHOD_MAX:
                double maxX = 0;
                for(int i=0;i<len;i++)
                    {
                    XYDataItem item = (XYDataItem)(aggregateSeries.getDataItem(i));
                    y = item.getY().doubleValue();
                    temp = item.getX().doubleValue();
                    if( maxX < temp || i==0) maxX = temp;
                    }
                addToMainSeries( maxX, y, false );
                break;
            case AGGREGATIONMETHOD_MIN:
                double minX = 0;
                for(int i=0;i<len;i++)
                    {
                    XYDataItem item = (XYDataItem)(aggregateSeries.getDataItem(i));
                    y = item.getY().doubleValue();
                    temp = item.getX().doubleValue();
                    if( minX > temp || i==0) minX = temp;
                    }
                addToMainSeries( minX, y, false );
                break;
            case AGGREGATIONMETHOD_MEAN:
                double sumX = 0;
                int n = 0;
                for(int i=0;i<len;i++)
                    {
                    XYDataItem item = (XYDataItem)(aggregateSeries.getDataItem(i));
                    y = item.getY().doubleValue();
                    sumX += item.getX().doubleValue();
                    n++;
                    }
                if (n == 0)
                    {
                    // System.err.println( "No element????" );
                    }
                else addToMainSeries(sumX / n, y, false);
                break;
            default:
                throw new RuntimeException("No valid aggregation method provided");
            }
        aggregateSeries.clear();
        }

    // Should not load data except when the simulation is running
    protected boolean alwaysUpdateable() { return false; }
    }
