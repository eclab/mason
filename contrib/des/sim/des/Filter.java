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

public abstract class Filter extends Provider implements Receiver
    {
    private static final long serialVersionUID = 1;

    Resource _amount;
    double _atLeast;
    double _atMost;

    public Resource getTypicalReceived() { return typical; }
    public boolean hideTypicalReceived() { return true; }

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

    public abstract boolean accept(Provider provider, Resource amount, double atLeast, double atMost);

    boolean refusesOffers = false;
    public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
    }
