/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.balls3d;
import sim.portrayal3d.network.*;
import sim.portrayal3d.continuous.*;
import sim.portrayal3d.simple.*; 
import sim.display3d.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.*;

public class Balls3DWithUI extends GUIState
    {
    public Display3D display;
    public JFrame displayFrame;

    NetworkPortrayal3D edgePortrayal = new NetworkPortrayal3D();
    ContinuousPortrayal3D nodePortrayal = new ContinuousPortrayal3D();
    LightPortrayal3D light; 

    public static void main(String[] args)
        {
        Balls3DWithUI vid = new Balls3DWithUI();
        Console c = new Console(vid);
        c.setVisible(true);
        }

    public Balls3DWithUI() { super(new Balls3D( System.currentTimeMillis())); }
    public Balls3DWithUI(SimState state) 
        { 
        super(state); 
        light = new LightPortrayal3D(Color.white, new sim.util.Double3D(-2,-3,-1)); 
        }

    public static String getName() { return "3D Balls and Bands"; }
    
    public Object getSimulationInspectedObject() { return state; }

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
        Balls3D tut = (Balls3D) state;
        
        final java.text.NumberFormat strengthFormat = java.text.NumberFormat.getInstance();
        strengthFormat.setMinimumIntegerDigits(1);
        strengthFormat.setMaximumFractionDigits(2);
        
        // tell the portrayals what to portray and how to portray them
        edgePortrayal.setField( new SpatialNetwork3D( tut.balls, tut.bands ) );
        edgePortrayal.setPortrayalForAll(
            new SimpleEdgePortrayal3D()
                {
                public String getLabel(sim.field.network.Edge e)
                    {
                    return strengthFormat.format(e.getWeight());
                    }
                }); 

        nodePortrayal.setField( tut.balls );
        nodePortrayal.setPortrayalForAll(new BallPortrayal(5)); 
                        
        display.createSceneGraph(); 
        display.reset();
        }

    public void init(Controller c)
        {
        super.init(c);

        Balls3D tut = (Balls3D) state;

        light = new LightPortrayal3D(Color.white, new sim.util.Double3D(-2,-3,-1)); 

        // make the displayer
        display = new Display3D(600,600,this,1);
        display.attach( edgePortrayal, "Bands" );
        display.attach( nodePortrayal, "Balls" );
        display.attach( light, "Spotlight" ); 

        display.translate(-tut.gridWidth/2,
                          -tut.gridHeight/2,
                          -tut.gridLength/2);
        
        display.scale(1.0/tut.gridWidth);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Balls and Bands");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        display.mSelectBehavior.setTolerance(10.0f);
        }

    public void quit()
        {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
