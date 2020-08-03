/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.opt;
import java.awt.*;

import ec.Evolve;
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
import java.util.List;
import java.util.function.Function;
import java.time.*;

import sim.util.sweep.*;


public class OptimizeGUI extends JPanel
    {
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
    double minValue = 0;
    double maxValue = 1;
    int divisionValue = 1;
    // GA
    int tournamentSize = 2;
    double mutationRate = 1.0;
    double mutationStDev = 0.1;
    double distributionIndex = 21;
    double crossover = 0.5;
    // ES
    int mu = 10;
//    int lambda = 100;
    // NSGA2
//    String[] mutationTypes = {"Polynomial", "Gaussian"}; //{"polynomial", "gauss"};
    enum MutationType
	    {
	    POLYNOMIAL("polynomial"),
	    GAUSSIAN("gauss")
	    ;
	    static MutationType DEFAULT = POLYNOMIAL;
    	String parameterValue;
    	MutationType(String parameterValue) {
    		this.parameterValue = parameterValue;
    	}
	    }
    MutationType mutationType;
    
    // EC Model Type
    enum ECClassType
	    {
	    GA("GA")
	    	{
	    	String getParamsFileName() { return "simple.params"; }
			String getClassName() { return "ec.simple.SimpleBreeder"; }
	    	},
	    CMAES("CMA-ES")
	    	{
	    	String getParamsFileName() { return "cmaes.params"; }
			String getClassName() { return "ec.eda.cmaes.CMAESBreeder"; }
	    	},
	    ES("ES")
	    	{
	    	String getParamsFileName() { return "es.params"; }
			String getClassName() { return "ec.es.ESSelection"; }
	    	},
	    NSGA2("NSGA-II")
	    	{
	    	String getParamsFileName() { return "nsga2.params"; }
			String getClassName() { return "ec.multiobjective.nsga2.NSGA2Breeder"; }
	    	},
	    ASYNCHRONOUS("Steady State GA")
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
    JList<ParameterSettings> propList = new JList<ParameterSettings>();;

    DefaultListModel<ParameterSettings> propertySettingsList;

    public OptimizeGUI(sim.util.Properties properties, final GUIState state) 
        {
        this.state = state;
        this.properties = properties;
        
        setLayout(new BorderLayout());

        ///// GLOBAL AND FOOTER
        JPanel p = new JPanel();
        add(p, BorderLayout.SOUTH);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

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

        // GLOBAL SETTINGS       
        PropertyField numWorkersField = new PropertyField("" + numWorkers)
    		{
        	public String newValue(String value)
	        	{
	        	try
		        	{
	        		//TODO max = # cpus?
	        		//TODO default = 1?
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
//        globalSettings.addLabelled("Num Objectives", objectivesField);
        globalSettings.addLabelled("Rebuild Model", rebuildField);
        globalSettings.addLabelled("Population Size", popSizeField);
        globalSettings.addLabelled("Num Generations", numGenerationsField);
        globalSettings.addLabelled("Compress Output File", compressField);
        p.add(globalSettings, BorderLayout.CENTER);

        Container algBox = Box.createVerticalBox();
//        Arrays.stream(ECClassType.values()).map(new Function<ECClassType,String>(){
//			public String apply(ECClassType ecType) {
//				return ecType.displayName;
//			}
//        });
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
				else if (ecType.equals(ECClassType.ASYNCHRONOUS)) {
					// TODO ? 
				}
				else if (ecType.equals(ECClassType.GA)) {
			        algSettings.addLabelled("Tournament Size", tournamentSizeField);
			        algSettings.addLabelled("Mutation Rate", mutationRateField);
			        algSettings.addLabelled("Mutation Standard Deviation", mutationStDevField);
			        algSettings.addLabelled("Crossover Rate", crossoverField);
				}
				else if (ecType.equals(ECClassType.ES)) {
			        algSettings.addLabelled("Tournament Size", tournamentSizeField);
			        algSettings.addLabelled("Mutation Rate", mutationRateField);
			        algSettings.addLabelled("Mutation Standard Deviation", mutationStDevField);
			        algSettings.addLabelled("Crossover Rate", crossoverField);
			        algSettings.addLabelled("Mu", new PropertyField("" + mu)
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
//			        algSettings.addLabelled("Lambda", new PropertyField("" + lambda)
//			        	{
//				        public String newValue(String value)
//				            {
//				            try
//				                {
//				            	lambda = Integer.parseInt(value);
//				            	if (lambda < 0)
//				            		lambda = 1;
//				                }
//				            catch (NumberFormatException ex) { }
//				            return "" + lambda;
//				            }
//				        });
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
        propList.setSelectedIndex(0);
        
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createTitledBorder("Optimization Variables"));
        header.add(propertyPanel, BorderLayout.SOUTH);
        header.add(new JScrollPane(propList), BorderLayout.CENTER);             
        add(header, BorderLayout.CENTER);
        }

    /**
     * Validate the Configuration
     * @return a string representation of the invalid reason; otherwise, <code>null</code> if no error
     */
    public String isValidConfiguration() 
        {
        String str = "";
        	
        if (ecType.equals(ECClassType.ES))
        	{
        	if (popSize % mu != 0)
        		{
        		return "lambda must be a multiple of mu";
        		}
        	}
        else if (ecType.equals(ECClassType.NSGA2))
        	{
        	if (popSize % 2 != 0)
        		{
        		return "population size must be even";
        		}
        	}
        	
        if (str.isEmpty())
        	return null;
        return str;
        }

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
            String filePath = getFilePath(); 
            if (filePath == null) { return; }
            
            try
                {
            	ParameterDatabase pd = createParameterDatabase(filePath); //probably pass class selection parameters here
            	String classname = pd.getString(new ec.util.Parameter("mason-class"), null);
                Class c = Class.forName(classname);
                opt = new Optimize(Optimize.buildSimState(c), pd);
                System.out.println("hi");
                }
            catch (ClassNotFoundException ex)
                {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Invalid class " + state.state);
                return;
                }
            catch (Exception e)
            	{
                //For now , do nothing probably will remove this section
                e.printStackTrace();
                return;
            	}
            
            // Worker Threads
            workers = new Thread[numWorkers];
            for (int i = 0; i < numWorkers; i++)
	            {
            	Thread worker = new Thread(new Runnable() {
					public void run()
						{
//						try
//							{
			            	ec.eval.Slave.main(new String[] { "-from", "mason.worker.params", "-at", "sim.util.opt.Worker" });
//							}
						//TODO Slave needs to throw an Interrupted Exception?
//						catch (Exception ex) { System.out.println("caught " + ex.getCause()); }
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
//                                    System.err.println("progress: " + opt.evolutionState.generation);
                           
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
               
    //TODO
    void stop()
        {
    	System.err.println("stop() not working atm");
//        // request a stop
//        if (opt!=null) 
//            {
////        	if (opt.evolutionState != null)
////        		opt.finish();	// <- throws NPE
//        	
//        	// Kill any worker threads
//        	for (int i = 0; i < workers.length; i++)
//	        	{
//	        	workers[i].interrupt();
//	        	}
//            }
        }

    String getFilePath() {
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

    ParameterDatabase createParameterDatabase(String filePath) throws FileNotFoundException, IOException , ClassNotFoundException
        {
	    ParameterDatabase pd = new ParameterDatabase();
	    
        // load parents
        String classname = ecType.getClassName();
        String fname = ecType.getParamsFileName();
        pd.addParent(new ParameterDatabase(fname, Class.forName(classname, true, Thread.currentThread().getContextClassLoader())));

        //TODO not working
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
    	
        // Multiobjective
    	pd.set(new Parameter("multi.fitness.max"), "200");
    	pd.set(new Parameter("multi.fitness.min"), "0");
    	pd.set(new Parameter("stat.do-hypervolume"), "false");
    	
        // Mason Properties
        List propValues = propList.getSelectedValuesList();
        pd.set(new Parameter("num-mason-properties"), ""+propValues.size());
        for (int i = 0; i < propValues.size(); i++)
            {
            ParameterSettings ps = (ParameterSettings) propValues.get(i);
            pd.set(new Parameter("mason-property." + i), ps.getName());
            }
        
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
	    	pd.set(new Parameter("select.tournament.size"), ""+tournamentSize);
            
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
//	    	pd.set(new Parameter("pop.subpop.0.species.crossover-distribution-index"), "20");
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-prob"),  ""+mutationRate);
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-type"), mutationType.parameterValue);
	    	pd.set(new Parameter("pop.subpop.0.species.alternative-polynomial-version"), "true");
//	    	pd.set(new Parameter("pop.subpop.0.species.mutation-distribution-index"), "20");
	    	pd.set(new Parameter("pop.subpop.0.species.mutation-bounded"), "true");
	    	pd.set(new Parameter("stat.reference-point"), "-1 -1 -1 -1 -1 -1");
	    	
	    	// Polynomial Distribution
	    	if (mutationType.equals(MutationType.POLYNOMIAL)) {
		    	pd.set(new Parameter("pop.subpop.0.species.mutation-distribution-index"), ""+distributionIndex);
//		    	pd.set(new Parameter("pop.subpop.0.species.crossover-distribution-index"),  ""+distributionIndex);
	    	}
	    	// Gaussian Distribution
	    	else if (mutationType.equals(MutationType.GAUSSIAN)) {
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
	    	}
        return pd;
        }
    }













 