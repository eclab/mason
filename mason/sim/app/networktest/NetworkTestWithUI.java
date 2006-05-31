/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.networktest;

import sim.portrayal.network.*;
import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.Color;


public class NetworkTestWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args)
        {
        NetworkTestWithUI vid = new NetworkTestWithUI();
        Console c = new Console(vid);
        c.setVisible(true);
        }

    public NetworkTestWithUI() { super(new NetworkTest( System.currentTimeMillis())); }
    public NetworkTestWithUI(SimState state) { super(state); }

    public static String getName() { return "Network Test"; }
    
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
        edgePortrayal.setField( new SpatialNetwork2D( ((NetworkTest)state).environment, ((NetworkTest)state).network ) );
        edgePortrayal.setPortrayalForAll(new SimpleEdgePortrayal2D(Color.red, Color.black, Color.gray));
        nodePortrayal.setField( ((NetworkTest)state).environment );
        
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
        display = new Display2D(800,600,this,1);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Network Test Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( edgePortrayal, "Edges" );
        display.attach( nodePortrayal, "Nodes" );
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
