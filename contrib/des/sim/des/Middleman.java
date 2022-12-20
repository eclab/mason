/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
   An object which is both a Provider and a Receiver.   Includes a basic implementation
   of getRefusesOffers(), and by default has getTypicalReceived() return the same value
   as getTypicalProvided().
*/

public abstract class Middleman extends Provider implements Receiver
    {
    private static final long serialVersionUID = 1;

    public Middleman(SimState state, Resource typical)
        {
        super(state, typical);
		}
		
	boolean refusesOffers;

    public void setRefusesOffers(boolean value)
    	{
    	refusesOffers = value;
    	}

    public boolean getRefusesOffers()
    	{
    	return refusesOffers;
    	}

    public Resource getTypicalReceived() 
    	{ 
    	return getTypicalProvided(); 
    	}

//    public boolean hideTypicalReceived() { return true; }

	
    public abstract boolean accept(Provider provider, Resource resource, double atLeast, double atMost);
    }
