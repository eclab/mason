package masoncsc.datawatcher;

import java.util.EventListener;

/**
 * Listens to a DataWatcher and fires a method when the data is updated.
 * 
 * @param <T> Type of data provided by the DataWatcher we'll attach to.
 * 
 * @author Eric 'Siggy' Scott
 * @author Joey Harrison
 */
public interface DataListener<T> extends EventListener
{
    public void dataUpdated(DataWatcher<T> source);
}
