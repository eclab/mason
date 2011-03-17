/*
 * TouchingWorldWithUI
 *
 *  
 *
 * $Id: TouchingWorldWithUI.java,v 1.2 2010-08-20 20:30:12 kemsulli Exp $
 * 
 */

package sim.app.geo.touchingworld;

import sim.display.*; 
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.engine.*; 
import java.awt.Color; 
import javax.swing.*; 
import sim.portrayal.geo.GeomPortrayal;


/** MASON GUI wrapper for TouchingWorld
 *
 */
public class TouchingWorldWithUI extends GUIState {

    private Display2D display;
    private JFrame displayFrame;

    private GeomVectorFieldPortrayal shapePortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal selectedDistrictPortrayal = new GeomVectorFieldPortrayal();
    
    public TouchingWorldWithUI(SimState state)
    {
        super(state);
    }

    public TouchingWorldWithUI()
    {
        super(new TouchingWorld(System.currentTimeMillis()));
    }

	public static String getName() { return "Touching World Demonstration"; }

	public Object getSimulationInspectedObject() { return state; }

    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(TouchingWorld.WIDTH, TouchingWorld.HEIGHT, this, 1);

        display.attach(shapePortrayal, "Shapes");
        display.attach(selectedDistrictPortrayal, "Selected shape");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
    }

	public void quit()
	{
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
	}

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
		selectedDistrictPortrayal.setField(world.selectedShape);
        selectedDistrictPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED,false));
		
        shapePortrayal.setField(world.shapes);
        shapePortrayal.setPortrayalForAll(new GeomPortrayal(Color.BLUE,false));
        
        display.reset();
        display.repaint();
    }

    public static void main(String[] args)
    {
        TouchingWorldWithUI worldGUI = new TouchingWorldWithUI();
		Console console = new Console(worldGUI);
        console.setVisible(true);
    }
    
}
