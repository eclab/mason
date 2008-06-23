/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial7;
import sim.portrayal3d.grid.*;
import sim.portrayal3d.grid.quad.*;
import sim.portrayal3d.simple.*;
import sim.engine.*;
import sim.display.*;
import sim.display3d.*;
import sim.util.gui.*;
import javax.swing.*;
import java.awt.*;

public class Tutorial7WithUI extends GUIState
    {
    public Display3D display;
    public JFrame displayFrame;
    Tutorial7 tutorial7;
    
    SparseGridPortrayal3D fliesPortrayal = new SparseGridPortrayal3D();
    ValueGrid2DPortrayal3D xProjectionPortrayal = new ValueGrid2DPortrayal3D("X Projection");
    ValueGrid2DPortrayal3D yProjectionPortrayal = new ValueGrid2DPortrayal3D("Y Projection");
    ValueGrid2DPortrayal3D zProjectionPortrayal = new ValueGrid2DPortrayal3D("Z Projection");

    public static void main(String[] args)
        {
        Tutorial7WithUI t = new Tutorial7WithUI();
        Console c = new Console(t);
        c.setVisible(true);
        }

    public Tutorial7WithUI() { super(new Tutorial7( System.currentTimeMillis())); }
    public Tutorial7WithUI(SimState state) { super(state); }
    public static String getName() { return "Tutorial 7: Projections"; }

// We comment this out of the example, which will cause MASON to look
// for a file called "index.html" in the same directory -- which we've
// included for consistency with the other applications in the demo 
// apps directory.

/*
  public static Object getInfoByClass(Class theClass) { return "<H2>Tutorial 7</H2> Projections of randomly moving stuff!  Woohoo!"; }
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

    public void setupPortrayals()
        {
        display.destroySceneGraph();

        Tutorial7 tut = (Tutorial7) state;
        
        fliesPortrayal.setField(tut.flies);
        xProjectionPortrayal.setField(tut.xProjection);
        yProjectionPortrayal.setField(tut.yProjection);
        zProjectionPortrayal.setField(tut.zProjection);

        display.reset();
        display.createSceneGraph();
        }

    public void quit()
        {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    public void init(Controller c)
        {
        super.init(c);

        Tutorial7 tut = (Tutorial7) state;

        // the flies will be white spheres, half normal size
        fliesPortrayal.setPortrayalForAll(new SpherePortrayal3D(0.5f));
        
        // X projection: 
        // go from green to yellow, semitransparent.
        SimpleColorMap map = new SimpleColorMap(0.0,4.0, Color.green, Color.yellow);
        xProjectionPortrayal.setPortrayalForAll(new TilePortrayal(map));
        xProjectionPortrayal.setTransparency(0.8f);  // a little transparent
        // rotate it in place and back it up a little
        xProjectionPortrayal.translate(0,0,-1);
        xProjectionPortrayal.rotateX(90);
        xProjectionPortrayal.rotateZ(90);  // swing around Z axis
        
        // Y projection: 
        // go from blue to yellow, opaque, stairstep-style, scale = 1
        map = new SimpleColorMap(0.0,4.0,Color.blue,Color.yellow);
        yProjectionPortrayal.setPortrayalForAll(new TilePortrayal(map,1.0f));
        // rotate it in place and back it up a little
        yProjectionPortrayal.translate(0,0,1);
        yProjectionPortrayal.rotateX(90);

        // Z projection:
        // go from red to blue, opaque, landscape-style (mesh grid), scale = 1/2 (but pointing down)
        map = new SimpleColorMap(0.0,4.0,Color.red,Color.blue);
        zProjectionPortrayal.setPortrayalForAll(new MeshPortrayal(map,-0.5f));
        // back it up a little (it's already in the right rotation)
        zProjectionPortrayal.translate(0,0,-1);
        
        // Make the Z projection use triangles rather than quads
        zProjectionPortrayal.setUsingTriangles(true);
                
        // Change the Z projection to display an image instead.  :-)
        zProjectionPortrayal.setImage(sim.app.tutorial6.Tutorial6WithUI.loadImage("earthmap.jpg"));

        // make the display
        display = new Display3D(600,600,this,1);
        display.attach(fliesPortrayal,"Flies");
        display.attach(xProjectionPortrayal,"X Projection");
        display.attach(yProjectionPortrayal,"Y Projection");
        display.attach(zProjectionPortrayal,"Z Projection");
        
        // scale down the display to fit in the 2x2x2 cube
        float scale = Math.max(Math.max(tut.width,tut.height),tut.length);
        display.scale(1f/scale);

        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);
        }
    }
