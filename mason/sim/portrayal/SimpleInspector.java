/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import sim.portrayal.inspector.*;
import java.awt.*;
import sim.engine.*;
import sim.util.gui.*;
import sim.util.*;
import sim.display.*;
import javax.swing.*;
import java.awt.event.*;

/**
   A simple inspector class that looks at the "getX" and "setX" method of the object to be investigates
   and creates a user-friendly graphical interface with read only and read/write components of the object.
   
   <p>SimpleInspector automatically creates an UpdateButton and adds it to itself at position BorderLayout.NORTH
   whenever you set it to be non-volatile, and when you set it to be volatile, it removes the UpdateButton.
*/

public class SimpleInspector extends Inspector
    {
    public static final int MAX_PROPERTIES = 25;
    /** The GUIState  of the simulation */
    public GUIState state;
    /** The object being inspected */
    public Object object;
    /** The property list displayed -- this may change at any time */
    public LabelledList propertyList;
    /** The generated object properties -- this may change at any time */
    public Properties properties;
    /** Each of the property fields in the property list, not all of which may exist at any time. */
    public PropertyField[] members = new PropertyField[0];
    /** The displayed name of the inspector */
    public String name;
    /** The current index of the topmost element */
    public int start = 0;
    /** The number of items presently in the propertyList */
    public int count = 0;
    public JPanel header = new JPanel()
        {
        public Insets getInsets () { return new Insets(2,2,2,2); }
        };

    public JLabel numElements = new JLabel();
    public Box startField = null;
    
    
    boolean fixedProperties = false;
    public SimpleInspector(Properties properties, GUIState state, String name)
        {
        super();
        setLayout(new BorderLayout());
        this.object = null;
        this.state = state;
        this.name = name;
        this.properties = properties;
        this.fixedProperties = true;
        header.setLayout(new BorderLayout());
        add(header,BorderLayout.NORTH);
        generateProperties(0);
        }
        
    public SimpleInspector(Object object, GUIState state)
        {
        this(object,state,null);
        }
    
    public SimpleInspector(Object object, GUIState state, String name) 
        { 
        super();
        setLayout(new BorderLayout());
        this.object = object;
        this.state = state;
        this.name = name;
        header.setLayout(new BorderLayout());
        add(header,BorderLayout.NORTH);
        generateProperties(0);
        }
    
    /* Creates a JPopupMenu that possibly includes "View" to
       view the object instead of using the ViewButton.  If not, returns null. */
    JPopupMenu makePreliminaryPopup(final int index)
        {
        Class type = properties.getType(index);
        if (properties.isComposite(index))
            {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem menu = new JMenuItem("View");
            menu.setEnabled(true);
            menu.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    Properties props = properties;
                    final SimpleInspector simpleInspector = new SimpleInspector(props.getValue(index), SimpleInspector.this.state);
                    final Stoppable stopper = simpleInspector.reviseStopper(
                        SimpleInspector.this.state.scheduleImmediateRepeat(true,simpleInspector.getUpdateSteppable()));
                    SimpleInspector.this.state.controller.registerInspector(simpleInspector,stopper);
                    JFrame frame = simpleInspector.createFrame(stopper);
                    frame.setVisible(true);
                    }
                });
            popup.add(menu);
            return popup;
            }
        else return null;
        }
    
    PropertyField makePropertyField(final int index)
        {
        Class type = properties.getType(index);
        return new PropertyField(
            null,
            properties.betterToString(properties.getValue(index)),
            properties.isReadWrite(index),
            properties.getDomain(index),
                (properties.isComposite(index) ?
                //PropertyField.SHOW_VIEWBUTTON : 
                PropertyField.SHOW_TEXTFIELD :
                    (type == Boolean.TYPE || type == Boolean.class ?
                    PropertyField.SHOW_CHECKBOX :
                        (properties.getDomain(index) == null ? PropertyField.SHOW_TEXTFIELD :
                        (properties.getDomain(index) instanceof Interval) ? 
                        PropertyField.SHOW_SLIDER : PropertyField.SHOW_LIST ))))
            {
            Properties props = properties;

            // The return value should be the value you want the display to show instead.
            public String newValue(final String newValue)
                {
                // the underlying model could still be running, so we need
                // to do this safely
                synchronized(SimpleInspector.this.state.state.schedule)
                    {
                    // try to set the value
                    if (props.setValue(index, newValue) == null)
                        java.awt.Toolkit.getDefaultToolkit().beep();
                    // refresh the controller -- if it exists yet
                    if (SimpleInspector.this.state.controller != null)
                        SimpleInspector.this.state.controller.refresh();
                    // set text to the new value
                    return props.betterToString(props.getValue(index));
                    }
                }
            };
        }
    
    /** Private method.  Does a repaint that is guaranteed to work (on some systems, plain repaint())
        fails if there's lots of updates going on as is the case in our simulator thread.  */
    void doEnsuredRepaint(final Component component)
        {
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                if (component!=null) component.repaint();
                }
            });
        }

    void generateProperties(int start)
        {
        if (!fixedProperties)
            properties = Properties.getProperties(object,true,true,false,true);
        final int len = properties.numProperties();
        if (start < 0) start = 0;
        if (start > len) return;  // failed
                
        if (propertyList != null) 
            remove(propertyList);
        propertyList = new LabelledList(name);

        if (len > MAX_PROPERTIES)
            {
            final String s = "Page forward/back through properties.  " + MAX_PROPERTIES + " properties shown at a time.";
            if (startField == null)
                {
                NumberTextField f = new NumberTextField(" Go to ", start,1,MAX_PROPERTIES)
                    {
                    public double newValue(double newValue)
                        {
                        int newIndex = (int) newValue;
                        if (newIndex<0) newIndex = 0;
                        if (newIndex >= len) return (int)getValue();
                        // at this point we need to build a new properties list!
                        generateProperties(newIndex);
                        return newIndex; // for good measure, though it'll be gone by now
                        }
                    };

                f.setToolTipText(s);
                numElements.setText(" of " + len + " ");
                numElements.setToolTipText(s);
                f.valField.setColumns(4);
                startField = new Box(BoxLayout.X_AXIS);
                startField.add(f);
                startField.add(numElements);
                startField.add(Box.createGlue());
                header.add(startField, BorderLayout.CENTER);
                }
            }
        else 
            {
            start = 0;
            if (startField!=null) header.remove(startField);
            }

        members = new PropertyField[len];

        int end = start + MAX_PROPERTIES;
        if (end > len) end = len;
        count = end - start;
        for( int i = start ; i < end; i++ )
            {
            if (!properties.isHidden(i))  // don't show if the user asked that it be hidden
                {
                members[i] = makePropertyField(i);
                propertyList.add(null,
                    new JLabel(properties.getName(i) + " "), 
                    PropertyInspector.getPopupMenu(properties,i,state, makePreliminaryPopup(i)), 
                    members[i], 
                    null);
                }
            else members[i] = null;
            }
        add(propertyList, BorderLayout.CENTER);
        this.start = start;
        revalidate();
        }
    
    JButton updateButton = null;
    public void setVolatile(boolean val)
        {
        super.setVolatile(val);
        if (isVolatile())
            {
            if (updateButton!=null) 
                {
                header.remove(updateButton); revalidate();
                }
            }
        else
            {
            if (updateButton==null)
                {
                updateButton = (JButton) makeUpdateButton();
                                
                // modify height -- stupid MacOS X 1.4.2 bug has icon buttons too big
                NumberTextField sacrificial = new NumberTextField(1,true);
                Dimension d = sacrificial.getPreferredSize();
                d.width = updateButton.getPreferredSize().width;                                
                updateButton.setPreferredSize(d);
                d = sacrificial.getMinimumSize();
                d.width = updateButton.getMinimumSize().width;
                updateButton.setMinimumSize(d);
                                
                // add to header
                header.add(updateButton,BorderLayout.WEST);
                revalidate(); 
                }
            } 
        }

    public void updateInspector()
        {
        if (properties.isVolatile())  // need to rebuild each time, YUCK
            {
            remove(propertyList);
            generateProperties(start);
            doEnsuredRepaint(this);
            }
        else for( int i = start ; i < start+count ; i++ )
                 if (members[i] != null) 
                     members[i].setValue(properties.betterToString(properties.getValue(i)));
        }
                
    public String getTitle()
        {
        return "" + object;
        }
    }
