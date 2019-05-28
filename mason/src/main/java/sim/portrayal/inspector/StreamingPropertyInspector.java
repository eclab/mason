/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;
import java.awt.*;
import java.awt.event.*;
import sim.util.*;
import java.io.*;
import sim.display.*;
import sim.engine.*;
import javax.swing.*;
import sim.util.gui.*;

/** A PropertyInspector which streams its result out to a file, window, or stream. */

public class StreamingPropertyInspector extends PropertyInspector
    {
    PrintWriter out;
    boolean shouldCloseOnStop = true;
    int streamingTo;
    int interval = 1;
    static final int CUSTOM = -1;
    static final int FILE = 0;
    static final int WINDOW = 1;
    static final int STDOUT = 2;
    JTextArea area;
    JScrollPane pane;
    JFrame frame;
    double lastTime  = Schedule.BEFORE_SIMULATION;
    
    public static String name() { return "Stream"; }
    public static Class[] types() { return null; } // accepts all types
        
    /** Creates a custom StreamingPropertyInspector which writes to the provided stream, with the associated short name streamName.
        This constructor is intended for people who want to create custom streaming inspectors programmatically. */
    public StreamingPropertyInspector(Properties properties, int index, 
        Frame parent, GUIState simulation, PrintWriter stream, String streamName)
        {
        super(properties,index,parent,simulation);
        out = stream;
        setLayout(new BorderLayout());
        add(new JLabel("Streaming to..."), BorderLayout.NORTH);
        add(new JLabel(streamName), BorderLayout.CENTER);
        streamingTo = CUSTOM;
        setValidInspector(true);
        }
        
    public StreamingPropertyInspector(final Properties properties, final int index, final Frame parent, final GUIState simulation)
        {
        super(properties,index,parent,simulation);
                
        Object[] possibilities = {"A file (overwriting)",
                                  "A file (appending)",
                                  "A window", 
                                  "Standard Out"};
        String s = (String)JOptionPane.showInputDialog(
            parent,
            "Stream the property to:",
            "Stream",
            JOptionPane.PLAIN_MESSAGE,
            null,
            possibilities,
            possibilities[0]);
                                        
        if (s!=null)  // valid inspector maybe?
            {
            // make the interval (skip) field
            NumberTextField skipField = new NumberTextField("Skip: ", 1, false)
                {
                public double newValue(double newValue)
                    {
                    int val = (int) newValue;
                    if (val < 1) val = (int)currentValue;
                    interval = val;
                    return val;
                    }
                };
            skipField.setToolTipText("Specify the number of steps between stream fetches");
            skipField.setBorder(BorderFactory.createEmptyBorder(2,2,0,2));
                        
            if (s.equals(possibilities[0]) || s.equals(possibilities[1]))
                {
                streamingTo = FILE;
                                
                FileDialog fd = new FileDialog(parent,"Stream the Property " + 
                    (s.equals(possibilities[1]) ? "(appending) " : "") + 
                    "\"" + properties.getName(index) + "\" to File...", FileDialog.SAVE);
                fd.setFile(properties.getName(index)+".out");
                fd.setVisible(true);
                if (fd.getFile()!=null) try
                                            {
                                            File file = new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(),".out"));
                                            // we'll make a writer that appends or doesn't append -- note we use
                                            // new FileWriter(String,appends) because new FileWriter(file,appends) 
                                            // is only available in Java 1.4 and up.
                                            out = new PrintWriter(new BufferedWriter(new FileWriter(file.getCanonicalPath(), s.equals(possibilities[1]))));
                                            setLayout(new BorderLayout());
                                            Box b = new Box(BoxLayout.Y_AXIS);
                                            b.add(skipField);
                                            b.add(new JLabel("Streaming to" + 
                                                    (s.equals(possibilities[1]) ? " (appending)" : "") + 
                                                    "..."));
                                            b.add(new JLabel(file.getPath()));
                                            b.add(new JLabel("Format: \"timestamp: value\""));
                                            b.add(Box.createGlue());
                                            add(b,BorderLayout.NORTH);
                                            setValidInspector(true);
                                            }
                    catch (IOException e)
                        {
                        e.printStackTrace();
                        }
                }
            else if (s.equals(possibilities[2]))
                {
                streamingTo = WINDOW;
                area = new JTextArea();
                pane = new JScrollPane(area);
                setLayout(new BorderLayout());
                add(pane,BorderLayout.CENTER);
                add(skipField, BorderLayout.NORTH);
                Box box = new Box(BoxLayout.X_AXIS);
                JButton saveButton = new JButton("Save Contents");
                box.add(saveButton);
                saveButton.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        synchronized(simulation.state.schedule)  // stop the simulation for a sec
                            {
                            FileDialog fd = new FileDialog(frame,"Save the (Present) Contents to File...", FileDialog.SAVE);
                            fd.setFile(properties.getName(index)+".out");
                            fd.setVisible(true);
                            if (fd.getFile()!=null) try
                                                        {
                                                        File file = new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(),".out"));
                                                        // we'll make a writer that appends or doesn't append -- note we use
                                                        // new FileWriter(String,appends) because new FileWriter(file,appends) 
                                                        // is only available in Java 1.4 and up.
                                                        PrintWriter p = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                                                        p.println(area.getText());
                                                        p.close();
                                                        }
                                catch (IOException ex)
                                    {
                                    ex.printStackTrace();
                                    }
                            }
                        }                                       
                    });
                box.add(new JLabel("Format: \"timestamp: value\""));
                box.add(Box.createGlue());
                add(box, BorderLayout.SOUTH);
                setValidInspector(true);
                }
            else // s.equals(possibilities[3])
                {
                streamingTo = STDOUT;
                shouldCloseOnStop = false;  // don't want to close System.out!
                setLayout(new BorderLayout());
                Box b = new Box(BoxLayout.Y_AXIS);
                b.add(skipField);
                b.add(new JLabel("Streaming to Standard Out"),BorderLayout.CENTER);
                b.add(new JLabel("Format: \"timestamp/object/property: value\""));
                b.add(Box.createGlue());
                add(b,BorderLayout.NORTH);
                setValidInspector(true);
                }
            }
        }
                
    public void updateInspector()
        {
        double time = simulation.state.schedule.getTime();
        if (time >= Schedule.EPOCH && time < Schedule.AFTER_SIMULATION &&
            lastTime <= time - interval)
            {
            lastTime = time;
            switch(streamingTo)
                {
                case FILE: case CUSTOM:
                    if (out != null) out.println(time + ": " + properties.getValue(index));
                    break;
                case WINDOW:
                    area.append(time + ": " + properties.getValue(index) + "\n");
                    break;
                case STDOUT:
                    System.out.println(properties.getObject() + "/" + properties.getName(index) + 
                        "/" + time + ": " + properties.getValue(index));
                    break;
                default:
                    throw new RuntimeException("default case should never occur");
                }
            }
        }
    
    public Stoppable reviseStopper(Stoppable stopper)
        {
        // we want to flush our streams if we've been stopped
        final Stoppable newStopper = super.reviseStopper(stopper);
        return new Stoppable()
            {
            public void stop()
                {
                if (newStopper != null) newStopper.stop();
                if (out!=null)
                    {
                    if (streamingTo == STDOUT) out.flush();
                    else if (streamingTo == FILE || streamingTo == CUSTOM) out.close();
                    }
                out = null;  // so we don't write to it any more
                }
            };
        }
        
    public JFrame createFrame(final Stoppable stopper)
        {
        frame = super.createFrame(stopper);
        frame.getContentPane().setLayout(new BorderLayout());  // just in case
        frame.getContentPane().removeAll();  // get rid of the built-in scroller
        frame.getContentPane().add(this, BorderLayout.CENTER);
        if (pane!=null) frame.setSize(400,300);
        else frame.pack();
        return frame;
        }
    }
