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
    and hand it on.  This is one as follows:
    
    <ul>
    <li>First the Filter has its accept(...) method called.
    
    <li>The accept(...) method then calls the offerReceivers(..., ..., ...) method.
    By default offerReceivers(..., ..., ...) will store the accepted resource, 
    the atLeast value, and the atMost value in three protected instance variables 
    called _amount, _atLeast, and _atMost respectively.
    
    <li>The offerReceivers(..., ..., ...) method then calls offerReceivers() to
    offer the resource as appropriate.
    
    <li>The accept(...) method then calls process(...) to process statistics on
    the amount accepted and successfully offered.  By default this method does
    nothing.
    </ul>
    
    <p>Filter subclasses can and do override any of these methods.  Because of a lot
    of rearranging, it is possible that Filter may be eliminated or folded into
    another class in the near future, so be prepared for that possibility.
    
    <p>The default version of Filter merely accepts resources, immediately offers
    them to downstream receivers, and that's it.n
*/

public class Filter extends Middleman
    {
    private static final long serialVersionUID = 1;

    protected Resource _amount;
    protected double _atLeast;
    protected double _atMost;

    /** Throws an exception indicating that a provision cycle was detected. */
    protected void throwCyclicProvisions()
        {
        throw new RuntimeException("Zero-time cycle found among provide(...) calls including this one." );
        }

    boolean providing;
    /** Returns true if the Filter is currently making an a provide() call to a provider (this is meant to allow you
        to check for provision cycles. */
    protected boolean isProviding() { return providing; }

    public Filter(SimState state, Resource typical)
        {
        super(state, typical);
        }

    public boolean provide(Receiver receiver, double atMost) 
        {
        if (isProviding())
            {
            throwCyclicProvisions();
            }
        if (provider == null) return false;
        if (!isPositiveNonNaN(atMost))
            throwInvalidNumberException(atMost);
        providing = true;
        boolean val = provider.provide(this, atMost);
        providing = false;
        return val;
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
                CountableResource removed = (CountableResource)(_amount.duplicate());
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

    public void process(Resource amountOfferedMe, Resource amountAcceptedFromMe)
        {
        }
        
    /** Override this as you like.  The default version offers to downstream Receivers whatever it is being
        offered here; and then calls process(...) to process the difference between the two.  By default
        process(...) does nothing, but you could override that too. */
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (isOffering()) throwCyclicOffers();  // cycle

        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);
        
        Resource oldAmount = null;
        if (amount instanceof CountableResource)
            oldAmount = amount.duplicate();

        boolean result = offerReceivers(amount, atLeast, atMost);

        if (result)     
            {
            double diff = amount.getAmount() - (oldAmount == null ? 1.0 : oldAmount.getAmount());
            totalAcceptedOfferResource += diff;
            totalReceivedResource += diff;
            }

        process(oldAmount, amount);
        return result;
        }

    Provider provider = null;
    
    public Provider getProvider()
        {
        return provider;
        }
        
    public void setProvider(Provider provider)
        {
        this.provider = provider;
        }
    }
