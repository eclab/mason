/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 * 
 * $Id$
 * 
 */

package sim.app.geo.touchingworld;

import java.awt.Color;
import javax.swing.JFrame;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;


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

        display = new Display2D(TouchingWorld.WIDTH, TouchingWorld.HEIGHT, this);

        display.attach(shapePortrayal, "Shapes");
        display.attach(selectedDistrictPortrayal, "Selected shape");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
    }

	public void quit()
	{
        super.quit();
        
        if (displayFrame!=null)
        {
            displayFrame.dispose();
        }
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
        selectedDistrictPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED,true));
		
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
