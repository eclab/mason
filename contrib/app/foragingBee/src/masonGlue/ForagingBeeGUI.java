/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package masonGlue;

import java.awt.Color;

import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;

/**
 * This class provides an interface for a 2D display of the simulation results.
 * This is a glue class that is gluing MASON with the basic simulation. An
 * instance of class {@link SimState} holds the simulation with all simulation
 * parameters. This class usually takes a
 * {@link ParametrizedForagingHoneyBeeSimulation} or
 * {@link ForagingHoneyBeeSimulation} instance.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */

public class ForagingBeeGUI extends GUIState {
	/**
	 * The display that is embedded in a window. The displays shows the visuals
	 * of the simulation.
	 */
	public Display2D display;

	/**
	 * The Java frame (window) where the simulation is displayed in.
	 */
	public JFrame displayFrame;

	/**
	 * Where to display the agents.
	 */
	ContinuousPortrayal2D vidPortrayal = new ContinuousPortrayal2D();

	/**
	 * A constructor that creates a
	 * {@link ParametrizedForagingHoneyBeeSimulation} instance with the current
	 * system time as an argument. The
	 * {@link ParametrizedForagingHoneyBeeSimulation} holds information about
	 * the simulation but does no visualization.
	 * <p>
	 * This class creates the visuals.
	 */
	public ForagingBeeGUI() {
		super(new ParametrizedForagingHoneyBeeSimulation(System
				.currentTimeMillis()));
	}

	/**
	 * A constructor that takes a {@link SimState} instance as an arguement to
	 * create the visuals.
	 * 
	 * @param state
	 */
	public ForagingBeeGUI(SimState state) {
		super(state);
	}

	/**
	 * Return the {@link GUIState#state} object so the inspector can inspect this
	 * object.
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
	public void setupPortrayals() {
		ForagingHoneyBeeSimulation beeSimulation = (ForagingHoneyBeeSimulation) state;
		// tell the portrayals what to portray and how to portray them
		vidPortrayal.setField(beeSimulation.environment);
		// reschedule the displayer
		display.reset();
		display.setBackdrop(Color.black);

		// redraw the display
		display.repaint();
	}

	/**
	 * Initializing structures.
	 */
	public void start() {
		super.start();
		setupPortrayals();
	}

	/**
	 * Called by the Console when the user is loading in a new state from a
	 * checkpoint.
	 * 
	 * @param state The current simulation.
	 */
	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}

	/**
	 * Setting up the visuals.
	 */
	public void init(Controller c) {
		super.init(c);

		// make the displayer, using the maximum values of the simulation (if
		// not, some stretching may occur)
		display = new Display2D(ForagingHoneyBeeSimulation.WIDTH,
				ForagingHoneyBeeSimulation.HEIGHT, this, 1);

		displayFrame = display.createFrame();
		displayFrame.setTitle("Honey bee playground");
		c.registerFrame(displayFrame); // register the frame so it appears in
		// the "Display" list
		displayFrame.setVisible(true);
		display.attach(vidPortrayal, "Agents");

		//display.setInterval(5);
	}

	/**
	 * Tidying up when simulation is to be quit.
	 */
	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;
	}
}
