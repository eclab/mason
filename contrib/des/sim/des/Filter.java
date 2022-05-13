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
*/

public abstract class Filter extends Provider implements Receiver
	{
    private static final long serialVersionUID = 1;

    protected Resource _amount;
    protected double _atLeast;
    protected double _atMost;

    public Resource getTypicalReceived() { return typical; }
	public boolean hideTypicalReceived() { return true; }

	public Filter(SimState state, Resource typical)
		{
		super(state, typical);
		}

    /** Returns false always and does nothing: Transformer is push-only. */
    public boolean provide(Receiver receiver)
        {
        return false;
        }

    protected boolean offerReceiver(Receiver receiver, double atMost)
        {
        //return receiver.accept(this, _amount, Math.min(_atLeast, atMost), Math.min(_atMost, atMost));
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

    public String toString()
        {
        return "Filter@" + System.identityHashCode(this);
        }

    /** Does nothing. */
    public void step(SimState state)
        {
        // do nothing
        }
        
    public abstract boolean accept(Provider provider, Resource amount, double atLeast, double atMost);

    boolean refusesOffers = false;
	public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
	}