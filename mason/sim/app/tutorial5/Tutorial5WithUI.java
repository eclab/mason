/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial5;
import sim.portrayal.network.*;
import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.Color;


public class Tutorial5WithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args)
        {
        Tutorial5WithUI vid = new Tutorial5WithUI();
        Console c = new Console(vid);
        c.setVisible(true);
        }

    public Tutorial5WithUI() { super(new Tutorial5( System.currentTimeMillis())); }
    public Tutorial5WithUI(SimState state) { super(state); }

    public static String getName() { return "Tutorial 5: Hooke's Law"; }
    
// We comment this out of the example, which will cause MASON to look
// for a file called "index.html" in the same directory -- which we've
// included for consistency with the other applications in the demo 
// apps directory.

/*
  public static Object getInfoByClass(Class theClass) { return "<H2>Tutorial 5</H2> Hooke's Law"; }
*/

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
        // turn off clipping
        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Tutorial 5 Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( edgePortrayal, "Bands" );
        display.attach( nodePortrayal, "Balls" );
        }

    public void quit()
        {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
