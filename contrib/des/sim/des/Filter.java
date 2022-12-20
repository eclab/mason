package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;

/** 
    A convenience superclass for DES objects which accept offers and immediately 
    turn around in zero time and offer them to a single downstream receiver.  This
    class largely exists to include a bunch of boilerplate variables and methods
    common to all these kinds of objects.
    
    <p>Because they're just passing on their resource, Filter objects don't place
    the resource in the Entities list or in the resource pool; they just stash it
    and hand it on.  This is done by calling the special method 
    offerReceivers(amount, atLeast, atMost) during the accept(...) method.  This in
    turn stashes these three values and calls offerReceivers() which eventually
    calls a new offerReceiver(...) method that uses the three stashed values. 
 */

public abstract class Filter extends Middleman
    {
    private static final long serialVersionUID = 1;

    Resource _amount;
    double _atLeast;
    double _atMost;

    public Filter(SimState state, Resource typical)
        {
        super(state, typical);
        }

    /** Returns false always and does nothing: Filter is push-only. */
    public boolean provide(Receiver receiver)
        {
        return false;
        }

    protected boolean offerReceivers(Resource amount, double atLeast, double atMost)
        {
        _amount = amount;
        _atLeast = atLeast;
        _atMost = atMost;
        boolean ret = offerReceivers();
        _amount = null;
        return ret;
        }

    protected boolean offerReceiver(Receiver receiver, double atMost)
        {
        if (!getMakesOffers())
            {
            return false;
            }
                
        if (_amount instanceof CountableResource)
            {
            double originalAmount = _amount.getAmount();
            lastOfferTime = state.schedule.getTime();
            boolean result = receiver.accept(this, _amount, Math.min(_atLeast, atMost), Math.min(_atMost, atMost));
            if (result)
                {
                CountableResource removed = (CountableResource)(resource.duplicate());
                removed.setAmount(originalAmount - _amount.getAmount());
                updateLastAcceptedOffers(removed, receiver);
                }
            return result;
            }
        else
            {
            // FIXME: do I need to check _atLeast and _atMost?
            return offerReceiver(receiver, (Entity) _amount);
            }
        }

    public String toString()
        {
        return "Filter@" + System.identityHashCode(this);
        }

	/** By default does nothing. */
	public void process(Resource amountOfferedMe, Resource amountAcceptedFromMe)
		{
		// does nothing
		}
	
	/** Override this as you like.  The default version offers to downstream Receivers whatever it is being
		offered here; and then calls process(...) to process the difference between the two.  By default
		process(...) does nothing, but you could override that too. */
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
    	{
        if (getRefusesOffers()) { return false; }
        if (isOffering()) throwCyclicOffers();  // cycle

        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0))
            throwInvalidAtLeastAtMost(atLeast, atMost);
        
        Resource oldAmount = null;
        if (amount instanceof CountableResource)
            oldAmount = amount.duplicate();

        boolean result = offerReceivers(amount, atLeast, atMost);
        process(oldAmount, amount);
        return result;
    	}
    }
