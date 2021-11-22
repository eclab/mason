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

public interface Receiver extends Named
    {
    /**
       Offers a resource from a Provider to a Receiver.
                
       <p>If the resource is a COUNTABLE or UNCOUNTABLE resource of some kind,
       The provider may respond by removing between atLeast and atMost, inclusive,
       from the given amount, and returning TRUE, or returning FALSE if it refuses
       the offer.
                
       <p>If the resource is an ENTITY of some kind,
       The provider may respond by taking the entitym and returning TRUE, 
       or returning FALSE if it refuses the offer.
    */
    public boolean accept(Provider provider, Resource resource, double atLeast, double atMost);
    }
