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
   See Middleman as an abstract Receiver+Provider, and Filter as a common abstract
   subclass of Middleman.
*/

public interface Receiver extends Parented
    {
    /**
       Offers a resource from a Provider to a Receiver.
                
       <p>If the resource is a COUNTABLE or UNCOUNTABLE resource of some kind,
       The receiver may respond by removing between atLeast and atMost, inclusive,
       from the given amount, and returning TRUE, or returning FALSE if it refuses
       the offer.
                
       <p>If the resource is an ENTITY of some kind,
       The receiver may respond by taking the entity and returning TRUE, 
       or returning FALSE if it refuses the offer.  atLeast and atMost may be ignored,
       but generally atLeast should be 0 and atMost should be 1.
       
       <p>May throw a RuntimeException if the resource does not
       match the typical resource of the receiver, or if a cycle was detected in accepting
       offers (A offers to B, which offers to C, which then offers to A).
       At present does not check that atLeast and atMost are valid.
        
       <p>It must be the case that 0 &lt;= atLeast &lt; = atMost &lt;= resource.getAmount(), 
       or else a RuntimeException may be thrown. 
       
       <p>Receivers must never accept 0 of any resource.  Thus if atLeast = 0, then this has
       a special meaning: it means that the receiver must accept &gt; atLeast, rather than
       &gt;= atLeast. Similarly, Providers should never provide atMost=0.
    */
    public boolean accept(Provider provider, Resource resource, double atLeast, double atMost);
    
    /** Returns the typical kind of resource the receiver can accept. 
        When a Receiver is also a Provider, this is very often implemented
        simply by calling getTypicalProvided().  If (rarely) the Receiver 
        may receive a variety of types, such as a Composer, then this method should return null. 
    */
    public Resource getTypicalReceived();
    
    /** Sets whether the receiver currently refuses all offers.  The default should be FALSE. */
    public void setRefusesOffers(boolean value);

    /** Returns whether the receiver currently refuses all offers.  The default should be FALSE. */
    public boolean getRefusesOffers();

    /** Returns the total amount of received (and accepted) resource */
    // Implement this as 
    // return totalReceivedResource;
    public double getTotalReceivedResource();
    
    /** Returns the received (and accepted) resource rate. */
    // Implement this as 
    // double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalReceivedResource / time;
    public double getReceiverResourceRate();
    
    /** Resets the received (and accepted) resource amount to 0, among other possible things. */
    public void reset(SimState state);
    }
