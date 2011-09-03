/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.heatbugs;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import sim.portrayal.*;
import java.awt.*;
import javax.swing.*;
import sim.field.grid.*;
import sim.util.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.geom.*;
import sim.portrayal.simple.*;

public class HeatBugsWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D heatPortrayal = new FastValueGridPortrayal2D("Heat");
    SparseGridPortrayal2D bugPortrayal = new SparseGridPortrayal2D();

// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
// you'll also need to cause a Bug to wander a lot more in order to see the trail -- I suggest setting
// its idealTemperature to 0 in the Inspector.
/*
  SparseGridPortrayal2D trailsPortrayal = new SparseGridPortrayal2D(); 
*/

    public static void main(String[] args)
        {
        new HeatBugsWithUI().createController();
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
        bugPortrayal.setField(((HeatBugs)state).buggrid);
        bugPortrayal.setPortrayalForAll( new MovablePortrayal2D(new sim.portrayal.simple.OvalPortrayal2D(Color.white)));   // all the heatbugs will be white ovals

        heatPortrayal.setField(((HeatBugs)state).valgrid);
        heatPortrayal.setMap(new sim.util.gui.SimpleColorMap(0,HeatBugs.MAX_HEAT,Color.black,Color.red));
                                
// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
// you'll also need to cause a Bug to wander a lot more in order to see the trail -- I suggest setting
// its idealTemperature to 0 in the Inspector.
/*
  trailsPortrayal.setField(((HeatBugs)state).buggrid);
  SimplePortrayal2D heatBugPortrayal = new sim.portrayal.simple.OvalPortrayal2D(Color.white);
  for(int x=0;x<((HeatBugs)state).bugs.length;x++)
  {
  trailsPortrayal.setPortrayalForObject(((HeatBugs)state).bugs[x], 
  new TrailedPortrayal2D(this, heatBugPortrayal, trailsPortrayal, 10));
  }
*/

        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }
    
    public void init(final Controller c)
        {
        super.init(c);
        
        // Make the Display2D.  We'll have it display stuff later.
        display = new Display2D(400,400,this); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        displayFrame.setTitle(displayFrame.getTitle() + 
                (HeatBugs.availableProcessors() > 1 ?
                " (Multiprocessor)" : "" ));
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals
        display.attach(heatPortrayal,"Heat");

// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
// you'll also need to cause a Bug to wander a lot more in order to see the trail -- I suggest setting
// its idealTemperature to 0 in the Inspector.
/*
  display.attach( trailsPortrayal, "Trails" ); 
*/

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
    
    
    
    
    
