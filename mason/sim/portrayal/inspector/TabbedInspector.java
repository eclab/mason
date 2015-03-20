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
import sim.util.Properties;
import sim.util.SimpleProperties;
import sim.display.*;

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
    public JTabbedPane tabs = null;
    boolean updatingAllInspectors;
    
    /** Creates a volatile TabbedInspector */
    public TabbedInspector()
        {
        buildTabbedInspector(null, true);
        }

    /**
       @deprecated
    */
    public TabbedInspector(boolean isVolatile)
        {
        buildTabbedInspector(null, isVolatile);
        }

    /** Creates a volatile TabbedInspector */
    public TabbedInspector(String name)
        {
        buildTabbedInspector(name, true);
        }
    
    void buildTabbedInspector(String name, boolean isVolatile)
        {
        // Make a JTabbedPane with small tabs
        UIDefaults defaults = UIManager.getDefaults( );
        Object def = defaults.get("TabbedPane.useSmallLayout");
        defaults.put("TabbedPane.useSmallLayout", Boolean.TRUE);
        tabs = new JTabbedPane();
        defaults.put("TabbedPane.useSmallLayout", def);

        // Continue...
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
        if (name != null)
            setBorder(new javax.swing.border.TitledBorder(name));
        }

    /** Creates a volatile TabbedInspector in which an object's properties are broken into
        several groups, each under its own tab, according to the object's Tabbable interface.
        The tab names are provided with the object.provideTabNames(), and the properties 
        under each tab are provided by object.provideTabProperties.  object.provideExtraTab()
        gives the name of a tab holding all extra properties which do not appear among ANY 
        of the propertyNames arrays.  If object.provideExtraTab() is null, these properties 
        will not appear at all.  The subinspectors under each tab are SimpleInspectors. */
    public TabbedInspector(Tabbable object, GUIState state, String name)
        {
        Properties properties = Properties.getProperties(object);
        if (properties instanceof SimpleProperties)
            {
            buildTabbedInspector((SimpleProperties)properties, state, name,
                object.provideTabProperties(), object.provideTabNames(),
                object.provideExtraTab());
            }
        else throw new RuntimeException("A Tabbable Object must provide SimpleProperties.");
        }
    
    /** Creates a TabbedInspector in which the provided properties are broken into
        several groups, each under its own tab.  The tab names are provided with tabNames,
        and the properties under each tab are given with the corresponding propertyNames.
        extraTab gives the name of a tab holding all extra properties which do not 
        appear among ANY of the propertyNames arrays.  If extraTab is null, these properties 
        will not appear at all.  The subinspectors under each tab are SimpleInspectors. */
    public TabbedInspector(SimpleProperties properties, GUIState state, String name,
        String[][] propertyNames, String[] tabNames, String extraTab,
        boolean isVolatile)
        { buildTabbedInspector(properties, state, name, propertyNames, tabNames, extraTab); }

    void buildTabbedInspector(SimpleProperties properties, GUIState state, String name,
        String[][] propertyNames, String[] tabNames, String extraTab)
        {
        buildTabbedInspector(name, true);
        
        if (tabNames == null)
            throw new RuntimeException("Tab names provided is null.");
        if (propertyNames == null)
            throw new RuntimeException("Property names provided is null.");
        if (tabNames.length != propertyNames.length)
            {
            throw new RuntimeException("Property names and tab names must have the same length.");
            }
        
        for(int i = 0; i < propertyNames.length; i++)
            {
            SimpleProperties simp = properties.getPropertiesSubset(propertyNames[i], true);
            addInspector(new SimpleInspector(simp, state, null), tabNames[i]);
            }
                
        if (extraTab != null)  // one extra "Misc" tab
            {
            // flatten all properties
            int count = 0;
            for(int i = 0; i < propertyNames.length; i++)
                count += propertyNames[i].length;
            String[] group = new String[count];
            count = 0;
            for(int i = 0; i < propertyNames.length; i++)
                {
                System.arraycopy(propertyNames[i], 0, group, count, propertyNames[i].length);
                count += propertyNames[i].length;
                }
                
            SimpleProperties simp = properties.getPropertiesSubset(group, false);
            addInspector(new SimpleInspector(simp, state, null), extraTab);
            }

        setTitle("" + properties.getObject());
        }

    /** Calls updateInspector() and repaint() on the currently-displayed inspector */
    void updateDisplayedInspector()
        {
        if (tabs.getTabCount() > 0 && tabs.getSelectedIndex() >= 0)
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
        i.setTitle(tab);
        inspectors.add(i);
        tabs.addTab(tab,i);
        i.setVolatile(isVolatile());
        }
    
    /** Adds an inspector. Inspector must already have its name field set. */
    public void addInspector(Inspector i)
        {
        inspectors.add(i);
        tabs.addTab(i.getTitle(),i);
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
