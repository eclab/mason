/** 
	An object (typically a Steppable) which can be Blocked.  Typically this
	increments (and unblocking decrements) an internal block count in the object.
	The object then decides to do any work during its step() based on whether it
	is blocked or not.  You will probably want to just subclass the BlockableAgent
	class instead.
*/


public interface Blockable extends Receiver
	{
	public void block(BlockingProvider blockingProvider);

	public void unblock(BlockingProvider blockingProvider);
	}