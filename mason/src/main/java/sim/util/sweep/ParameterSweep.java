/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.sweep;

import sim.engine.*;

import java.io.*;
import java.util.*;
import sim.util.*;
import ec.util.*;
import java.util.zip.GZIPOutputStream;

public class ParameterSweep 
    {
    public static final String GZIP_POSTFIX = ".gz";
    
    // Output stream to dump resuts 
    PrintWriter printWriter;
    
    // Independent Variables
    String indNames[];
    double indMinValues[];
    double indMaxValues[];
    int indIndexes[];   // index into a Properties where the variable is stored
    int indDivisions[];
    
    // Dependent Variables
    String depNames[];
    int depIndexes[];

    // Other Stuff
    Class modelClass;
    int numTrials = 1;
    int numThreads = 1;
    int numSteps;
    int mod;
    long baseSeed = 100;
    
    // This is an arraylist of arraylists of doubles, recursively generated, for each combination of values of our independent variables
    ArrayList<ArrayList<Double>> allIndependentVariableValueCombinations = new ArrayList<ArrayList<Double>>();
    
    public ParameterSweep(ParameterDatabase db) throws ClassNotFoundException
        {
        // Load class
        String modelPath = ((String)(db.getStringWithDefault(new Parameter(ParameterSettings.MODEL_P), null, ""))).replace("/",".");
        if (modelPath == null) throw new RuntimeException("No valid model provided.");
        try { modelClass = Class.forName(modelPath); }
        catch (Exception ex) { throw new RuntimeException("No valid model provided."); }

        // Load independent vars
        indNames = ((String)(db.getStringWithDefault(new Parameter(ParameterSettings.INDEPENDENT_P), null, ""))).split("\\s");
        indMinValues = db.getDoublesUnconstrained(new Parameter(ParameterSettings.MIN_P), null, indNames.length);
        indMaxValues = db.getDoublesUnconstrained(new Parameter(ParameterSettings.MAX_P), null, indNames.length);
        double[] d = db.getDoubles(new Parameter(ParameterSettings.DIVISIONS_P), null, 1, indNames.length);
        if (indNames.length == 0) throw new RuntimeException("must have at least one independent variable");
        if (indMinValues == null) throw new RuntimeException("min is invalid or not the same length as independent");
        if (indMaxValues == null) throw new RuntimeException("max is invalid or not the same length as independent");
        if (d == null) throw new RuntimeException("divisions is invalid, less than 1, or not the same length as independent");
        indDivisions = new int[d.length];
        for(int i = 0; i < d.length; i++)
            {
            indDivisions[i] = (int)d[i];
            if (indDivisions[i] != d[i]) throw new RuntimeException("division #" + (i + 1) + " is not an integer.");
            }
        
        // Load dependent vars
        depNames = ((String)(db.getStringWithDefault(new Parameter(ParameterSettings.DEPENDENT_P), null, ""))).split("\\s");
        if (depNames.length == 0) throw new RuntimeException("Must have at least one dependent variable");
        mod = db.getInt(new Parameter(ParameterSettings.MOD_P), null, 0);
        if (mod < 0) throw new RuntimeException("Invalid mod value.  You have: " + mod);

        // Load other parameters
        numSteps = db.getInt(new Parameter(ParameterSettings.STEPS_P), null, 0);
        if (numSteps < 0) throw new RuntimeException("Invalid steps value.  You have: " + numSteps);
        numTrials = db.getInt(new Parameter(ParameterSettings.TRIALS_P), null, 1);
        if (numTrials < 1) throw new RuntimeException("Trials must be at least 1.  You have: " + numTrials);
        numThreads = db.getInt(new Parameter(ParameterSettings.THREADS_P), null, 1);
        if (numThreads < 1) throw new RuntimeException("Threads must be at least 1.  You have: " + numThreads);
        baseSeed = db.getLong(new Parameter(ParameterSettings.SEED_P), null, 1);
        if (baseSeed < 1) throw new RuntimeException("Seed must be at least 1.  You have: " + baseSeed);

        try
            {
            String filename = db.getStringWithDefault(new Parameter(ParameterSettings.OUT_P), null, "");
            if (db.getBoolean(new Parameter(ParameterSettings.COMPRESS_P), null, false))
                {
                printWriter = new PrintWriter(new GZIPOutputStream(new FileOutputStream(filename + GZIP_POSTFIX)), true);
                }
            else 
                {   
                printWriter = new PrintWriter(new FileOutputStream(filename), true);
                }
            }
        catch (IOException e)
            {
            throw new RuntimeException("Could not open file.", e);
            }
                
        SimState simState = newInstance(baseSeed, modelClass);
        sim.util.Properties properties = sim.util.Properties.getProperties(simState);
        initializeIndexes(properties);
        }
                    
    public static void main(String[] args) throws  IOException, ClassNotFoundException
        {
        try
            {
            ParameterSweep sweep = new ParameterSweep(new ParameterDatabase(new File(new File(args[0]).getAbsolutePath()), args));
            sweep.run();
            }
        catch (Exception e)
            {
            System.err.println("Could not run a parameter sweep.\n\nMESSAGE: " + e);
            System.err.println("Format:   java sim.util.sweep.ParameterSweep [parameter file]");
            }
        }  
  
    Object[] printWriterLock = new Object[0];
    // the only way we print results: one line at a time 
    void println(String val) { synchronized(printWriterLock) { printWriter.println(val); } }

    Object[] lock = new Object[0];
    public void printSynchronized(String str)
        {
        synchronized(lock)
            {
            System.err.println(str);
            }
        }
    
        
    boolean running;
    boolean stop;
    Object runningLock = new Object[0];
    Thread outer;
    public void stop()
        {
        synchronized(runningLock)
            {
            if (!running) return;  // already stopped
            stop = true;
            }
        waitUntilStopped();
        }
                
    public void waitUntilStopped()
        {
        try { outer.join();     }               // wait for thread manager to die.  Must be outside runnningLock
        catch (InterruptedException ex) { } // does not happen
        }
                
    // returns true if we're already running
    public void run() 
        {
        synchronized(runningLock)
            {
            if (running) return;    // already running
        
            generateAllIndependentVariableValueCombinations(new ArrayList<Double>());
                        
            writeFileHeader(); 
            running = true;
            stop = false;
            outer = new Thread(new Runnable()
                {
                public void run()
                    {
                    Thread[] threads = new Thread[numThreads];
                    for(int i = 0; i < threads.length; i++) 
                        {
                        threads[i] = new Thread(new Runnable()
                            {
                            public void run()
                                {
                                SimState simState = null;
                                sim.util.Properties properties = null;
                                ParameterSweepSimulationJob job = null;
                                while ((job = (ParameterSweepSimulationJob)getNextJob()) != null) 
                                    {
                                    if (stop) 
                                        {
                                        break;
                                        }

                                    // initialize simstate and properties
                                    if (simState == null)
                                        {
                                        simState = newInstance(job.jobNumber + baseSeed, modelClass);
                                        properties = sim.util.Properties.getProperties(simState);
                                        }
                                    else
                                        {
                                        simState.setSeed(job.jobNumber + baseSeed);
                                        } 

                                    int combination = job.jobNumber / numTrials;  // which variable combination are we doing this time?
                                    job.run(simState, properties, allIndependentVariableValueCombinations.get(combination));
                                    }
                                }
                            });
                        threads[i].start();
                        }
                        
                    for(int i = 0; i<threads.length; i++) 
                        {
                        try 
                            {
                            threads[i].join();
                            }
                        catch(InterruptedException e)
                            {
                            // doesn't happen
                            }
                        }
                    synchronized(printWriterLock) { printWriter.close(); }
                    synchronized(runningLock) { running = false; }
                    }
                });
            outer.start();
            }
        }
    
    
    // Populate all permutations of settings. Recursive.
    public void generateAllIndependentVariableValueCombinations(ArrayList<Double> current)
        {
        if (current.size() == indIndexes.length)
            {
            allIndependentVariableValueCombinations.add((ArrayList<Double>)current.clone());
            return;
            }

        int index = current.size();
        double increment = 0;
        if (indDivisions[index] != 1)
            increment  = (indMaxValues[index]-indMinValues[index]) / (indDivisions[index]-1);
                
        for(int i = 0; i < indDivisions[index]; i++)
            {
            current.add(indMinValues[index] + i * increment);
            generateAllIndependentVariableValueCombinations(current);
            current.remove(current.size()-1);
            }
        }

    // Takes the property names, and gets the property indexes
    // FIXME: What does this do precisely?
    void initializeIndexes(sim.util.Properties p) 
        {
        indIndexes = new int[indNames.length];
        for(int i = 0; i<indNames.length; i++)
            {
            boolean success = false;
            for(int a = 0; a<p.numProperties(); a++)
                {
                if (p.getName(a).equals(indNames[i].trim()))
                    {
                    indIndexes[i] = a;
                    success = true;
                    }
                }
            if (!success)
                {
                System.err.println("Independent parameter does not exist: " + indNames[i].trim());
                System.err.println("Available parameters:");
                for(int a = 0; a<p.numProperties(); a++)
                    System.err.println("--> " + p.getName(a));
                System.exit(1);
                }
            }

        depIndexes = new int[depNames.length];
        for(int i = 0; i<depNames.length; i++)
            {
            boolean success = false;
            for(int a = 0; a<p.numProperties(); a++)
                {
                if (p.getName(a).equals(depNames[i].trim()))
                    {
                    depIndexes[i] = a;
                    success = true;
                    }
                }
            if (!success)
                {
                System.err.println("Dependent parameter does not exist: " + depNames[i].trim());
                System.err.println("Available parameters:");
                for(int a = 0; a < p.numProperties(); a++)
                    System.err.println("--> " + p.getName(a));
                System.exit(1);
                }
            }
        }
    

    //creates a new simstate with reflection
    public SimState newInstance(long seed, Class c)
        {
        try
            {
            return (SimState)(c.getConstructor(new Class[] { Long.TYPE }).newInstance(new Object[] { Long.valueOf(seed) } ));
            }
        catch (Exception e)
            {
            throw new RuntimeException("Exception occurred while trying to construct the simulation " + c + "\n" + e);
            }
        }

    // current job count, don't play with this
    int jobCount = 0;
    Object[] nextJobLock = new Object[0];
    public ParameterSweepSimulationJob getNextJob() 
        {
        synchronized(nextJobLock)
            {
            if (jobCount < allIndependentVariableValueCombinations.size() * numTrials)              // I think this means we're done?
                {
                int combination = jobCount / numTrials;  // which variable combination are we doing this time?
                int trial = jobCount % numTrials;  // which trial are we doing this time?
                ParameterSweepSimulationJob job = new ParameterSweepSimulationJob(allIndependentVariableValueCombinations.get(combination), this, jobCount, trial);
                printSynchronized("Job " + jobCount);
                jobCount++;
                return job;
                }
            else
                return null;
            }
        }
    
    public int getTotalJobs()
        {
        synchronized(nextJobLock)
            {
            return allIndependentVariableValueCombinations.size() * numTrials;
            }
        }
        
    public int getJobCount()
        {
        synchronized(nextJobLock)
            {
            return jobCount;
            }
        }
        
    void writeFileHeader() 
        {
        StringBuilder header = new StringBuilder(); 
        header.append("job, trial, rng");
        for(int i = 0; i < indNames.length; i++) 
            {
            header.append(", " + indNames[i]);
            }
            
        for (int i = 0; i < depNames.length; i++) 
            {
            header.append(", " + depNames[i] + "-final");
            header.append(", " + depNames[i] + "-min");
            header.append(", " + depNames[i] + "-max");
            header.append(", " + depNames[i] + "-avg");
            }
                
        if (mod != 0)
            {
            for(int j = mod - 1; j < numSteps; j += mod)
                {
                for(int i = 0; i < depIndexes.length; i++)
                    {
                    header.append(", " + depNames[i] + "-" + j); 
                    }
                }
            }

        println(header.toString());
        header = null;
        }
    }

//nest this and make static
//
class ParameterSweepSimulationJob
    {
    ArrayList<Double> settings;
    ParameterSweep sweep;
    sim.util.Properties properties;
    StringBuilder builder = new StringBuilder();
    int jobNumber;
    int trial;

    double[] curs;
    double[] mins;
    double[] maxes;
    double[] avgs;
    
    boolean started = false;
        
    public ParameterSweepSimulationJob( ArrayList<Double> settings, ParameterSweep sweep, int jobNumber, int trial)
        {
        this.jobNumber = jobNumber;
        this.trial = trial;
        this.sweep = sweep;
        this.settings = settings;
        avgs = new double[sweep.depIndexes.length];
        mins = new double[sweep.depIndexes.length];
        maxes = new double[sweep.depIndexes.length];
        curs = new double[sweep.depIndexes.length];
        }
    

    public void record(int step, sim.util.Properties properties)
        {
        for(int i = 0; i < sweep.depIndexes.length; i++)
            {
            double value = getPropertyValueAsDouble(properties, sweep.depIndexes[i]);
            curs[i] = value;
            avgs[i] += value;
            if (mins[i] > value || !started)
                mins[i] = value;
            if (maxes[i] < value || !started)
                maxes[i] = value;
                        
            started = true;

            if (sweep.mod != 0 && (step + 1) % sweep.mod == 0)
                {
                builder.append(value + ", ");
                }
            }
        }

    public void recordFinal(sim.util.Properties properties, long seed, ArrayList<Double> combos)
        {
        String str = jobNumber + ", " + (trial + 1) + ", " + seed + ", ";
        
        for(int i = 0; i < combos.size(); i++)
            {
            str += (combos.get(i) + ", ");
            }

        for(int i = 0; i < sweep.depIndexes.length; i++)
            {
            str += (curs[i] + ", " + 
                (avgs[i] / sweep.numSteps) + ", " + 
                mins[i] + ", " +
                maxes[i] + ", ");
            }
        str = str + builder.toString();
                        
        sweep.println(str);
        }
    
    public void run(SimState simState, sim.util.Properties properties, ArrayList<Double> combos) 
        {
        simState.start();
        properties = initSweepValuesFromProperties(properties);
        for(int i = 0; i< sweep.numSteps; i++)
            {
            if (sweep.stop)
                {
                simState.finish();  
                return;
                }
                        
            simState.schedule.step(simState);
            record(i, properties);
            }
      
        recordFinal(properties, simState.seed(), combos);
        simState.finish();
        }
        
    sim.util.Properties initSweepValuesFromProperties(sim.util.Properties properties) 
        {

        for(int index = 0; index < sweep.indIndexes.length; index++)
            {
            String type = properties.getType(sweep.indIndexes[index]).toString();

            if (type.equals("double")) 
                {
                properties.setValue(sweep.indIndexes[index], settings.get(index));
                }
            else if (type.equals("int")) 
                {
                properties.setValue(sweep.indIndexes[index], Integer.valueOf((int)(settings.get(index).doubleValue())));
                }
            else if (type.equals("boolean")) 
                {
                properties.setValue(sweep.indIndexes[index], Boolean.valueOf(settings.get(index).doubleValue() != 0));
                }
            else
                {
                //System.err.println("Independent: unsupported type " + properties.getType(src.main.java.sim.util.sweep.indIndexes[index]).toString() +  " on index " + index + " which should be..." + properties.getName(src.main.java.sim.util.sweep.indIndexes[index]));
                throw new RuntimeException("Unsupported type");
                }
            }
        return properties;
        }

    public double getPropertyValueAsDouble(sim.util.Properties properties, int dependentIndex) 
        {
        double dValue = 0.0;
        int propertyIndex = dependentIndex;
        String type = properties.getType(propertyIndex).toString();

        if (type.equals("double")) 
            {
            dValue = (Double)properties.getValue(propertyIndex);
            }
        else if (type.equals("int")) 
            {
            dValue = ((Integer)properties.getValue(propertyIndex)).doubleValue();
            }
        else if (type.equals("boolean")) 
            {
            dValue = ((Boolean)properties.getValue(propertyIndex)) ? 1  : 0;
            }
        else
            {
            //System.err.println("Independent: unsupported type " + properties.getType(propertyIndex).toString());
            System.exit(1);
            }
        return dValue;
        }
    }
