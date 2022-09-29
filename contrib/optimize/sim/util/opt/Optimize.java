/*
  Copyright 2019 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.opt;

import ec.*;
import ec.util.*;
import sim.engine.*;
import java.lang.reflect.*;
import sim.util.*;
import ec.vector.*;

/** 
	A special version of Master which automatically handles many common optimization cases.
	
	<p>Optimize is designed to allow you to optimize simple numerical and boolean global 
		parameters in your MASON model using one or more vector optimization techniques
		(such as a genetic algorithm, evolution strategy, or CMA-ES).  You can optimize
		for one objective, or for multiple objectives if you use an appropriate algorithm
		(such as NSGA-III).  All you have to do is provide an ECJ parameter file detailing
		the particular algorithm, and further outfitted with some parameters which specify
		which variables you wish to optimize.

	
	<p>You will also need to subclass the <b>assess(...)</b> method in your MASON
	model to provide an assessment. 
	
	<p>In addition to standard ECJ model parameters
		
	<pre>

	# The global MASON model variables to optimize.  Note that their names are their
	# Java Bean Property names.  These must be both readable and writable properties.
	
	num-mason-properties = 5
	mason-property.0 = Avoidance
	mason-property.1 = Cohesion
	mason-property.2 = Consistency
	mason-property.3 = Momentum
	mason-property.4 = Randomness
	
	# How long should a model be run?
	mason-steps = 1000
	
	# How many models should be run with given variable settings, then averaged,
	# to obtain an assessment?
	mason-num-trials = 1
	
	# How many objectives?  DEFAULT = 1
	mason-objectives = 1
	
	# Which MASON model should we be running?
	mason-class = sim.app.flockers.Flockers

	# Does every run of the model require its own brand-new constructed SimState?
	# (As opposed to reusing an existing SimState and cleaning it up during its 
	# start() method).  DEFAULT = false
	mason-rebuild-model = false
	</pre>
	
		
*/

public class Optimize extends Master
    {
    public Optimize(SimState simState, ParameterDatabase base)
        {
        super(simState, base);
        }
                
    public boolean start()  
        {
        Properties properties = MASONProblem.getProperties(simState);
        base.set(new ec.util.Parameter("eval.problem"), "sim.util.opt.MASONProblem");

        // set up genome
        int numIndexes = base.getInt(new ec.util.Parameter("num-mason-properties"), null);
        base.set(new ec.util.Parameter("pop.subpop.0.species.genome-size"), "" + numIndexes);
        int[] indexes = new int[numIndexes];
        Object[] throwaway = new Object[0];
        
        boolean treatParametersAsArray = base.getBoolean(new ec.util.Parameter("mason-properties-as-array"), null, false);
        if (!base.exists(new ec.util.Parameter("mason-properties-as-array"), null))
        	{
			System.err.println("mason-properties-as-array missing or malformed.  Default is false.");
        	}
        
        if (!treatParametersAsArray)
        	{
        	// need to specify min/max
			for(int i = 0; i < numIndexes; i++)
				{
				String name = base.getString(new ec.util.Parameter("mason-property." + i), null);
				indexes[i] = properties.indexForName(name);
				//System.err.println("Property " + name + " is " + indexes[i]);
				Object domain = properties.getDomain(indexes[i]);
				Class type = properties.getType(indexes[i]);
				double _min = 0.0;
				double _max = 1.0;
				if (type == Boolean.TYPE)
					{
					if (domain != null)
						System.err.println("ERROR: boolean property " + name + " has a domain: " + domain);
					else
						{
						// do nothing, we're set up
						}
					}
				else if (type == Integer.TYPE)
					{
					if (domain == null || !(throwaway.getClass().isAssignableFrom(domain.getClass())))
						System.err.println("ERROR: integer property " + name + " has a domain: " + domain);
					else
						{
						_min = 0;
						_max = ((Object[])domain).length - 1;
						}
					}
				else if (type == Double.TYPE)
					{
					if (domain == null || !(domain instanceof sim.util.Interval))
						System.err.println("ERROR: double property " + name + " has a domain: " + domain);
					else
						{
						sim.util.Interval interval = (sim.util.Interval)domain;
						_min = interval.getMin().doubleValue();
						_max = interval.getMax().doubleValue();
						}
					}
				else
					{
					System.err.println("ERROR: property " + name + " has is of invalid type: " + type);
					}
				base.set(new ec.util.Parameter("pop.subpop.0.species.min-gene." + i), "" + _min);
				base.set(new ec.util.Parameter("pop.subpop.0.species.max-gene." + i), "" + _max);
				}
			}
                
                
        // Other Model parameters
        int numObjectives = base.getInt(new ec.util.Parameter("mason-objectives"), null);
        if (numObjectives == -1)
            {
            System.err.println("mason-numobjectives missing or malformed.   Default is 1");
            numObjectives = 1;
            }
        base.set(new ec.util.Parameter("multi.fitness.num-objectives"), "" + numObjectives);
    	
    	int fitnessType = MASONProblem.FITNESS_TYPE_SIMPLE;
    	String fit = base.getString(new ec.util.Parameter("pop.subpop.0.species.fitness"), null);
    	if (fit == null)
    		{
            System.err.println("ERROR: there is no parameter pop.subpop.0.species.fitness set.");
    		}
    	else
    		{
    		if (fit.trim().equals("ec.multiobjective.nsga2.NSGA2MultiObjectiveFitness"))
    			{
    			fitnessType = MASONProblem.FITNESS_TYPE_NSGA2;
    			}
    		}
    	
        //int maximumSteps = base.getInt(new ec.util.Parameter("mason-steps"), null);
        int maximumSteps = base.getInt(new ec.util.Parameter("mason-steps"), null, 0);
        if (maximumSteps <= 0)
        	{
			System.err.println("mason-steps missing or malformed.  Default is 0.");
			maximumSteps = 0;
        	}
        	
        //double maximumTime = base.getDouble(new ec.util.Parameter("mason-time"), null);
        double maximumTime = base.getDouble(new ec.util.Parameter("mason-time"), null, 0.0);
        if (maximumTime <= 0)
        	{
			System.err.println("mason-time missing or malformed.  Default is 0.");
			maximumTime = 0.0;
        	}
        	
        if (maximumSteps == 0 && maximumTime == 0)
        	{
        	System.err.println("mason-steps or mason-time cannot both be missing, malformed, or 0. Bailing.");
        	return false;
        	}
        	
        int numTrials = base.getInt(new ec.util.Parameter("mason-num-trials"), null);
		if (numTrials <= 0) 
			{
			System.err.println("mason-num-trials missing or malformed.  Default is 1.");
			numTrials = 1;
			}
			
        boolean rebuildSimState = base.getBoolean(new ec.util.Parameter("mason-rebuild-model"), null, false);
        if (!base.exists(new ec.util.Parameter("mason-rebuild-model"), null))
        	{
			System.err.println("mason-rebuild-model missing or malformeds.  Default is false.");
        	}
                
        // Build an EvolutionState on it
        super.start();
                
        // After startFresh we have loaded from the parameter database and we're ready to go.
        // But we have not yet called sendAdditionalData on the MasterProblem.  So we set up
        // the MASONProblem to prepare it for that eventuality.  Now we build the MASONProblem.
                
        MASONProblem problem = (MASONProblem)(evolutionState.evaluator.masterproblem.problem);
        problem.modelClassName = simState.getClass().getName();
                
        problem.loadDefaults(simState, indexes, numObjectives, fitnessType, maximumSteps, maximumTime, numTrials, rebuildSimState, treatParametersAsArray);

/*
        // We may need to reset the individuals now
        if (base.getBoolean(new ec.util.Parameter("mason-start-from-settings"), null, false))
            {
            Subpopulation subpop = evolutionState.population.subpops.get(0);
            for(int i = 0; i < subpop.individuals.size(); i++)
                {
                DoubleVectorIndividual ind = (DoubleVectorIndividual)(subpop.individuals.get(i));
                for(int j = 0; j < ind.genome.length; j++)
                    {
                    ind.genome[j] = problem.parameterValue[problem.indParameterIndex[j]];
                    System.err.println("Reset Gene " + j + " TYPE " + problem.parameterType[problem.indParameterIndex[j]] + " VAL " + problem.parameterValue[problem.indParameterIndex[j]]);
                    }
                }
            }
*/
                
        return true; // always for now FIXME
        }
    
    public void stop()
	    {
	    
	    }
                
    public static void main(String[] args)
        {
        ParameterDatabase base = Evolve.loadParameterDatabase(new String[] { "-file", args[0] });
        //ParameterDatabase base = Evolve.loadParameterDatabase(new String[] { "-from", args[0], "-at", "sim.util.opt.Optimize" });
        String classname = base.getString(new ec.util.Parameter("mason-class"), null);
        try { 
            Class c = Class.forName(classname); 
            new Optimize(buildSimState(c), base).run();
            }
        catch (ClassNotFoundException ex) { System.err.println("Invalid value for parameter 'mason-class'.  Should be a full classname.  Was: " + classname); System.exit(1); }
        }
    }
