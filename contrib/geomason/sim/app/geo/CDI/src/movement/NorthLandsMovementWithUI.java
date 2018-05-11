package CDI.src.movement;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import masoncsc.util.ChartUtils;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.field.continuous.Continuous2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.inspector.TabbedInspector;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.TrailedPortrayal2D;
import sim.util.Interval;
import sim.util.media.chart.BarChartGenerator;
import sim.util.media.chart.HistogramGenerator;
import CDI.src.environment.Cell;
import CDI.src.environment.DoubleColorMap;
import CDI.src.environment.HouseholdSelectionGridPortrayal2D;
import CDI.src.environment.HouseholdTrackerPortrayal2D;
import CDI.src.environment.HouseholdTrailPortrayal2D;
import CDI.src.environment.Map;
import CDI.src.environment.SmartColorMap;
import CDI.src.environment.SmartDoubleColorMap;
import CDI.src.environment.SmartFastValueGridPortrayal2D;

public class NorthLandsMovementWithUI extends GUIState 
{
	public Display2D display;
	public JFrame displayFrame;
	public Controller controller;
	private final double INITIAL_WIDTH = Map.GRID_WIDTH * 0.5;
	private final double INITIAL_HEIGHT = Map.GRID_HEIGHT * 0.5;

	private static int stepNum = 0;

	public double popPortrayalExp = 0.17;
	public double smoothedPopPortrayalExp = 0.17;
	public double tempDesExp = 5.0;
    public double tempVarExp = 1.0;

	double totalDesStdDevs = 1.0;

	// for drawing the arctic circle
	private Continuous2D arcticCircle;
	private ContinuousPortrayal2D arcticCirclePortrayal;

	private HistogramGenerator satisFactionHistogram;
	private HistogramGenerator householdHistogram;
	private HistogramGenerator temperatureHistogram;
	private HistogramGenerator infrastructurePotentialHistogram;
	private HistogramGenerator assetsHistogram;
	
	private HistogramGenerator urbanHouseholdTemperatureHistogram;
	private HistogramGenerator urbanHouseholdElevationHistogram;
	private HistogramGenerator urbanHouseholdPortHistogram;
	private HistogramGenerator urbanHouseholdRiverHistogram;
	private HistogramGenerator urbanHouseholdSocialHistogram;
	private HistogramGenerator urbanHouseholdInfrastructureAvailabilityHistogram;
	
	private HistogramGenerator ruralHouseholdTemperatureHistogram;
	private HistogramGenerator ruralHouseholdElevationHistogram;
	private HistogramGenerator ruralHouseholdPortHistogram;
	private HistogramGenerator ruralHouseholdRiverHistogram;
	private HistogramGenerator ruralHouseholdSocialHistogram;
	private HistogramGenerator ruralHouseholdInfrastructureAvailabilityHistogram;

    private HistogramGenerator cellTemperatureHistogram;
    private HistogramGenerator cellElevationHistogram;
    private HistogramGenerator cellPortHistogram;
    private HistogramGenerator cellRiverHistogram;
    private HistogramGenerator cellSocialHistogram;
    private HistogramGenerator cellInfrastructureAvailabilityHistogram;
    private HistogramGenerator cellUrbanDesirabilityHistogram;
    private HistogramGenerator cellRuralDesirabilityHistogram;

	private HistogramGenerator householdWealthHistogram;
	private HistogramGenerator ruralHouseholdWealthHistogram;
	private HistogramGenerator urbanHouseholdWealthHistogram;
	private HistogramGenerator householdWealthAdjustmentHistogram;
	
	private HistogramGenerator cellCohesion;
	private HistogramGenerator infrastructureTaxHistrogram;
	
	private BarChartGenerator urbanHouseholdMagnitude;
	private BarChartGenerator ruralHouseholdMagnitude;

	public int householdHistogramLowerBound = 0;
	public int householdHistogramUpperBound = 5000;

	private FastValueGridPortrayal2D tempDesPortrayal;
	private FastValueGridPortrayal2D portDesPortrayal;
	private FastValueGridPortrayal2D riverDesPortrayal;
	private FastValueGridPortrayal2D elevDesPortrayal;
	private FastValueGridPortrayal2D totalDesPortrayal;
	private FastValueGridPortrayal2D popChangePortrayal;
    private FastValueGridPortrayal2D cumulativePopChangePortrayal;
	private SmartFastValueGridPortrayal2D urbanResidencePortrayal;
	private SmartFastValueGridPortrayal2D ruralResidencePortrayal;
	private SmartFastValueGridPortrayal2D urbanDesirabilityPortrayal;
	private SmartFastValueGridPortrayal2D ruralDesirabilityPortrayal;
	private SmartFastValueGridPortrayal2D urbanRouletteWheelPortrayal;
	private SmartFastValueGridPortrayal2D ruralRouletteWheelPortrayal;

	private SmartFastValueGridPortrayal2D infrastructurePortrayal;
	private SmartFastValueGridPortrayal2D infrastructureAvailabilityPortrayal;
	
	private HouseholdTrailPortrayal2D householdTrailPortrayal;
	private HouseholdSelectionGridPortrayal2D householdSelectionGridPortrayal;

	private SmartColorMap infrastructurecolorMap = new SmartColorMap(0.25,
			new Color(0, 0, 0, 0), Color.yellow);;
	private SmartColorMap desColorMap = new SmartColorMap(2.5, new Color(0, 0,
			0, 0), Color.green);
	private SmartColorMap ruralResidenceColorMap = new SmartColorMap(0.144,
			new Color(0, 0, 0, 0), Color.blue);
	private SmartColorMap urbanResidenceColorMap = new SmartColorMap(0.144,
			new Color(0, 0, 0, 0), Color.cyan);
	private SmartColorMap socialTermColorMap = new SmartColorMap(0.35,
			new Color(0, 0, 0, 0), new Color(255, 0, 255));
	private SmartColorMap rouletteWheelColorMap = new SmartColorMap(0.15,
			new Color(0, 0, 0, 0), new Color(200, 255, 0));

	// FIXME the scale factor is not working here, need to fix it
	private SmartDoubleColorMap opporColorMap = new SmartDoubleColorMap(0.10,
			Color.cyan, Color.orange);

	public NorthLandsMovement getModel()
	{	return (NorthLandsMovement)state;	}
	
	public NorthLandsMovementWithUI() 
	{ this(new String[] {"-file", "src/CDI/data/parameters/baseline/baseline.param" });	}
	
	public NorthLandsMovementWithUI(String[] args)
	{	this(new NorthLandsMovement(System.currentTimeMillis(), args));	}

	public NorthLandsMovementWithUI(NorthLandsMovement model) 
	{	super(model);	}

	public static Object getInfo() 
	{ return NorthLandsMovementWithUI.class.getResource("NorthLandsMovement.html"); }

	public static String getName() 
	{	return "NorthLandsMovement";	}

	public Object getSimulationInspectedObject() 
	{	return state;	}

	public void start() 
	{
		super.start();

		//getModel().map.initializeDesirabilityGrid();
		double[] data = getModel().map.getTotalDesData();
		if (data != null)
			((SmartColorMap) totalDesPortrayal.getMap()).setBoundsMinMax(data,
					new Color(0, 0, 0, 0), Color.green);

		System.out.println("Initialization finished!");

		urbanResidencePortrayal.updateBounds(getModel().residenceDataBounds);
		ruralResidencePortrayal.updateBounds(getModel().residenceDataBounds);
		ruralDesirabilityPortrayal.updateBounds(getModel().desDataBounds);
		urbanDesirabilityPortrayal.updateBounds(getModel().desDataBounds);
		urbanRouletteWheelPortrayal.updateBounds(getModel().rouletteBounds);
		ruralRouletteWheelPortrayal.updateBounds(getModel().rouletteBounds);
		infrastructurePortrayal.updateBounds(getModel().infrastructureDataBounds);
		infrastructureAvailabilityPortrayal.updateBounds(getModel().infrastructureAvailabilityDataBounds);

		display.reset();
		display.repaint();
	}

	@Override
	public Inspector getInspector() 
	{
		TabbedInspector i = new TabbedInspector();

		i.setVolatile(true);
		i.addInspector(new SimpleInspector(getModel(), this), "Main");
		i.addInspector(new SimpleInspector(new DisplayProperties(this), this),
				"Display");
		i.addInspector(new SimpleInspector(new InfoProperties(this), this),
				"Info");
		i.addInspector(new SimpleInspector(new UrbanProperties(this), this),
				"Urban");
		i.addInspector(new SimpleInspector(new RuralProperties(this), this),
				"Rural");
		return i;
	}

	protected void setupPortrayals() 
	{
		display.attach(getModel().map.portrayals.getNationsPortrayal(), "Nations", false);
        display.attach(getModel().map.portrayals.getProvincesPortrayal(), "Provinces", false);
		// display.attach(getModel().map.portrayals.getTemperaturePortrayal(),"Temperature", false);
		display.attach(getModel().map.portrayals.getCoastalPortrayal(), "Coastal", false);
		// display.attach(getModel().map.portrayals.getTempDesPortrayal(),"Temperature Des.", false);
		display.attach(getModel().map.portrayals.getTempPortrayal(), "Temp", false);
		display.attach(getTempDesPortrayal(), "Temperature Des.", false);
        display.attach(getTempVarPortrayal(), "Temperature Variance", false);
		display.attach(getModel().map.portrayals.getPortDesPortrayal(), "Port Des.", false);
		display.attach(getModel().map.portrayals.getRiverDesPortrayal(), "River Des.", false);
		display.attach(getModel().map.portrayals.getElevDesPortrayal(), "Elevation Des.", false);
		display.attach(getModel().map.portrayals.getMegaCellPortrayal(), "MegaCell", false);
		display.attach(getModel().map.portrayals.getMegaCellSignPortrayal(), "MegaCellSign", false);
		
		this.prepareHouseholdTrailGrid();
		
		display.attach(householdTrailPortrayal, "householdTrail",false);
		display.attach(householdSelectionGridPortrayal, "householdSelection",false);
		
		totalDesPortrayal = getModel().map.portrayals.getTotalDesPortrayal();
		display.attach(totalDesPortrayal, "Total Des.", false);

		display.attach(getPopulationPortrayal(), "Empirical Pop.", false);
		display.attach(getSmoothedPopulationPortrayal(), "Smoothed Empirical Pop.", false);
		display.attach(getArcticCirclePortrayal(), "Arctic Circle", false);

		urbanResidencePortrayal = this.getUrbanResidencePortrayal();
		ruralResidencePortrayal = this.getRuralResidencePortrayal();
		urbanDesirabilityPortrayal = this.getUrbanDesirabilityPortrayl();
		ruralDesirabilityPortrayal = this.getRuralDesirabilityPortrayl();
		urbanRouletteWheelPortrayal = this.getUrbanRouletteWheelPortrayal();
		ruralRouletteWheelPortrayal = this.getRuralRouletteWheelPortrayal();
		infrastructurePortrayal = this.getInfrastructurePortrayal();
		infrastructureAvailabilityPortrayal = this.getInfrastructureAvailabilityPortrayal();
		popChangePortrayal = this.getPopulationChangePortrayal();
        cumulativePopChangePortrayal = this.getCumulativePopulationChangePortrayal();

		display.attach(urbanDesirabilityPortrayal, "Urban Total Des", false);
		display.attach(ruralDesirabilityPortrayal, "Rural Total Des", false);
		display.attach(urbanRouletteWheelPortrayal, "Urban Wheel", false);
		display.attach(ruralRouletteWheelPortrayal, "Rural Wheel", false);

		display.attach(urbanResidencePortrayal, "Urban People", false);
		display.attach(ruralResidencePortrayal, "Rural People", false);

		display.attach(infrastructurePortrayal, "Infrastructure", false);
		display.attach(infrastructureAvailabilityPortrayal, "InfrastructureAvailability", false);
		display.attach(popChangePortrayal, "Population Change", false);
        display.attach(cumulativePopChangePortrayal, "Cumulative Pop Change", false);
	}

	@Override
	public void init(Controller c) 
	{
		super.init(c);
		controller = c;

		display = new Display2D(INITIAL_WIDTH, INITIAL_HEIGHT, this);
		displayFrame = display.createFrame();
		c.registerFrame(displayFrame);
		displayFrame.setVisible(false);
		setupCharts(c);
		setupPortrayals();

		display.reset();
		display.repaint();

		stepNum = 0;

		display.setScale(3);
		display.setScrollPosition(1, 1);
	}

	/**
	 * Create charts from combinations of the time series and histograms
	 * collected by DataCollector, and attach them to the specified controller.
	 */
	private void setupCharts(Controller c) 
	{
		XYSeries[] serisArray = new XYSeries[] 
		{
				getModel().collector.urbanPopSeries.getData(),
				getModel().collector.ruralPopSeries.getData() 
		};
		ChartUtils.attachTimeSeries(serisArray, "Urban and Rural population",
				"Years", "Number of Households", c, 1);

		serisArray = new XYSeries[] 
		{
				getModel().collector.cellToRuralSeries.getData(),
				getModel().collector.cellToUrbanSeries.getData() 
		};
		ChartUtils.attachTimeSeries(serisArray, "Change of cell type", "Years",
				"Number", c, 1);

		serisArray = new XYSeries[] 
		{
				getModel().collector.totalUrbanDistanceSeries.getData(),
				getModel().collector.totalRuralDistanceSeries.getData() 
		};
		ChartUtils.attachTimeSeries(serisArray,
				"Distance Moved by urban and rural households", "Years",
				"Distance", c, 1);
	
		serisArray = new XYSeries[] 
		{
				getModel().collector.ruralToRuralSeries.getData(),
				getModel().collector.ruralToUrbanSeries.getData(),
				getModel().collector.urbanToRuralSeries.getData(),
				getModel().collector.urbanToUrbanSeries.getData()
		};
		ChartUtils.attachTimeSeries(serisArray, "Change in household types",
				"Years", "Number of households",c, 1);
		
		serisArray = new XYSeries[] 
		{
				getModel().collector.ruralToRuralMoveSeries.getData(),
				getModel().collector.ruralToUrbanMoveSeries.getData(),
				getModel().collector.urbanToRuralMoveSeries.getData(),
				getModel().collector.urbanToUrbanMoveSeries.getData()
		};
		ChartUtils.attachTimeSeries(serisArray, "Change in household types due to migration",
				"Years", "Number of households",c, 1);
		
		serisArray = new XYSeries[] 
		{
				getModel().collector.wealthGiniSeries.getData(),
				getModel().collector.urbanWealthGiniSeries.getData(), 
				getModel().collector.ruralWealthGiniSeries.getData()
		};
		ChartUtils.attachTimeSeries(
				serisArray,"Household Wealth Gini Coefficient", "Years","Gini Coefficient", c, 1);
		
		serisArray = new XYSeries[] 
		{
				getModel().collector.ruralGiniCoeffSeries.getData(),
				getModel().collector.urbanGiniCoeffSeries.getData() 
		};
		ChartUtils.attachTimeSeries(
				serisArray,"Satisfaction Gini Coefficient for Urban and Rural Households", "Years","Gini Coefficient", c, 1);
		
		serisArray = new XYSeries[] 
		{
				getModel().collector.ruralSatisMeanSeries.getData(),
				getModel().collector.urbanSatisMeanSeries.getData() 
		};
		ChartUtils.attachTimeSeries(
				serisArray,"Satisfaction Mean for Urban and Rural Households", "Years","Satisfaction Mean", c, 1);
		
		serisArray = new XYSeries[] 
		{
				getModel().collector.ruralSatisStdevSeries.getData(),
				getModel().collector.urbanSatisStdevSeries.getData() 
		};
		ChartUtils.attachTimeSeries(
				serisArray,"Satisfaction St. Dev. for Urban and Rural Households", "Years","Satisfaction St. Dev.", c, 1);
		
		serisArray = new XYSeries[] 
		{
				getModel().collector.ruralSatisKurtosisSeries.getData(),
				getModel().collector.urbanSatisKurtosisSeries.getData() 
		};
		ChartUtils.attachTimeSeries(
				serisArray,"Satisfaction Kurtosis for Urban and Rural Households", "Years","Satisfaction Kurtosis", c, 1);

        serisArray = new XYSeries[] 
        {
        		getModel().collector.federalRevenueSeries.getData(),
        		getModel().collector.federalAssetsSeries.getData(),
        		getModel().collector.totalRevenueSeries.getData(),
        		getModel().collector.totalAssetsSeries.getData(),
        		getModel().collector.federalExpensesSeries.getData()
        };

        ChartUtils.attachTimeSeries(
        		serisArray,"Total Government Revenue & Assets", "Years", "Dollars", c, 1);
        
        serisArray = new XYSeries[] 
        {
        		getModel().collector.trappedRuralSeries.getData(),
        		getModel().collector.trappedUrbanSeries.getData() 
        };
        ChartUtils.attachTimeSeries(
        		serisArray,"Trapped Households", "Years","Number of households", c, 1);

        // HISTOGRAMS

		satisFactionHistogram = ChartUtils.attachHistogram(
				getModel().desirabilityArray(0), 8, "Satisfaction", "satisfaction",
				"Households", c,true);
		householdHistogram = ChartUtils.attachHistogram(getModel().residenceArray(
				this.householdHistogramLowerBound,
				this.householdHistogramUpperBound), 8, "Households",
				"Number of Households", "Number of Cells", c,true);

		temperatureHistogram = ChartUtils.attachHistogram(
				getModel().map.getTemperatureData(), 8, "Moving Average Temperature",
				"Temperature (C)", "Number of Cells", c,true);
		
		assetsHistogram = ChartUtils.attachHistogram(
				getModel().assetsHistogram(), 8, "Net assets", "Net assets",
				"Number of cells", c,true);
		
		householdWealthHistogram = ChartUtils.attachHistogram(
				getModel().getHouseholdWealthArray(0), 8, "Household Wealth", "Household Wealth",
				"Number of households", c,true);
		
		urbanHouseholdWealthHistogram = ChartUtils.attachHistogram(
				getModel().getHouseholdWealthArray(1), 8, "Urban Household Wealth", "Urban Household Wealth",
				"Number of households", c,true);
		
		ruralHouseholdWealthHistogram = ChartUtils.attachHistogram(
				getModel().getHouseholdWealthArray(2), 8, "Rural Household Wealth", "Rural Household Wealth",
				"Number of households", c,true);
		
		cellCohesion = ChartUtils.attachHistogram(
				getModel().calculateCohesion(), 8, "Cell Cohesion", "Average length of residence", 
				"Frequency", c,true);
		
		householdWealthAdjustmentHistogram = ChartUtils.attachHistogram(
				getModel().householdWealthAdjustmentArray(), 8, "Household Wealth Change", "Household Wealth Change",
				"Number of households", c,true);
		
		infrastructureTaxHistrogram = ChartUtils.attachHistogram(
				getModel().infrastructureTaxArray(), 8, "Infrastructure Tax", "Infrastructure Tax",
				"Number of cells", c,true);
		
		//urbanHouseholdMagnitude = ChartUtils.attachBarChart(getModel().getPopulationWithFactor(0), "Leading Cause of Urban Migrations", "x", "Number of Households", c);
		//ruralHouseholdMagnitude = ChartUtils.attachBarChart(getModel().getPopulationWithFactor(1), "Leading Cause of Rural Migrations", "x", "Number of Households", c);
		
		
		if(getModel().parameters.populationHistrograms)
		{
			attachUrbanHistograms(c);
			attachRuralHistograms(c);
		}
			
        if(getModel().parameters.cellHistrograms)
        {   attachCellHistograms(c);	}
	}

	private void attachRuralHistograms(Controller c) 
	{
		ruralHouseholdTemperatureHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(1, getModel().map.getTempDes(), getModel().parameters.ruralTempCoeff), 8,
				"Rural Temperature Desirability", "Temperature Desirability",
				"Number of households", c,true);
		
		ruralHouseholdElevationHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(1, getModel().map.getElevDes(), getModel().parameters.ruralElevCoeff), 8,
				"Rural Elevation Desirability", "Elevation Desirability",
				"Number of households", c,true);
		
		ruralHouseholdPortHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(1, getModel().map.getPortDes(), getModel().parameters.ruralPortCoeff), 8,
				"Rural Port Desirability", "Port Desirability",
				"Number of households", c,true);
		
		ruralHouseholdRiverHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(1, getModel().map.getRiverDes(), getModel().parameters.ruralRiverCoeff), 8,
				"Rural River Desirability", "River Desirability",
				"Number of households", c,true);
		
		ruralHouseholdSocialHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(1, getModel().ruralSocialWeightGrid, 1), 8,
				"Rural Social Weight", "Social Weight",
				"Number of households", c,true);
		
		ruralHouseholdInfrastructureAvailabilityHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(1, getModel().infrastructureAvailabilityGrid, 1), 8,
				"Rural Infrastructure Availability", "InfrastructureAvailability",
				"Number of households", c,true);
	}

	private void attachUrbanHistograms(Controller c) 
	{
		urbanHouseholdTemperatureHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(0, getModel().map.getTempDes(), getModel().parameters.urbanTempCoeff), 8,
				"Urban Temperature Desirability", "Temperature Desirability",
				"Number of households", c,true);
		
		urbanHouseholdElevationHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(0, getModel().map.getElevDes(), getModel().parameters.urbanElevCoeff), 8,
				"Urban Elevation Desirability", "Elevation Desirability",
				"Number of households", c,true);
		
		urbanHouseholdPortHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(0, getModel().map.getPortDes(), getModel().parameters.urbanPortCoeff), 8,
				"Urban Port Desirability", "Port Desirability",
				"Number of households", c,true);
		
		urbanHouseholdRiverHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(0, getModel().map.getRiverDes(), getModel().parameters.urbanRiverCoeff), 8,
				"Urban River Desirability", "River Desirability",
				"Number of households", c,true);
		
		urbanHouseholdSocialHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(0, getModel().urbanSocialWeightGrid, 1), 8,
				"Urban Social Weight", "Social Weight",
				"Number of households", c,true);
		
		urbanHouseholdInfrastructureAvailabilityHistogram = ChartUtils.attachHistogram(
				getModel().householdDesirabilityArray(0, getModel().infrastructureAvailabilityGrid, 1), 8,
				"Urban Infrastructure Availability", "InfrastructureAvailability",
				"Number of households", c,true);
	}

    private void attachCellHistograms(Controller c) 
    {
        cellTemperatureHistogram = ChartUtils.attachHistogram(
                getModel().cellDesirabilityArray(getModel().map.getTempDes()), 8,
                "Cell Temperature Desirability", "Temperature Desirability",
                "Number of cells", c,true);
        
        cellElevationHistogram = ChartUtils.attachHistogram(
                getModel().cellDesirabilityArray(getModel().map.getElevDes()), 8,
                "Cell Elevation Desirability", "Elevation Desirability",
                "Number of cells", c,true);
        
        cellPortHistogram = ChartUtils.attachHistogram(
                getModel().cellDesirabilityArray(getModel().map.getPortDes()), 8,
                "Cell Port Desirability", "Port Desirability",
                "Number of cells", c,true);
        
        cellRiverHistogram = ChartUtils.attachHistogram(
                getModel().cellDesirabilityArray(getModel().map.getRiverDes()), 8,
                "Cell River Desirability", "River Desirability",
                "Number of cells", c,true);
        
        // XXX Ideally I'd like to use the normalizedPopGrid here, but it's temporary
        //     Alternatively I should display both the urban and rural social grids
        cellSocialHistogram = ChartUtils.attachHistogram(
                getModel().cellDesirabilityArray(getModel().urbanSocialWeightGrid), 8,
                "Cell Social Weight", "Social Weight",
                "Number of cells", c,true);
        
        cellInfrastructureAvailabilityHistogram = ChartUtils.attachHistogram(
                getModel().cellDesirabilityArray(getModel().infrastructureAvailabilityGrid), 8,
                "Cell InfrastructurecAvailability Desirability", "Infrastructure Availability Desirability",
                "Number of cells", c,true);

        cellUrbanDesirabilityHistogram = ChartUtils.attachHistogram(
                getModel().cellDesirabilityArray(getModel().urbanDesirabilityGrid), 8,
                //getModel().worldAgent.urbanDesirability, 8,   // This doesn't exit yet
                "Cell Urban Aggregate Desirability", "Urban Aggregate Desirability",
                "Number of cells", c,true);

        cellRuralDesirabilityHistogram = ChartUtils.attachHistogram(
                getModel().cellDesirabilityArray(getModel().ruralDesirabilityGrid), 8,
                //getModel().worldAgent.ruralDesirability, 8,   // This doesn't exit yet
                "Cell Rural Aggregate Desirability", "Rural Aggregate Desirability",
                "Number of cells", c,true);
        
        infrastructurePotentialHistogram = ChartUtils.attachHistogram(
				getModel().infrastructureHistogram(), 8, "Infrastructure potential", "Infrastructure potential",
				"Number of cells", c,true);
    }
    


	@Override
	public void quit() 
	{
		super.quit();

		if (displayFrame != null) { displayFrame.dispose(); }
		displayFrame = null;
		display = null;
	}

	public boolean step() 
	{
		satisFactionHistogram.updateSeries(0, getModel().desirabilityArray(0));
		householdHistogram.updateSeries(0, 
				getModel().residenceArray(this.householdHistogramLowerBound, 
										  this.householdHistogramUpperBound));
		temperatureHistogram.updateSeries(0, getModel().map.getTemperatureData());
		assetsHistogram.updateSeries(0, getModel().assetsHistogram());
		householdWealthHistogram.updateSeries(0, getModel().getHouseholdWealthArray(0));
		urbanHouseholdWealthHistogram.updateSeries(0, getModel().getHouseholdWealthArray(1));
		ruralHouseholdWealthHistogram.updateSeries(0, getModel().getHouseholdWealthArray(2));
		cellCohesion.updateSeries(0, getModel().calculateCohesion());
		householdWealthAdjustmentHistogram.updateSeries(0, getModel().householdWealthAdjustmentArray());
		infrastructureTaxHistrogram.updateSeries(0, getModel().infrastructureTaxArray());
        //System.err.println("urban house " + urbanHouseholdMagnitude);
		//urbanHouseholdMagnitude.updateSeries(0, getModel().getPopulationWithFactor(0));
		//ruralHouseholdMagnitude.updateSeries(0, getModel().getPopulationWithFactor(1));
		
		getModel().ruralHouseholdsMovedThisStep=0; //TODO : is this really where we want to do this? Where is a good place to do this? 
		getModel().urbanHouseholdsMovedThisStep=0;
		
		long currentStep = getModel().schedule.getSteps();
		satisFactionHistogram.updateChartLater(currentStep);
		householdHistogram.updateChartLater(currentStep);
		temperatureHistogram.updateChartLater(currentStep);
		assetsHistogram.updateChartLater(currentStep);
		//urbanHouseholdMagnitude.updateChartLater(currentStep);
		//ruralHouseholdMagnitude.updateChartLater(currentStep);
		householdWealthHistogram.updateChartLater(currentStep);
		urbanHouseholdWealthHistogram.updateChartLater(currentStep);
		ruralHouseholdWealthHistogram.updateChartLater(currentStep);
		cellCohesion.updateChartLater(currentStep);
		householdWealthAdjustmentHistogram.updateChartLater(currentStep);
		infrastructureTaxHistrogram.updateChartLater(currentStep);
		
        if(getModel().parameters.populationHistrograms)
        {
            updateUrbanHistogram(currentStep);
            updateRuralHistogram(currentStep);  
        }

        if(getModel().parameters.cellHistrograms) 
        { updateCellHistogram(currentStep); }

		return super.step();
	}

	private void updateRuralHistogram(long currentStep) 
	{
		ruralHouseholdTemperatureHistogram.updateSeries(0, 
				getModel().householdDesirabilityArray(1, 
						getModel().map.getTempDes(), 
						getModel().parameters.ruralTempCoeff));
		ruralHouseholdElevationHistogram.updateSeries(0, 
				getModel().householdDesirabilityArray(1, 
						getModel().map.getElevDes(),
						getModel().parameters.ruralElevCoeff));
		ruralHouseholdPortHistogram.updateSeries(0, 
				getModel().householdDesirabilityArray(1, 
						getModel().map.getPortDes(),
						getModel().parameters.ruralPortCoeff));
		ruralHouseholdRiverHistogram.updateSeries(0, 
				getModel().householdDesirabilityArray(1, 
						getModel().map.getRiverDes(),
						getModel().parameters.ruralRiverCoeff));
		ruralHouseholdSocialHistogram.updateSeries(0,
				getModel().householdDesirabilityArray(1, 
						getModel().ruralSocialWeightGrid, 1));
		
		ruralHouseholdInfrastructureAvailabilityHistogram.updateSeries(0,
				getModel().householdDesirabilityArray(1, 
						getModel().infrastructureAvailabilityGrid, 1));
		
		ruralHouseholdTemperatureHistogram.updateChartLater(currentStep);
		ruralHouseholdElevationHistogram.updateChartLater(currentStep);
		ruralHouseholdPortHistogram.updateChartLater(currentStep);
		ruralHouseholdRiverHistogram.updateChartLater(currentStep);
		ruralHouseholdSocialHistogram.updateChartLater(currentStep);
		ruralHouseholdInfrastructureAvailabilityHistogram.updateChartLater(currentStep);		
	}

	private void updateUrbanHistogram(long currentStep) 
	{
		urbanHouseholdTemperatureHistogram.updateSeries(0, getModel().householdDesirabilityArray(0, getModel().map.getTempDes(), getModel().parameters.urbanTempCoeff));
		urbanHouseholdElevationHistogram.updateSeries(0, getModel().householdDesirabilityArray(0, getModel().map.getElevDes(),getModel().parameters.urbanElevCoeff));
		urbanHouseholdPortHistogram.updateSeries(0, getModel().householdDesirabilityArray(0, getModel().map.getPortDes(),getModel().parameters.urbanPortCoeff));
		urbanHouseholdRiverHistogram.updateSeries(0,getModel().householdDesirabilityArray(0, getModel().map.getRiverDes(),getModel().parameters.urbanRiverCoeff));
		urbanHouseholdSocialHistogram.updateSeries(0,
				getModel().householdDesirabilityArray(0, getModel().urbanSocialWeightGrid, 1));
		
		urbanHouseholdInfrastructureAvailabilityHistogram.updateSeries(0,
				getModel().householdDesirabilityArray(0, getModel().infrastructureAvailabilityGrid, 1));
		
		urbanHouseholdTemperatureHistogram.updateChartLater(currentStep);
		urbanHouseholdElevationHistogram.updateChartLater(currentStep);
		urbanHouseholdPortHistogram.updateChartLater(currentStep);
		urbanHouseholdRiverHistogram.updateChartLater(currentStep);
		urbanHouseholdSocialHistogram.updateChartLater(currentStep);
		urbanHouseholdInfrastructureAvailabilityHistogram.updateChartLater(currentStep);
	}

    private void updateCellHistogram(long currentStep) 
    {
        cellTemperatureHistogram.updateSeries(0, getModel().cellDesirabilityArray(getModel().map.getTempDes()));
        cellElevationHistogram.updateSeries(0, getModel().cellDesirabilityArray(getModel().map.getElevDes()));
        cellPortHistogram.updateSeries(0, getModel().cellDesirabilityArray(getModel().map.getPortDes()));
        cellRiverHistogram.updateSeries(0, getModel().cellDesirabilityArray(getModel().map.getRiverDes()));
        cellSocialHistogram.updateSeries(0, getModel().cellDesirabilityArray(getModel().urbanSocialWeightGrid));
        cellInfrastructureAvailabilityHistogram.updateSeries(0, getModel().cellDesirabilityArray(getModel().infrastructureAvailabilityGrid));
        //cellUrbanDesirabilityHistogram.updateSeries(0, getModel().cellDesirabilityArray(getModel().urbanDesirabilityGrid));
        //cellRuralDesirabilityHistogram.updateSeries(0, getModel().cellDesirabilityArray(getModel().ruralDesirabilityGrid));
        cellUrbanDesirabilityHistogram.updateSeries(0, getModel().worldAgent.urbanDesirability);
        cellRuralDesirabilityHistogram.updateSeries(0, getModel().worldAgent.ruralDesirability);
        infrastructurePotentialHistogram.updateSeries(0, getModel().infrastructureHistogram());
        
        cellTemperatureHistogram.updateChartLater(currentStep);
        cellElevationHistogram.updateChartLater(currentStep);
        cellPortHistogram.updateChartLater(currentStep);
        cellRiverHistogram.updateChartLater(currentStep);
        cellSocialHistogram.updateChartLater(currentStep);
        cellInfrastructureAvailabilityHistogram.updateChartLater(currentStep);
        cellUrbanDesirabilityHistogram.updateChartLater(currentStep);
        cellRuralDesirabilityHistogram.updateChartLater(currentStep);
        infrastructurePotentialHistogram.updateChartLater(currentStep);
    }

	public static void main(String[] args) 
	{
		//System.out.println("NorthLandsMovementWithUI>main>reading parameter file: " + args[0] + ", " + args[1]);
		NorthLandsMovementWithUI ui = new NorthLandsMovementWithUI();
		Console c = new Console(ui);
		c.setVisible(true);
	}
	
//=======================================================================

	public FastValueGridPortrayal2D getTempDesPortrayal() 
	{
		FastValueGridPortrayal2D tempDesPortrayal = new FastValueGridPortrayal2D();
		tempDesPortrayal.setField(getModel().map.tempDes);
		SmartColorMap colorMap = new SmartColorMap(getModel().map.getTempDesData(),
				new Color(0, 0, 0, 0), Color.red) 
		{
			@Override
			public double filterLevel(double level) 
			{	return Math.pow(level, tempDesExp);	}
		};
		tempDesPortrayal.setMap(colorMap);
		return tempDesPortrayal;
	}

    public FastValueGridPortrayal2D getTempVarPortrayal() 
    {
/*
        FastValueGridPortrayal2D tempVarPortrayal = new FastValueGridPortrayal2D();
        tempVarPortrayal.setField(getModel().map.tempVariance);
        SmartColorMap colorMap = new SmartColorMap(getModel().map.getTempVarData(),
                new Color(0, 0, 0, 0), Color.orange) {
            @Override
            public double filterLevel(double level) {
                return Math.pow(level, 0.1);   // XXX Make a new variable
            }
        };
        tempVarPortrayal.setMap(colorMap);
        return tempVarPortrayal;
*/
        FastValueGridPortrayal2D tempVarPortrayal = new FastValueGridPortrayal2D();
        tempVarPortrayal.setField(getModel().map.tempVariance);
        SmartColorMap colorMap = new SmartColorMap(getModel().map.getTempVarData(),
                new Color(0, 0, 0, 0), Color.orange) 
        {
            @Override
            public double filterLevel(double level) 
            {   return Math.pow(level, tempDesExp);   }
        };
        tempVarPortrayal.setMap(colorMap);
        return tempVarPortrayal;
    }

	public FastValueGridPortrayal2D getPopulationPortrayal() 
	{
		FastValueGridPortrayal2D populationPortrayal = new FastValueGridPortrayal2D();
		populationPortrayal.setField(getModel().map.getPopulationGrid());

		int data[] = getModel().map.getPopGridData();
		SmartColorMap colorMap = new SmartColorMap(data, new Color(0, 0, 0, 0),
				Color.red) 
		{
			@Override
			public double filterLevel(double level) 
			{	return Math.pow(level, popPortrayalExp);	}
		};

		populationPortrayal.setMap(colorMap);
		return populationPortrayal;
	}

	public FastValueGridPortrayal2D getSmoothedPopulationPortrayal() 
	{
		FastValueGridPortrayal2D smoothedPopulationPortrayal = new FastValueGridPortrayal2D();
		smoothedPopulationPortrayal.setField(getModel().smoothedTargetPopGrid);

		double[] data = new double[getModel().map.canadaCells.size()];
		int index = 0;
		for (Cell c : getModel().map.canadaCells) 
		{	data[index++] = getModel().smoothedTargetPopGrid.field[c.x][c.y];	}

		SmartColorMap colorMap = new SmartColorMap(data, new Color(0, 0, 0, 0),
				Color.red) 
		{
			@Override
			public double filterLevel(double level) 
			{	return Math.pow(level, smoothedPopPortrayalExp);	}
		};

		smoothedPopulationPortrayal.setMap(colorMap);
		return smoothedPopulationPortrayal;
	}

	public ContinuousPortrayal2D getArcticCirclePortrayal() 
	{
		arcticCircle = new Continuous2D(Double.MAX_VALUE, Map.GRID_WIDTH,
				Map.GRID_HEIGHT);
		arcticCircle.setObjectLocation(new Object(), Map.NORTH_POLE);

		arcticCirclePortrayal = new ContinuousPortrayal2D();

		arcticCirclePortrayal.setField(arcticCircle);
		arcticCirclePortrayal.setPortrayalForAll(new OvalPortrayal2D(
				Color.gray, Map.ARCTIC_CIRCLE_DIAMETER, false) 
		{
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) 
			{
				info.precise = true;
				super.draw(object, graphics, info);
			}

			public boolean hitObject(Object object, DrawInfo2D range) 
			{
				return false; // make it so this circle isn't clickable so it
							  // doesn't pollute the
			}
		});

		return arcticCirclePortrayal;
	}

	public SmartFastValueGridPortrayal2D getInfrastructureAvailabilityPortrayal() 
	{
		SmartFastValueGridPortrayal2D portrayal = new SmartFastValueGridPortrayal2D();
		portrayal.setField(getModel().infrastructureAvailabilityGrid);
		portrayal.setMap(opporColorMap);
		return portrayal;
	}

	public SmartFastValueGridPortrayal2D getInfrastructurePortrayal() 
	{
		SmartFastValueGridPortrayal2D portrayal = new SmartFastValueGridPortrayal2D();
		portrayal.setField(getModel().infrastructureGrid);
		portrayal.setMap(infrastructurecolorMap);
		return portrayal;
	}

	private SmartFastValueGridPortrayal2D getRuralRouletteWheelPortrayal() 
	{
		SmartFastValueGridPortrayal2D portrayal = new SmartFastValueGridPortrayal2D();
		portrayal.setField(getModel().ruralRouletteWheelGrid);
		portrayal.setMap(rouletteWheelColorMap);
		return portrayal;
	}

	private SmartFastValueGridPortrayal2D getUrbanRouletteWheelPortrayal() 
	{
		SmartFastValueGridPortrayal2D portrayal = new SmartFastValueGridPortrayal2D();
		portrayal.setField(getModel().urbanRouletteWheelGrid);
		portrayal.setMap(rouletteWheelColorMap);
		return portrayal;
	}

	private SmartFastValueGridPortrayal2D getRuralDesirabilityPortrayl() 
	{
		SmartFastValueGridPortrayal2D portrayal = new SmartFastValueGridPortrayal2D();
		portrayal.setField(getModel().ruralDesirabilityGrid);
		portrayal.setMap(desColorMap);
		return portrayal;
	}

	private SmartFastValueGridPortrayal2D getUrbanDesirabilityPortrayl() 
	{
		SmartFastValueGridPortrayal2D portrayal = new SmartFastValueGridPortrayal2D();
		portrayal.setField(getModel().urbanDesirabilityGrid);
		portrayal.setMap(desColorMap);
		return portrayal;
	}

	public SmartFastValueGridPortrayal2D getUrbanResidencePortrayal() 
	{
		SmartFastValueGridPortrayal2D portrayal = new SmartFastValueGridPortrayal2D();
		portrayal.setField(getModel().urbanResidenceGrid);
		portrayal.setMap(urbanResidenceColorMap);
		return portrayal;
	}

	public SmartFastValueGridPortrayal2D getRuralResidencePortrayal() 
	{
		SmartFastValueGridPortrayal2D portrayal = new SmartFastValueGridPortrayal2D();
		portrayal.setField(getModel().ruralResidenceGrid);
		portrayal.setMap(ruralResidenceColorMap);
		return portrayal;
	}
		
	public FastValueGridPortrayal2D getPopulationChangePortrayal() 
	{
    	FastValueGridPortrayal2D popChangePortrayal = new FastValueGridPortrayal2D();
    	
    	popChangePortrayal.setField(getModel().populationChangeGrid);
    	double baseline = 0; // convert K to C
    	
        DoubleColorMap colorMap = new DoubleColorMap(baseline-100,baseline,baseline+100,Color.cyan,new Color(0,0,0,0),Color.red);
        
        popChangePortrayal.setMap(colorMap);
        return popChangePortrayal;
    }	
	
	public void prepareHouseholdTrailGrid() 
	{
		this.householdSelectionGridPortrayal = new HouseholdSelectionGridPortrayal2D();
		this.householdTrailPortrayal = new HouseholdTrailPortrayal2D();
		
		this.householdSelectionGridPortrayal.setField(getModel().householdSelectionGrid);
		this.householdTrailPortrayal.setField(getModel().householdTrailGrid);
		
		TrailedPortrayal2D trail = new TrailedPortrayal2D(this, new HouseholdTrackerPortrayal2D(), this.householdTrailPortrayal, 10);
		
		this.householdSelectionGridPortrayal.setPortrayalForAll(trail);
		this.householdTrailPortrayal.setPortrayalForAll(trail);
	}
	
    public FastValueGridPortrayal2D getCumulativePopulationChangePortrayal() 
    {
        FastValueGridPortrayal2D cumulativePopChangePortrayal = new FastValueGridPortrayal2D();
        
        cumulativePopChangePortrayal.setField(getModel().cumulativePopChangeGrid);
        double baseline = 0; // convert K to C
        
        DoubleColorMap colorMap = new DoubleColorMap(baseline-100,baseline,baseline+100,Color.cyan,new Color(0,0,0,0),Color.red);
        
        cumulativePopChangePortrayal.setMap(colorMap);
        return cumulativePopChangePortrayal;
    }

    public class DisplayProperties 
    {
		NorthLandsMovement model;
		NorthLandsMovementWithUI modelUI;

		public DisplayProperties(NorthLandsMovementWithUI modelUI) 
		{
			this.modelUI = modelUI;
			this.model = modelUI.getModel();
		}

		public double getRuralResidencePortrayalExp() 
		{	return modelUI.ruralResidenceColorMap.scaleFactor;	}

		public void setRuralResidencePortrayalExp(double val) 
		{	modelUI.ruralResidenceColorMap.scaleFactor = val;	}
		
		public double getUrbanResidencePortrayalExp() 
		{	return modelUI.urbanResidenceColorMap.scaleFactor;	}

		public void setUrbanResidencePortrayalExp(double val) 
		{	modelUI.urbanResidenceColorMap.scaleFactor = val;	}

		public Object domResidencePortrayalExp() 
		{	return new Interval(0.0, 1.0);	}

		public double getDesPortrayalExp() 
		{	return modelUI.desColorMap.scaleFactor;	}

		public void setDesPortrayalExp(double val) 
		{	modelUI.desColorMap.scaleFactor = val;	}

		public Object domDesPortrayalExp() 
		{	return new Interval(0.0, 10.0);	}

		public double getInfrastructurePortrayalExp() 
		{	return modelUI.infrastructurecolorMap.scaleFactor;	}

		public void setInfrastructurePortrayalExp(double val) 
		{	modelUI.infrastructurecolorMap.scaleFactor = val;	}

		public Object domInfrastructurePortrayalExp() 
		{	return new Interval(0.0, 1.0);	}

		public double getInfrastructureAvailabilityPortrayalExp() 
		{	return modelUI.opporColorMap.scaleFactor;	}

		public void setInfrastructureAvailabilityPortrayalExp(double val) 
		{	modelUI.opporColorMap.scaleFactor = val;	}

		public Object domInfrastructureAvailabilityPortrayalExp()
		{	return new Interval(0.0, 1.0);	}

		public double getSocialTermExp()
		{	return modelUI.socialTermColorMap.scaleFactor;	}

		public void setSocialTermExp(double val) 
		{	modelUI.socialTermColorMap.scaleFactor = val;	}

		public Object domSocialTermExp()
		{	return new Interval(0.0, 1.0);	}

		public double getRouletteWheelExp()
		{	return modelUI.rouletteWheelColorMap.scaleFactor;	}

		public void setRouletteWheelExp(double val) 
		{	modelUI.rouletteWheelColorMap.scaleFactor = val;	}

		public Object domRouletteWheelExp() 
		{	return new Interval(0.0, 1.0);	}

		public double getTotalDesStdDevs() 
		{	return totalDesStdDevs;	}

		public void setTotalDesStdDevs(double val) 
		{
			if (totalDesPortrayal == null)
				return;

			totalDesStdDevs = val;
			useTotalDesBounds = false;

			double[] data = model.map.getTotalDesData();
			if (data != null)
				((SmartColorMap) totalDesPortrayal.getMap()).setBoundsStdDev(
						data, totalDesStdDevs, new Color(0, 0, 0, 0),
						Color.green);
		}

		public Object domTotalDesStdDevs() 
		{	return new Interval(0.0, 10.0);	}

		public boolean useTotalDesBounds = false;

		public boolean getUseTotalDesBounds() 
		{	return useTotalDesBounds;	}

		public void setUseTotalDesBounds(boolean val) 
		{
			useTotalDesBounds = val;
			double[] data = model.map.getTotalDesData();
			if (data != null)
				((SmartColorMap) totalDesPortrayal.getMap()).setBoundsMinMax(
						data, new Color(0, 0, 0, 0), Color.green);
		}

		public double getPopPortrayalExp() 
		{	return modelUI.popPortrayalExp;	}

		public void setPopPortrayalExp(double val) 
		{	modelUI.popPortrayalExp = val;	}

		public Object domPopPortrayalExp() 
		{	return new Interval(0.0, 1.0);	}

		public double getSmoothedPopPortrayalExp() 
		{	return modelUI.smoothedPopPortrayalExp;	}

		public void setSmoothedPopPortrayalExp(double val) 
		{	modelUI.smoothedPopPortrayalExp = val;	}

		public Object domSmoothedPopPortrayalExp() 
		{	return new Interval(0.0, 1.0);	}

		public double getTempDesExp() 
		{	return modelUI.tempDesExp;	}

		public void setTempDesExp(double val) 
		{	modelUI.tempDesExp = val;	}

		public Object domTempDesExp() 
		{	return new Interval(0.0, 10.0);	}
		
	    public double getTempVarExp() 
	    {   return modelUI.tempVarExp;	}

	    public void setTempVarExp(double val) 
	    {   modelUI.tempVarExp = val;	}

	    public Object domTempVarExp() 
	    {   return new Interval(0.0, 10.0);	}
	}

	public class UrbanProperties 
	{
		NorthLandsMovement model;
		NorthLandsMovementWithUI modelUI;

		public UrbanProperties(NorthLandsMovementWithUI modelUI) 
		{
			this.modelUI = modelUI;
			this.model = modelUI.getModel();
		}

		
		public double getTempDesCoefficient() 
		{	return model.parameters.urbanTempCoeff;	}

		public void setTempDesCoefficient(double val) 
		{
			if (NorthLandsMovement.COEFF_LOWER_BOUND <= val
					&& val <= NorthLandsMovement.COEFF_UPPER_BOUND)
				model.parameters.urbanTempCoeff = val;
		}

		public Object domTempDesCoefficient() 
		{
			return new Interval(NorthLandsMovement.COEFF_LOWER_BOUND,
					NorthLandsMovement.COEFF_UPPER_BOUND);
		}

		public double getPortDesCoefficient() 
		{	return model.parameters.urbanPortCoeff;	}

		public void setPortDesCoefficient(double val) 
		{
			if (NorthLandsMovement.COEFF_LOWER_BOUND <= val
					&& val <= NorthLandsMovement.COEFF_UPPER_BOUND)
				model.parameters.urbanPortCoeff = val;
		}

		public Object domPortDesCoefficient() 
		{
			return new Interval(NorthLandsMovement.COEFF_LOWER_BOUND,
					NorthLandsMovement.COEFF_UPPER_BOUND);
		}

		public double getRiverDesCoefficient() 
		{	return model.parameters.urbanRiverCoeff;	}

		public void setRiverDesCoefficient(double val) 
		{
			if (NorthLandsMovement.COEFF_LOWER_BOUND <= val
					&& val <= NorthLandsMovement.COEFF_UPPER_BOUND)
				model.parameters.urbanRiverCoeff = val;
		}

		public Object domRiverDesCoefficient() 
		{
			return new Interval(NorthLandsMovement.COEFF_LOWER_BOUND,
					NorthLandsMovement.COEFF_UPPER_BOUND);
		}

		public double getElevDesCoefficient() 
		{	return model.parameters.urbanElevCoeff;	}

		public void setElevDesCoefficient(double val) 
		{
			if (NorthLandsMovement.COEFF_LOWER_BOUND <= val
					&& val <= NorthLandsMovement.COEFF_UPPER_BOUND)
				model.parameters.urbanElevCoeff = val;
		}

		public Object domElevDesCoefficient() 
		{
			return new Interval(NorthLandsMovement.COEFF_LOWER_BOUND,
					NorthLandsMovement.COEFF_UPPER_BOUND);
		}

		
		public double getInfrastructureAvailabilityCoeff() 
		{	return model.parameters.urbanInfrastructureAvailabilityCoeff;	}

		public void setInfrastructureAvailabilityCoeff(double infrastructureAvailabilityCoeff) 
		{	model.parameters.urbanInfrastructureAvailabilityCoeff = infrastructureAvailabilityCoeff;	}

		public double getAdjacentSocialDiscount() 
		{	return model.parameters.urbanAdjacentSocialDiscount;	}

		public void setAdjacentSocialDiscount(double val) 
		{	model.parameters.urbanAdjacentSocialDiscount = val;	}

		public double getGrowthRate() 
		{	return model.parameters.urbanGrowthRate;	}

		public void setGrowthRate(double val) 
		{	model.parameters.urbanGrowthRate = val;	}

		public void setMovementWill(double val) 
		{	model.parameters.urbanMovementWill = val;	}

		public double getMovementWill() 
		{	return model.parameters.urbanMovementWill;	}

		public double getSocialWeight() 
		{	return model.parameters.urbanSocialWeight;	}

		public void setSocialWeight(double val) 
		{	model.parameters.urbanSocialWeight = val;	}

		public double getDesExp() 
		{	return model.parameters.urbanDesExp;	}

		public void setDesExp(double val) 
		{	model.parameters.urbanDesExp = val;	}
	}

	public class RuralProperties 
	{
		NorthLandsMovement model;
		NorthLandsMovementWithUI modelUI;

		public RuralProperties(NorthLandsMovementWithUI modelUI) 
		{
			this.modelUI = modelUI;
			this.model = modelUI.getModel();
		}

		public double getTempDesCoefficient() 
		{	return model.parameters.ruralTempCoeff;	}

		public void setTempDesCoefficient(double val) 
		{
			if (NorthLandsMovement.COEFF_LOWER_BOUND <= val
					&& val <= NorthLandsMovement.COEFF_UPPER_BOUND)
				model.parameters.ruralTempCoeff = val;
		}

		public Object domTempDesCoefficient() 
		{
			return new Interval(NorthLandsMovement.COEFF_LOWER_BOUND,
					NorthLandsMovement.COEFF_UPPER_BOUND);
		}

		public double getPortDesCoefficient() 
		{	return model.parameters.ruralPortCoeff;	}

		public void setPortDesCoefficient(double val) 
		{
			if (NorthLandsMovement.COEFF_LOWER_BOUND <= val
					&& val <= NorthLandsMovement.COEFF_UPPER_BOUND)
				model.parameters.ruralPortCoeff = val;
		}

		public Object domPortDesCoefficient() 
		{
			return new Interval(NorthLandsMovement.COEFF_LOWER_BOUND,
					NorthLandsMovement.COEFF_UPPER_BOUND);
		}

		public double getRiverDesCoefficient() 
		{	return model.parameters.ruralRiverCoeff;	}

		public void setRiverDesCoefficient(double val) 
		{
			if (NorthLandsMovement.COEFF_LOWER_BOUND <= val
					&& val <= NorthLandsMovement.COEFF_UPPER_BOUND)
				model.parameters.ruralRiverCoeff = val;
		}

		public Object domRiverDesCoefficient() 
		{
			return new Interval(NorthLandsMovement.COEFF_LOWER_BOUND,
					NorthLandsMovement.COEFF_UPPER_BOUND);
		}

		public double getElevDesCoefficient() 
		{	return model.parameters.ruralElevCoeff;	}

		public void setElevDesCoefficient(double val) 
		{
			if (NorthLandsMovement.COEFF_LOWER_BOUND <= val
					&& val <= NorthLandsMovement.COEFF_UPPER_BOUND)
				model.parameters.ruralElevCoeff = val;
		}

		public Object domElevDesCoefficient() 
		{
			return new Interval(NorthLandsMovement.COEFF_LOWER_BOUND,
					NorthLandsMovement.COEFF_UPPER_BOUND);
		}

		public double getInfrastructureAvailabilityCoeff() 
		{	return model.parameters.ruralInfrastructureAvailabilityCoeff;	}

		public void setInfrastructureAvailabilityCoeff(double infrastructureAvailabilityCoeff) 
		{
			model.parameters.ruralInfrastructureAvailabilityCoeff = infrastructureAvailabilityCoeff;
		}

		public double getAdjacentSocialDiscount() 
		{	return model.parameters.ruralAdjacentSocialDiscount;	}

		public void setAdjacentSocialDiscount(double val) 
		{	model.parameters.ruralAdjacentSocialDiscount = val;	}

		public double getGrowthRate() 
		{	return model.parameters.ruralGrowthRate;	}

		public void setGrowthRate(double val) 
		{	model.parameters.ruralGrowthRate = val;	}

		public void setMovementWill(double val) 
		{	model.parameters.ruralMovementWill = val;	}

		public double getMovementWill() 
		{	return model.parameters.ruralMovementWill;	}

		public double getSocialWeight() 
		{	return model.parameters.ruralSocialWeight;	}

		public void setSocialWeight(double val) 
		{	model.parameters.ruralSocialWeight = val;	}

		public double getDesExp() 
		{	return model.parameters.ruralDesExp;	}

		public void setDesExp(double val) 
		{	model.parameters.ruralDesExp = val;	}
	}
}
