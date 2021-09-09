/** 
	A resource sink. 
*/

import sim.engine.*;
import java.util.*;


public class Sink implements Receiver
	{
	SimState state;
	Resource typical;
	
	void throwUnequalTypeException(Resource resource)
		{
		throw new RuntimeException("Expected resource type " + this.typical.getName() + "(" + this.typical.getType() + ")" +
			 	" but got resource type " + resource.getName() + "(" + resource.getType() + ")" );
		}

	public Sink(SimState state, Resource typical)
		{
		this.state = state;
		this.typical = typical;
		}

	public void step(SimState state)
		{
		}

	public void accept(Provider provider, Resource amount, double atLeast, double atMost)
		{
		if (!typical.isSameType(amount)) throwUnequalTypeException(amount);
		amount.clear();			// accept all of it
		}
	}