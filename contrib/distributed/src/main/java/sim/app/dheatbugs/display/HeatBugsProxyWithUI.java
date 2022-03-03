/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dheatbugs.display;

import java.awt.Color;

import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.display.SimStateProxy;
import sim.engine.SimState;
import sim.portrayal.Inspector;
import sim.portrayal.grid.DenseGridPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.simple.MovablePortrayal2D;
import sim.util.Bag;
import sim.util.Properties;

public class HeatBugsProxyWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D heatPortrayal = new FastValueGridPortrayal2D("Heat");
    DenseGridPortrayal2D bugPortrayal = new DenseGridPortrayal2D();

    public static final double MAX_HEAT = 32000;

    public static void main(String[] args)
        {
        new HeatBugsProxyWithUI().createController();
        }
    
    public HeatBugsProxyWithUI()
    	{
    	super(new HeatBugsProxy(System.currentTimeMillis()));
    	}
    
    public HeatBugsProxyWithUI(SimState state)
    	{
    	super(state);
		}
    
    public static String getName()
        {
        return "HeatBugs Proxy";
        }
    
    public Object getSimulationInspectedObject()
    	{
    	return state; // non-volatile
    	}

    public void start()
        {
        super.start();
        // set up our portrayals
        setupPortrayals();
        
        //Inspector ins = Inspector.getInspector(((SimStateProxy)state).getStats(), this, "Properties");   
        //Inspector ins = Inspector.getInspector(((SimStateProxy)state).getStatsAligned(), this, "Properties");   
        //call a SimStateProxy getProperties I think (implement this, make it return a property object with all values
        System.out.println("hello");
        Properties prop_test = ((SimStateProxy)state).getProperties(0);  //null for some reason?
        
        System.out.println(prop_test.getClass());
        
        int numProp = ((SimStateProxy)state).getProperties(0).numProperties();
        System.out.println(prop_test);
        //System.exit(-1);
        String method_name = ((SimStateProxy)state).getProperties(0).getName(0);
        System.out.println(method_name);        
        
        Inspector ins = Inspector.getInspector(((SimStateProxy)state).getProperties(0), this, "Properties");   

        
        Bag insBag = new Bag();
        Bag insName = new Bag();
        insBag.add(ins);
        insName.add("stats_inspector : "+state);
        this.controller.setInspectors(insBag, insName);
        
        System.out.println(insName);
        System.out.println("----");
        System.out.println(insBag);
        //System.exit(-1);
        
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
        bugPortrayal.setField(((HeatBugsProxy)state).buggrid);
        
        // COMMENT OUT THIS LINE to try out trails
        bugPortrayal.setPortrayalForAll( new MovablePortrayal2D(new sim.portrayal.simple.OvalPortrayal2D(Color.white)));   // all the heatbugs will be white ovals

        heatPortrayal.setField(((HeatBugsProxy)state).valgrid);
        heatPortrayal.setMap(new sim.util.gui.SimpleColorMap(0, MAX_HEAT,Color.black,Color.red));
                             
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }
    
    public void init(final Controller c)
        {
        super.init(c);
        
        // Make the Display2D.  We'll have it display stuff later.
        display = new Display2D(500,500,this); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        displayFrame.setTitle(displayFrame.getTitle());
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
    
    
    
    
    
