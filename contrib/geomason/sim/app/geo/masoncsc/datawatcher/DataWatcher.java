package masoncsc.datawatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of the current value of something observed in the model.
 * 
 * @author Eric 'Siggy' Scott
 * @author Joey Harrison
 * @see DataWatcher, DataListener
 */
public abstract class DataWatcher<T>
{
    /** Listeners that are notified when the observed value is updated */
    private List<DataListener<T>> dataListeners = new ArrayList<DataListener<T>>();
    
    /** The current observed value */
    public abstract T getDataPoint();
    
    /** Measure the observation.  This method's behavior depends on updateData(). */
    public final void update()
    {
        updateDataPoint();
        fireChange();
    }
    
    /** Updates the value returned by getDataPoint().  Called by update(). */
    protected abstract void updateDataPoint();
    
    /** Notifies the listeners that the data has been updated. */
    private void fireChange()
    {
        for (DataListener<T> l : dataListeners)
            l.dataUpdated(this);
    }
    
    /**
     * @return A CSV representation of the data's value (corresponding to the
     * value returned by getDataPoint()
     */
    public abstract String dataToCSV();
    
    /** @return The column names for the CSV representation givey by dataToCSV() */
    public abstract String getCSVHeader();
    
    /** Add a listener to be notified when this data is updated. */
    public final void addListener(DataListener<T> listener)
    {
        dataListeners.add(listener);
    }
    
    /** Remove a listener */
    public final void removeListener(DataListener<T> listener)
    {
        dataListeners.remove(listener);
    }
}
