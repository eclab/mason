/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.foragingBee.masonGlue;

import java.util.Random;

import javax.swing.JOptionPane;

import sim.display.Console;
import sim.display.GUIState;
import de.thinktel.foragingBee.masonGlue.dimension2.ForagingBeeGUI2D;
import de.thinktel.foragingBee.masonGlue.dimension3.ForagingBeeGUI3D;

/**
 * This is the main class just holding the main method to start the application.
 * <p>
 * Changes:
 * <ul>
 * <li>20090920: Added the the selection of the simulation mode.</li>
 * </ul>
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class MainBeeForaging {
	/**
	 * No arguments are evaluated by this application.
	 * 
	 * @param args
	 *            No argument will be evaluated.
	 */
	public static void main(String[] args) {
		Console c;

		// Custom button text
		Object[] options = { "Surprise me!", "3D mode", "2D mode", "Abort" };
		int n = JOptionPane.showOptionDialog(null,
				"Please choose the simulation mode.",
				"Simulation mode selection.", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

		GUIState gui = null;
		switch (n) {
		case 0:
			gui = new Random(System.currentTimeMillis()).nextBoolean() ? new ForagingBeeGUI2D()
					: new ForagingBeeGUI3D();
			break;
		case 1:
			gui = new ForagingBeeGUI3D();
			break;
		case 2:
			gui = new ForagingBeeGUI2D();
			break;
		}

		if (gui != null) {
			c = new Console(gui);
			c.setVisible(true);
		}
	}
}
