/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.heatbugs3d;

import sim.engine.*;
import sim.display.*;
import sim.display3d.*;
import sim.portrayal3d.*;
import sim.portrayal3d.simple.*;
import sim.portrayal3d.grid.*;
import sim.portrayal3d.grid.quad.*;
import sim.app.heatbugs.*;
import sim.util.gui.*;
import java.awt.*;
import javax.swing.*;

/**
 * @author Gabriel Catalin Balan
 *
 * This is a HeatBugs view. 
 *      -the bugs can be displayed STACKED or CLASSIC way (only one shown 
 *       when more on top of one another).
 *      -the heat is both color and altitude coded. You can turn off
 *   altitudes by using NOZ.      
 */
public class HeatBugs3DWithUI extends GUIState
    {
                
    public JFrame displayFrame; 
    public static final int CLASSIC = 0;
    public static final int STACKED = 1;
        
    public static final int TILE = 0;
    public static final int MESH = 1;
    public static final int NOZ  = 2;

    int bugmode = STACKED;
    int heatmode = MESH;

       
    ValueGrid2DPortrayal3D heatPortrayal = new ValueGrid2DPortrayal3D();        
    FieldPortrayal3D bugPortrayal = null;
    QuadPortrayal quadP = null;
     
    public static void main(String[] args)
        {
        HeatBugs3DWithUI heatbugs = 
            new HeatBugs3DWithUI(       new HeatBugs( System.currentTimeMillis(),
                    100,100,100),
                HeatBugs3DWithUI.STACKED, 
//                                      HeatBugs3DWithUI.CLASSIC,
                                         
//                                      HeatBugs3DWithUI.TILE);
//                                      HeatBugs3DWithUI.NOZ);
                HeatBugs3DWithUI.MESH);
                                                                                                                
        Console c = new Console(heatbugs);
        c.setVisible(true);        
        }

    public HeatBugs3DWithUI() 
        { 
        this(new HeatBugs(System.currentTimeMillis()), STACKED, MESH); 
        }

    public HeatBugs3DWithUI(SimState state, final int bugmode, final int heatmode)
        {
        super(state); 
        this.bugmode = bugmode;
        this.heatmode = heatmode;

        // Here we define the bugPortrayal so it can be attached in init().  Otherwise
        // if we defined it in start(), we'd have to detatch the bugPortrayal and reattach
        // a new one, which is kinda dorky.
        //
        // The particular heatmode can be done in start() without any trouble.
        if (bugmode == STACKED)
            bugPortrayal = new SparseGrid2DPortrayal3D();
        else // if bugmode == CLASSIC
            bugPortrayal = new SparseGridPortrayal3D();
        }

    public static String getName()
        {
        return "3D HeatBugs";
        }
    
    public void start()
        {
        super.start();
        setupPortrayals();
        }
     
    public void load(SimState state)
        {
        super.load(state);
        // we now have new grids.  Set up the portrayals to reflect that
        setupPortrayals();
        }
        
    public void setupPortrayals()
        {
        display.destroySceneGraph();

        // determine 
        SimpleColorMap cm = new SimpleColorMap();
        cm.setLevels(0.0,HeatBugs.MAX_HEAT,Color.blue,Color.red);
                
        // specify that the "bugs" are going to be cones pointing straight up.
        TransformedPortrayal3D p = new TransformedPortrayal3D(new ConePortrayal3D());
        p.rotateX(90.0);
        bugPortrayal.setPortrayalForAll(p);
        
        // the heat can be tiles, meshes, or tiles with no change in height (NOZ).
        // Specify which one here.
        switch(heatmode)
            {
            case TILE : quadP = new TilePortrayal(cm, 1f/2000); break;
            case MESH : quadP = new MeshPortrayal(cm, 1f/2000); break;
            case NOZ :  
                quadP = new TilePortrayal(cm);   // no height changes, but we need to raise the bugs a little bit
                bugPortrayal.translate(0,0,1.0f);
                break;
            }
        heatPortrayal.setPortrayalForAll(quadP);
// With this line we can tell the bug portrayal to use two triangles rather than
// a rect to draw each cell.  See the documentation for ValueGrid2DPortrayal3D for
// why this would be useful and when it is not.
//      heatPortrayal.setUsingTriangles(true);


        heatPortrayal.setField(((HeatBugs)state).valgrid);
        bugPortrayal.setField(((HeatBugs)state).buggrid);


        // reschedule the displayer
        display.reset();        

        // rebuild the scene graph
        display.createSceneGraph();
        }
    
    public Display3D display;

    public void init(Controller c)
        {
        super.init(c);
        // Make the Display3D.  We'll have it display stuff later.
        display = new Display3D(600,600,this,1);
                
        // attach the portrayals to the displayer, from bottom to top
        display.attach(heatPortrayal,"Heat");
        display.attach(bugPortrayal, "Bugs");
        heatPortrayal.valueName = "Heat";
                
        HeatBugs hbState = (HeatBugs)state;
        
        // center the bug graph.  Right now it's located at the (0,0) position.  For
        // example, if it's a 5x5 graph, and the origin is at (0,0), we want to move it
        // to (2,2).  So we want it to be at (  (5-1)/2 = 2,  (5-1/2) = 2  ).  Similarly,
        // if it's a 6x6 graph we want the origin to be at (2.5, 2.5), dead center between
        // the (2,2) and (3,3) grid positions.  To center
        // the origin there, we need to move the graph in the opposite direction.
        // so the general equation for each dimension: (numGridPoints - 1) / -2.0.
        display.translate((hbState.gridWidth - 1)/-2.0, (hbState.gridHeight - 1)/-2.0, 0);
        
        // now let's scale it so it fits inside a 1x1x1 cube centered at the origin.  We don't
        // have to, but it'll look nicer.
        display.scale(1.0/Math.max(hbState.gridWidth,hbState.gridHeight));
        
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
