package sim.util.opt;

import ec.*;
import ec.util.*;
import sim.engine.*;
import java.lang.reflect.*;
import sim.util.*;

public class Master
    {
    public SimState simState = null;
    public EvolutionState evolutionState = null;
    public int result = EvolutionState.R_NOTDONE;
    public ParameterDatabase base;
        
    public Master(SimState simState, ParameterDatabase base)
        {
        this.simState = simState;
        this.base = base;
        }
                
    public boolean start()  
        {
        // Build an EvolutionState on it
        evolutionState = Evolve.initialize(base, 0, Evolve.buildOutput(true));            // need to change the offset later FIXME
        evolutionState.startFresh();
        return true; // always for now FIXME
        }
                
    public boolean go()
        {
        result = evolutionState.evolve();
        return (result == EvolutionState.R_NOTDONE);
        }
                
    public void finish()
        {
        evolutionState.finish(result);
        }
        
    public void run()
        {
        if (start())
            {
            while(go());
            finish();
            }
        }
                
    /*
    public static void main(String[] args)
        {
        ParameterDatabase base = Evolve.loadParameterDatabase(args);
        new Master(buildSimState(sim.app.serengeti.Serengeti.class), base).run();
        }
    */
                
    public static SimState buildSimState(Class cls)
        {
        SimState simstate = null;
        try
            {
            Constructor cons = cls.getConstructor(new Class[] { Long.TYPE });

            try
                {
                return (SimState)(cons.newInstance(new Object[] { Long.valueOf(1) }));          // some dummy random number seed
                }
            catch (InstantiationException e)
                {
                System.err.println("Could not instantiate " + cls);
                return null;
                }
            catch (IllegalAccessException e)
                {
                System.err.println("Could not instantiate " + cls);
                return null;
                }
            catch (InvocationTargetException e)
                {
                System.err.println("Could not instantiate " + cls);
                return null;
                }
            }
        catch (NoSuchMethodException e)
            {
            System.err.println("Could not find constructor(long) for " + cls);
            return null;
            }
        }
        
    }
