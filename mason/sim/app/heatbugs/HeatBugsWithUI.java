/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.heatbugs;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import java.awt.*;
import javax.swing.*;

public class HeatBugsWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D heatPortrayal = new FastValueGridPortrayal2D("Heat");
    SparseGridPortrayal2D bugPortrayal = new SparseGridPortrayal2D();

    public static void main(String[] args)
        {
        HeatBugsWithUI heatbugs = new HeatBugsWithUI();
        Console c = new Console(heatbugs);
        c.setVisible(true);
        }
    
    public HeatBugsWithUI() { super(new HeatBugs(System.currentTimeMillis())); }
    
    public HeatBugsWithUI(SimState state) { super(state); }
    
    public static String getName()
        {
        return "HeatBugs";
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
        // tell the portrayals what to portray and how to portray them
        heatPortrayal.setMap(new sim.util.gui.SimpleColorMap(0,HeatBugs.MAX_HEAT,Color.black,Color.red));
        bugPortrayal.setPortrayalForAll( new sim.portrayal.simple.OvalPortrayal2D(Color.white) );   // all the heatbugs will be white ovals
            
        heatPortrayal.setField(((HeatBugs)state).valgrid);
        bugPortrayal.setField(((HeatBugs)state).buggrid);

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
        displayFrame.setTitle(displayFrame.getTitle() + 
                              (HeatBugs.availableProcessors() > 1 ?
                               " (Multiprocessor)" : "" ));
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals
        display.attach(heatPortrayal,"Heat");
        display.attach(bugPortrayal,"Bugs");

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
    
    
    
    
    
