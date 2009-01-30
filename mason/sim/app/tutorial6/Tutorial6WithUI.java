/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial6;
import sim.portrayal3d.continuous.*;
import sim.portrayal3d.simple.*;
import sim.engine.*;
import sim.display.*;
import sim.display3d.*;
import javax.swing.*;
import java.awt.*;
import sim.util.*;

public class Tutorial6WithUI extends GUIState
    {
    public Display3D display;
    public JFrame displayFrame;
    
    ContinuousPortrayal3D bodyPortrayal = new ContinuousPortrayal3D();

    public static void main(String[] args)
        {
        Tutorial6WithUI vid = new Tutorial6WithUI();
        Console c = new Console(vid);
        c.setVisible(true);
        }

    public Tutorial6WithUI() { super(new Tutorial6( System.currentTimeMillis())); }
    public Tutorial6WithUI(SimState state) { super(state); }

    public static String getName() { return "Tutorial 6: Planets"; }

// We comment this out of the example, which will cause MASON to look
// for a file called "index.html" in the same directory -- which we've
// included for consistency with the other applications in the demo 
// apps directory.

/*
  public static Object getInfoByClass(Class theClass) { return "<H2>Tutorial 6</H2> Planetary Orbits"; }
*/

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

    public void quit()
        {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    /** Gets an image relative to the tutorial6 directory */
    public static Image loadImage(String filename)
        { 
        return new ImageIcon(Tutorial6.class.getResource(filename)).getImage(); 
        }
    
    public void setupPortrayals()
        {
        display.destroySceneGraph();
        Tutorial6 tut = (Tutorial6) state;
        bodyPortrayal.setField(tut.bodies);
        
        // make individual portrayals of proper size
        Bag objs = tut.bodies.getAllObjects();
        
//        Color colors[] = {Color.yellow, Color.white, Color.green , Color.blue, Color.red, Color.orange, 
//                          Color.magenta, Color.cyan, Color.pink, Color.white};
        
        String imageNames[] = {"sunmap.jpg","mercurymap.jpg","venusmap.jpg","earthmap.jpg","marsmap.jpg","jupitermap.jpg","saturnmap.jpg","uranusmap.jpg","neptunemap.jpg","plutomap.jpg"};

/*
  for(int i=0;i<10;i++)
  bodyPortrayal.setPortrayalForObject(
  objs.objs[i], new SpherePortrayal3D(colors[i], 
//               objs.objs[i], new SpherePortrayal3D(loadImage(imageNames[i]),
(float) (Math.log(Tutorial6.DIAMETER[i])*50),
50));
*/

     for(int i=0;i<10;i++)
         {
         TransformedPortrayal3D trans =
             new TransformedPortrayal3D(new SpherePortrayal3D(loadImage(imageNames[i]),
                     (float) (Math.log(Tutorial6.DIAMETER[i])*50), 
                     50));
            
         trans.rotateX(90.0); // move pole from Y axis up to Z axis
         bodyPortrayal.setPortrayalForObject(objs.objs[i], trans);
         }

     display.reset();
     display.createSceneGraph();
        } 

    public void init(Controller c)
        {
        super.init(c);

        Tutorial6 tut = (Tutorial6) state;
        bodyPortrayal.setField(tut.bodies);

        display = new Display3D(600,600,this,1);
        display.attach(bodyPortrayal, "The Solar System");
        display.scale(1.0/(Tutorial6.DISTANCE[Tutorial6.PLUTO]*1.05));  // give a little room (1.05) to see pluto
        
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        }

    }
