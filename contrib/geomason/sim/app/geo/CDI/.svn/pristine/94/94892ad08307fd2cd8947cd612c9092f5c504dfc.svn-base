/*
 * YScalableXSeries.java
 *
 * $Id: YScalableXYSeries.java 1588 2013-02-06 17:14:12Z escott8 $
 */

package migration.util;

import org.jfree.data.xy.XYDataItem;

/** A sequence of (X,Y) points that 
 */
public class YScalableXYSeries extends org.jfree.data.xy.XYSeries
{
    private static final long serialVersionUID = 1L;
    double scale = 1;

    public YScalableXYSeries(String name) {
        super(name);
    }

    public YScalableXYSeries(String name, double scale) {
        this(name);
        this.scale = scale;
    }

    @Override
    public void add(double x, double y, boolean notify) {
        super.add(x, y * scale, notify);
    }

    public double getScale()
    {
        return scale;
    }

    public void setScale(double newScale) {
        setScale(newScale, true);
    }

    public void setScale(double newScale, boolean notify) {
        if (newScale == 0)
        {
            return; //not a valid scale
        }
        double newScaleOverOldScale = newScale / scale;
        scale = newScale;
        int nPoints = getItemCount();
        for (int i = nPoints - 1; i >= 0; i--)
        {
            XYDataItem item = getDataItem(i);
            item.setY(item.getY().doubleValue() * newScaleOverOldScale);
        }
        if (notify)
        {
            fireSeriesChanged();
        }
    }
}
