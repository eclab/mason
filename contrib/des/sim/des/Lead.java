package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
   A LEAD is attached between a provider and a receiver somewhere UPSTREAM of its associated
   PROBE.   With the LEAD installed, a Probe
   can also be used to measure the utilization, average idle time, current sum resources between
   the In and the Probe, and so on.
   
   <p> You should attach a Lead upstream of a Probe and generally in such a position that the Probe cannot
   receive any resources via some route other than through the Lead, and similarly the Lead cannot send
   resources out via any route other than through the Probe.  Otherwise resource statistics will be lost
   between the two and I'm not positive what the behavior would be.
   */

public class Lead extends Provider implements Receiver
    {
    Probe probe;
    
    Lead(Probe probe)
    	{
    	super(probe.state, Probe.DEFAULT_TYPICAL);
    	}
    
    public Probe getProbe() { return probe; }
    
    public Resource getTypical()
    	{
    	if (!receivers.isEmpty())
    		{
    		return (receivers.get(0).getTypical());
    		}
    	else return typical;
    	}

    double _atLeast;
    double _atMost;
    Resource _amount;
        
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)	
    	{
        if (isOffering()) throwCyclicOffers();  // cycle

        if (!(atLeast >= 0 && atMost >= atLeast))
        	throwInvalidAtLeastAtMost(atLeast, atMost);

        _amount = amount;
        _atLeast = atLeast;
        _atMost = atMost;
    	Resource _oldAmount = null;
        if (amount instanceof CountableResource)
        	_oldAmount = amount.duplicate();
        
         boolean val = offerReceivers();
         
         if (val)
         	{
         	if (amount instanceof Entity)
         		{
         		probe.updateFromLead(1);
         		}
         	else
         		{
         		CountableResource cr = (CountableResource)amount;
         		CountableResource crOld = (CountableResource)_oldAmount;
         		double amt = _oldAmount.getAmount() - amount.getAmount();
         		if (amt == 0) // uh
         			{
         			throw new RuntimeException("Receivers returned TRUE when offered, but didn't change the amount.  Uh oh!");
         			}
         		else
         			{
	         		probe.updateFromLead(amt);
	         		}
         		}
         	}
         
         _amount = null;		// let it gc
         return val;
    	}

    public boolean provide(Receiver receiver)
        {
        return false;
        }

    public String toString()
        {
        return "Lead@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + 
        	(probe == null ? "" : ("Probe@" + System.identityHashCode(probe) + "(" + (probe.getName() == null ? "" : probe.getName()) + ")"));
        }  
                     
    /** Does nothing. */
    public void step(SimState state)
        {
        // do nothing
        }
    }
