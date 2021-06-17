package sim.display;

import java.io.Serializable;

public class Stat implements Serializable
{
	private static final long serialVersionUID = 1L;

	public Serializable data;
	public long steps;

	public Stat(Serializable _data, long _steps)
	{
		data = _data;
		steps = _steps;
	}
}
