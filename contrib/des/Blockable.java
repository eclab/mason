/** 
	An object (typically a Steppable) which can be Blocked.  Typically this
	increments (and unblocking decrements) an internal block count in the object.
	The object then decides to do any work during its step() based on whether it
	is blocked or not.  You will probably want to just subclass the BlockableAgent
	class instead.
*/


public interface Blockable extends Receiver
	{
	/** Informs the Blockable that it has been put on the block list of the given BlockingProvider. */
	public void block(BlockingProvider blockingProvider);

	/** Informs the Blockable that it has been removed from the block list of the given BlockingProvider
		because the BlockingProvider can now provide up to the given amount of resource requested.  
		The Blockable must call provide(...) on the BlockingProvider during this unblock() method
		to receive the amount, else it forfeits it. */
	public void unblock(BlockingProvider blockingProvider, double amount);
	}