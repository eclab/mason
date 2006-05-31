/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims;


import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.Color;

public class WoimsDemoWithUI extends GUIState
    {

    public Display2D display;
    public JFrame displayFrame;

    ContinuousPortrayal2D woimsPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D obstaclesPortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args)
        {
        WoimsDemoWithUI woims = new WoimsDemoWithUI();
        Console c = new Console(woims);
        c.setVisible(true);
        }

    public WoimsDemoWithUI() { super(new WoimsDemo( System.currentTimeMillis())); }
    public WoimsDemoWithUI(SimState state) { super(state); }

    public static String getName() { return "Woims"; }

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
        // tell the portrayals what to portray and how to portray them
        woimsPortrayal.setField(((WoimsDemo)state).woimsEnvironment);
        obstaclesPortrayal.setField(((WoimsDemo)state).obstaclesEnvironment);
                
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
        display = new Display2D(600,600,this,1);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Woims Demonstration Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( woimsPortrayal, "Woims" );
        display.attach( obstaclesPortrayal, "Obstacles" );
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
