/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso3d;

import java.awt.Color;
import javax.swing.JFrame;
import sim.display.Console;
import sim.display.Controller;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.util.gui.SimpleColorMap;
import sim.display3d.*;
import sim.portrayal3d.simple.*;
import sim.portrayal3d.continuous.*;
import javax.media.j3d.*;

/**
   @author Ankur Desai and Joey Harrison
*/
public class PSO3DWithUI extends GUIState
    {
    public Display3D display;
    public JFrame displayFrame;

    public static void main(String[] args)
        {
        PSO3DWithUI pso = new PSO3DWithUI(); 
        Console c = new Console(pso);
        c.setVisible(true);
        }

    public Object getSimulationInspectedObject() { return state; }

    public static String getName() { return "Particle Swarm Optimization 3D"; }

    ContinuousPortrayal3D swarmPortrayal = new ContinuousPortrayal3D();

    public PSO3DWithUI()
        {
        super(new PSO3D(System.currentTimeMillis()));
        }

    public PSO3DWithUI(SimState state)
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
        display.destroySceneGraph();
        PSO3D swarm = (PSO3D) state;
        final SimpleColorMap map = new SimpleColorMap(
            swarm.fitnessFunctionLowerBound[swarm.fitnessFunction], 1000, Color.blue, Color.red);

        swarmPortrayal.setField(swarm.space);

        for (int x = 0; x < swarm.space.allObjects.numObjs; x++)
            {
            final Particle3D p = (Particle3D) (swarm.space.allObjects.objs[x]);
            swarmPortrayal.setPortrayalForObject(p, new CubePortrayal3D(Color.green, 0.05f) 
                {
                public TransformGroup getModel(Object obj,TransformGroup j3dModel)
                    {
                    appearance = appearanceForColor(map.getColor(p.getFitness()));
                    TransformGroup model = super.getModel(obj, j3dModel);
                    Shape3D shape = (Shape3D) (model.getChild(0));
                    shape.setAppearance(appearance);
                    return model;
                    }
                });
            }

        display.attach(new WireFrameBoxPortrayal3D(-5.12, -5.12, -5.12, 5.12, 5.12, 5.12), "Bounds");
                
        display.createSceneGraph();
        display.reset();

        }

    public void init(Controller c)
        {
        super.init(c);

        double w = 10.24;

        display = new Display3D(600, 600, this, 1);
        display.attach(swarmPortrayal, "Swarm");

        display.scale(1.0 / w);

        displayFrame = display.createFrame();
        displayFrame.setTitle("PSO 3D Display");
        c.registerFrame(displayFrame); // register the frame so it appears in
        // the "Display" list
        displayFrame.setVisible(true);
        }

    public void quit()
        {
        super.quit();

        if (displayFrame != null)
            displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
