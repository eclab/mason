/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.sweep;
import java.awt.*;
import javax.swing.JScrollPane;

import ec.util.*;
import sim.display.*;
import sim.display.Console;
import sim.engine.*;
import sim.portrayal.*;
import sim.portrayal.inspector.*;
import sim.portrayal.simple.*;
import sim.util.*;
import sim.util.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.time.*;


public class ParameterSweepGUI extends JPanel
    {
    GUIState state;
    sim.util.Properties properties;
    
    int numMaxSteps = 10000;
    int numTrials = 1;
    int numThreads = 1;
    long initialSeed = 0;
    boolean compressOutput = false;
    int modulo = 0;
    double minValue = 0;
    double maxValue = 1;
    int divisionValue = 1;
        
    JRadioButton independentRadio;
    JRadioButton dependentRadio;
    JRadioButton neitherRadio;
    JButton run;
    JButton stop;
    JPanel cards;
    CardLayout cardLayout;
    JProgressBar progressBar;

    boolean running = false;
    javax.swing.Timer timer;

    /**
     * The current index of the topmost element
     */
    int start = 0;
    /**
     * The number of items presently in the propertyList
     */
    int count = 0;
    int currentIndex = 0;
    
    ParameterSweep parameterSweep = null;
    JList<ParameterSettings> propList;
    private ArrayList<Component> currentComponents = new ArrayList<Component>();

    DefaultListModel<ParameterSettings> propertySettingsList;

    public ParameterSweepGUI(sim.util.Properties properties, final GUIState state) 
        {
        this.state = state;
        this.properties = properties;

        setLayout(new BorderLayout());
        
 
        ///// GLOBAL AND FOOTER

        JPanel p = new JPanel();
        add(p, BorderLayout.SOUTH);
        p.setLayout(new BorderLayout());


        ///// RUN AND STOP BUTTONS IN FOOTER

        run = new JButton(sim.display.Console.iconFor("NotPlaying.png"));
        stop = new JButton(sim.display.Console.iconFor("NotStopped.png"));

        run.setPressedIcon(sim.display.Console.iconFor("Playing.png"));
        stop.setPressedIcon(sim.display.Console.iconFor("Stopped.png"));
        
        run.setBorderPainted(false);
        run.setContentAreaFilled(false);
        run.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        stop.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        stop.setBorderPainted(false);
        stop.setContentAreaFilled(false);
 
        run.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                run();
                }
            });
                
        stop.addActionListener(new ActionListener() 
            {
            public void actionPerformed(ActionEvent e) 
                {
                stop();
                }
            });
            
        progressBar = new JProgressBar(0, 0);
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
        Box footerBox = new Box(BoxLayout.X_AXIS);
        footerBox.add(run);
        footerBox.add(stop);
        JPanel footer = new JPanel();
        footer.setLayout(new BorderLayout());
        footer.add(footerBox, BorderLayout.WEST);
        footer.add(progressPanel, BorderLayout.CENTER);
        p.add(footer, BorderLayout.SOUTH);



        // GLOBAL SETTINGS       
       
        PropertyField maxStepsField = new PropertyField("" + numMaxSteps)
            {
            public String newValue(String value)
                {
                try
                    {
                    numMaxSteps = Math.max(Integer.parseInt(value), 1);
                    }
                catch (NumberFormatException ex) { }
                return "" + numMaxSteps;
                }
            };


        PropertyField moduloField = new PropertyField("" + modulo)
            {
            public String newValue(String value)
                {
                try
                    {
                    modulo = Math.max(Integer.parseInt(value), 0);
                    }
                catch (NumberFormatException ex) { }
                return "" + modulo;
                }
            };
        
        initialSeed = state.state.seed();
        if (initialSeed == 0) initialSeed = 423151237;
        if (initialSeed < 0) initialSeed = 0 - initialSeed;
        
        PropertyField seedField = new PropertyField("" + initialSeed )
            {
            public String newValue(String value)
                {
                try
                    {
                    long s = Long.parseLong(value);
                    if (s > 0)
                        initialSeed = s; 
                    }
                catch (NumberFormatException ex) { }
                return "" + initialSeed;
                }
            };
        
        PropertyField trialsField = new PropertyField("" + numTrials)
            {
            public String newValue(String value)
                {
                try
                    {
                    numTrials = Math.max(Integer.parseInt(value), 1);
                    }
                catch (NumberFormatException ex) { }
                return "" + numTrials;
                }
            };
        
        PropertyField threadsField = new PropertyField("" + numThreads)
            {
            public String newValue(String value)
                {
                try
                    {
                    numThreads = Math.max(Integer.parseInt(value), 1);
                    }
                catch (NumberFormatException ex) { }
                return "" + numThreads;
                }
            };
    
        PropertyField compressField = new PropertyField(null, "" + compressOutput, true, null, PropertyField.SHOW_CHECKBOX)
            {
            public String newValue(String value)
                {
                compressOutput = (value.equals("true"));
                return "" + compressOutput;
                }
            };

        LabelledList globalSettings = new LabelledList("Sweep Settings");
        globalSettings.addLabelled("Num Trials", trialsField);
        globalSettings.addLabelled("Num Threads", threadsField);
        globalSettings.addLabelled("Max Steps", maxStepsField);
        globalSettings.addLabelled("Step Modulo", moduloField);
        globalSettings.addLabelled("Initial Seed", seedField);
        globalSettings.addLabelled("Compress Output File", compressField);

        p.add(globalSettings, BorderLayout.CENTER);




        // Add Property Panel

        independentRadio = new JRadioButton("Independent", true);
        dependentRadio = new JRadioButton("Dependent");
        neitherRadio = new JRadioButton("Neither");

        JButton resetParams = new JButton("Reset Params");
        resetParams.addActionListener(new ActionListener() 
            {
            public void actionPerformed(ActionEvent e) 
                {
                for (int i = 0; i < propList.getModel().getSize(); i++) 
                    {
                    propList.getModel().getElementAt(i).amSet = false;
                    }
                updateParameterSettings(propList.getSelectedValue());
                }
            });


        ButtonGroup bgroup = new ButtonGroup();
        bgroup.add(independentRadio);
        bgroup.add(dependentRadio);
        bgroup.add(neitherRadio);
        neitherRadio.setSelected(true);
                
        Box propertyBox = new Box(BoxLayout.Y_AXIS);
        propertyBox.add(independentRadio);
        propertyBox.add(dependentRadio);
        propertyBox.add(neitherRadio);
        propertyBox.add(resetParams);
        propertyBox.add(propertyBox.createGlue());
        
        JPanel propertyPanel = new JPanel();
        propertyPanel.setLayout(new BorderLayout());
        propertyPanel.add(propertyBox, BorderLayout.WEST);
        
        cards = new JPanel();
        cardLayout = new CardLayout();
        cards.setLayout(cardLayout);
        JPanel dependentPanel = new JPanel();
        JPanel neitherPanel = new JPanel();
        JPanel independentPanel = new JPanel();
        independentPanel.setLayout(new BorderLayout());
        cards.add(dependentPanel, "dependent");
        cards.add(independentPanel, "independent");
        cards.add(neitherPanel, "neither");
        propertyPanel.add(cards, BorderLayout.CENTER);
        cardLayout.show(cards, "neither");
                
        independentRadio.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e) 
                {
                cardLayout.show(cards, "independent");
                changeParameterSettings(propList.getSelectedValue(), true);
                }
            });

        dependentRadio.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e) 
                {
                cardLayout.show(cards, "dependent");
                changeParameterSettings(propList.getSelectedValue(), true);
                }
            });

        neitherRadio.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e) 
                {
                cardLayout.show(cards, "neither");
                changeParameterSettings(propList.getSelectedValue(), true);
                }
            });
                        
        LabelledList list = new LabelledList();
        PropertyField minField = new PropertyField(null, "" + minValue) // , true, new sim.util.Interval(0,1), PropertyField.SHOW_SLIDER)
            {
            public String newValue(String value)
                {
                try
                    {
                    double m = Double.parseDouble(value);
                    if (m != Double.POSITIVE_INFINITY && 
                        m != Double.NEGATIVE_INFINITY && 
                        m == m && // NaN
                        m <= maxValue)
                        {
                        minValue = m;
                        changeParameterSettings(propList.getSelectedValue(), false);
                        }
                    }
                catch (NumberFormatException ex) { }
                return "" + minValue;
                }
            };
        minField.getField().setColumns(8);
        list.addLabelled("Min ", minField);

        PropertyField maxField = new PropertyField(null, "" + maxValue) //   , true, new sim.util.Interval(0,1), PropertyField.SHOW_SLIDER)
            {
            public String newValue(String value)
                {
                try
                    {
                    double m = Double.parseDouble(value);
                    if (m != Double.POSITIVE_INFINITY && 
                        m != Double.NEGATIVE_INFINITY && 
                        m == m && // NaN
                        m >= minValue)
                        {
                        maxValue = m;
                        changeParameterSettings(propList.getSelectedValue(), false);
                        }
                    }
                catch (NumberFormatException ex) { }
                return "" + maxValue;
                }
            };
        list.addLabelled("Max ", maxField);
        maxField.getField().setColumns(8);

        PropertyField divisionField = new PropertyField("" + divisionValue)
            {
            public String newValue(String value)
                {
                try
                    {
                    divisionValue = Math.max(Integer.parseInt(value), 1);
                    changeParameterSettings(propList.getSelectedValue(), false);
                    }
                catch (NumberFormatException ex) { }
                return "" + divisionValue;
                }
            };
        list.addLabelled("Divisions ", divisionField);
        independentPanel.add(list, BorderLayout.CENTER);
                
                
                
                
        // Add property list
        propList = new JList<ParameterSettings>();
        propertySettingsList = new DefaultListModel<ParameterSettings>();

        for (int i = 0; i < properties.numProperties(); i++) 
            {
            if(!properties.isHidden(i) && (properties.getType(i)==Integer.TYPE
                    ||properties.getType(i)==Double.TYPE || properties.getType(i)==Float.TYPE
                    || properties.getType(i)==Boolean.TYPE || properties.getType(i)==Long.TYPE))
                propertySettingsList.addElement(new ParameterSettings(properties, i));
            }
        propList.setModel(propertySettingsList);
        propList.setVisibleRowCount(10);

        ListSelectionListener listener = new ListSelectionListener() 
            {
            public void valueChanged(ListSelectionEvent e) 
                {
                updateParameterSettings(propList.getSelectedValue());
                }
            };
            
        propList.getSelectionModel().addListSelectionListener(listener);
        
        // show the first item in the list's settings by default
        propList.setSelectedIndex(0);
        


                
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createTitledBorder("Model Variables"));
        header.add(propertyPanel, BorderLayout.SOUTH);
        header.add(new JScrollPane(propList), BorderLayout.CENTER);             
        add(header, BorderLayout.CENTER);
        }




    public boolean isValidConfiguration() 
        {
        boolean isValidConfiguration = true;
        boolean hasIndependent = false;
        boolean hasDependent = false;
        for(int i = 0; i<propList.getModel().getSize(); i++)
            {
            ParameterSettings prop = propList.getModel().getElementAt(i);
            if(prop.amSet && prop.amDependent)
                {
                hasIndependent = true;
                }else if(prop.amSet && !prop.amDependent)
                {
                hasDependent = true;
                }
            }
        if(!hasIndependent || !hasDependent)
            {
            isValidConfiguration = false;
            }
        return isValidConfiguration;
        }


    void updateParameterSettings(ParameterSettings currentProp)
        {
        if (currentProp.amSet)
            {
            if (currentProp.amDependent)
                {
                dependentRadio.setSelected(true);
                cardLayout.show(cards, "dependent");
                }
            else
                {
                minValue = currentProp.min;
                maxValue = currentProp.max;
                divisionValue = currentProp.divisions;
                independentRadio.setSelected(true);
                cardLayout.show(cards, "independent");
                }
            }
        else
            {
            neitherRadio.setSelected(true);
            cardLayout.show(cards, "neither");
            }

        propList.repaint();
        cards.repaint();
        }


    void changeParameterSettings(ParameterSettings currentProp, boolean entering)
        {
        // update card
                
        if (dependentRadio.isSelected())
            {
            currentProp.amSet = true;
            currentProp.amDependent = true;
            }
        else if (independentRadio.isSelected())
            {
            currentProp.amSet = true;
            currentProp.amDependent = false;
            if (entering)
                {
                minValue = currentProp.min;
                maxValue = currentProp.max;
                divisionValue = currentProp.divisions;
                }
            else
                {
                currentProp.min = minValue;
                currentProp.max = maxValue;
                currentProp.divisions = divisionValue;
                }
            }
        else // neither
            {
            currentProp.amSet = false;
            }
                
        propList.repaint();
        cards.repaint();
        }
        
    void run()
        {
        if      (!isValidConfiguration())
            {
            JOptionPane.showMessageDialog(null, "You need to have both an independent and a dependent variable set to run");
            return;
            }
        else
            {
            String filePath = getFilePath(); 
            if (filePath == null) { return; }
            final ParameterDatabase pd = ParameterSettings.convertToDatabase(propList.getModel(), state.state, numMaxSteps, modulo, numTrials, numThreads, initialSeed, compressOutput, filePath);
            pd.listNotAccessed(new PrintWriter(new OutputStreamWriter(System.out)));

            // set up ParameterSweep

            try
                {
                parameterSweep = new ParameterSweep(pd);
                }
            catch (ClassNotFoundException ex)
                {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Invalid class " + state.state);
                return;
                }
                                
            new Thread(new Runnable()
                {
                public void run() 
                    {
                    System.err.println("STARTED");
                    running = true;
                    run.setIcon(sim.display.Console.iconFor("Playing.png"));
                    stop.setIcon(sim.display.Console.iconFor("NotStopped.png"));
                    try 
                        {
                        timer = new javax.swing.Timer(1000, new ActionListener() 
                            {
                            public void actionPerformed(ActionEvent e)
                                {
                                System.err.println("progress: " + parameterSweep.getJobCount());
                                if (running)
                                    {
                                    progressBar.setMaximum(parameterSweep.getTotalJobs());
                                    progressBar.setMinimum(0);
                                    progressBar.setValue(parameterSweep.getJobCount());
                                    }
                                }
                            });
                        timer.start();
                        parameterSweep.run();
                        parameterSweep.waitUntilStopped();
                        }
                    catch(Exception e)
                        {
                        e.printStackTrace();
                        }
                    System.err.println("STOPPED");
                    running = false;
                    timer.stop();
                    run.setIcon(sim.display.Console.iconFor("NotPlaying.png"));
                    stop.setIcon(sim.display.Console.iconFor("Stopped.png"));
                    progressBar.setMaximum(0);
                    progressBar.setMinimum(0);
                    progressBar.setValue(0);
                    }
                }).start();
            }
        }
                
    void stop()
        {
        // request a stop
        if (parameterSweep!=null) 
            {
            parameterSweep.stop = true;
            }
        }



    private String getFilePath() {
        String filePath = "";
        FileDialog dialog = new FileDialog((Frame)null, "Save Results to File...", FileDialog.SAVE);
        dialog.setFilenameFilter(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                return !name.endsWith(".csv");
                }
            });
            
        dialog.setVisible(true);
        String filename = dialog.getFile();
        if (filename == null)
            return null;
        filename = filename + ".csv";
        if (compressOutput) { filename = filename + ".gz"; }
        String directory = dialog.getDirectory();
                
        try 
            { 
            filePath = new File(new File(directory), filename).getCanonicalPath();
            }
        catch (IOException ex)
            {
            ex.printStackTrace();
            return null;
            }
        return filePath;
        }
    }
