package sim.app.tutorial1and2.ec;
import ec.*;
import ec.simple.*;
import ec.util.*;
import ec.vector.*;
import sim.field.grid.*;
import sim.app.tutorial1and2.*;

public class CAProblem extends Problem implements SimpleProblemForm
    {
    ECExample[] simulation;
    int numIterations = 1000;
    
    public void setup(EvolutionState state, Parameter param)
    	{
    	simulation = new ECExample[state.evalthreads];
    	for(int i = 0; i < simulation.length; i++)
    		simulation[i] = new ECExample(state.random[i]);
    	} 
    
    public void evaluate(EvolutionState state,
        Individual ind,
        int subpopulation,
        int threadnum)
        	{
        	ECExample sim = simulation[threadnum];
        	sim.start();

        	BitVectorIndividual bool = (BitVectorIndividual) ind;
        	boolean[] genome = bool.genome;
        	
        	// seed the grid
        	int count = 0;
        	int[][] field = sim.grid.field;
        	for(int x = 0; x < 10; x++)
        		{
        		int[] fieldx = field[45 + x];
        		for(int y = 0; y < 10; y++)
        			{
        			fieldx[y] = (genome[count++] ? 1 : 0);
        			}
        		}

        	// backup grid
        	IntGrid2D backup = new IntGrid2D(sim.grid.getWidth(), sim.grid.getHeight());        	
        	int size = 0;
        	for(int i = 0; i < numIterations; i++)
        		{
        		backup.setTo(sim.grid);
        		sim.schedule.step(sim);
        		int val = size(sim.grid);
        		if (val > size) size = val;
        		if (!diff(backup, sim.grid)) break;
        		}
        	
        	// assess as maximum size
            ((SimpleFitness)(ind.fitness)).setFitness( state, count, count == 100 * 100 );
            ind.evaluated = true;
            
        	sim.finish();
        	}
        	
    // returns number of non-zero elements in g1
	public int size(IntGrid2D g1)
		{
		int count = 0;
		int[][] g1_field = g1.field;
		int len = g1_field.length;
		
		for(int x = 0; x < len; x++)
			{
			int[] g1_fieldx = g1.field[x];
			int lenx = g1_fieldx.length;
			for(int y = 0; y < len; y++)
				{
				count += g1_fieldx[y];
				}
			}
		return count;
		}

    // returns true if there is a difference between g1 and g2
    // assumes g1 and g2 are the same shape
	public boolean diff(IntGrid2D g1, IntGrid2D g2)
		{
		int[][] g1_field = g1.field;
		int[][] g2_field = g2.field;
		int len = g1_field.length;
		
		for(int x = 0; x < len; x++)
			{
			int[] g1_fieldx = g1.field[x];
			int[] g2_fieldx = g2.field[x];
			int lenx = g1_fieldx.length;
			for(int y = 0; y < len; y++)
				{
				if (g1_fieldx[y] != g2_fieldx[y]) return false;
				}
			}
		return true;
		}

    public void describe(
        EvolutionState state, 
        Individual ind, 
        int subpopulation,
        int threadnum,
        int log)
        	{
        	}
    }
