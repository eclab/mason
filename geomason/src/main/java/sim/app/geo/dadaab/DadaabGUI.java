/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.app.geo.dadaab;

//import sim.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;

import javax.swing.JFrame;

//import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.dial.DialBackground;
import org.jfree.chart.plot.dial.DialCap;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialTextAnnotation;
import org.jfree.chart.plot.dial.DialValueIndicator;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialScale;

/**
 *
 * @author gmu
 */
//import java.awt.*;
//import javax.swing.JFrame;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.inspector.TabbedInspector;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.Valuable;
import sim.util.geo.MasonGeometry;

public class DadaabGUI extends GUIState {

	private Display2D display;
	private JFrame displayFrame;

	private Display2D displayRainfall;
	private JFrame displayFrameRainfall;

	// FastObjectGridPortrayal2D landPortrayal = new FastObjectGridPortrayal2D();
	FastValueGridPortrayal2D rainfallPortrayal = new FastValueGridPortrayal2D();

	ContinuousPortrayal2D refugeePortrayal = new ContinuousPortrayal2D();
	SparseGridPortrayal2D facilPortrayal = new SparseGridPortrayal2D();

	GeomVectorFieldPortrayal roadShapeProtrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal campShapeProtrayal = new GeomVectorFieldPortrayal();
	// FastValueGridPortrayal2D roadPortrayal = new FastValueGridPortrayal2D();

	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesCholera;
	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesCholeraNewly;

	sim.util.media.chart.TimeSeriesChartGenerator chartSeriesPopulation;

	sim.util.media.chart.ScatterPlotGenerator chartSeriesPopulation2;

	public static void main(final String[] args) {

		final DadaabGUI dadaabGUI = new DadaabGUI(args);
		final Console console = new Console(dadaabGUI);
		console.setVisible(true);
	}

	public DadaabGUI(final String[] args) {

		super(new Dadaab(System.currentTimeMillis(), args));

	}

	public DadaabGUI(final SimState state) {
		super(state);
	}

	public static String getName() {
		return "Dadaab Refugee Camp";
	}

	public Object getSimulationInspectedObject() {
		return state;
	} // non-volatile

	public void start() {
		super.start();
		// set up our portrayals
		setupPortrayals();
	}

	@Override
	public void load(final SimState state) {
		super.load(state);
		setupPortrayals();
	}

	public void setupPortrayals() {

		final Dadaab dadaab = (Dadaab) state;

//
//       landPortrayal.setField(dadaab.allCamps);20000
//       landPortrayal.setMap(new sim.util.gui.SimpleColorMap(new Color[]{new Color(255, 255, 255), new Color(224, 255, 224), new Color(255, 180, 210), new Color(204, 204, 153)}));
//         double rng = (dadaab.rainfallInMM * dadaab.rainDuration * 90*90 *5);
		rainfallPortrayal.setField(dadaab.rainfallGrid);
		final double rng = (dadaab.params.global.getRainfall_MM_Per_Minute() * 90 * 90 * 3); // 5 cell * area* amount of
																								// water
		rainfallPortrayal.setMap(new sim.util.gui.SimpleColorMap(0, rng, Color.WHITE, Color.BLUE));
//       elevation
//       rainfallPortrayal.setMap(new sim.util.gui.SimpleColorMap (110,140,Color.green, Color.red));
//       rainfallPortrayal.setMap(new sim.util.gui.SimpleColorMap (0,100000000,Color.WHITE, Color.RED));

		// refugeePortrayal.setObjectPosition(after, null, null)
		refugeePortrayal.setField(dadaab.allRefugees);

		final OvalPortrayal2D rPortrayal = new OvalPortrayal2D(0.20) {
			final Color healthy = new Color(0, 128, 0);
			final Color exposed = new Color(0, 0, 255); // 0-0-255 255-255-0 184-134-11
			final Color infected = new Color(255, 0, 0);
			final Color recovered = new Color(102, 0, 102);

			// to draw each refugee type with differnet color
			public void draw(final Object object, final Graphics2D graphics, final DrawInfo2D info) {
				if (object != null) {
					final double cType = ((Valuable) object).doubleValue();

					if (cType == 1) {
						paint = healthy;
					} else if (cType == 2) {
						paint = exposed;
					} else if (cType == 3) {
						paint = infected;
					} else {
						paint = recovered;
					}

					super.draw(object, graphics, info);
				} else {
					super.draw(object, graphics, info);
				}
			}
		};

		refugeePortrayal.setPortrayalForAll(rPortrayal);

		facilPortrayal.setField(dadaab.facilityGrid);
		// facility portrial
		final RectanglePortrayal2D facPortrayal = new RectanglePortrayal2D(1.0, false) {
			final Color borehole = new Color(0, 128, 255);
			final Color healthC = new Color(255, 0, 0);
			final Color school = new Color(0, 255, 0);
			final Color foodC = new Color(102, 0, 102);
			final Color mosq = new Color(0, 0, 102);
			final Color market = new Color(0, 102, 102);
			final Color other = new Color(255, 255, 255);

			// to draw each refugee type with differnet color
			public void draw(final Object object, final Graphics2D graphics, final DrawInfo2D info) {
				if (object != null) {

					final double cType = ((Valuable) object).doubleValue();
					if (cType == 1) {
						paint = school;

					} else if (cType == 2) {
						paint = borehole;
					} else if (cType == 3) {
						paint = mosq;

					} else if (cType == 4) {
						paint = market;

					} else if (cType == 5) {
						paint = foodC;

					} else if (cType == 6) {
						paint = healthC;

					} else {
						paint = other;
					}

					super.draw(object, graphics, info);
				} else {
					super.draw(object, graphics, info);
				}
			}
		};

		facilPortrayal.setPortrayalForAll(facPortrayal);

		// camp shape port..

		campShapeProtrayal.setField(dadaab.campShape);

		final GeomPortrayal gp = new GeomPortrayal(true) {
			final Color d = new Color(224, 255, 224);
			final Color i = new Color(255, 180, 210);
			final Color h = new Color(204, 204, 153);
			final Color o = new Color(255, 255, 255);

			public void draw(final Object object, final Graphics2D graphics, final DrawInfo2D info) {
				if (object != null) {
					final MasonGeometry mg = (MasonGeometry) object;

					// ArrayList cID = (ArrayList) mg.getAttribute("CAMPID");

					// AttributeValue key = new AttributeValue();

					final Integer cType = mg.getIntegerAttribute("CAMPID");
					// TODO why is CAMPID not being set?
					// It seems that while reading Camp_n.dbf the names of all attributes
					// show up as empty
					// Why is that??

					// int cType = (Integer) cID.get(afterSize);
					if (cType == null)
						paint = o;
					else if (cType == 1)
						paint = d;
					else if (cType == 2)
						paint = i;
					else if (cType == 3)
						paint = h;
					else
						paint = o;

					super.draw(object, graphics, info);
				} else {
					super.draw(object, graphics, info);
				}
			}

		};

		campShapeProtrayal.setPortrayalForAll(gp);

		roadShapeProtrayal.setField(dadaab.roadLinks);
		roadShapeProtrayal.setPortrayalForAll(new GeomPortrayal(Color.LIGHT_GRAY, false));

		// roadPortrayal.setField(dadaab.roadGrid);
		// roadPortrayal.setPortrayalForAll(new RectanglePortrayal2D(Color.LIGHT_GRAY));

		display.reset();
		display.setBackdrop(Color.white);
		// redraw the display
		display.repaint();

		displayRainfall.reset();
		displayRainfall.setBackdrop(Color.white);
		// redraw the display
		displayRainfall.repaint();

	}

	public void init(final Controller c) {

		super.init(c);

		display = new Display2D(380, 760, this);

		displayRainfall = new Display2D(380, 760, this);

		// display.attach(landPortrayal, "Camps");
		display.attach(campShapeProtrayal, "Camps Vector");
		display.attach(roadShapeProtrayal, "Road Vector");
		display.attach(refugeePortrayal, "Refugees");
		display.attach(facilPortrayal, "Facility");

		// Dadaab db = (Dadaab) state;

		displayFrame = display.createFrame();
		c.registerFrame(displayFrame);
		displayFrame.setVisible(true);

		displayRainfall.attach(rainfallPortrayal, "Rainfall");

		displayFrameRainfall = displayRainfall.createFrame();
		c.registerFrame(displayFrameRainfall);
		displayFrameRainfall.setVisible(false);
		displayFrameRainfall.setTitle("Rainfall");

		// Portray activity chart
		final JFreeChart chart = ChartFactory.createBarChart("Refugee's Activity", "Activity", "Percentage",
				((Dadaab) state).dataset, PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(Color.WHITE);
		chart.getTitle().setPaint(Color.BLACK);

		final CategoryPlot p = chart.getCategoryPlot();
		p.setBackgroundPaint(Color.WHITE);
		p.setRangeGridlinePaint(Color.red);

		// set the range axis to display integers only...
		final NumberAxis rangeAxis = (NumberAxis) p.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		final int max = 100; // ((Dadaab) this.state).getInitialRefugeeNumber();
		rangeAxis.setRange(0, max);

		final ChartFrame frame = new ChartFrame("Activity Chart", chart);
		frame.setVisible(false);
		frame.setSize(400, 350);

		frame.pack();
		c.registerFrame(frame);

		// Portray activity chart
		final JFreeChart agechart = ChartFactory.createBarChart("Age Distribution", "Age  Group",
				"Percentage of Total Population", ((Dadaab) state).agedataset, PlotOrientation.VERTICAL, false, false,
				false);
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

		// Portray activity chart
		final JFreeChart famchart = ChartFactory.createBarChart("Household Size", "Size", "Total",
				((Dadaab) state).familydataset, PlotOrientation.VERTICAL, false, false, false);
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
		//

		final Dimension dm = new Dimension(30, 30);
		final Dimension dmn = new Dimension(30, 30);

		chartSeriesCholera = new sim.util.media.chart.TimeSeriesChartGenerator();
		chartSeriesCholera.createFrame();
		chartSeriesCholera.setSize(dm);
		chartSeriesCholera.setTitle("Health Status");
		chartSeriesCholera.setRangeAxisLabel("Number of People");
		chartSeriesCholera.setDomainAxisLabel("Minutes");
		chartSeriesCholera.setMaximumSize(dm);
		chartSeriesCholera.setMinimumSize(dmn);
//        chartSeriesCholera.setMinimumChartDrawSize(400, 300); // makes it scale at small sizes
//        chartSeriesCholera.setPreferredChartSize(400, 300); // lets it be small

		chartSeriesCholera.addSeries(((Dadaab) state).totalsusceptibleSeries, null);
		chartSeriesCholera.addSeries(((Dadaab) state).totalExposedSeries, null);
		chartSeriesCholera.addSeries(((Dadaab) state).totalInfectedSeries, null);
		chartSeriesCholera.addSeries(((Dadaab) state).totalRecoveredSeries, null);

		chartSeriesCholera.addSeries(((Dadaab) state).totalBacteriaLoadSeries, null);

		chartSeriesCholera.addSeries(((Dadaab) state).rainfallSeries, null);

		final JFrame frameSeries = chartSeriesCholera.createFrame(this);
		frameSeries.pack();
		c.registerFrame(frameSeries);

		// newly cholera

		chartSeriesCholeraNewly = new sim.util.media.chart.TimeSeriesChartGenerator();

		chartSeriesCholeraNewly.createFrame();
		chartSeriesCholeraNewly.setSize(dm);
		chartSeriesCholeraNewly.setTitle("Health Status - Newly infected");
		chartSeriesCholeraNewly.setRangeAxisLabel("Number of People");
		chartSeriesCholeraNewly.setDomainAxisLabel("Minutes");
		chartSeriesCholeraNewly.setMaximumSize(dm);
		chartSeriesCholeraNewly.setMinimumSize(dmn);
//        chartSeriesCholera.setMinimumChartDrawSize(400, 300); // makes it scale at small sizes
//        chartSeriesCholera.setPreferredChartSize(400, 300); // lets it be small

		chartSeriesCholeraNewly.addSeries(((Dadaab) state).totalsusceptibleSeriesNewly, null);
		chartSeriesCholeraNewly.addSeries(((Dadaab) state).totalExposedSeriesNewly, null);
		chartSeriesCholeraNewly.addSeries(((Dadaab) state).totalInfectedSeriesNewly, null);
		chartSeriesCholeraNewly.addSeries(((Dadaab) state).totalRecoveredSeriesNewly, null);

		final JFrame frameSeriesNewly = chartSeriesCholeraNewly.createFrame(this);
		frameSeriesNewly.pack();
		c.registerFrame(frameSeriesNewly);

		// population dynamics

		chartSeriesPopulation = new sim.util.media.chart.TimeSeriesChartGenerator();

		chartSeriesPopulation.resize(100, 50);
		chartSeriesPopulation.setTitle("Refugee Population Dynamics");
		chartSeriesPopulation.setRangeAxisLabel(" Number of Refugees");
		chartSeriesPopulation.setDomainAxisLabel("Minutes");

		chartSeriesPopulation.addSeries(((Dadaab) state).totalTotalPopSeries, null);
		chartSeriesPopulation.addSeries(((Dadaab) state).totalDeathSeries, null);

		final JFrame frameSeriesPop = chartSeriesPopulation.createFrame(this);

		// frameSeriesPop.setSize(dmn)
		frameSeries.pack();
		c.registerFrame(frameSeriesPop);
		// time

		final StandardDialFrame dialFrame = new StandardDialFrame();
		final DialBackground ddb = new DialBackground(Color.white);
		dialFrame.setBackgroundPaint(Color.lightGray);
		dialFrame.setForegroundPaint(Color.darkGray);

		final DialPlot plot = new DialPlot();
		plot.setView(0.0, 0.0, 1.0, 1.0);
		plot.setBackground(ddb);
		plot.setDialFrame(dialFrame);

		plot.setDataset(0, ((Dadaab) state).hourDialer);
		plot.setDataset(1, ((Dadaab) state).dayDialer);

		final DialTextAnnotation annotation1 = new DialTextAnnotation("Hour");
		annotation1.setFont(new Font("Dialog", Font.BOLD, 14));
		annotation1.setRadius(0.1);
		plot.addLayer(annotation1);

//        DialValueIndicator dvi = new DialValueIndicator(0);
//        dvi.setFont(new Font("Dialog", Font.PLAIN, 10));
//        dvi.setOutlinePaint(Color.black);
//        plot.addLayer(dvi);
//

		final DialValueIndicator dvi2 = new DialValueIndicator(1);
		dvi2.setFont(new Font("Dialog", Font.PLAIN, 22));
		dvi2.setOutlinePaint(Color.red);
		dvi2.setRadius(0.3);
		plot.addLayer(dvi2);

		final DialTextAnnotation annotation2 = new DialTextAnnotation("Day");
		annotation2.setFont(new Font("Dialog", Font.BOLD, 18));
		annotation2.setRadius(0.4);
		plot.addLayer(annotation2);

		final StandardDialScale scale = new StandardDialScale(0.0, 23.99, 90, -360, 1.0, 59);
		scale.setTickRadius(0.9);
		scale.setTickLabelOffset(0.15);
		scale.setTickLabelFont(new Font("Dialog", Font.PLAIN, 12));
		plot.addScale(0, scale);
		scale.setMajorTickPaint(Color.black);
		scale.setMinorTickPaint(Color.lightGray);

//        StandardDialScale scale2 = new StandardDialScale(1, 7, -150, -240, 1,1);
//        scale2.setTickRadius(0.50);
//        scale2.setTickLabelOffset(0.15);
//        scale2.setTickLabelPaint(Color.RED);
//        scale2.setTickLabelFont(new Font("Dialog", Font.PLAIN, 12));
//        plot.addScale(1, scale2);
//
//        DialPointer needle2 = new DialPointer.Pin(1);
//        plot.addPointer(needle2);
//        needle2.setRadius(0.40);
		// plot.mapDatasetToScale(1, 1);

		final DialPointer needle = new DialPointer.Pointer(0);
		plot.addPointer(needle);

		final DialCap cap = new DialCap();
		cap.setRadius(0.10);
		plot.setCap(cap);

		final JFreeChart chart1 = new JFreeChart(plot);
		final ChartFrame timeframe = new ChartFrame("Time Chart", chart1);
		timeframe.setVisible(false);
		timeframe.setSize(200, 100);
		timeframe.pack();
		c.registerFrame(timeframe);

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
				((Dadaab) state).params.global, this), "Paramters");
		return i;
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

	}

}
