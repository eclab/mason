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

    void throwInvalidMinMax(Resource min, double max)
        {
        throw new RuntimeException("Resource " + min + " has a minimum of " + min.getAmount() + " but a maximum of " + max + ", which is not permittedReceived.");
        }

    void throwInvalidEntityMax(Entity e, double max)
        {
        throw new RuntimeException("Resource " + e + " is an entity but has a real-valued maximum " + max);
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
        consists of resources with the given minimums and maximums.  If a resource is an
        entity, and its maximum (which must be an integer) is larger than 1, this 
        indicates that you want more than one of this entity present in the composition. 
        
        <p>Throws a RuntimeException if there is a duplicate among the provided resources, or
        if a minimum is > its maximum, or if a resource is an Entity but its maximum is
        not an integer.
    */
    public Composer(SimState state, Entity typicalProvided, Resource[] minimums, double[] maximums)
        {
        super(state, typicalProvided);
        mappedTotals = new HashMap<Integer, Node>();
        totals = new Node[minimums.length];
        permittedReceived = new Resource[minimums.length];
        
        for(int i = 0; i < minimums.length; i++)
            {
            permittedReceived[i] = minimums[i].duplicate();
            permittedReceived[i].clear();
            
            if (mappedTotals.get(minimums[i].getType()) != null)  // uh oh, already have one!
                throwDuplicateType(minimums[i]);
            else if (minimums[i].getAmount() < 0 || maximums[i] < minimums[i].getAmount() || 
                maximums[i] != maximums[i] || minimums[i].getAmount() != minimums[i].getAmount())
                {
                throwInvalidMinMax(minimums[i], maximums[i]);
                }
            else if (minimums[i] instanceof Entity && maximums[i] != (int)maximums[i])  // it's not an integer
                {
                throwInvalidEntityMax((Entity)minimums[i], maximums[i]);
                }
            else
                {
                Resource res = minimums[i].duplicate();
                res.clear();            // so it's 0.0 if a CountableResource
                Node node = new Node(res, minimums[i].getAmount(), maximums[i]);
                mappedTotals.put(minimums[i].getType(), node);
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
            else return false;
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
        
    void deploy()
        {
        // have we met the minimum counts yet?
        for(int i = 0; i < totals.length; i++)
            {
            if (!totals[i].getMeetsMinimum()) // crap, failed
                return;
            }
                
        // do a load into entities
        entities.clear();
        Entity entity = (Entity)(getTypicalProvided().duplicate());
        Resource[] resources = new Resource[totals.length];
        for(int i = 0; i < totals.length; i++)
            resources[i] = totals[i].resource.duplicate();
        entity.setStorage(resources);
        setInfoFor(entity);
        entities.add(entity);
        
        resetTotals();   
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
        return "Composer@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : (getName() + ": ")) + getTypicalProvided().getName() + " -> " + getTypicalReceived().getName() + ")";
        }

    /** If stepped, offers the composed entity if it is ready. */
    public void step(SimState state)
        {
        deploy();
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
