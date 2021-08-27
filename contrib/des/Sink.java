/** 
	A resource sink. 
*/

import sim.engine.*;
import java.util.*;


public class Sink implements Receiver
	{
	SimState state;
	Resource typical;
	Provider provider;
	
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

	public Provider getProvider() { return provider; }
	public void setProvider(Provider provider) { this.provider = provider; }
	public boolean isPulling() { return getProvider() != null; }

	public void step(SimState state)
		{
		if (provider != null) 
			{
			Resource token = provider.provide(0, Double.POSITIVE_INFINITY);
			if (!typical.isSameType(token)) throwUnequalTypeException(token);
			}
		}

	public boolean consider(Provider provider, double amount)
		{
		Resource providedType = provider.provides();
		if (!typical.isSameType(providedType)) throwUnequalTypeException(providedType);
		Resource token = provider.provide(0, amount);
		if (token != null)
			{
			return true;
			}
		return false;
		}
		
	public boolean receive(Provider provider, Resource token)
		{
		return true;
		}
	}