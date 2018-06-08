package environment;



import sim.portrayal.grid.FastValueGridPortrayal2D;


public class SmartFastValueGridPortrayal2D extends FastValueGridPortrayal2D{
	public void updateBounds(double minLevel, double maxLevel)
	{		
		((SmartColorMap)this.getMap()).setBoundsMinMax(minLevel, maxLevel);
	}
	
	/**
	 * Set the bounds of the SmartColorMap
	 * @param bounds the first element is the minLevel, the second element is the maxLevel
	 */
	public void updateBounds(double[] bounds)
	{
		((SmartColorMap)this.getMap()).setBoundsMinMax(bounds[0], bounds[1]);
	}
	
	
	
}
