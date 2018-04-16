package masoncsc.datawatcher;



/**
 * A DataListener that keeps a historical record of measurements made by a
 * DataWatcher.  Used, for instance, to store a time series for display.
 * 
 * @param <T> Type of data provided by the DataWatcher we'll attach to.
 * @param <U> The historical aggregate of the data.  ex. You might set this to
 * List&lt;T&gt;
 * @author Eric `Siggy Scott
 * @author Joey Harrison
 */
public interface DataStore<T, U> extends DataListener<T>
{
    public U getData();
    public void clear();
}
