package sim.app.geo.nearbyworld;

import java.awt.Color;
import javax.swing.JFrame;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.simple.OvalPortrayal2D;


/** 
 * MASON GUI wrapper for the NearbyWorld GeoMASON example.
 */
public class NearbyWorldWithUI extends GUIState {

    private Display2D display;
    private JFrame displayFrame;

    private GeomVectorFieldPortrayal worldFieldPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal agentFieldPortrayal = new GeomVectorFieldPortrayal();

    
    public NearbyWorldWithUI(SimState state) { super(state); }

    public NearbyWorldWithUI()  { super(new NearbyWorld(System.currentTimeMillis())); }


    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(300, 300, this, 1);
        display.attach(worldFieldPortrayal, "World");
        display.attach(agentFieldPortrayal, "Agent");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
    }

    public void start()
    {
        super.start();
        setupPortrayals();
    }

    private void setupPortrayals()
    {
        NearbyWorld world = (NearbyWorld)state;

        worldFieldPortrayal.setField(world.world);

        agentFieldPortrayal.setField(world.agentField);
        // We want a red dot for the agent.  We also need to specify the scale; if
        // we don't then the default agent dot will cover the entire area.
        agentFieldPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.RED,0.01));

        display.reset();
        display.setBackdrop(Color.WHITE);
        display.repaint();
    }

    public static void main(String[] args)
    {
        NearbyWorldWithUI worldGUI = new NearbyWorldWithUI();
        Console console = new Console(worldGUI);
        console.setVisible(true);
    }
    
}
