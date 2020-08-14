/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.opt;

import java.awt.*;

import ec.util.*;
import sim.display.*;
import sim.util.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;

public class OptimizeGUI extends JPanel
    {
	public static final String MU_UNICODE = "\u03BC";
	public static final String LAMBDA_UNICODE = "\u03BB";
	
    GUIState state;
    sim.util.Properties properties;
 
    // Global Settings
    int numMaxSteps = 1000;
    int numTrials = 1;
    int numObjectives = 1;
    int popSize = 24;
    int numGenerations = 30;
    boolean rebuildP = false;
    int numThreads = 1;
    long initialSeed = 0;
    boolean compressOutput = false;
    int modulo = 0;
    static final double DEFAULT_MIN = 0.0;
    static final double DEFAULT_MAX = 1.0;
    double minValue = DEFAULT_MIN;
    double maxValue = DEFAULT_MAX;
    PropertyField minField;
    PropertyField maxField;
//    int divisionValue = 1;
    // GA
    int tournamentSize = 2;
    double mutationRate = 1.0;
    double mutationStDev = 0.1;
    double distributionIndex = 20;
    double crossover = 0.5;
    // Steady State GA
    double replacementProb = 1.0;
    // ES
    int mu = 10;
//    int lambda = 100;
    // NSGA2
    enum MutationType
	    {
	    POLYNOMIAL("polynomial"),
	    GAUSSIAN("gauss")
	    ;
	    static MutationType DEFAULT = POLYNOMIAL;
    	String parameterValue;
    	MutationType(String parameterValue)
    		{
    		this.parameterValue = parameterValue;
    		}
	    }
    MutationType mutationType;
    
    // EC Model Type
    enum ECClassType
	    {
	    GA("Genetic Algorithm")
	    	{
	    	String getParamsFileName() { return "simple.params"; }
			String getClassName() { return "ec.simple.SimpleBreeder"; }
	    	},
	    CMAES("CMA-ES")
	    	{
	    	String getParamsFileName() { return "cmaes.params"; }
			String getClassName() { return "ec.eda.cmaes.CMAESBreeder"; }
	    	},
	    ES("(" + MU_UNICODE + ", " + LAMBDA_UNICODE + ") Evolution Strategy")
	    	{
	    	String getParamsFileName() { return "es.params"; }
			String getClassName() { return "ec.es.ESSelection"; }
	    	},
	    NSGA2("NSGA-II")
	    	{
	    	String getParamsFileName() { return "nsga2.params"; }
			String getClassName() { return "ec.multiobjective.nsga2.NSGA2Breeder"; }
	    	},
	    ASYNCHRONOUS("Steady-State GA")
	    	{
	    	String getParamsFileName() { return "steadystate.params"; }
			String getClassName() { return "ec.steadystate.SteadyStateEvolutionState"; }
	    	},
	    	;
	    static ECClassType DEFAULT = GA;
	    abstract String getParamsFileName();
	    abstract String getClassName();
	    String displayName;
	    ECClassType(String displayName)
	    	{
	    	this.displayName = displayName;
	    	}
	    }
    ECClassType ecType = ECClassType.DEFAULT;

    /**
     * Specific display for EC's JComboBox
     */
    class MyComboBoxRenderer extends JLabel implements ListCellRenderer {
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			ECClassType type = (ECClassType) value;
			setText(type.displayName);
			return this;
		}
    }
    
    int numWorkers = 1;
    /** List of worker threads **/
    Thread[] workers;

    JButton run;
    JButton stop;
    JProgressBar progressBar;

    boolean running = false;
    javax.swing.Timer timer;
    
    //-------------------------------------------------

    /**
     * The current index of the topmost element
     */
    int start = 0;
    /**
     * The number of items presently in the propertyList
     */
    int count = 0;
    int currentIndex = 0;
    
    Optimize opt = null;
    
    
    JList<VariableSettings> propList = new JList<VariableSettings>();

    DefaultListModel<VariableSettings> propertySettingsList;

    public OptimizeGUI(sim.util.Properties properties, final GUIState state) 
        {
        this.state = state;
        this.properties = properties;
        
        setLayout(new BorderLayout());

        // GLOBAL AND FOOTER
        JPanel p = new JPanel();
        add(p, BorderLayout.SOUTH);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // RUN AND STOP BUTTONS IN FOOTER
        run = new JButton(sim.display.Console.iconFor("NotPlaying.png"));
        stop = new JButton(sim.display.Console.iconFor("Stopped.png"));
        
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

        // GLOBAL SETTINGS       
        PropertyField numWorkersField = new PropertyField("" + numWorkers)
    		{
        	public String newValue(String value)
	        	{
	        	try
		        	{
	        		numWorkers = Math.max(Integer.parseInt(value), 1);
		        	}
	        	catch (NumberFormatException ex) { }
	        	return "" + numWorkers;
	        	}
    		};
    	
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
        
        PropertyField objectivesField = new PropertyField("" + numObjectives)
	        {
	        public String newValue(String value)
	            {
	            try
	                {
	            	numObjectives = Math.max(Integer.parseInt(value), 1);
	                }
	            catch (NumberFormatException ex) { }
	            return "" + numObjectives;
	            }
	        };
	        
        PropertyField rebuildField = new PropertyField(null, "" + rebuildP, true, null, PropertyField.SHOW_CHECKBOX)
            {
            public String newValue(String value)
                {
            	rebuildP = (value.equals("true"));
                return "" + rebuildP;
                }
            };

        PropertyField popSizeField = new PropertyField("" + popSize)
	        {
	        public String newValue(String value)
	            {
	            try
	                {
	            	popSize = Math.max(Integer.parseInt(value), 1);
	                }
	            catch (NumberFormatException ex) { }
	            return "" + popSize;
	            }
	        };
	        
        PropertyField numGenerationsField = new PropertyField("" + numGenerations)
	        {
	        public String newValue(String value)
	            {
	            try
	                {
	            	numGenerations = Math.max(Integer.parseInt(value), 1);
	                }
	            catch (NumberFormatException ex) { }
	            return "" + numGenerations;
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
            
            
        PropertyField tournamentSizeField = new PropertyField("" + tournamentSize)
	        {
	        public String newValue(String value)
	            {
	            try
	                {
	            	tournamentSize = Math.max(Integer.parseInt(value), 1);
	                }
	            catch (NumberFormatException ex) { }
	            return "" + tournamentSize;
	            }
	        };
	        
	    PropertyField mutationRateField = new PropertyField("" + mutationRate)
	        {
	        public String newValue(String value)
	            {
	            try
	                {
	            	mutationRate = Double.parseDouble(value);
	            	if ((mutationRate < 0.0) || (mutationRate > 1.0)){
	            		mutationRate = 0.5;
	            	}
	                }
	            catch (NumberFormatException ex) { }
	            return "" + mutationRate;
	            }
	        };

		PropertyField mutationStDevField = new PropertyField("" + mutationStDev)
	        {
	        public String newValue(String value)
	            {
	            try
	                {
	            	mutationStDev = Double.parseDouble(value);
	            	
	            	//can't be negative?
	            	if (mutationStDev < 0.0) {
	            		mutationStDev = mutationStDev * -1.0; //should we do this?
	            	}
	        
	                }
	            catch (NumberFormatException ex) { }
	            return "" + mutationStDev;
	            }
	        };
	        
		PropertyField crossoverField = new PropertyField("" + crossover)
	        {
	        public String newValue(String value)
	            {
	            try
	                {
	            	crossover = Double.parseDouble(value);
	            	
	            	if ((crossover < 0.0) || (crossover > 1.0)) {
	            		crossover = 0.5; //or should we throw an exception
	            	}
	        
	                }
	            catch (NumberFormatException ex) { }
	            return "" + crossover;
	            }
	        };

        LabelledList globalSettings = new LabelledList("Global Settings");
        globalSettings.addLabelled("Num Workers", numWorkersField);
        globalSettings.addLabelled("Max Steps", maxStepsField);
        globalSettings.addLabelled("Num Trials", trialsField);
        globalSettings.addLabelled("Rebuild Model", rebuildField);
        globalSettings.addLabelled("Population Size", popSizeField);
        globalSettings.addLabelled("Num Generations", numGenerationsField);
        globalSettings.addLabelled("Compress Output File", compressField);
        p.add(globalSettings, BorderLayout.CENTER);

        Container algBox = Box.createVerticalBox();

        JComboBox ecComboBox = new JComboBox(ECClassType.values());
        ecComboBox.setRenderer(new MyComboBoxRenderer());
        ecComboBox.addActionListener(new ActionListener()
        	{
			public void actionPerformed(ActionEvent e)
				{
				ecType = (ECClassType)ecComboBox.getSelectedItem();
				
				// Setup the specific controls
				algBox.removeAll();
				
		        LabelledList algSettings = new LabelledList("Specific Settings");

				if (ecType.equals(ECClassType.CMAES)) {
					// nothing to do
				}
				else if (ecType.equals(ECClassType.GA)) {
			        algSettings.addLabelled("Tournament Size", tournamentSizeField);
			        algSettings.addLabelled("Mutation Rate", mutationRateField);
			        algSettings.addLabelled("Mutation Standard Deviation", mutationStDevField);
			        algSettings.addLabelled("Crossover Rate", crossoverField);
				}
				else if (ecType.equals(ECClassType.ES)) {
			        algSettings.addLabelled("Mutation Rate", mutationRateField);
			        algSettings.addLabelled("Mutation Standard Deviation", mutationStDevField);
			        algSettings.addLabelled("Crossover Rate", crossoverField);
			        algSettings.addLabelled(MU_UNICODE, new PropertyField("" + mu)
			        	{
				        public String newValue(String value)
				            {
				            try
				                {
				            	mu = Integer.parseInt(value);
				            	if (mu < 1)
				            		mu = 1;
				                }
				            catch (NumberFormatException ex) { }
				            return "" + mu;
				            }
				        });
				}
				else if (ecType.equals(ECClassType.NSGA2)) {
			        algSettings.addLabelled("Num Objectives", objectivesField);
			        algSettings.addLabelled("Mutation Rate", mutationRateField);
			        algSettings.addLabelled("Crossover", crossoverField);

			        // === Mutation Type === //
			        Box mutTypeBox = Box.createHorizontalBox();
			        mutTypeBox.add(new JLabel("Mutation Type"));
			        JComboBox mutTypeCB = new JComboBox(MutationType.values());
			        mutTypeCB.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							mutationType = (MutationType)mutTypeCB.getSelectedItem();
						}
			        });
			        
			        Box mutSettingsBox = Box.createHorizontalBox();			        
			        
			        mutTypeCB.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							mutSettingsBox.removeAll();
							
							MutationType mutType = (MutationType) mutTypeCB.getSelectedItem();
							LabelledList list = new LabelledList();

							// Polynomial
							if (mutType.equals(MutationType.POLYNOMIAL)) {
								list.addLabelled("Distribution Index", new PropertyField("" + distributionIndex)
						        {
						        public String newValue(String value)
						            {
						            try
						                {
						            	distributionIndex = Double.parseDouble(value);
						            	
						            	if (distributionIndex < 0.0) {
						            		distributionIndex = distributionIndex * -1.0; //should we do this?
						            	}
						        
						                }
						            catch (NumberFormatException ex) { }
						            return "" + distributionIndex;
						            }
						        });
							}
							// Gaussian
							else if (mutType.equals(MutationType.GAUSSIAN)) {
								list.addLabelled("Mutation Standard Deviation", mutationStDevField);
							}
							mutSettingsBox.add(list);
							
							mutSettingsBox.revalidate();
							mutSettingsBox.repaint();
						}
			        });
			        mutTypeCB.setSelectedItem(MutationType.DEFAULT);
			        mutTypeBox.add(mutTypeCB);
			        
			        algSettings.add(mutTypeBox);
			        algSettings.add(mutSettingsBox);
				}
				else if (ecType.equals(ECClassType.ASYNCHRONOUS)) {
			        algSettings.addLabelled("Tournament Size", tournamentSizeField);
			        algSettings.addLabelled("Mutation Rate", mutationRateField);
			        algSettings.addLabelled("Mutation Standard Deviation", mutationStDevField);
			        algSettings.addLabelled("Crossover Rate", crossoverField);
			        algSettings.addLabelled("Replacement Probability", new PropertyField("" + replacementProb)
			        	{
				        public String newValue(String value)
				            {
				            try
				                {
				            	replacementProb = Double.parseDouble(value);
				            	
				            	//can't be negative?
				            	if (replacementProb < 0.0) {
				            		replacementProb = replacementProb * -1.0; //should we do this?
				            	}
				            	else if (replacementProb > 1.0) {
				            		replacementProb = 1.0;
				            	}
				        
				                }
				            catch (NumberFormatException ex) { }
				            return "" + replacementProb;
				            }
				        });
				}
				else {
					throw new UnsupportedOperationException();
				}
				
				algBox.add(algSettings);
				algBox.revalidate();
				algBox.repaint();
				}
        	});
        ecComboBox.setSelectedItem(ecType);
        
        p.add(ecComboBox);
        p.add(algBox, BorderLayout.CENTER);

        Box footerBox = new Box(BoxLayout.X_AXIS);
        footerBox.add(run);
        footerBox.add(stop);
        JPanel footer = new JPanel();
        footer.setLayout(new BorderLayout());
        footer.add(footerBox, BorderLayout.WEST);
        footer.add(progressPanel, BorderLayout.CENTER);
        p.add(footer, BorderLayout.SOUTH);
        
        // Add Property Panel
        JPanel propertyPanel = new JPanel();
        propertyPanel.setLayout(new BorderLayout());
        
        // Add property list
        propertySettingsList = new DefaultListModel<VariableSettings>();
        for (int i = 0; i < properties.numProperties(); i++) 
            {
            if(!properties.isHidden(i) && (properties.getType(i)==Integer.TYPE
                    ||properties.getType(i)==Double.TYPE || properties.getType(i)==Float.TYPE
                    || properties.getType(i)==Boolean.TYPE || properties.getType(i)==Long.TYPE))
                propertySettingsList.addElement(new VariableSettings(properties, i));
            }
        propList.setModel(propertySettingsList);
        propList.setCellRenderer(new ListCellRenderer<VariableSettings>()
        	{
			public Component getListCellRendererComponent(JList<? extends VariableSettings> list,
					VariableSettings value, int index, boolean isSelected, boolean cellHasFocus)
				{
				JPanel row = new JPanel();
				row.setLayout(new BorderLayout());
				JLabel nameLabel = new JLabel(value.getName());
				JCheckBox checkBox = new JCheckBox();
				checkBox.setSelected(value.amSet);
				
				Box textBox = Box.createHorizontalBox();
				textBox.add(nameLabel);
				textBox.add(new JLabel(value.getSettings()));
				
				row.add(textBox, BorderLayout.WEST);
				row.add(checkBox, BorderLayout.EAST);
				
				// Show which cell we're currently focused on
				row.setOpaque(true);
				if (isSelected)
					{
		            row.setBackground(list.getSelectionBackground());
		            row.setForeground(list.getSelectionForeground());
					}
				else
					{
		            row.setBackground(list.getBackground());
		            row.setForeground(list.getForeground());
					}
				
				return row;
				}
        	});
        
        propList.addMouseListener(new MouseListener()
        	{
			public void mousePressed(MouseEvent e)
				{
				VariableSettings settings = propList.getSelectedValue();
				settings.amSet = !settings.amSet;
				if (settings.amSet)
					{
					minField.setEnabled(true);
					maxField.setEnabled(true);
					}
				else
					{
					minField.setEnabled(false);
					maxField.setEnabled(false);
					}
				propList.repaint();
				}

			public void mouseClicked(MouseEvent e) { }
			public void mouseReleased(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }
        	});
        
        propList.setVisibleRowCount(10);
        propList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        Box varRangeRegion = Box.createHorizontalBox();
        minField = new PropertyField(null, "" + minValue)
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
                    VariableSettings settings = propList.getSelectedValue();
                    settings.min = minValue;
                    varRangeRegion.revalidate();
                    varRangeRegion.repaint();
                    updateParameterSettings();
                    }
                }
            catch (NumberFormatException ex) { }
            return "" + minValue;
            }
        };
        maxField = new PropertyField(null, "" + maxValue)
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
                    VariableSettings settings = propList.getSelectedValue();
                    settings.max = maxValue;
                    varRangeRegion.revalidate();
                    varRangeRegion.repaint();
                    updateParameterSettings();
                    }
                }
            catch (NumberFormatException ex) { }
            return "" + maxValue;
            }
        };
        minField.setEnabled(false);
        maxField.setEnabled(false);
        JButton resetParams = new JButton("Reset Params");
        resetParams.addActionListener(new ActionListener() 
            {
            public void actionPerformed(ActionEvent e) 
                {
                for (int i = 0; i < propList.getModel().getSize(); i++) 
                    {
                    VariableSettings settings = propList.getModel().getElementAt(i);
                    settings.min = DEFAULT_MIN;
                    settings.max = DEFAULT_MAX;
                    settings.amSet = false;                    
                    }
                propList.clearSelection();
                updateParameterSettings();
                }
            });
        
        propList.addListSelectionListener(new ListSelectionListener()
        	{
			public void valueChanged(ListSelectionEvent e)
				{
				VariableSettings settings = propList.getSelectedValue();
				if (settings == null)
					return;
				minValue = settings.min;
				maxValue = settings.max;
				minField.setValue(""+minValue);
				maxField.setValue(""+maxValue);
				varRangeRegion.revalidate();
				varRangeRegion.repaint();
				}
        	});
//        propList.setSelectedIndex(0);
        
        LabelledList minList = new LabelledList();
        LabelledList maxList = new LabelledList();
        minList.addLabelled("min", minField);
        maxList.addLabelled("max", maxField);
        varRangeRegion.add(minList);
        varRangeRegion.add(maxList);
        varRangeRegion.add(resetParams);
        propertyPanel.add(varRangeRegion);
        
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createTitledBorder("Optimization Variables"));
        header.add(propertyPanel, BorderLayout.SOUTH);
        header.add(new JScrollPane(propList), BorderLayout.CENTER);             
        add(header, BorderLayout.CENTER);
        }

    /**
     * Refresh JList after changes to its components
     */
    void updateParameterSettings()
	    {
        propList.repaint();
	    }
    
    /**
     * Validate the Configuration
     * @return a string representation of the invalid reason;
     * 		otherwise, <code>null</code> if no error
     */
    public String isValidConfiguration() 
        {
        String str = "";
        
        // Correct # objectives?
        int numOptVars = getNumSelectedObjectives();
        if (numOptVars == 0)
	        {
	        str += "Must select at least one model variable to optimize over." + "\n";
	        }
        else if (numOptVars > 1
        		&& !ecType.equals(ECClassType.NSGA2))
    		{
        	str += "Must use " + ECClassType.NSGA2.displayName + " for multiobjective optimization." + "\n";
    		}
        
        // ES constraints
        if (ecType.equals(ECClassType.ES))
        	{
        	if (popSize % mu != 0)
        		{
        		str += LAMBDA_UNICODE + "(population size) must be a multiple of " + MU_UNICODE + "." + "\n";
        		}
        	}
        // NSGA2 constraints
        else if (ecType.equals(ECClassType.NSGA2))
        	{
        	if (popSize % 2 != 0)
        		{
        		str += "Population size must be even." + "\n";
        		}
        	}
        	
        if (str.isEmpty())
        	return null;
        return str;
        }

    /**
     * Determine the number of chosen optimization variables from the GUI
     * @return
     */
    int getNumSelectedObjectives()
    	{
    	int counter = 0;
    	for (int i = 0; i < propList.getModel().getSize(); i++)
    		{
        	VariableSettings settings = propList.getModel().getElementAt(i);
        	if (settings.amSet)
        		{
        		counter++;
        		}
    		}
    	return counter;
    	}
    
    /**
     * Sets up the parameter database according to the gui and executes Optimize
     */
    void run()
        {
    	// If already running, abort
    	if (running)
    		return;
    	
    	String ans = isValidConfiguration();
        if (ans != null)
            {
            JOptionPane.showMessageDialog(null, ans);
            return;
            }
        else
            {
            String filePath = setFilePath(); 
            if (filePath == null) { return; }
            
            try
                {
            	ParameterDatabase pd = createParameterDatabase(filePath); //probably pass class selection parameters here
            	String classname = pd.getString(new ec.util.Parameter("mason-class"), null);
                Class c = Class.forName(classname);
                opt = new Optimize(Optimize.buildSimState(c), pd);
                }
            catch (ClassNotFoundException ex)
                {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Invalid class " + state.state);
                return;
                }
            catch (Exception e)
            	{
                e.printStackTrace();
                return;
            	}
            
            // Worker Threads
            workers = new Thread[numWorkers];
            for (int i = 0; i < numWorkers; i++)
	            {
            	Thread worker = new Thread(new Runnable()
            	{
					public void run()
						{
						try
							{
//		            		ec.eval.Slave.main(new String[] { "-from", "mason.worker.params", "-at", "sim.util.opt.Worker" });
		            		ec.eval.Slave.main(new String[] { "-from", "mason.worker.params", "-at", "sim.util.opt.Worker", "-p", "eval.slave.one-shot=true" });
							}
						// If Slave throws an Interrupted Exception
						catch (Exception ex)
							{
//							System.out.println("caught " + ex);
							}
						}
            	});
            	worker.start();
            	workers[i] = worker;
	            }
            
            // Optimize Thread
            new Thread(new Runnable()
                {
                public void run() 
                    {
                    running = true;
                    run.setIcon(sim.display.Console.iconFor("Playing.png"));
                    stop.setIcon(sim.display.Console.iconFor("NotStopped.png"));
                    try 
                        {
                        timer = new javax.swing.Timer(1000, new ActionListener() 
                            {
                            public void actionPerformed(ActionEvent e)
                                {
                                if (running)
                                    {
                                    progressBar.setMaximum(opt.evolutionState.numGenerations);
                                    progressBar.setMinimum(0);
                                    progressBar.setValue(opt.evolutionState.generation);
                                    }
                                }
                            });
                        timer.start();
                        opt.run();
                        }
                    catch(Exception e)
                        {
                        e.printStackTrace();
                        }
                    stop();
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
              
    /**
     * Stops optimizer and kills any <i>local</i> workers
     */
    void stop()
        {
    	if (!running)
	    	return;
    	    	
        // request a stop
        if (opt!=null) 
            {
        	if (opt.evolutionState != null)
        		{
        		try
        			{
        			opt.finish();
        			}
        		catch (Exception e)
        			{
        			// throws NPE if first gen hasn't completed. catch silently
        			}
        		}
        	
        	// TODO This correct?
        	// Kill any worker threads
        	for (int i = 0; i < workers.length; i++)
	        	{
	        	workers[i].interrupt();
	        	}
            }
        
        running = false;
        progressBar.setValue(0);
    	run.setIcon(sim.display.Console.iconFor("NotPlaying.png"));
        stop.setIcon(sim.display.Console.iconFor("Stopped.png"));
        }

    /**
     * Set Output File
     * @return the output filepath
     */
    String setFilePath()
    	{
        String filePath = "";
        FileDialog dialog = new FileDialog((Frame)null, "Save Results to File...", FileDialog.SAVE);
        dialog.setFilenameFilter(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                return !name.endsWith(".stat");
                }
            });
            
        dialog.setVisible(true);
        String filename = dialog.getFile();
        if (filename == null)
            return null;
        filename = filename + ".stat";
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

    /**
     * Creates the PD for the current GUI state
     * @param filePath
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    ParameterDatabase createParameterDatabase(String filePath) throws FileNotFoundException, IOException, ClassNotFoundException
        {
	    ParameterDatabase pd = new ParameterDatabase();
	    
        // load parents
        String classname = ecType.getClassName();
        String fname = ecType.getParamsFileName();
        pd.addParent(new ParameterDatabase(fname, Class.forName(classname, true, Thread.currentThread().getContextClassLoader())));

        // Silence ecj's stdout and stderr
    	pd.set(new Parameter("silent"), ""+true);
    	pd.set(new Parameter("eval.slave.silent"), ""+true);
        
        // ============================ //
        // === Overwrite properties === //
        pd.set(new Parameter("mason-class"), state.state.getClass().getName());
        
        // "parent.0 = @sim.util.opt.Worker mason.master.params"
        classname = "sim.util.opt.Worker";
        fname = "mason.master.params";
        try
            {
        	pd.addParent(new ParameterDatabase(fname, Class.forName(classname, true, Thread.currentThread().getContextClassLoader())));
            }
        catch (ClassNotFoundException ex)
            {
            throw new FileNotFoundException("Could not parse file into filename and classname");
            }

    	// Out file
    	pd.set(new Parameter("stat.file"), filePath);
    	// Don't print to screen
    	pd.set(new Parameter("stat.silent.print"), ""+true);
        
        // Global Settings
        pd.set(new Parameter("mason-steps"), ""+numMaxSteps);
    	pd.set(new Parameter("mason-num-trials"), ""+numTrials);
        pd.set(new Parameter("mason-objectives"), ""+numObjectives);
    	pd.set(new Parameter("mason-rebuild-model"), ""+rebuildP);
    	pd.set(new Parameter("pop.subpop.0.size"), ""+popSize);
    	pd.set(new Parameter("generations"), ""+numGenerations);
    	if (compressOutput)
        	pd.set(new Parameter("stat.gzip"), "true");
    	
        // Multiobjective
    	pd.set(new Parameter("multi.fitness.max"), "200");
    	pd.set(new Parameter("multi.fitness.min"), "0");
    	pd.set(new Parameter("stat.do-hypervolume"), "false");
    	
        // Selected Property(/ies)
    	int counter = 0;
    	for (int i = 0; i < propList.getModel().getSize(); i++)
    		{
        	VariableSettings settings = propList.getModel().getElementAt(i);
        	if (settings.amSet)
        		{
        		pd.set(new Parameter("mason-property." + counter), settings.getName());
        		counter++;
        		}
    		}
    	int numMasonProperties = counter;
    	if (numMasonProperties > 1)
    		{
    		assert(ecType.equals(ECClassType.NSGA2));
    		}
		pd.set(new Parameter("num-mason-properties"), ""+numMasonProperties);
        
        // Specific to EC type
	    if (ecType.equals(ECClassType.GA))
	    	{
            //GA based on pg 93 and 94 of ecj manual
	    	pd.set(new Parameter("pop.subpop.0.species.pipe"), "ec.vector.breed.VectorMutationPipeline");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0"), "ec.vector.breed.VectorCrossoverPipeline");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0.source.0"), "ec.select.TournamentSelection");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0.source.1"), "ec.select.TournamentSelection");
	    	pd.set(new Parameter("select.tournament.size"), ""+tournamentSize);
            
	    	pd.set(new Parameter("pop.subpop.0.species"), "ec.vector.FloatVectorSpecies");
	    	pd.set(new Parameter("pop.subpop.0.species.crossover-type"), "any");
	    	pd.set(new Parameter("pop.subpop.0.species.crossover-prob"), ""+crossover);
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-prob"),  ""+mutationRate);
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-type"), "gauss");
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-stdev"),  ""+mutationStDev);
	    	}
	    else if (ecType.equals(ECClassType.ES))
	    	{
	    	pd.set(new Parameter("es.mu.0"), ""+mu);
	    	pd.set(new Parameter("es.lambda.0"), ""+popSize);

	    	pd.set(new Parameter("pop.subpop.0.species.pipe"), "ec.vector.breed.VectorMutationPipeline");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0"), "ec.vector.breed.VectorCrossoverPipeline");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0.source.0"), "ec.es.ESSelection");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0.source.1"), "ec.select.TournamentSelection");
            
	    	pd.set(new Parameter("pop.subpop.0.species"), "ec.vector.FloatVectorSpecies");
	    	pd.set(new Parameter("pop.subpop.0.species.crossover-type"), "any");
	    	pd.set(new Parameter("pop.subpop.0.species.crossover-prob"), ""+crossover);
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-prob"),  ""+mutationRate);
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-type"), "gauss");
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-stdev"),  ""+mutationStDev);
	    	}
	    else if (ecType.equals(ECClassType.NSGA2))
		    {
	    	pd.set(new Parameter("pop.subpop.0.species.crossover-type"), "sbx");
	    	pd.set(new Parameter("pop.subpop.0.species.crossover-distribution-index"), "20");
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-prob"),  ""+mutationRate);
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-type"), mutationType.parameterValue);
	    	pd.set(new Parameter("pop.subpop.0.species.alternative-polynomial-version"), "true");
//	    	pd.set(new Parameter("pop.subpop.0.species.mutation-distribution-index"), "20");
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-bounded"), "true");
	    	pd.set(new Parameter("stat.reference-point"), "-1 -1 -1 -1 -1 -1");
	    	
	    	// Polynomial Distribution
	    	if (mutationType.equals(MutationType.POLYNOMIAL))
	    		{
		    	pd.set(new Parameter("pop.subpop.0.species.mutation-distribution-index"), ""+distributionIndex);
//		    	pd.set(new Parameter("pop.subpop.0.species.crossover-distribution-index"),  ""+distributionIndex);
	    		}
	    	// Gaussian Distribution
	    	else if (mutationType.equals(MutationType.GAUSSIAN))
	    		{
		    	pd.set(new Parameter("pop.subpop.0.species.mutation-stdev"),  ""+mutationStDev);
	    		}
		    }
	    else if (ecType.equals(ECClassType.CMAES))
	    	{
	    	// nothing to do
	    	}
	    else if (ecType.equals(ECClassType.ASYNCHRONOUS))
	    	{
	    	pd.set(new Parameter("pop.subpop.0.species.pipe"), "ec.vector.breed.VectorMutationPipeline");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0"), "ec.vector.breed.VectorCrossoverPipeline");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0.source.0"), "ec.select.TournamentSelection");
	    	pd.set(new Parameter("pop.subpop.0.species.pipe.source.0.source.1"), "ec.select.TournamentSelection");
	    	pd.set(new Parameter("select.tournament.size"), ""+tournamentSize);
            
	    	pd.set(new Parameter("pop.subpop.0.species"), "ec.vector.FloatVectorSpecies");
	    	pd.set(new Parameter("pop.subpop.0.species.crossover-type"), "any");
	    	pd.set(new Parameter("pop.subpop.0.species.crossover-prob"), ""+crossover);
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-prob"),  ""+mutationRate);
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-type"), "gauss");
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-stdev"),  ""+mutationStDev);
	    	
	    	pd.set(new Parameter("steady.replacement-probability"), ""+replacementProb);
	    	}
	    
//	    pd.set(new Parameter("min"), "0.1");// 0.9");
//	    pd.set(new Parameter("max"), "0.9");// 1.0");
	    
//	    pd.set(new Parameter("mason-property.0.min"), "0.1");
//	    pd.set(new Parameter("mason-property.0.max"), "0.9");
	    
//	    pd.set(new Parameter("silent"), "true");
	    
        return pd;
        }
    }









 