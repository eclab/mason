package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

public class In extends Provider implements Receiver
    {
    Probe probe;
    
    In(Probe probe)
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
         		probe.updateFromIn(1);
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
	         		probe.updateFromIn(amt);
	         		}
         		}
         	}
         
         _amount = null;		// let it gc
         return val;
    	}

    public String toString()
        {
        return "In@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + 
        	(probe == null ? "" : ("Probe@" + System.identityHashCode(probe) + "(" + (probe.getName() == null ? "" : probe.getName()) + ")"));
        }  
                     
    /** Does nothing. */
    public void step(SimState state)
        {
        // do nothing
        }
    }
