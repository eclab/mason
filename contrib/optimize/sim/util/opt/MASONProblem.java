package sim.util.opt;

import sim.*;
import sim.engine.*;
import sim.util.*;
import ec.*;
import ec.simple.*;
import ec.multiobjective.*;
import ec.util.*;
import ec.eval.*;
import ec.vector.*;
import java.io.*;
import java.lang.reflect.*;


public class MASONProblem extends Problem implements SimpleProblemForm
    {
    public static final int TYPE_OTHER = 0;
    public static final int TYPE_DOUBLE = 1;
    public static final int TYPE_INT = 2;
    public static final int TYPE_BOOLEAN = 3;
        
    /** Type of each default parameter.  If the parameter is not to be set (perhaps it's not settable, like a String), then its type is TYPE_OTHER. */
    public int[] parameterType;
    /** Value of each default parameter that we'll be setting, except for default parameters of TYPE_OTHER */
    public double[] parameterValue;
    /** Indexes of the default parameters that we'll override with the individual's genetic settings. 
        This is the same length as the individual's genetic representation. */
    public int[] indParameterIndex;
        
    public static final int FITNESS_TYPE_SIMPLE = 0;
    public static final int FITNESS_TYPE_NSGA2 = 1;

    public int fitnessType = FITNESS_TYPE_SIMPLE;
    /** Min objective values */
    public int numObjectives;
    /** Fully qualified class name of our model class. */
    public String modelClassName;
    /** How long we run the simulation. */
    public int maximumSteps;
    public double maximumTime;
    /** How many trials do we run for? */
    public int numTrials;
    /** Do we re-construct the SimState every time? */
    public boolean rebuildSimState;
    /** Do we trat the parameters as an array or as properties? */
    public boolean treatParametersAsArray;
        
    boolean ready;
    Object[] lock = new Object[0];
    void raiseReady()
        {
        synchronized(lock) { ready = true; lock.notifyAll(); }
        }
    void blockOnReady()
        {
        synchronized(lock)
            {
            while (!ready)
                {
                // System.err.println("NOT READY!");
                try { lock.wait(); }
                catch (InterruptedException ex) 
                    {
                    System.err.println("MASONProblem.blockOnReady: this should never happen");
                    }
                }
            }
        }

    /** The *slave's* SimState.  This is used internally by the slave.  It won't be set in the master. */
    public SimState simstate;
                
    public void sendAdditionalData(EvolutionState state, DataOutputStream dataOut)
        {
        blockOnReady();
        try
            {
            dataOut.writeUTF(modelClassName);
            dataOut.writeInt(maximumSteps);
            dataOut.writeDouble(maximumTime);
            dataOut.writeInt(numTrials);
            dataOut.writeBoolean(rebuildSimState);
            dataOut.writeBoolean(treatParametersAsArray);
            int size = parameterType.length;
            dataOut.writeInt(size);
            for(int i = 0; i < size; i++)
                {
                //System.err.println("PARAM " + i + " " + " TYPE " + parameterType[i] + " VALUE " + parameterValue[i]);
                dataOut.writeInt(parameterType[i]);
                dataOut.writeDouble(parameterValue[i]);
                }
            dataOut.writeInt(fitnessType);
            dataOut.writeInt(numObjectives);
            size = indParameterIndex.length;
            dataOut.writeInt(size);
            for(int i = 0; i < size; i++)
                {
                dataOut.writeInt(indParameterIndex[i]);
                }
            }
        catch (IOException e)
            {
            state.output.fatal("IOException in sending data");
            }
        }

    public void receiveAdditionalData(EvolutionState state, DataInputStream dataIn)
        {
        try
            {
            modelClassName = dataIn.readUTF();
            maximumSteps = dataIn.readInt();
            maximumTime = dataIn.readDouble();
            numTrials = dataIn.readInt();
            rebuildSimState = dataIn.readBoolean();
            treatParametersAsArray = dataIn.readBoolean();
            int size = dataIn.readInt();
            parameterType = new int[size];
            parameterValue = new double[size];
            for(int i = 0; i < size; i++)
                {
                parameterType[i] = dataIn.readInt();
                parameterValue[i] = dataIn.readDouble();
                //System.err.println("PARAM " + i + " " + " TYPE " + parameterType[i] + " VALUE " + parameterValue[i]);
                }
            fitnessType = dataIn.readInt();
            numObjectives = dataIn.readInt();
            size = dataIn.readInt();
            indParameterIndex = new int[size];
            for(int i = 0; i < size; i++)
                {
                indParameterIndex[i] = dataIn.readInt();
                }

//            System.err.println("FITNESS TYPE " + fitnessType);
/*
            if (fitnessType == FITNESS_TYPE_NSGA2)
            	{
//            	System.err.println("SETTING UP SPECIES AGAIN");
	            state.parameters.set(new ec.util.Parameter("pop.subpop.0.species.fitness"), "ec.multiobjective.nsga2.NSGA2MultiObjectiveFitness");
	            state.population.subpops.get(0).species.setup(state, new ec.util.Parameter("pop.subpop.0.species"));
	            }
*/
            }
        catch (IOException e)
            {
            state.output.fatal("IOException in sending data");
            }
        
        }

    public void transferAdditionalData(EvolutionState state)
        {
        // we don't know if state has a MasterProblem or not.
        Problem problem = state.evaluator.p_problem;
        if (problem instanceof MasterProblem)  // maybe this will never happen?  Dunno FIXME
            {
            problem = ((MasterProblem)problem).problem;
            }
                
        if (problem instanceof MASONProblem)
            {
            MASONProblem mp = (MASONProblem)problem;
            mp.parameterType = (int[])(parameterType.clone());
            mp.parameterValue = (double[])(parameterValue.clone());
            mp.indParameterIndex = (int[])(indParameterIndex.clone());
            mp.simstate = null;
            mp.maximumSteps = maximumSteps;
            mp.maximumTime = maximumTime;
            mp.numTrials = numTrials;
            mp.modelClassName = modelClassName;
            mp.numObjectives = numObjectives;
            mp.rebuildSimState = rebuildSimState;
            mp.treatParametersAsArray = treatParametersAsArray;
            mp.fitnessType = fitnessType;
            
//            System.err.println("FITNESS TYPE " + fitnessType);
            if (fitnessType == FITNESS_TYPE_NSGA2)
            	{
//            	System.err.println("SETTING UP SPECIES AGAIN");
	            state.parameters.set(new ec.util.Parameter("pop.subpop.0.species.fitness"), "ec.multiobjective.nsga2.NSGA2MultiObjectiveFitness");
	            state.population.subpops.get(0).species.setup(state, new ec.util.Parameter("pop.subpop.0.species"));
	            }
            }
        else
            {
            // uh oh
            state.output.fatal("Not a MASON Problem: " + problem);
            }
        }
        
    public Object clone()
        {
        MASONProblem mp = (MASONProblem)(super.clone());
        mp.parameterType = (int[])(parameterType.clone());
        mp.parameterValue = (double[])(parameterValue.clone());
        mp.indParameterIndex = (int[])(indParameterIndex.clone());
        mp.simstate = null;
        mp.modelClassName = modelClassName;             // not technically necessary
        mp.maximumSteps = maximumSteps;                 // not technically necessary
        mp.maximumTime = maximumTime;
        mp.numTrials = numTrials;
        mp.numObjectives = numObjectives;               // not technically necessay
        mp.rebuildSimState = rebuildSimState;
        mp.treatParametersAsArray = treatParametersAsArray;
        mp.fitnessType = fitnessType;					// not technically necessary
        return mp;
        }
        
    void initSimState(EvolutionState state, int threadnum)
        {
        if (simstate == null || rebuildSimState)
            {
            try
                {
                Class cls = Class.forName(modelClassName);

                try
                    {
                    Constructor cons = cls.getConstructor(new Class[] { Long.TYPE });

                    try
                        {
                        simstate = (SimState)(cons.newInstance(new Object[] { Long.valueOf(50957) }));          // some dummy random number seed
                        }
                    catch (InstantiationException e)
                        {
                        state.output.fatal("Could not instantiate " + modelClassName);
                        }
                    catch (IllegalAccessException e)
                        {
                        state.output.fatal("Could not instantiate " + modelClassName);
                        }
                    catch (InvocationTargetException e)
                        {
                        state.output.fatal("Could not instantiate " + modelClassName);
                        }
                    }
                catch (NoSuchMethodException e)
                    {
                    state.output.fatal("Could not find constructor(long) for " + modelClassName);
                    }

                }
            catch(ClassNotFoundException e)
                {
                state.output.fatal("Could not find class " + modelClassName);
                }
                        
                                
            }
        //simstate.random = state.random[threadnum];            // this is the real generator we'll use
        }
    
    public static Properties getProperties(SimState simstate)
        {
        return Properties.getProperties(simstate, false, false, false, true, true);
        }
    
    public void loadDefaults(SimState state, int[] indParameterIndex, int numObjectives, int fitnessType, int maxSteps, double maxTime, int numTrials, boolean rebuildSimState, boolean treatParametersAsArray)
        {
        this.indParameterIndex = (int[])(indParameterIndex.clone());
        this.maximumSteps = maxSteps;
        this.maximumTime = maxTime;
        this.numTrials = numTrials;
        this.numObjectives = numObjectives;
        this.modelClassName = state.getClass().getName();
        this.rebuildSimState = rebuildSimState;
        this.treatParametersAsArray = treatParametersAsArray;
        this.fitnessType = fitnessType;
        
        // We restrict the properties as:
        // 1. Don't expand collections (we're ignoring those properties anyway)
        // 2. Don't include SimState as a superclass
        // 3. Don't include getClass()
        // 4. Allow domFoo() and hideFoo() extensions
        // 5. Allow dynamic properties / proxies
        simstate = state;
        Properties prop = getProperties(state);
        
        parameterType = new int[prop.numProperties()];
        parameterValue = new double[prop.numProperties()];
        
        // load default parameters
        for(int j = 0; j < parameterType.length; j++)
            {
            if (prop.getType(j) == Double.TYPE)
                {
                parameterType[j] = TYPE_DOUBLE;
                parameterValue[j] = ((Double)(prop.getValue(j))).doubleValue();
                }
            else if (prop.getType(j) == Integer.TYPE)
                {
                parameterType[j] = TYPE_INT;
                parameterValue[j] = (int)(((Integer)(prop.getValue(j))).intValue());
                }
            else if (prop.getType(j) == Boolean.TYPE)
                {
                parameterType[j] = TYPE_BOOLEAN;
                parameterValue[j] = ((((Boolean)(prop.getValue(j))).booleanValue()) == true ? 1.0 : 0.0);
                }
            else
                {
                parameterType[j] = TYPE_OTHER;
                parameterValue[j] = 0.0;
                }
            }
        
        for(int j = 0; j < indParameterIndex.length; j++)
            {               
            //System.err.println("Gene " + j + " TYPE " + parameterType[indParameterIndex[j]] + " VAL " + parameterValue[indParameterIndex[j]] + " = " + prop.getName(indParameterIndex[j]));
            }

        raiseReady();           
        }
        
        	
    void setParameter(EvolutionState state, Properties properties, int index, double value)
        {
        String val = "";
        if (parameterType[index] == TYPE_OTHER)
            {
            state.output.fatal("Invalid type for MASON Problem at index " + index);
            }
        else if (parameterType[index] == TYPE_DOUBLE)
            {
            val = "" + value;
            }
        else if (parameterType[index] == TYPE_INT)
            {
            if (value != (int)value) // uh oh
                {
                state.output.fatal("Value " + value + " is not an integer, as expected in index " + index);
                }
            else
                {
                val = "" + (int)value;
                }
            }
        else if (parameterType[index] == TYPE_BOOLEAN)
            {
            if (value != 1 && value != 0)
                {
                state.output.fatal("Value " + value + " is not 1 or 0, as expected in index " + index);
                }
            else
                {
                val = (value == 1 ? "true" : "false");
                }
            }
        else 
            {
            state.output.fatal("Whaaaaaa?");
            }
        
        properties.setValue(index, val);
        }
        
    void setProperties(EvolutionState state, Individual ind)
        {
        // We restrict the properties as:
        // 1. Don't expand collections (we're ignoring those properties anyway)
        // 2. Don't include SimState as a superclass
        // 3. Don't include getClass()
        // 4. Allow domFoo() and hideFoo() extensions
        // 5. Allow dynamic properties / proxies
        Properties prop = Properties.getProperties(simstate, false, false, false, true, true);
        
        // reload default parameters
        for(int j = 0; j < parameterType.length; j++)
            {
            if (parameterType[j] != TYPE_OTHER)
                {
                setParameter(state, prop, j, parameterValue[j]);
                }
            }
        
        // override with individual parameters
        if (ind instanceof DoubleVectorIndividual)
            {
            double[] genome = ((DoubleVectorIndividual)ind).genome;
            if (genome.length != indParameterIndex.length)
                {
                state.output.fatal("DoubleVectorGenome length is wrong.  Length is " + genome.length + " but I expected " + indParameterIndex.length);
                }
                
            if (treatParametersAsArray)
            	{
    			simstate.setOptimizationParameters(genome);
            	}
        	else
            	{            
				for(int i = 0; i < indParameterIndex.length; i++)
					{
					setParameter(state, prop, indParameterIndex[i], genome[i]);
					}
				}
            }
        else
            {
            state.output.fatal("Individual is not a DoubleVectorIndivdual in MASONProblem.");
            }
        }
        
    void setFitness(EvolutionState state, Individual ind, double[] assessment)
        {
        Fitness fit = ind.fitness;
        
        if (fit instanceof MultiObjectiveFitness)
            {
            MultiObjectiveFitness mf = (MultiObjectiveFitness)fit;
            for(int i = 0; i < mf.maxObjective.length; i++)
                {                       
                mf.maxObjective[i] = 1;
                mf.minObjective[i] = 0;
                mf.maximize[i] = true;
                }
                
            // load the objectives
            mf.setObjectives(state, assessment);
            ind.evaluated = true;
            }
        else if (fit instanceof SimpleFitness)
            {
            SimpleFitness sf = (SimpleFitness)fit;
            sf.setFitness(state, assessment[0], assessment[0] == 1.0);
            ind.evaluated = true;
            }
        else
            {
            state.output.fatal("Unknown Fitness for MASONProblem");
            }
        }
        
    public void describe(
        final EvolutionState state, 
        final Individual ind, 
        final int subpopulation,
        final int threadnum,
        final int log)
        {
		initSimState(state, threadnum);
		PrintWriter writer = state.output.getLog(log).writer;
		simstate.setDescription(writer);
		setProperties(state, ind);
		simstate.start();
		do
			{
			if (!simstate.schedule.step(simstate)) 
				{
				break; 
				}
			}
		// we test for maximumTime second in the hopes that the compiler will
		// compile it second, since getTime() is synchronized
		while((maximumSteps > 0 && simstate.schedule.getSteps() < maximumSteps) ||
			  (maximumTime > 0 && simstate.schedule.getTime() < maximumTime)); 
		simstate.finish();
		if (writer != null) 
			{
			writer.flush();
			}
		simstate.setDescription(null);
		}

    public void evaluate(
        final EvolutionState state,
        final Individual ind,
        final int subpopulation,
        final int threadnum)
        {
        double[] results = new double[numObjectives];
        
        for(int i = 0; i < numTrials; i++)
            {
            initSimState(state, threadnum);
            setProperties(state, ind);
            simstate.start();
                        
            do
                {
                if (!simstate.schedule.step(simstate)) 
                    {
                    break; 
                    }
                }
            while((simstate.schedule.getSteps() < maximumSteps) || (simstate.schedule.getTime() < maximumTime)); 
            double[] r = simstate.assess(numObjectives);
            for(int j = 0; j < r.length; j++)
                results[j] += r[j];

            simstate.finish();
            }
                        
        for(int j = 0; j < results.length; j++)
            results[j] /= numTrials;
                        
        setFitness(state, ind, results);
        }
    }
