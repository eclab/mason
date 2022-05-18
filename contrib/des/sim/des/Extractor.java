/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.distribution.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;


/** 
    A subclass of Source which, when stepped, provides resources to Receivers by first requesting them
    from a Provider via a pull operation (calling provide()).  The amount of resources and
    the timing of the steps are exactly the same as described in Source.  Unlike Source, capacity
    is ignored and getCapacity() and setCapacity() do nothing.
        
    <p>Extractors are Receivers but are not designed to receive things via push, only via pull.
    Thus you should not register them as receivers of a given Provider.  Instead you should 
    attach an Extractor to a Provider via its setProvider() method or in its constructor.
*/


public class Extractor extends Source implements Receiver
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_POINTER_LEFT, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    Provider provider = null;
    
    public Resource getTypicalReceived() { return typical; }
    public boolean hideTypicalReceived() { return true; }

    /** 
        Builds a source with the given typical resource type.  The provider is initially null.
    */
    public Extractor(SimState state, Resource typical)
        {
        super(state, typical);
        }
               
    /** 
        Builds a source with the given typical resource type and provider.
    */
    public Extractor(SimState state, Resource typical, Provider provider)
        {
        this(state, typical);
        setProvider(provider);
        }
               
    public void setProvider(Provider provider) { this.provider = provider; }
        
    public Provider getProvider() { return provider; }
    public boolean hideProvider() { return true; }
                
    boolean offersImmediately = true;
    
    /** Returns whether the Extractor offers items immediately upon accepting (when possible) in zero time,
        as opposed to when it is stepped. */
    public boolean getOffersImmediately() { return offersImmediately; }

    /** Sets whether the Extractor offers items immediately upon accepting (when possible) in zero time,
        as opposed to when it is stepped. */
    public void setOffersImmediately(boolean val) { offersImmediately = val; }

    protected Entity buildEntity()
        {
        Entity ret = (Entity)(typical.duplicate());
        ret.clear();
        return ret;
        }
        
    static final int OFF = -1;
    double acceptValue = OFF;
    
    /** Builds a single entity, ignoring the amount passed in, by asking the provider to provide it.  */
    protected void buildEntities(double amt)
        {
        acceptValue = 1;                        // we always just grab ONE, not multiple entities
        if (provider != null) provider.provide(this);           // the provider will immediately call accept(...)
        acceptValue = OFF;
        }
        
    /** Builds resource by asking the provider to provide it.  */
    protected void buildResource(double amt)
        {
        acceptValue = amt;
        if (provider != null) provider.provide(this);           // the provider will immediately call accept(...)
        acceptValue = OFF;
        }

    public boolean accept(Provider provider, Resource res, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (!typical.isSameType(res)) throwUnequalTypeException(res);

        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast))
            throwInvalidAtLeastAtMost(atLeast, atMost);

        if (acceptValue < atLeast || acceptValue > atMost) return false;                // Also if it's == OFF, which is -1

        if (res instanceof CountableResource) 
            {
            resource.increase(acceptValue);
            ((CountableResource) res).decrease(acceptValue);
            if (getOffersImmediately()) offerReceivers(); 
            return true;
            }
        else
            {
            entities.add((Entity)res);
            if (getOffersImmediately()) offerReceivers(); 
            return true;
            }
        }

    public String toString()
        {
        return "Extractor@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ")";
        }               

    /** Returns Double.POSITIVE_INFINITY, which means nothing: Extractors do not have a capacity. */
    public double getCapacity() { return Double.POSITIVE_INFINITY; }
    public boolean hideCapacity() { return true; }
    
    /** Does nothing. */
    public void setCapacity(double d) 
        { 
        super.setCapacity(d);
        capacity = Double.POSITIVE_INFINITY;            // reset to default
        }
        
    boolean refusesOffers = false;
    public void setRefusesOffers(boolean value) { refusesOffers = value; }
    public boolean getRefusesOffers() { return refusesOffers; }
    }
        
