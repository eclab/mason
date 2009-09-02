/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.networktest;

import sim.portrayal.network.*;
import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.Color;
import javax.swing.event.*;
import java.awt.event.*;
import sim.portrayal.*;
import sim.util.*;
import java.awt.*;
import sim.field.continuous.*;
import java.awt.geom.*;



public class NetworkTestWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args)
        {
        NetworkTestWithUI vid = new NetworkTestWithUI();
        Console c = new Console(vid);
        c.setVisible(true);
        }

    public NetworkTestWithUI() { super(new NetworkTest( System.currentTimeMillis())); }
    public NetworkTestWithUI(SimState state) { super(state); }

    public static String getName() { return "Network Test"; }
    
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
        // tell the portrayals what to portray and how to portray them
        edgePortrayal.setField( new SpatialNetwork2D( ((NetworkTest)state).environment, ((NetworkTest)state).network ) );
        SimpleEdgePortrayal2D p = new SimpleEdgePortrayal2D(Color.lightGray, Color.lightGray, Color.black);
        p.setShape(SimpleEdgePortrayal2D.SHAPE_TRIANGLE);
        p.setBaseWidth(10);
        edgePortrayal.setPortrayalForAll(p);
        nodePortrayal.setField( ((NetworkTest)state).environment );
        
        // Set the nodes in the node portrayal to show a 20-pixel non-scaling 
        // circle around them only when they're being selected (the 'true').
        // the 'null' means "Assume the underlying object is its own portrayal". 
        nodePortrayal.setPortrayalForAll(new sim.portrayal.simple.CircledPortrayal2D(null, 20, 10, Color.green, true));

        // reschedule the displayer
        display.reset();
        display.setBackdrop(Color.white);
                
        // redraw the display
        display.repaint();
        }

    public void init(final Controller c)
        {
        super.init(c);

        // make the displayer
        display = new Display2D(800,600,this,1);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Network Test Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( edgePortrayal, "Edges" );
        display.attach( nodePortrayal, "Nodes" );



        ////////// BEGIN OPTIONAL MOVEMENT CODE

        // In this code we're showing how to augment MASON to enable moving objects around with
        // the mouse.  The general idea is: when we click on a location, we gather all the objects
        // at that location and find the one we want to move around.  Then as we drag, we query
        // MASON for the location in the field that would be equivalent to where the mouse is located,
        // then set the object to that location and redraw everything.
        
        // We augment that simple approach with a little bit of niftyness in the form of 'nodeLocDelta'.
        // What we do is: when the user depressed the mouse on the the object we compute the difference
        // in where he clicked the mouse and the actual origin of the object.  As the user drags around,
        // we always add this difference before setting the object.  This makes it appear that the user
        // can drag the object from any spot on the object -- otherwise the object would "pop" to 
        // center itself at the mouse cursor, which doesn't feel quite right drag-and-drop-wise.
        
        // We also perform Selection of the object we've just dragged, for good measure.
        
        // A somewhat simpler example is show in HeatBugsWithUI.java, which doesn't use 
        // a nodeLocDelta and also doesn't bother to perform selection.
        
        
        
        // add mouse motion listener
        MouseInputAdapter adapter = new MouseInputAdapter()
            {
            Object node = null;                 // the object we're dragging
            LocationWrapper nodeWrapper = null; // the wrapper for the object -- useful for selection
            Double2D nodeLocDelta = null;       // our computed difference to be nifty
            
            // figure out what object we clicked on (if any) and what the
            // computed difference is.
            public void mousePressed(MouseEvent e)
                {
                final Point point = e.getPoint();
                Continuous2D field = (Continuous2D)(nodePortrayal.getField());
                if (field == null) return;
                node = null;
                
                // go through all the objects at the clicked point.  The objectsHitBy method
                // doesn't return objects: it returns LocationWrappers.  You can extract the object
                // by calling getObject() on the LocationWrapper.
                
                Rectangle2D.Double rect = new Rectangle2D.Double( point.x, point.y, 1, 1 );
                
                Bag hit = new Bag();
                nodePortrayal.hitObjects(display.getDrawInfo2D(nodePortrayal, rect), hit);
                if (hit.numObjs > 0)
                    {
                    nodeWrapper = ((LocationWrapper)hit.objs[hit.numObjs - 1]);  // grab the topmost one from the user's perspective
                    node = nodeWrapper.getObject();
                    display.performSelection(nodeWrapper); 
                        
                    Double2D nodeLoc = (Double2D)(field.getObjectLocation(node));   // where the node is actually located
                    Double2D mouseLoc = nodePortrayal.getLocation(display.getDrawInfo2D(nodePortrayal, point));  // where the mouse clicked
                    nodeLocDelta = new Double2D(nodeLoc.x - mouseLoc.x, nodeLoc.y - mouseLoc.y);
                    }
                c.refresh();                    // get the other displays and inspectors to update their locations
                // we need to refresh here only in order to display that the node is now selected
                // btw: c must be final.
                }
                
            public void mouseReleased(MouseEvent e)
                {
                node = null;
                }


            // We move the node in our Field, adding in the computed difference as necessary
            public void mouseDragged(MouseEvent e)
                {
                final Point point = e.getPoint();
                Continuous2D field = (Continuous2D)(nodePortrayal.getField());
                if (node==null || field == null) return;
                
                Double2D mouseLoc = nodePortrayal.getLocation(display.getDrawInfo2D(nodePortrayal, point));  // where the mouse dragged to
                Double2D newBallLoc = new Double2D(nodeLocDelta.x + mouseLoc.x, nodeLocDelta.y + mouseLoc.y);  // add in computed difference
                field.setObjectLocation(node, newBallLoc); 
                c.refresh();                                // get the other displays and inspectors to update their locations
                // btw: c must be final.
                }
            };
        
        // We then attach our listener to the "INSIDE DISPLAY" that's part of the Display2D.  The insideDisplay
        // is the object inside the scrollview which does the actual drawing.
        display.insideDisplay.addMouseListener(adapter);
        display.insideDisplay.addMouseMotionListener(adapter);
        
        
        ////////// END MOVEMENT CODE
        
        
        



        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
