import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
   A non-blocking receiver of resources.  
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
