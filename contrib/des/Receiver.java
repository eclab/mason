/**
	A non-blocking receiver of resources.  
*/

public interface Receiver
	{
	/**
		An offer to accept up to a certain amount from a provider
	*/
	public boolean consider(Provider provider, double amount);
	}