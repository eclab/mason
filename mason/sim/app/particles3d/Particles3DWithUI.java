/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.particles3d;
import sim.engine.*;
import sim.display3d.*;
import sim.display.*;
import sim.portrayal3d.grid.*;
import sim.portrayal3d.simple.*;
import java.awt.*;
import javax.swing.*;

public class Particles3DWithUI extends GUIState
    {
    public Display3D display;
    public JFrame displayFrame;

    SparseGridPortrayal3D particlesPortrayal = new SparseGridPortrayal3D();
    ValueGridPortrayal3D trailsPortrayal = new ValueGridPortrayal3D("Trail"); 
    WireFrameBoxPortrayal3D wireFramePortrayal; 
        

    public static void main(String[] args)
        {
        Particles3DWithUI t = new Particles3DWithUI();
        Console c = new Console(t);
        c.setVisible(true);
        }
    
    public Particles3DWithUI() { super(new Particles3D(System.currentTimeMillis())); }
    
    public Particles3DWithUI(SimState state) { super(state); }
    
    public static String getName() { return "3D Particles"; }
    
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
        }

    public void start()
        {
        super.start();
        // set up our portrayals
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

        particlesPortrayal.setField(((Particles3D)state).particles);
        particlesPortrayal.setPortrayalForAll(new SpherePortrayal3D(Color.red)); 
        
        trailsPortrayal.setField(((Particles3D)state).trails); 
        trailsPortrayal.setMap(new sim.util.gui.SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), Color.black)); 

        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.createSceneGraph();
        }
    
    public void init(Controller c)
        {
        super.init(c);
        display = new Display3D(600,600,this,1); 

        wireFramePortrayal = new WireFrameBoxPortrayal3D(-0.5,-0.5,-0.5,Particles3D.gridWidth, Particles3D.gridHeight, Particles3D.gridLength, Color.blue); 
        
        // attach the portrayals
        display.attach(wireFramePortrayal,"Wire Frame"); 
        display.attach(particlesPortrayal,"Particles");
        display.attach(trailsPortrayal, "Trails"); 

        display.translate(-Particles3D.gridWidth/2, 
            -Particles3D.gridHeight/2, 
            -Particles3D.gridLength/2); 
        
        display.scale(1.0/Particles3D.gridWidth); 

        display.setBackdrop(Color.white);
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        }
    }
    
    
    
    
    
