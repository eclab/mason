/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class DialogToolTip extends JDialog 
    {
    static DialogToolTip tip = new DialogToolTip();
    static JLabel label = new JLabel("",JLabel.CENTER);
    static
        {
        tip.setUndecorated(true);
        tip.getContentPane().setBackground(Color.yellow);  // or whatever it is
        tip.getContentPane().setLayout(new BorderLayout());
        tip.getContentPane().add(label);
            
        tip.setModal(false);
            
        label.setBorder(new EmptyBorder(2,2,2,2));
        label.setBackground(Color.yellow);  // just in case
        }

    // you'll want the text to be "<font size=-1>" I think.  See
    // the text I used in the 2D Display tooltip generator
    // (which is compatible with JDK 1.3)
    public static void showToolTip(Point locationOnScreen, String htmlText)
        {
        if (htmlText == null)
            tip.setVisible(false);
        else
            {
            label.setText(htmlText);
            tip.pack();  // resize around the new label
                        
            // on Macs, sometimes the initial pack gets the wrong Y value -- pack again
            tip.pack();  // resize around the new label
            tip.setLocation(locationOnScreen);              
            tip.setVisible(true);
            }
        }

    public static void hideToolTip()
        {
        tip.setVisible(false);
        }

    }
