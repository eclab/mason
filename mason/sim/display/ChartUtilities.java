package sim.display;

import sim.util.media.chart.*;
import sim.util.*;
import javax.swing.*;
import sim.engine.*;
import org.jfree.data.xy.*;
import java.util.*;

/**
ChartUtilities.java

A collection of static methods designed to make it easy for you to set up charts as displays.

<p>In your init() method, you will create a chart by calling one of the build...Generator(...) methods.
Then you will create one or more series by calling one of the add...Series(...) methods.

<p>Store the series you created in instance variables.

<p>Finally, in your start() and load() methods (perhaps in setupPortrayals() if both of them call that method),
you will schedule each of the series by calling the schedule...Series(...) methods.

<p>The ChartUtilities schedule...Series(...) methods expect a "value provider" object which implements
one of the ChartUtilities.Provides... interfaces.  This object is called each timestep to add new
data (for ProvidesDoubles) or replace all the data (for the other Provides interfaces) in the chart before
the chart is redrawn.  You can pass in null for this object, which prevents the data from ever being updated;
in this case it's up to you to update the data on your own.
*/

public class ChartUtilities
	{
	/** This class provides arrays of doubles to chart, or provides null if the current charted values shouldn't be changed. */
	public interface ProvidesDoubles { public double[] provide(); }
	/** This class provides arrays of doubles to chart, with associated labels, or provides null if the current charted values shouldn't be changed. */
	public interface ProvidesDoublesAndLabels extends ProvidesDoubles { public String[] provideLabels(); }
	/** This class provides two double arrays (that is, an array of the form double[2][]), which are
		the same length, which represent the x and y coordinates of points, or else 
		provides null if the current charted values shouldn't be changed. */
	public interface ProvidesDoubleDoubles { public double[][] provide(); }
	/** This class provides arrays of Objects to chart, or provides null if the current charted values shouldn't be changed. 
		The array of objects will be sorted and counted, and the counts will be used to describe a distribution such
		as for a pie chart.  The labels of the pie chart will be drawn from the object strings. */
	public interface ProvidesObjects { public Object[] provide(); }
	/** This class provides Collections of Objects to chart, or provides null if the current charted values shouldn't be changed. 
		The array of objects will be sorted and counted, and the counts will be used to describe a distribution such
		as for a pie chart.  The labels of the pie chart will be drawn from the object strings. */
	public interface ProvidesCollection { public Collection provide(); }
	
	/** Builds a TimeSeriesChartGenerator not attached to any MASON simulation. */
	public static TimeSeriesChartGenerator buildTimeSeriesChartGenerator(String title, String domainAxisLabel)
		{
		TimeSeriesChartGenerator chart = new TimeSeriesChartGenerator();
		if (title == null) title = "";
		chart.setTitle(title);
		if (domainAxisLabel == null) domainAxisLabel = "";
		chart.setXAxisLabel(domainAxisLabel);
		return chart;		
		}

	/** Builds a TimeSeriesChartGenerator and attaches it as a display in a MASON simulation. */
	public static TimeSeriesChartGenerator buildTimeSeriesChartGenerator(GUIState state, String title, String domainAxisLabel)
		{
		TimeSeriesChartGenerator chart = buildTimeSeriesChartGenerator(title, domainAxisLabel);
		JFrame frame = chart.createFrame();
		frame.setVisible(true);
		frame.pack();
		state.controller.registerFrame(frame);
		return chart;
		}
	
	/** Adds a series to the TimeSeriesChartGenerator */
	public static TimeSeriesAttributes addSeries(final TimeSeriesChartGenerator chart, String seriesName)
		{
		final XYSeries series = new XYSeries(seriesName /* not sure if this has to be unique any more */, false);
		return (TimeSeriesAttributes)(chart.addSeries(series, null));
		}

// TODO: figure out how to extract or set the (non-series) arrays in stuff like
//        Histograms so you can serialize them.



	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final TimeSeriesAttributes attributes, 
										 final Valuable valueProvider)
		{
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			final XYSeries series = attributes.getSeries();
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null) 
						series.add(x, valueProvider.doubleValue(), true);
					attributes.possiblyCull();
					attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}
		
	/** Builds a HistogramGenerator not attached to any MASON simulation. */
	public static HistogramGenerator buildHistogramGenerator(String title, String rangeAxisLabel)
		{
		HistogramGenerator chart = new HistogramGenerator();
		if (title == null) title = "";
		chart.setTitle(title);
		if (rangeAxisLabel == null) rangeAxisLabel = "";
		chart.setYAxisLabel(rangeAxisLabel);
		return chart;		
		}

	/** Builds a HistogramGenerator and attaches it as a display in a MASON simulation. */
	public static HistogramGenerator buildHistogramGenerator(GUIState state, String title, String rangeAxisLabel)
		{
		HistogramGenerator chart = buildHistogramGenerator(title, rangeAxisLabel);
		JFrame frame = chart.createFrame();
		frame.setVisible(true);
		frame.pack();
		state.controller.registerFrame(frame);
		return chart;
		}
	
	/** Adds a series to the HistogramGenerator.  You also provide the default number of historgram bins. */
	public static HistogramSeriesAttributes addSeries(final HistogramGenerator chart, String seriesName, final int bins)
		{
		return (HistogramSeriesAttributes)(chart.addSeries(new double[0], bins, seriesName, null));
		}
	
	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final HistogramSeriesAttributes attributes,
											final ProvidesDoubles valueProvider)
		{	
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null) 
						{
						double[] vals = valueProvider.provide();
						if (vals != null)
							attributes.setValues(vals);
						}
					attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}

	/** Builds a ScatterPlotGenerator not attached to any MASON simulation. */
	public static ScatterPlotGenerator buildScatterPlotGenerator(String title, String rangeAxisLabel, String domainAxisLabel)
		{
		ScatterPlotGenerator chart = new ScatterPlotGenerator();
		if (title == null) title = "";
		chart.setTitle(title);
		if (rangeAxisLabel == null) rangeAxisLabel = "";
		chart.setYAxisLabel(rangeAxisLabel);
		if (domainAxisLabel == null) domainAxisLabel = "";
		chart.setXAxisLabel(domainAxisLabel);
		return chart;		
		}

	/** Builds a ScatterPlotGenerator and attaches it as a display in a MASON simulation. */
	public static ScatterPlotGenerator buildScatterPlotGenerator(GUIState state, String title, String rangeAxisLabel, String domainAxisLabel)
		{
		ScatterPlotGenerator chart = buildScatterPlotGenerator(title, rangeAxisLabel, domainAxisLabel);
		JFrame frame = chart.createFrame();
		frame.setVisible(true);
		frame.pack();
		state.controller.registerFrame(frame);
		return chart;
		}
	
	/** Adds a series to the ScatterPlotGenerator. */
	public static ScatterPlotSeriesAttributes addSeries(final ScatterPlotGenerator chart, String seriesName)
		{
		return (ScatterPlotSeriesAttributes)(chart.addSeries(new double[0][0], seriesName, null));
		}
		
	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final ScatterPlotSeriesAttributes attributes,
											final ProvidesDoubleDoubles valueProvider)
		{
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null)
						{
						double[][] vals = valueProvider.provide();
						if (vals != null)
							attributes.setValues(vals);
						}
						attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}


	/** Builds a PieChartGenerator not attached to any MASON simulation. */
	public static PieChartGenerator buildPieChartGenerator(String title)
		{
		PieChartGenerator chart = new PieChartGenerator();
		if (title == null) title = "";
		chart.setTitle(title);
		return chart;		
		}

	/** Builds a PieChartGenerator and attaches it as a display in a MASON simulation. */
	public static PieChartGenerator buildPieChartGenerator(GUIState state, String title)
		{
		PieChartGenerator chart = buildPieChartGenerator(title);
		JFrame frame = chart.createFrame();
		frame.setVisible(true);
		frame.pack();
		state.controller.registerFrame(frame);
		return chart;
		}
	
	/** Adds a series to the PieChartGenerator. */
	public static PieChartSeriesAttributes addSeries(final PieChartGenerator chart, String seriesName)
		{
		return (PieChartSeriesAttributes)(chart.addSeries(new double[0], new String[0], seriesName, null));
		}
	
	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final PieChartSeriesAttributes attributes, 
								 	  	   final ProvidesDoublesAndLabels valueProvider)
		{
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null)
						{
						double[] vals = valueProvider.provide();
						String[] labels = valueProvider.provideLabels();
						if (vals != null)
							{
							attributes.setValues(vals);
							attributes.setLabels(labels);
							}
						}
						attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}

	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final PieChartSeriesAttributes attributes, 
								 	  	   final ProvidesObjects valueProvider)
		{
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null)
						{
						Object[] vals = valueProvider.provide();
						if (vals != null)
							attributes.setElements(vals);
						}
						attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}

	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final PieChartSeriesAttributes attributes, 
								 	  	   final ProvidesCollection valueProvider)
		{
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null)
						{
						Collection vals = valueProvider.provide();
						if (vals != null)
							attributes.setElements(new ArrayList(vals));  // just in case he tries to change the Collection before it's updated
						}
						attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}


	/** Builds a BarChartGenerator not attached to any MASON simulation. */
	public static BarChartGenerator buildBarChartGenerator(String title)
		{
		BarChartGenerator chart = new BarChartGenerator();
		if (title == null) title = "";
		chart.setTitle(title);
		return chart;		
		}

	/** Builds a BarChartGenerator and attaches it as a display in a MASON simulation. */
	public static BarChartGenerator buildBarChartGenerator(GUIState state, String title)
		{
		BarChartGenerator chart = buildBarChartGenerator(title);
		JFrame frame = chart.createFrame();
		frame.setVisible(true);
		frame.pack();
		state.controller.registerFrame(frame);
		return chart;
		}
	
	/** Adds a series to the BarChartGenerator. */
	public static BarChartSeriesAttributes addSeries(final BarChartGenerator chart, String seriesName)
		{
		return (BarChartSeriesAttributes)(chart.addSeries(new double[0], new String[0], seriesName, null));
		}
	
	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final BarChartSeriesAttributes attributes, 
										   final ProvidesDoublesAndLabels valueProvider)
		{
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null)
						{
						double[] vals = valueProvider.provide();
						String[] labels = valueProvider.provideLabels();
						if (vals != null)
							{
							attributes.setValues(vals);
							attributes.setLabels(labels);
							}
						}
						attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}

	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final BarChartSeriesAttributes attributes, 
										   final ProvidesObjects valueProvider)
		{
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null)
						{
						Object[] vals = valueProvider.provide();
						if (vals != null)
							attributes.setElements(vals);
						}
						attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}

	/** Schedules a series with the MASON simulation.  You specify a value provider to give series data each timestep.  This value provider can be null, in which case no data will ever be updated -- you'd have to update it on your own as you saw fit. */
	public static Stoppable scheduleSeries(final GUIState state, final BarChartSeriesAttributes attributes, 
										   final ProvidesCollection valueProvider)
		{
		return state.scheduleRepeatingImmediatelyAfter(new Steppable()
			{
			double last = state.state.schedule.BEFORE_SIMULATION;
			public void step(SimState state)
				{
				double x = state.schedule.getTime();
				if (x > last && x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					{
					last = x;
					if (valueProvider != null)
						{
						Collection vals = valueProvider.provide();
						if (vals != null)
							attributes.setElements(new ArrayList(vals));  // just in case he tries to change the Collection before it's updated
						}
					attributes.getGenerator().updateChartLater(state.schedule.getSteps());
					}
				}
			});
		}
	}
	