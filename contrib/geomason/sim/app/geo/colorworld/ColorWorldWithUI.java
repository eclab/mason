/*
 * ColorWorldWithUI
 *
 *   After starting the demo you should see
 *
 * $Id: ColorWorldWithUI.java,v 1.6 2010-08-23 22:42:34 kemsulli Exp $
 *
 */

package sim.app.geo.colorworld;

import sim.display.*;
import sim.portrayal.simple.*;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.engine.*;
import java.awt.Color;
import javax.swing.*;
import sim.util.gui.SimpleColorMap;


/** MASON GUI wrapper for ColorWorld
 *
 * @author mcoletti
 */
public class ColorWorldWithUI extends GUIState {

    private Display2D display;
    private JFrame displayFrame;

    private GeomVectorFieldPortrayal countyPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal agentPortrayal = new GeomVectorFieldPortrayal();

    public ColorWorldWithUI(SimState state)
    {
        super(state);
    }

    public ColorWorldWithUI()
    {
        super(new ColorWorld(System.currentTimeMillis()));
    }

    public static String getName() { return "Color World Demonstration"; }
    public Object getSimulationInspectedObject() { return state; }

    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(300, 300, this, 1);

        display.attach(countyPortrayal, "FFX County Politcal Boundaries");
        display.attach(agentPortrayal, "Agents");

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
        ColorWorld world = (ColorWorld)state;

        agentPortrayal.setField(ColorWorld.agents);
        agentPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.RED,6.0));

        countyPortrayal.setField(world.county);
        countyPortrayal.setPortrayalForAll(new ColorWorldPortrayal(
        		new SimpleColorMap(0.0, ColorWorld.NUM_AGENTS, Color.WHITE, Color.BLUE))); 

        display.reset();

        display.repaint();
    }

    public static void main(String[] args)
    {
        ColorWorldWithUI worldGUI = new ColorWorldWithUI();
        Console console = new Console(worldGUI);
        console.setVisible(true);
    }

}
