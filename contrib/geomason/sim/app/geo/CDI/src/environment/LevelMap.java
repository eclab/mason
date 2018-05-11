package CDI.src.environment;

public class LevelMap {

	public int levelMin;
	public int levelMax;
	public double magnitudeMin;
	public double magnitudeMax;
	
	public LevelMap(int levelMin, int levelMax, double magnitudeMin, double magnitudeMax) {
		this.levelMin = levelMin;
		this.levelMax = levelMax;
		this.magnitudeMin = magnitudeMin;
		this.magnitudeMax = magnitudeMax;
	}
	
	
	public int getLevel(double val) {
		if(val < 0)
			return 0;
		
		if(val >= magnitudeMax)
			return this.levelMax;
		else if(val <= magnitudeMin)
			return this.levelMin;
		else {
			return (int)Math.floor((val-magnitudeMin)/(magnitudeMax-magnitudeMin)*(levelMax-levelMin)+levelMin);
		}
	}

}
