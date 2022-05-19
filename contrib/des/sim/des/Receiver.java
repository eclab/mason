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
   A receiver of resources.  Receivers can ACCEPT offers from providers (or refuse them).
   Receivers can also register themselves with providers to be informed of offers.
*/

public interface Receiver extends Named, Resettable
    {
    /**
       Offers a resource from a Provider to a Receiver.
                
       <p>If the resource is a COUNTABLE or UNCOUNTABLE resource of some kind,
       The provider may respond by removing between atLeast and atMost, inclusive,
       from the given amount, and returning TRUE, or returning FALSE if it refuses
       the offer.
                
       <p>If the resource is an ENTITY of some kind,
       The provider may respond by taking the entity and returning TRUE, 
       or returning FALSE if it refuses the offer.
       
       <p>May throw a RuntimeException if the resource does not
       match the typical resource of the receiver, or if a cycle was detected in accepting
       offers (A offers to B, which offers to C, which then offers to A).
       At present does not check that atLeast and atMost are valid.
        
       <p>It must be the case that 0 &lt;= atLeast &lt; = atMost &lt;= resource.getAmount(), 
       or else a RuntimeException may be thrown. 
    */
    public boolean accept(Provider provider, Resource resource, double atLeast, double atMost);

    /** Returns the typical kind of resource the receiver can accept. 
        When a Receiver is also a Provider, this is very often implemented
        simply by calling getTypicalProvided() */
    public Resource getTypicalReceived();
    
    /** Sets whether the receiver currently refuses all offers.  The default should be FALSE. */
    public void setRefusesOffers(boolean value);

    /** Returns whether the receiver currently refuses all offers.  The default should be FALSE. */
    public boolean getRefusesOffers();
    }
