/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;

/**
   A composer composes multiple resources received from a provider into a single Entity to offer to
   downstream receivers.  To do this you specify which entities must be present in the composition, 
   and how much (min, max) of each.  When the composer can meet the minimums, it will build the entity 
   and send it on immediately upon receiving the last required resource, if getOffersImmediately() 
   is true (by default it is). Othewise it will wait until step() is called.  Thus you only need 
   (and only should) schedule the Composer if you have turned off setOffersImmediately(false).
**/

public class Composer extends Middleman
    {
    public SimplePortrayal2D buildDefaultPortrayal(double scale)
        {
        return new ShapePortrayal2D(ShapePortrayal2D.POLY_STAR, 
            getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
        }

    private static final long serialVersionUID = 1;

    void throwDuplicateType(Resource res)
        {
        throw new RuntimeException("Resource " + res + " may not be provided multiple times with identical types.");
        }

    void throwNotComposableResource(Resource res)
        {
        throw new RuntimeException("Provided resource " + res + " is not among the ones listed as valid for composition by this Composer.");
        }

    void throwInvalidMinMax(Resource res, double min, double max)
        {
        throw new RuntimeException("Resource " + res + " has a minimum of " + min + " but a maximum of " + max + ", which is not permittedReceived.");
        }

    void throwInvalidEntityMax(Entity e, double max)
        {
        throw new RuntimeException("Resource " + e + " is an entity but must have an integer maximum.  Maximum is presently " + max);
        }
        
    void throwInvalidEntityMin(Entity e, double min)
        {
        throw new RuntimeException("Resource " + e + " is an entity but must have an integer minimum >= 1.  Minimum is presently " + min);
        }
        
    void throwUnknownResource(Resource res)
        {
        throw new RuntimeException("Resource " + res + " is not used by this Composer.");
        }
        
    class Node
        {
        // The resource we'll send out at the end of the day.  We fill up its amount.
        CountableResource resource = null;
        Entity[] entity = null;
        // The maximum amount we fill up
        double maximum;
        // The minimum amount we fill up
        double minimum;
        int entityCount = 0;
        
        // Compute the space left for this resource
        double spaceLeft()
            {
            if (entity != null)
                {
                return maximum - entityCount;
                }
            else
                {
                return maximum - resource.getAmount();
                }
            }
                
        // Compute the space left for this resource
        boolean getMeetsMinimum()
            {
            if (entity != null)
                {
                return minimum <= entityCount;
                }
            else
                {
                return minimum <= resource.getAmount();
                }
            }
                
        Node(Resource resource, double minimum, double maximum)
            {
            this.maximum = maximum;
            this.minimum = minimum;
            if (resource instanceof CountableResource)
                {
                this.resource = (CountableResource)resource;
                }
            else
                {
                entity = new Entity[(int)maximum];
                this.entityCount = 0;
                }
            }
        }
    
    boolean offersImmediately = true;
    
    /** Returns whether the Composer offers Entities immediately in zero time upon accepting the last resource
        necessary to build them, as opposed to only when it is stepped. The default is TRUE.  */
    public boolean getOffersImmediately() { return offersImmediately; }

    /** Sets whether the Composer offers Entities immediately in zero time upon accepting the last resource
        necessary to build them, as opposed to only when it is stepped. The default is TRUE.   */
    public void setOffersImmediately(boolean val) { offersImmediately = val; }

    // This is a mapping of types to total-nodes
    HashMap<Integer, Node> mappedTotals;
    
    Resource[] permittedReceived;
    
    // This is the same set of total-nodes organized as an array for faster scanning
    Node[] totals;
    
    /** Builds a composer which outputs composite entities of the given type.  Each entity
        consists of a set of resources each with a minimum and a maximum.  If a resource is an
        entity, its minimum and maximum must be integers >= 1. 
        
        <p>Throws a RuntimeException if there is a duplicate among the provided resources, or
        if a minimum is > its maximum, or if a resource is an Entity but its minimum or maximum is
        not an integer.
    */
    public Composer(SimState state, Entity typicalProvided, Resource[] resources, double[] minimums, double[] maximums)
        {
        super(state, typicalProvided);
        if (resources == null) throw new RuntimeException("resources cannot be null.");
        if (minimums == null) throw new RuntimeException("minimums cannot be null.");
        if (maximums == null) throw new RuntimeException("minimums cannot be null.");
        if (resources.length != minimums.length) throw new RuntimeException("resources and minimums must have the same length.");
        if (resources.length != maximums.length) throw new RuntimeException("resources and maximums must have the same length.");
        setup(typicalProvided, resources, minimums, maximums);
        }

    /** Builds a composer which outputs composite entities of the given type.  Each entity
        consists of a set of resources each with a minimum and a maximum.  The minimum value of
        a resource is its amount (Entities always have an amount of 1).  If a resource is an entity, 
        its maximum must be an integer >= 1. 
        
        <p>Throws a RuntimeException if there is a duplicate among the provided resources, or
        if a minimum is > its maximum, or if a resource is an Entity but its minimum or maximum is
        not an integer.
    */
    public Composer(SimState state, Entity typicalProvided, Resource[] resources, double[] maximums)
    	{
        super(state, typicalProvided);
        if (resources == null) throw new RuntimeException("minimums cannot be null.");
        if (maximums == null) throw new RuntimeException("minimums cannot be null.");
        if (resources.length != maximums.length) throw new RuntimeException("resources and maximums must have the same length.");

		double[] minimums = new double[resources.length];
		for(int i = 0; i < minimums.length; i++) minimums[i] = resources[i].getAmount();
        setup(typicalProvided, resources, minimums, maximums);
    	}
    	
    	
    void setup(Entity typicalProvided, Resource[] resources, double[] minimums, double[] maximums)
        {
        mappedTotals = new HashMap<Integer, Node>();
        totals = new Node[resources.length];
        permittedReceived = new Resource[resources.length];
        
        for(int i = 0; i < resources.length; i++)
            {
            permittedReceived[i] = resources[i].duplicate();
            permittedReceived[i].clear();
            
            if (mappedTotals.get(resources[i].getType()) != null)  // uh oh, already have one!
                {
                throwDuplicateType(resources[i]);
                }
            else if (minimums[i] < 0 || maximums[i] < minimums[i] || maximums[i] != maximums[i] || minimums[i] != minimums[i])
                {
                throwInvalidMinMax(resources[i], minimums[i], maximums[i]);
                }
            else if (resources[i] instanceof Entity && maximums[i] != (int)maximums[i])  // it's not an integer
                {
                throwInvalidEntityMax((Entity)resources[i], maximums[i]);
                }
            else if (resources[i] instanceof Entity && (minimums[i] != (int) minimums[i] || minimums[i] < 1))  // it's not an integer >= 1
                {
                throwInvalidEntityMin((Entity)resources[i], minimums[i]);
                }
            else
                {
                Resource res = resources[i].duplicate();
                res.clear();            // so it's 0.0 if a CountableResource
                Node node = new Node(res, minimums[i], maximums[i]);
                mappedTotals.put(resources[i].getType(), node);
                totals[i] = node;
                }
            }
        }
                                
    public boolean accept(Provider provider, Resource amount, double atLeast, double atMost)
        {
        if (getRefusesOffers()) { return false; }
        if (isOffering()) throwCyclicOffers();  // cycle
        
        if (!(atLeast >= 0 && atMost >= atLeast && atMost > 0 && atMost <= amount.getAmount()))
            throwInvalidAtLeastAtMost(atLeast, atMost, amount);
       
        // Find the appropriate node
        Node total = mappedTotals.get(amount.getType());
        if (total == null) throwNotComposableResource(amount);
        
        if (total.entity != null)
            {
            if (total.maximum - total.entityCount > 0)
                {
                total.entity[total.entityCount++] = (Entity)amount;
                if (getOffersImmediately()) deploy();
                return true;
                }
            else 
                {
                return false;
                }
            }
        else
            {
            CountableResource res = (CountableResource)(total.resource);
            if (total.maximum - res.getAmount() < atLeast)  // cannot accept
                return false;
            else
                {
                double amt = Math.min(Math.min(total.maximum - res.getAmount(), atMost), ((CountableResource)amount).getAmount());
                res.increase(amt);
                ((CountableResource)amount).decrease(amt);
                if (getOffersImmediately()) deploy();
                return true;
                }
            }
        }
        
    /** Rescinds some of the Resource gathered so far.  This would normally be done if for some reason an
		upstream Provider needs to claw back some Resource it had previously offered to the Composer.
		This is done as follows.  First, it finds the given Resource, by type, among those in the Composer's stock.
    	If there is no such Resource, throws an error.  Otherwise, if the Resources is
    	a CountableResource, reduces the Composer's stock of the given Resource by up to 
    	resource.getAmount() and returns it as a new Resource.  If EXACTLY is true, then the 
    	Composer's stock must contain at least resource.getAmount() -- and so exactly
    	resource.getAmount() can be returned -- else null is returned.  
    	
    	<p>If the Resource instead is an Entity, then if the Composer has at least one 
    	Entity of this type, then one Entity is removed from the stock and returned.  If 
    	the Composer has no stock, then null is returned.  EXACTLY is ignored.
    */
    public Resource rescind(Resource resource, boolean exactly)
    	{
    	// find the resource list
    	for(int i = 0; i < permittedReceived.length; i++)
    		{
    		if (permittedReceived[i].isSameType(resource))
    			{
    			// found it!
    			if (totals[i].entity != null)		// it's an entity
    				{
    				if (totals[i].entityCount > 0)	// we have one to rescind
    					{
    					totals[i].entityCount--;
    					return totals[i].entity[totals[i].entityCount];	// the top one before we reduced
    					}
    				else return null;					// couldn't provide one
    				}
    			else
    				{
    				double amt = totals[i].resource.getAmount();
    				if (amt >= resource.getAmount())	// we can rescind the whole amount requested
    					{
    					return totals[i].resource.reduce(resource.getAmount());
    					}
    				else if (amt > 0 && !exactly)		// we can rescind SOME of the amount requested
    					{
    					return totals[i].resource.reduce(amt);
    					}
    				else return null;
    				}
    			}
    		}
		throwUnknownResource(resource);
		return null;			// never happens
    	}
    	
    /** Returns the amount of Resource, by type, the Composer has in stock.  If the Composer
    	doesn't use the provided Resource, an exception is thrown. */
    public double getAmount(Resource resource)
    	{
    	// find the resource list
    	for(int i = 0; i < permittedReceived.length; i++)
    		{
    		if (permittedReceived[i].isSameType(resource))
    			{
    			// found it!
    			if (totals[i].entity != null)		// it's an entity
    				{
    				return totals[i].entityCount;
    				}
    			else
    				{
    				return totals[i].resource.getAmount();
    				}
    			}
    		}
		throwUnknownResource(resource);
		return -1;			// never happens
    	}
    	
    /** Returns the amount of Resource in stock for each Resource used by the Composer.
    	The order of the returned array is the same as the order of the Resources
    	returned by getPermittedReceived(). */
    public double[] getAmounts()
    	{
    	double[] amts = new double[totals.length];
    	for(int i = 0; i < amts.length; i++)
    		{
    		if (totals[i].entity != null)
    			{
    			amts[i] = totals[i].entityCount;
    			}
    		else
    			{
    			amts[i] = totals[i].resource.getAmount();
    			}
    		}
    	return amts;
    	}

    protected void deploy()
        {
        // have we met the minimum counts yet?
        for(int i = 0; i < totals.length; i++)
            {
            if (!totals[i].getMeetsMinimum()) // crap, failed
                return;
            }
                
        // do a load into entities
        if (entities.isEmpty())
            {
            Entity entity = (Entity)(getTypicalProvided().duplicate());
            ArrayList<Resource> resources = new ArrayList<>();
            for(int i = 0; i < totals.length; i++)
                {
                if (totals[i].entity != null)
                    {
                    for(int j = 0; j < totals[i].entityCount; j++)
                        {
                        resources.add(totals[i].entity[j]);
                        }
                    }
                else
                    {
                    resources.add(totals[i].resource.duplicate());
                    }
                }
            entity.setStorage((Resource[])(resources.toArray(new Resource[0])));
            setInfoFor(entity);
            entities.add(entity);
            resetTotals();
            offerReceivers();
            }               
        }
        
    /** This is called when the Composer constructs a composite entity
        and now must set its Info object, if there is one.  Override
        this as you like set it to an appropriate Info object if any.
        By default this does nothing. */
    protected void setInfoFor(Entity entity)
        {
        }
    

    void resetTotals()
        {
        // reset totals
        for(int i = 0; i < totals.length; i++)
            {
            totals[i].entityCount = 0;
            if (totals[i].resource != null)
                {
                totals[i].resource.clear();
                }
            }
        }
                
    public void clear()
        {
        super.clear();
        resetTotals();
        }
        
    public String toString()
        {
        return "Composer@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + ")";
        }

    /** If stepped, offers the composed entity if it is ready. */
    public void step(SimState state)
        {
        if (entities.isEmpty())
            {
            deploy();
            }
        else
            {
            offerReceivers();
            }
        }

    /** Returns NULL because various resource types are received.
        To get the full list of legal received resource types, call getPermittedReceived() */
    public Resource getTypicalReceived() 
        { 
        return null; 
        }

    /** Returns the full list of legal received resource types. */
    public Resource[] getPermittedReceived() 
        { 
        return permittedReceived;
        }

    }
