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
    in a single window.  You add Inspectors manually to the TabbedInspector with
    addInspector(...), specifying the string for a Tab in a JTabbedPane
    in which the inspectors are displayed.
    
    <p>Alternatively, TabbedInspector will generate an inspector automatically for 
    an object with is Tabbable, or for an object which is TabbableAndGroupable.
    In the latter case, the TabbedInspector will create a tabbed inspector,
    and under each tab, it will create a GroupedInspector.  You can manually
    add additional tabs thereafter.
    
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
    ArrayList inspectors = new ArrayList();
    JTabbedPane tabs = null;
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
        add(getHeader(),BorderLayout.NORTH);
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
    public TabbedInspector(TabbableAndGroupable object, GUIState state, String name)
        {
        Properties properties = Properties.getProperties(object);
        if (properties instanceof SimpleProperties)
            {
            buildTabbedInspector((SimpleProperties)properties, state, name,
                object.provideTabGroupProperties(), object.provideTabGroups(), 
                object.provideTabNames(), object.provideExtraTab());
            }
        else throw new RuntimeException("A Tabbable Object must provide SimpleProperties.");
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
    
    /** Creates a TabbedInspector in which the provided properties are collected under various tabs.
     	The tab names are provided with tabNames,
		and the properties under each tab are given with the corresponding propertyNames.
        extraTab gives the name of a tab holding all extra properties which do not 
        appear among ANY of the propertyNames arrays.  If extraTab is null, these properties 
        will not appear at all.  The subinspectors under each tab are SimpleInspectors. */
    public TabbedInspector(SimpleProperties properties, GUIState state, String name,
        String[][] propertyNames, String[] tabNames, String extraTab)
        { buildTabbedInspector(properties, state, name, propertyNames, tabNames, extraTab); }


    /** Creates a TabbedInspector in which the provided properties are collected under various tabs.
    	then collected under groups under those tabs.  The tab names are provided with tabNames,
        and the groups under each tab are given with the corresponding tabGroups.
		Properties for each tab/group combination are given under tabGroupProperties.  There may
		be one additional array of properties under a given tab: these will appear as separate ungrouped
		properties.  
        extraTab gives the name of a tab holding all extra properties which do not 
        appear among ANY of the propertyNames arrays.  If extraTab is null, these properties 
        will not appear at all.  The subinspectors under each tab are SimpleInspectors. */
    public TabbedInspector(SimpleProperties properties, GUIState state, String name,
        String[][][] tabGroupProperties, String[][] tabGroups, String[] tabNames, String extraTab)
        { buildTabbedInspector(properties, state, name, tabGroupProperties, tabGroups, tabNames, extraTab); }


	/** Creates a TabbedInspector with separate tabbed subinspectors for each of the
		provided objects.  The names of each of these subinspectors is provided in tabNames.
		If a name is provided, then the entire TabbedInspector
		will have a bordered layout with that given name.  If a title is provided (and it generally
		out to be), then this will serve as the TabbedInspector's title (which appears when it is
		in windows or higher-level tabs).   
	*/
	public TabbedInspector(Object[] objects, GUIState state, String name,
							String title, String[] tabNames)
		{
        if (objects == null)
        	throw new RuntimeException("Objects array provided is null.");
    	Properties[] properties = new SimpleProperties[objects.length];
    	
		for(int i = 0; i < objects.length; i++)
			properties[i] = Properties.getProperties(objects[i]);
		
		buildTabbedInspector(properties, state, name, title, tabNames);
		}

	/** Creates a TabbedInspector with separate tabbed subinspectors for each of the
		provided properties.  The names of each of these subinspectors is provided in tabNames.
		If a name is provided, then the entire TabbedInspector
		will have a bordered layout with that given name.  If a title is provided (and it generally
		out to be), then this will serve as the TabbedInspector's title (which appears when it is
		in windows or higher-level tabs).   
	*/
	public TabbedInspector(Properties[] properties, GUIState state, String name,
							String title, String[] tabNames)
		{
		buildTabbedInspector(properties, state, name, title, tabNames);
		}


	// flattens a 2-dimensional array of strings to a single array
	String[] flatten(String[][] names)
		{
		int count = 0;
		for(int i = 0; i < names.length; i++)
			count += names[i].length;
		String[] group = new String[count];
		count = 0;
		for(int i = 0; i < names.length; i++)
			{
			System.arraycopy(names[i], 0, group, count, names[i].length);
			count += names[i].length;
			}
		return group;
		}

	// flattens a 3-dimensional array of strings to a single array
	String[] flatten(String[][][] names)
		{
		String[][] group = new String[names.length][];
		for(int i = 0; i < names.length; i++)
			group[i] = flatten(names[i]);
		return flatten(group);
		}

	// builds the inspector for both tabs and groups under the tabs.
	// tabGroupProperties are per-tab, then per-group, lists of properties.
	// tabGroups are per-tab lists of group names
	//
	// NOTE: It must be the case that tabGroupProperties[tab].length == tabGroups[i].length
	// or that tabGroupProperties[tab].length == tabGroups[i].length + 1
	// for a given tab. In the latter case, groups under that tab will be formed
	// for properties in tabGroupProperties[tab][group] for group = 0 ... tabGroupProperties[tab].length - 2 
	// and the final group (tabGroupProperties[tab][tabGroupProperties.length-1])
	// will be displayed as separate ungrouped properties under the tab (they will appear first).
	// 
	// 
	// tabNames is a list of tab names
	// extraTab is where all properties go that are not found under tabGroupProperties.
	// If extraTab is null, then these "extra" properties will not be shown.
    void buildTabbedInspector(SimpleProperties properties, GUIState state, String name,
        String[][][] tabGroupProperties, String[][] tabGroups, String[] tabNames, String extraTab)
        {
        buildTabbedInspector(name, true);
        
        if (tabNames == null)
            throw new RuntimeException("Tab names provided is null.");
        if (tabGroupProperties == null)
            throw new RuntimeException("Tab Group Property names provided is null.");
        if (tabNames.length != tabGroupProperties.length)
            {
            throw new RuntimeException("Property names and tab names must have the same length.");
            }
        
        for(int i = 0; i < tabNames.length; i++)
        	{
        	Inspector insp = null;
        	if (tabGroupProperties[i].length == tabGroups[i].length)
        		{
	         	insp = new GroupedInspector(
    	    		properties.getPropertiesSubset(flatten(tabGroupProperties[i]), true),
        			state, tabNames[i], tabGroupProperties[i], tabGroups[i], false);
        		}
        	else if (tabGroupProperties[i].length == tabGroups[i].length + 1)
        		{
        		String[][] tgb = new String[tabGroupProperties[i].length - 1][];
        		for(int j = 0; j < tgb.length; j++)
        			tgb[j] = tabGroupProperties[i][j];
        			
	        	insp = new GroupedInspector(
    	    		properties.getPropertiesSubset(flatten(tabGroupProperties[i]), true),
        			state, tabNames[i], tgb, tabGroups[i], true);
        		}
        	else throw new RuntimeException("Number of tab group property groups for tab " + tabNames[i] + " must be equal to or one more than the number of group names.");
        	
        	addInspector(insp, tabNames[i]);
        	}

        if (extraTab != null)  // one extra "Misc" tab
            {
            SimpleProperties simp = properties.getPropertiesSubset(flatten(tabGroupProperties), false);
            addInspector(new SimpleInspector(simp, state, null), extraTab);
            }

        setTitle("" + properties.getObject());
        }

	// builds the inspector for tabs only (no groups).
	// properties are per-tab lists of properties.
	// tabNames is a list of tab names
    void buildTabbedInspector(Properties[] properties, GUIState state, String name,
							String title, String[] tabNames)
        {
        buildTabbedInspector(name, true);
        
        if (tabNames == null)
            throw new RuntimeException("Tab names provided is null.");
        if (properties == null)
            throw new RuntimeException("Properties provided is null.");
        if (tabNames.length != properties.length)
            {
            throw new RuntimeException("Properties and tab names must have the same length.");
            }
        
        for(int i = 0; i < properties.length; i++)
            {
            addInspector(new SimpleInspector(properties[i], state, null), tabNames[i]);
            }
                
        setTitle(title);
        }


	// builds the inspector for tabs only (no groups).
	// properties are per-tab lists of properties.
	// tabNames is a list of tab names
	// extraTab is where all properties go that are not found under tabGroupProperties.
	// If extraTab is null, then these "extra" properties will not be shown.
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
            SimpleProperties simp = properties.getPropertiesSubset(flatten(propertyNames), false);
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
        
    /** Adds an inspector. Inspector must already have its name field set. */
    public void addInspector(Inspector i)
        {
        inspectors.add(i);
        tabs.addTab(i.getTitle(),i);
        i.setVolatile(isVolatile());
        i.setShowsUpdate(false);
        }
        
    /** Adds an inspector with the given tab name. */
    public void addInspector(Inspector i, String tab)
        {
        i.setTitle(tab);
        addInspector(i);
        }
    
    public Inspector removeInspector(String tab)
    	{
        int len = inspectors.size();
        int x=0;
        for(x=0;x<len;x++)
            if (tabs.getTitleAt(x).equals(tab)) break;
        if (x==len) return null;  // failed
        tabs.removeTabAt(x);
        return (Inspector)(inspectors.remove(x));
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
