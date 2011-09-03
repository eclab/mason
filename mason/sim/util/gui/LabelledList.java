/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.*;
import javax.swing.*;

/** LabelledList is a JPanel which makes it easy to set up two columns of
    Components, with the right column taking up the extra space if there is any.  
    Additionally, you can add up to three interpersed columns for checkboxes or icons or bullets
    or the like.
    
    <p>The columns are:
    <ol>
    <li><b>Far Left.</b>  Left-justified.
    <li><b>Left.</b>  Right-justified.  Typically used for a string label describing the Right component.
    <li><b>Center.</b>  Center-justified.
    <li><b>Right.</b>  Left-justified, fills up all extra space.  The primary component.
    <li><b>Far Right.</b>  Right-justified.
    </ol>
    
    <p>Most commonly, you would have the right column be widgets and
    the left column be the labels describing those widgets.  Two special methods
    are provided: <code>add(Component,Component)</code> lets you explicitly add
    a new row consisting of a component in the left column and another in the
    right column.  <code>addLabelled(String,Component)</code> lets you add a
    new row with a component in the right column and a JLabel showing the given
    string in the left column.
    
    <p>The general method is <code>add(Component,Component,Component,Component,Component)</code>
    to add up to all five columns.
    
    <p>LabelledList uses BorderLayout as its layout manager.  Please do not change it.
    However, your are welcome to add subcomponents anywhere but BorderLayout.NORTH,
    which is where the columns hang down from.
    
    <p>There is no way to remove components (this is a very simple class!).  What did
    you want, a JTable?  Sheesh!
*/

public class LabelledList extends JPanel
    {
    JPanel consolePanel = new JPanel()
        {
        public Insets getInsets() { return new Insets(0,2,0,2); } // provide some border on each side
        };
        
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    int y =0;
    
    /*
      public void setBackground(Color c)
      {
      super.setBackground(c);
      //if (consolePanel != null)  // may be null when Quaqua tries to set things up
      //      consolePanel.setBackground(new Color(0,0,0,0));
      }
    */
    
    public LabelledList()
        {
        super.setLayout(new BorderLayout());
        consolePanel.setLayout(gridbag);
        super.add(consolePanel, BorderLayout.NORTH);
        gbc.ipady=0; gbc.ipady = 0; gbc.weighty = 0;
        //consolePanel.setBackground(new Color(0,0,0,0));
        }
    
    /** Creates a Labelled List with a provided border label.  If label is null, just does new LabelledList()*/
    public LabelledList(String borderLabel)
        {
        this();
        if (borderLabel != null) setBorder(new javax.swing.border.TitledBorder(borderLabel));
        }
        
    /** Creates a new row, with the given components in the right column, and a JLabel of the given string in the left column.  The component and the label may be null. */
    public void addLabelled(String left, Component right)
        {
        add(new JLabel(""+left),right);
        }

    /** Creates a new row, with the given components in the left and right columns of the row respectively.  Either component may be null. */
    public void add(Component left, Component right)
        {
        add(null,left,null,right,null);
        }
    
    /** Creates a new row, with the given components in the farLeft, left, center, right, and farRight columns respectively.  Any component may be null. */
    public void add(Component farLeft, Component left, Component center, Component right, Component farRight)
        {
        gbc.insets = new Insets(0,2,0,2);
        gbc.gridy = y;
        
        if (farLeft!=null)
            {
            gbc.gridx = 0; 
            gbc.weightx = 0; 
            gbc.anchor=GridBagConstraints.WEST; 
            gbc.fill=GridBagConstraints.NONE; 
            gbc.gridwidth = 1;
            gridbag.setConstraints(farLeft,gbc);
            consolePanel.add(farLeft);
            }
        
        if (left!=null)
            {
            gbc.gridx = 1; 
            gbc.weightx = 0; 
            gbc.anchor=GridBagConstraints.EAST; 
            gbc.fill=GridBagConstraints.NONE; 
            gbc.gridwidth = 1; 
            gridbag.setConstraints(left,gbc);
            consolePanel.add(left);
            }

        if (center!=null)
            {
            gbc.gridx = 2; 
            gbc.weightx = 0; 
            gbc.anchor=GridBagConstraints.CENTER; 
            gbc.fill=GridBagConstraints.NONE; 
            gbc.gridwidth = 1; 
            gridbag.setConstraints(center,gbc);
            consolePanel.add(center);
            }

        if (right!=null)
            {
            gbc.gridx = 3; 
            gbc.weightx = 1; 
            gbc.anchor=GridBagConstraints.WEST; 
            gbc.fill=GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 1; 
            gridbag.setConstraints(right,gbc);
            consolePanel.add(right);
            }
        
        if (farRight!=null)
            {
            gbc.gridx = 4; 
            gbc.weightx = 0; 
            gbc.anchor=GridBagConstraints.EAST; 
            gbc.fill=GridBagConstraints.NONE; 
            gbc.gridwidth = GridBagConstraints.REMAINDER; 
            gridbag.setConstraints(farRight,gbc);
            consolePanel.add(farRight);
            }
        
        // increment the count
        y++;
        }

    /** Inserts a component spanning all five columns. */
    public Component add(Component comp)
        {
        addComponent(comp);
        return comp;
        }
        
    /** 
        @deprecated Inserts a component spanning all five columns.  Synonym for add(comp) 
    */
    public void addComponent(Component comp)
        {
        // set as a "left" component, but spanning using HORIZONTAL/REMAINDER
        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 1; gbc.anchor=GridBagConstraints.CENTER; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.gridwidth = 5; gbc.insets = new Insets(0,2,0,2);
        gridbag.setConstraints(comp, gbc);
        consolePanel.add(comp);
        
        // increment the count
        y++;
        }
    }
