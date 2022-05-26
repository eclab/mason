/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.desexample;
import sim.portrayal.network.*;
import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import sim.field.network.*;
import sim.des.portrayal.*;

public class DESExampleWithUI extends GUIState
    {
    public Display2D display;
	public JFrame displayFrame;
	
    ContinuousPortrayal2D layoutPortrayal = new ContinuousPortrayal2D();
    NetworkPortrayal2D graphPortrayal = new NetworkPortrayal2D();
        
    public static void main(String[] args)
        {
        DESExampleWithUI app = new DESExampleWithUI();
        Console c = new Console(app);
        c.setVisible(true);
        }

    public DESExampleWithUI() { super(new DESExample( System.currentTimeMillis())); }
    public DESExampleWithUI(SimState state) { super(state); }

    public Object getSimulationInspectedObject() { return state; }

	// make the main model inspector volatile so it updates each time?
    /*
    public Inspector getInspector()
        {
        Inspector i = super.getInspector();
        i.setVolatile(true);
        return i;
        }
    */

    public static String getName() { return "DES Example"; }
    
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
        DESExample example = (DESExample) state;
        
        layoutPortrayal.setField(example.field.getNodes());
        graphPortrayal.setField(example.field);
        SimpleEdgePortrayal2D edge = new DelayedEdgePortrayal(); //new ResourceEdgePortrayal(1.0);
        edge.setBaseWidth(1);
        //edge.setShape(SimpleEdgePortrayal2D.SHAPE_TRIANGLE);
        graphPortrayal.setPortrayalForAll(edge);

        //layoutPortrayal.setPortrayalForAll(new MovablePortrayal2D(new RectanglePortrayal2D(5.0, false)));        
        //SimpleEdgePortrayal2D edge = ResourceEdge.getDefaultEdgePortrayal2D(); //new SimpleEdgePortrayal2D(Color.BLUE, Color.RED, Color.BLACK, new Font("SansSerif", Font.PLAIN, 2));
        //edge.setShape(SimpleEdgePortrayal2D.SHAPE_LINE_ROUND_ENDS);

        // reschedule and repaint the displayer
        display.reset();
        display.setBackdrop(Color.white);
        display.repaint();
        }

    public void init(Controller c)
        {
        super.init(c);

        DESPortrayalParameters.setImageClass(DESExampleWithUI.class);

        // make the displayer
        display = new Display2D(600,600,this);
        // turn off clipping
        display.setClipping(false);
        display.attach( graphPortrayal, "Connections" );
        display.attach( layoutPortrayal, "Layout" );

        displayFrame = display.createFrame();
        displayFrame.setTitle("Amazing!");
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
