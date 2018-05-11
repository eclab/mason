package CDI.src.environment;

import java.awt.Color;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.simple.OrientedPortrayal2D;
import sim.util.gui.SimpleColorMap;

public class MapPortrayals {
	Map map;
	public MapPortrayals(Map map) {
		this.map=map;
	}
	
	public FastValueGridPortrayal2D getNationsPortrayal() {
        FastValueGridPortrayal2D nationsPortrayal = new FastValueGridPortrayal2D();
        nationsPortrayal.setField(map.nationGrid);
        Color[] colorTable = {new Color(0f,0f,0f,0f),
                              new Color(0.9f, 0.9f, 0.9f, 1f), Color.BLACK,
                              Color.yellow, Color.magenta, Color.DARK_GRAY,
                              Color.green, Color.lightGray, Color.blue,
                              Color.pink, Color.gray, Color.cyan,
                              Color.orange, new Color(0.8f, 0.8f, 0.8f, 1f),
                              Color.red, Color.blue};
        nationsPortrayal.setMap(new SimpleColorMap(colorTable));
        return nationsPortrayal;
    }
	
    public FastValueGridPortrayal2D getProvincesPortrayal() {
        FastValueGridPortrayal2D provincesPortrayal = new FastValueGridPortrayal2D();
        provincesPortrayal.setField(map.provinceGrid);
        Color[] colorTable = {new Color(0f,0f,0f,0f),
                              new Color(0.85f, 0.85f, 0.85f, 1f),  // Alberta
                              new Color(0.90f, 0.90f, 0.90f, 1f),  // Nunavut
                              new Color(0.95f, 0.95f, 0.95f, 1f),  // Quebec
                              new Color(0.95f, 0.95f, 0.95f, 1f),  // Nova Scotia
                              new Color(0.85f, 0.85f, 0.85f, 1f),  // Yukon
                              new Color(0.85f, 0.85f, 0.85f, 1f),  // New Brunswick
                              new Color(0.95f, 0.95f, 0.95f, 1f),  // Northwest Territories
                              new Color(0.95f, 0.95f, 0.95f, 1f),  // Manitoba
                              new Color(0.90f, 0.90f, 0.90f, 1f),  // British Columbia
                              new Color(0.90f, 0.90f, 0.90f, 1f),  // Prince Edward Island
                              new Color(0.90f, 0.90f, 0.90f, 1f),  // Newfoundland & Labrador
                              new Color(0.90f, 0.90f, 0.90f, 1f),  // Saskatchewan
                              new Color(0.90f, 0.90f, 0.90f, 1f),  // Ontario
                              Color.red};  // Just to make sure nothing is weird
        provincesPortrayal.setMap(new SimpleColorMap(colorTable));
        return provincesPortrayal;
    }
    
	public FastValueGridPortrayal2D getCoastalPortrayal() {
		FastValueGridPortrayal2D coastalPortrayal = new FastValueGridPortrayal2D();
		coastalPortrayal.setField(map.coastalGrid);
		Color[] colorTable = {new Color(0f,0f,0f,0f),new Color(0f,0f,0f,0f), Color.decode("0xA6DBED")};
		coastalPortrayal.setMap(new SimpleColorMap(colorTable));
		return coastalPortrayal;
	}
	
	public FastValueGridPortrayal2D getMegaCellPortrayal() {
		FastValueGridPortrayal2D megaCellPortrayal = new FastValueGridPortrayal2D();
		megaCellPortrayal.setField(map.megaCellGrid);
		Color[] colorTable = {new Color(0f,0.5f,0.5f,0.5f),new Color(0f,0f,0f,0f)};
		SimpleColorMap map = new SimpleColorMap(colorTable);
		megaCellPortrayal.setMap(map);
		return megaCellPortrayal;
	}
	
	
    
    public FastValueGridPortrayal2D getPopulationPortrayal() {
        FastValueGridPortrayal2D populationPortrayal = new FastValueGridPortrayal2D();
        populationPortrayal.setField(map.popGrid);
        SimpleColorMap colorMap = new SimpleColorMap(
        		map.parameters.popColorMapLowerBound,
        		map.parameters.popColorMapUpperBound,
        		new Color(1, 0, 0, 0), Color.red);
        colorMap.setColorTable(new Color[]{new Color(0, 0, 0, 0)});
        populationPortrayal.setMap(colorMap);
        return populationPortrayal;
    }
    
    public FastValueGridPortrayal2D getTempPortrayal() {
    	FastValueGridPortrayal2D tempPortrayal = new FastValueGridPortrayal2D();
    	//FIXME Should this be tempRawToAdd of tempRawMovingAverage?
    	tempPortrayal.setField(map.tempRaw);
    	double baseline = 273.15; // convert K to C
    	
        DoubleColorMap colorMap = new DoubleColorMap(baseline-40,baseline,baseline+40,Color.cyan,new Color(0,0,0,0),Color.red);
        colorMap.addExtraColor(Ucar.MISSING, Color.black);
        tempPortrayal.setMap(colorMap);
        return tempPortrayal;
    }
    
    
    
    
    
    // TODO What is this?
    public FastValueGridPortrayal2D getTemperaturePortrayal() {
        FastValueGridPortrayal2D temperaturePortrayal = new FastValueGridPortrayal2D();
        temperaturePortrayal.setField(map.tempRawMovingAverage);
        //SimpleColorMap colorMap = new SimpleColorMap(240, 310, Color.white, Color.blue.darker());
        SimpleColorMap colorMap = new SimpleColorMap(
        		map.parameters.tempColorMapLowerBound,
        		map.parameters.tempColorMapUpperBound,
        		Color.white, Color.blue.darker());
        colorMap.setColorTable(new Color[]{new Color(0, 0, 0, 0)});
        temperaturePortrayal.setMap(colorMap);
        return temperaturePortrayal;
    }
    

    public FastValueGridPortrayal2D getTempVarPortrayal() {
        FastValueGridPortrayal2D tempVarPortrayal = new FastValueGridPortrayal2D();
        tempVarPortrayal.setField(map.tempDes);
        SmartColorMap colorMap = new SmartColorMap(map.getTempVarData(), new Color(0,0,0,0), Color.black);
        tempVarPortrayal.setMap(colorMap);
        return tempVarPortrayal;
    }
    
    public FastValueGridPortrayal2D getTempDesPortrayal() {
        FastValueGridPortrayal2D tempDesPortrayal = new FastValueGridPortrayal2D();
        tempDesPortrayal.setField(map.tempDes);
        SmartColorMap colorMap = new SmartColorMap(map.getTempDesData(), new Color(0,0,0,0), Color.red);
        tempDesPortrayal.setMap(colorMap);
        return tempDesPortrayal;
    }
    
    public FastValueGridPortrayal2D getPortDesPortrayal() {
        FastValueGridPortrayal2D portDesPortrayal = new FastValueGridPortrayal2D();
        portDesPortrayal.setField(map.portDes);
        SmartColorMap colorMap = new SmartColorMap(map.getPortDesData(), new Color(0,0,0,0), Color.blue);
        portDesPortrayal.setMap(colorMap);
        return portDesPortrayal;
    }
    
    public FastValueGridPortrayal2D getRiverDesPortrayal() {
        FastValueGridPortrayal2D riverDesPortrayal = new FastValueGridPortrayal2D();
        riverDesPortrayal.setField(map.riverDes);
        SmartColorMap colorMap = new SmartColorMap(map.getRiverDesData(), new Color(0,0,0,0), Color.cyan);
        riverDesPortrayal.setMap(colorMap);
        return riverDesPortrayal;
    }
    
    public FastValueGridPortrayal2D getElevDesPortrayal() {
        FastValueGridPortrayal2D riverDesPortrayal = new FastValueGridPortrayal2D();
        riverDesPortrayal.setField(map.elevDes);
        SmartColorMap colorMap = new SmartColorMap(map.getTempDesData(), new Color(0,0,0,0), new Color(153, 76, 0));
        riverDesPortrayal.setMap(colorMap);
        return riverDesPortrayal;
    }
    
    public FastValueGridPortrayal2D getTotalDesPortrayal() {
        FastValueGridPortrayal2D riverDesPortrayal = new FastValueGridPortrayal2D();
        riverDesPortrayal.setField(map.initDesGrid);
        SmartColorMap colorMap = new SmartColorMap(map.getTotalDesData(), new Color(0,0,0,0), Color.green);
        riverDesPortrayal.setMap(colorMap);
        return riverDesPortrayal;
    }
    
    
    public ContinuousPortrayal2D getMegaCellSignPortrayal() {
    	
    	ContinuousPortrayal2D megaCellSignPortrayal = new ContinuousPortrayal2D();
    	megaCellSignPortrayal.setField(map.megaCellSignGrid);
        
    	
    	
    	
        // set the sign
        for(int i=0;i<map.megaCellSignGrid.allObjects.numObjs;i++)
        {
            SimplePortrayal2D basic = new VectorSignPortrayal2D(
                    new SimplePortrayal2D(), 0, 0.5, new LevelMap(0,10,0,50),
                    new DoubleColorMap(-1000, 0, 1000, Color.blue, Color.black, Color.red));

            megaCellSignPortrayal.setPortrayalForObject(map.megaCellSignGrid.allObjects.objs[i], basic);            
        }
        
        return megaCellSignPortrayal;
    }

}
