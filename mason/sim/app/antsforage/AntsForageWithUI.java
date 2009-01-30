/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.antsforage;

import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import java.awt.*;
import javax.swing.*;

public class AntsForageWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D homePheromonePortrayal = new FastValueGridPortrayal2D("Home Pheromone");
    FastValueGridPortrayal2D foodPheromonePortrayal = new FastValueGridPortrayal2D("Food Pheromone");
    FastValueGridPortrayal2D sitesPortrayal = new FastValueGridPortrayal2D("Site", true);  // immutable
    FastValueGridPortrayal2D obstaclesPortrayal = new FastValueGridPortrayal2D("Obstacle", true);  // immutable
    SparseGridPortrayal2D bugPortrayal = new SparseGridPortrayal2D();
                
    public static void main(String[] args)
        {
        AntsForageWithUI antsForage = new AntsForageWithUI();
        Console c = new Console(antsForage);
        c.setVisible(true);
        }
    
    public AntsForageWithUI() { super(new AntsForage(System.currentTimeMillis())); }
    public AntsForageWithUI(SimState state) { super(state); }
    
    public static String getName() { return "Ant Foraging"; }
    
    public void setupPortrayals()
        {
        AntsForage af = (AntsForage)state;

        // tell the portrayals what to portray and how to portray them
        homePheromonePortrayal.setField(af.toHomeGrid);
        homePheromonePortrayal.setMap(new sim.util.gui.SimpleColorMap(
                AntsForage.MIN_PHEROMONE,
                AntsForage.MAX_PHEROMONE,
                // home pheromones are beneath all, just make them opaque
                Color.white, //new Color(0,255,0,0),
                new Color(0,255,0,255) ));
        foodPheromonePortrayal.setField(af.toFoodGrid);
        foodPheromonePortrayal.setMap(new sim.util.gui.SimpleColorMap(
                AntsForage.MIN_PHEROMONE,
                AntsForage.MAX_PHEROMONE,
                new Color(0,0,255,0),
                new Color(0,0,255,255) ));
        sitesPortrayal.setField(af.sites);
        sitesPortrayal.setMap(new sim.util.gui.SimpleColorMap(
                0,
                1,
                new Color(0,0,0,0),
                new Color(255,0,0,255) ));
        obstaclesPortrayal.setField(af.obstacles);
        obstaclesPortrayal.setMap(new sim.util.gui.SimpleColorMap(
                0,
                1,
                new Color(0,0,0,0),
                new Color(128,64,64,255) ));
        bugPortrayal.setField(af.buggrid);
            
        // make the ants look like cameras!
        /*
          bugPortrayal.setPortrayalForAll(
          new sim.portrayal.simple.ImagePortrayal2D(
          sim.display.Display2D.CAMERA_ICON.getImage()));
        */
        
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
        display = new Display2D(400,400,this,1); // at 400x400, we've got 4x4 per array position
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
    
    
    
    
