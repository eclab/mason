/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import sim.engine.*;
import java.awt.*;
import java.text.*;
import java.util.*;
import ec.util.*;
import java.io.*;
import sim.util.*;
import sim.util.gui.*;
import sim.portrayal.*;
import java.lang.ref.*;

/**
   Console is an elaborate Controller which provides a variety of GUI niceties to control the basics
   of a simulation.  Most significantly it provides:

   <ul>
   <li>Playing, stopping, pausing, stepping, and various associated controls
   <li>View of the current time and frame rate
   <li>Control of the random number generator
   <li>An HTML "page" of information about the model
   <li>Loading and saving checkpointed simulations, and starting new simulation classes
   <li>Hiding and showing simulation displays
   <li>Storage for inspectors
   </ul>

   <p>Console maintains the underlying play thread of the model, and handles much of the complexities
   that come with doing threads.
   
   <p>When you create new simulations (by frobbing the "New Simulation..." menu), you're presented with
   a ComboBox.  Here you can type in any simulation class you like, or you can pick from a collection of
   pre-defined classes.  The pre-defined class names are stored in the text file "simulation.classes",
   located in the same directory as the Console.class file.  Feel free to edit it.
   
   <p>While normally you'd start a MASON application by running a main() method created by the developer,
   you can also fire up the Console directly and pick a model from the aforementioned ComboBox.  To do
   this, simply run <tt>java sim.display.Console</tt>
   
   <p>If you <i>attach</i> a Frame to the Console, it will appear in the Console's "Displays" tab, where
   the user has control over hiding and showing various frames.  The best time to do such attaching
   is during your GUIState's init() method.  Such Frames should be set to hide (not dispose) when closed.
   JFrames do this by default.
   
   <p>Console places itself on-screen using the following rule.  First it moves itself to an unusual location
   (presently -10000 x -10000).  Then it calls init() on your GUIState.  If in init() you move the Console
   to a position, then that's where it will stay.  If not, then the Console looks up all the Frames attached
   to it during init() and places itself to the right of the rightmost such Frame, if there is room on the
   main display.  If not, then Console puts itself in its default position (typically the top left corner
   of the screeen).

   <p>Console generates a mammoth number of anonymous subclasses.  Well, such is life with a complicated GUI I guess.
   Don't be daunted by them -- almost all of them are little tiny things like Runnables to pass into 
   SwingUtilities.invokeLater() or various anonymous listeners and adapters for buttons and text fields etc.
*/

public class Console extends JFrame implements Controller
    {
    /** Default width of the Console. */
    public final static int DEFAULT_WIDTH = 380;
    /** Default height of the Console. */
    public final static int DEFAULT_HEIGHT = 380;
    /** When the Console is laid out to the right of some window, the space allocated between it and the window */
    public final static int DEFAULT_GUTTER = 5;

    /** Our simulation */
    public GUIState simulation;
    
    /** List of fully qualified classnames to include in the Console's "New Simulation" combo box */
    public static Vector classNames = new Vector();
    /** List of short descriptive names for the classes in classNames.  If any one of them is null, then the
        name must be fetched using the GUIState.getName(class) method */
    public static Vector shortNames = new Vector();
    
    /** Do we only allow the user to type in other classNames? */
    public static boolean allowOtherClassNames;

    /** Sets various MacOS X features */
    static 
        {
        // use heavyweight tooltips -- otherwise they get obscured by the Canvas3D
        // [this appears to be ignored by MacOS X Java 1.4.1 and 1.4.2.  A bug? ]
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        // Use Quaqua if it exists
        try
            {
            System.setProperty( "Quaqua.TabbedPane.design","auto" );  // UI Manager Properties docs differ
            System.setProperty( "Quaqua.visualMargin","1,1,1,1" );
            UIManager.put("Panel.opaque", Boolean.TRUE);
            UIManager.setLookAndFeel((String)(Class.forName("ch.randelshofer.quaqua.QuaquaManager").
                    getMethod("getLookAndFeelClassName",(Class[])null).invoke(null,(Object[])null)));
            } 
        catch (Exception e) { /* e.printStackTrace(); */ }

        try  // now we try to set certain properties if the security permits it
            {
            // turn on hardware acceleration on MacOS X.  As of September 2003, 1.3.1
            // turns this off by default, which makes 1.3.1 half the speed (and draws
            // objects wrong to boot).
            System.setProperty("com.apple.hwaccel","true");  // probably settable as an applet.  D'oh! Looks like it's ignored.
            System.setProperty("apple.awt.graphics.UseQuartz","true");  // counter the awful effect in OS X's Sun Renderer
            // the following are likely not settable
            // macOS X 1.4.1 java doesn't show the grow box.  We force it here.
            System.setProperty("apple.awt.showGrowBox","true");
            // we set this so that macos x application packages appear as files
            // and not as directories in the file viewer.  Note that this is the 
            // 1.3.1 version -- Apple gives us an obnoxious warning in 1.4.1 when
            // we call forth an open/save panel saying we should now use
            // apple.awt.use-file-dialog-packages instead, as if 1.3.1 isn't also
            // in common use...
            System.setProperty("com.apple.macos.use-file-dialog-packages","true");
            }
        catch (Exception e) { }
        }

    /** Returns icons for a given filename, such as "NotPlaying.png". A utility function. */
    public static ImageIcon iconFor(String name)
        {
        return new ImageIcon(Console.class.getResource(name));
        }
    
    public static final ImageIcon I_PLAY_ON = iconFor("Playing.png");
    public static final ImageIcon I_PLAY_OFF = iconFor("NotPlaying.png");
    public static final ImageIcon I_STOP_ON = iconFor("Stopped.png");
    public static final ImageIcon I_STOP_OFF = iconFor("NotStopped.png");
    public static final ImageIcon I_PAUSE_ON = iconFor("PauseOn.png");
    public static final ImageIcon I_PAUSE_OFF = iconFor("PauseOff.png");
    public static final ImageIcon I_STEP_ON = iconFor("StepOn.png");
    public static final ImageIcon I_STEP_OFF = iconFor("StepOff.png");

    /** The HTML Display pane */
    //public JEditorPane infoPane;
    /** The HTML Display pane's URL stack */
    //java.util.Stack stack;
    /** The HTML Display's back button */
    //JButton backButton;
    /** The Box that holds the back button */
    //Box backButtonBox;
    /** The HTML display's container panel */
    JComponent infoPanel;
    /** The current time */
    JLabel time;
    /** The frame rate */
    JSlider slider;
    /** The associated text with the speed of play slider */
    JLabel sliderText;
    /** The slider which controls the number of steps per press of the step-button */
    JSlider stepSlider;
    /** The associated text for number of steps per press of the step-button */
    JLabel stepSliderText;
    /** The slider which controls the thread priority of the underlying model thread */
    JSlider prioritySlider;
    /** The associiated text for the thread priority of the underlying model thread */
    JLabel prioritySliderText;
    /** The checkbox which states whether or not we should give way just a little bit */
    JCheckBox repeatButton;
    //    /** The checkbox which states whether or not we should give way just a little bit */
    //    JCheckBox yield;
    /** The stop button */
    JButton stopButton;
    /** The play button */
    JButton playButton;
    /** The pause button */
    JButton pauseButton;
    /** The top-level tabbed view */
    JTabbedPane tabPane;
    /** The list of frames shown in the "Displays" tab */
    JList frameListDisplay;
    /** The actual list of frames used in frameListDisplay */
    Vector frameList;
    /** Where the user can enter in a step count to stop at */
    PropertyField endField;
    /** Where the user can enter in a step count to pause at */
    PropertyField pauseField;
    /** Where the user can enter in a time to stop at */
    PropertyField timeEndField;
    /** Where the user can enter in a time to pause at */
    PropertyField timePauseField;
    /** Where the user can enter a new random number seed */
    PropertyField randomField;
    /** The Console's menu bar */
    JMenuBar menuBar;
    /** The split pane shown under the "Inspectors" tab, holding the list of 
        inspectors at top, and specific inspectors at bottom */
    JSplitPane innerInspectorPanel;
    /** An outer panel which holds the innerInspectorPanel, plus associated buttons */
    JPanel inspectorPanel;
    /** The checkbox for whether or not the random seed should be incremented each play-button press */
    JCheckBox incrementSeedOnPlay;
    /** The list of inspectors at the top of the split pane */
    JList inspectorList;
    /** Holds the inspectors shown at the bottom of the split pane (if any) */
    JPanel inspectorSwitcher;
    /** The card layout which enables inspectorSwitcher to show various inspectors */
    CardLayout inspectorCardLayout;
    /** The button for detatching inspectors */
    JButton detatchButton;
    /** The button for emptying the inspector list */
    JButton removeButton;
    /** The global model inspector, if any */
    Inspector modelInspector;
    /** The JScrollPane which holds the global model inspector, if any */
    JScrollPane modelInspectorScrollPane;
    /** The box which holds the play/stop/pause buttons, and the time and rate fields. */
    Box buttonBox;
    /** The combo box which specifies what's displayed in the time field */
    JComboBox timeBox;

    /** Random number generator seed */
    int randomSeed = (int) System.currentTimeMillis();

    /** how many steps we should take on one press of the "step" button.  As this is only relevant
        when there is NO underlying play thread (stepping happens inside the event loop, with the
        play thread killed), it can be safely set, but only do so from the event loop. */
    int numStepsPerStepButtonPress = 1;
    
    
    /////////////////////// CONSTRUCTORS
    /////////////////////// This is the single most elaborate piece of code in Console.java, but it's
    /////////////////////// mostly just boring stick-a-button-here stick-a-text-field-there stuff


    /** Creates a Console, using the default initial start behavior (INITIAL_BEHAVIOR_START).
        Sets the simulation's controller to point to this Console.    */
    public Console(final GUIState simulation)
        {
        super(GUIState.getName(simulation.getClass()));

        final Color transparentBackground = new JPanel().getBackground();  // sacrificial JPanel


        this.simulation = simulation;

        rateFormat = NumberFormat.getInstance();
        rateFormat.setMaximumFractionDigits(3);
        rateFormat.setMinimumIntegerDigits(1);


        /////// Make the lower buttons, steps, and frame rate

        buttonBox = new Box(BoxLayout.X_AXIS);

        // Create play button

        playButton = new JButton(I_PLAY_OFF);
        playButton.setPressedIcon(I_PLAY_ON);
        playButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                pressPlay();
                }
            });
        playButton.setBorderPainted(false);
        playButton.setContentAreaFilled(false);
        playButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        //playButton.setBackground(transparentBackground);  // looks better in Windows
        //playButton.setToolTipText("<html><i>When Stopped:</i> Start Simulation<br><i>When Paused:</i> Step Simulation</html>");
        buttonBox.add(playButton);

        // create pause button

        pauseButton = new JButton(I_PAUSE_OFF);
        pauseButton.setPressedIcon(I_PAUSE_ON);
        pauseButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                pressPause();
                }
            });
        pauseButton.setBorderPainted(false);
        pauseButton.setContentAreaFilled(false);
        pauseButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        //pauseButton.setToolTipText("<html><i>When Playing:</i> Pause/Resume Simulation<br><i>When Stopped:</i> Start Simulation Paused</html>");
        //pauseButton.setBackground(transparentBackground);  // looks better in Windows
        buttonBox.add(pauseButton);

        // create stop button
        stopButton = new JButton(I_STOP_OFF);
        stopButton.setIcon(I_STOP_ON);
        stopButton.setPressedIcon(I_STOP_OFF);
        
        stopButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                pressStop();
                }
            });
        stopButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        //stopButton.setBackground(transparentBackground);  // looks better in Windows
        //stopButton.setToolTipText("End Simulation");
        stopButton.setBorderPainted(false);
        stopButton.setContentAreaFilled(false);
        buttonBox.add(stopButton);

        timeBox = new JComboBox(new Object[] { "Time", "Steps", "Rate", "None" });
        timeBox.setSelectedIndex(0);
        timeBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                synchronized(time)
                    {
                    showing = timeBox.getSelectedIndex();
                    }
                updateTime();
                }
            });
        
        // "float" it vertically so it looks nice in Windows  -- or does this work?
        Box timeBox1 = new Box(BoxLayout.Y_AXIS);
        timeBox1.add(Box.createGlue());
        timeBox1.add(timeBox);
        timeBox1.add(Box.createGlue());

        // create Time fields
        time = new JLabel("");
        // need space for 9218868437227405312
        time.setPreferredSize(new JLabel("8888888888888888888").getPreferredSize()); // ensure enough space
        //time.setMaximumSize(new JLabel("8888888888888888888").getMaximumSize()); // don't go over this
        time.setMinimumSize(new JLabel("888").getMinimumSize()); // ensure enough space for most cases
        buttonBox.add(time);
        buttonBox.add(Box.createGlue());
        buttonBox.add(new JLabel(" "));
        buttonBox.add(timeBox1);
        if (Display2D.isMacOSX()) buttonBox.add(new JLabel("    "));  // move away from the scroll box
        



        //////// create the "about" tab pane
        
        infoPanel = new HTMLBrowser(GUIState.getInfo(simulation.getClass()));

        //////// create the "Displays" tab pane

        frameList = new Vector();
        frameListDisplay = new JList(frameList);
        frameListDisplay.setCellRenderer(new ListCellRenderer()
            {
            // this ListCellRenderer will show the frame titles in black if they're
            // visible, and show them as gray if they're hidden.  You can add frames
            // to this list by calling the registerFrame() method.
            protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
                {
                JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                JFrame frame = (JFrame) value;
                if (frame.isVisible())
                    renderer.setForeground(Color.black);
                else
                    renderer.setForeground(Color.gray);
                renderer.setText(frame.getTitle());
                return renderer;
                }
            });

        // put the FrameList, and the show/hide buttons, all together into one panel
        Box b = new Box(BoxLayout.X_AXIS);
        JButton button = new JButton("Show All");
        button.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                pressShowAll();
                }
            });
        b.add(button);
        button = new JButton("Show");
        button.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                pressShow();
                }
            });
        b.add(button);
        button = new JButton("Hide");
        button.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                pressHide();
                }
            });
        b.add(button);
        button = new JButton("Hide All");
        button.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                pressHideAll();
                }
            });
        b.add(button);
        b.add(Box.createGlue());
        JPanel frameListPanel = new JPanel();
        frameListPanel.setLayout(new BorderLayout());
        frameListPanel.add(new JScrollPane(frameListDisplay), BorderLayout.CENTER);
        frameListPanel.add(b, BorderLayout.SOUTH);







        //////// Create the "Console" tab panel  
        
        // our panel will be laid out as labels on the left and widgets on the right,
        // thus we use a pretty Labelled List.
        LabelledList controlPanel = new LabelledList()
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };


        // create speed slider
        // Slider:  0   1     2    3   4  5  6 7  8  9
        // speed:   0   1/4  1/2   1   2  4  8 16 32 64 (speed is in seconds per tick, higher is slower)
        slider = new JSlider(0, 100, 0); // ranges from 0 to 100
        slider.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e)
                {
                int val = slider.getValue();
                long speed = (long)( 64000.0 / (Math.pow(4,5)-1) * ( Math.pow(4,val/20.0) - 1 ) );
                if (!slider.getValueIsAdjusting())
                    setPlaySleep(speed); // convert to milliseconds
                sliderText.setText("" + ((double) (speed)) / 1000);
                }
            });
        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        b.add(slider);
        sliderText = new JLabel("0.0");
        sliderText.setMinimumSize(new JLabel("88.888").getMinimumSize()); // ensure enough space
        sliderText.setPreferredSize(new JLabel("88.888").getPreferredSize()); // ensure enough space
        b.add(sliderText);
        controlPanel.addLabelled("Delay (Sec/Step) ", b);

        // create speed slider
        prioritySlider = new JSlider(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, Thread.NORM_PRIORITY); // ranges from 0 to 100
        prioritySlider.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e)
                {
                int val = prioritySlider.getValue();
                if (!prioritySlider.getValueIsAdjusting())
                    { setThreadPriority(val); } 
                prioritySliderText.setText("" + val + (val==Thread.NORM_PRIORITY ? ": norm" : ""));
                }
            });
        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        b.add(prioritySlider);
        prioritySliderText = new JLabel(Thread.NORM_PRIORITY + ": norm");
        prioritySliderText.setMinimumSize(new JLabel("88: norm").getMinimumSize()); // ensure enough space
        prioritySliderText.setPreferredSize(new JLabel("88: norm").getPreferredSize()); // ensure enough space
        b.add(prioritySliderText);
        controlPanel.addLabelled("Thread Priority ", b);


        // Create the step slider
        // the equation is: step = slider
        stepSlider = new JSlider(1, 20, 1); // ranges from 1 to 20
        stepSlider.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e)
                {
                numStepsPerStepButtonPress = stepSlider.getValue();
                stepSliderText.setText("" + numStepsPerStepButtonPress);
                }
            });
        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        b.add(stepSlider);
        stepSliderText = new JLabel("1");
        stepSliderText.setMinimumSize(new JLabel("8.888").getMinimumSize()); // ensure enough space
        stepSliderText.setPreferredSize(new JLabel("8.888").getPreferredSize()); // ensure enough space
        b.add(stepSliderText);
        controlPanel.addLabelled("Steps per Step-Button ", b);


        // Create the 'Automatically Stop at:' text field
        endField = new PropertyField("")
            {
            public String newValue(String value)
                {
                long l = -1;
                try
                    {
                    l = Long.parseLong(value);
                    if (l < 0 )
                        l = Long.MAX_VALUE;
                    }
                catch (NumberFormatException num) // bad data -- assume MAX_VALUE
                    {
                    l = Long.MAX_VALUE;
                    }
                setWhenShouldEnd(l);
                if (l == Long.MAX_VALUE)
                    return "";
                else
                    return ""+l;
                }
            };
        // need space for 9218868437227405312
        endField.valField.setColumns(19);  // make enough space
        endField.setMaximumSize(endField.valField.getPreferredSize());
        endField.setPreferredSize(endField.valField.getPreferredSize());

        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        b.add(endField);
        controlPanel.addLabelled("Automatically Stop at Step ", b);
        
        timeEndField = new PropertyField("")
            {
            public String newValue(String value)
                {
                double l = Schedule.BEFORE_SIMULATION;
                try
                    {
                    l = Double.parseDouble(value);
                    if (l < Schedule.EPOCH || (l!=l) /* NaN */)
                        l = Schedule.AFTER_SIMULATION;
                    }
                catch (NumberFormatException num) // bad data -- assume AFTER_SIMULATION
                    {
                    l = Schedule.AFTER_SIMULATION;
                    }
                setWhenShouldEndTime(l);
                if (l == Schedule.AFTER_SIMULATION)
                    return "";
                else
                    return ""+l;
                }
            };
        // need space for 9218868437227405312
        timeEndField.valField.setColumns(19);  // make enough space
        timeEndField.setMaximumSize(endField.valField.getPreferredSize());
        timeEndField.setPreferredSize(endField.valField.getPreferredSize());

        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        b.add(timeEndField);
        controlPanel.addLabelled("Automatically Stop After Time ", b);


        // Create the Pause text field
        pauseField = new PropertyField("")
            {
            public String newValue(String value)
                {
                long l = Long.MAX_VALUE;
                try
                    {
                    l = Long.parseLong(value);
                    if (l < 0)
                        l = Long.MAX_VALUE;
                    }
                catch (NumberFormatException num) // bad data -- assume AFTER_SIMULATION
                    {
                    l = Long.MAX_VALUE;
                    }
                setWhenShouldPause(l);
                if (l == Long.MAX_VALUE)
                    return "";
                else
                    return ""+l;
                }
            };
        // need space for 9218868437227405312
        pauseField.valField.setColumns(19);  // make enough space
        pauseField.setMaximumSize(pauseField.valField.getPreferredSize());
        pauseField.setPreferredSize(pauseField.valField.getPreferredSize());

        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        b.add(pauseField);
        controlPanel.addLabelled("Automatically Pause at Step ", b);


        // Create the Pause text field
        timePauseField = new PropertyField("")
            {
            public String newValue(String value)
                {
                double l = Schedule.BEFORE_SIMULATION;
                try
                    {
                    l = Double.parseDouble(value);
                    if (l < Schedule.EPOCH || (l!=l) /* NaN */)
                        l = Schedule.AFTER_SIMULATION;
                    }
                catch (NumberFormatException num) // bad data -- assume AFTER_SIMULATION
                    {
                    l = Schedule.AFTER_SIMULATION;
                    }
                setWhenShouldPauseTime(l);
                if (l == Schedule.AFTER_SIMULATION)
                    return "";
                else
                    return ""+l;
                }
            };
        // need space for 9218868437227405312
        timePauseField.valField.setColumns(19);  // make enough space
        timePauseField.setMaximumSize(pauseField.valField.getPreferredSize());
        timePauseField.setPreferredSize(pauseField.valField.getPreferredSize());

        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        b.add(timePauseField);
        controlPanel.addLabelled("Automatically Pause After Time ", b);

        // Create the Random text field
        randomField = new PropertyField("")
            {
            public String newValue(String value)
                {
                try
                    {
                    int l = Integer.parseInt(value);
                    randomSeed = l;
                    setRandomNumberGenerator(randomSeed);
                    return "" + l;
                    } 
                catch (NumberFormatException num)
                    { 
                    return getValue();
                    }
                }
            };
        randomField.valField.setColumns(10);  // make enough space
        randomField.setMaximumSize(randomField.valField.getPreferredSize());
        randomField.setPreferredSize(randomField.valField.getPreferredSize());
        randomField.setValue("" + randomSeed);  // so the user can see the initial seed value

        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        b.add(randomField);
        controlPanel.addLabelled("Random Number Seed ", b);

        
        // Create the Increment Seed on Play button
        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        incrementSeedOnPlay = new JCheckBox();
        incrementSeedOnPlay.setSelected(true);
        b.add(incrementSeedOnPlay);
        controlPanel.addLabelled("Increment Seed on Stop ", b);

        
        // Create the repeatButton checkbox
        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(2, 4, 2, 4);  // Java jams the widgets too closely for my taste
            public Insets getInsets()
                {
                return insets;
                }
            };
        repeatButton = new JCheckBox();
        repeatButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                setShouldRepeat(repeatButton.isSelected());
                }
            });

        repeatButton.setSelected(false);
        b.add(repeatButton);
        controlPanel.addLabelled("Repeat Play on Stop ", b);
        





        //////// Create the "Inspectors" tab panel  
        
        // Make the "Empty List" button at bottom
        JPanel lowerPane = new JPanel();
        lowerPane.setLayout(new BorderLayout());
        removeButton = new JButton("Empty List");
        removeButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                removeAllInspectors(false);
                }
            });
        removeButton.setEnabled(false);
        detatchButton = new JButton("Detatch");
        detatchButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                detatchInspector();
                }
            });
        detatchButton.setEnabled(false);
        Box removeButtonBox = new Box(BoxLayout.X_AXIS);
        removeButtonBox.add(removeButton);
        removeButtonBox.add(detatchButton);
        removeButtonBox.add(Box.createGlue());

        // Make the inspector list at top
        inspectorList = new JList(inspectorNames);
        inspectorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inspectorList.addListSelectionListener( new ListSelectionListener()
            {
            public void valueChanged(ListSelectionEvent e)
                {
                if (!e.getValueIsAdjusting() &&
                    inspectorList.getSelectedIndex() != -1)
                    {
                    inspectorCardLayout.show(inspectorSwitcher,""+inspectorList.getSelectedIndex());
                    }
                }
            });
        JScrollPane listPane = new JScrollPane(inspectorList);
        listPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        // Make the inspector switcher at bottom.  This will show various inspectors, so it uses a CardLayout 
        inspectorSwitcher = new JPanel();
        inspectorSwitcher.setLayout(inspectorCardLayout = new CardLayout());
            
        // Make split pane and the panel which holds the split pane and the button 
        innerInspectorPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
            listPane, inspectorSwitcher);
        innerInspectorPanel.setDividerLocation(60);  // enough space for about 3 rows at top (roughly 2/3)
        inspectorPanel = new JPanel();
        inspectorPanel.setLayout(new BorderLayout());
        inspectorPanel.add(innerInspectorPanel, BorderLayout.CENTER);
        inspectorPanel.add(removeButtonBox, BorderLayout.SOUTH);
        
        
        
        
        //////// Stick everything in the Tabbed Pane  
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonBox, BorderLayout.SOUTH);
        tabPane = new JTabbedPane()
            {
            public Dimension getMinimumSize()
                {
                // allow total vertical closure 
                return new Dimension(super.getMinimumSize().width,0);
                }
            };
        tabPane.addTab("About", infoPanel);
        
        // add the control panel such that it doesn't have a horizontal scroller
        AbstractScrollable consoleScrollable = new AbstractScrollable()
            {
            public boolean getScrollableTracksViewportWidth() { return true; }
            };
        consoleScrollable.setLayout(new BorderLayout());
        consoleScrollable.add(controlPanel, BorderLayout.CENTER);
        
        JScrollPane controlScroll = new JScrollPane(consoleScrollable)
            {
            Insets insets = new Insets(0,0,0,0);  // MacOS X adds a border
            public Insets getInsets()
                {
                return insets;
                }
            };
        controlScroll.getViewport().setBackground(transparentBackground);//UIManager.getColor("window"));  // make nice stripes on MacOS X
            
        tabPane.addTab("Console", controlScroll);
        tabPane.addTab("Displays", frameListPanel);
        tabPane.addTab("Inspectors", inspectorPanel);
        // add an optional pane if the GUIState has an inspector
        buildModelInspector();
        
        // set up tab pane
        getContentPane().add(tabPane, BorderLayout.CENTER);





        //////// Create the Menu Bar  
        
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // add the File menu

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        buildClassList(); // load the simulation class list in case it's not been loaded yet, to determine if we want to have simulations

        JMenuItem _new = new JMenuItem("New Simulation...");
        if (!allowOtherClassNames && classNames.size() == 0)  // nothing permitted
            _new.setEnabled(false);
        _new.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doNew();
                }
            });
        fileMenu.add(_new);
        JMenuItem open = new JMenuItem("Open...");
        if (SimApplet.isApplet) open.setEnabled(false);
        open.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doOpen();
                }
            });
        fileMenu.add(open);
        JMenuItem save = new JMenuItem("Save");
        if (SimApplet.isApplet) save.setEnabled(false);
        save.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doChangeCode(new Runnable()
                    {
                    public void run()
                        {
                        doSave();
                        }
                    });
                }
            });
        fileMenu.add(save);
        JMenuItem saveAs = new JMenuItem("Save As...");
        if (SimApplet.isApplet) saveAs.setEnabled(false);
        saveAs.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doChangeCode(new Runnable()
                    {
                    public void run()
                        {
                        doSaveAs();
                        }
                    });
                }
            });
        fileMenu.add(saveAs);

        JMenuItem _about = new JMenuItem("About MASON");
        _about.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doAbout();
                }
            });
        fileMenu.add(_about);

        JMenuItem quit = new JMenuItem("Quit");
        quit.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doQuit();
                }
            });
        fileMenu.add(quit);


        // Bug in MacOS X Java 1.3.1 requires that we force a repaint, dorky.
        addComponentListener(new ComponentAdapter()
            {
            public void componentResized(ComponentEvent e)
                {
                Utilities.doEnsuredRepaint(getContentPane());
                Utilities.doEnsuredRepaint(menuBar);
                }
            });

        // make ourselves good citizens
        addWindowListener(new WindowAdapter()
            {
            public void windowClosing(WindowEvent e)
                {
                doClose();
                }
            });
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);  // we'll handle it above

        //////// Set up the window.
        
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);  // my default height
        Point defLoc = getLocation();
        setLocation(-10000,-10000);  // my default "user didn't move me" location
        setResizable(true);
        repaint();

        //////// Prepare the simulation
        
        // add me to the console list
        allConsoles.put(this,this);
        numConsoles++;
        
        // Fire up the simulation displays
        simulation.init(this);


        // Set the location of the console if it hasn't already
        // been set by the user
        Point loc = getLocation();
        if (loc.x == -10000 && loc.y == -10000)  // user didn't set me I think
            {
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            // If there is room, put us to the far right of all the displays
            // which have been attached so far.
            Rectangle bounds = new Rectangle(0,0,0,0);
            Iterator i = frameList.iterator();
            while(i.hasNext())
                bounds = bounds.union(((Component)i.next()).getBounds());
            if (bounds.width + getWidth() + DEFAULT_GUTTER <= screen.width)  // enough space on right, though MacOS X will be a problem
                setLocation(bounds.width + DEFAULT_GUTTER,defLoc.y);
            else setLocation(defLoc);
            }
        }

    /** Throws out the old model inspector, if any, and creates a new model inspector, if any. */
    void buildModelInspector()
        {
        // remove existing tab if it's there
        if (modelInspectorScrollPane!=null) 
            tabPane.remove(modelInspectorScrollPane);
        modelInspector = simulation.getInspector();
        if (modelInspector!=null)
            {
            String name = modelInspector.getName();
            if (name==null || name.length() == 0) name = "Model";
            modelInspectorScrollPane = new JScrollPane(modelInspector)
                {
                Insets insets = new Insets(0,0,0,0);  // MacOS X adds a border
                public Insets getInsets()
                    {
                    return insets;
                    }
                };
            modelInspectorScrollPane.getViewport().
                setBackground(new JPanel().getBackground()); // UIManager.getColor("window"));  // make nice stripes on MacOS X
            tabPane.addTab(name,modelInspectorScrollPane);
            }
        tabPane.revalidate();
        }



    /////////////////////// SMALL SYNCHRONIZED METHODS FOR MANIPULATING THE UNDERLYING PLAY STATE VARIABLES
    /////////////////////// (many of these are private, and nearly all are synchronized to communicate state information
    /////////////////////// to the underlying thread)


    /** Should the simulation repeat when the stop button is pressed? */
    boolean shouldRepeat = false;
    
    /** Set whether or not the simualtion should repeat when the stop button is pressed. */
    public void setShouldRepeat(boolean val)
        {
        synchronized (playThreadLock)
            {
            shouldRepeat = val;
            }
        }
    
    /** Get whether or not the simualtion should repeat when the stop button is pressed. */
    public boolean getShouldRepeat()
        {
        synchronized (playThreadLock)
            {
            return shouldRepeat;
            }
        }

    /** What should the simulation thread priority be?  Don't play with this. */
    int threadPriority = Thread.NORM_PRIORITY;
    
    /** Set when the simulation should end. */
    public void setThreadPriority(int val)
        {
        synchronized (playThreadLock)
            {
            threadPriority = val;
            if (playThread != null)
                playThread.setPriority(threadPriority);
            }
        }
    
    /** Get when the simulation should end.*/
    public int getThreadPriority()
        {
        synchronized (playThreadLock)
            {
            return threadPriority;
            }
        }

    /** When should the simulation end?  Don't play with this. */
    long whenShouldEnd = Long.MAX_VALUE;
    
    /** Set when the simulation should end. */
    public void setWhenShouldEnd(long val)
        {
        synchronized (playThreadLock)
            {
            whenShouldEnd = val;
            }
        }
    
    /** Get when the simulation should end.*/
    public long getWhenShouldEnd()
        {
        synchronized (playThreadLock)
            {
            return whenShouldEnd;
            }
        }

    /** When should the simulation pause?  Don't play with this. */
    long whenShouldPause = Long.MAX_VALUE;
    
    /** Sets when the simulation should pause. */
    public void setWhenShouldPause(long val)
        {
        synchronized (playThreadLock)
            {
            whenShouldPause = val;
            }
        }

    /** Get when the simulation should pause. */
    public long getWhenShouldPause()
        {
        synchronized (playThreadLock)
            {
            return whenShouldPause;
            }
        }

    /** When should the simulation end?  Don't play with this. */
    double whenShouldEndTime = Schedule.AFTER_SIMULATION;
    
    /** Set when the simulation should end. */
    public void setWhenShouldEndTime(double val)
        {
        synchronized (playThreadLock)
            {
            whenShouldEndTime = val;
            }
        }
    
    /** Get when the simulation should end.*/
    public double getWhenShouldEndTime()
        {
        synchronized (playThreadLock)
            {
            return whenShouldEndTime;
            }
        }

    /** When should the simulation pause?  Don't play with this. */
    double whenShouldPauseTime = Schedule.AFTER_SIMULATION;
    
    /** Sets when the simulation should pause. */
    public void setWhenShouldPauseTime(double val)
        {
        synchronized (playThreadLock)
            {
            whenShouldPauseTime = val;
            }
        }

    /** Get when the simulation should pause. */
    public double getWhenShouldPauseTime()
        {
        synchronized (playThreadLock)
            {
            return whenShouldPauseTime;
            }
        }


    /** Milliseconds of how long we should sleep between each step. Don't play with this. */
    long playSleep = 0;
    
    /** Sets (in milliseconds) how long we should sleep between each step in the play thread. 
        This method is run as a doChangeCode so it can interrupt the possibly sleeping
        thread and give it a new interval. */
    public void setPlaySleep(final long sleep)
        {
        doChangeCode(new Runnable()
            {
            public void run()
                {
                synchronized (playThreadLock)
                    {
                    playSleep = sleep;
                    }
                }
            });
        }

    /** Gets how long we should sleep between each step in the play thread (in milliseconds). */
    public long getPlaySleep()
        {
        synchronized (playThreadLock)
            {
            return playSleep;
            }
        }

    /** The thread that actually goes through the steps */
    Thread playThread;
    
    /** A general lock used by a number of short methods which need to "synchronize on the play thread"
        even if it's changing to another thread.  To do this, we use this official 'play thread lock' */
    final Object playThreadLock = new Object();
    
    /** Whether the thread should stop.  Don't play with this. */
    boolean threadShouldStop = false;
    
    /** Returns whether or not a flag has been raised to ask the underlying play thread to stop.  */
    boolean getThreadShouldStop()
        {
        synchronized (playThreadLock)
            {
            return threadShouldStop;
            }
        }
        
    /** Sets or clears the flag indicating whether or not the underlying play thread should stop. */
    void setThreadShouldStop(final boolean stop)
        {
        synchronized (playThreadLock)
            {
            threadShouldStop = stop;
            }
        }

    /** The play thread is presently stopped. */
    public static final int PS_STOPPED = 0;
    
    /** The play thread is presently playing. */
    public static final int PS_PLAYING = 1;
    
    /** The play thread is presently paused. */
    public static final int PS_PAUSED = 2;

    /** The current state of the simulation: playing, stopped, or paused.  Don't play with this.*/
    int playState = PS_STOPPED;

    /** Sets whether or not the current thread is playing, stopped, or paused.  An internal method only. */
    void setPlayState(int state)
        {
        synchronized (playThreadLock)
            {
            playState = state;
            }
        }

    /** Gets whether or not the current thread is PS_PLAYING, PS_STOPPED, or PS_PAUSED. */
    public int getPlayState()
        {
        synchronized (playThreadLock)
            {
            return playState;
            }
        }

    
    /** Starts the simulation.  Called internally by methods when a simulation is fired up.
        Basically various custodial methods.
        Removes the existing inspectors, sets the random number generator, calls start()
        on the GUIState (and thus the model underneath), and sets up the global model
        inspector. */
    void startSimulation()
        {
        removeAllInspectors(true);      // clear inspectors
        setRandomNumberGenerator(randomSeed);
        simulation.start();
        updateTime(simulation.state.schedule.getSteps(), simulation.state.schedule.time(), -1.0);
        //setTime(simulation.state.schedule.time());
        //setStepsPerSecond(-1.0);  // negative value, guarantees nothing is shown
        
        // no need to clear out the global inspector.  It stays.
        
        // update the model (global) inspector if any
        if (modelInspector != null)
            {
            Steppable stepper = new Steppable()
                {
                public void step(final SimState state)
                    {
                    SwingUtilities.invokeLater(new Runnable()
                        {
                        public void run()
                            {
                            synchronized(state.schedule)
                                {
                                // this is called while we have a lock on state.schedule,
                                // so we have control over the model.
                                if (modelInspector.isVolatile())
                                    {
                                    modelInspector.updateInspector();
                                    modelInspector.repaint();
                                    }
                                }
                            }
                        });
                    }
                };
            if (modelInspector.isVolatile())  // should we update the inspector each time -- expensive
                simulation.scheduleImmediateRepeat(true, stepper);
            // stepper.step(simulation.state);  // update the model inspector one time at the beginning at any rate
            }
        }







    /////////////////////// UTILITY FUNCTIONS


    /** Simulations can call this to get access to the tabPane -- 
        to add tabbed panes as they like.  */
    public synchronized JTabbedPane getTabPane()
        {
        return tabPane;
        }
    
    /** Sets the random number generator of the underlying model, pausing it first, then unpausing it after. 
        Updates the randomField. */ 
    void setRandomNumberGenerator(final int val)
        {
        doChangeCode(new Runnable()
            {
            public void run()
                {
                simulation.state.setRandom(new MersenneTwisterFast(val));
                }
            });
          
        // The following invokeLater wrapper is commented out because we've discovered that
        // it causes a hang bug.  We're not exactly
        // sure why, but setRandomNumberGenerator() is called from the Console's constructor,
        // and this is before Console is shown on-screen.  We think that for some reason the
        // invokeLater freaks out the text field perhaps when the Console is being put on-screen
        // with a setVisible(true); clearly some underlying Java conflict.  Anyway, without
        // the wrapper it seems to work fine, though I'm still concerned about the possibility
        // that setText() would be called from this method, which is in turn called from methods
        // like pressStop(), which can be called by underlying threads.  If we see any further
        // hangs as a result, I will revisit the issue.  -- Sean
  
        /*      
                SwingUtilities.invokeLater(new Runnable()   // just in case, to avoid possible deadlock, though I've not seen it
                {
                public void run()
                { 
        */
        randomField.setValue("" + val);
        /*
          }
          }); 
        */
        }





    /////////////////////// MENU FUNCTIONS
    /////////////////////// You probably shouldn't call these methods except from within the event loop

    /** Private internal flag which indicates if the program is already in the process of quitting. */    
    static boolean isQuitting = false;
    /** Private lock used by doQuit() to avoid synchronizing on Console. */
    final static Object isQuittingLock = new Object();
    
    /** Quits the program.  Called by the Quit menu option. */
    public void doQuit()
        {
        synchronized(isQuittingLock)  // quitting causes closing, which in turn causes quitting...
            {
            if (isQuitting) return;  // already in progress...
            else isQuitting = true;
        
            // close all consoles.  We dump into an array because they'll try to remove themselves
            Object[] entries = allConsoles.entrySet().toArray();
            for(int x=0;x<entries.length;x++)
                if (entries[x] != null)  // might occur if weak?  dunno
                    ((Console)(((Map.Entry)(entries[x])).getKey())).doClose();

            if(!(SimApplet.isApplet))
                try { System.exit(0); } catch (Exception e) { }
            isQuitting = false; // obviously if we've sucessfully exited this won't happen
            }
        }
            
    // This stuff allows us to fire up multiple consoles (multiple simulations) in the same process,
    // but when we close one of them, it doesn't quit everything. 
    
    /** A weak container for all current consoles. */
    static WeakHashMap allConsoles = new WeakHashMap();
    /** A reference count of open (unclosed) Consoles.  When this reaches 0, the program ends. */
    static int numConsoles;
    /** Private internal flag which indicates if the program is already in the process of quitting. */    
    boolean isClosing = false;
    /** Private lock used by doClose() to avoid synchronizing on Console. */
    final Object isClosingLock = new Object();

    /** Closes the Console and shuts down the simulation.  Quits the program only if other simulations
        are not running in the same program.  Called when the user clicks on the close button of the Console,
        or during a program-wide doQuit() process.  Can also be called programmatically. */
    public void doClose()
        {
        synchronized(isClosingLock)  // closing can cause quitting, which in turn can cause closing...
            {
            if (isClosing) return;  // already in progress...
            else isClosing = true;
            }
        pressStop();  // stop threads
        simulation.quit();  // clean up simulation
        dispose();
        allConsoles.remove(this);
        if (--numConsoles <= 0)  // try to quit if we're all gone
            doQuit();
        }

    static boolean sacrificial;
    /** Pops up a window allowing the user to enter in a class name to start a new simulation. */
    public static void main(String[] args)
        {
        // this line is to fix a stupidity in MacOS X 1.3.1, where if Display2D isn't loaded before
        // windows are created (so its static { } can be executed before the graphics subsystem
        // fires up) the underlying graphics subsystem is messed up.  Apple's fixed this in 1.4.1.
        sacrificial = Display2D.isMacOSX();  // sacrificial  -- something to force Display2D.class to load
                
        // Okay here we go with the real code.
        if (!doNew(null, true) && !SimApplet.isApplet) System.exit(0); // just a dummy JFrame
        }
    
    /** Pops up the about box */
    static JFrame aboutFrame = null;
    public void doAbout()
        {
        if (aboutFrame == null)
            {
            // construct the frame
            
            aboutFrame = new JFrame("About MASON");
            JPanel p = new JPanel();  // 1.3.1 only has borders for JComponents, not Boxes
            p.setBorder(BorderFactory.createEmptyBorder(25,30,30,30));
            Box b = new Box(BoxLayout.Y_AXIS);
            p.add(b,BorderLayout.CENTER);
            aboutFrame.getContentPane().add(p,BorderLayout.CENTER);
            aboutFrame.setResizable(false);
            Font small = new Font("Dialog",0,9);

            // start dumping in text
            JLabel j = new JLabel("MASON");
            j.setFont(new Font("Serif",0,36));
            b.add(j);
                    
            java.text.NumberFormat n = java.text.NumberFormat.getInstance();
            n.setMinimumFractionDigits(0);
            j = new JLabel("Version " + n.format(SimState.version()));
            b.add(j);
            JLabel spacer = new JLabel(" ");
            spacer.setFont(new Font("Dialog",0,6));
            b.add(spacer);

            j = new JLabel("Co-created by George Mason University's");
            b.add(j);
            j = new JLabel("Evolutionary Computation Laboratory and");
            b.add(j);
            j = new JLabel("Center for Social Complexity");
            b.add(j);

            spacer = new JLabel(" ");
            spacer.setFont(new Font("Dialog",0,6));
            b.add(spacer);
            
            j = new JLabel("http://cs.gmu.edu/~eclab/projects/mason/");
            b.add(j);

            spacer = new JLabel(" ");
            spacer.setFont(new Font("Dialog",0,6));
            b.add(spacer);

            j = new JLabel("Major contributors include Sean Luke,");
            b.add(j);
            j = new JLabel("Gabriel Catalin Balan, Liviu Panait,");
            b.add(j);
            j = new JLabel("Claudio Cioffi-Revilla, Sean Paus,");
            b.add(j);
            j = new JLabel("Keith Sullivan, and Daniel Kuebrich.");
            b.add(j);
                
            spacer = new JLabel(" ");
            spacer.setFont(new Font("Dialog",0,6));
            b.add(spacer);
                        
            j = new JLabel("MASON is (c) 2005 Sean Luke and George Mason University,");
            j.setFont(small);
            b.add(j);

            j = new JLabel("with various elements copyrighted by the above contributors.");
            j.setFont(small);
            b.add(j);

            j = new JLabel("PNGEncoder is (c) 2000 J. David Eisenberg.  MovieEncoder,", JLabel.LEFT);
            j.setFont(small);
            b.add(j);
            
            j = new JLabel("SelectionBehavior, and WireFrameBoxPortrayal3D are partly", JLabel.LEFT);
            j.setFont(small);
            b.add(j);
            
            j = new JLabel("(c) 1996 Sun Microsystems.  MersenneTwisterFast is partly", JLabel.LEFT);
            j.setFont(small);
            b.add(j);

            j = new JLabel("(c) 1993 Michael Lecuyer.  CapturingCanvas3D is based in", JLabel.LEFT);
            j.setFont(small);
            b.add(j);
        
            j = new JLabel("part on code by Peter Kunszt.", JLabel.LEFT);
            j.setFont(small);
            b.add(j);
            aboutFrame.pack();
            }
            
        // if not on screen right now, move to center of screen
        if (!aboutFrame.isVisible())
            {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            d.width -= aboutFrame.getWidth();
            d.height -= aboutFrame.getHeight();
            d.width /= 2;
            d.height /= 2;
            if (d.width < 0) d.width = 0;
            if (d.height < 0) d.height = 0;
            aboutFrame.setLocation(d.width,d.height);
            }
        
        // show it!
        aboutFrame.setVisible(true);
        }

    /** Pops up a window allowing the user to enter in a class name to start a new simulation. */
    public void doNew()
        {
        doNew(this, false);
        }
        
    /** Returns the index of the option selected, or -1 if the user pressed the
        close button on the window instead -- AWT is stupid. */
    static int showOptionDialog(JFrame originalFrame, JComponent component,
        String title, Object[] options, boolean resizable)
        {
        JOptionPane p = new JOptionPane(component, 
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION,
            null, options, options[0]);
        JDialog d = p.createDialog(originalFrame, title);
        d.pack();
        d.setResizable(resizable);
        p.selectInitialValue();
        d.setVisible(true);;
        for(int counter = 0; counter < options.length; counter++)
            if(options[counter].equals(p.getValue()))
                return counter;
        return -1;
        }
        
    static Object classLock = new Object();
    static boolean classListLoaded = false;
    static void buildClassList()
        {
        // just in case someone crazy tries to load twice
        synchronized(classLock) { if (classListLoaded) return; else classListLoaded = true; }
                
        ///////// Build doNew() comboBox
        allowOtherClassNames = true;
        try
            {
            InputStream s = Console.class.getResourceAsStream("simulation.classes");
            StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(s)));
            st.resetSyntax();
            st.wordChars(32,255);
            st.whitespaceChars(0,31);  // everything but space
            st.commentChar(35);
            boolean errout = false;
            String nextName = null;
            //st.whitespaceChars(0,32);  // control chars
            while(st.nextToken()!=StreamTokenizer.TT_EOF)
                {
                if (st.sval == null) { } // ignore
                else if ("ONLY".equalsIgnoreCase(st.sval))
                    allowOtherClassNames = false;
                else if (st.sval.toUpperCase().startsWith("NAME:"))
                    {
                    //if (shortNames.size() == 0) throw new Exception("The 'NAME:' tag occurred before any class name was declared");
                    //shortNames.set(shortNames.size()-1, st.sval.substring(5).trim());
                    nextName = st.sval.substring(5).trim();
                    }
                else 
                    {
                    String shortName = null;
                    if (nextName==null)
                        {
                        try
                            {
                            Class c = Class.forName(st.sval);
                            try
                                { shortName = GUIState.getName(c); }
                            catch (Throwable e)
                                { shortName = GUIState.getTruncatedName(c); }
                            }
                        catch (Throwable e) 
                            {
                            if (!errout) 
                                System.err.println("Not all classes loaded, due to error: probably no Java3D");
                            errout = true;
                            }
                        }
                    else { shortName = nextName; nextName = null; }
                    // at this point if it's still null we shouldn't list it.
                    if (shortName!=null)
                        {
                        classNames.add(st.sval);
                        shortNames.add(shortName);
                        }
                    }
                }
            if (nextName != null) System.err.println("Spurious NAME tag at end of simulation.classes file:\n\tNAME: " + nextName);
            s.close();
            }
        catch (Exception e)
            {
            System.err.println("Couldn't load the simulation.classes file because of error. \nLikely the file does not exist or could not be opened.\nThe error was:\n");
            e.printStackTrace();
            }
        }

    /** Returns true if a new simulation has been created; false if the user cancelled. */
    static boolean doNew(JFrame originalFrame, boolean startingUp)
        {
        buildClassList();
                
        final String defaultText = "<html><body bgcolor='white'><font face='dialog'><br><br><br><br><p align='center'>Select a MASON simulation from the list at left,<br>or type a Java class name below.</p></font></body></html>";
        final String nothingSelectedText = "<html><body bgcolor='white'></body></html>";
                
        while(true)
            {
            final JList list = new JList(classNames);
            final JScrollPane pane = new JScrollPane(list);
                        
            list.setCellRenderer(new DefaultListCellRenderer()
                {
                public Component getListCellRendererComponent(
                    JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) 
                    {
                    JLabel label = (JLabel)(super.getListCellRendererComponent(
                            list,value,index,isSelected,cellHasFocus)); 
                    if (index >= 0)
                        {
                        label.setText("<html><body><font face='dialog'> " + shortNames.get(index) + 
                            "<font size='-2' color='#AAAAAA'><br> " + classNames.get(index) +
                            "</font></font></body></html>");
                        }
                    return label;
                    }
                });
                        
            final HTMLBrowser browser = new HTMLBrowser(defaultText)
                {
                public Dimension getPreferredSize() { return new Dimension(400, 400); }
                public Dimension getMinimumSize() { return new Dimension(10,10); }
                };
                        
            final JTextField field = new JTextField("sim.app.");
            JPanel fieldp = new JPanel();
            fieldp.setLayout(new BorderLayout());
            fieldp.add(field,BorderLayout.CENTER);
            fieldp.add(new JLabel("Simulation class name: "), BorderLayout.WEST);
            fieldp.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
                        
            list.addListSelectionListener(new ListSelectionListener()
                {
                public void valueChanged(ListSelectionEvent e)
                    {
                    if (!e.getValueIsAdjusting()) try
                                                      {
                                                      field.setText((String)list.getSelectedValue());
                                                      browser.setText(GUIState.getInfo(Class.forName(field.getText())));
                                                      }
                        catch (Throwable ex)
                            {
                            field.setText((String)list.getSelectedValue());
                            browser.setText(nothingSelectedText);
                            }
                    }
                });

            list.addMouseListener(new MouseAdapter() 
                {
                public void mouseClicked(MouseEvent e) 
                    {
                    if (e.getClickCount() == 2) 
                        {
                        // prematurely get frame and close it
                        Component c = list;
                        while(c.getParent() != null)
                            c = c.getParent();
                        ((Window)c).dispose();
                        }
                    }
                });

            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(browser, BorderLayout.CENTER);
            p.add(pane,BorderLayout.WEST);
            p.add(fieldp, BorderLayout.SOUTH);
                        
            int reply = showOptionDialog(null,p, "New Simulation", new Object[] {"Select", 
                                                                                 startingUp ? "Quit" : "Cancel"}, true);
            if (reply == 1)  // not -1 -- caused by disposing the window, and not 0 -- caused by "Select"
                return false;
                                
            String className = field.getText(); // (String)cb.getEditor().getItem();
            try
                {
                // check first for a default constructor
                java.lang.reflect.Constructor cons = Class.forName(className).getConstructor(new Class[] {});
                // okay, we're past that.  Now try to build the instance
                GUIState state = (GUIState)(Class.forName(className).newInstance());
                Console c = new Console(state);
                c.setVisible(true);
                return true;
                }
            catch (NoSuchMethodException e)
                {
                Utilities.informOfError(e, "The simulation does not have a default constructor: " + className, originalFrame);
                }
            catch (Throwable e)  // Most likely NoClassDefFoundError
                {
                Utilities.informOfError(e, 
                    "An error occurred while creating the simulation " + className, originalFrame);
                }
            }
        }

    /** The last filename the user requested.  Used to open file dialogs intelligently */
    File simulationFile = null;

    /** Lets the user checkpoint out a simulation to a file with a given name. */
    public void doSaveAs()
        {
        FileDialog fd = new FileDialog(this, "Save Simulation As...", FileDialog.SAVE);
        if (simulationFile == null)
            {
            fd.setFile("Untitled.checkpoint");
            }
        else
            {
            fd.setFile(simulationFile.getName());
            fd.setDirectory(simulationFile.getParentFile().getPath());
            }
        fd.setVisible(true);;
        File f = null; // make compiler happy
        if (fd.getFile() != null)
            try
                {
                f = new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(), ".checkpoint"));
                simulation.state.writeToCheckpoint(f);
                simulationFile = f;
                } 
            catch (Exception e) // fail
                {
                Utilities.informOfError(e, 
                    "An error occurred while saving the simulation to the file " + (f == null ? " " : f.getName()), null);
                }
        }


    /** Lets the user checkpoint out a simulation to the last checkpoint filename. */
    public void doSave()
        {
        if (simulationFile == null)
            {
            doSaveAs();
            } 
        else
            try
                {
                simulation.state.writeToCheckpoint(simulationFile);
                }
            catch (Exception e) // fail
                {
                Utilities.informOfError(e, 
                    "An error occurred while saving the simulation to the file " + simulationFile.getName(), null);
                }
        }


    /** Reverts the current simulation to the simulation stored at a user-specified checkpoint filename. */
    public void doOpen()
        {
        FileDialog fd = new FileDialog(this, "Load Saved Simulation...", FileDialog.LOAD);
        fd.setFilenameFilter(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                return Utilities.ensureFileEndsWith(name, ".checkpoint").equals(name);
                }
            });

        if (simulationFile != null)
            {
            fd.setFile(simulationFile.getName());
            fd.setDirectory(simulationFile.getParentFile().getPath());
            }
                
        boolean failed = true;
        int originalPlayState = getPlayState();
        if (originalPlayState == PS_PLAYING) // need to put into paused mode
            pressPause();
                
        fd.setVisible(true);
        File f = null; // make compiler happy
        if (fd.getFile() != null)
            try
                {
                f = new File(fd.getDirectory(), fd.getFile());
                if (!simulation.readNewStateFromCheckpoint(f))
                    throw new RuntimeException("Invalid SimState class.  Original state: " + simulation.state);
                simulationFile = f;
                
                buildModelInspector();  // throw away old one, link in new one
                removeAllInspectors(true);  // kill all existing inspectors

                if (originalPlayState == PS_STOPPED)
                    pressPause(false);
                failed = false;                         // hooray, succeeded
                }        
            catch (Throwable e) // fail  -- could be an Error or an Exception
                {
                Utilities.informOfError(e, 
                    "An error occurred while loading the simulation from the file " + 
                    (f == null ? fd.getFile(): f.getName()), null);
                }
                
        // if we failed, reset play state.  If we were stopped, do nothing (we're still stopped).
        // if we were playing, we're presently paused and should continue playing.  If we
        // were paused, do nothing.
        if (failed && originalPlayState == PS_PLAYING)
            pressPause();  // unpause

        //setTime(simulation.state.schedule.time());
        updateTime(simulation.state.schedule.getSteps(), simulation.state.schedule.time(), -1.0);        
        
        // the random seed is no longer true -- who knows what the orignal seed was -- so we'll
        // set the field to "Unknown"
        randomField.setValue("Unknown");
        }








    /////////////////////// SHOW/HIDE DISPLAY BUTTON FUNCTIONS

    /** Called when the "show" button is pressed in the Displays window */
    synchronized void pressShow()
        {
        Object[] vals = (Object[]) (frameListDisplay.getSelectedValues());
        for (int x = 0; x < vals.length; x++)
            {
            ((JFrame) (vals[x])).toFront();
            ((JFrame) (vals[x])).setVisible(true);
            }
        frameListDisplay.repaint();
        }

    /** Called when the "show all" button is pressed in the Displays window */
    synchronized void pressShowAll()
        {
        Object[] vals = (Object[]) (frameList.toArray());
        for (int x = 0; x < vals.length; x++)
            {
            ((JFrame) (vals[x])).toFront();
            ((JFrame) (vals[x])).setVisible(true);
            }
        frameListDisplay.repaint();
        }

    /** Called when the "hide" button is pressed in the Displays window */
    synchronized void pressHide()
        {
        Object[] vals = (Object[]) (frameListDisplay.getSelectedValues());
        for (int x = 0; x < vals.length; x++)
            {
            ((JFrame) (vals[x])).setVisible(false);
            }
        frameListDisplay.repaint();
        }

    /** Called when the "hide all" button is pressed in the Displays window */
    synchronized void pressHideAll()
        {
        Object[] vals = (Object[]) (frameList.toArray());
        for (int x = 0; x < vals.length; x++)
            {
            ((JFrame) (vals[x])).setVisible(false);
            }
        frameListDisplay.repaint();
        }








    /////////////////////// PLAY/STOP/PAUSE BUTTON FUNCTIONS


    /** Called when the user presses the stop button.  You can call this as well to simulate the same. */
    public synchronized void pressStop()
        {
        if (getPlayState() != PS_STOPPED)
            {
            stopButton.setIcon(I_STOP_ON);
            stopButton.setPressedIcon(I_STOP_OFF);
            playButton.setIcon(I_PLAY_OFF);
            playButton.setPressedIcon(I_PLAY_ON);
            pauseButton.setIcon(I_PAUSE_OFF);
            pauseButton.setPressedIcon(I_PAUSE_ON);

            repaint();
            killPlayThread();
            simulation.finish();
            stopAllInspectors(true);                // stop the inspectors, letting them flush themselves out
            setPlayState(PS_STOPPED);
            repaint();

            // increment the random number seed if the user had said to do so
            if (incrementSeedOnPlay.isSelected())
                {
                randomSeed++;
                setRandomNumberGenerator(randomSeed);
                }
            }
        
        // now let's start again if the user had stated a desire to repeat the simulation automatically
        if (getShouldRepeat())
            {
            // do this later -- don't just call pressPlay() here because it could
            // get us in an infinite loop if the user calls pressStop() for some reason
            // in his start() method.
            SwingUtilities.invokeLater(new Runnable() { public void run() { pressPlay(); }});
            }
        }
        
        
    /** Called when the user presses the pause button.  You can call this as well to simulate the same.  Keep in mind that pause is a toggle. */
    public synchronized void pressPause()
        {
        pressPause(true);
        }
        
    // presses the pause button.  If the simulation is presently stopped, and
    // shouldStartSimulationIfStopped is true (the default), then the simulation
    // is started and put into a paused state.  The only situation where you'd not
    // want to do this is if you're loading a simulation from a stopped state (see
    // doOpen() ).
    //
    synchronized void pressPause(boolean shouldStartSimulationIfStopped)
        {
        if (getPlayState() == PS_PLAYING) // pause
            {
            killPlayThread();

            pauseButton.setIcon(I_PAUSE_ON);
            pauseButton.setPressedIcon(I_PAUSE_OFF);
            playButton.setIcon(I_STEP_OFF);
            playButton.setPressedIcon(I_STEP_ON);
            setPlayState(PS_PAUSED);
            refresh();  // update displays even if they're skipping
            } 
        else if (getPlayState() == PS_PAUSED) // unpause
            {
            pauseButton.setIcon(I_PAUSE_OFF);
            pauseButton.setPressedIcon(I_PAUSE_ON);
            playButton.setIcon(I_PLAY_ON);
            playButton.setPressedIcon(I_PLAY_OFF);

            spawnPlayThread();
            setPlayState(PS_PLAYING);
            } 
        else if (getPlayState() == PS_STOPPED) // start stepping
            {
            // Be careful adding to here -- we should just optionally start
            // the simulation and then set the various icons and change the
            // play state.  Additional stuff should be done only with consideration
            // and examination of how it's used in doOpen()...  -- Sean 
            if (shouldStartSimulationIfStopped) startSimulation();
            
            stopButton.setIcon(I_STOP_OFF);
            stopButton.setPressedIcon(I_STOP_ON);

            pauseButton.setIcon(I_PAUSE_ON);
            pauseButton.setPressedIcon(I_PAUSE_OFF);
            playButton.setIcon(I_STEP_OFF);
            playButton.setPressedIcon(I_STEP_ON);
            setPlayState(PS_PAUSED);
            refresh();  // update displays even if they're skipping
            }

        repaint();
        }

        
    /** Called when the user presses the play button.  You can call this as well to simulate the same.  Keep in mind that play will change to step if pause is down. */
    public synchronized void pressPlay()
        {
        if (getPlayState() == PS_STOPPED)
            {
            // set up states
            stopButton.setIcon(I_STOP_OFF);
            stopButton.setPressedIcon(I_STOP_ON); 

            playButton.setIcon(I_PLAY_ON);
            playButton.setPressedIcon(I_PLAY_OFF);
            pauseButton.setIcon(I_PAUSE_OFF);
            pauseButton.setPressedIcon(I_PAUSE_ON);
            repaint();

            startSimulation();

            spawnPlayThread();

            setPlayState(PS_PLAYING);
            } 
        else if (getPlayState() == PS_PAUSED) // step N times
            {
            for (int x = 0; x < numStepsPerStepButtonPress; x++)
                {
                // at this point we KNOW the play thread doesn't exist
                if (!simulation.step() || simulation.state.schedule.time() >= getWhenShouldEndTime() ||
                    simulation.state.schedule.getSteps() >= getWhenShouldEnd() )
                    // end of run! Clean up in next event loop
                    {
                    pressStop();
                    //setTime(simulation.state.schedule.time());
                    //setStepsPerSecond(-1.0); // negative value, guarantees that nothing is shown
                    updateTime(simulation.state.schedule.getSteps(), simulation.state.schedule.time(), -1.0);
                    break;
                    } 
                else
                    {
                    //setTime(simulation.state.schedule.time());
                    //setStepsPerSecond(-1.0); // negative value, guarantees that nothing is shown
                    updateTime(simulation.state.schedule.getSteps(), simulation.state.schedule.time(), -1.0);
                    }
                }
            refresh();  // update displays even if they're skipping
            }
        repaint();
        }









    /////////////////////// TIME AND FRAME RATE UPDATING FUNCTIONS

    /** The last value the time was set to. */
    double lastTime = sim.engine.Schedule.BEFORE_SIMULATION;
    /** The last value the frame rate was set to. */
    double lastRate = 0.0;
    long lastSteps = 0;
    final static int SHOWING_TIME = 0;
    final static int SHOWING_STEPS = 1;
    final static int SHOWING_TPS = 2;
    final static int SHOWING_NOTHING = -1;
    int showing = SHOWING_TIME;
     
    /** How the frame rate should look */
//    DecimalFormat rateFormat = new DecimalFormat("##0.###");
    NumberFormat rateFormat;


    String lastText = null;
    /** updates the time and frame rate labels to the provided strings and color */
    void updateTimeText(final String timeString)
        {
        if (!(timeString.equals(lastText)))  // only update if we're changing
            {
            lastText = timeString;
            // invokeLater is required in J3D mode or it'll lock -- dunno why
            SwingUtilities.invokeLater(new Runnable()
                {
                public void run()
                    {
                    time.setText(timeString); // will auto-repaint
                    }
                });
            }
        }


    void updateTime()
        {
        long steps; double time; double rate;
        synchronized (this.time)
            {
            steps = lastSteps; time = lastTime; rate = lastRate;
            }
        updateTime(steps,time,rate);
        }
        
    void updateTime(long steps, double time, double rate)
        {
        int showing;
        boolean simulationExists = (simulation != null && simulation.state != null);
        synchronized (this.time) { lastRate = rate; lastSteps = steps; lastTime = time; showing = this.showing; }
        switch(showing)
            {
            case SHOWING_TIME:
                updateTimeText(simulationExists ? 
                    simulation.state.schedule.getTimestamp(lastTime, "At Start", "At End") : "");
                break;
            case SHOWING_STEPS:
                updateTimeText(simulationExists ? "" + lastSteps: "");
                break;
            case SHOWING_TPS:
                if (lastRate != 0) updateTimeText(lastRate < 0 ? "" : rateFormat.format(lastRate));
                break;
            default:
                updateTimeText("");
                break;
            }
        }

    /** Returns the frame rate.  If val is <= 0, then the frame rate is presently unknown. */
    public double getStepsPerSecond()
        {
        synchronized (time)
            {
            return lastRate;
            }
        }





    /////////////////////// METHODS FOR MANIPULATING THE PLAY THREAD
    /////////////////////// These are the most complex to think about methods in Console.  They go through
    /////////////////////// the elaborate dance of spawning or killing the underlying play thread.
    /////////////////////// Handling an underlying thread which paints and updates lots of widgets despite
    /////////////////////// the fact that Swing on top prefers to handle everything through the even thread,
    /////////////////////// AND Java3D does its own weird thread handling underneath -- well, it can get
    /////////////////////// pretty complex.


    /** Interrupts the play thread and asks it to die.  Spin-waits until it dies, repeatedly interrupting it. */

    // synchronized so that if I do a doChangeCode(...) and it checks to see that the playThread is
    // null, then does its stuff, the playThread WILL be null even after the check.
    synchronized void killPlayThread()
        {
        // request that the play thread die
        setThreadShouldStop(true);

        // join the thread.  
        try
            {
            if (playThread != null)
                {
                // we need to do a spin-wait interrupt, then join; rather than
                // a single interrupt followed by a join, because it's possible
                // that the play thread could test if it's interrupted, see that
                // it's not, then we interrupt it, and THEN the play thread goes
                // into its blocking situation.  This is extremely unlikely but
                // theoretically possible.  So we repeatedly interrupt the thread
                // even in this situation until it gets a clue.
                do
                    {
                    try
                        {
                        // grab lock on schedule so interruption can't
                        // occur within movie-making (which causes JMF to freak out)
                        // I hope this doesn't mess up things.  Looks like it shouldn't
                        // (the function of interrupt() here is to release invokeAndWait,
                        // and at that point nothing's blocked on the schedule)
                        synchronized(simulation.state.schedule)
                            {
                            playThread.interrupt();
                            }
                        }
                    catch (SecurityException ex) { } // some stupid browsers -- *cough* IE -- don't like interrupts
                    playThread.join(50);
                    }
                while(playThread.isAlive());
                playThread = null;
                }
            } 
        catch (InterruptedException e)
            { System.err.println("This should never happen: " + e); }
        }




    /** Used to block until a repaint is handled -- see spawnPlayThread() below */
    Runnable blocker = new Runnable()
        {
        public void run()
            {
            // intentionally do nothing
            }
        };



    /** Spawns a new play thread.   The code below actually contains the anonymous subclass that iterates
        the play thread itself.  That's why it's so long.*/

    // synchronized so that if I do a doChangeCode(...) and it checks to see that the playThread is
    // null, then does its stuff, the playThread WILL be null even after the check.
    synchronized void spawnPlayThread()
        {
        setThreadShouldStop(false);

        // start the playing thread
        Runnable run = new Runnable()
            {
            //final static int STEPHISTORY = 32;
            //long[] stephistory = new long[STEPHISTORY];

            public void run()
                {
                try
                    {
                    // set up the step history
                    //long v = System.currentTimeMillis();
                    //for (int x = 0; x < STEPHISTORY; x++)
                    //    stephistory[x] = v;
                    long lastStepTime = System.currentTimeMillis();
                    int currentSteps = 0;
                    double currentRate = 0.0;
                    final long RATE_UPDATE_INTERVAL = 500;

                    // we begin by doing a blocker on the swing event loop.  This gives any
                    // existing repaints a chance to do their thing.  See comments below as to
                    // why such a thing is necessary
                    if (!Thread.currentThread().isInterrupted() && !getThreadShouldStop())
                        try  // it's possible we could be interrupted in-between here (see killPlayThread)
                            {
                            // important here that we're not synchronized on schedule -- because
                            // killPlayThread blocks on schedule before interrupting for JMF bug
                            SwingUtilities.invokeAndWait(blocker);
                            }                    
                        catch (InterruptedException e)
                            {
                            try
                                {
                                Thread.currentThread().interrupt();
                                }
                            catch (SecurityException ex) { } // some stupid browsers -- *cough* IE -- don't like interrupts
                            }                    
                        catch (java.lang.reflect.InvocationTargetException e)
                            {
                            System.err.println("This should never happen: " + e);
                            }                    
                        catch (Exception e)
                            {
                            e.printStackTrace();
                            }
                    // start the main loop

//                    int numSteps = 1;
                    boolean result = true;
                    while (true)
                        {
                        // check to see if we are being asked to quit
                        if (getThreadShouldStop())
                            break;

                        result = simulation.step();
                        double t = simulation.state.schedule.time();
                        long s = simulation.state.schedule.getSteps();
                        //setTime(t);

                        // update the stephistory
                        //for (int x = 0; x < STEPHISTORY - 1; x++)
                        //    stephistory[x] = stephistory[x + 1];
                        //stephistory[STEPHISTORY - 1] = System.currentTimeMillis();
                        //if (numSteps < STEPHISTORY) numSteps++;  // as of first call, this will be equal to 2
                        
                        // BUG in MacOS X 1.4.1.  Reported to Apple and had a long discussion with them.
                        // Basically, when MacOS X's Hotspot gets around to compiling this function, it
                        // smashes the updateTime call.  If you wrote the line thusly...

                        // updatetime(s, t, 1000 / ((stephistory[STEPHISTORY - 1] - stephistory[STEPHISTORY - numSteps]) / (double) numSteps));

                        // ... then it breaks as soon as compilation kicks in.  BAD bug in the VM.  They're
                        // fixed it as of September 10, 2003.  The workaround is
                        // to store in a long first as follows:
                       
                        //                      long a = (stephistory[STEPHISTORY - 1] - stephistory[STEPHISTORY - numSteps]);

                        // now we do something different instead anyway, so the above bug fix is immaterial
                        currentSteps++;
                        long l = System.currentTimeMillis();
                        if (l - lastStepTime >= RATE_UPDATE_INTERVAL)
                            {
                            currentRate = currentSteps / ((double) (l - lastStepTime) / 1000.0);
                            currentSteps = 0;
                            lastStepTime = l;
                            }
                                                        
                        updateTime(s, t, currentRate);  // 1000 / (a / (double) numSteps));

                        // Some steps (notably 2D displays and the timer) call repaint()
                        // to update themselves.  We need to try to guarantee that this repaint()
                        // actually gets fulfilled and not bundled up with other repaints
                        // isssued by the same display.  We do that by blocking on the event
                        // loop here, giving them a chance to redraw themselves without our
                        // thread running.  Issuing an invokeAndWait also has the effect of flushing
                        // out and forcing all current repaints and events; so since we're blocked
                        // waiting, we want to make sure that no events get called which then try
                        // to call us!

                        if (!Thread.currentThread().isInterrupted() && !getThreadShouldStop())
                            try  // it's possible we could be interrupted in-between here (see killPlayThread)
                                {
                                // important here that we're not synchronized on schedule -- because
                                // killPlayThread blocks on schedule before interrupting for JMF bug
                                SwingUtilities.invokeAndWait(blocker);
                                }                        
                            catch (InterruptedException e)
                                {
                                try
                                    {
                                    Thread.currentThread().interrupt();
                                    }
                                catch (SecurityException ex) { } // some stupid browsers -- *cough* IE -- don't like interrupts
                                }                        
                            catch (java.lang.reflect.InvocationTargetException e)
                                {
                                System.err.println("This should never happen" + e);
                                }                        
                            catch (Exception e)
                                {
                                e.printStackTrace();
                                }

                        // let's check if we're supposed to quit BEFORE we do any sleeping...

                        if (!result || getThreadShouldStop() || t >= getWhenShouldEndTime() || t >= getWhenShouldPauseTime() || s >= getWhenShouldEnd() || s >= getWhenShouldPause())
                            break;

                        // sleep for a little while according to the slider
                        long sleep = getPlaySleep();
                        //if (sleep==0 && getShouldYield())
                        //    sleep = 1;
                        if (sleep > 0 && !Thread.currentThread().isInterrupted() && !getThreadShouldStop())
                            try  // it's possible we could be interrupted in-between here (see killPlayThread)
                                {
                                Thread.sleep(sleep);
                                }                        
                            catch (InterruptedException e)
                                {
                                try
                                    {
                                    Thread.currentThread().interrupt();
                                    }
                                catch (SecurityException ex) { } // some stupid browsers -- *cough* IE -- don't like interrupts
                                }                        
                            catch (Exception e)
                                {
                                e.printStackTrace();
                                }
                        //else if (getShouldYield())
                        //    Thread.yield();
                        }

                    // We include this code in case the reason we dropped out was that we
                    // actually ran OUT of simulation time.  So we "press stop" to reset the
                    // buttons and call finish().  Note that this will only happen if the
                    // system actually ISN'T in a PS_STOPPED state yet -- so that should prevent
                    // us from calling finish() twice accidentally if the user just so happens
                    // to press top at exactly the right time.  I think!
                    if (!result || simulation.state.schedule.time() >= getWhenShouldEndTime() ||
                        simulation.state.schedule.getSteps() >= getWhenShouldEnd())
                        SwingUtilities.invokeLater(new Runnable()
                            {
                            public void run()
                                {
                                try
                                    {
                                    pressStop();
                                    }                        
                                catch (Exception e)
                                    {  
                                    System.err.println("This should never happen: " + e);
                                    } // On X Windows, if we close the window during an invokeLater, we get a spurious exception
                                }
                            });
                    else if (simulation.state.schedule.time() >= getWhenShouldPauseTime() ||
                        simulation.state.schedule.getSteps() >= getWhenShouldPause() )
                        SwingUtilities.invokeLater(new Runnable()
                            {
                            public void run()
                                {
                                try
                                    {
                                    pressPause();
                                    // now reset the pause break
                                    pauseField.setValue("");
                                    timePauseField.setValue("");
                                    setWhenShouldPause(Long.MAX_VALUE);
                                    }                        
                                catch (Exception e)
                                    {  
                                    System.err.println("This should never happen: " + e);
                                    } // On X Windows, if we close the window during an invokeLater, we get a spurious exception
                                }
                            });
                    }
                catch(Exception e) {e.printStackTrace();}
                }
            };
        playThread = new Thread(run);
        playThread.setPriority(getThreadPriority());
        playThread.start();
        }









    /////////////////////// METHODS FOR IMPLEMENTING THE CONTROLLER INTERFACE


    /** Simulations can call this to add a frame to be listed in the "Display list" of the console */
    public synchronized boolean registerFrame(JFrame frame)
        {
        frameList.add(frame);
        frameListDisplay.setListData(frameList);
        return true;
        }

    /** Simulations can call this to remove a frame from the "Display list" of the console */
    public synchronized boolean unregisterFrame(JFrame frame)
        {
        frameList.removeElement(frame);
        frameListDisplay.setListData(frameList);
        return true;
        }

    /** Simulations can call this to clear out the "Display list" of the console */
    public synchronized boolean unregisterAllFrames()
        {
        frameList.removeAllElements();
        frameListDisplay.setListData(frameList);
        return true;
        }

    public synchronized void doChangeCode(Runnable r)
        {
        if (playThread != null)
            {
            killPlayThread();
            r.run();
            spawnPlayThread();
            } 
        else
            r.run();
        }

    // we presume this isn't being called from the model thread.
    public void refresh()
        {
        // updates the displays.
        final Enumeration e = frameList.elements();
        while(e.hasMoreElements())
            ((JFrame)(e.nextElement())).getContentPane().repaint();

        // updates the inspectors
        Iterator i = allInspectors.keySet().iterator();
        while(i.hasNext())
            {
            Inspector c = (Inspector)(i.next());
            if (c!=null)  // this is a WeakHashMap, so the keys can be null if garbage collected
                {
                if (c.isVolatile())
                    {
                    c.updateInspector();
                    c.repaint();
                    }
                }
            }
            
        // finally, update ourselves
        if (modelInspector!=null && modelInspector.isVolatile()) 
            {
            modelInspector.updateInspector();
            modelInspector.repaint();
            }
        getContentPane().repaint();
        }








    /////////////////////// METHODS FOR HANDLING INSPECTORS
        
    // Inspectors may be in one of two places: 
    // 1. Stored by the Console in its Inspectors panel
    // 2. Detatched, and stored in a JFrame that should be closed when the inspector is told to go away
    //
    // When the Console stores inspectors in the first case, it places them in the vectors
    // inspectorNames, inspectorStoppables, and inspectorToolbars below.  In both cases,
    // the inspectors are stored in the WeakHashMap allInspectors.  The map is weak so that if
    // the JFrame is closed, the inspector can go away and save some memory perhaps.  Note that
    // not only is the inspector stored weakly, but so is the Stoppable responsible for stopping
    // it.  Thus if no one else is holding onto the Stoppable, it might get GCed.  This is ordinarily
    // not an issue because the JFrame itself is typically holding onto the Stoppable (to call it when
    // the JFrame's close button is pressed).  But in unusual cases you want to make sure that
    // it's held onto.

    // I dislike Vectors, but JList uses them, so go figure...

    /** Holds the names for each inspector presently in the inspectorSwitcher */
    Vector inspectorNames = new Vector();
    /** Holds the Stoppable objects for each inspector presently in the inspectorSwitcher. */
    Vector inspectorStoppables = new Vector();
    /** Holds the toolbars wrapping each inspector presently in the inspectorSwitcher. */
    Vector inspectorToolbars = new Vector();
        
    /** Weakly maps inspectors to their stoppables for all inspectors that might possibly be around.
        Cleaned out when the user presses play. 
        As inspectors are closed or eliminated, they may disappear from this WeakHashMap and be garbage collected. */
    WeakHashMap allInspectors = new WeakHashMap();

    /** Resets the Console-internal inspectors to the values given in the data structures above.  Sets the preferred
        selection.  If it does not exist, sets selection 0.  If that does not exist, no selection is set. */
    void resetInspectors(int preferredSelection)
        {
        // due to apparent bugs in Sun's CardLayout layout manager, the
        // only way to get the first card to appear reliably is to eliminate
        // the panel and the layout manager and recreate them.  Thus we don't need
        // the removeAll() as shown above.
        inspectorSwitcher = new JPanel();
        inspectorSwitcher.setLayout(inspectorCardLayout = new CardLayout());
        int loc = innerInspectorPanel.getDividerLocation();
        innerInspectorPanel.setBottomComponent(inspectorSwitcher);  // side effect: divider location is reset
        innerInspectorPanel.setDividerLocation(loc);  // restore the user-specified divider location
        for(int x=0;x<inspectorToolbars.size();x++)
            // we presume the three vectors are the same length
            inspectorSwitcher.add(((JComponent)(inspectorToolbars.elementAt(x))), "" + x);

        // The next line is required because of a bug in Windows.  Ordinarily if we
        // tell the CardLayout to show a labelled card which doesn't exist
        // (like myCardLayout.show(theContainer, "NoComponentWasAddedWithThisLabel") )
        // it shouldn't show anything.  But on Java for Windows, what it does is give
        // an exception when there are no components (cards) in the CardLayout component
        // at all.  So what we'll do is put a dummy panel in the layout, labelled with a "-1",
        // which is what we'll switch to when we want to display nothing at all.
        inspectorSwitcher.add(new JPanel(), "-1");
        inspectorList.setListData(inspectorNames);
        if (preferredSelection >= inspectorToolbars.size())
            preferredSelection = 0;
        if (preferredSelection >= inspectorToolbars.size())
            preferredSelection = -1;
        inspectorCardLayout.show(inspectorSwitcher, "" + preferredSelection);
        inspectorList.setSelectedIndex(preferredSelection);
      
        boolean shouldEnableButtons = (inspectorNames.size() > 0);
        detatchButton.setEnabled(shouldEnableButtons);
        removeButton.setEnabled(shouldEnableButtons);
        }


    /** Detatches a Console-internal inspector, that is, removes it from the list and makes it into
        its own JFrame */    
    void detatchInspector()
        {
        int currentInspector = inspectorList.getSelectedIndex();
        if (currentInspector == -1) return;
        
        inspectorNames.remove(currentInspector);
        final Stoppable stoppable = (Stoppable)(inspectorStoppables.remove(currentInspector));
        JScrollPane oldInspector = (JScrollPane)(inspectorToolbars.remove(currentInspector));
        Point oldInspectorLocation = oldInspector.getLocationOnScreen();  // set here before inspector goes away

        // make a new JScrollPane -- for some reason sometimes JScrollPanes don't display
        // properly when removed from one window and stuck into another
        Inspector i = (Inspector)(oldInspector.getViewport().getView());
        oldInspector.remove(i);        
        JFrame frame = i.createFrame(stoppable);

        // stick it in a cool place: exactly where the inspector was inside
        // the console
        frame.setLocation(oldInspectorLocation);
        frame.setVisible(true);

        // change selection
        if (inspectorNames.size() == 0)
            currentInspector = -1;
        else if (currentInspector == inspectorNames.size())
            currentInspector--;
        // else currentInspector stays as it is...
        resetInspectors(currentInspector);
        }
            
                        
    /** Adds new inspectors to the Console's list, given the provided inspectors, their portrayals, and appropriate names for them.
        These bags must match in size, else an exception will be thrown. */
    public void setInspectors(final Bag inspectors, final Bag names)
        {
        // clear out old inspectors
        removeAllInspectors(false);
        
        // check for sizes
        if (inspectors.numObjs != names.numObjs)
            throw new RuntimeException("Number of inspectors and names do not match");

        // schedule the inspectors and add them
        for(int x=0;x<inspectors.numObjs;x++)
            {
            if (inspectors.objs[x]!=null)  // double-check
                {
                final int xx = x; // duh, Java's anonymous classes are awful compared to true closures...
                Steppable stepper = new Steppable()
                    {
                    public void step(final SimState state)
                        {
                        SwingUtilities.invokeLater(new Runnable()
                            {
                            Inspector inspector = (Inspector)(inspectors.objs[xx]);
                            public void run()
                                {
                                synchronized(state.schedule)
                                    {
                                    // this is called while we have a lock on state.schedule,
                                    // so we have control over the model.
                                    if (inspector.isVolatile()) 
                                        {
                                        inspector.updateInspector();
                                        inspector.repaint();
                                        }
                                    }
                                }
                            });
                        }
                    };
                
                Stoppable stopper = null;
                try
                    {
                    stopper = ((Inspector)(inspectors.objs[x])).reviseStopper(simulation.scheduleImmediateRepeat(true,stepper));
                    inspectorStoppables.addElement(stopper);
                    }
                catch (IllegalArgumentException ex) { /* do nothing -- it's thrown if the user tries to pop up an inspector when the time is over. */ }

                // add the inspector
                registerInspector((Inspector)(inspectors.objs[x]),stopper);
                JScrollPane scrollInspector = new JScrollPane((Component)(inspectors.objs[x]));
                scrollInspector.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
                inspectorSwitcher.add(scrollInspector, "" + x);
                inspectorNames.addElement((String)(names.objs[x]));
                inspectorToolbars.add(scrollInspector);
                }
            }
        
        resetInspectors(0);

        // switch to the inspector panel and repaint
        try
            {
            tabPane.setSelectedComponent(inspectorPanel);
            Utilities.doEnsuredRepaint(inspectorPanel);
            }
        catch (java.lang.IllegalArgumentException e) { } // it's possible inspectorPanel has been removed
        }

    /**
       Registers an inspector to be Stopped if necessary in the future.  This automatically happens
       if you call setInspectors(...).
    */
    public void registerInspector(Inspector inspector, Stoppable stopper)
        {
        allInspectors.put(inspector, new WeakReference(stopper));  // warning: if no one else refers to stopper, it gets GCed!
        }

    /** Stops all inspectors.  If killDraggedOutWindowsToo is true, then the detatched inspectors are stopped as well. */
    public void stopAllInspectors(boolean killDraggedOutWindowsToo)
        {
        // inspectors may get stop() called on them multiple times
        for(int x=0;x<inspectorStoppables.size();x++)
            {
            Stoppable temp = ((Stoppable)(inspectorStoppables.elementAt(x)));
            if (temp!=null) temp.stop();  // stop all inspectors
            }

        if (killDraggedOutWindowsToo)
            {
            Iterator i = allInspectors.keySet().iterator();
            while(i.hasNext())
                {
                Stoppable stopper = (Stoppable)(((WeakReference)allInspectors.get(i.next())).get());
                if (stopper != null) stopper.stop();
                }
            }
        }

    /** Stops and removes all inspectors. If killDraggedOutWindowsToo is true, then all inspector windows will be closed; else only
        the inspectors presently embedded in the console will be stopped and removed. */
    public void removeAllInspectors(boolean killDraggedOutWindowsToo)
        {
        stopAllInspectors(killDraggedOutWindowsToo);
        if (killDraggedOutWindowsToo)
            {
            // this will probably result in the inspectors getting 'stop' called on them a second time
            Iterator i = allInspectors.keySet().iterator();
            while(i.hasNext())
                {
                Component inspector = (Component)(i.next());
                
                // run up to the top-level component and see if it's the Console or not...
                while(inspector != null && !(inspector instanceof JFrame))
                    inspector = inspector.getParent();
                if (inspector != null && inspector != this)  // not the console -- it's a dragged-out window
                    ((JFrame)(inspector)).dispose();
                }
            allInspectors = new WeakHashMap();
            }
        inspectorNames = new Vector();
        inspectorToolbars = new Vector();
        resetInspectors(-1);
        }

    }

