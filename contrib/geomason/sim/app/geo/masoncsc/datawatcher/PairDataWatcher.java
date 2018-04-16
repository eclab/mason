package masoncsc.datawatcher;

import masoncsc.util.Pair;

/**
 * A DataWatcher that maintains an observation of the form (x,y).
 * 
 * @author Eric 'Siggy' Scott
 * @author Joey Harrison
 */
public abstract class PairDataWatcher<X, Y> extends DataWatcher<Pair<X, Y>>
{
    protected Pair<X, Y> dataPoint;
    
    public PairDataWatcher() { super(); };
    
    @Override
    public Pair<X, Y> getDataPoint()
    {
        return dataPoint;
    }

    @Override
    public String dataToCSV()
    {
        if (dataPoint == null)
            return "null";
        return String.format("%s, %s", dataPoint.getLeft().toString(), dataPoint.getRight().toString());
    }
}
