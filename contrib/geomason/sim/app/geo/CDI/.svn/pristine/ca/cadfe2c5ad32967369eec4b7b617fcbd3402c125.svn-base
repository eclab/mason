package masoncsc.datawatcher;



/**
 *
 * @author Eric 'Siggy' Scott
 * @author Joey Harrison
 */
public class ScreenDataWriter implements DataListener
{
    public ScreenDataWriter() { };
    
    public ScreenDataWriter(DataWatcher source)
    {
        // XXX Move this out of constructor?
    	String s = source.getCSVHeader();
    	if (s != null)
            System.out.println();
    	
        source.addListener(this);
    }
    
    @Override
    public void dataUpdated(DataWatcher source)
    {
        System.out.println(source.dataToCSV());
    }
}
