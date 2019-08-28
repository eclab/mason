/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package masonGlue;

import sim.display.Console;

/**
 * This is the main class just holding the main method to start the application.
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

		ForagingBeeGUI fbg2D = new ForagingBeeGUI();
		c = new Console(fbg2D);
		c.setVisible(true);
	}
}
