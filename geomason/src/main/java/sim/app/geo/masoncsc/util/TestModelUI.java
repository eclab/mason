package sim.app.geo.masoncsc.util;

import java.awt.Color;

import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.grid.FastValueGridPortrayal2D;

public class TestModelUI extends GUIState
{
    public Display2D display;
    public JFrame displayFrame;
	FastValueGridPortrayal2D gridPortrayal = new FastValueGridPortrayal2D("Grid");
	
	public TestModelUI() {
		super(new TestModel(System.currentTimeMillis()));
	}

	public TestModelUI(SimState state) {
		super(state);
	}
	
    public Object getSimulationInspectedObject() { return state; }  // non-volatile

	public static String getName() {
		return "Test Model";
	}
	
	public void setupPortrayals() {
		gridPortrayal.setField(((TestModel)state).grid);
		gridPortrayal.setMap(new TriColorMap(-1, 0, 1, Color.red, Color.white, Color.blue));
//		gridPortrayal.setMap(new SimpleColorMap(-1, 1, Color.red, Color.white));
		
		display.reset();
		display.repaint(); 
	}
	
	@Override
	public void init(final Controller c) {
		super.init(c);
		display = new Display2D(400, 400, this);
		displayFrame = display.createFrame();
		displayFrame.setTitle("Test Model");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
	        
        display.attach(gridPortrayal, "Grid");
        display.setBackdrop(Color.black);
	}

	@Override
	public void start() {
		super.start();
		setupPortrayals();
	}

	public void quit() {
		super.quit();

		if (displayFrame != null) displayFrame.dispose();
		displayFrame = null; 
		display = null; 
	}
	
	public static void main(String[] args) {
		new TestModelUI().createController();
	}

}
