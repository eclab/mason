/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.schelling;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import java.awt.*;
import javax.swing.*;

public class SchellingWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D agentPortrayal = new FastValueGridPortrayal2D("Agents");

    public static void main(String[] args)
        {
        SchellingWithUI schelling = new SchellingWithUI();
        Console c = new Console(schelling);
        c.setVisible(true);
        }
    
    public SchellingWithUI() { super(new Schelling(System.currentTimeMillis())); }
    
    public SchellingWithUI(SimState state) { super(state); }
    
    public static String getName()
        {
        return "Schelling Segregation";
        }
    
    public Object getSimulationInspectedObject() { return state; }  // non-volatile

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
        
    // This is called by start() and by load() because they both had this code
    // so I didn't have to type it twice :-)
    public void setupPortrayals()
        {
        agentPortrayal.setMap(new sim.util.gui.SimpleColorMap(new Color[]{new Color(0,0,0,0), new Color(64,64,64), Color.red, Color.blue}));
        agentPortrayal.setField(((Schelling)state).neighbors);
        
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
        display.attach(agentPortrayal,"Agents");
        
        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(Color.black);
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
        }
    }
    
    
    
    
    
