/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.swarmgame;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import javax.swing.*;
import java.awt.*;
import sim.portrayal.simple.*;

public class SwarmGameWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    public static void main(String[] args)
        {
        SwarmGameWithUI mav = new SwarmGameWithUI();  // randomizes by currentTimeMillis
        Console c = new Console(mav);
        c.setVisible(true);
        }

    public Object getSimulationInspectedObject() { return state; }  // non-volatile

    ContinuousPortrayal2D agentsPortrayal = new ContinuousPortrayal2D();
    
    public SwarmGameWithUI()
        {
        super(new SwarmGame(System.currentTimeMillis()));
        }
    
    public SwarmGameWithUI(SimState state) 
        {
        super(state);
        }

    public static String getName() { return "The Swarm Game"; }

    public void start()
        {
        super.start();
        setupPortrayals();
        }

    public void load(SimState state)
        {
        super.load(state);
        setupPortrayals();
        }
        
    public void setupPortrayals()
        {
        SwarmGame swarm = (SwarmGame)state;
        // obstacle portrayal needs no setup
        agentsPortrayal.setField(swarm.agents);
        agentsPortrayal.setPortrayalForAll(new OrientedPortrayal2D(new OvalPortrayal2D(Color.black),0,1.0));
        
        // reschedule the displayer
        display.reset();
        display.setBackdrop(Color.white);
                
        // redraw the display
        display.repaint();
        }

    public void init(Controller c)
        {
        super.init(c);

        // make the displayer
        display = new Display2D(500,500,this,1);
        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Swarmers");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( agentsPortrayal, "Fear the Swarmers!" );
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
