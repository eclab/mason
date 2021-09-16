import sim.engine.*;
import sim.util.*;
import java.util.*;

/**
	A non-blocking receiver of resources.  
*/

public interface Receiver extends Named
	{
	/**
		Modifies the amount and returns the amount actually accepted.
	*/
	public void accept(Provider provider, Resource amount, double atLeast, double atMost);
	}