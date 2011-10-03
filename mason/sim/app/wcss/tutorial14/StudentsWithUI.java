/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.wcss.tutorial14;
import sim.portrayal.network.*;
import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;

import sim.display3d.*;
import sim.portrayal3d.continuous.*;
import sim.portrayal3d.network.*;
import sim.portrayal3d.simple.*;
import java.text.*;
import sim.field.network.*;

public class StudentsWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    ContinuousPortrayal2D yardPortrayal = new ContinuousPortrayal2D();
    NetworkPortrayal2D buddiesPortrayal = new NetworkPortrayal2D();
        
        
    public Display3D display3d;
    public JFrame displayFrame3d;
    ContinuousPortrayal3D agitatedYardPortrayal = new ContinuousPortrayal3D();
    NetworkPortrayal3D agitatedBuddiesPortrayal = new NetworkPortrayal3D();


    public static void main(String[] args)
        {
        StudentsWithUI vid = new StudentsWithUI();
        Console c = new Console(vid);
        c.setVisible(true);
        }

    public StudentsWithUI() { super(new Students( System.currentTimeMillis())); }
    public StudentsWithUI(SimState state) { super(state); }

    public Object getSimulationInspectedObject() { return state; }

    public Inspector getInspector()
        {
        Inspector i = super.getInspector();
        i.setVolatile(true);
        return i;
        }

    public static String getName() { return "WCSS Tutorial 14: Student Cliques (in 3D)"; }
    
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
        Students students = (Students) state;
        
        // tell the portrayals what to portray and how to portray them
        yardPortrayal.setField( students.yard );
        yardPortrayal.setPortrayalForAll(
            new MovablePortrayal2D(
                new CircledPortrayal2D(
                    new LabelledPortrayal2D(
                        new OvalPortrayal2D()
                            {
                            public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
                                {
                                Student student = (Student)object;

                                int agitationShade = (int) (student.getAgitation() * 255 / 10.0);
                                if (agitationShade > 255) agitationShade = 255;
                                paint = new Color(agitationShade, 0, 255 - agitationShade);
                                super.draw(object, graphics, info);
                                }
                            }, 
                        5.0, null, Color.black, true),
                    0, 5.0, Color.green, true)));
                                                

        
        buddiesPortrayal.setField( new SpatialNetwork2D( students.yard, students.buddies ) );
        buddiesPortrayal.setPortrayalForAll(new SimpleEdgePortrayal2D());

        // reschedule the displayer
        display.reset();
        display.setBackdrop(Color.white);

        // redraw the display
        display.repaint();
                
                
        agitatedYardPortrayal.setField( students.agitatedYard );
        agitatedYardPortrayal.setPortrayalForAll(new ConePortrayal3D(Color.red, 2.0));

        agitatedBuddiesPortrayal.setField( new SpatialNetwork3D( students.agitatedYard, students.buddies ) );
        SimpleEdgePortrayal3D ep = new CylinderEdgePortrayal3D()
            {
            DecimalFormat format = new DecimalFormat("#.##");
                                
            public String getLabel(Edge edge)
                {
                return "" + format.format(edge.getWeight());
                }
            };
                        
        ep.setLabelScale(0.5);
        agitatedBuddiesPortrayal.setPortrayalForAll(ep);
                
        display3d.createSceneGraph(); 
        display3d.reset();
        }

    public void init(Controller c)
        {
        super.init(c);

        // make the displayer
        display = new Display2D(600,600,this);
        // turn off clipping
        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Schoolyard Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        display.attach( buddiesPortrayal, "Buddies" );
        display.attach( yardPortrayal, "Yard" );


        display3d = new Display3D(300, 300,this);
        double width = 100;
        display3d.translate(-width / 2.0, -width / 2.0, 0);
        display3d.scale(2.0 / width);

        displayFrame3d = display3d.createFrame();
        displayFrame3d.setTitle("Schoolyard Display... NOW IN 3-D!");
        c.registerFrame(displayFrame3d);   // register the frame so it appears in the "Display" list
        displayFrame3d.setVisible(true);
        display3d.attach( agitatedBuddiesPortrayal, "Buddies ... IN 3-D!" );
        display3d.attach( agitatedYardPortrayal, "Yard ... IN 3-D!" );
        }

    public void quit()
        {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
                
        if (displayFrame3d!=null) displayFrame3d.dispose();
        displayFrame3d = null;
        display3d = null;
        }

    }
