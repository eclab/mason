/**
	A non-blocking receiver of resources.  
*/

public interface Receiver
	{
	/**
		An offer to accept up to a certain amount from a provider.
		This amount must be equal to provider.available() and
		ought to be > 0.
	*/
	public void consider(Provider provider, double amount);
	}