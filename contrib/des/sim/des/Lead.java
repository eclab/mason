package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;

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

public class Lead extends Filter
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_CHOMP, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    Probe probe;
    
    Lead(Probe probe)
        {
        super(probe.state, Probe.DEFAULT_TYPICAL);
        setName("Lead for " + System.identityHashCode(this));
        }
    
    public Probe getProbe() { return probe; }
    public boolean hideProbe() { return true; }
    
    public Resource getTypicalReceived()
        {
        if (!receivers.isEmpty())
            {
            return (receivers.get(0).getTypicalReceived());
            }
        else return super.getTypicalReceived();
        }
    public boolean hideTypicalReceived() { return true; }

    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)    
        {
        if (getRefusesOffers()) { return false; }
        if (isOffering()) throwCyclicOffers();  // cycle

        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0))
            throwInvalidAtLeastAtMost(atLeast, atMost);

        Resource oldAmount = null;
        if (amount instanceof CountableResource)
            oldAmount = amount.duplicate();
        
        boolean val = offerReceivers(amount, atLeast, atMost);
         
        if (val)
            {
            if (amount instanceof Entity)
                {
                probe.updateFromLead(1);
                }
            else
                {
                CountableResource cr = (CountableResource)amount;
                CountableResource crOld = (CountableResource)oldAmount;
                double amt = oldAmount.getAmount() - amount.getAmount();
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
         
        _amount = null;                // let it gc
        return val;
        }

    public String toString()
        {
        return "Lead@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + 
            (probe == null ? "" : ("Probe@" + System.identityHashCode(probe) + "(" + (probe.getName() == null ? "" : probe.getName()) + ")"));
        }  
    }
