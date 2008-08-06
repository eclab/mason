/*
  Copyright 2008 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
package sim.util.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** 
    A panel with a small disclosure triangle which toggles between two subcomponents:
    notionally an "abridged" (short) component and an expanded ("disclosed") component.
    The panel can sprout an optional titled label.

    <p>Thanks to:
    http://lists.apple.com/archives/java-dev/2005/Feb/msg00171.html

    ... for the idea.
*/


/*
  f = new JFrame();
  l = new Label("Four score and seven years ago!");
  b = new JButton("yo yo yo!");
  d = new sim.util.gui.DisclosurePanel(l,b);
  f.getContentPane().setLayout(new BorderLayout());
  f.getContentPane().add(d);
  f.pack();
  f.show();
*/

public class DisclosurePanel extends JPanel
    {
    JToggleButton disclosureToggle = new JToggleButton();
    Component abridgedComponent;
    Component disclosedComponent;
    
    public DisclosurePanel(Component abridgedComponent, Component disclosedComponent)
        {
        this(abridgedComponent, disclosedComponent, null);
        }
        
    public DisclosurePanel(Component abridgedComponent, Component disclosedComponent, String borderLabel)
        {
        disclosureToggle.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        disclosureToggle.setContentAreaFilled(false);
        disclosureToggle.setFocusPainted(false);
        disclosureToggle.setRequestFocusEnabled(false);
        disclosureToggle.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
        disclosureToggle.setSelectedIcon(UIManager.getIcon("Tree.expandedIcon"));
        this.abridgedComponent = abridgedComponent;
        this.disclosedComponent = disclosedComponent;
        setLayout(new BorderLayout());
        Box b = new Box(BoxLayout.Y_AXIS);
        b.add(disclosureToggle);
        b.add(Box.createGlue());
        add(b, BorderLayout.WEST);
        add(abridgedComponent, BorderLayout.CENTER);

        if (borderLabel!=null)
            setBorder(new javax.swing.border.TitledBorder(borderLabel));
        
        disclosureToggle.addItemListener(new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (disclosureToggle.isSelected()) // disclose
                    {
                    DisclosurePanel.this.remove(DisclosurePanel.this.abridgedComponent);
                    DisclosurePanel.this.add(DisclosurePanel.this.disclosedComponent, BorderLayout.CENTER);
                    DisclosurePanel.this.revalidate();
                    }
                else // hide
                    {
                    DisclosurePanel.this.remove(DisclosurePanel.this.disclosedComponent);
                    DisclosurePanel.this.add(DisclosurePanel.this.abridgedComponent, BorderLayout.CENTER);
                    DisclosurePanel.this.revalidate();
                    }
                }
            });
        }
        
    public void setAbridgedComponent(Component abridgedComponent)
        {
        if (!disclosureToggle.isSelected())
            {
            remove(this.abridgedComponent);
            add(abridgedComponent, BorderLayout.CENTER);
            revalidate();
            }
        this.abridgedComponent = abridgedComponent;
        }

    public void setDisclosedComponent(Component disclosedComponent)
        {
        if (disclosureToggle.isSelected())
            {
            remove(this.disclosedComponent);
            add(disclosedComponent, BorderLayout.CENTER);
            revalidate();
            }
        this.disclosedComponent = disclosedComponent;
        }
    }

