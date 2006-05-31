/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display;
import java.applet.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/** A simple class for creating Applets out of your simulations.  Due to security restrictions,
    certain things will not work, such as reading files (loading models) or writing files
    (producing movies etc.).
    
    <p>You can check to see if you're running under an applet by testing the value of
    SimApplet.isApplet
*/
public class SimApplet extends Applet 
    {
    // some random static stuff to force Display2D and Console classes to load, as they have
    // some important properties to set
    private static boolean b;
    private static int i;
    static
        {
        b = Display2D.isMacOSX();
        i = Console.DEFAULT_WIDTH;
        }

    public static boolean isApplet = false;
    public SimApplet() { isApplet = true; }
    
    public void init() 
        {
        String simClassName = getParameter("Simulation");
        String simHumanName = getParameter("Name");
        final JButton button = new JButton("Start " + simHumanName);
        setLayout(new BorderLayout());
        add(button, BorderLayout.CENTER);
        try
            {
            // load that class
            final Class simClass = Class.forName(simClassName);
            if (!(GUIState.class.isAssignableFrom(simClass)) && !simClass.equals(sim.display.Console.class))  // if it's not a GUIState or Console
                throw new Exception("Class is not a GUIState or the Console: " + simClass);
            button.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent evt)
                    {
                    try
                        {
                        setupApplet(simClass);
                        }
                    catch (Exception e)
                        {
                        doException(button,e);
                        }
                    }
                });
            }
        catch (Exception e)
            {
            doException(button,e);
            }
        }
        
    public void setupApplet(Class GUIStateClass) throws Exception  // yes, purposely capitalized
        {
        if (GUIStateClass.equals(Console.class))
            Console.main(new String[] {});
        else
            {
            GUIState state = (GUIState)(GUIStateClass.newInstance());
            Console c = new Console(state);
            c.setVisible(true);
            }
        }
    
    public void doException(JComponent button, Exception e)
        {
        JTextArea text = new JTextArea();
        text.setText("" + e);
        JScrollPane scroll = new JScrollPane(text);
        if (button != null) remove(button);
        add(scroll,BorderLayout.CENTER);
        e.printStackTrace();        
        }
    }
