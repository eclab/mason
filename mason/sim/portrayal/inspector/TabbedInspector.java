/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;
import sim.portrayal.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;

/** TabbedInspector is an Inspector which holds multiple subInspectors
    in turn, allowing you to device complex inspector sets to be displayed
    in a single window.  You add Inspectors to the TabbedInspector with
    addInspector(...), specifying the string for a Tab in a JTabbedPane
    in which the inspectors are displayed.
    
    <p>Volatility is an issue: if some of the subInspectors are volatile
    and some are not, should TabbedInspector behave as if it's volatile?
    We have chosen to enforce the rule that all subInspectors must have
    the same volatility as the TabbedInspector itself.  
    
    <p>You can also specify, through setUpdatingAllInspectors(...), whether
    or not the TabbedInspector updates <i>all</i> of its subinspectors
    whenever it is in turn updated, or if only the currently-displayed
    subinspector is updated (and when we switch to a new inspector, it's
    updated as of the switch).
*/

public class TabbedInspector extends Inspector
    {
    public ArrayList inspectors = new ArrayList();
    public JTabbedPane tabs = new JTabbedPane();
    boolean updatingAllInspectors;
    
    /** Creates a volatile TabbedInspector */
    public TabbedInspector()
        {
        this(true);
        }

    public TabbedInspector(boolean isVolatile)
        {
        setLayout(new BorderLayout());
        add(tabs,BorderLayout.CENTER);
        tabs.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e)
                {
                updateDisplayedInspector();
                }
            });
        setVolatile(isVolatile);
        }

    /** Calls updateInspector() and repaint() on the currently-displayed inspector */
    public void updateDisplayedInspector()
        {
        if (inspectors.size() > 0)
            {
            Inspector i = ((Inspector)(inspectors.get(tabs.getSelectedIndex())));
            i.updateInspector();
            i.repaint();
            }
        }
        
    /** Here you set whether TabbedInspector updates all underlying inspectors whenever
        it receives an updateInspector or if it is only updating them lazily as they are
        displayed */
    public void setUpdatingAllInspectors(boolean val)
        {
        updatingAllInspectors = true;
        }
    
    /** Returns true if the TabbedInspector updates all underlying inspectors whenever
        it receives an updateInspector or false if it is only updating them lazily as they are
        displayed */
    public boolean isUpdatingAllInspectors()
        {
        return updatingAllInspectors;
        }

    /** Sets all subinspectors to be volatile */
    public void setVolatile(boolean val)
        {
        int len = inspectors.size();
        for(int x=0;x<len;x++)
            ((Inspector)(inspectors.get(x))).setVolatile(val);
        super.setVolatile(val);
        }
    
    public void updateInspector()
        {
        int len = inspectors.size();
        if (updatingAllInspectors)
            for(int x=0;x<len;x++)
                ((Inspector)(inspectors.get(x))).updateInspector();
        else updateDisplayedInspector();
        }
        
    /** Adds an inspector with the given tab name. */
    public void addInspector(Inspector i, String tab)
        {
        inspectors.add(i);
        tabs.addTab(tab,i);
        i.setVolatile(isVolatile());
        }
        
    /** Removes and returns the inspector, or null if there is no such inspector in the TabbedInspector */
    public Inspector removeInspector(Inspector i)
        {
        int len = inspectors.size();
        int x=0;
        for(x=0;x<len;x++)
            if (inspectors.get(x) == i) break;
        if (x==len) return null;  // failed
        
        tabs.removeTabAt(x);
        return (Inspector)(inspectors.remove(x));
        }
    
    /** Removes all inspectors. */
    public void clear()
        {
        tabs.removeAll();
        inspectors.clear();
        }
    }
