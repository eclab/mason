package sim.app.geo.omolandCA;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;

/**
 *
 * @author gmu
 */
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.FastObjectGridPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.inspector.TabbedInspector;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;

public class LandscapeGUI extends GUIState {
	public Display2D display;
	public JFrame displayFrame;
	private Display2D displayRainfall;
	private JFrame displayFrameRainfall;
	private Display2D displayVegetation;
	private JFrame displayFrameVegetation;
	FastObjectGridPortrayal2D landPortrayal = new FastObjectGridPortrayal2D();
	GeomVectorFieldPortrayal woredaShapeProtrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal woredaShapeProtrayal2 = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal roadShapeProtrayal = new GeomVectorFieldPortrayal();
	SparseGridPortrayal2D householdPortrayal = new SparseGridPortrayal2D();

	SparseGridPortrayal2D herdPortrayal = new SparseGridPortrayal2D();
	SparseGridPortrayal2D cropPortrayal = new SparseGridPortrayal2D();
	FastValueGridPortrayal2D rainfallPortrayal = new FastValueGridPortrayal2D();
	FastValueGridPortrayal2D VegetationGridPortrayal = new FastValueGridPortrayal2D();

	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesHousehold;
	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesHouseholdWealth;
	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesCrop;
	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesRain;
	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesLivestock;
	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesAdaptation;
	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesOnsetAmount;

	public LandscapeGUI(final String[] args) {

		super(new Landscape(System.currentTimeMillis(), args));

	}

	public LandscapeGUI(final SimState state) {
		super(state);
	}

	public static String getName() {
		return "OMOLAND-CA MODEL";
	}

	public Object getSimulationInspectedObject() {
		return state;
	} // non-volatile

	public void start() {
		super.start();
		System.out.println("Started");
		// set up our portrayals
		setupPortrayals();
	}

	@Override
	public void load(final SimState state) {
		super.load(state);
		setupPortrayals();
	}

	public void setupPortrayals() {

		final Landscape land = (Landscape) state;

////
//        landPortrayal.setField(land.allLand); //240-230-140 ,24-252-0
//        landPortrayal.setMap(new sim.util.gui.SimpleColorMap(0.0, 1400, Color.white,new Color(24,252,0)));

		landPortrayal.setField(land.allLand); // 240-230-140 ,24-252-0 //new Color(152, 251, 152),

		final Color transparent = new Color(152, 251, 152);
		final int colTran = transparent.getTransparency();

		landPortrayal.setMap(new sim.util.gui.SimpleColorMap(new Color[] { Color.white,
				new Color(30, 144, 255), new Color(0, 0, 255), new Color(152, 251, 152, colTran),
				new Color(49, 79, 79), new Color(85, 107, 47), new Color(255, 153, 0), Color.MAGENTA }));

		householdPortrayal.setField(land.households);
		householdPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.black, 1.0));

		VegetationGridPortrayal.setField(land.vegetationPortrial);
		VegetationGridPortrayal.setMap(
				new sim.util.gui.SimpleColorMap(0.001, land.getVegDrawerRange(), Color.white, new Color(48, 128, 20)));

		woredaShapeProtrayal.setField(land.woredaShape);
		woredaShapeProtrayal.setPortrayalForAll(new GeomPortrayal(new Color(176, 48, 96), false));
		woredaShapeProtrayal2.setField(land.woredaShape);
		woredaShapeProtrayal2.setPortrayalForAll(new GeomPortrayal(new Color(1f, 1f, 0f, .1f), true));

		roadShapeProtrayal.setField(land.roadShape);
		roadShapeProtrayal.setPortrayalForAll(new GeomPortrayal(Color.LIGHT_GRAY, false));

		herdPortrayal.setField(land.herdTLU);

		herdPortrayal.setPortrayalForAll(new OvalPortrayal2D(new Color(0f, 0f, 1f, .3f)) {
			public void draw(final Object object, final Graphics2D graphics, final DrawInfo2D info) {
				scale = Math.sqrt(((Herd) object).getHerdSizeTLU()) * 0.8;
				info.precise = true;
				super.draw(object, graphics, info);
			}

			public boolean hitObject(final Object object, final DrawInfo2D range) {
				scale = Math.sqrt(((Herd) object).getHerdSizeTLU()) * 0.8;
				return super.hitObject(object, range);
			}
		});

		rainfallPortrayal.setField(land.dailyRainfall);
		rainfallPortrayal.setMap(new sim.util.gui.SimpleColorMap(0.0, 14.0, Color.WHITE, Color.BLUE));

		// crop portrial
		cropPortrayal.setField(land.crops);
		cropPortrayal.setPortrayalForAll(new RectanglePortrayal2D(Color.red));

		display.reset();
		display.setBackdrop(Color.white);
		// redraw the display
		display.repaint();

	}

	public void init(final Controller c) {

		super.init(c);
		display = new Display2D(480, 680, this);
		displayRainfall = new Display2D(400, 620, this);
		displayVegetation = new Display2D(400, 620, this);

		display.attach(VegetationGridPortrayal, "Vegetation");
		display.attach(landPortrayal, "Landscape");
		display.attach(woredaShapeProtrayal, "Woreda Vector");
		display.attach(roadShapeProtrayal, "Road Vector");
		display.attach(householdPortrayal, "Households");
		display.attach(herdPortrayal, "Herds");
		display.attach(cropPortrayal, "Crops");
		display.attach(woredaShapeProtrayal2, "");

		// rainfall
		displayRainfall.attach(rainfallPortrayal, "Daily Rainfall");
		displayFrameRainfall = displayRainfall.createFrame();
		c.registerFrame(displayFrameRainfall);
		displayFrameRainfall.setVisible(false);
		displayFrameRainfall.setTitle("Daily Rainfall");

		// vegetation
		displayVegetation.attach(VegetationGridPortrayal, "Vegetation");
		displayVegetation.attach(woredaShapeProtrayal, "Woreda Vector");

		displayFrameVegetation = displayVegetation.createFrame();
		c.registerFrame(displayFrameVegetation);
		displayFrameVegetation.setVisible(false);
		displayFrameVegetation.setTitle("Vegetation");

		// household
		chartSeriesHousehold = new sim.util.media.chart.TimeSeriesChartGenerator();
		chartSeriesHousehold.setTitle("Total Household ");
		chartSeriesHousehold.setRangeAxisLabel("Total");
		chartSeriesHousehold.setDomainAxisLabel("Time (Days)");
		chartSeriesHousehold.addSeries(((Landscape) state).totalHousehold, null);
		chartSeriesHousehold.addSeries(((Landscape) state).totalPopulation, null);

		final JFrame frameSeriesHouseholdPop = chartSeriesHousehold.createFrame(this);
		frameSeriesHouseholdPop.pack();
		c.registerFrame(frameSeriesHouseholdPop);

		chartSeriesHouseholdWealth = new sim.util.media.chart.TimeSeriesChartGenerator();
		chartSeriesHouseholdWealth.setTitle("Household wealth");
		chartSeriesHouseholdWealth.setRangeAxisLabel("Currency (Birr)");
		chartSeriesHouseholdWealth.setDomainAxisLabel("Time (Days)");
		chartSeriesHouseholdWealth.addSeries(((Landscape) state).totalWealth, null);
		chartSeriesHouseholdWealth.addSeries(((Landscape) state).totalStoredCapital, null);
		chartSeriesHouseholdWealth.addSeries(((Landscape) state).totalLivestockCapital, null);

		final JFrame frameSeriesHousehold = chartSeriesHouseholdWealth.createFrame(this);

		frameSeriesHousehold.pack();
		c.registerFrame(frameSeriesHousehold);

		// crop
		chartSeriesCrop = new sim.util.media.chart.TimeSeriesChartGenerator();

		chartSeriesCrop.setTitle("Crop Production");
		chartSeriesCrop.setRangeAxisLabel("Area (Ha)/ Yield (KG)");
		chartSeriesCrop.setDomainAxisLabel("Time (Days)");

		chartSeriesCrop.addSeries(((Landscape) state).totalMaizeSeriesHA, null);
		chartSeriesCrop.addSeries(((Landscape) state).totalMaizeSeriesYield, null);

		final JFrame frameSeries = chartSeriesCrop.createFrame(this);
		frameSeries.pack();
		c.registerFrame(frameSeries);

		// climate adaptation

		chartSeriesAdaptation = new sim.util.media.chart.TimeSeriesChartGenerator();

		chartSeriesAdaptation.setTitle("Climate Change Adaptation");
		chartSeriesAdaptation.setRangeAxisLabel("Ratio of Total Household");
		chartSeriesAdaptation.setDomainAxisLabel("Time (Days)");
		chartSeriesAdaptation.addSeries(((Landscape) state).totalAdaptationSeries, null);
		chartSeriesAdaptation.addSeries(((Landscape) state).totalNonAdaptationSeries, null);
		chartSeriesAdaptation.addSeries(((Landscape) state).totalAdaptationExperienceSeries, null);

		final JFrame frameSeriesAdaptation = chartSeriesAdaptation.createFrame(this);
		frameSeriesAdaptation.pack();
		c.registerFrame(frameSeriesAdaptation);

		chartSeriesOnsetAmount = new sim.util.media.chart.TimeSeriesChartGenerator();

		chartSeriesOnsetAmount.setTitle("Rainfall Prediction");
		chartSeriesOnsetAmount.setRangeAxisLabel("Ratio of Total Household");
		chartSeriesOnsetAmount.setDomainAxisLabel("Time (Days)");
		chartSeriesOnsetAmount.addSeries(((Landscape) state).totalEarlyOnsetSeries, null);
		chartSeriesOnsetAmount.addSeries(((Landscape) state).totalNormalOnsetSeries, null);
		chartSeriesOnsetAmount.addSeries(((Landscape) state).totalLateOnsetExperienceSeries, null);

		chartSeriesOnsetAmount.addSeries(((Landscape) state).totalBelowNormalAmountSeries, null);
		chartSeriesOnsetAmount.addSeries(((Landscape) state).totalNormalAmountSeries, null);
		chartSeriesOnsetAmount.addSeries(((Landscape) state).totalAboveNormalSeries, null);

		final JFrame frameSeriesOnsetAmount = chartSeriesOnsetAmount.createFrame(this);
		// frameSeriesOnsetAmount.
		frameSeriesOnsetAmount.pack();
		c.registerFrame(frameSeriesOnsetAmount);

		// livestock
		chartSeriesLivestock = new sim.util.media.chart.TimeSeriesChartGenerator();

		chartSeriesLivestock.setTitle("Livestock");
		chartSeriesLivestock.setRangeAxisLabel("TLU");
		chartSeriesLivestock.setDomainAxisLabel("Time ( Days)");
		chartSeriesLivestock.addSeries(((Landscape) state).totalLivestockSeries, null);

		final JFrame frameSeriesLivestock = chartSeriesLivestock.createFrame(this);
		frameSeriesLivestock.pack();
		c.registerFrame(frameSeriesLivestock);

		chartSeriesRain = new sim.util.media.chart.TimeSeriesChartGenerator();

		chartSeriesRain.setTitle("Parcel Rainfal ");
		chartSeriesRain.setRangeAxisLabel("Millimeters (mm) ");
		chartSeriesRain.setDomainAxisLabel("Time ( Days)");

		chartSeriesRain.addSeries(((Landscape) state).rainfallSeriesNorth, null);
		chartSeriesRain.addSeries(((Landscape) state).rainfallSeriesCentral, null);
		chartSeriesRain.addSeries(((Landscape) state).rainfallSeriesSouth, null);

		final JFrame frameSeriesR = chartSeriesRain.createFrame(this);
		frameSeriesR.pack();
		c.registerFrame(frameSeriesR);

		displayFrame = display.createFrame();
		c.registerFrame(displayFrame);
		displayFrame.setVisible(false);

		final JFreeChart agechart = ChartFactory.createBarChart("Age Distribution", "Age  Group",
				"Percentage of Total Population", ((Landscape) state).agedataset, PlotOrientation.VERTICAL, false,
				false, false);
		agechart.setBackgroundPaint(Color.WHITE);
		agechart.getTitle().setPaint(Color.BLACK);

		final CategoryPlot pl = agechart.getCategoryPlot();
		pl.setBackgroundPaint(Color.WHITE);
		pl.setRangeGridlinePaint(Color.BLUE);

		// set the range axis to display integers only...
		final NumberAxis agerangeAxis = (NumberAxis) pl.getRangeAxis();
		agerangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		final ChartFrame ageframe = new ChartFrame("Age Chart", agechart);
		ageframe.setVisible(false);
		ageframe.setSize(400, 350);

		ageframe.pack();
		c.registerFrame(ageframe);

		// Portray Household Size
		final JFreeChart famchart = ChartFactory.createBarChart("Household Size", "Size", "Total",
				((Landscape) state).familydataset, PlotOrientation.VERTICAL, false, false, false);
		famchart.setBackgroundPaint(Color.WHITE);
		famchart.getTitle().setPaint(Color.BLACK);

		final CategoryPlot pf = famchart.getCategoryPlot();
		pf.setBackgroundPaint(Color.WHITE);
		pf.setRangeGridlinePaint(Color.BLUE);

		// set the range axis to display integers only...
		final NumberAxis famrangeAxis = (NumberAxis) pf.getRangeAxis();
		famrangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		final ChartFrame famframe = new ChartFrame("Household Size Chart", famchart);
		famframe.setVisible(false);
		famframe.setSize(400, 350);

		famframe.pack();
		c.registerFrame(famframe);

		final JFreeChart popWorChart = ChartFactory.createBarChart("Households by Woreda", "Woreda",
				" Percentage of Total Households", ((Landscape) state).popWoredadataset, PlotOrientation.VERTICAL,
				false, false, false);
		popWorChart.setBackgroundPaint(Color.WHITE);
		popWorChart.getTitle().setPaint(Color.BLACK);

		final CategoryPlot popl = popWorChart.getCategoryPlot();
		pl.setBackgroundPaint(Color.WHITE);
		pl.setRangeGridlinePaint(Color.BLUE);

		// set the range axis to display integers only...
		final NumberAxis poprangeAxis = (NumberAxis) popl.getRangeAxis();
		poprangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		final ChartFrame popframe = new ChartFrame("Household By Woreda Chart", popWorChart);
		popframe.setVisible(false);
		popframe.setSize(400, 350);

		popframe.pack();
		c.registerFrame(popframe);

		final Dimension dl = new Dimension(300, 700);
		final Legend legend = new Legend();
		legend.setSize(dl);

		final JFrame legendframe = new JFrame();
		legendframe.setVisible(false);
		legendframe.setPreferredSize(dl);
		legendframe.setSize(300, 700);

		legendframe.setBackground(Color.white);
		legendframe.setTitle("Legend");
		legendframe.getContentPane().add(legend);
		legendframe.pack();
		c.registerFrame(legendframe);

	}

	public Inspector getInspector() {
		super.getInspector();

		final TabbedInspector i = new TabbedInspector();

		i.addInspector(new SimpleInspector(
				((Landscape) state).params.globalParam, this), "Global");
		i.addInspector(new SimpleInspector(
				((Landscape) state).params.climateParam, this), "Climate");
		i.addInspector(new SimpleInspector(
				((Landscape) state).params.householdParam, this), "HouseHold");
		i.addInspector(new SimpleInspector(
				((Landscape) state).params.herdingParam, this), "Herding");
		i.addInspector(new SimpleInspector(
				((Landscape) state).params.farmingParam, this), "Farming");
		i.addInspector(new SimpleInspector(
				((Landscape) state).params.vegetationParam, this), "Vegetation");

		return i;
	}

	public static void main(final String[] args) {
		final LandscapeGUI landGUI = new LandscapeGUI(args);
		final Console console = new Console(landGUI);
		console.setVisible(true);
	}

	public void quit() {
		super.quit();

		if (displayFrame != null) {
			displayFrame.dispose();
		}
		displayFrame = null;
		display = null;

		if (displayFrameRainfall != null) {
			displayFrameRainfall.dispose();
		}
		displayFrameRainfall = null;
		displayRainfall = null;

		if (displayFrameVegetation != null) {
			displayFrameVegetation.dispose();
		}

		displayFrameVegetation = null;
		displayVegetation = null;

	}
}
