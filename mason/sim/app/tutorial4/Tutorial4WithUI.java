/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial4;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import sim.portrayal.*;
import java.awt.*;
import javax.swing.*;

public class Tutorial4WithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;
    
    public Display2D display2;
    public JFrame displayFrame2;

    SparseGridPortrayal2D particlesPortrayal = new SparseGridPortrayal2D();
    SparseGridPortrayal2D particlesPortrayal2 = new SparseGridPortrayal2D();
    FastValueGridPortrayal2D trailsPortrayal = new FastValueGridPortrayal2D("Trail");

    public static void main(String[] args)
        {
        Tutorial4WithUI t = new Tutorial4WithUI();
        Console c = new Console(t);
        c.setVisible(true);
        }
    
    public Tutorial4WithUI() { super(new Tutorial4(System.currentTimeMillis())); }
    
    public Tutorial4WithUI(SimState state) { super(state); }
    
    public static String getName() { return "Tutorial4: Particles"; }
    
// We comment this out of the example, which will cause MASON to look
// for a file called "index.html" in the same directory -- which we've
// included for consistency with the other applications in the demo 
// apps directory.

/*
  public static Object getInfoByClass(Class theClass)
  {
  return "<H2>Tutorial4</H2><p>An odd little particle-interaction example.";
  }
*/
    
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
        // we now have new grids.  Set up the portrayals to reflect that
        setupPortrayals();
        }
        
    // This is called by start() and by load() because they both had this code
    // so I didn't have to type it twice :-)
    public void setupPortrayals()
        {
        // tell the portrayals what to portray and how to portray them
        trailsPortrayal.setField(((Tutorial4)state).trails);
        trailsPortrayal.setMap(
            new sim.util.gui.SimpleColorMap(
                0.0,1.0,Color.black,Color.white));
        particlesPortrayal.setField(((Tutorial4)state).particles);
        particlesPortrayal.setPortrayalForClass(
            Particle.class, new sim.portrayal.simple.OvalPortrayal2D(Color.green) );
        particlesPortrayal.setPortrayalForClass(
            BigParticle.class, new sim.portrayal.simple.RectanglePortrayal2D(Color.red, 1.5)
                {
                public Inspector getInspector(LocationWrapper wrapper, GUIState state)
                    {
                    // make the inspector
                    return new BigParticleInspector(super.getInspector(wrapper,state), wrapper, state);
                    }
                });
        particlesPortrayal2.setField(((Tutorial4)state).particles);
        particlesPortrayal2.setPortrayalForAll( new sim.portrayal.simple.RectanglePortrayal2D(Color.green) );
                   
        // reschedule the displayer
        display.reset();
        display2.reset();
        
        // redraw the display
        display.repaint();
        display2.repaint();
        }
    
    public void init(Controller c)
        {
        super.init(c);
        
        display = new Display2D(400,400,this,1);
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);
        display.setBackdrop(Color.black);
        display.attach(trailsPortrayal,"Trails");
        display.attach(particlesPortrayal,"Particles");

        display2 = new Display2D(400,600,this,1);
        displayFrame2 = display2.createFrame();
        displayFrame2.setTitle("The Other Display");
        c.registerFrame(displayFrame2);
        displayFrame2.setVisible(true);
        display2.setBackdrop(Color.blue);
        display2.attach(particlesPortrayal2,"Squished Particles!");
        }

    public Object getSimulationInspectedObject()
        {
        return state;
        }

    public Inspector getInspector()
        {
        Inspector i = super.getInspector();
        i.setVolatile(true);
        return i;
        }
    }
    
    
    
    
    
