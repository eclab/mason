package sim.display;

import java.io.Serializable;

public class Stat implements Serializable
{
	private static final long serialVersionUID = 1L;

	public Serializable data;
	public long steps;
	public double time;

	public Stat(Serializable _data, long _steps, double _time)
	{
		data = _data;
		steps = _steps;
		time = _time;
	}
}
