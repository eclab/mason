/*
 * NetworkWorldWithUI
 */

package sim.app.geo.networkworld;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.io.ParseException;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.geo.GeomFieldPortrayal;
import sim.portrayal.geo.GeomPortrayal;


/** MASON GUI wrapper for GeoOgrTest
 *
 * @author mcoletti
 */
public class NetworkWorldWithUI extends GUIState {

    private Display2D display;
    private JFrame displayFrame;

    private GeomFieldPortrayal geometryPortrayal = new GeomFieldPortrayal();
    private GeomFieldPortrayal intersectionPortrayal = new GeomFieldPortrayal();
    private GeomFieldPortrayal agentPortrayal = new GeomFieldPortrayal();

    
    public NetworkWorldWithUI(SimState state) { super(state); }
    public NetworkWorldWithUI() throws ParseException { super(new NetworkWorld(System.currentTimeMillis())); }
    
    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(300, 300, this, 1);
        display.attach(geometryPortrayal, "World");
        display.attach(intersectionPortrayal, "Intersections");
        display.attach(agentPortrayal, "Agent");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
    }

    public void start()
    {
        super.start();
        setupPortrayals();
        syncMBRs();
    }

    private void setupPortrayals()
    {
        NetworkWorld world = (NetworkWorld)state;

        geometryPortrayal.setField(world.world);

        intersectionPortrayal.setField(world.junctions);
        intersectionPortrayal.setPortrayalForAll(new GeomPortrayal(Color.LIGHT_GRAY, true));

        agentPortrayal.setField(world.agents);
        agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED, true));

        display.reset();
        display.setBackdrop(Color.WHITE);
        display.repaint();
    }


    /** Ensure that the MBRs for all GeomFields is the same
     *
     */
    void syncMBRs()
    {
        NetworkWorld world = (NetworkWorld) state;

        Envelope mbr = world.world.getMBR();

        mbr.expandToInclude(world.junctions.getMBR());
        mbr.expandToInclude(world.agents.getMBR());

        world.world.setMBR(mbr);
        world.junctions.setMBR(mbr);
        world.agents.setMBR(mbr);
    }

    public static void main(String[] args)
    {
        NetworkWorldWithUI worldGUI = null;
        
        try
            {
                worldGUI = new NetworkWorldWithUI();
            }
        catch (ParseException ex)
            {
                Logger.getLogger(NetworkWorldWithUI.class.getName()).log(Level.SEVERE, null, ex);
            }

        Console console = new Console(worldGUI);
        console.setVisible(true);
    }
    
}
