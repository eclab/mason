package optimization.desirability ;

import java.awt.Color;

import sim.util.gui.SimpleColorMap;
import sim.field.grid.DoubleGrid2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;

import environment.Map ;

public class DesirabilityMapsPortrayals
{
	private DesirabilityMaps dmaps ;

	public DesirabilityMapsPortrayals(DesirabilityMaps dmaps)
	{
        //Thread.dumpStack();
	    //System.exit(0);
		this.dmaps = dmaps ;
	}

	// Raw population data, green
	public FastValueGridPortrayal2D getPopulationPortrayal()
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D populationPortrayal = new FastValueGridPortrayal2D();
		populationPortrayal.setField(this.dmaps.getPopGrid());
		SimpleColorMap colorMap = new SimpleColorMap(DesirabilityMaps.POPVISIBILITY_MIN, 
				DesirabilityMaps.POPVISIBILITY_MAX, 
				new Color(0, 1, 0, 0), Color.GREEN);
		populationPortrayal.setMap(colorMap);
		return populationPortrayal;
	}
	
	// Smoothed population portrayal, green
	public FastValueGridPortrayal2D getSmoothedPopulationPortrayal()
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D smoothedPopulationPortrayal = new FastValueGridPortrayal2D();
		smoothedPopulationPortrayal.setField(this.dmaps.getSmoothedPopGrid());
		SimpleColorMap colorMap = new SimpleColorMap(DesirabilityMaps.POPVISIBILITY_MIN, 
				DesirabilityMaps.POPVISIBILITY_MAX, 
				new Color(0, 1, 0, 0), new Color(0f, 1f, 0f, 0.6f));
		smoothedPopulationPortrayal.setMap(colorMap);
		return smoothedPopulationPortrayal;
	}
	
	// Normalized population portrayal, green
	public FastValueGridPortrayal2D getNormalizedPopulationPortrayal()
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D normalizedPopulationPortrayal = new FastValueGridPortrayal2D();
		normalizedPopulationPortrayal.setField(this.dmaps.getNormalizedPopGrid());
		// Please note that the normalization is done only in canada. So, if you visualize 
		// the map with this normVisMin and normVisMax values, other countries may seem to 
		// be flooded with green dots, but we are ignoring them for the time being. 
		SimpleColorMap colorMap = new SimpleColorMap(DesirabilityMaps.NORM_POP_VIS_MIN, 
				DesirabilityMaps.NORM_POP_VIS_MAX, 
				new Color(0, 1, 0, 0), new Color(0f, 1f, 0f, 0.6f));
		normalizedPopulationPortrayal.setMap(colorMap);
		return normalizedPopulationPortrayal;
	}

	// Temperature desirability, red
	public FastValueGridPortrayal2D getTempDesPortrayal()
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D tempDesPortrayal = new FastValueGridPortrayal2D();
		tempDesPortrayal.setField(this.dmaps.getTempDes());
		SimpleColorMap colorMap = new SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), Color.RED);
		tempDesPortrayal.setMap(colorMap);
		return tempDesPortrayal;
	}

	// Elevation desirability, brown
	public FastValueGridPortrayal2D getElevDesPortrayal()
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D elevDesPortrayal = new FastValueGridPortrayal2D();
		elevDesPortrayal.setField(this.dmaps.getElevDes());
		SimpleColorMap colorMap = new SimpleColorMap(0.0, 1.0, 
				new Color(0,0,0,0), new Color(139, 69, 19, 255)); // brown
		elevDesPortrayal.setMap(colorMap);
		return elevDesPortrayal;
	}
	
	// Dist. from water source desirability, purple
	public FastValueGridPortrayal2D getRiverDesPortrayal(boolean clamp)
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D riverDesPortrayal = new FastValueGridPortrayal2D();
		riverDesPortrayal.setField(this.dmaps.getRiverDes());
		// this is most visible at a threshold of 0.6
		SimpleColorMap colorMap ;
		if(clamp)
			colorMap = new SimpleColorMap(0.6, 1.0, new Color(0,0,0,0), 
					new Color(128, 0, 128, 200));
		else
			colorMap = new SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), 
					new Color(128, 0, 128, 200));
		riverDesPortrayal.setMap(colorMap);
		return riverDesPortrayal;
	}
	
	// Dist. from the ports desirability, blue 
	public FastValueGridPortrayal2D getPortDesPortrayal(boolean clamp)
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D portDesPortrayal = new FastValueGridPortrayal2D();
		portDesPortrayal.setField(this.dmaps.getPortDes());
		// this is most visible at a threshold of 0.65
		SimpleColorMap colorMap ;
		if(clamp)
			colorMap = new SimpleColorMap(0.65, 1.0, new Color(0,0,0,0), Color.BLUE);
		else
			colorMap = new SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), Color.BLUE);
		portDesPortrayal.setMap(colorMap);
		return portDesPortrayal;
	}
	
	// Total desirability with median coefficients, red
	public FastValueGridPortrayal2D getTotalDesMedianPortrayal(boolean clamp)
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D totalDesPortrayal = new FastValueGridPortrayal2D();
		totalDesPortrayal.setField(this.dmaps.getTotalDesMedian());
		SimpleColorMap colorMap ;
		if(clamp)
			colorMap = new SimpleColorMap(0.65, 1.0, new Color(0,0,0,0), Color.RED);
		else
			colorMap = new SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), Color.RED);
		totalDesPortrayal.setMap(colorMap);
		return totalDesPortrayal;
	}
	
	// Total desirability with mean coefficients, red
	public FastValueGridPortrayal2D getTotalDesMeanPortrayal(boolean clamp)
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D totalDesPortrayal = new FastValueGridPortrayal2D();
		totalDesPortrayal.setField(this.dmaps.getTotalDesMean());
		SimpleColorMap colorMap ;
		if(clamp)
			colorMap = new SimpleColorMap(0.65, 1.0, new Color(0,0,0,0), Color.RED);
		else
			colorMap = new SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), Color.RED);
		totalDesPortrayal.setMap(colorMap);
		return totalDesPortrayal;
	}
	
	// Code to check if the bounding box is in right dimension
	public FastValueGridPortrayal2D getBoundingBoxPortrayal()
	{
        //Thread.dumpStack();
	    //System.exit(0);
		FastValueGridPortrayal2D p = new FastValueGridPortrayal2D();
		DoubleGrid2D bb = new DoubleGrid2D(Map.GRID_WIDTH, Map.GRID_HEIGHT);
		for(int i = DesirabilityMaps.START_I ; i < DesirabilityMaps.END_I ; i++)
			for(int j = DesirabilityMaps.START_J ; j < DesirabilityMaps.END_J ; j++)
				bb.set(j,i,1.0);
		p.setField(bb);
		SimpleColorMap colorMap = new SimpleColorMap(0.0, 1.0, new Color(0,0,0,0), 
				new Color(0,0,1,0.1f));
		p.setMap(colorMap);
		return p;
	}
}
