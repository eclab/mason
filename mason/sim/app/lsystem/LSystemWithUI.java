/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.lsystem;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import java.awt.*;
import javax.swing.*;

public class LSystemWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;
    public static Console c;

    private ContinuousPortrayal2D systemPortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args)
        {
        LSystemWithUI lsystem = new LSystemWithUI();
        c = new Console(lsystem);
        c.setVisible(true);
        }
    
    public LSystemWithUI() { super(new LSystem(System.currentTimeMillis())); }
    
    public LSystemWithUI(SimState state) { super(state); }
    
    public static String getName() { return "Lindenmayer Systems"; }
    
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
        // this portrayal will draw the objects in the drawEnvironment field... 
        // which contains all the segments that make up the tree.
        systemPortrayal.setField(((LSystem)state).drawEnvironment);
                
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }
    
    public void init(Controller c)
        {
        super.init(c);
        
        // make the display2d in all of its glory
        display = new Display2D(400,400,this,1); // at 400x400, we've got 4x4 per array position
        
        // No clipping!
        // The effect of this call becomes apparent when the display is zoomed out (zoom factor < 1)
        // -- instead of drawing the boundaries of your field and clipping there, the display
        // instead draws the entire view window, allowing you to view areas that are out of bounds.
        // This is useful when your L-system starts to go off the edge of the display.
        display.setClipping(false);
        
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals
        display.attach(systemPortrayal,"LSystem");

        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(Color.white);
        
        // setup the defaults
        LSystem ls = (LSystem)state;
        LSystemData.setVector(ls.l.code, "F");
        ls.l.seed = "F";
        
        ls.l.rules.add(new Rule((byte)'F', "F[+F]F[-F]F"));
        
        
        // You have been granted the power to both create and destroy at whim...
        // create and destroy the tabs on the Console anyway.
        // Here we remove the Inspectors tab.
        // Be careful though!  In order to safely remove this, you must make sure that your portrayals have 
        // the hitObjects function overridden so that it never returns an object to be inspected..
        ((Console)c).getTabPane().removeTabAt(3);
        // add drawUI as tab
        DrawUI draw = new DrawUI(this);
        ((Console)c).getTabPane().addTab("Draw", new JScrollPane(draw));
        // add rulesUI as tab
        ((Console)c).getTabPane().addTab("Rules", new RuleUI(this, draw));

        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
        }
    }
