package sim.app.dantsforage.display;

import java.awt.Color;

import javax.swing.JFrame;

import sim.app.dantsforage.DAntsForage;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.grid.DenseGridPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;

public class AntsForageProxywithUI extends GUIState{
	
    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D homePheromonePortrayal = new FastValueGridPortrayal2D("Home Pheromone");
    FastValueGridPortrayal2D foodPheromonePortrayal = new FastValueGridPortrayal2D("Food Pheromone");
    FastValueGridPortrayal2D sitesPortrayal = new FastValueGridPortrayal2D("Site", true);  // immutable
    FastValueGridPortrayal2D obstaclesPortrayal = new FastValueGridPortrayal2D("Obstacle", true);  // immutable
    DenseGridPortrayal2D bugPortrayal = new DenseGridPortrayal2D();
                
    public static void main(String[] args)
        {
        new AntsForageProxywithUI().createController();
        }
    
    public AntsForageProxywithUI() { super(new AntsForageProxy(System.currentTimeMillis())); }
    public AntsForageProxywithUI(SimState state) { super(state); }
    
    // allow the user to inspect the model
    public Object getSimulationInspectedObject() { return state; }  // non-volatile

    public static String getName() { return "Ant Foraging"; }
    
    public void setupPortrayals()
        {
    	AntsForageProxy af = (AntsForageProxy)state;

        // tell the portrayals what to portray and how to portray them
        homePheromonePortrayal.setField(af.toHomeGridGrid);
        homePheromonePortrayal.setMap(new sim.util.gui.SimpleColorMap(
                0,
                DAntsForage.LIKELY_MAX_PHEROMONE,
                // home pheromones are beneath all, just make them opaque
                Color.white, //new Color(0,255,0,0),
                new Color(0,255,0,255) )
            { public double filterLevel(double level) { return Math.sqrt(Math.sqrt(level)); } } );  // map with custom level filtering
        foodPheromonePortrayal.setField(af.toFoodGridGrid);
        foodPheromonePortrayal.setMap(new sim.util.gui.SimpleColorMap(
                0,
                DAntsForage.LIKELY_MAX_PHEROMONE,
                new Color(0,0,255,0),
                new Color(0,0,255,255) )
            { public double filterLevel(double level) { return Math.sqrt(Math.sqrt(level)); } } );  // map with custom level filtering
        sitesPortrayal.setField(af.sitesGrid);
        sitesPortrayal.setMap(new sim.util.gui.SimpleColorMap(
                0,
                1,
                new Color(0,0,0,0),
                new Color(255,0,0,255) ));
        obstaclesPortrayal.setField(af.obstaclesGrid);
        obstaclesPortrayal.setMap(new sim.util.gui.SimpleColorMap(
                0,
                1,
                new Color(0,0,0,0),
                new Color(128,64,64,255) ));
        bugPortrayal.setField(af.antGrid);
            
        // reschedule the displayer
        display.reset();

        // redraw the display
        display.repaint();
        }
    
    public void start()
        {
        super.start();  // set up everything but replacing the display
        // set up our portrayals
        setupPortrayals();
        }
            
    public void load(SimState state)
        {
        super.load(state);
        // we now have new grids.  Set up the portrayals to reflect that
        setupPortrayals();
        }

    public void init(Controller c)
        {
        super.init(c);
        
        // Make the Display2D.  We'll have it display stuff later.
        display = new Display2D(400,400,this); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals from bottom to top
        display.attach(homePheromonePortrayal,"Pheromones To Home");
        display.attach(foodPheromonePortrayal,"Pheromones To Food");
        display.attach(sitesPortrayal,"Site Locations");
        display.attach(obstaclesPortrayal,"Obstacles");
        display.attach(bugPortrayal,"Agents");
        
        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(Color.white);
        }
        
    public void quit()
        {
        super.quit();
        
        // disposing the displayFrame automatically calls quit() on the display,
        // so we don't need to do so ourselves here.
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
        }
        

}
