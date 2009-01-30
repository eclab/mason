/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;
import sim.portrayal.*;
import java.awt.*;
import javax.swing.*;
import sim.display.*;
import sim.engine.*;
import sim.util.*;
import java.io.*;
import java.awt.event.*;

/** PropertyInspector is a subclass of Inspector which is intended for plug-in inspectors of a single property of an object.
    Plug-in inspectors may be associated with a Frame.  To create a PropertyInspector, you need to do several things:

    <ol>
    <li>In your class, override the static method <code>public static String name()</code> to return the desired name to display for this inspector
    in a pop-up chooser or menu.
    <li>In your class, override the static method <code>public static Class[] types()</code> to return an array of valid types the inspector
    is capable of inspecting.  The inspector will be made available for any property which is a subtype of a type in this array.
    If the array is set to <code>null</code>, all types will be assumed valid.
    <li>Add your full class name to the <b>sim/portrayal/inspector/propertyinspector.classes</b> file so you're listed among
    the plug-ins to query.
    <li>Override your constructor <code>public <i>Foo</i>(Properties properties, int index, Frame parent, GUIState simulation)</code>.
    Immediately after this constructor is called, the system will call <code>isValidInspector()</code> to test to see if the inspector created
    is valid and ready to be used, or should be thrown away.  Thus in your constructor you should query the user as appropriate,
    and if the user wishes to go ahead with the inspector, then arrange for <code>isValidInspector()</code> to return true.  The
    easiest way to do this is to simply set the protected variable <code>validInspector</code> to true.  By default <code>validInspector</code>
    returns false.
    <li>If we have a valid inspector, the system may then ask if it should construct a JFrame and put the inspector in it.  If
    you are handling your own frames etc., you may turn down this request by having 
    <code>public boolean shouldCreateFrame()</code> return false.  By default this method always returns true.
    <li>PropertyInspector is an Inspector: thus you may also wish to override createFrame(...) and reviseStopper(...).
    </ol>

    Like any inspector, your PropertyInspector will be asked to update and refresh itself to reflect new data whenevever
    <code>public void updateInspector()</code> is called.  
        
    <p>Similarly, your PropertyInspector will be provided a Stoppable which it can use to stop MASON from continuing to
    send update requests.  For example, if your PropertyInspector has a cancel button and the user has just clicked it,
    you might wish to call stop() on that Stoppable.  This Stoppable is not provided immediately on PropertyInspector
    construction, but later when the system has built the Stoppable and is ready to go.  When it does so, it will call the
    method <code>public void setStopper(Stoppable stopper)</code> to provide you with the Stoppable.  You may override
    this method to determine what the Stoppable is; but be sure to call <code>super.setStopper(stopper)</code>.  Note that
    the Stoppable provided will be whatever was returned by reviseStopper(...).
*/


public abstract class PropertyInspector extends Inspector
    {
    public int index;
    public Properties properties;
    /* public Frame parent; */
    public GUIState simulation;
    
    static Bag classes = null;
    protected boolean validInspector = false;
    Stoppable stopper;
        
    public void setStopper(Stoppable stopper)
        {
        this.stopper = stopper;
        }
                
    public Stoppable getStopper()
        {
        return stopper;
        }

    /** Returns true if the inspector is valid.  By default this just returns the validInspector variable. */
    public boolean isValidInspector() { return  validInspector; }
        
    /** Returns a PropertyInspector ready to go, or null if the user cancelled or some
        other event or error occurred which prevented the PropertyIinspector from being constructed. */
    public static PropertyInspector makeInspector(Class inspectorClass, Properties properties, int index, 
        Frame parent, GUIState simulation)
        {
        synchronized(simulation.state.schedule)
            {
            try
                {
                PropertyInspector inspector = (PropertyInspector)(inspectorClass.getConstructor(
                        new Class[] {Properties.class, Integer.TYPE, Frame.class, GUIState.class}).newInstance(
                            new Object[] { properties, new Integer(index), parent, simulation}));
                if (inspector.isValidInspector()) return inspector;
                else return null;  // failed -- perhaps the user cancelled
                }
            catch (Exception e)
                {
                e.printStackTrace();
                return null;
                }
            }
        }
    
    /** A string which defines the task the user performs when constructing this Inspector: such as "Make Histogram" */
    public static String name() { return "Name Not Set"; }
        
    /** A list of data types this Inspector is capable of inspecting. */
    public static Class[] types() { return new Class[0]; }
        
    /** Create a PropertyInspector for a given property.  The property is element #index in the provided Properties class. Also provided
        are the simulation and a 
        'parent' (a Frame which serves as the location where dialog boxes will pop up as part of the PropertyInspector construction
        process -- it's fine if you provide null for this).   */

    public PropertyInspector(Properties properties, int index, Frame parent, GUIState simulation)
        {
        this.properties = properties;
        this.index = index;
        /* this.parent = parent; */
        this.simulation = simulation;
        }

    public static String getMenuNameForPropertyInspectorClass(String classname)
        {
        try
            {
            return (String)(Class.forName(classname).getMethod("name", new Class[0]).invoke(null, new Object[0]));
            }
        catch(Throwable e)
            {
            return null;
            }
        }

    static boolean typesForClassCompatable(String classname, Class type)
        {
        try
            {
            Class[] types = (Class[])(Class.forName(classname).getMethod("types", new Class[0]).invoke(null, new Object[0]));
            if (types==null) return true; // all types are legal
            for(int x=0;x<types.length;x++)
                if (types[x].isAssignableFrom(type))
                    return true;
            }
        catch(Throwable e) { }
        return false;
        }
    
    public static Bag getPropertyInspectorClassNames()
        {
        if (classes == null)
            {
            classes = new Bag();
            
            try
                {
                InputStream s = PropertyInspector.class.getResourceAsStream("propertyinspector.classes");
                StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(s)));
                st.resetSyntax();
                st.wordChars(33,255);
                st.commentChar(35);
                st.whitespaceChars(0,32);  // control chars
                while(st.nextToken()!=StreamTokenizer.TT_EOF)
                    if (st.sval != null) classes.add(st.sval);  // ignore otherwise
                s.close();
                }
            catch (Throwable e)
                {
                System.err.println("Couldn't load the Propertyinspector.classes file because of error. \nLikely the file does not exist or could not be opened.\nThe error was:\n");
                e.printStackTrace();
                }
            }
        return classes;
        }
                
    /** Provides a popup menu attached to a JToggleButton which produces PropertyInspectors for
        a given property index.  Returns null if nothing compatible.  Provide null for pop if you don't have a JPopupMenu you'd
        like the PropertyInspector to build off of. */
    public static JToggleButton getPopupMenu(final Properties properties, final int index, final GUIState state, JPopupMenu pop)
        {
        boolean somethingCompatable = false;
        final Bag classes = getPropertyInspectorClassNames();
        
        // build the popup menu
        
        if (pop == null) pop = new JPopupMenu();
        final JPopupMenu popup = pop;
        popup.setLightWeightPopupEnabled(false);
        final JToggleButton toggleButton = new JToggleButton(INSPECT_ICON);
        toggleButton.setPressedIcon(INSPECT_ICON_P);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        toggleButton.setToolTipText("Show Additional Per-Property Inspectors");
        toggleButton.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                popup.show(e.getComponent(),0,toggleButton.getSize().height);
                }
            public void mouseReleased(MouseEvent e) 
                {
                toggleButton.setSelected(false);
                }
            });

        for(int x = 0; x < classes.numObjs; x++)
            {                                           
            JMenuItem menu = new JMenuItem((String)(getMenuNameForPropertyInspectorClass((String)(classes.objs[x]))));
            popup.add(menu);
            if (!typesForClassCompatable((String)(classes.objs[x]),properties.getType(index)))
                menu.setEnabled(false);
            else somethingCompatable = true;

            final int menuIndex = x;
            menu.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    PropertyInspector inspector = null;
                    try
                        {
                        inspector = PropertyInspector.makeInspector(
                            Class.forName((String)(classes.objs[menuIndex])),
                            properties, index, (sim.display.Console)(state.controller), state);
                        }
                    catch (ClassNotFoundException error) { }
                    
                    if (inspector != null)  // it'll be null if we've been cancelled
                        {
                        try
                            {
                            inspector.setStopper(inspector.reviseStopper(state.scheduleImmediateRepeat(
                                        true, inspector.getUpdateSteppable())));
                            }
                        catch (IllegalArgumentException ex)
                            {
                            Utilities.inform("The simulation is over and the item will not be tracked further.", 
                                "If you wanted to track, restart the simulation in paused state, then try tracking the item again.", null);
                            inspector.setStopper(inspector.reviseStopper(new Stoppable() { public void stop(){ } } ));  // does nothing
                            }
                                                        
                        state.controller.registerInspector(inspector,inspector.getStopper());
                        
                        if (inspector.shouldCreateFrame())
                            {
                            JFrame frame = inspector.createFrame(inspector.getStopper());
                            frame.setVisible(true);
                            }
                                                        
                        // update at least one time
                        inspector.updateInspector();
                        }
                    }
                });
            }
        
        if (!somethingCompatable) return null;
        else return toggleButton;
        }
    
    public JFrame createFrame(Stoppable stopper)
        {
        JFrame frame = super.createFrame(stopper);
        frame.setTitle("" + properties.getName(index) + " of " + properties.getObject());
        return frame;
        }
        
    /** Override this to tell the system to automatically create and display a frame for your PropertyInspector*/
    public boolean shouldCreateFrame() { return true; }
    }
