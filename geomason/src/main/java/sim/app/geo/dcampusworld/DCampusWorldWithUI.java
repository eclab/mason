/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: CampusWorldWithUI.java 846 2013-01-08 21:47:51Z mcoletti $
 */

package sim.app.geo.dcampusworld;

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
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;


/** MASON GUI wrapper for Campus World demo
 *
 */
public class DCampusWorldWithUI extends GUIState
{

    private Display2D display;
    private JFrame displayFrame;

    private GeomVectorFieldPortrayal walkwaysPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal buildingPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal agentPortrayal = new GeomVectorFieldPortrayal();

    public DCampusWorldWithUI(SimState state)
    {
        super(state);
    }

    public DCampusWorldWithUI() throws ParseException
    {
        super(new DCampusWorld(System.currentTimeMillis()));
    }

    @Override
    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(DCampusWorld.WIDTH, DCampusWorld.HEIGHT, this);

        display.attach(walkwaysPortrayal, "Walkways", true);
        display.attach(buildingPortrayal, "Buildings", true);
        display.attach(roadsPortrayal, "Roads", true);
        display.attach(agentPortrayal, "Agents", true);

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
    }


    @Override
    public void start()
    {
        super.start();
        setupPortrayals();
    }

    private void setupPortrayals()
    {
        DCampusWorld world = (DCampusWorld)state;

        walkwaysPortrayal.setField(world.walkways);
        walkwaysPortrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN,true));

        buildingPortrayal.setField(world.buildings);
        BuildingLabelPortrayal b = new BuildingLabelPortrayal(new GeomPortrayal(Color.DARK_GRAY,true), Color.BLUE);
        buildingPortrayal.setPortrayalForAll(b);
        
        roadsPortrayal.setField(world.roads);
        roadsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.GRAY,true));

        agentPortrayal.setField(world.agents);
        agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED,10.0,true));
//        agentPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.RED,6.0));
        
        display.reset();
        display.setBackdrop(Color.WHITE);

        display.repaint();
    }

    public static void main(String[] args)
    {
        DCampusWorldWithUI worldGUI = null;

        try
        {
            worldGUI = new DCampusWorldWithUI();
        }
        catch (ParseException ex)
        {
            Logger.getLogger(DCampusWorldWithUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        Console console = new Console(worldGUI);
        console.setVisible(true);
    }

}
