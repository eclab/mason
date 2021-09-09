/**
	A non-blocking receiver of resources.  
*/

public interface Receiver
	{
	/**
		Modifies the amount and returns the amount actually accepted.
	*/
	public void accept(Provider provider, Resource amount, double atLeast, double atMost);
	}