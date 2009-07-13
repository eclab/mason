/*
 Hypercycle simulation. Copyright by Jšrg Hšhne.
 For suggestions or questions email me at hoehne@thinktel.de
 */

package hyperCycle;

import sim.display.Console;

/**
 * This is the main class just holding the main method to start the application.
 * In the main method are some code options so choose wisely.
 * 
 * @author hoehne
 * 
 */
public class MainHyperCycle {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// the option of the display mode, 0 = 3D, 1 = 2D
		int option = 0;
		Console c;

		switch (option) {
		// start the simulation with in 3D
		case 0:
			HyperCycle3DWithUI hyperCycle3D = new HyperCycle3DWithUI(
					new HyperCycleSimulation(System.currentTimeMillis(),
							new HyperCycleParameters()),
					// HyperCycle3DWithUI.TILE);
					HyperCycle3DWithUI.NOZ);
			// HyperCycle3DWithUI.MESH);

			c = new Console(hyperCycle3D);
			c.setVisible(true);
			break;

		// start the simulation in 2D
		case 1:
			HyperCycleGUI hyperCycle2D = new HyperCycleGUI();
			c = new Console(hyperCycle2D);
			c.setVisible(true);
			break;
		}
	}
}
