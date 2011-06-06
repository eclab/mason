/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.mav;

import sim.portrayal.continuous.*;
import sim.portrayal.simple.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.Color;

public class MavDemoWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    public static void main(String[] args)
        {
        new MavDemoWithUI().createController();  // randomizes by currentTimeMillis
        }

    ContinuousPortrayal2D obstaclePortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D mavPortrayal = new ContinuousPortrayal2D();
    
    public MavDemoWithUI()
        {
        super(new MavDemo(System.currentTimeMillis()));
        }
    
    public MavDemoWithUI(SimState state) 
        {
        super(state);
        }

    public static String getName() { return "Micro Air Vehicles"; }

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
        MavDemo mavdemo = (MavDemo)state;
        // obstacle portrayal needs no setup
        obstaclePortrayal.setField(mavdemo.ground);
        mavPortrayal.setField(mavdemo.mavs);
        mavPortrayal.setPortrayalForAll(
            new CircledPortrayal2D(
                new LabelledPortrayal2D(
                    new OrientedPortrayal2D(
                        new OvalPortrayal2D(20),
                        0,20), 
                    20.0, null, Color.blue, true), // 'null' means to display toString() for underlying object, rather than a predefined label
                0, 30.0, Color.blue, true));
        
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
        display = new Display2D(500,500,this);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Mav Demonstration Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( obstaclePortrayal, "Regions" );
        display.attach( mavPortrayal, "MAVs" );
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
