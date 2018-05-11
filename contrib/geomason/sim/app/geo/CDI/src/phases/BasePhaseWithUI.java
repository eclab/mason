package CDI.src.phases;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import javax.swing.JFrame;

import masoncsc.datawatcher.TimeSeriesDataStore;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.field.continuous.Continuous2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.inspector.TabbedInspector;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.Interval;
import sim.util.media.chart.ChartGenerator;
import sim.util.media.chart.TimeSeriesChartGenerator;
import CDI.src.environment.Cell;
import CDI.src.environment.Map;
import CDI.src.environment.SmartColorMap;

public class BasePhaseWithUI extends GUIState
{
	// <editor-fold defaultstate="collapsed" desc="Fields">
	public BasePhase model;
	public Display2D display;
	public JFrame displayFrame;
	public Controller controller;
	private final double INITIAL_WIDTH = Map.GRID_WIDTH * 0.5;
	private final double INITIAL_HEIGHT = Map.GRID_HEIGHT * 0.5;

	protected ArrayList<ChartGenerator> chartGenerators;
	protected ArrayList<Integer>chartUpdateIntervals;

	static int stepNum = 0;  // Kludge!  Use the scheduler.
	static int monthNum = 0;

	FastValueGridPortrayal2D tempDesPortrayal;
	FastValueGridPortrayal2D portDesPortrayal;
	FastValueGridPortrayal2D riverDesPortrayal;
	FastValueGridPortrayal2D elevDesPortrayal;
	FastValueGridPortrayal2D totalDesPortrayal;
	double totalDesStdDevs = 1.0;
	
	// for drawing the arctic circle
	Continuous2D arcticCircle;
	ContinuousPortrayal2D arcticCirclePortrayal;

	/**private FieldPortrayal2D terrainPortrayal;
	private FieldPortrayal2D provincePortrayal;
	private FieldPortrayal2D resourcePortrayal;*/
	//</editor-fold>

	public BasePhaseWithUI()
	{
		super(new BasePhase(System.currentTimeMillis(), new String[]{"-file", "src/CDI/data/parameters/default.param"}));
		model = (BasePhase)state;
		initCharts();
	}
	
	public BasePhaseWithUI(BasePhase model) {
		super(model);
		initCharts();
	}
	
	private void initCharts() {
		this.chartGenerators = new ArrayList<ChartGenerator>();
		this.chartUpdateIntervals = new ArrayList<Integer>();
	}
	
	public static String getName() {
		return "Base Phase";
	}

	public Object getSimulationInspectedObject() {
		return state;
	}

	@Override
	public void start()
	{
		super.start();
		
		double[] data = model.map.getTotalDesData();
		if (data != null)
			((SmartColorMap)totalDesPortrayal.getMap()).setBoundsMinMax(data, new Color(0,0,0,0), Color.green);
		
		
		// TODO temperature
//		data = model.map.getTempDesData();
//		if (data != null)
//			((SmartColorMap)tempDesPortrayal.getMap()).setBoundsMinMax(data, new Color(0,0,0,0), Color.red);

		// temperature

		System.out.println("Initialization finished!");
	}
	
	

	@Override
	public Inspector getInspector() {
        TabbedInspector i = new TabbedInspector();

        i.setVolatile(true);
        i.addInspector(new SimpleInspector(model, this), "Main");
        i.addInspector(new SimpleInspector(new DisplayProperties(this), this), "Display");

        return i;
	}

	/** Attach field portrayals to their respective grids. */
	protected void setupPortrayals()
	{

		/*terrainPortrayal = ((Country) state).terrain.getPortrayal();
		display.attach(terrainPortrayal, "Terrain");
		provincePortrayal = ((Country) state).stateGovernment.getProvinces().getBorderLinesPortrayal();
		display.attach(provincePortrayal, "Provinces");

		resourcePortrayal = ((Country) state).stateGovernment.getResources().getResourcesPortrayal();
		display.attach(resourcePortrayal, "Resources");*/

		// These lines are rearranged a little bit, 
		// and the normalized numHouseholds grid is attached -- khaled
		display.attach(model.map.portrayals.getNationsPortrayal(),"Nations", true);
//		display.attach(model.map.portrayals.getTemperaturePortrayal(),"Temperature", false);
		display.attach(model.map.portrayals.getCoastalPortrayal(), "Coastal", false);
		//display.attach(model.map.portrayals.getTempDesPortrayal(),"Temperature Des.", false);
		// TODO temperature
		display.attach(getTempDesPortrayal(),"Temperature Des.", false);
		display.attach(model.map.portrayals.getPortDesPortrayal(), "Port Des.", false);
		display.attach(model.map.portrayals.getRiverDesPortrayal(), "River Des.", false);
		display.attach(model.map.portrayals.getElevDesPortrayal(), "Elevation Des.", false);
		
		totalDesPortrayal = model.map.portrayals.getTotalDesPortrayal();
		display.attach(totalDesPortrayal, "Total Des.", false);

		display.attach(getPopulationPortrayal(),"Empirical Pop.",false);
		display.attach(getSmoothedPopulationPortrayal(),"Smoothed Empirical Pop.", false);
		
		display.attach(getArcticCirclePortrayal(), "Arctic Circle", true);

	}

	@Override
	public void init(Controller c)
	{
		super.init(c);
		controller = c;

		display = new Display2D(INITIAL_WIDTH, INITIAL_HEIGHT, this);
		displayFrame = display.createFrame();
		c.registerFrame(displayFrame);
		displayFrame.setVisible(true);
		setupCharts(c);

		setupPortrayals();

		display.reset();
		display.repaint();
		
		
		stepNum = 0;  // kludge! Use the scheduler
		monthNum = 0;

		display.setScale(3);
		display.setScrollPosition(1, 1);
	}

	public double popPortrayalExp = 0.17;
	public double smoothedPopPortrayalExp = 0.17;
	public double tempDesExp = 5.0;
	
	
	
	// TODO temperature
	public FastValueGridPortrayal2D getTempDesPortrayal() {
        FastValueGridPortrayal2D tempDesPortrayal = new FastValueGridPortrayal2D();
        tempDesPortrayal.setField(model.map.tempDes);
        SmartColorMap colorMap = new SmartColorMap(model.map.getTempDesData(), new Color(0,0,0,0), Color.red) {
        	@Override
        	public double filterLevel(double level) {
        		return Math.pow(level, tempDesExp);
        	}
        };
        tempDesPortrayal.setMap(colorMap);
        return tempDesPortrayal;
    }
	
	
    public FastValueGridPortrayal2D getPopulationPortrayal() {
        FastValueGridPortrayal2D populationPortrayal = new FastValueGridPortrayal2D();
        populationPortrayal.setField(model.map.getPopulationGrid());
        
        int data[] = model.map.getPopGridData();	
        SmartColorMap colorMap = new SmartColorMap(data, new Color(0, 0, 0, 0), Color.red) {
			@Override
			public double filterLevel(double level) {
				return Math.pow(level, popPortrayalExp);
			}
        };
        
        populationPortrayal.setMap(colorMap);
        return populationPortrayal;
    }
	
    public FastValueGridPortrayal2D getSmoothedPopulationPortrayal() {
        FastValueGridPortrayal2D smoothedPopulationPortrayal = new FastValueGridPortrayal2D();
        smoothedPopulationPortrayal.setField(model.smoothedTargetPopGrid);
        
        double[] data = new double[model.map.canadaCells.size()];
        int index = 0;
        for (Cell c : model.map.canadaCells) {
        	data[index++] = model.smoothedTargetPopGrid.field[c.x][c.y];
        }
        
        SmartColorMap colorMap = new SmartColorMap(data, new Color(0, 0, 0, 0), Color.red) {
			@Override
			public double filterLevel(double level) {
				return Math.pow(level, smoothedPopPortrayalExp);
			}
        };
        
        smoothedPopulationPortrayal.setMap(colorMap);
        return smoothedPopulationPortrayal;
    }
    
    public ContinuousPortrayal2D getArcticCirclePortrayal() {
		arcticCircle = new Continuous2D(Double.MAX_VALUE, Map.GRID_WIDTH, Map.GRID_HEIGHT);
		arcticCircle.setObjectLocation(new Object(), Map.NORTH_POLE);
		
    	arcticCirclePortrayal = new ContinuousPortrayal2D();
    	
    	arcticCirclePortrayal.setField(arcticCircle);
    	arcticCirclePortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.gray, Map.ARCTIC_CIRCLE_DIAMETER, false){
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
				info.precise = true;
				super.draw(object, graphics, info);
			}
			
			public boolean hitObject(Object object, DrawInfo2D range) {
				return false;	// make it so this circle isn't clickable so it doesn't pollute the 
			}
		});
    	
    	return arcticCirclePortrayal;
    }


	/** Create charts from combinations of the time series and histograms
	 * collected by DataCollector, and attach them to the specified controller. */
	private void setupCharts(Controller c)
	{
		
	}

	public void updateCharts(long currentStep)
	{
		int i=0;
		for (ChartGenerator cg : this.chartGenerators)
		{
			if(currentStep % chartUpdateIntervals.get(i)==0)
				cg.updateChartLater(currentStep);
			i++;
		}
	}

	/**
	 * Create a chart from a single time series and register it with the
	 * controller.  The title and Y axis labels for the chart will be taken from
	 * TimeSeriesDataStore.getDescription().
	 *
	 * @param series The time series data.
	 * @param xLabel Label for the X axis.
	 * @param c Controller to register the chart with (so it can be opened by
	 * the user).
	 * @param updateInterval How frequently the chart needs refreshed.
	 */
	private void attachTimeSeriesToChart(TimeSeriesDataStore series,
	                                     String xLabel, Controller c, int updateInterval)
	{
		attachTimeSeriesToChart(new TimeSeriesDataStore[] {series},
		                        series.getDescription(),
		                        xLabel,
		                        series.getDescription(),
		                        c,
		                        updateInterval);
	}

	/**
	 * Create a chart from several time series and register it with the
	 * controller.
	 *
	 * @param seriesArray The time series data.
	 * @param title Label to display at the top of the chart.
	 * @param xLabel Label for the X axis.
	 * @param yLabel Label for the Y axis.
	 * @param c Controller to register the chart with (so it can be opened by
	 * the user).
	 * @param updateInterval How frequently the chart needs refreshed.
	 */
	private void attachTimeSeriesToChart(TimeSeriesDataStore[] seriesArray, String title,
	                                     String xLabel, String yLabel, Controller c,int updateInterval)
	{
		TimeSeriesChartGenerator chartGen = new TimeSeriesChartGenerator();
		chartGen.setTitle(title);
		chartGen.setXAxisLabel(xLabel);
		chartGen.setYAxisLabel(yLabel);
		for (TimeSeriesDataStore dw : seriesArray)
			chartGen.addSeries(dw.getData(), null);
		this.chartGenerators.add(chartGen);
		this.chartUpdateIntervals.add(updateInterval);

		JFrame frame = chartGen.createFrame();
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
		frame.setTitle(title);
		frame.pack();
		c.registerFrame(frame);
	}

	/** Create a frame from the given chart and register it using the given parameters. */
	public void registerChartFrame(TimeSeriesChartGenerator chart, String title,
	                               String xLabel, String yLabel, Controller c, boolean visible)
	{
		chart.setTitle(title);
		chart.setXAxisLabel(xLabel);
		chart.setYAxisLabel(yLabel);
		JFrame frame = chart.createFrame();
		frame.pack();
		c.registerFrame(frame);
		frame.setVisible(visible);
	}


	@Override
	public void quit()
	{
		super.quit();

		if (displayFrame != null)
		{
			displayFrame.dispose();
		}
		displayFrame = null;
		display = null;
	}

	
	/**
	 * Why we have a step method here?
	 */
	@Override
	public boolean step()
	{
/*
		// Kludge!  Use the scheduler.

		if (stepNum % 1 == 0)
		{
			model.map.updateTemperatures(monthNum);
			monthNum++;
		}
		stepNum++;
*/

		// we are not updating charts right now, we will see later -- khaled
		// updateCharts(model.schedule.getSteps());
		return super.step();
	}

	public static void main(String[] args)
	{
		BasePhaseWithUI basePhaseWithUI = new BasePhaseWithUI();

		Console c = new Console(basePhaseWithUI);
		c.setVisible(true);
	}
	
	
	public class DisplayProperties {

		BasePhase model;
		BasePhaseWithUI modelUI;
		
		
		public DisplayProperties(BasePhaseWithUI modelUI) {
			this.modelUI = modelUI;
			this.model = modelUI.model;
		}
		
		public double getTotalDesStdDevs() { return totalDesStdDevs; }
		public void setTotalDesStdDevs(double val) { 
			if (totalDesPortrayal == null)
				return;
			
			totalDesStdDevs = val;
			useTotalDesBounds = false;

			double[] data = model.map.getTotalDesData();
			if (data != null)
				((SmartColorMap)totalDesPortrayal.getMap()).setBoundsStdDev(data, totalDesStdDevs, new Color(0,0,0,0), Color.green);
		}
		public Object domTotalDesStdDevs() { return new Interval(0.0, 10.0); }
		
		public boolean useTotalDesBounds = false;
		public boolean getUseTotalDesBounds() { return useTotalDesBounds; }
		public void setUseTotalDesBounds(boolean val) {
			useTotalDesBounds = val;
			double[] data = model.map.getTotalDesData();
			if (data != null)
				((SmartColorMap)totalDesPortrayal.getMap()).setBoundsMinMax(data, new Color(0,0,0,0), Color.green);
		}
		

		public double getPopPortrayalExp() { return modelUI.popPortrayalExp; }
		public void setPopPortrayalExp(double val) { modelUI.popPortrayalExp = val; }
		public Object domPopPortrayalExp() { return new Interval(0.0, 1.0); }
		
		public double getSmoothedPopPortrayalExp() { return modelUI.smoothedPopPortrayalExp; }
		public void setSmoothedPopPortrayalExp(double val) { modelUI.smoothedPopPortrayalExp = val; }
		public Object domSmoothedPopPortrayalExp() { return new Interval(0.0, 1.0); }
		
		public double getTempDesExp() { return modelUI.tempDesExp; }
		public void setTempDesExp(double val) { modelUI.tempDesExp = val; }
		public Object domTempDesExp() { return new Interval(0.0, 10.0); }
		
	}
}


