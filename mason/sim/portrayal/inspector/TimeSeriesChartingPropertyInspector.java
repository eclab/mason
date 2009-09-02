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
        setupSeriesAttributes();
        }
    
    public TimeSeriesChartingPropertyInspector(Properties properties, int index, final GUIState simulation, ChartGenerator generator)
        {
        super(properties, index, simulation, generator);
        setupSeriesAttributes();
        }
    
    //I isolated this code from the constructor into this method because I have two constructors now. 
    private void setupSeriesAttributes()
        {            
        if (validInspector)
            {
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

                // remove the chart from the GUIState's guiObjects
                if( simulation.guiObjects != null )
                    simulation.guiObjects.remove(this);
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
    
//      public DataCuller dataCuller = null;    //null means no culling
    public void addToMainSeries(double x, double y, boolean notify)
        {
        chartSeries.add(x, y, false);
        //I postpone <code>fireSeriesChanged</code> till after  
        //the culling decision to save a repaint.
        
        DataCuller dataCuller = ((TimeSeriesChartGenerator)generator).getDataCuller();
        if(dataCuller!=null && dataCuller.tooManyPoints(chartSeries.getItemCount()))
            deleteItems(dataCuller.cull(getXValues(), true));
        else
            //no chage to chartSeries other then the add(), so
            if(notify)
                chartSeries.fireSeriesChanged();
        }
//    public void setDataCuller(DataCuller dc)
//    {
//      dataCuller = dc;
//    }
    static Bag tmpBag = new Bag();
    void deleteItems(IntBag items)
        {
        if(items.numObjs==0)
            return;
//      //I would sure hate to to do this (O(n^2), plus each remove causes a SeriesChangeEvent):
//      for(int i=items.numObjs-1;i>=0;i--)
//              chartSeries.remove(items.objs[i]);
        //here's the O(n) version (and just 2 SeriesChangeEvents)

        tmpBag.clear();
        int currentTabooIndex = 0;
        int currentTaboo = items.objs[0];
        Iterator iter = chartSeries.getItems().iterator();
        int index=0;
        while(iter.hasNext())
            {
            Object o = iter.next();
            if(index==currentTaboo)
                {
                //skip the copy, let's move on to next taboo index
                if(currentTabooIndex<items.numObjs-1)
                    {
                    currentTabooIndex++;
                    currentTaboo = items.objs[currentTabooIndex];
                    }
                else
                    currentTaboo=-1;//no more taboos
                }
            else//save o
                tmpBag.add(o);
            index++;
            }
        //now we clear the chartSeries and then put back the saved objects only.
        chartSeries.clear();
        //In my test this did not cause the chart to flicker.
        //But if it does, one could do an update for the part the will be refill and 
        //only clear the rest using delete(start, end).
        for(int i=0;i<tmpBag.numObjs;i++)
            chartSeries.add((XYDataItem)tmpBag.objs[i], false);//no notifying just yet.
        tmpBag.clear();
        //it doesn't matter that I clear this twice in a row 
        //(once here, once at next time through this fn), the second time is O(1).
        chartSeries.fireSeriesChanged();
        }
    double[] getXValues()
        {
        double[] xValues = new double[chartSeries.getItemCount()];
        for(int i=0;i<xValues.length;i++)
            xValues[i]=chartSeries.getX(i).doubleValue();
        return xValues;
        }

    protected void updateSeries(double time, double lastTime)
        {
        double d = 0;
                
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
                    System.err.println( "No element????" );
                else addToMainSeries(sumX / n, y, false);
                break;
            default:
                System.err.println( "There are only four aggregation method implemented" );
            }
        aggregateSeries.clear();
        }
    }
