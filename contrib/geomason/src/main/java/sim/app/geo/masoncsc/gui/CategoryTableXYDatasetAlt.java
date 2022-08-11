package sim.app.geo.masoncsc.gui;

/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates. 
 * Other names may be trademarks of their respective owners.]
 *
 * ---------------------------
 * CategoryTableXYDataset.java
 * ---------------------------
 * (C) Copyright 2004-2011, by Andreas Schroeder and Contributors.
 *
 * Original Author:  Andreas Schroeder;
 * Contributor(s):   David Gilbert (for Object Refinery Limited);
 *
 * Changes
 * -------
 * 31-Mar-2004 : Version 1 (AS);
 * 05-May-2004 : Now extends AbstractIntervalXYDataset (DG);
 * 15-Jul-2004 : Switched interval access method names (DG);
 * 18-Aug-2004 : Moved from org.jfree.data --> org.jfree.data.xy (DG);
 * 17-Nov-2004 : Updates required by changes to DomainInfo interface (DG);
 * 11-Jan-2005 : Removed deprecated code in preparation for 1.0.0 release (DG);
 * 05-Oct-2005 : Made the interval delegate a dataset change listener (DG);
 * 02-Feb-2007 : Removed author tags all over JFreeChart sources (DG);
 * 22-Apr-2008 : Implemented PublicCloneable, and fixed clone() method (DG);
 * 18-Oct-2011 : Fixed bug 3190615 - added clear() method (DG);
 *
 */


import org.jfree.data.DefaultKeyedValues2D;
import org.jfree.data.DomainInfo;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.IntervalXYDelegate;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.util.PublicCloneable;

/**
 * An implementation variant of the {@link TableXYDataset} where every series
 * shares the same x-values (required for generating stacked area charts).
 * This implementation uses a {@link DefaultKeyedValues2D} Object as backend
 * implementation and is hence more "category oriented" than the {@link
 * DefaultTableXYDataset} implementation.
 * <p>
 * This implementation provides no means to remove data items yet.
 * This is due to the lack of such facility in the DefaultKeyedValues2D class.
 * <p>
 * This class also implements the {@link IntervalXYDataset} interface, but this
 * implementation is provisional.
 * 
 * This Alt version was created to provide public access to the removeItem
 * function for the purpose of culling.
 */
public class CategoryTableXYDatasetAlt extends AbstractIntervalXYDataset
    implements TableXYDataset, IntervalXYDataset, DomainInfo,
    PublicCloneable {

    /**
     * The backing data structure.
     */
    private DefaultKeyedValues2D values;

    /** A delegate for controlling the interval width. */
    private IntervalXYDelegate intervalDelegate;

    /**
     * Creates a new empty CategoryTableXYDataset.
     */
    public CategoryTableXYDatasetAlt() {
        this.values = new DefaultKeyedValues2D(true);
        this.intervalDelegate = new IntervalXYDelegate(this);
        addChangeListener(this.intervalDelegate);
        }

    /**
     * Adds a data item to this dataset and sends a {@link DatasetChangeEvent}
     * to all registered listeners.
     *
     * @param x  the x value.
     * @param y  the y value.
     * @param seriesName  the name of the series to add the data item.
     */
    public void add(double x, double y, String seriesName) {
        add(new Double(x), new Double(y), seriesName, true);
        }

    /**
     * Adds a data item to this dataset and, if requested, sends a
     * {@link DatasetChangeEvent} to all registered listeners.
     *
     * @param x  the x value.
     * @param y  the y value.
     * @param seriesName  the name of the series to add the data item.
     * @param notify  notify listeners?
     */
    public void add(Number x, Number y, String seriesName, boolean notify) {
        this.values.addValue(y, (Comparable) x, seriesName);
        if (notify) {
            fireDatasetChanged();
            }
        }

    /**
     * Removes a value from the dataset.
     *
     * @param x  the x-value.
     * @param seriesName  the series name.
     */
    public void remove(double x, String seriesName) {
        remove(new Double(x), seriesName, true);
        }

    /**
     * Removes an item from the dataset.
     *
     * @param x  the x-value.
     * @param seriesName  the series name.
     * @param notify  notify listeners?
     */
    public void remove(Number x, String seriesName, boolean notify) {
        this.values.removeValue((Comparable) x, seriesName);
        if (notify) {
            fireDatasetChanged();
            }
        }
    
    /**
     * Remove the item at the given index from all series.
     * 
     * @param index index of the item.
     */
    public void removeItem(int index) {
        this.values.removeRow(index);
        }

    /**
     * Clears all data from the dataset and sends a {@link DatasetChangeEvent}
     * to all registered listeners.
     * 
     * @since 1.0.14
     */
    public void clear() {
        this.values.clear();
        fireDatasetChanged();
        }

    /**
     * Returns the number of series in the collection.
     *
     * @return The series count.
     */
    @Override
    public int getSeriesCount() {
        return this.values.getColumnCount();
        }

    /**
     * Returns the key for a series.
     *
     * @param series  the series index (zero-based).
     *
     * @return The key for a series.
     */
    @Override
    public Comparable getSeriesKey(int series) {
        return this.values.getColumnKey(series);
        }

    /**
     * Returns the number of x values in the dataset.
     *
     * @return The item count.
     */
    @Override
    public int getItemCount() {
        return this.values.getRowCount();
        }

    /**
     * Returns the number of items in the specified series.
     * Returns the same as {@link CategoryTableXYDataset#getItemCount()}.
     *
     * @param series  the series index (zero-based).
     *
     * @return The item count.
     */
    @Override
    public int getItemCount(int series) {
        return getItemCount();  // all series have the same number of items in
                                // this dataset
        }

    /**
     * Returns the x-value for the specified series and item.
     *
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     *
     * @return The value.
     */
    @Override
    public Number getX(int series, int item) {
        return (Number) this.values.getRowKey(item);
        }

    /**
     * Returns the starting X value for the specified series and item.
     *
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     *
     * @return The starting X value.
     */
    @Override
    public Number getStartX(int series, int item) {
        return this.intervalDelegate.getStartX(series, item);
        }

    /**
     * Returns the ending X value for the specified series and item.
     *
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     *
     * @return The ending X value.
     */
    @Override
    public Number getEndX(int series, int item) {
        return this.intervalDelegate.getEndX(series, item);
        }

    /**
     * Returns the y-value for the specified series and item.
     *
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     *
     * @return The y value (possibly <code>null</code>).
     */
    @Override
    public Number getY(int series, int item) {
        return this.values.getValue(item, series);
        }

    /**
     * Returns the starting Y value for the specified series and item.
     *
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     *
     * @return The starting Y value.
     */
    @Override
    public Number getStartY(int series, int item) {
        return getY(series, item);
        }

    /**
     * Returns the ending Y value for the specified series and item.
     *
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     *
     * @return The ending Y value.
     */
    @Override
    public Number getEndY(int series, int item) {
        return getY(series, item);
        }

    /**
     * Returns the minimum x-value in the dataset.
     *
     * @param includeInterval  a flag that determines whether or not the
     *                         x-interval is taken into account.
     *
     * @return The minimum value.
     */
    @Override
    public double getDomainLowerBound(boolean includeInterval) {
        return this.intervalDelegate.getDomainLowerBound(includeInterval);
        }

    /**
     * Returns the maximum x-value in the dataset.
     *
     * @param includeInterval  a flag that determines whether or not the
     *                         x-interval is taken into account.
     *
     * @return The maximum value.
     */
    @Override
    public double getDomainUpperBound(boolean includeInterval) {
        return this.intervalDelegate.getDomainUpperBound(includeInterval);
        }

    /**
     * Returns the range of the values in this dataset's domain.
     *
     * @param includeInterval  a flag that determines whether or not the
     *                         x-interval is taken into account.
     *
     * @return The range.
     */
    @Override
    public Range getDomainBounds(boolean includeInterval) {
        if (includeInterval) {
            return this.intervalDelegate.getDomainBounds(includeInterval);
            }
        else {
            return DatasetUtilities.iterateDomainBounds(this, includeInterval);
            }
        }

    /**
     * Returns the interval position factor.
     *
     * @return The interval position factor.
     */
    public double getIntervalPositionFactor() {
        return this.intervalDelegate.getIntervalPositionFactor();
        }

    /**
     * Sets the interval position factor. Must be between 0.0 and 1.0 inclusive.
     * If the factor is 0.5, the gap is in the middle of the x values. If it
     * is lesser than 0.5, the gap is farther to the left and if greater than
     * 0.5 it gets farther to the right.
     *
     * @param d  the new interval position factor.
     */
    public void setIntervalPositionFactor(double d) {
        this.intervalDelegate.setIntervalPositionFactor(d);
        fireDatasetChanged();
        }

    /**
     * Returns the full interval width.
     *
     * @return The interval width to use.
     */
    public double getIntervalWidth() {
        return this.intervalDelegate.getIntervalWidth();
        }

    /**
     * Sets the interval width to a fixed value, and sends a
     * {@link DatasetChangeEvent} to all registered listeners.
     *
     * @param d  the new interval width (must be > 0).
     */
    public void setIntervalWidth(double d) {
        this.intervalDelegate.setFixedIntervalWidth(d);
        fireDatasetChanged();
        }

    /**
     * Returns whether the interval width is automatically calculated or not.
     *
     * @return whether the width is automatically calculated or not.
     */
    public boolean isAutoWidth() {
        return this.intervalDelegate.isAutoWidth();
        }

    /**
     * Sets the flag that indicates whether the interval width is automatically
     * calculated or not.
     *
     * @param b  the flag.
     */
    public void setAutoWidth(boolean b) {
        this.intervalDelegate.setAutoWidth(b);
        fireDatasetChanged();
        }

    /**
     * Tests this dataset for equality with an arbitrary object.
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CategoryTableXYDatasetAlt)) {
            return false;
            }
        CategoryTableXYDatasetAlt that = (CategoryTableXYDatasetAlt) obj;
        if (!this.intervalDelegate.equals(that.intervalDelegate)) {
            return false;
            }
        if (!this.values.equals(that.values)) {
            return false;
            }
        return true;
        }

    /**
     * Returns an independent copy of this dataset.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException if there is some reason that cloning
     *     cannot be performed.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        CategoryTableXYDatasetAlt clone = (CategoryTableXYDatasetAlt) super.clone();
        clone.values = (DefaultKeyedValues2D) this.values.clone();
        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
            getIntervalPositionFactor());
        return clone;
        }

    }
