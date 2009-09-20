/*
 Hypercycle simulation. Copyright by Jšrg Hšhne.
 For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.hyperCycle;

import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.GUIState;
import sim.display3d.Display3D;
import sim.engine.SimState;
import sim.portrayal3d.grid.ValueGrid2DPortrayal3D;
import sim.portrayal3d.grid.quad.MeshPortrayal;
import sim.portrayal3d.grid.quad.QuadPortrayal;
import sim.portrayal3d.grid.quad.TilePortrayal;
import sim.util.gui.SimpleColorMap;

/**
 * This class visualize the hyper cycle simulation in 3D. This class was
 * retrieved by modifying the heatbugs example from the MASON framework so don't
 * blame me for errors (and I also do not earn the merits).
 * <p>
 * The state of a cell is color coded and also by the height. Three display
 * modes are supported:
 * <ul>
 * <li>TILE: Colored and tiles in different heights. Same colored tiles have the
 * same height.</li>
 * <li>MESH: A meshed colored grid. Same colors have the same height.</li>
 * <li>NOZ: Color coding only, no different heights.</li>
 * </ul>
 * 
 * @author hoehne
 */
public class HyperCycle3DWithUI extends GUIState {

	public JFrame displayFrame;
	public static final int TILE = 0;
	public static final int MESH = 1;
	public static final int NOZ = 2;

	int cellMode = MESH;

	ValueGrid2DPortrayal3D cellPortrayal = new ValueGrid2DPortrayal3D();
	QuadPortrayal quadP = null;

	public Display3D display;

	public HyperCycle3DWithUI() {
		this(new HyperCycleSimulation(System.currentTimeMillis(),
				new HyperCycleParameters()), MESH);
	}

	public HyperCycle3DWithUI(SimState state, final int cellMode) {
		super(state);
		this.cellMode = cellMode;
	}

	public static String getName() {
		return "Hyper Cycle Simulation 3D";
	}

	public void start() {
		super.start();
		setupPortrayals();
	}

	public void load(SimState state) {
		super.load(state);
		// we now have new grids. Set up the portrayals to reflect that
		setupPortrayals();
	}

	/**
	 * This method has also its roots at the heatbugs example. It sets up the
	 * visuals.
	 */
	public void setupPortrayals() {
		display.destroySceneGraph();

		HyperCycleSimulation hcsState = (HyperCycleSimulation) state;
		HyperCycleParameters p = hcsState.getParameters();

		// set the color map
		SimpleColorMap cm = new SimpleColorMap(p.colorTable);

		// the heat can be tiles, meshes, or tiles with no change in height
		// (NOZ).
		// Specify which one here.
		switch (cellMode) {
		case TILE:
			quadP = new TilePortrayal(cm, 2.0f);
			break;
		case MESH:
			quadP = new MeshPortrayal(cm, 1.1f);
			break;
		case NOZ:
			quadP = new TilePortrayal(cm); // no height changes
			break;
		}
		cellPortrayal.setPortrayalForAll(quadP);

		// With this line we can tell the bug portrayal to use two triangles
		// rather than a rect to draw each cell. See the documentation for
		// ValueGrid2DPortrayal3D for
		// why this would be useful and when it is not.
		cellPortrayal.setUsingTriangles(true);

		cellPortrayal.setField(((HyperCycleSimulation) state).gridBuffer);

		// reschedule the displayer
		display.reset();

		// rebuild the scene graph
		display.createSceneGraph();
	}

	/**
	 * The overwritten init method. Mainly taken from the heatbugs example.
	 */

	public void init(Controller c) {
		super.init(c);

		// Make the Display3D. We'll have it display stuff later.
		display = new Display3D(600, 600, this, 1);

		// attach the portrayals to the displayer, from bottom to top
		display.attach(cellPortrayal, "HyperCycle Cells");
		cellPortrayal.valueName = "HyperCycle Cells";

		HyperCycleSimulation hcsState = (HyperCycleSimulation) state;
		HyperCycleParameters p = hcsState.getParameters();

		// center the bug graph. Right now it's located at the (0,0) position.
		// For
		// example, if it's a 5x5 graph, and the origin is at (0,0), we want to
		// move it
		// to (2,2). So we want it to be at ( (5-1)/2 = 2, (5-1/2) = 2 ).
		// Similarly,
		// if it's a 6x6 graph we want the origin to be at (2.5, 2.5), dead
		// center between
		// the (2,2) and (3,3) grid positions. To center
		// the origin there, we need to move the graph in the opposite
		// direction.
		// so the general equation for each dimension: (numGridPoints - 1) /
		// -2.0.
		display.translate((p.getWidth() - 1) / -2.0,
				(p.getHeight() - 1) / -2.0, 0);

		// now let's scale it so it fits inside a 1x1x1 cube centered at the
		// origin. We don't
		// have to, but it'll look nicer.
		display.scale(1.0 / Math.max(p.getWidth(), p.getHeight()));

		displayFrame = display.createFrame();
		c.registerFrame(displayFrame); // register the frame so it appears in
		// the "Display" list
		displayFrame.setVisible(true);
	}

	/**
	 * The overwritten quit method.
	 */
	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;
	}
}
