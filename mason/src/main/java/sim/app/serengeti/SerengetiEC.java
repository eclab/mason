/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package sim.app.serengeti;
import ec.app.ant.func.*;
import ec.util.*;
import ec.*;
import ec.gp.*;
import ec.gp.koza.*;
import java.io.*;
import java.util.*;
import ec.simple.*;
import sim.util.*;


public class SerengetiEC extends GPProblem implements SimpleProblemForm
    {
    public static final int MAX_MOVES = 15;
    
    public Serengeti simstate;
	int tnum;
	public GPIndividual _ind;
	public EvolutionState state;
	
    public Object clone()
        {
        SerengetiEC myobj = (SerengetiEC) (super.clone());
        myobj.input = (SerengetiData)(input.clone());
        myobj.simstate = null;
        return myobj;
        }

    public void setup(EvolutionState state, Parameter base)
        {
        // very important, remember this
        super.setup(state,base);

        // verify our input is the right class (or subclasses from it)
        if (!(input instanceof SerengetiData))
            state.output.fatal("GPData class must subclass from " + SerengetiData.class,
                base.push(P_DATA), null);
        }

    public void evaluate( EvolutionState state, Individual ind, int subpopulation, int threadnum)
        {
        initSimState(state, threadnum);
        simstate.problem = this;
        this.state = state;
        tnum = threadnum;
        
        double sum = 0;
        for(int a = 0; a < 100; a++)
        	{
			simstate.start();
		
			_ind = (GPIndividual) ind;
		
			int i = 0;
			for(i = 0; i < MAX_MOVES; i++)
				{
				simstate.schedule.step(simstate);
				if (simstate.gazelle.isDead()) break;
				}
		
			double d = Double.POSITIVE_INFINITY;
			for(int j = 0; j < simstate.lions.size(); j++)
				{
				Double2D lloc = simstate.field.getObjectLocation((Lion)(simstate.lions.get(j)));
				Double2D gloc = simstate.field.getObjectLocation(simstate.gazelle);
				d = Math.min(d, simstate.field.tdx(lloc.x, gloc.x)*simstate.field.tdx(lloc.x, gloc.x) + 
								simstate.field.tdy(lloc.y, gloc.y) * simstate.field.tdy(lloc.y, gloc.y));
			sum += d;

			simstate.finish();
			}
		
		sum /= 10;
		
		// the fitness better be KozaFitness!
		KozaFitness f = ((KozaFitness)ind.fitness);
		f.setStandardizedFitness(state, sum);
		f.hits = 0;
		ind.evaluated = true;		// we don't care
            
            simstate.finish();
            }
        }

    void initSimState(EvolutionState state, int threadnum)
    	{
    	if (simstate == null)
    		{
    		simstate = new Serengeti(1);
    		}
		simstate.random = state.random[threadnum];		// this is the real generator we'll use
    	}
    }
