/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.foragingBee.masonGlue;

import java.util.ListIterator;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import sim.display.Controller;
import sim.display.GUIState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.media.chart.TimeSeriesAttributes;
import sim.util.media.chart.TimeSeriesChartGenerator;
import de.thinktel.foragingBee.simulation.FoodSource;
import de.thinktel.foragingBee.simulation.Hive;
import de.thinktel.utils.TimeSeriesChartInformation;

/**
 * This class provides an interface for a 2D display of the simulation results.
 * This is a glue class that is gluing MASON with the basic simulation. An
 * instance of class {@link SimState} holds the simulation with all simulation
 * parameters. This class usually takes an instance of
 * {@link ForagingHoneyBeeSimulation} or more likely one of the subclasses as an
 * argument.
 * <p>
 * Changes:
 * <ul>
 * <li>20090827: Added the windows for displaying the amount of foraging bees
 * and the amount of honey in the hives (one hive at the moment).</li>
 * <li>20090828: Changed this class to a abstract to allow 2D and 3D subclass
 * visualization.</li>
 * </ul>
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */

public abstract class ForagingBeeGUI extends GUIState {
	/**
	 * The time series diagram for the foraging bees locked on the sources.
	 */
	TimeSeriesChartInformation tsForagingBees;

	/**
	 * The time series diagram for the honey amount of the hives.
	 */
	TimeSeriesChartInformation tsHives;

	/**
	 * The time series diagram for the nectar concentration of the food sources.
	 */
	TimeSeriesChartInformation tsFoodSources;

	/**
	 * The food sources in the simulation, used for plotting concentrations of
	 * nectar and the number of bees locked on this source.
	 */
	FoodSource foodSources[];

	/**
	 * The hives in the simulation used for plotting the amount of nectar in
	 * each hive.
	 */
	Hive hives[];

	/**
	 * The Java frame (window) where the simulation is displayed in.
	 */
	public JFrame displayFrame;

	/**
	 * A constructor that creates a instance with the current system time as an
	 * argument. The holds information about the simulation but does not
	 * visualize. Visualization is implemented by the subclasses.
	 * <p>
	 * This class creates the visuals so it calls
	 * {@link ForagingHoneyBeeSimulation#prepareSimulation()} to create all
	 * objects that will be displayed in the model inspector user interface.
	 */
	public ForagingBeeGUI(ForagingHoneyBeeSimulation sim) {
		super(sim);
		sim.prepareSimulation();
	}

	/**
	 * A constructor that takes a {@link SimState} instance as an argument to
	 * create the visuals.
	 * 
	 * @param state
	 */
	public ForagingBeeGUI(SimState state) {
		super(state);
	}

	/**
	 * Return the {@link GUIState#state} object so the inspector can inspect
	 * this object.
	 * 
	 * @return Return the {@link GUIState#state} object.
	 */
	public Object getSimulationInspectedObject() {
		return state;
	}

	/**
	 * The name of the simulation.
	 * 
	 * @return The string containing the name of the simulation.
	 */
	public static String getName() {
		return "Foraging Bee Simulation in 2D";
	}

	/**
	 * Set up the displays.
	 */
	public abstract void setupPortrayals();

	/**
	 * Initializing structures.
	 */
	public void start() {
		super.start();
		setupPortrayals();

		int i;
		ForagingHoneyBeeSimulation bs = (ForagingHoneyBeeSimulation) state;
		foodSources = new FoodSource[bs.foodSources.size()];
		hives = new Hive[bs.hives.size()];

		{
			TimeSeriesChartGenerator chart = tsForagingBees.getChart();
			chart.removeAllSeries();
			XYSeries series[] = new XYSeries[foodSources.length];
			tsForagingBees.setSeries(series);

			ListIterator<FoodSource> li = bs.foodSources.listIterator();
			for (i = 0; i < series.length; i++) {
				foodSources[i] = li.next();
				series[i] = new XYSeries("Food source" + (i + 1), false);
				chart.addSeries(series[i], null);

				// get the series for the current index
				TimeSeriesAttributes tsa = (TimeSeriesAttributes) chart
						.getSeriesAttributes(i);

				tsa.setStrokeColor(foodSources[i].getVisualizationObject()
						.getColor());
			}
		}

		{
			TimeSeriesChartGenerator chart = tsFoodSources.getChart();
			chart.removeAllSeries();
			XYSeries series[] = new XYSeries[foodSources.length];
			tsFoodSources.setSeries(series);

			// ListIterator<FoodSource> li = bs.foodSources.listIterator();
			for (i = 0; i < series.length; i++) {
				// foodSources[i] = li.next();
				series[i] = new XYSeries("Concentration food source" + (i + 1),
						false);
				chart.addSeries(series[i], null);

				// get the series for the current index
				TimeSeriesAttributes tsa = (TimeSeriesAttributes) chart
						.getSeriesAttributes(i);

				tsa.setStrokeColor(foodSources[i].getVisualizationObject()
						.getColor());
			}
		}

		{
			TimeSeriesChartGenerator chart = tsHives.getChart();
			chart.removeAllSeries();
			XYSeries series[] = new XYSeries[hives.length];
			tsHives.setSeries(series);

			ListIterator<Hive> li = bs.hives.listIterator();
			for (i = 0; i < series.length; i++) {
				hives[i] = li.next();
				series[i] = new XYSeries("Hive" + (i + 1), false);
				chart.addSeries(series[i], null);

				// get the series for the current index
				TimeSeriesAttributes tsa = (TimeSeriesAttributes) chart
						.getSeriesAttributes(i);

				tsa.setStrokeColor(foodSources[i].getVisualizationObject()
						.getColor());
			}
		}

		scheduleImmediateRepeat(true, new Steppable() {
			public void step(SimState state) {
				// at this stage we're adding data to our chart. We
				// need an X value and a Y value. Typically the X
				// value is the schedule's timestamp. The Y value
				// is whatever data you're extracting from your
				// simulation. For purposes of illustration, let's
				// extract the number of steps from the schedule and
				// run it through a sin wave.

				double t = state.schedule.time();

				// now add the data
				if (t >= Schedule.EPOCH && t < Schedule.AFTER_SIMULATION) {
					int i;

					{
						XYSeries series[] = tsForagingBees.getSeries();
						for (i = 0; i < series.length; i++) {
							series[i].add(t, foodSources[i].getBeeCount(),
									false);
						}

						tsForagingBees.startTimer(1000);
					}

					{
						XYSeries series[] = tsFoodSources.getSeries();
						for (i = 0; i < series.length; i++) {
							series[i].add(t, foodSources[i].getConcentration(),
									false);
						}

						tsFoodSources.startTimer(1000);
					}

					{
						XYSeries series[] = tsHives.getSeries();
						for (i = 0; i < series.length; i++) {
							series[i].add(t, hives[i].getHoneyAmount(), false);
						}

						tsHives.startTimer(1000);
					}
				}
			}
		});
	}

	/**
	 * Called by the Console when the user is loading in a new state from a
	 * checkpoint.
	 * 
	 * @param state
	 *            The current simulation.
	 */
	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}

	/**
	 * Initialize the graphical displays.
	 * 
	 * @param c
	 */
	public void initGraphDisplays(Controller c) {
		{
			tsForagingBees = new TimeSeriesChartInformation(this);
			JFrame frame = tsForagingBees.create("Number of foraging bees",
					"Bees", "Time");
			// perhaps you might move the chart to where you like.
			frame.setVisible(true);
			frame.pack();
			c.registerFrame(frame);
		}

		{
			tsFoodSources = new TimeSeriesChartInformation(this);
			JFrame frame = tsFoodSources.create(
					"Concentration nectar food sources", "Concentration",
					"Time");
			// perhaps you might move the chart to where you like.
			frame.setVisible(true);
			frame.pack();
			c.registerFrame(frame);
		}

		{
			tsHives = new TimeSeriesChartInformation(this);
			JFrame frame = tsHives.create("Amount of honey in the hives",
					"Hives", "Time");
			// perhaps you might move the chart to where you like.
			frame.setVisible(true);
			frame.pack();
			c.registerFrame(frame);
		}

		// the console automatically moves itself to the right of all
		// of its registered frames -- you might wish to rearrange the
		// location of all the windows, including the console, at this
		// point in time....

	}

	/**
	 * Tidying up when simulation is to be quit.
	 */
	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
	}
}
