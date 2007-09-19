/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.crowd3d;

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
    public JFrame mDisplayFrame; 
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
        boidsP.setField(((CrowdSim)state).boidSpace);
        
        mDisplay.reset();

        // rebuild the scene graph
        mDisplay.createSceneGraph();        
        }
    
    public Display3D mDisplay;

    public void init(Controller c)
        {
        CrowdSim cState = (CrowdSim)state;
        super.init(c);
        mDisplay = new Display3D(500,500,this,1);

        mDisplay.attach(wireFrameP, "Fish tank");
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
                        
        mDisplay.attach(boidsP, "boids");
        mDisplay.attach(new LightPortrayal3D(new Color(127,127,255), new Double3D(-1,-1,1)), "Light One");
        mDisplay.attach(new LightPortrayal3D(new Color(127,255,127), new Double3D(1,-1,-1)), "Light Two");
        mDisplay.attach(new LightPortrayal3D(new Color(255,127,127), new Double3D(1,1,-1)), "Light Three");
        mDisplay.setShowsSpotlight(false);  // we have our own spotlights
                
        mDisplay.translate(-.5*cState.spaceWidth,-.5*cState.spaceHeight,-0.5*cState.spaceDepth);
        mDisplay.scale(1.0/Math.max(cState.spaceWidth, Math.max(cState.spaceHeight, cState.spaceDepth)));

        mDisplayFrame = mDisplay.createFrame();
        c.registerFrame(mDisplayFrame);   // register the frame so it appears in the "Display" list
        mDisplayFrame.setVisible(true);
        }
        
    public void quit()
        {
        super.quit();

        if (mDisplayFrame!=null) mDisplayFrame.dispose();
        mDisplayFrame = null;  
        mDisplay = null;       
        }


    }
