/**
	A non-blocking provider of resources.
*/

public interface Provider
	{
	public Resource provides();
	public Resource provide(double atLeast, double atMost);
	public Resource provide();		// same as provide(1, 1);
	public double available();
	}