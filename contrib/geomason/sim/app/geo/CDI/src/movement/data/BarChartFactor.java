package CDI.src.movement.data;





public class BarChartFactor {
	public enum Factor
	{
		UNKNOWN,
		TEMPERATURE,
		ELEVATION,
		PORT,
		RIVER,
		SOCIAL,
		OPPORTUNITY
	};
	
	public Factor factor;
	
	public BarChartFactor()
	{
		this.factor = Factor.UNKNOWN;
	}
	
	public BarChartFactor(Factor f)
	{
		this.factor = f;
	}
	
	public String toString()
	{
		return this.factor.name();
	}
	
	public boolean equals(Object obj)
	{
		Factor factor = (Factor)obj;
		if(factor.equals(this.factor))
			return true;
		return false;
	}
}
