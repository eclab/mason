/**
	A non-blocking provider of resources.
*/

public abstract class Provider
	{
	ArrayList<Receiver> receivers;

	public boolean registerReceiver(Receiver receiver)
		{
		if (receivers.contains(receiver)) return false;
		receivers.add(receiver);
		return true;
		}
		
	public boolean unregisterReceiver(Receiver receiver)
		{
		if (receivers.contains(receiver)) return false;
		receivers.remove(receiver);
		return true;
		}

	public abstract Resource provides();
	public abstract Resource provide(double atLeast, double atMost);
	public abstract double available();

	public Resource provide()
		{
		return provide(1.0, 1.0);
		}
	}