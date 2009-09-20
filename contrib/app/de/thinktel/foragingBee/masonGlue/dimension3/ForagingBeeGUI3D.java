/**
 * 
 */
package de.thinktel.foragingBee.masonGlue.dimension3;

import java.awt.Color;

import sim.display.Controller;
import sim.display3d.Display3D;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import de.thinktel.foragingBee.masonGlue.ForagingBeeGUI;
import de.thinktel.foragingBee.masonGlue.ForagingHoneyBeeSimulation;

/**
 * Creation of this class for a 3D visual display of the simulation.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class ForagingBeeGUI3D extends ForagingBeeGUI {
	/**
	 * The display that is embedded in a window. The displays shows the visuals
	 * of the simulation.
	 */
	public Display3D display;

	/**
	 * Where to display the agents.
	 */
	ContinuousPortrayal3D vidPortrayal = new ContinuousPortrayal3D();

	public ForagingBeeGUI3D() {
		this(true);
	}

	public ForagingBeeGUI3D(boolean is3dMode) {
		super(new ForagingHoneyBeeSimulation3D(System.currentTimeMillis(),
				is3dMode));
	}

	/**
	 * Setting up the visuals.
	 */
	public void init(Controller c) {
		super.init(c);

		// make the displayer, using the maximum values of the simulation (if
		// not, some stretching may occur)
		display = new Display3D(ForagingHoneyBeeSimulation.WIDTH,
				ForagingHoneyBeeSimulation.HEIGHT, this, 1);

		display.attach(vidPortrayal, "Agents");

		display.translate(-ForagingHoneyBeeSimulation.MIDDLE_WIDTH,
				-ForagingHoneyBeeSimulation.MIDDLE_HEIGHT,
				-ForagingHoneyBeeSimulation.MIDDLE_LENGTH);

		// now let's scale it so it fits inside a 1x1x1 cube centered at the
		// origin. We don't
		// have to, but it'll look nicer.
		display.scale(1.0 / Math.max(ForagingHoneyBeeSimulation.WIDTH,
				ForagingHoneyBeeSimulation.HEIGHT));

		displayFrame = display.createFrame();
		displayFrame.setTitle("Honey bee playground");
		c.registerFrame(displayFrame); // register the frame so it appears in
		// the "Display" list
		displayFrame.setVisible(true);

		display.setInterval(5);

		initGraphDisplays(c);
	}

	/**
	 * Set up the displays.
	 */
	public void setupPortrayals() {
		ForagingHoneyBeeSimulation3D beeSimulation = (ForagingHoneyBeeSimulation3D) state;
		// tell the portrayals what to portray and how to portray them
		vidPortrayal.setField(beeSimulation.environment);
		// reschedule the displayer
		display.reset();
		display.setBackdrop(Color.darkGray);

		// redraw the display
		display.repaint();
	}

	/**
	 * Tidying up when simulation is to be quit.
	 */
	public void quit() {
		super.quit();

		display = null;
	}
}