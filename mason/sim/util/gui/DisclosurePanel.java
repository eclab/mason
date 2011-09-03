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
    
    public DisclosurePanel(String abridgedText, Component disclosedComponent)
        {
        this(abridgedText, disclosedComponent, null);
        }
                
    public DisclosurePanel(String abridgedText, Component disclosedComponent, String borderLabel)
        {
        this(new JButton(abridgedText), disclosedComponent);
        JButton button = (JButton)abridgedComponent;
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRequestFocusEnabled(false);
        button.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent event)
                {
                disclosureToggle.doClick();
                }
            });
        }

    public DisclosurePanel(Component abridgedComponent, Component disclosedComponent)
        {
        this(abridgedComponent, disclosedComponent, null);
        }
        
    public DisclosurePanel(Component abridgedComponent, Component disclosedComponent, String borderLabel)
        {
        disclosureToggle.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        disclosureToggle.setContentAreaFilled(false);
        disclosureToggle.setFocusPainted(false);
        disclosureToggle.setRequestFocusEnabled(false);
        disclosureToggle.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
        disclosureToggle.setSelectedIcon(UIManager.getIcon("Tree.expandedIcon"));
        this.abridgedComponent = abridgedComponent;
        this.disclosedComponent = disclosedComponent;
        setLayout(new BorderLayout());
        JPanel b = new JPanel();
        b.setLayout(new BorderLayout());
        b.add(disclosureToggle, BorderLayout.NORTH);
        add(b, BorderLayout.WEST);
        add(abridgedComponent, BorderLayout.CENTER);

        if (borderLabel!=null)
            setBorder(new javax.swing.border.TitledBorder(borderLabel));
        
        disclosureToggle.addItemListener(new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                setDisclosed(disclosureToggle.isSelected());
                }
            });
        }
        
    boolean disclosed = false;  // abridged
        
    public void setDisclosed(boolean disclosed)
        {
        this.disclosed = disclosed;
        if (disclosed) // disclose
            {
            remove(abridgedComponent);
            add(disclosedComponent, BorderLayout.CENTER);
            revalidate();
            }
        else // hide
            {
            remove(disclosedComponent);
            add(abridgedComponent, BorderLayout.CENTER);
            revalidate();
            }
        disclosureToggle.setSelected(disclosed);
        }
                
    public boolean isDisclosed() { return disclosed; }
        
    public Component getAbridgedComponent()
        {
        return abridgedComponent;
        }
                
    public Component getDisclosedComponent()
        {
        return disclosedComponent;
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

