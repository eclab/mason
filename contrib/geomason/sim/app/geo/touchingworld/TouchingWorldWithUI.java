/*
 * TouchingWorldWithUI
 *
 *   After starting the demo you should see [rewrite!].
 *   The agents have the same behavior dictated by two rules.  First keep
 *   moving in one of the eight cardinal directions.  Second, if the next
 *   step is into water, randomly choose a new cardinal direction.  This
 *   has the effect of having the agents "bounce" around the main island.
 *
 * $Id: TouchingWorldWithUI.java,v 1.1 2010-04-12 20:32:40 mcoletti Exp $
 * 
 */

package sim.app.geo.touchingworld;

import sim.display.*; 
import sim.portrayal.geo.GeomFieldPortrayal;
import sim.engine.*; 
import java.awt.Color; 
import javax.swing.*; 
import sim.portrayal.geo.GeomPortrayal;


/** MASON GUI wrapper for TouchingWorld
 *
 * @author mcoletti
 */
public class TouchingWorldWithUI extends GUIState {

    private Display2D display;
    private JFrame displayFrame;

    private GeomFieldPortrayal shapePortrayal = new GeomFieldPortrayal();
//    private GeomFieldPortrayal selectedDistrictPortrayal = new GeomFieldPortrayal();
    
    public TouchingWorldWithUI(SimState state)
    {
        super(state);
    }

    public TouchingWorldWithUI()
    {
        super(new TouchingWorld(System.currentTimeMillis()));
    }

	public static String getName() { return "Touching World Demonstration"; }

    @Override
	public Object getSimulationInspectedObject() { return state; }

    @Override
    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(300, 300, this, 1);

        display.attach(shapePortrayal, "Shapes");
//        display.attach(selectedDistrictPortrayal, "Selected shape");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
    }

    @Override
	public void quit()
	{
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
	}

    @Override
    public void start()
    {
        super.start();
        setupPortrayals();
    }

    private void setupPortrayals()
    {
        TouchingWorld world = (TouchingWorld)state;

		// we use a GeomPortrayal for the agents also, since GeomPortrayal 
		// handles the translation between screen and map coordinates gracefully
//		selectedDistrictPortrayal.setField(world.selectedShape);
//        agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.PINK,10.0,true));
//        selectedDistrictPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED,true));
		
        shapePortrayal.setField(world.shapes);
        shapePortrayal.setPortrayalForAll(new GeomPortrayal(Color.BLUE,false));
//        shapePortrayal.setImmutableField(true);
        
        display.reset();
//        display.setBackdrop(Color.GRAY);

        display.repaint();
    }

    public static void main(String[] args)
    {
        TouchingWorldWithUI worldGUI = new TouchingWorldWithUI();
		Console console = new Console(worldGUI);
        console.setVisible(true);
    }
    
}
