package sim.app.example.streetfood;

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

public class StreetFoodWithUI extends GUIState {

	public Display2D display;
	public JFrame displayFrame;
	ContinuousPortrayal2D layoutPortrayal = new ContinuousPortrayal2D();
	NetworkPortrayal2D graphPortrayal = new NetworkPortrayal2D();

	public StreetFoodWithUI(SimState state) {
		super(state);
	}

	public StreetFoodWithUI() {
		super(new StreetFood(System.currentTimeMillis()));
	}

	public Object getSimulationInspectedObject() {
		return state;
	}

	public static String getName() {
		return "StreetFood example";
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
		StreetFood example = (StreetFood) state;

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

		DESPortrayalParameters.setImageClass(StreetFoodWithUI.class);

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
		StreetFoodWithUI app = new StreetFoodWithUI();
		Console c = new Console(app);
		c.setVisible(true);
	}
}
