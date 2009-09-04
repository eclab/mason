package sim.util.media.chart;

import sim.util.IntBag;

/**
 * 
 * This is meant to accomodate an on-line algorithm for keeping a constant number of data points
 * from an on-going time series.
 * 
 * It only looks at the X values of the time series data points.
 * 
 * Implementations can assume the X values come in in increasing order.
 * 
 * 
 * <p>Now if a series holds the data points in a Bag, removing data points 
 * screws with the order, so the next time a data point needs to be 
 * removed, its index will be compromized.
 * 
 * The way this works is ... I get a bunch of X values and I return the indices of values that should be dropped.
 * It's then your job to delete the correct items and recompact the remaining data.
 * 
 * 
 * <p>Changing the API to include the actual data item, not just its x value
 * could be useful if one wants to choose based on similarity of Y values, not
 * just closeness of x values.  Another example would be an implementation that averages data
 *  (e.g. average 2 consecutive data points)!
 * 
 * 
 * <p>If a chart has multiple time series, does one cull each data series
 * separatelly? I.e. is this a chart global property or each series 
 * has its own?  
 * 
 * I kinda expect that when multiple series reside in 
 * the same chart, they're "synchronized" in the sense that they have
 * data points at the same moments in time. In this case, the cullers of 
 * those series do the same work, so one should be enough.
 * 
 * <br>The user is expected to add the series at the same time. I make no attempt to 
 * detect if series in the same chart have the same set of x values.
 * 
 * <br>The current implementation in TimeSeriesChartGenerator and the corresponding property inspector 
 * uses a single DacaCuller for all series.  In the future I guess I could give each series a clone of the 
 * data culling algorithm, if it turns out that stateful algoriths are needed.
 * 
 *  
 * <p>In order to improve the amortized time complexity, more than 1 data point
 * should be culled at a time.  E.g. as soon as you get 200 points, drop 100.
 * 
 * After each such operation there's a linear time
 * data shifting, so it pays off to delete multiple points at a time.
 * It also helps with the stuff during the operation, since one does not have to 
 * scan starting from the beginning for each data point to be deleted.
 * 
 * Heaps might be helpful while deciding which points to drop. The recompacting is still linear, 
 * but it should be very fast.
 * 
 * @author Gabriel Balan
 */
public interface DataCuller
    {
    boolean tooManyPoints(int currentPointCount);
    IntBag cull(double[] xValues, IntBag droppedIndices, boolean sortOutput);
    IntBag cull(double[] xValues, boolean sortOutput);

    /**
     * This must keep <code>size</code> elements and use the <code>droppedIndices<code> IntBag to store the indices of the 
     * <code>xValues.length - size</code> elements that do NOT survive the culling.
     * 
     * The implementators can expect the xValues to be sorted!
     * The implementators must be prepared to sort output bag if so requested.
     */
    IntBag cull(double[] xValues, int size, IntBag droppedIndices, boolean sortOutput);
    }
