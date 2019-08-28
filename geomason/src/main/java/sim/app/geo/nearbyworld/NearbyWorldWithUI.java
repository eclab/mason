/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package sim.app.geo.nearbyworld;

import java.awt.Color;
import javax.swing.JFrame;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.simple.CircledPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;


/** 
 * MASON GUI wrapper for the NearbyWorld GeoMASON example.
 */
public class NearbyWorldWithUI extends GUIState {

    private Display2D display;
    private JFrame displayFrame;

    private GeomVectorFieldPortrayal objectsFieldPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal nearbyFieldPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal agentFieldPortrayal = new GeomVectorFieldPortrayal();

    
    public NearbyWorldWithUI(SimState state) { super(state); }

    public NearbyWorldWithUI()  { super(new NearbyWorld(System.currentTimeMillis())); }


    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(NearbyWorld.WIDTH, NearbyWorld.HEIGHT, this);
        display.attach(objectsFieldPortrayal, "World");
        display.attach(nearbyFieldPortrayal, "Near Objects");
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

        // Since no object portrayal is given, the default GeomPortrayal with
        // Color.GRAY is used.
        objectsFieldPortrayal.setField(world.objects);
        objectsFieldPortrayal.setPortrayalForAll(new GeomPortrayal(Color.DARK_GRAY, true));

        nearbyFieldPortrayal.setField(world.nearbyField);
        nearbyFieldPortrayal.setPortrayalForAll(new GeomPortrayal(Color.PINK, true));

        agentFieldPortrayal.setField(world.agentField);

        // We want a red dot for the agent.  We also need to specify the scale; if
        // we don't then the default agent dot will cover the entire area.
        agentFieldPortrayal.setPortrayalForAll(new CircledPortrayal2D(new OvalPortrayal2D(Color.RED,5), 0, Agent.DISTANCE, Color.GREEN, false));
//        agentFieldPortrayal.setPortrayalForAll(new CircledPortrayal2D(new OvalPortrayal2D(Color.RED,5), 0, 50, Color.GREEN, false));

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
