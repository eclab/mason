/*
  Copyright 2017 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.jbox2d.blobtest;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import javax.swing.*;
import java.awt.*;
import sim.portrayal.simple.*;
import sim.portrayal.SimplePortrayal2D;
import java.awt.geom.*;

public class BlobTestWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    public static void main(String[] args)
        {
        new BlobTestWithUI().createController();  // randomizes by currentTimeMillis
        }

    public Object getSimulationInspectedObject() { return state; }  // non-volatile

    ContinuousPortrayal2D boxesPortrayal = new ContinuousPortrayal2D();

    public BlobTestWithUI()
        {
        super(new BlobTest(System.currentTimeMillis()));
        }
    
    public BlobTestWithUI(SimState state) 
        {
        super(state);
        }

    public static String getName() { return "Blob Test Example"; }

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
        BlobTest jbox2d = (BlobTest)state;

        boxesPortrayal.setField(jbox2d.boxes);
        boxesPortrayal.setPortrayalForAll(new AdjustablePortrayal2D(
        	new sim.portrayal.simple.MovablePortrayal2D(        			
        		new JBox2DPortrayal(new Rectangle2D.Double(-100, -100, 200, 200)))));
        
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }

    public void init(Controller c)
        {
        super.init(c);

        // make the displayer
        display = new Display2D(400, 400 ,this);
        display.setBackdrop(Color.black);


        displayFrame = display.createFrame();
        displayFrame.setTitle("Boxes");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
                
        display.attach( boxesPortrayal, "Boxes" );
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }
    }
