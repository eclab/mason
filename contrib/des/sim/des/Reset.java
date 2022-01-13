package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
   A simple class to make it easy to reset various objects, typically when start() is called in a model.
*/

public class Reset
    {
    ArrayList<Resettable> resets;
    SimState state;
    
    public Reset(SimState state)
    	{
    	this.state = state;
    	resets = new ArrayList<Resettable>();
    	}
    	
    public void reset()
    	{
    	for(Resettable r : resets)
    		r.reset(state);
    	}
    
    public void add(Resettable r)
    	{
    	resets.add(r);
    	}
    
    public void remove(Resettable r)
    	{
    	resets.remove(r);
    	}
    }
