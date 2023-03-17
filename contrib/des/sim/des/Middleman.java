/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
   An object which is both a Provider and a Receiver.   Includes a basic implementation
   of getRefusesOffers(), and by default has getTypicalReceived() return the same value
   as getTypicalProvided().  Because it is both a Provider and a Receiver, a Middleman 
   can also perform transactions with another Middleman object, providing a resource
   and simultaneously receiving a resource as a result.  To implement this, override the
   performTransaction() method.
*/

public abstract class Middleman extends Provider implements Receiver
    {
    private static final long serialVersionUID = 1;

    public Middleman(SimState state, Resource typical)
        {
        super(state, typical);
		}
		
	boolean refusesOffers;

    public void setRefusesOffers(boolean value)
    	{
    	refusesOffers = value;
    	}

    public boolean getRefusesOffers()
    	{
    	return refusesOffers;
    	}

    public Resource getTypicalReceived() 
    	{ 
    	return getTypicalProvided(); 
    	}
	
    /** Throws an exception indicating that the given resource does not match the Provider's typical provided resource. */
    protected void throwUnequalReceivedTypeException(Resource res)
        {
        throw new RuntimeException("Expected resource type " + this.getTypicalReceived().getName() + "(" + this.getTypicalReceived().getType() + ")" +
            " but got resource type " + res.getName() + "(" + res.getType() + ")" );
        }

    public abstract boolean accept(Provider provider, Resource resource, double atLeast, double atMost);
    
    /** Received by the Middleman when a Provider and Receiver are asking or a transaction of one resource for another.  
    	The Provider would provide a resource to the Middleman and a Receiver would receive the transacted returned Resource.
    	Very commonly this Provider and Receiver are one and the same: they are also a Middleman.    But this does not have to be the case.  	
		If the transaction is agreed to, you should modify the provided resource and return the requested resource.
		Otherwise, return null.  The default form simply returns null. 
		
		<p> By the time this method has been called, refuses-offers,
		cyclic, and type compatibility checks have already been performed, but you might still benefit from 
		knowing the requestedType, so it is provided: but you should not modify this resource nor return it.
				
		<p>The transaction is offering atLeast and atMost a certain amount of provided resouce in exchange for
    	(from you) a requested resource.  atLeastRequested is the amount of requested resource to be provided
    	in exchange for the *least* amount of provided resource.  If you decide to take some X provided resource
    	where X is between atLeast and atMost, then the resource amount you provide in return X * atLeastRequested / atMost.
    	For requested CountableResources, I suggest that the amount returned in response to a request would best be
    	(int)(X * atLeastRequested / atMost), but you can do as your model deems appropriate.
    	
    	<p>For Entities, only a single Entity can be provided.  If an Entity is being provided, then atLeast = atMost = 1.
    	
    	<p>For Entities, only a single Entity can be requested.  If an Entity is being requested, atLeastRequested = 1
    	and exactly one Entity should be returned regardless of its value. 
	    */
    	 
    protected Resource performTransaction(Provider provider, Receiver receiver, Resource provided, double atLeast, double atMost, Resource requestedType, double atLeastRequested)
    	{
    	return null;
    	}
    
    /** You may call this method in order to request a transaction of one resource for another.    	
    	The Provider would provide a resource to the Middleman and a Receiver would receive the transacted returned Resource.
    	Very commonly this Provider and Receiver are one and the same: they are also a Middleman.  But this does not have to be the case.  	
		If the transaction is agreed to, your provided resource will be accordingly modified (reduced) and the requested
		resource will have been provided.  Otherwise null will be returned.
		
		<p>The transaction is offering atLeast and atMost a certain amount of provided resouce in exchange for
    	(from you) a requested resource.  atLeastRequested is the amount of requested resource to be provided
    	in exchange for the *least* amount of provided resource.  If you decide to take some X provided resource
    	where X is between atLeast and atMost, then the resource amount you provide in return is X * atLeastRequested / atMost.
    	For requested CountableResources, I suggest that the amount returned in response to a request would best be
    	(int)(X * atLeastRequested / atMost), but you can do as your model deems appropriate.
    	
    	<p>For Entities, only a single Entity can be provided.  If an Entity is being provided, then atLeast = atMost = 1.
    	
    	<p>For Entities, only a single Entity can be requested.  If an Entity is being requested, atLeastRequested = 1
    	and exactly one Entity should be returned regardless of its value, and atLeast = atMost. 
    	
    	<p>Don't override this method.  Instead, override performTransaction().
	    */
    public Resource transact(Provider provider, Receiver receiver, Resource provided, double atLeast, double atMost, Resource requestedType, double atLeastRequested)
    	{
    	if (isOffering())
    		{
    		throwCyclicOffers();
    		}
		if (provided.getType() != getTypicalReceived().getType())
			{
			throwUnequalReceivedTypeException(provided);
			}
		if (requestedType.getType() != getTypicalProvided().getType())
			{
			throwUnequalTypeException(requestedType);
			}
		if (getRefusesOffers())
			{
			return null;
			}
			
		// bugs in the Java compiler prevent the if-statements above from being in the try statement below
		try
			{
	    	offering = true;
			return performTransaction(provider, receiver, provided, atLeast, atMost, requestedType, atLeastRequested);
			}
		finally
			{
			offering = false;
			}
    	}


    /** You may call this method in order to request a transaction of one resource for another.    	
    	The other Middleman would provide a resource to this Middleman and would receive the transacted returned Resource.
		If the transaction is agreed to, your provided resource will be accordingly modified (reduced) and the requested
		resource will have been provided.  Otherwise null will be returned.
		
		<p>The transaction is offering atLeast and atMost a certain amount of provided resouce in exchange for
    	(from you) a requested resource.  atLeastRequested is the amount of requested resource to be provided
    	in exchange for the *least* amount of provided resource.  If you decide to take some X provided resource
    	where X is between atLeast and atMost, then the resource amount you provide in return is X * atLeastRequested / atMost.
    	For requested CountableResources, I suggest that the amount returned in response to a request would best be
    	(int)(X * atLeastRequested / atMost), but you can do as your model deems appropriate.
    	
    	<p>For Entities, only a single Entity can be provided.  If an Entity is being provided, then atLeast = atMost = 1.
    	
    	<p>For Entities, only a single Entity can be requested.  If an Entity is being requested, atLeastRequested = 1
    	and exactly one Entity should be returned regardless of its value, and atLeast = atMost. 
    	
    	<p>Don't override this method.  Instead, override performTransaction().
	    */
    public Resource transact(Middleman middleman, Resource provided, double atLeast, double atMost, Resource requestedType, double atLeastRequested)
    	{
    	return transact(middleman, middleman, provided, atLeast,atMost, requestedType, atLeastRequested);
    	}
    }
