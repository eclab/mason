/**
 * 
 */
package de.thinktel.utils;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import sim.display.GUIState;
import sim.util.media.chart.TimeSeriesChartGenerator;

/**
 * A class holding all information needed to create and display information in a
 * time series chart.
 * 
 * @author hoehne
 * 
 */
public class TimeSeriesChartInformation {
	/**
	 * The {@link TimeSeriesChartGenerator} that shows the number of foraging
	 * bees and their food sources they are locked on.
	 */
	TimeSeriesChartGenerator chart;
	/**
	 * The data sets for the display.
	 */
	XYSeries series[];

	/**
	 * The timer for a periodic display.
	 */
	Thread chartUpdateTimer = null;

	/**
	 * The simulation the chart will be displayed in.
	 */
	GUIState simulation;

	/**
	 * The frame holding the chart.
	 */
	JFrame frame;

	/**
	 * The constructor that takes the current simulation GUI as an argument.
	 * 
	 * @param simulation
	 *            The current simulation GUI.
	 */
	public TimeSeriesChartInformation(GUIState simulation) {
		this.simulation = simulation;
	}

	/**
	 * Create all necessary structures and a {@link JFrame} where the chart is
	 * displayed.
	 * 
	 * @param title
	 *            The title of the chart.
	 * @param xAxis
	 *            The label on the x-axis.
	 * @param yAxis
	 *            The label on the y-axis.
	 * @return The frame with the chart.
	 */
	public JFrame create(String title, String xAxis, String yAxis) {
		chart = new TimeSeriesChartGenerator();
		chart.setTitle(title);
		chart.setDomainAxisLabel(xAxis);
		chart.setRangeAxisLabel(yAxis);
		frame = chart.createFrame(simulation);

		return frame;
	}

	/**
	 * Return the chart.
	 * 
	 * @return the chart
	 */
	public final TimeSeriesChartGenerator getChart() {
		return chart;
	}

	/**
	 * Get the data series.
	 * 
	 * @return the series
	 */
	public final XYSeries[] getSeries() {
		return series;
	}

	/**
	 * Set the data series.
	 * 
	 * @param series
	 *            the series to set
	 */
	public final void setSeries(XYSeries[] series) {
		this.series = series;
	}

	public void startTimer(final long milliseconds) {
		if (chartUpdateTimer == null)
			chartUpdateTimer = sim.util.Utilities.doLater(milliseconds,
					new Runnable() {
						public void run() {
							if (chart != null)
								chart.update();
							chartUpdateTimer = null; // reset the timer
						}
					});
	}
}
