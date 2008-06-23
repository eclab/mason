/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims3d;

import sim.portrayal3d.continuous.*;
import sim.portrayal3d.simple.*;
import sim.engine.*;
import sim.display.*;
import sim.display3d.*;

import javax.swing.*;

public class WoimsDemo3DWithUI extends GUIState
    {
    public Display3D display;
    public JFrame displayFrame;

    public static String getName() { return "3D Woims"; }

    public static void main(String[] args)
        {
        WoimsDemo3DWithUI woims = new WoimsDemo3DWithUI(new WoimsDemo3D(System.currentTimeMillis()));
        Console c = new Console(woims);
        c.setVisible(true);
        }
                
    public WoimsDemo3DWithUI()
        {
        this(new WoimsDemo3D(System.currentTimeMillis()));
        }

    ContinuousPortrayal3D p2;
    WireFrameBoxPortrayal3D wireFrameP;

    public WoimsDemo3DWithUI(SimState state)
        {
        super(state);
        WoimsDemo3D wd = (WoimsDemo3D) state;
        p2 = new ContinuousPortrayal3D();
        p2.setField(wd.environment); 

        // build the box
        wireFrameP = new WireFrameBoxPortrayal3D(-WoimsDemo3D.EXTRA_SPACE,-WoimsDemo3D.EXTRA_SPACE,-WoimsDemo3D.EXTRA_SPACE,wd.environment.width + 2*WoimsDemo3D.EXTRA_SPACE,wd.environment.height + 2*WoimsDemo3D.EXTRA_SPACE,wd.environment.length + 2*WoimsDemo3D.EXTRA_SPACE);
        }

        
    public void start()
        {
        super.start();  // clear out the schedule
        setupPortrayals();
        }
        
    public void load(SimState state)
        {
        super.load(state);  // clear out the schedule
        setupPortrayals();
        }
        
    public void setupPortrayals()
        {
        display.destroySceneGraph();
        p2.setField(((WoimsDemo3D) state).environment);

        display.createSceneGraph();
        display.reset();
        }

    public void init(Controller c)
        {
        super.init(c);
        display = new Display3D(600,600,this,1);
        
        display.attach(p2,"Woims");
        display.attach(wireFrameP, "WireFrame");

        display.translate(-100,-100,-100);
        display.scale(1.0/200);

        displayFrame = display.createFrame();

        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
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
