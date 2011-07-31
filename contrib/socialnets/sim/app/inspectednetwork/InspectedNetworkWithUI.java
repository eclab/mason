/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.inspectednetwork;
import sim.app.tutorial5.*;
import sim.portrayal.network.stats.*;
import sim.portrayal.network.*;
import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.Color;


public class InspectedNetworkWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();
    SocialNetworkInspector inspector = new SocialNetworkInspector();

    public static void main(String[] args)
        {
        InspectedNetworkWithUI vid = new InspectedNetworkWithUI();
        Console c = new Console(vid);
        c.setVisible(true);
        }

    public InspectedNetworkWithUI() { super(new Tutorial5( System.currentTimeMillis())); }
    public InspectedNetworkWithUI(SimState state) { super(state); }

    public static String getName() { return "Inspected Network"; }

    public Object getSimulationInspectedObject() { return state; }

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
        Tutorial5 tut = (Tutorial5) state;
        
        // tell the portrayals what to portray and how to portray them
        edgePortrayal.setField( new SpatialNetwork2D( tut.balls, tut.bands ) );
        edgePortrayal.setPortrayalForAll(new BandPortrayal2D());
        nodePortrayal.setField( tut.balls );
        
        inspector.setField(tut.bands, this);
        
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
        display = new Display2D(600,600,this);
        // turn off clipping
        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Tutorial 5 Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( edgePortrayal, "Bands" );
        display.attach( nodePortrayal, "Balls" );
        display.attach( inspector, "Inspector" );
        }

    public void quit()
        {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
