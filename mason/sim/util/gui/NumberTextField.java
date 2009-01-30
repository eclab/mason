/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

/** A simple class that lets you specify a label and validate a numerical value.  NumberTextField assumes access
    to several image files for the widgets to the right of the text field: a left-arrow button, a right-arrow button, 
    and a "belly button".  The left-arrow button decreases the numerical value, the right-arrow button increases it, 
    and the belly button resets it to its initial default value.  You can also change the value in the text field proper.  
    Why use this class instead of a slider?  Because it is not ranged: the numbers can be any value.

    <p>NumberTextField lets users increase values according to a provided formula of the form
    value = value * M + A, and similarly decrease values as value = (value - A) / M. You specify the
    values of M and A and the initial default value.  This gives you some control on how values should change:
    linearly or geometrically.

    <p>You can exercise further control by subclassing the class and overriding the newValue(val) method, which
    filters all newly user-set values and "corrects" them.  Programmatically set values (by calling setValue(...)) 
    are not filtered through newValue by default.  If you need to filter, you should do setValue(newValue(val));

    <p>NumberTextFields can also be provided with an optional label.
*/

public class NumberTextField extends JComponent
    {
    public JTextField valField = new JTextField();
    public JButton downButton;
    public JButton upButton;
    public JButton bellyButton;
    public JLabel fieldLabel;
    public double initialValue;
    public double multiply;
    public double add;
    public double currentValue;

    public Color defaultColor;
    public Color editedColor = new Color(225,225,255);
    public void setEditedColor(Color c) { editedColor = c; }
    public Color getEditedColor() { return editedColor; }

    public static final ImageIcon I_DOWN = iconFor("LeftArrow.png");
    public static final ImageIcon I_DOWN_PRESSED = iconFor("LeftArrowPressed.png");
    public static final ImageIcon I_BELLY = iconFor("BellyButton.png");
    public static final ImageIcon I_BELLY_PRESSED = iconFor("BellyButtonPressed.png");
    public static final ImageIcon I_UP = iconFor("RightArrow.png");
    public static final ImageIcon I_UP_PRESSED = iconFor("RightArrowPressed.png");

    public static ImageIcon iconFor(String name)
        {
        return new ImageIcon(NumberTextField.class.getResource(name));
        }
    
    boolean edited = false;
    void setEdited(boolean edited)
        {
        if (this.edited != edited)
            {
            this.edited = edited;
            if (edited)
                {
                valField.setBackground(editedColor);
                }
            else
                {
                valField.setBackground(defaultColor);
                }
            }
        }
        
    public void submit()
        {
        if (edited)
            {
            double val;
            try
                {
                val = Double.parseDouble(valField.getText());
                }
            catch (NumberFormatException e) { val = initialValue; }
            setValue(newValue(val));
            }
        }
        
    public void update()
        {
        setValue(getValue());
        }

    public KeyListener listener = new KeyListener()
        {
        public void keyReleased(KeyEvent keyEvent) { }
        public void keyTyped(KeyEvent keyEvent) { }
        public void keyPressed(KeyEvent keyEvent) {
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
    
    public FocusAdapter focusAdapter = new FocusAdapter()
        {
        public void focusLost ( FocusEvent e )
            {
            submit();
            }
        };

    /** Sets the value without filtering first. */
    public void setValue(double val)
        {
        if (((int)val) == val)
            valField.setText(""+(int)val);
        else valField.setText(""+val);
        currentValue = val;
        setEdited(false);
        }

    
    /** Returns the most recently set value. */
    public double getValue()
        {
        return currentValue;
        }
    
    
    /** Creates a NumberTextField which does not display the belly button or arrows. */
    public NumberTextField(double initialValue)
        {
        this(null,initialValue,0,0);
        }
    
    /** Creates a NumberTextField which (if <code>doubleEachTime</code>)
        doubles or halves the current value, or (if not <code>doubleEachTime</code>) 
        increases or decreases by 1 each time. */
    
    public NumberTextField(double initialValue, boolean doubleEachTime)
        {
        this(null,initialValue,doubleEachTime);
        }
    
    /** Creates a NumberTextField according to the provided parameters.
        initialValue specifies the initial number value.  multiply and add work as follows:
        If the right arrow is pressed, then the current value is changed to newValue(value * multiply + add);
        similarly, if the left arrow is pressed, then the current value is changed to newValue((value - add) / multiply);
        if the belly button is pressed, then the current value is changed to newValue(initialValue);
        Common settings include:  
        <br><br>initialValue = 1, multiply = 2, add = 0  (start at 1, double each time)
        <br>initialValue = 0, multiply = 1, add = 1   (start at 0, add 1 each time)

        <p>If multiply is 0, then no arrows are shown at all.
    */
    public NumberTextField(double initialValue, double multiply, double add)
        {
        this(null,initialValue,multiply,add);
        }
        
    /** Creates a NumberTextField with a provided label.  If the label is null or empty, no label is created.
        Creates a NumberTextField which (if <code>doubleEachTime</code>)
        doubles or halves the current value, or (if not <code>doubleEachTime</code>) 
        increases or decreases by 1 each time.
    */
    
    public NumberTextField(String label, double initialValue, boolean doubleEachTime)
        {
        if (doubleEachTime) 
            setValues(label,initialValue,2,0);
        else
            setValues(label,initialValue,1,1);
        }

    /** Creates a NumberTextField with a provided label.  If the label is null or empty, no label is created.
        initialValue specifies the initial number value.  multiply and add work as follows:
        If the right arrow is pressed, then the current value is changed to newValue(value * multiply + add);
        similarly, if the left arrow is pressed, then the current value is changed to newValue((value - add) / multiply);
        if the belly button is pressed, then the current value is changed to newValue(initialValue);
        Common settings include:  
        <br><br>initialValue = 1, multiply = 2, add = 0  (start at 1, double each time)
        <br>initialValue = 0, multiply = 1, add = 1   (start at 0, add 1 each time)
        <p>If multiply is 0, then no arrows are shown at all.
    */
    public NumberTextField(String label, double initialValue, double multiply, double add)
        {
        setValues(label,initialValue,multiply,add);
        }
        
    //final static Color transparentBackground = new JPanel().getBackground();  // sacrificial JPanel
    protected void setValues(String label, double initialValue, double multiply, double add)
        {
        defaultColor = valField.getBackground();

        this.initialValue = initialValue;
        this.multiply = multiply;
        this.add = add;
        
        currentValue = initialValue;
        
        setLayout(new BorderLayout());

        if (label!=null && label.length() != 0)
            add(fieldLabel = new JLabel(label),BorderLayout.WEST);
        
        valField.addKeyListener(listener);
        valField.addFocusListener(focusAdapter);
        setValue(initialValue);
        add(valField,BorderLayout.CENTER);
        
        if (multiply != 0.0)
            {
            Box box = new Box(BoxLayout.X_AXIS);
            
            // add the up, bellybutton, and down buttons
            downButton = new JButton(I_DOWN);
            downButton.setPressedIcon(I_DOWN_PRESSED);
            downButton.addActionListener(
                new ActionListener()
                    { public void actionPerformed(ActionEvent e)
                        { 
                        setValue(newValue((getValue() - NumberTextField.this.add) / NumberTextField.this.multiply ));
                        }});
            downButton.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));//4,4,4,4));
            downButton.setBorderPainted(false);
            downButton.setContentAreaFilled(false);
            //downButton.setBackground(transparentBackground);  // looks better in Windows
            box.add(downButton);
            bellyButton = new JButton(I_BELLY);
            bellyButton.setPressedIcon(I_BELLY_PRESSED);
            bellyButton.addActionListener(
                new ActionListener()
                    { public void actionPerformed(ActionEvent e)
                        { 
                        setValue(newValue(NumberTextField.this.initialValue));
                        }});
            bellyButton.setBorder(BorderFactory.createEmptyBorder(1,0,1,0)); //2,2,2,2));
            bellyButton.setBorderPainted(false);
            bellyButton.setContentAreaFilled(false);
            //bellyButton.setBackground(transparentBackground);  // looks better in Windows
            box.add(bellyButton);        
            upButton = new JButton(I_UP);
            upButton.setPressedIcon(I_UP_PRESSED);
            upButton.addActionListener(
                new ActionListener()
                    { public void actionPerformed(ActionEvent e)
                        { 
                        setValue(newValue(getValue() * NumberTextField.this.multiply + NumberTextField.this.add));
                        }});
            upButton.setBorder(BorderFactory.createEmptyBorder(1,1,1,1)); //2,2,2,2));
            upButton.setBorderPainted(false);
            upButton.setContentAreaFilled(false);
            //upButton.setBackground(transparentBackground);  // looks better in Windows
            box.add(upButton);
            add(box,BorderLayout.EAST);
            }
        }
        
    /** Override this to be informed when a new value has been set.
        The return value should be the value you want the display to show 
        instead. */
    public double newValue(double newValue)
        {
        return newValue;
        }
    
    public void setToolTipText(String text)
        {
        super.setToolTipText(text);
        if (downButton!=null) downButton.setToolTipText(text);
        if (upButton!=null) upButton.setToolTipText(text);
        if (bellyButton!=null) bellyButton.setToolTipText(text);
        if (valField!=null) valField.setToolTipText(text);
        if (fieldLabel!=null) fieldLabel.setToolTipText(text);
        }
        
    public void setEnabled(boolean b)
        {
        if (downButton!=null) downButton.setEnabled(b);
        if (upButton!=null) upButton.setEnabled(b);
        if (bellyButton!=null) bellyButton.setEnabled(b);
        if (valField!=null) valField.setEnabled(b);
        if (fieldLabel!=null) fieldLabel.setEnabled(b);
        }
    }
