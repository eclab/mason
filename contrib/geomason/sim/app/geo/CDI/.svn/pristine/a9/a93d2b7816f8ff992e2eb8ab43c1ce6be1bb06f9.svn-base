package masoncsc.datawatcher;


import masoncsc.util.Pair;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

/**
 * Listens to a DataWatcher and records a time series of scalar values from it.
 * 
 * @author Eric 'Siggy' Scott
 * @author Joey Harrison
 * @see org.jfree.data.xy.XYSeries
 */
public class TimeSeriesDataStore<T extends Number> implements DataStore<Pair<Long,T>, XYSeries>
{
    private XYSeries series;
    
    /** Takes a unique description of the series that can be used for display */
    public TimeSeriesDataStore(String description)
    {
        assert(description != null);
        series = new XYSeries(description);
        series.setDescription(description);
    }
    
    @Override
    public XYSeries getData()
    {
        return series;
    }
    
    public String getDescription()
    {
        return series.getDescription();
    }

    @Override
    public void dataUpdated(DataWatcher<Pair<Long,T>> source)
    {
        Long time = source.getDataPoint().getLeft();
        T value = source.getDataPoint().getRight();
        series.add(new XYDataItem(time, value));
    }

    @Override
    public void clear()
    {
        series.clear();
    }
}
