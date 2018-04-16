package masoncsc.datawatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * A DataWatcher that maintains an observed List.
 * 
 * @author Eric 'Siggy' Scott
 * @author Joey Harrison
 */
public abstract class ListDataWatcher<T> extends DataWatcher<List<T>>
{
    protected List<T> dataList = new ArrayList<T>();
    
    public ListDataWatcher() {
		super();
	}

	@Override
    public List<T> getDataPoint()
    {
        return dataList;
    }

    @Override
    public String dataToCSV()
    {
        if (dataList.isEmpty())
            return "null";
        else
        {
            StringBuilder bldr = new StringBuilder(dataList.get(0).toString());
            for (int i = 1; i < dataList.size(); i++)
                bldr.append(", ").append(dataList.get(i).toString());
            return bldr.toString();
        }
    }
}
