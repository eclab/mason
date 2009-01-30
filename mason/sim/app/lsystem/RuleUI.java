/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

// Class RuleUI
package sim.app.lsystem;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import sim.util.gui.*;

// This class becomes the Rules pane of the Console

public class RuleUI extends JPanel
    {
    // components
    JButton buttonGo = new JButton("Calculate");
    JButton buttonCancel = new JButton("Cancel");
    JButton buttonSave = new JButton("Save");
    JButton buttonLoad = new JButton("Load");
    JButton buttonHelp = new JButton("Help");

    // put the table in its own scroll pane, as the console is not too big
    JTable ruleTable = new JTable(20,2);
    JScrollPane scrollPane = new JScrollPane(ruleTable);
    
    JProgressBar calcProgress = new JProgressBar(0,100);
    
    JTextField seedField = new JTextField("F-F-F-F", 10);
    JTextField stepField = new JTextField("3", 3);
    
    // help panel (see very end of the init() function for elaboration on this)
    JPanel helpPanel = new JPanel();
    
    // references to sim with ui, sim state
    LSystemWithUI lsui;
    LSystem ls;
    
    // so that we can update the draw settings tab also
    DrawUI dui;
    
    // for calculation thread
    int steps=0;                // expansions
    Runnable calcRunnable;
    Thread calcThread;
    Object lock = new Object();
    boolean stop = false;
    
    // returns the frame that should be used as the parent frame for dialogs
    public Frame getFrame()
        {
        Component c = this;
        
        while(c.getParent() != null)
            c = c.getParent();
        
        return (Frame)c;
        }
    
    
    // this is currently used only by buttonGo
    // it takes the currently entered data for rules and sends it to the LSystem.LSystemData intstance
    void getRulesFromUI()
        {
        // set l-system parameters
            
        // seed
        ls.l.seed = seedField.getText();
        LSystemData.setVector(ls.l.code, ls.l.seed);
        // expansions
        ls.l.expansions = Integer.valueOf(stepField.getText()).intValue();
        
        // erase old rules
        ls.l.rules.clear();
        
        // build new rule list
        for(int r=0; r<ruleTable.getRowCount(); r++)
            {
            // if both not null
            if(ruleTable.getValueAt(r,0) != null && ruleTable.getValueAt(r,1) != null)
                // and both not zero length
                if(((String)(ruleTable.getValueAt(r,0))).length() > 0 && ((String)(ruleTable.getValueAt(r,0))).length() > 0)
                    ls.l.rules.add( new Rule( (byte)(((String)(ruleTable.getValueAt(r,0))).
                                substring(0,1).charAt(0)), (String)ruleTable.
                            getValueAt(r,1)) );
            }
        
        // set # of expansions
        steps = Integer.valueOf(stepField.getText()).intValue();
        }
    
    
    // constructor
    public RuleUI(LSystemWithUI nLsui, DrawUI nDui)
        {
        lsui = nLsui;
        ls = (LSystem)lsui.state;
        dui = nDui;
        
        try
            {
            init();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        }
    
    
    
    public void init()
        {
        // This runnable calculates the expansions of the L-system when the "Calculate"
        // button is pushed.  Because it runs in a separate thread, it can conveniently 
        // be cancelled, and updates a JProgressBar to show that it is still thinking.
        calcRunnable = new Runnable()
            {
            public void run()
                {
                int h=0; //number of expansions
                int p=0; //position in original code
                int r=0; //rule check index
                boolean ruleApplied = false; // has a rule been applied to this symbol yet
                
                // Speed... Make a new ByteList and copy into there
                // instead of inserting into the old one and shifting the elements over...
                // Also, I have not written an insert function, so this is a double bonus.
                ByteList newCode;
                newCode = new ByteList(ls.l.code.b.length);
                
                // main expanion loop
                while(true)
                    {
                    // stop if external stop requested
                    // this occurrs when the cancel button is pressed
                    synchronized(lock)
                        {
                        if(stop)
                            break;
                        }
                    
                    // stop if enough expansions have been completed
                    if(h >= steps)
                        break;
                    
                    // else keep expanding
                    ruleApplied = false;        // reset this
                    
                    for(r=0; r<ls.l.rules.size(); r++)
                        {
                        if(ls.l.code.b[p] == (((Rule)ls.l.rules.get(r)).pattern))               // replace!
                            {
                            newCode.addAll(((Rule)ls.l.rules.get(r)).replace);
                            ruleApplied = true;
                            
                            // dont try to expand extra rules, that would be trouble
                            break;
                            }
                        }
                    
                    if(!ruleApplied)            // if no rule was found for this item
                        {
                        newCode.add(ls.l.code.b[p]);
                        }
                    
                    p++;                        // increment p to go to the next
                    
                        
                    // Cycle the progress bar to show thinking!
                    // You're probably thinking.. "Hey.. this is such a waste of time, why 
                    // didn't you just use that nifty 'barber pole' auto-scrolling effect?"
                    // Well, that's good stuff, and it's called "Indeterminate mode".  However, 
                    // this capability seems to only exist in >= 1.4...
                    if(p%100 == 0)
                        {
                        SwingUtilities.invokeLater(
                            new Runnable()
                                {
                                public void run()
                                    {
                                    int i = calcProgress.getValue();
                                    if(i < 100)
                                        i++;
                                    else
                                        i = 0;
                                    
                                    calcProgress.setValue(i);
                                    }
                                }
                            );
                        }
                     
                    // an expansion has been completed
                    // hurray
                    if(p >= ls.l.code.length)
                        {
                        p = 0;
                        h++;
                        ls.l.code = newCode;
                        newCode = new ByteList(ls.l.code.length);
                        }
                    }
                // end main expansion loop

                // on successful end, enable calculate and disable cancel buttons
                SwingUtilities.invokeLater(
                    new Runnable()
                        {
                        public void run()
                            {
                            buttonGo.setEnabled(true);
                            buttonCancel.setEnabled(false);
                            calcProgress.setValue(0);
                            calcProgress.setString("Done!");
                            }
                        }
                    );
            
                }// end run
            };
        
        // buttonGo calculates the expansions of the rules from the given seed
        buttonGo.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                getRulesFromUI();
                
                // now we will begin..
                calcProgress.setString("Calculating...");
                
                // expand
                // in a separate thread
                stop = false;
                calcThread = new Thread(calcRunnable);
                calcThread.start();
                
                // juggle buttons
                buttonCancel.setEnabled(true);
                buttonGo.setEnabled(false);
                }
            });
                
        // buttonCancel stops an expansion being processed (started by buttonGo "Calculate")
        buttonCancel.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                synchronized(lock)
                    {
                    // let the thread stop on its own...
                    stop = true;
                    }
                
                // but wait for it
                try
                    {
                    calcThread.join();
                    }
                catch (Exception ex)
                    {
                    ex.printStackTrace();
                    }
                
                // reset buttons
                calcProgress.setValue(0);
                calcProgress.setString("Cancelled");
                buttonCancel.setEnabled(false);
                buttonGo.setEnabled(true);
                }
            });
        
        
        // buttonSave saves the current seed, rules, draw settings, and expansions
        // Saves the data after a Calculate has been executed.. so if you
        // Enter data A
        // then Calculate
        // then Enter data B
        // then save, you will be saving data A.
        // so be careful!
        buttonSave.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                try
                    {

                    JOptionPane.showMessageDialog( getFrame(), 
                        "<html>IF you have changed the settings since the last time you calculated the L-system,<br>"+
                        "the L-system you save will be the one last calculated--not the current data!</html>" );
                    
                    // show save dialog
                    FileDialog fd = new FileDialog(getFrame(), 
                        "Save Current L-System Settings As", FileDialog.SAVE);
                    fd.setFile("Untitled.lss");
                    fd.setVisible(true);;
                    
                    // on cancel, return
                    if(fd.getFile() == null)
                        return;
                    
                    // else do the thing
                    File outputFile = new File(fd.getDirectory(), fd.getFile());
                    FileOutputStream outputFileStream = new FileOutputStream(outputFile);
                    java.util.zip.GZIPOutputStream g = new java.util.zip.GZIPOutputStream(
                        new BufferedOutputStream(outputFileStream));
                    
                    ObjectOutputStream out = new ObjectOutputStream(g);
                    out.writeObject(ls.l);
                    
                    // now need to do a little dance with the GZIPOutputStream to write
                    // this stuff out correctly -- see sim.engine.SimState.writeToCheckpoint(OutputStream)
                    // for more info
                    
                    out.flush();
                    g.finish();
                    g.flush();
                    out.close();
                    }
                catch (Exception ex)
                    {
                    ex.printStackTrace();
                    }
                }
            });
        
        // buttonLoad loads the file's seed, rule, and expansions
        buttonLoad.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                try
                    {
                    // show load dialog
                    FileDialog fd = new FileDialog(getFrame(), 
                        "Open L-System Settings File (.lss)", FileDialog.LOAD);
                    fd.setFile("*.lss");
                    fd.setVisible(true);;
                    
                    // on cancel, return
                    if(fd.getFile() == null)
                        return;
                        
                    File inputFile = new File(fd.getDirectory(), fd.getFile());
                    FileInputStream inputFileStream = new FileInputStream(inputFile);
                    ObjectInputStream in = new ObjectInputStream(
                        new java.util.zip.GZIPInputStream(new BufferedInputStream(inputFileStream)));
                    ls.l = (LSystemData)in.readObject();
                    in.close();
                    
                    
                    // change UI fields to reflect newly loaded settings
                    // seed
                    seedField.setText(ls.l.seed);
                    // # expansions
                    stepField.setText(String.valueOf(ls.l.expansions));
                    // line size
                    dui.distField.setText(String.valueOf(ls.l.segsize));
                    // angle... has been stored in radians, so un-radian it
                    dui.angleField.setText(String.valueOf(ls.l.angle*180/Math.PI));
                    // x, y
                    // --- unneccessary now that Display2D options do this
                    //dui.xField.setText(String.valueOf(ls.l.x));
                    //dui.yField.setText(String.valueOf(ls.l.y));
                    
                    
                    // rules
                    // first clear table
                    for(int t=0; t<ruleTable.getRowCount(); t++)
                        {
                        ruleTable.setValueAt(null, t, 0);
                        ruleTable.setValueAt(null, t, 1);
                        }
                    
                    // now set new stuff
                    for(int t=0; t<ls.l.rules.size(); t++)
                        {
                        ruleTable.setValueAt(String.valueOf((char)((Rule)(ls.l.rules.get(t))).pattern), t, 0);
                        ruleTable.setValueAt(LSystemData.fromVector(((Rule)(ls.l.rules.get(t))).replace), t, 1);
                        }
                    }
                catch (Exception ex)
                    {
                    ex.printStackTrace();
                    }
                }
            });
        
        // buttonHelp displays symbol help
        buttonHelp.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                // this makes it free-floating rather than modal
                // so you can move to the side and consult the dialog while using the console
                JFrame help = new JFrame();
                help.getContentPane().setLayout( new BorderLayout() );
                help.getContentPane().add(helpPanel, BorderLayout.CENTER);
                help.setSize(400,300);
                help.setVisible(true);
                }
            });

         
        ///// OK
        // now build the actual UI
        this.setLayout(new BorderLayout());
        
        // so much panel juggling
        // looks like this:
        /*
          All JPanels use BorderLayout.
          N = NORTH, C = CENTER, S = SOUTH
          
          +-------JPanel this (ruleUI)-----------------+
          | +-------JPanel "mid"---------------------+ |
          | | +--------JPanel "top"----------------+ | |
          | | | +--------LabelledList "list"-----+ | | |
          | | |N| Seed           =============== | | | |
          | |N| | Expanions      =============== | | | |
          |N| | +--------------------------------+ | | |
          | | |C  Box( Calculate   Cancel )        | | |
          | | |S  (____progressbar__________)      | | |
          | | +------------------------------------+ | |
          | |C  Box( Save    Load )                  | |
          | +----------------------------------------+ |
          |                                            |
          |C     JScrollPane(JTable rules)             |
          +--------------------------------------------+
        
          So that's the overview of the next 50 lines or so
        */
        
        JPanel top = new JPanel();
        JPanel mid = new JPanel();
        top.setLayout(new BorderLayout());
        mid.setLayout(new BorderLayout());
        
        // List of L-system parameters.. what is a LabelledList?
        // sim.display.LabelledList is a convenient way to draw lists of the format
        //  text   component
        //  text   component
        LabelledList list = new LabelledList()
            {
            Insets insets = new Insets(5, 5, 5, 5);
            public Insets getInsets()
                {
                return insets;
                }
            };
        
        seedField.setText(ls.l.seed);
        
        list.addLabelled("Seed", seedField);
        list.addLabelled("Expansions", stepField);
        
        // add everything so far (in list)
        top.add(list, BorderLayout.NORTH);
        
        // box with calculate and cancel buttons
        Box b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(5, 5, 5, 5);
            public Insets getInsets()
                {
                return insets;
                }
            };
        
        b.add(buttonGo);
        b.add(buttonCancel);
        buttonCancel.setEnabled(false);
        b.add(Box.createGlue());     
        
        top.add(b, BorderLayout.CENTER);
        
        top.add(calcProgress, BorderLayout.SOUTH);
        calcProgress.setStringPainted(true);
        calcProgress.setString("Press Calculate");
        
        b = new Box(BoxLayout.X_AXIS)
            {
            Insets insets = new Insets(5, 5, 5, 5);
            public Insets getInsets()
                {
                return insets;
                }
            };
        
        b.add(buttonSave);
        b.add(buttonLoad);
        b.add(buttonHelp);
        b.add(Box.createGlue());     
        
        mid.add(top, BorderLayout.NORTH);
        mid.add(b, BorderLayout.CENTER);
        this.add(mid, BorderLayout.NORTH);
        
        // allows table to have named headers
        class NamedTableModel extends DefaultTableModel
            {
            NamedTableModel(int rows, int cols)
                {
                super(rows, cols);
                }
            
            public String getColumnName( int i )
                {
                if(i == 0)
                    return "Symbol";
                else if(i == 1)
                    return "Replacement";
                else
                    return "Error.";
                }
            };
        
        ruleTable.setModel(new NamedTableModel(20,2));
        
        // rules table setup
        // same defaults as in LSystemWithUI.java
        seedField.setText("F");
        
        ruleTable.setValueAt("F", 0, 0);
        ruleTable.setValueAt("F[+F]F[-F]F", 0, 1);
        
        this.add(scrollPane, BorderLayout.CENTER);
        
        
        // Make the Help popup!
        // This is executed on a click of buttonHelp.. but we've got it set up now.
        LabelledList list2 = new LabelledList();
        
        helpPanel.setLayout(new BorderLayout());
        
        list2.addLabelled("Symbols", new JLabel(""));
        list2.addLabelled("Uppercase (A-Z)", new JLabel("Draw forward Distance units"));
        list2.addLabelled("Lowercase (a-z)", new JLabel("Move forward Distance units (no draw)"));
        list2.addLabelled("-", new JLabel("Turn right by angle degrees"));
        list2.addLabelled("+", new JLabel("Turn left by angle degrees"));
        list2.addLabelled("[", new JLabel("Push position, angle"));
        list2.addLabelled("]", new JLabel("Pop position, angle"));
        list2.addLabelled("", new JLabel(""));
        list2.addLabelled("Save: ", new JLabel("Saves the rules, seed, draw settings, and "));
        list2.addLabelled("", new JLabel("calculated expansions of the "));
        list2.addLabelled("", new JLabel("Last calculated L-system."));
        list2.addLabelled("Load: ", new JLabel("Loads saved L-system settings."));
        
        helpPanel.add(list2, BorderLayout.CENTER);
        }
    }
