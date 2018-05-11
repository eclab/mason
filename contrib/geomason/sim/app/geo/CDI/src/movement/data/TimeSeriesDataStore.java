package CDI.src.movement.data;


import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

/**
 * Store the Data for Time Series
 * 
 * @author Eric 'Siggy' Scott
 
 * @see org.jfree.data.xy.XYSeries
 */
public class TimeSeriesDataStore<T extends Number>
{
    private XYSeries series;
    
    /** Takes a unique description of the series that can be used for display */
    public TimeSeriesDataStore(String description)
    {
        assert(description != null);
        series = new XYSeries(description);
        series.setDescription(description);
    }
    
    public XYSeries getData()
    {
        return series;
    }
    
    public String getDescription()
    {
        return series.getDescription();
    }

    public void addDataPoint(double time, T value)
    {
        series.add(new XYDataItem(time, value));
    }


    public void clear()
    {
        series.clear();
    }
}
