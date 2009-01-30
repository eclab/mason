/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.lightcycles;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import java.awt.*;
import javax.swing.*;

public class LightCyclesWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;
    
    // For keyboard input
    public ControlUI controls;
    
    // gridPortrayal draws the "walls" created in the paths of the cycles
    FastValueGridPortrayal2D gridPortrayal = new FastValueGridPortrayal2D();
    // cycleGridPortrayal draws the cycles themselves
    SparseGridPortrayal2D cycleGridPortrayal = new SparseGridPortrayal2D();
    
    public static void main(String[] args)
        {
        LightCyclesWithUI cycles = new LightCyclesWithUI();
        Console c = new Console(cycles);
        c.setVisible(true);
        }
    
    public LightCyclesWithUI() { super(new LightCycles(System.currentTimeMillis())); }
    public LightCyclesWithUI(SimState state) { super(state); }
    
    public static String getName() { return "Light Cycles"; }
    
    public void start()
        {
        super.start();
        // set up our portrayals
        setupPortrayals();
        
        // If this is not the first time we have played the sim in this run,
        // the user might still have a cycle selected... so clear the cycle var
        if(controls != null)
            controls.c = null;
        }
    
    public void load(SimState state)
        {
        super.load(state);
        // we now have new grids.  Set up the portrayals to reflect that
        setupPortrayals();
        }

    public void setupPortrayals()
        {
        // portray varying levels on the gradient of transparent to red for cycle trails
        gridPortrayal.setField(((LightCycles)state).grid);
                
        // make an array of random colors
        Color[] colors = new Color[((LightCycles)state).cycleCount+1];  
        colors[0] = new Color(0,0,0,0);
        for(int i = 1 ; i < colors.length; i++)
            { colors[i] = new Color(state.random.nextInt(255), state.random.nextInt(255), state.random.nextInt(255)); }
                
        gridPortrayal.setMap(new sim.util.gui.SimpleColorMap(colors));
        
        // just draw the cycles as white ovals
        cycleGridPortrayal.setField(((LightCycles)state).cycleGrid);
        cycleGridPortrayal.setPortrayalForClass(Cycle.class,
            new sim.portrayal.simple.OvalPortrayal2D(Color.white));
        
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
        display.attach(gridPortrayal,"Paths");
        display.attach(cycleGridPortrayal,"Cycles");

        // make the ControlUI the first time around
        controls = new ControlUI(this, cycleGridPortrayal);

        // Specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(new Color(0,0,128));  
        // or try: display.setBackdrop(new GradientPaint(0,0,Color.green,400,400,Color.blue));
        // but be prepared to have to increase the amount of memory because it GC's a lot.  But
        // it's nifty nonetheless!
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
        }
    }
