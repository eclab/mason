/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.keepaway;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import java.awt.*;
import javax.swing.*;

public class KeepawayWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    ContinuousPortrayal2D entityPortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args)
        {
        KeepawayWithUI botball = new KeepawayWithUI();
        Console c = new Console(botball);
        c.setVisible(true);
        }
    
    public KeepawayWithUI() { super(new Keepaway(System.currentTimeMillis())); }
    
    public KeepawayWithUI(SimState state) { super(state); }
    
    public static String getName() { return "Keep-Away Soccer"; }
    
    public void start()
        {
        super.start();
        // set up our portrayals
        setupPortrayals();
        }
    
    public void load(SimState state)
        {
        super.load(state);
        // we now have new grids.  Set up the portrayals to reflect that
        setupPortrayals();
        }
        

    public void setupPortrayals()
        {
        // tell the portrayals what to portray and how to portray them
        entityPortrayal.setField(((Keepaway)state).fieldEnvironment);
        entityPortrayal.setPortrayalForClass(Bot.class, new sim.portrayal.simple.RectanglePortrayal2D(Color.red));
        entityPortrayal.setPortrayalForClass(Ball.class, new sim.portrayal.simple.OvalPortrayal2D(Color.white));
                    
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }
    
    public void init(Controller c)
        {
        super.init(c);
        
        // Make the Display2D.  We'll have it display stuff later.
        display = new Display2D(400,400,this,1); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals
        display.attach(entityPortrayal,"Bots and Balls");

        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(new Color(0,80,0));  // a dark green
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
        }
    }
    
    
    
    
    
