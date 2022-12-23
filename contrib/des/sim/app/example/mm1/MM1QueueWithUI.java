package sim.app.example.mm1;

import java.awt.Color;
import javax.swing.JFrame;
import sim.des.portrayal.DESPortrayalParameters;
import sim.des.portrayal.DelayedEdgePortrayal;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;

public class MM1QueueWithUI extends GUIState {

	public Display2D display;
	public JFrame displayFrame;
	ContinuousPortrayal2D layoutPortrayal = new ContinuousPortrayal2D();
	NetworkPortrayal2D graphPortrayal = new NetworkPortrayal2D();

	public MM1QueueWithUI(SimState state) {
		super(state);
	}

	public MM1QueueWithUI() {
		super(new MM1Queue(System.currentTimeMillis()));
	}

	public Object getSimulationInspectedObject() {
		return state;
	}

	public static String getName() {
		return "MM1Queue example";
	}

	public void start() {
		super.start();
		setupPortrayals();
	}

	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}

	public void setupPortrayals() {
		MM1Queue example = (MM1Queue) state;

		layoutPortrayal.setField(example.field.getNodes());
		graphPortrayal.setField(example.field);
		SimpleEdgePortrayal2D edge = new DelayedEdgePortrayal();
		edge.setBaseWidth(1);
		graphPortrayal.setPortrayalForAll(edge);

		display.reset();
		display.setBackdrop(Color.WHITE);
		display.repaint();
	}

	public void init(Controller c) {
		super.init(c);

		DESPortrayalParameters.setImageClass(MM1QueueWithUI.class);

		display = new Display2D(600, 600, this);
		display.setClipping(false);
		display.attach(graphPortrayal, "Connections");
		display.attach(layoutPortrayal, "Layout");
		displayFrame = display.createFrame();

		c.registerFrame(displayFrame);
		displayFrame.setVisible(true);

	}

	public void quit() {
		super.quit();
		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;
	}

	public static void main(String[] args) {
		MM1QueueWithUI app = new MM1QueueWithUI();
		Console c = new Console(app);
		c.setVisible(true);
	}

}
