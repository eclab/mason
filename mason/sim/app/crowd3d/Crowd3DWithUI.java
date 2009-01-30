/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.crowd3d;

import sim.engine.*;
import sim.display.*;
import sim.display3d.*;
import sim.portrayal3d.*;
import sim.portrayal3d.simple.*;
import sim.portrayal3d.continuous.*;
import sim.util.*;
import java.awt.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import javax.swing.*;

public class Crowd3DWithUI extends GUIState
    {
    public JFrame displayFrame; 
    FieldPortrayal3D boidsP;
    Portrayal3D wireFrameP;

    public static void main(String[] args)
        {
        Crowd3DWithUI boids = new Crowd3DWithUI(new CrowdSim(System.currentTimeMillis()));

        Console c = new Console(boids);
        c.setVisible(true);        
        }

    public Crowd3DWithUI()
        {
        this(new CrowdSim(System.currentTimeMillis())); 
        }
    public Crowd3DWithUI(CrowdSim b)
        {
        super(b);
        boidsP = new ContinuousPortrayal3D();
        wireFrameP = new WireFrameBoxPortrayal3D(0,0,0, b.spaceWidth, b.spaceHeight, b.spaceDepth);
        }
    
    public static String getName() { return "Crowd Spacing"; }
        
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
        display.destroySceneGraph();

        boidsP.setField(((CrowdSim)state).boidSpace);
        
        display.reset();

        // rebuild the scene graph
        display.createSceneGraph();        
        }
    
    public Display3D display;

    public void init(Controller c)
        {
        CrowdSim cState = (CrowdSim)state;
        super.init(c);
        display = new Display3D(500,500,this,1);

        display.attach(wireFrameP, "Fish tank");
        Appearance appearance = new Appearance();
        appearance.setColoringAttributes(
            new ColoringAttributes(new Color3f(new Color(0,0,255)), ColoringAttributes.SHADE_GOURAUD));           
        Material m= new Material();
        m.setDiffuseColor(new Color3f(new Color(255,255,0)));
        m.setSpecularColor(0.5f,0.5f,0.5f);
        m.setShininess(64f);
        appearance.setMaterial(m);
        boidsP.setPortrayalForAll(new Shape3DPortrayal3D(new GullCG(),
                appearance)); //new GullPortrayal3D());
                        
        display.attach(boidsP, "boids");
        display.attach(new LightPortrayal3D(new Color(127,127,255), new Double3D(-1,-1,1)), "Light One");
        display.attach(new LightPortrayal3D(new Color(127,255,127), new Double3D(1,-1,-1)), "Light Two");
        display.attach(new LightPortrayal3D(new Color(255,127,127), new Double3D(1,1,-1)), "Light Three");
        display.setShowsSpotlight(false);  // we have our own spotlights
                
        display.translate(-.5*cState.spaceWidth,-.5*cState.spaceHeight,-0.5*cState.spaceDepth);
        display.scale(1.0/Math.max(cState.spaceWidth, Math.max(cState.spaceHeight, cState.spaceDepth)));

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
