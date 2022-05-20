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

	ArrayList<Provider> providers = new ArrayList<Provider>();

    /** 
        Registers a provider with the Extractor.  Returns false if the receiver was already registered.
    */
    public boolean addProvider(Provider provider)
        {
        if (providers.contains(provider)) return false;
        providers.add(provider);
        return true;
        }
                                
    /** 
        Returns all registered providers.
    */
    public ArrayList<Provider> getProviders()
        {
        return providers;
        }
                                
    /** 
        Unregisters a provider with the Extractor.  Returns false if the provider was not registered.
    */
    public boolean removeProvider(Provider provider)
        {
        if (providers.contains(provider)) return false;
        providers.remove(provider);
        return true;
        }

    
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
        addProvider(provider);
        }
                               
                               
    boolean offersImmediately = true;
   
   
    /** Request Policy: requests are made to the first provider, then the second, and so on. */
    public static final int REQUEST_POLICY_FORWARD = 0;
    /** Request Policy: requests are made to the last provider, then the second to last, and so on. */
    public static final int REQUEST_POLICY_BACKWARD = 1;	
    /** Request Policy: requests are made to the providers in a randomly shuffled order. */
    public static final int REQUEST_POLICY_SHUFFLE = 2;
    /** Request Policy: requests are made to only one random provider, chosen via an offer distribution or, if the offer distribution is null, chosen uniformly. */
    public static final int REQUEST_POLICY_RANDOM = 3;
    /** Request Policy: requests are made to only one provider, chosen via selectProvider. */
    public static final int REQUEST_POLICY_SELECT = 4;
    int requestPolicy = REQUEST_POLICY_FORWARD;
    AbstractDiscreteDistribution requestDistribution;

    public static final int REQUEST_TERMINATION_EXHAUST = 0;
    public static final int REQUEST_TERMINATION_FAIL = 1;
    public static final int REQUEST_TERMINATION_SUCCEED = 2;
    int requestTermination = REQUEST_TERMINATION_EXHAUST;

	public void setRequestPolicy(int requestPolicy)
		{
		if (requestPolicy < REQUEST_POLICY_FORWARD || requestPolicy > REQUEST_POLICY_SELECT)
            throw new IllegalArgumentException("Request Policy " + requestPolicy + " out of bounds.");
        this.requestPolicy = requestPolicy;
		}
		
	public int getRequestPolicy() { return requestPolicy; }
	public boolean hideRequestPolicy() { return true; }

	public void setRequestTermination(int requestTermination)
		{
		if (requestTermination < REQUEST_TERMINATION_EXHAUST || requestTermination > REQUEST_TERMINATION_SUCCEED)
            throw new IllegalArgumentException("Request Termination " + requestTermination + " out of bounds.");
        this.requestTermination = requestTermination;
		}
		
	public int getRequestTermination() { return requestTermination; }
	public boolean hideRequestTermination() { return true; }
        
    boolean requesting;
    /** Returns true if the Extractor is currently requesting an offer(this is meant to allow you
        to check for offer cycles. */
    protected boolean isRequesting() { return requesting; }
    protected boolean requestProviders(double amt)
    	{
    	requesting = true;
    	boolean result = false;
    	
    	switch(requestPolicy)
    		{
    		case REQUEST_POLICY_FORWARD:
    			{
                for(Provider p : providers)
                    {
                    boolean r = p.provide(this, amt);
                    result = result || r;
                    if (r && requestTermination == REQUEST_TERMINATION_SUCCEED) break;
                    if ((!r) && requestTermination == REQUEST_TERMINATION_FAIL) break;
                    }
    			}
    		break;
    		case REQUEST_POLICY_BACKWARD:
    			{
                for(int i = providers.size() - 1; i >= 0; i--)
                    {
                    Provider p = providers.get(i);
                    boolean r = p.provide(this, amt);
                    result = result || r;
                    if (r && requestTermination == REQUEST_TERMINATION_SUCCEED) break;
                    if ((!r) && requestTermination == REQUEST_TERMINATION_FAIL) break;
                    }
    			}
    		break;
    		case REQUEST_POLICY_SHUFFLE:
    			{
                shuffleProviders();
                while(true)
                    {
                    Provider p = nextShuffledProvider();
                    if (p == null) break;		// all done
                    boolean r = p.provide(this, amt);
                    result = result || r;
                    if (r && requestTermination == REQUEST_TERMINATION_SUCCEED) break;
                    if ((!r) && requestTermination == REQUEST_TERMINATION_FAIL) break;
                    }
    			}
    		break;
            case REQUEST_POLICY_RANDOM:
                {
                int size = providers.size();
                if (size == 0) 
                    break;
                
                if (requestDistribution == null)  // select uniformly
                    {
                    Provider p = providers.get(state.random.nextInt(size));
                    result = p.provide(this, amt);
                    }
                else
                    {
                    int val = requestDistribution.nextInt();
                    if (val < 0 || val >= size )
                        {
                        if (!requestDistributionWarned)
                            {
                            new RuntimeException("Warning: Request distribution for Extractor " + this + " returned a value outside the Provider range: " + val).printStackTrace();
                            requestDistributionWarned = true;
                            }
                        result = false;
                        }
                    else
                        {
                        result = providers.get(val).provide(this, amt);
                        }
                    }
                }
            break;
            case REQUEST_POLICY_SELECT:
                {
                int size = providers.size();
                if (size == 0) 
                    {
                    if (!requestSelectWarned)
                        {
                        new RuntimeException("Warning: Request policy is SELECT but there are no providers to select from in " + this).printStackTrace();
                        requestSelectWarned = true;
                        }
                    }
                else
                    {
                    Provider p = selectProvider(providers);
                    if (p == null) break;
                    p.provide(this, amt);
                    }
                }
            break;
    		}
    	
    	requesting = false;
    	return result;
    	}    

    /**
       If the provider policy is REQUEST_POLICY_SELECT, then when the providers are non-empty,
       this method will be called to specify which provider should be asked to offer a resource.
       Override this method as you see fit.  The default implementation simply returns the first one.
    */
    public Provider selectProvider(ArrayList<Provider> providers)
    	{
    	return providers.get(0);
    	}

    boolean requestDistributionWarned = false; 
    boolean requestSelectWarned = false; 
 
     //// SHUFFLING PROCEDURE
    //// You'd think that shuffling would be easy to implement but it's not.
    //// We want to avoid an O(n) shuffle just to (most likely) select the
    //// very first receiver.  So this code attemps to do shuffling on the
    //// fly as necessary.
        
    int currentShuffledProvider;
    /** 
        Resets the receiver shuffling
    */
    void shuffleProviders() 
        {
        currentShuffledProvider = 0;
        }
                
    /** 
        Returns the next provider in the shuffling
    */
    Provider nextShuffledProvider()
        {
        int size = providers.size();
        if (currentShuffledProvider == size) 
            return null;
        int pos = state.random.nextInt(size - currentShuffledProvider) + currentShuffledProvider;

        Provider ret = providers.get(pos);
        providers.set(pos, providers.get(currentShuffledProvider));
        providers.set(currentShuffledProvider, ret);
                
        currentShuffledProvider++;
        return ret;
        }
       
    Receiver distinguishedReceiver = null;
    
    public boolean provide(Receiver receiver)
        {
        return provide(receiver, Double.POSITIVE_INFINITY);
        }

    public boolean provide(Receiver receiver, double atMost)
        {
        if (requesting) return false;		// break cycles
        
        if (!isPositiveNonNaN(atMost))
            throwInvalidNumberException(atMost);
        distinguishedReceiver = receiver;
        boolean val = requestProviders(atMost);
        if (val)
        	{
        	if (entities != null)
        		{        		
        		val = accept(this, resource, 0, atMost);
        		}
        	else
        		{
        		val = accept(this, entities.getFirst(), 0, atMost);
        		}
        	}
        entities.clear();
        resource.setAmount(0);
        distinguishedReceiver = null;
        return val;
        }
    
    static final int OFF = -1;
    double acceptValue = OFF;
    
    /** Builds a single entity, ignoring the amount passed in, by asking the provider to provide it.  */
    protected void buildEntities(double amt)
        {
        acceptValue = 1;                        // we always just grab ONE, not multiple entities
        requestProviders(amt);
        acceptValue = OFF;
        }
        
    /** Builds resource by asking the provider to provide it.  */
    protected void buildResource(double amt)
        {
        acceptValue = amt;
        requestProviders(amt);
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
            if (distinguishedReceiver != null) offerReceivers(); 
            return true;
            }
        else
            {
            entities.add((Entity)res);
            if (distinguishedReceiver != null) offerReceivers(); 
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
        
