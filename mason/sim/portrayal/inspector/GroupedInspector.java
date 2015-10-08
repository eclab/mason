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
    /** The property list displayed -- this may change at any time */
    LabelledList propertyList;

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
        and the properties under each group are given with the corresponding propertyNames.
        showExtraProperties specifies whether additional properties not appearing 
        among those in propertyNames should appear as ungrouped properties in the inspector.
        The subinspectors under each group are SimpleInspectors. */
    public GroupedInspector(SimpleProperties properties, GUIState state, String name,
        String[][] propertyNames, String[] groupNames, boolean showExtraProperties)
        { buildGroupedInspector(properties, state, name, propertyNames, groupNames, showExtraProperties); }

    void buildGroupedInspector(SimpleProperties properties, GUIState state, String name,
        String[][] propertyNames, String[] groupNames, boolean showExtraProperties)
        {
        setVolatile(true);
        setLayout(new BorderLayout());
        setTitle("" + properties.getObject());
        propertyList = new LabelledList();
        add(propertyList, BorderLayout.CENTER);
        add(getHeader(), BorderLayout.NORTH);
        
        if (groupNames == null)
            throw new RuntimeException("Group names provided is null.");
        if (propertyNames == null)
            throw new RuntimeException("Property names provided is null.");
        if (groupNames.length != propertyNames.length)
            {
            throw new RuntimeException("Property names and group names must have the same length.");
            }

		if (showExtraProperties)
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
            SimpleInspector insp = new SimpleInspector(simp, state, null);
            propertyList.add(insp);
            inspectors.add(insp);
        	insp.setVolatile(isVolatile());
        	insp.setShowsUpdate(false);
			}
        
        for(int i = 0; i < propertyNames.length; i++)
            {
            SimpleProperties simp = properties.getPropertiesSubset(propertyNames[i], true);
            SimpleInspector insp = new SimpleInspector(simp, state, groupNames[i]);
            addInspector(insp, groupNames[i]);
            }

        setTitle("" + properties.getObject());
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
        propertyList.add(new DisclosurePanel(i.getTitle(), i));
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
