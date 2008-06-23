/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.mousetraps3d;

import sim.engine.*;
import sim.display.*;
import sim.display3d.*;
import sim.app.mousetraps.*;
import sim.util.gui.*;
import sim.portrayal3d.grid.*;
import sim.portrayal3d.simple.*;
import sim.portrayal3d.continuous.*;
import sim.portrayal3d.grid.quad.*;
import java.awt.*;
import javax.swing.*;

public class MouseTraps3DWithUI extends GUIState
    {
    public JFrame displayFrame; 
        
    ValueGrid2DPortrayal3D trapsPortrayal = new ValueGrid2DPortrayal3D();
    ContinuousPortrayal3D ballPortrayal = new ContinuousPortrayal3D();
    WireFrameBoxPortrayal3D wireFrameP;

    public static void main(String[] args)
        {
        MouseTraps3DWithUI simGUI = new MouseTraps3DWithUI();
        Console c = new Console(simGUI);
        c.setVisible(true);        
        }
        
    public MouseTraps3DWithUI() 
        { 
        super(new MouseTraps(System.currentTimeMillis(), 40.0,15,10,120.0,80.0,false)); 
        // ... or...
//      super(new MouseTraps(System.currentTimeMillis(), 3.9,100,100,10.0,10.0,false)); 
        }
    
    double scale;

    public static String getName() { return "3D Mouse Traps"; }
        
    public void start()
        {
        super.start();
        setup3DPortrayals();
        }
         
    public void load(SimState state)
        {
        super.load(state);
        // we now have new grids.  Set up the portrayals to reflect that
        setup3DPortrayals();
        }
            
    public void setup3DPortrayals()
        {       
        display.destroySceneGraph();

        trapsPortrayal.setField(((MouseTraps)state).trapStateGrid);
        ballPortrayal.setField(((MouseTraps)state).ballSpace);
        ballPortrayal.setPortrayalForAll(new SpherePortrayal3D(Color.green));

        // rebuild the scene graph
        display.createSceneGraph();

        // reschedule the displayer
        display.reset();        
        }
        
    public Display3D display;
        
    public void init(Controller c)
        {
        super.init(c);
        
        /// Build the portrayals
        
        MouseTraps sim = (MouseTraps) state;
        trapsPortrayal.setField(sim.trapStateGrid);
        SimpleColorMap map = new SimpleColorMap();
        map.setLevels(0.0,1.0,Color.blue,Color.gray);
        trapsPortrayal.setPortrayalForAll(new TilePortrayal(map));

        // the trapsPortrayal is a grid portrayal.  These suckers by default have the
        // CENTER of their <0,0> grid element at the origin:
        //
        // C-------+-------+---
        // |       |       |
        // |   x   |       |  ...
        // |       |       | 
        // +-------+-------+
        // |       |
        // |  ...
        //
        // The center is marked with an x.  We want the origin to be at the CORNER
        // of the <0,0> grid element (marked with a C) so that the corner lines up
        // with the <0,0> position in the continuous wireframe space.  To do this
        // we need to translate the trapsPortrayal by 0.5 units in the x and y 
        // directions each (a grid element is 1 unit).
        
        trapsPortrayal.translate(0.5,0.5,0);
        
        // Now keep in mind that the grid elements are 1 unit each.  This is much
        // smaller scale than the continuous space.  We need to scale up the
        // trapsPortrayal so that one grid element is equal to the right number
        // of continuous space units.  The easiest way to do this is just to scale
        // by the ratio of their relative widths (or relative heights).
        
        trapsPortrayal.scale(sim.spaceWidth / sim.trapGridWidth);
        
        // Now we build the ball portrayal (the continuous3D space)
        ballPortrayal.setField(sim.ballSpace);

        // finally we'll build a wireframe around it all
        
        wireFrameP = new WireFrameBoxPortrayal3D(0,0,0,sim.spaceWidth, sim.spaceHeight, sim.spaceLength);

        // Make the Display3D.  We'll have it display stuff later.
        display = new Display3D(600,600,this,1);
                
        // attach the portrayals to the displayer, from bottom to top
        display.attach(trapsPortrayal,"Traps");
        display.attach(ballPortrayal, "Balls");
        display.attach(wireFrameP, "Fish tank");
        
        // translate the whole kit and caboodle into the center
        display.translate(-sim.spaceWidth/2, -sim.spaceHeight/2, -sim.spaceLength/2);
        // scale it down to some reasonable value, say, the maximal dimension of the boxes
        display.scale(1/Math.max(sim.spaceHeight, Math.max(sim.spaceWidth, sim.spaceLength)));

        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);
        }
        
    public void quit()
        {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  
        display = null;       
        }
        
        
    }
