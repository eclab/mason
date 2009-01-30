/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.JFrame;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.gui.SimpleColorMap;

/**
   @author Ankur Desai and Joey Harrison
*/
public class PSOWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    public static void main(String[] args)
        {
        PSOWithUI po = new PSOWithUI();  // randomizes by currentTimeMillis
        Console c = new Console(po);
        c.setVisible(true);
        }

    public Object getSimulationInspectedObject() { return state; }  // non-volatile
    
    public static String getName() { return "Particle Swarm Optimization"; }

    ContinuousPortrayal2D swarmPortrayal = new ContinuousPortrayal2D();
    
    public PSOWithUI()
        {
        super(new PSO(System.currentTimeMillis()));
        }
    
    public PSOWithUI(SimState state) 
        {
        super(state);
        }

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
        PSO swarm = (PSO)state;
        final SimpleColorMap map = new SimpleColorMap(
            swarm.fitnessFunctionLowerBound[swarm.fitnessFunction], 1000, Color.blue, Color.red);
         
        // obstacle portrayal needs no setup
        swarmPortrayal.setField(swarm.space);
        
        // make the flockers random colors and four times their normal size (prettier)
        for(int x=0;x<swarm.space.allObjects.numObjs;x++)
            {
            final Particle p = (Particle)(swarm.space.allObjects.objs[x]);
            swarmPortrayal.setPortrayalForObject(p,
                new RectanglePortrayal2D(Color.green,0.05)
                    {
                    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
                        {
                        paint = map.getColor(p.getFitness());
                        super.draw(object,graphics,info);
                        }
                    });
            }
        
        // update the size of the display appropriately.
        double w = swarm.space.getWidth();
        double h = swarm.space.getHeight();
        if (w == h)
            { display.insideDisplay.width = display.insideDisplay.height = 750; }
        else if (w > h)
            { display.insideDisplay.width = 750; display.insideDisplay.height = 750 * (h/w); }
        else if (w < h)
            { display.insideDisplay.height = 750; display.insideDisplay.width = 750 * (w/h); }
            
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }

    public void init(Controller c)
        {
        super.init(c);

        // make the displayer
        display = new Display2D(750,750,this,1);
        display.setBackdrop(Color.black);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Particle Swarm Optimization");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach(swarmPortrayal, "Behold the Swarm!", 
            (display.insideDisplay.width * 0.5), (display.insideDisplay.height * 0.5), true);
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
