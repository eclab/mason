/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import sim.util.*;

/**
   A simple class designed to allow the user to modify a property in the form of a string, number, boolean value, or option.    PropertyField lets you control the values which the user sets by subclassing the class and overriding the newValue(val) method filters all newly user-set values and "corrects" them.  Programmatically set values (by calling setValue(...)) are not filtered through newValue by default.  If you need to filter, you should do setValue(newValue(val));

   <p>You can optionally specify how the string will be presented to the user: as a text field, as a text field with
   a slider (requiring certain numerical constraints on the text field), as a list of options (also requiring certain numerical constraints), as a check box (requiring the string to hold boolean values ("true" or "false"), or as a
   read-only field with a button to press (which in turn calls the viewProperty() method, which you may override).
   
   <p>The specifics about how to present the user with these options is described in the constructor documentation and in the documentation for setValues(...).

   <p>PropertyFields can also be set to be either read-only or read/write by the user.  When the user edits a
   read/write PropertyField, the text field changes color.  If the user then presses RETURN, the result is submitted
   to newValue(...).  If the user presses ESCAPE, the result is cancelled and reset.
*/

public class PropertyField extends JComponent
    {
    JComboBox list = new JComboBox();
    JTextField valField = new JTextField();
    JCheckBox checkField = new JCheckBox();
    JButton viewButton = new JButton("View");  // optionally displayed instead of valField (array or Object)
    JLabel viewLabel = new JLabel();
    JLabel optionalLabel = new JLabel();
    static final int SLIDER_MAX = 1000;
    static final int SLIDER_WIDTH = 80;
    JSlider slider = new JSlider(0,SLIDER_MAX)
        {
        public Dimension getMaximumSize() { return new Dimension(SLIDER_WIDTH, super.getMaximumSize().height); }
        public Dimension getPreferredSize() { return getMaximumSize(); }
        };

    DecimalFormat sliderFormatter = new DecimalFormat();        // to control the slider's number of decimal places
        
    public JTextField getField() { return valField; }

    Border valFieldBorder;
    Border emptyBorder;
    String currentValue;
    boolean isReadWrite;
    Object domain;
    
    int displayState;
    public static final int SHOW_CHECKBOX = 0;
    public static final int SHOW_TEXTFIELD = 1;
    public static final int SHOW_VIEWBUTTON = 2;
    public static final int SHOW_SLIDER = 3;
    public static final int SHOW_LIST = 4;

    Color defaultColor;
    Color editedColor = new Color(225,225,255);
        
    public void setEditedColor(Color c) { editedColor = c; }
    public Color getEditedColor() { return editedColor; }
    
    /** Commits to the current setting of the propertyField, filtering it through newValue. */
    public void submit()
        {
        if (edited) { setValue(newValue( valField.getText() )); }
        }
        
    /** Reverts the property field to its previous string value WITHOUT calling newValue() */
    public void update()
        {
        setValue(getValue());
        }

    boolean edited = false;
    void setEdited(boolean edited)
        {
        this.edited = edited;
        if (edited)
            {
            valField.setBackground(editedColor);
            }
        else
            {
            valField.setBackground(isReadWrite ? defaultColor : checkField.getBackground());
            }
        }
    
    KeyListener listener = new KeyListener()
        {
        public void keyReleased(KeyEvent keyEvent) { }
        public void keyTyped(KeyEvent keyEvent) { }
        public void keyPressed(KeyEvent keyEvent) 
            {
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER)
                {
                submit();
                }
            else if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)  // reset
                {
                update();
                }
            else
                {
                setEdited(true);
                }
            }
        };
        
    ActionListener checkListener = new ActionListener()
        {
        public void actionPerformed ( ActionEvent e )
            {
            setValue(newValue( "" + checkField.isSelected() ));
            }
        };

    ActionListener viewButtonListener = new ActionListener()
        {
        public void actionPerformed ( ActionEvent e )
            {
            viewProperty();
            }
        };
        
    FocusAdapter focusAdapter = new FocusAdapter()
        {
        public void focusLost ( FocusEvent e )
            {
            submit();
            }
        };
        

    boolean sliding = false;
    
    /*
     * Calculate the number of decimal places needed to show the smallest possible change for a slider.
     * @param low bottom of the range
     * @param high top of the range
     * @param ticks number of discrete stops within the range
     * @return the number of decimal places to show
     * @author jharrison
     */
    int calcDecimalPlacesForInterval(double low, double high, int ticks)
        {
        double epsilon = (high - low) / (double)ticks;
        return (int)Math.ceil(Math.log10(1/epsilon));
        }
      
    boolean ignoreEvent = false;  // set to true when we're first setting the PropertyField
    ChangeListener sliderListener = new ChangeListener()
        {
        public void stateChanged (ChangeEvent e)
            {
            if (!ignoreEvent && domain != null && domain instanceof Interval)
                {
                double d = 0;
                Interval domain = (Interval)(PropertyField.this.domain);
                int i = slider.getValue();
                String str;
                if (domain.isDouble())
                    {
                    double min = domain.getMin().doubleValue();
                    double max = domain.getMax().doubleValue();
                    d = (i / (double)SLIDER_MAX) * (max - min) + min;
                    sliderFormatter.setMinimumFractionDigits(calcDecimalPlacesForInterval(min, max, SLIDER_WIDTH));
                    str = sliderFormatter.format(d);
                    }
                else  // integer
                    str = Integer.toString(i);
                    
                sliding = true;
                setValue(newValue(str));
                sliding = false;
                }
            ignoreEvent = false;  // reset
            }
        };
    
    ActionListener listListener = new ActionListener()
        {
        public void actionPerformed ( ActionEvent e )
            {
            if (!settingList) 
                setValue(newValue(""+list.getSelectedIndex()));
            }
        };
    
    boolean settingList = false;
        
    /** Sets the value, not filtering it through newValue(val) first. */
    public void setValue(String val)
        {
        switch(displayState)
            {
            case SHOW_SLIDER:
                setEdited(false);
                if (!sliding) { slide(val); }
                valField.setText(val);
                break;
            case SHOW_TEXTFIELD:
                setEdited(false);
                valField.setText(val);
                break;
            case SHOW_CHECKBOX:
                if(val!=null && val.equals("true"))
                    checkField.setSelected(true);
                else
                    checkField.setSelected(false);
                break;
            case SHOW_VIEWBUTTON:
                viewLabel.setText(val);
                break;
            case SHOW_LIST:
                settingList = true;
                try { list.setSelectedIndex(Integer.parseInt(val)); }
                catch (Exception e) { settingList = false; throw new RuntimeException(""+e); }
                settingList = false;
                break;
            default:
                throw new RuntimeException("default case should never occur");
            }
        currentValue = val;
        }

    void slide(String val)
        {
        try
            {
            if (domain instanceof Interval)
                {
                Interval domain = (Interval)(this.domain);
                double d = Double.parseDouble(val);
                double min = domain.getMin().doubleValue();
                double max = domain.getMax().doubleValue();
                int i = (int)((d - min) / (max - min) * SLIDER_MAX);
                if (!domain.isDouble())
                    i = (int)d;
                slider.setValue(i);
                }
            }
        catch (Exception e) { }
        }

    /** Returns the most recently set value. */
    public String getValue()
        {
        return currentValue;
        }
    
    /** Constructs a PropertyField as just a writeable, empty text field. */
    public PropertyField()
        {
        this(null,"",true);
        }
        
    /** Constructs a PropertyField as a writeable text field with the provided initial value. */
    public PropertyField(String initialValue)
        {
        this(null,initialValue,true);
        }
    
    /** Constructs a PropertyField as a text field with the provided initial value, either writeable or not. */
    public PropertyField(String initialValue, boolean isReadWrite)
        {
        this(null,initialValue,isReadWrite);
        }
    
    /** Constructs a labelled PropertyField as a writeable text field with the provided initial value. */
    public PropertyField(String label, String initialValue)
        {
        this(label,initialValue,true);
        }

    /** Constructs a labelled PropertyField as a text field with the provided initial value, either writeable or not. */
    public PropertyField(String label, String initialValue, boolean isReadWrite)
        {
        this(label,initialValue,isReadWrite, null, SHOW_TEXTFIELD);
        }
    
    /** Constructs a PropertyField with an optional label, an initial value, a "writeable" flag, an optional domain
        (for the slider and list options), and a display form (checkboxes, view buttons, text fields, sliders, or lists).
        <ul>
        <li>If show is SHOW_CHECKBOX, a checkbox will be shown (expecting "true" and "false" string values); pass in null for domain.
        <li>If show is SHOW_VIEWBUTTON, a view button will be shown (expecting a true object); pass in null for domain.
        <li>If show is SHOW_TEXTFIELD, a textfield will be shown; pass in null for domain.
        <li>If show is SHOW_SLIDER, both a textfield and a slider will be shown; the initialValue must be a number, and
        domain must be a sim.util.Interval. 
        In this case, newValue(...) will be passed a String holding a number in the Interval range and must return
        a number.  PropertyField will automatically make certain that the numbers are integral or real-valued; you
        do not need to check this so long as the Interval returns Longs or Doubles respectively.  If isReadWrite is false,
        then the slider is not shown -- only the textfield.
        <li>If show is SHOW_LIST, a list will be shown; the initialValue must be an integer specifying the number in the list, and domain must be an array of Objects (strings, whatnot) or a java.util.List providing the objects in the list.
        In this case, newValue(...) will be passed a String holding a number; that number is the index in the list
        which the user has checked.  newValue(...) must also return a String with the desired index for the list to be
        set to.  */
    public PropertyField(String label, String initialValue, boolean isReadWrite, Object domain, int show)
        {
        // create object
        setLayout(new BorderLayout());
        add(optionalLabel,BorderLayout.WEST);
        
        valFieldBorder = valField.getBorder();
        Insets i = valFieldBorder.getBorderInsets(valField);
        emptyBorder = new EmptyBorder(i.top,i.left,i.bottom,i.right);
        
        defaultColor = valField.getBackground();
        valField.addKeyListener(listener);
        valField.addFocusListener(focusAdapter);
        checkField.addActionListener(checkListener);
        viewButton.addActionListener(viewButtonListener);
        slider.addChangeListener(sliderListener);
        list.addActionListener(listListener);
        
        // quaquaify
        viewButton.putClientProperty("Quaqua.Button.style","square");
        
        if ((domain != null) && (domain instanceof Interval)) 
            {
            Interval interval = (Interval)domain;
            if (interval.isDouble())
                { 
                // nothing
                }
            else 
                {

                slider.setMinimum(interval.getMin().intValue());
                slider.setMaximum(interval.getMax().intValue());
                }
            }
        
        sliderFormatter.setGroupingUsed(false); // no commas
                
        // set values
        ignoreEvent = true;  // don't change the underlying data yet
        setValues(label, initialValue, isReadWrite, domain, show);
        }


    /* Resets a PropertyField with an optional label, an initial value, a "writeable" flag, an optional domain
       (for the slider and list options), and a display form (checkboxes, view buttons, text fields, sliders, or lists).
       <ul>
       <li>If show is SHOW_CHECKBOX, a checkbox will be shown (expecting "true" and "false" string values); pass in null for domain.
       <li>If show is SHOW_VIEWBUTTON, a view button will be shown (expecting a true object); pass in null for domain.
       <li>If show is SHOW_TEXTFIELD, a textfield will be shown; pass in null for domain.
       <li>If show is SHOW_SLIDER, both a textfield and a slider will be shown; the initialValue must be a number, and
       domain must be a sim.util.Interval. 
       In this case, newValue(...) will be passed a String holding a number in the Interval range and must return
       a number.  PropertyField will automatically make certain that the numbers are integral or real-valued; you
       do not need to check this so long as the Interval returns Longs or Doubles respectively.  If isReadWrite is false,
       then the slider is not shown -- only the textfield.
       <li>If show is SHOW_LIST, a list will be shown; the initialValue must be an integer specifying the number in the list, and domain must be an array of Objects (strings, whatnot) or a java.util.List providing the objects in the list.
       In this case, newValue(...) will be passed a String holding a number; that number is the index in the list
       which the user has checked.  newValue(...) must also return a String with the desired index for the list to be
       set to.
    */
    void setValues(String label, String initialValue, boolean isReadWrite, Object domain, int show)
        {
        this.domain = domain;
        removeAll();
        add(optionalLabel,BorderLayout.WEST);
        
        // some conversions
        if (show==SHOW_SLIDER && !isReadWrite) show = SHOW_TEXTFIELD;
        if (domain !=null && domain.getClass().isArray())
            {
            domain = Arrays.asList((Object[])domain);
            }

        displayState = show;
        switch(displayState)
            {
            case SHOW_SLIDER:
                JPanel p = new JPanel();
                p.setLayout(new BorderLayout());
                p.add(valField, BorderLayout.CENTER);
                if (isReadWrite && domain!=null && domain instanceof Interval)
                    p.add(slider, BorderLayout.WEST);
                add(p,BorderLayout.CENTER);
                break;
            case SHOW_TEXTFIELD:
                add(valField, BorderLayout.CENTER);
                break;
            case SHOW_CHECKBOX:
                add(checkField, BorderLayout.CENTER);
                break;
            case SHOW_VIEWBUTTON:
                add(viewLabel, BorderLayout.CENTER);
                add(viewButton, BorderLayout.WEST);
                break;
            case SHOW_LIST:
                if (domain != null && domain instanceof java.util.List)
                    {
                    settingList = true;
                    list.setEditable(false);
                    list.setModel(new DefaultComboBoxModel(new Vector((java.util.List)domain)));
                    add(list,BorderLayout.CENTER);
                    list.setEnabled(isReadWrite);
                    settingList = false;
                    }
                break;
            default:
                throw new RuntimeException("default case should never occur");
            }
        revalidate();
        repaint();
        
        currentValue = initialValue;
        optionalLabel.setText(label);
        
        checkField.setEnabled(isReadWrite);
        valField.setEditable(isReadWrite);
        valField.setBorder(isReadWrite? valFieldBorder : emptyBorder);
        
        this.isReadWrite = isReadWrite;
        setValue(currentValue);
        }
        
    /** Override this to be informed when a new value has been set.
        The return value should be the value you want the display to show 
        instead. */
    public String newValue(String newValue)
        {
        return newValue;
        }

    /** Override this to be informed when a property is to be viewed in its
        own inspector because the user pressed the "view" button. */
    public void viewProperty()
        {
        }

    public void setToolTipText(String text)
        {
        super.setToolTipText(text);
        valField.setToolTipText(text);
        checkField.setToolTipText(text);
        optionalLabel.setToolTipText(text);
        viewButton.setToolTipText(text);
        viewLabel.setToolTipText(text);
        slider.setToolTipText(text);
        list.setToolTipText(text);
        }
        
    public Dimension getMinimumSize()
        {
        Dimension s = super.getMinimumSize();
        s.height = valField.getMinimumSize().height;
        return s;
        }
    public Dimension getPreferredSize()
        {
        Dimension s = super.getPreferredSize();
        s.height = valField.getPreferredSize().height;
        return s;
        }
 
    public void setEnabled(boolean b)
        {
        super.setEnabled(b);
        valField.setEnabled(b);
        checkField.setEnabled(b);
        optionalLabel.setEnabled(b);
        viewButton.setEnabled(b);
        viewLabel.setEnabled(b);
        slider.setEnabled(b);
        list.setEnabled(b);
        }
    }
