package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;

/**
   A Probe can be inserted between a provider and a receiver to gather statistical information such
   as the number of offers made, the sum total resource flow, mean resource flow, maximum resource
   offered, and so on.  
   
   <p>A Probe can also detatch a LEAD.  This is a Probe "helper" which is also inserted between
   a provider and a receiver somewhere UPSTREAM of the Probe.  With the LEAD installed, a Probe
   can also be used to measure the utilization, average idle time, current sum resources between
   the In and the Probe, and so on.
*/

public class Probe extends Filter
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_REVERSE_DELAY, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    static final Resource DEFAULT_TYPICAL = new Entity("Probe");
    
    long totalOffers;
    double lastTime;
    double lastThru;
    double sumThru;
    double maxThru;                             // FIXME: should we have a minThru?
    
    // From the Lead
    Lead lead;
    double lastLeadTime;
    double lastLeadThru;
    double current;
    double sumCurrent;
    double utilized;
    double idle;
    boolean processed;
    
    public Probe(SimState state)
        {
        super(state, DEFAULT_TYPICAL);
        reset();
        setName("Probe " + System.identityHashCode(this));
        }
    
    public Lead buildLead() 
        { 
        if (lead == null) lead = new Lead(this);
        return getLead();
        }
        
    public Lead getLead() { return lead; }
    public boolean hideLead() { return true; }
        
    public void reset()
        {
        totalOffers = 0;
        lastTime = Schedule.BEFORE_SIMULATION;
        lastThru = 0;
        maxThru = 0;
        lastLeadTime = Schedule.BEFORE_SIMULATION;
        lastLeadThru = 0;
        current = 0;
        sumCurrent = 0;
        utilized = 0;
        idle = 0;
        processed = true;
        }

    public Resource getTypicalReceived()
        {
        if (!receivers.isEmpty())
            {
            return (receivers.get(0).getTypicalReceived());
            }
        else return typical;
        }
    public boolean hideTypicalReceived() { return true; }

    public double getSumThru()
        {
        return sumThru;
        }
                        
    public double getMaxThru()
        {
        return maxThru;
        }
                  
    double computeTime()
        {
        double time = state.schedule.getTime();
        if (time == Schedule.AFTER_SIMULATION)
            time = lastTime;
        if (time <= 0) return 0;
        else return time;
        }
                        
    public double getThruRate()
        {
        return sumThru / computeTime();
        }

    public double getOfferRate()
        {
        return totalOffers / computeTime();
        }

    public double getRate()
        {
        return sumCurrent / computeTime();
        }

    public double getUtilizationRate()
        {
        return utilized / computeTime();
        }

    public double getIdleRate()
        {
        return idle / computeTime();
        }

    void update(double amt)
        {
        lastTime = state.schedule.getTime();
        lastThru = amt;
        sumThru += lastThru;
        if (lastThru > maxThru) 
            maxThru = lastThru;
        current -= lastThru;
        sumCurrent += current;
        if (!processed)
            {
            if (current > 0)
                utilized += (lastTime - lastLeadTime);
            else
                idle += (lastTime - lastLeadTime);
            processed = false;
            }
        }

    void updateFromLead(double amt)
        {
        lastLeadTime = state.schedule.getTime();
        lastLeadThru = amt;
        current += lastLeadThru;
        sumCurrent += current;
        processed = false;
        }
    
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
            totalOffers++;
            if (amount instanceof Entity)
                {
                update(1);
                }
            else
                {
                CountableResource cr = (CountableResource)amount;
                CountableResource crOld = (CountableResource)oldAmount;
                double amt = crOld.getAmount() - cr.getAmount();
                if (amt == 0) // uh
                    {
                    throw new RuntimeException("Receivers returned TRUE when offered, but didn't change the amount.  Uh oh!");
                    }
                else
                    {
                    update(amt);
                    }
                }
            }
         
        _amount = null;                // let it gc
        return val;
        }

    public String toString()
        {
        return "Probe@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + 
            (lead == null ? "" : ("In@"+ System.identityHashCode(lead) + "(" + (lead.getName() == null ? "" : lead.getName()) + ")"));
        }  
    }
