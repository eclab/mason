/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package sim.app.cto;
import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.Color;

public class CooperativeObservationWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    ContinuousPortrayal2D coPortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args)
        {
        CooperativeObservationWithUI co = new CooperativeObservationWithUI();
        Console c = new Console(co);
        c.setVisible(true);
        }

    public CooperativeObservationWithUI() { super(new CooperativeObservation( System.currentTimeMillis())); }
    public CooperativeObservationWithUI(SimState state) { super(state); }

    public static String getName() { return "Cooperative Target Observation"; }
    
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
        coPortrayal.setField(((CooperativeObservation)state).environment);
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
        displayFrame.setTitle("Cooperative Target Observation Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( coPortrayal, "Agents" );
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
