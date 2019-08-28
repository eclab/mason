/*
 Hypercycle simulation. Copyright by Jšrg Hšhne.
 For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.hyperCycle;

import java.awt.Color;

import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.grid.FastValueGridPortrayal2D;

/**
 * This class provides an interface for a 2D display of the simulation results.
 * This class has its roots in the Life example.
 * 
 * @author hoehne
 * 
 */

public class HyperCycleGUI extends GUIState {
	public Display2D display;
	public JFrame displayFrame;
	FastValueGridPortrayal2D gridPortrayal = new FastValueGridPortrayal2D();

	public HyperCycleGUI() {
		super(new HyperCycleSimulation(System.currentTimeMillis(),
				new HyperCycleParameters()));
	}

	public HyperCycleGUI(SimState state) {
		super(state);
	}

	public static String getName() {
		return "Hyper Cycle Simulation 2D";
	}

	public static Object getInfo() {
		return "<H2>Hyper Cycle Simulation in 2D</H2>";
	}

	public void setupPortrayals() {
		HyperCycleSimulation hcsState = (HyperCycleSimulation) state;
		HyperCycleParameters p = hcsState.getParameters();
		// tell the portrayals what to portray and how to portray them
		gridPortrayal.setField(hcsState.grid);
		gridPortrayal.setMap(new sim.util.gui.SimpleColorMap(p.colorTable));
	}

	/**
	 * This method overrides the start method.
	 */
	public void start() {
		super.start();

		setupPortrayals(); // set up our portrayals
		display.reset(); // reschedule the displayer
		display.repaint(); // redraw the display
	}


	public void init(Controller c) {
		super.init(c);

		// Make the Display2D. We'll have it display stuff later.
		HyperCycleSimulation hcsState = (HyperCycleSimulation) state;
		HyperCycleParameters p = hcsState.getParameters();

		display = new Display2D(p.getWidth() * 2, p.getHeight() * 2, this, 1);
		displayFrame = display.createFrame();
		c.registerFrame(displayFrame); // register the frame so it appears in
		// the "Display" list
		displayFrame.setVisible(true);

		// attach the portrayals
		display.attach(gridPortrayal, "HyperCycle");

		// specify the backdrop color -- what gets painted behind the displays
		display.setBackdrop(Color.black);
	}


	public void load(SimState state) {
		super.load(state);
		setupPortrayals(); // we now have new grids. Set up the portrayals to
		// reflect this
		display.reset(); // reschedule the displayer
		display.repaint(); // redraw the display
	}
}
