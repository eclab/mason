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
import sim.util.gui.*;

/** GroupedInspector is an Inspector which holds multiple subInspectors
    in turn, allowing you to device complex inspector sets to be displayed
    in a single window.  You add Inspectors to the GroupedInspector with
    addInspector(...), specifying the string for a group
    in which the inspectors are displayed.

    <p>Alternatively, Grouped will generate an inspector automatically for 
    an object with is Groupable. 
        
    <p>Volatility is an issue: if some of the subInspectors are volatile
    and some are not, should GroupedInspector behave as if it's volatile?
    We have chosen to enforce the rule that all subInspectors must have
    the same volatility as the GroupedInspector itself.  
    
    <p>You can also specify, through setUpdatingAllInspectors(...), whether
    or not the GroupedInspector updates <i>all</i> of its subinspectors
    whenever it is in turn updated, or if only the currently-displayed
    subinspector is updated (and when we switch to a new inspector, it's
    updated as of the switch).
*/

public class GroupedInspector extends Inspector
    {
    /** The Properties list displayed -- this may change at any time */
    LabelledList PropertiesList;

    ArrayList inspectors = new ArrayList();
    boolean updatingAllInspectors;
    
    /** Creates a volatile GroupedInspector in which an object's properties are broken into
        several groups, each under its own group, according to the object's Groupable interface.
        The group names are provided with the object.provideGroupNames(), and the properties 
        under each group are provided by object.provideGroupProperties.  object.showExtraProperties()
        specifies whether additional properties not appearing among those in object.provideGroupNames()
        should appear as ungrouped properties in the inspector.  
        The subinspectors under each group are SimpleInspectors. */
    public GroupedInspector(Groupable object, GUIState state, String name)
        {
        Properties properties = Properties.getProperties(object);
        if (properties instanceof SimpleProperties)
            {
            buildGroupedInspector((SimpleProperties)properties, state, name,
                object.provideGroupProperties(), object.provideGroupNames(),
                object.showExtraProperties());
            }
        else throw new RuntimeException("A Groupable Object must provide SimpleProperties.");
        }
    
    /** Creates a GroupedInspector in which the provided properties are broken into
        several groups, each under its own group.  The group names are provided with groupNames,
        and the properties under each group are given with the corresponding PropertiesNames.
        showExtraProperties specifies whether additional properties not appearing 
        among those in PropertiesNames should appear as ungrouped properties in the inspector.
        The subinspectors under each group are SimpleInspectors. */
    public GroupedInspector(SimpleProperties properties, GUIState state, String name,
        String[][] PropertiesNames, String[] groupNames, boolean showExtraProperties)
        { buildGroupedInspector(properties, state, name, PropertiesNames, groupNames, showExtraProperties); }
        
	/** Creates a GroupedInspector with separate grouped subinspectors for each of the
		provided properties.  The names of each of these subinspectors is provided in groupNames.
		Additionaly if extraProperties is provided, its properties are shown
		immediately before these groups.  If a name is provided, then the entire GroupedInspector
		will have a bordered layout with that given name.  If a title is provided (and it generally
		out to be), then this will serve as the GroupedInspector's title (which appears when it is
		in windows or tabs).   
	*/
    public GroupedInspector(Properties[] properties, Properties extraProperties, 
								GUIState state, String name, String title, String[] groupNames)
		{ buildGroupedInspector(properties, extraProperties, state, name, title, groupNames); }

	/** Creates a GroupedInspector with separate grouped subinspectors for each of the
		provided objects.  The names of each of these subinspectors is provided in groupNames.
		Additionaly if extraObject is provided, its properties are shown
		immediately before these groups.  If a name is provided, then the entire GroupedInspector
		will have a bordered layout with that given name.  If a title is provided (and it generally
		out to be), then this will serve as the GroupedInspector's title (which appears when it is
		in windows or tabs).   
	*/

    public GroupedInspector(Object[] objects, Object extraObject, 
								GUIState state, String name, String title, String[] groupNames)
		{ 
        if (objects == null)
        	throw new RuntimeException("Objects array provided is null.");
    	Properties[] properties = new SimpleProperties[objects.length];
    	Properties extraProperties = null;
    	
    	if (extraObject != null)
    		extraProperties = Properties.getProperties(extraObject);
    	
		for(int i = 0; i < objects.length; i++)
			properties[i] = Properties.getProperties(objects[i]);
		
		buildGroupedInspector(properties, extraProperties, state, name, title, groupNames); 
		}

	void buildGroupedInspector(Properties[] properties, Properties extraProperties, 
					GUIState state, String name, String title, String[] groupNames)
        {
        setVolatile(true);
        setLayout(new BorderLayout());
        setTitle(title);
        PropertiesList = new LabelledList(name);
        add(PropertiesList, BorderLayout.CENTER);
        add(getHeader(), BorderLayout.NORTH);
        
        if (properties == null)
        	throw new RuntimeException("Properties provided is null.");
        if (groupNames == null)
            throw new RuntimeException("Group names provided is null.");
        if (groupNames.length != properties.length)
            {
            throw new RuntimeException("Properties and group names must have the same length.");
            }

		if (extraProperties != null)
			{
            SimpleInspector insp = new SimpleInspector(extraProperties, state, null);
            PropertiesList.add(insp);
            inspectors.add(insp);
        	insp.setVolatile(isVolatile());
        	insp.setShowsUpdate(false);
			}
        
        for(int i = 0; i < groupNames.length; i++)
            {
            SimpleInspector insp = new SimpleInspector(properties[i], state, groupNames[i]);
            addInspector(insp, groupNames[i]);
            }
        }



    void buildGroupedInspector(SimpleProperties properties, GUIState state, String name,
        String[][] PropertiesNames, String[] groupNames, boolean showExtraProperties)
        {
        setVolatile(true);
        setLayout(new BorderLayout());
        setTitle("" + properties.getObject());
        PropertiesList = new LabelledList(name);
        add(PropertiesList, BorderLayout.CENTER);
        add(getHeader(), BorderLayout.NORTH);
        
        if (groupNames == null)
            throw new RuntimeException("Group names provided is null.");
        if (PropertiesNames == null)
            throw new RuntimeException("Properties names provided is null.");
        if (groupNames.length != PropertiesNames.length)
            {
            throw new RuntimeException("Properties names and group names must have the same length.");
            }

		if (showExtraProperties)
			{
            // flatten all properties
            int count = 0;
            for(int i = 0; i < PropertiesNames.length; i++)
                count += PropertiesNames[i].length;
            String[] group = new String[count];
            count = 0;
            for(int i = 0; i < PropertiesNames.length; i++)
                {
                System.arraycopy(PropertiesNames[i], 0, group, count, PropertiesNames[i].length);
                count += PropertiesNames[i].length;
                }
                
            SimpleProperties simp = properties.getPropertiesSubset(group, false);
            SimpleInspector insp = new SimpleInspector(simp, state, null);
            PropertiesList.add(insp);
            inspectors.add(insp);
        	insp.setVolatile(isVolatile());
        	insp.setShowsUpdate(false);
			}
        
        for(int i = 0; i < PropertiesNames.length; i++)
            {
            SimpleProperties simp = properties.getPropertiesSubset(PropertiesNames[i], true);
            SimpleInspector insp = new SimpleInspector(simp, state, groupNames[i]);
            addInspector(insp, groupNames[i]);
            }
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
        for(int x=0;x<len;x++)
            ((Inspector)(inspectors.get(x))).updateInspector();
        }

    /** Adds an inspector. Inspector must already have its name field set. */
    public void addInspector(Inspector i)
        {
        inspectors.add(i);
        PropertiesList.add(new DisclosurePanel(i.getTitle(), i));
        i.setVolatile(isVolatile());
        i.setShowsUpdate(false);
        }
        
    /** Adds an inspector with the given group name. */
    public void addInspector(Inspector i, String group)
        {
        i.setTitle(group);
        addInspector(i);
        }
    }
