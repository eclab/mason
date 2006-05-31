/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

// Class DrawUI
package sim.app.lsystem;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import sim.util.gui.*;

// This file contains the "Draw" pane of the Console
public class DrawUI extends JPanel
    {

    // components
    JButton buttonSet = new JButton("Set");
    JTextField distField = new JTextField("2", 2);
    JTextField angleField = new JTextField("90", 4);
    
    /*
    // unnecessary -- now in the Display2D Options
    JTextField xField = new JTextField("50", 4);
    JTextField yField = new JTextField("50", 4);
    */
    
    // references to sim with ui, sim state
    LSystemWithUI lsui;
    LSystem ls;
    
    public DrawUI(LSystemWithUI nLsui)
        {
        lsui = nLsui;
        ls = (LSystem)lsui.state;
        
        try
            {
            init();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        }
    
    public void init()
        {
        // buttonSet sets the current drawing settings
        buttonSet.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                // set draw parameters
                ls.l.segsize = Double.valueOf(distField.getText()).doubleValue();
                ls.l.angle = Double.valueOf(angleField.getText()).doubleValue()*Math.PI/180;
                //ls.l.x = Double.valueOf(xField.getText()).doubleValue();
                //ls.l.y = Double.valueOf(yField.getText()).doubleValue();
                
                // reset current stats
                ls.l.theta = -Math.PI/2;
                }
            });
        
         
        ///// OK
        // now build the actual UI
        this.setLayout(new BorderLayout());
        
        // sim.display.LabelledList is a convenient way to draw lists of the format
        //  text   component
        //  text   component
        LabelledList list = new LabelledList()
            {
            Insets insets = new Insets(5, 5, 5, 5);
            public Insets getInsets()
                {
                return insets;
                }
            };
        
        // so, the draw info:
        list.addLabelled("Distance: ", distField);
        list.addLabelled("Angle: ", angleField);
//        list.addLabelled("Start at:", new JLabel());
//        list.addLabelled("x: ", xField);
//        list.addLabelled("y: ", yField);
        
        Box b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(5, 5, 5, 5);
            public Insets getInsets()
                {
                return insets;
                }
            };
        
        b.add(buttonSet);
        b.add(Box.createGlue());
        
        list.addLabelled("",b);
        
        // add stuff to this
        this.add(list, BorderLayout.CENTER);

        setVisible(true);
        }
    }
