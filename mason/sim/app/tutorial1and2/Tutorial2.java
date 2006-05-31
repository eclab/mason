/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial1and2;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import java.awt.*;
import javax.swing.*;

public class Tutorial2 extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    public Tutorial2() { super(new Tutorial1(System.currentTimeMillis())); }
    
    public Tutorial2(SimState state) { super(state); }
    
    public static String getName() { return "Tutorial 2: Life"; }

// We comment this out of the example, which will cause MASON to look
// for a file called "index.html" in the same directory -- which we've
// included for consistency with the other applications in the demo 
// apps directory.   
/* 
   public static Object getInfo()
   {
   return 
   "<H2>Conway's Game of Life</H2>" +
   "<p>... with a B-Heptomino"; 
   }
*/   


    FastValueGridPortrayal2D gridPortrayal = new FastValueGridPortrayal2D();

    public void setupPortrayals()
        {
        // tell the portrayals what to portray and how to portray them
        gridPortrayal.setField(((Tutorial1)state).grid);
        gridPortrayal.setMap(
            new sim.util.gui.SimpleColorMap(
                new Color[] {new Color(0,0,0,0), Color.blue}));
        }
    
    public void start()
        {
        super.start();      
        setupPortrayals();  // set up our portrayals
        display.reset();    // reschedule the displayer
        display.repaint();  // redraw the display
        }
    
    public void init(Controller c)
        {
        super.init(c);
        
        // Make the Display2D.  We'll have it display stuff later.
        Tutorial1 tut = (Tutorial1)state;
        display = new Display2D(tut.gridWidth * 4, tut.gridHeight * 4,this,1);
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals
        display.attach(gridPortrayal,"Life");

        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(Color.black);
        }

    public static void main(String[] args)
        {
        Tutorial2 tutorial2 = new Tutorial2();
        Console c = new Console(tutorial2);
        c.setVisible(true);
        }
    
    public void load(SimState state)
        {
        super.load(state);
        setupPortrayals();  // we now have new grids.  Set up the portrayals to reflect this
        display.reset();    // reschedule the displayer
        display.repaint();  // redraw the display
        }
        
    }
    
    
    
    
    
