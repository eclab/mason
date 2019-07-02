/*
 * WorldGUI.java
 * 
 * $Id: WorldGUI.java 2002 2013-08-14 20:14:42Z jharri $
 * 
 */
package sim.app.geo.riftland;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.*;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import sim.app.geo.riftland.conflict.Conflict;
import sim.app.geo.riftland.gui.FarmPortrayal2D;
import sim.app.geo.riftland.gui.SnailTrailPortrayal;
import sim.app.geo.riftland.gui.TriColorMap;
import sim.app.geo.riftland.household.Herding;
import sim.app.geo.riftland.household.Household;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.app.geo.riftland.parcel.WaterHole;
import sim.app.geo.riftland.util.YScalableXYSeries;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.IntGrid2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.FastObjectGridPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.inspector.TabbedInspector;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.IntBag;
import sim.util.Valuable;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.DataCuller;
import sim.util.media.chart.HistogramGenerator;
import sim.util.media.chart.TimeSeriesAttributes;
import sim.util.media.chart.TimeSeriesChartGenerator;
import sim.app.geo.riftland.riftlandData.RiftlandData;

/** GUI wrapper for World
 *
 */
public class WorldGUI extends GUIState
{
    /** This is the main display showing the terrain and activities */
    public Display2D display;
    /** This is the secondary display showing the population distribution
     *  and the households. */
    private Display2D populationDisplay;
    /** Tertiary display for showing dailyRainfall */
    private Display2D rainfallDisplay;
    /** For showing vegetation changes */
    private Display2D vegetationDiffDisplay;
    /** For showing the DiscreteVoronoi diagram built around waterHoles */
    private Display2D voronoiDisplay;

    //<editor-fold defaultstate="collapsed" desc="Field Portrayals">
    /** rendering of land */
    ObjectGridPortrayal2D landPortrayal = new FastObjectGridPortrayal2D();
    ObjectGridPortrayal2D farmLandPortrayal = new ObjectGridPortrayal2D();
    
    FastObjectGridPortrayal2D populationPortrayal;
    FastObjectGridPortrayal2D populationChangePortrayal;
    /** for showing households */
    SparseGridPortrayal2D householdPortrayal = new SparseGridPortrayal2D();
    
    // Should show herding on grid
    SparseGridPortrayal2D herderPortrayal = new SparseGridPortrayal2D();
    SparseGridPortrayal2D herderTrailPortrayal = new SparseGridPortrayal2D();

    // Show water holes
    SparseGridPortrayal2D waterHolePortrayal = new SparseGridPortrayal2D();
    ObjectGridPortrayal2D waterHoleVoronoiPortrayal;

    // Show parcels in conflict
    SparseGridPortrayal2D conflictPortrayal = new SparseGridPortrayal2D();
    SparseGridPortrayal2D escalationPortrayal = new SparseGridPortrayal2D();

    /** for rendering the ethnic layer */
    private GeomVectorFieldPortrayal ethnicFieldPortrayal = new GeomVectorFieldPortrayal();

    /** for rendering the political boundaries layer */
    private GeomVectorFieldPortrayal politicalBoundaryPortrayal = new GeomVectorFieldPortrayal();

    /** for rendering LandScan population data */
    private FastValueGridPortrayal2D populationFieldPortrayal = new FastValueGridPortrayal2D();

    /** Visualizes the amount of dailyRainfall in each step */
    private FastValueGridPortrayal2D rainfallPortrayal = new FastValueGridPortrayal2D();

    /** Visualizes the difference in vegetation per parcel between steps */
    private FastValueGridPortrayal2D vegetationDiffPortrayal = new FastValueGridPortrayal2D();

    /** List containing all frames. Used for cleanup after a run. */
    private java.util.List<JFrame> frames = new ArrayList<JFrame>();
    
    /** List containing all displays. Used for cleanup after a run. */
    private java.util.List<Display2D> displays = new ArrayList<Display2D>();

    // Population chart
    TimeSeriesChartGenerator popChart;  
    YScalableXYSeries weatherSeries;        
    YScalableXYSeries farmerSeries;         // data series of farmer population
    YScalableXYSeries herderSeries;         // data series of herder pop
    YScalableXYSeries laborerSeries;        // data series of labor pop
    YScalableXYSeries displacedSeries;      // data series of displaced people (activity pop)
    YScalableXYSeries nonDisplacedSeries;      // data series of nondisplaced people
    
    // population change chart
    TimeSeriesChartGenerator popChangeChart; // 2nd charting facility
    YScalableXYSeries popChangeRateSeries;   // tracking resulting change rate
    
    // Conflict chart
    TimeSeriesChartGenerator conflictChart;
    YScalableXYSeries conflictSeries;       
    YScalableXYSeries herdingFarmerConflictSeries; 
    YScalableXYSeries herderHerderConflictSeries; 
    YScalableXYSeries herderCooperationSeries;
    
    // Wealth histogram
    HistogramGenerator wealthHistogram;
    
    //</editor-fold>

    public WorldGUI(String[] args)
    {
        //false means I tell World to not schedule the mediator.
        // XXX why is mediator schedulable in World *and* WorldGUI?!
        super(new World(System.currentTimeMillis(), args, false));
        initCustomPortrayals();
    }

    public WorldGUI(SimState state)
    {
        super(state);
        initCustomPortrayals();
    }
    
    //=====================
    //
    //   INIT
    //
    //=====================

    /** 
     * Create custom portrayals. These must be initialized on construction of the class,
     * so this is called from the constructor.
     */
	final public void initCustomPortrayals() {
		final World world = (World)this.state;
		
		populationPortrayal = new FastObjectGridPortrayal2D() {
			@Override
			public double doubleValue(Object obj) {
				if (!(obj instanceof GrazableArea)) return 0;
				GrazableArea area = (GrazableArea) obj;

				return area.getPopulation();
			}
		};
		

		populationChangePortrayal = new FastObjectGridPortrayal2D() {
			final IntGrid2D grid = (IntGrid2D) world.getPopulation().getPopulationGrid().getGrid();
			@Override
			public double doubleValue(Object obj) {
				if (!(obj instanceof GrazableArea)) return 0;
				GrazableArea area = (GrazableArea) obj;
				double currentPop = area.getPopulation();
				double startPop = grid.get(area.getX(), area.getY());
				
				return (currentPop - startPop) / (startPop+1);
			}
		};
	}
	
    @Override
    public void init(Controller controller)
    {
        super.init(controller);
        
        displays.clear();
        displays.add(display = new Display2D(312, 336, this));
        displays.add(populationDisplay = new Display2D(312, 336, this));
        displays.add(rainfallDisplay = new Display2D(312, 336, this));
        displays.add(vegetationDiffDisplay = new Display2D(312, 336, this));
        displays.add(voronoiDisplay = new Display2D(312, 336, this));
        
        frames.clear();       
        registerFrame(display, "WorldGUI", true, true);
        registerFrame(populationDisplay, "Population and Households", false, true);
        registerFrame(rainfallDisplay, "Daily Rainfall", false, true);
        registerFrame(vegetationDiffDisplay, "Daily Vegetation Difference", false, true);
        registerFrame(voronoiDisplay, "Voronoi Diagram for WaterHoles", false, true);
        

        //===============
        //  Charts
        //===============
        
        popChart = new TimeSeriesChartGenerator();
        registerChartFrame(popChart, "Populations, Rainfall", "Years", "People", false);

        popChangeChart = new TimeSeriesChartGenerator();
        registerChartFrame(popChangeChart, "Population Change Rate", "Days", "Annual Percent of Population Change", false);

        conflictChart = new TimeSeriesChartGenerator();
        registerChartFrame(conflictChart, "Conflicts", "Days", "Conflicts", false);
        


        for (Display2D d : displays)
        	setViewToSubArea(d);
    }
    
    /** Create a frame from the given display and register it using the given parameters. */
    private void registerFrame(Display2D display, String title, boolean visible, boolean setLocationByPlatform) {
    	JFrame frame = display.createFrame();
    	frame.setTitle(title);
    	frame.setLocationByPlatform(setLocationByPlatform);
    	controller.registerFrame(frame);
    	frame.setVisible(visible);
    	frames.add(frame);    	
    }
    
    /** Create a frame from the given chart and register it using the given parameters. */
    private void registerChartFrame(TimeSeriesChartGenerator chart, String title, String xLabel, String yLabel, boolean visible) {
    	chart.setTitle(title);
    	chart.setXAxisLabel(xLabel);
    	chart.setYAxisLabel(yLabel);
    	JFrame frame = chart.createFrame(this);
    	frame.pack();
    	controller.registerFrame(frame);
    	frame.setVisible(visible);
    	frames.add(frame);    	
    }

    /** Calculate the size of the subarea and set the zoom to view it. */
    public void setViewToSubArea(Display2D d) {
    	World world = (World)state;
    	if (!world.getLand().haveSubArea())
    		return;

    	double width = world.getLand().getWidth();
    	double height = world.getLand().getHeight();
    	
    	double subWidth = Math.abs(world.getLand().getSubAreaLowerRight().x - world.getLand().getSubAreaUpperLeft().x);
    	double subHeight = Math.abs(world.getLand().getSubAreaLowerRight().y  - world.getLand().getSubAreaUpperLeft().y);
    	
    	double scaleX = width / subWidth;
    	double scaleY = height / subHeight;

    	// Set the scroll based on the upper-left corner of the subarea, and where
    	// it falls within the overall area. Since the upper-left corner cant't be
    	// all the way to the right or down, we subtract off the size of the subarea.
    	double scrollX = world.getLand().getSubAreaUpperLeft().x / (width - subWidth);
    	double scrollY = world.getLand().getSubAreaUpperLeft().y / (height - subHeight);
    	
    	d.setScale(Math.min(scaleX, scaleY) * 0.95);
    	d.setScrollPosition(scrollX, scrollY);
    }

    //=====================
    //
    //  START
    //
    //=====================


    @Override
    public void start()
    {
        super.start();

//        setAllHerdersMaxTrailLengths();
        setupPortrayals();
        setupCharts();

        // Step the mediator *before* everything else, but especially after
        // the display is updated, so that we can see the conflict objects
        // before the mediator nukes them.
        scheduleRepeatingImmediatelyAfter(((World) this.state).getMediator());
    }



    /**
     * Load is called when a checkpoint is loaded in.  This function should
     * basically do the same things that start() does, so if you add anything
     * there, add it here too.
     *
     * XXX If they do the same thing, why not consolidate the functionality in a
     * single function and call that from both start() and load()?
     */
    @Override
    public void load(SimState state)
    {
        super.load(state);
//        setAllHerdersMaxTrailLengths();
        setupPortrayals();

        // Step the mediator *before* everything else, but especially after
        // the display is updated, so that we can see the conflict objects
        // before the mediator nukes them.
        scheduleRepeatingImmediatelyAfter(((World) this.state).getMediator());
    }


    /* I duplicated TimeSeriesChartingPropertyInspector.addToMainSeries cause
     * I need the statistics done at a particular ordering (i.e. before the mediator).
     *
     * Plus, since all series get data points at the same time, I only need to run the
     * data culling algorithm once!
     */
    private double[] getXValues(XYSeries series)
    {
        double[] xValues = new double[series.getItemCount()];
        for (int i = 0; i < xValues.length; i++)
        {
            xValues[i] = series.getX(i).doubleValue();
        }
        return xValues;
    }

    /**
     * XXX What is this?  <- As I recall, a global variable of agents for ??? //Bill
     */
    private static Bag tmpBag = new Bag();

    /**
     * XXX What does this do?  It uses the above tmpBag, which, as far as I can
     * tell is only used here in deleteItems(); so why is tmpBag declared out-
     * side deleteItems()?
     * 
     * @param items
     * @param series
     */
    private void deleteItems(IntBag items, XYSeries series)
    {
        if (items.numObjs == 0)
        {
            return;
        }
        
//      //I would sure hate to to do this (O(n^2), plus each remove causes a SeriesChangeEvent):
//      for(int i=items.numObjs-1;i>=0;i--)
//          chartSeries.remove(items.objs[i]);
        //here's the O(n) version (and just 2 SeriesChangeEvents)

        tmpBag.clear();
        int currentTabooIndex = 0;
        int currentTaboo = items.objs[0];
        java.util.Iterator iter = series.getItems().iterator();
        int index = 0;
        while (iter.hasNext())
        {
            Object o = iter.next();
            if (index == currentTaboo)
            {
                //skip the copy, let's move on to next taboo index
                if (currentTabooIndex < items.numObjs - 1)
                {
                    currentTabooIndex++;
                    currentTaboo = items.objs[currentTabooIndex];
                } else
                {
                    currentTaboo = -1;//no more taboos
                }
            } else//save o
            {
                tmpBag.add(o);
            }
            index++;
        }
        //now we clear the chartSeries and then put back the saved objects only.
        series.clear();
        //In my test this did not cause the chart to flicker.
        //But if it does, one could do an update for the part the will be refill and
        //only clear the rest using delete(start, end).
        for (int i = 0; i < tmpBag.numObjs; i++)
        {
            series.add((XYDataItem) tmpBag.objs[i], false);//no notifying just yet.
        }
        tmpBag.clear();
        //it doesn't matter that I clear this twice in a row
        //(once here, once at next time through this fn), the second time is O(1).
        series.fireSeriesChanged();
    }

    /** Builds a color map for the given minimum and maximum values
     *
     * Does a log ramping of colors to highlight lower population area structure
     *
     * @param min smallest population (not used ... yet)
     * @param max largest population
     * @return color map suitable for SimpleColorMap ctor
     */
    private Color[] buildColorMap(int min, int max)
    {
        Color[] colors = new Color[max];

        int curColor = 0;

        double maxValue = java.lang.Math.log(max);

        try
        {
            for (int i = 1; i <= max; i++)
            {
                curColor = /* 255 - */ (int) (255.0 * java.lang.Math.log((double) i) / maxValue);

                colors[i - 1] = new Color(curColor, curColor, curColor);
            }
        } catch (Exception e)
        {
            System.err.println(e);
        }

        return colors;
    }
    
    private void setupPortrayals()
    {
        final World world = (World) state;

        landPortrayal = world.getLand().getPortrayal();        
        //landPortrayal.setBuffering(FastObjectGridPortrayal2D.USE_BUFFER);

        farmLandPortrayal.setField(world.getLand().getLandGrid());
        farmLandPortrayal.setPortrayalForAll(new FarmPortrayal2D());

        waterHolePortrayal.setField(world.getWaterHoles().getWaterHolesGrid());
        RectanglePortrayal2D individualWaterHolePortrayal = new RectanglePortrayal2D(1)
        {
            final SimpleColorMap colorMap = new SimpleColorMap(0.0, 1.0, Color.WHITE, Color.BLUE);

            @Override
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
            {
                if (object != null)
                {   
                    assert(object instanceof WaterHole);
                    WaterHole waterHole = (WaterHole) object;

                    // We want how much water the watering hole has
                    double howFull = waterHole.getWater() / waterHole.getMaxWater();

                    Color valuedColor = colorMap.getColor(howFull);

                    paint = valuedColor;

                    super.draw(object, graphics, info);
                } else
                {
                    super.draw(object, graphics, info);
                }
            }

        };
        

//        waterHolePortrayal.setPortrayalForAll(new RectanglePortrayal2D(Color.BLUE));
        waterHolePortrayal.setPortrayalForAll(individualWaterHolePortrayal);
        waterHoleVoronoiPortrayal = (world.getWaterHoles().getVoronoi() == null) ? null : world.getWaterHoles().getVoronoi().getPortrayal();          
        
        householdPortrayal.setField(world.getPopulation().getHouseholdsGrid());
        householdPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.MAGENTA, 1.0, true));

        herderPortrayal.setField(world.getPopulation().getHerdingGrid());

        
        /* 
         *  Herders shown as circles with the color indicating their state
         */
        OvalPortrayal2D individualHerderPortrayal = new OvalPortrayal2D(0.5)
        {
            @Override
            @SuppressWarnings("empty-statement")
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
            {
                if (object != null)
                {   
                    final double MIN_SIZE = world.getParams().gui.getMinHerdingScale();
                    final double MAX_SIZE = world.getParams().gui.getMaxHerdingScale();
                    final int TLUS_FOR_MAX_SIZE = world.getParams().herding.getHerdSplitThreshold();
                    final int TLUS = ((Herding)object).getHerdSize();
                    scale = (double)TLUS/TLUS_FOR_MAX_SIZE * (MAX_SIZE - MIN_SIZE) + MIN_SIZE;
                    
                    if (world.getParams().gui.getHerdingJitter() == Parameters.HerdingJitter.FIXED)
                    {
                        double jitterX = ((Herding)object).getJitterX()*info.draw.width;
                        double jitterY = ((Herding)object).getJitterY()*info.draw.height;
                        info = new DrawInfo2D(info, jitterX, jitterY);
                    }
                    else if (world.getParams().gui.getHerdingJitter() == Parameters.HerdingJitter.RANDOM)
                    {
                        double jitterX = (world.random.nextDouble()*0.8 - 0.4)*info.draw.width;
                        double jitterY = (world.random.nextDouble()*0.8 - 0.4)*info.draw.height;
                        info = new DrawInfo2D(info, jitterX, jitterY);
                    }
                    
                    double scaledHunger = ((Herding) object).getScaledHunger();
                    double scaledThirst = ((Herding) object).getScaledThirst();

                    // Health will be the worst of thirst or hunger
                    double health = Math.min(scaledHunger, scaledThirst);
                    health = Math.min(health, 1);
                    health = Math.max(health, 0);
                    //assert((health >= 0) && (health <= 1)) : System.out.format("Herd health outside valid range [0,1]: %f\n", health);

                    // Map normalized health from [0,1] to [64,255] levels of transparency
                    paint = getTransparentColor(Color.RED, 64 + (int)(191 * health));
                    super.draw(object, graphics, info);
                } else
                {
                    super.draw(object, graphics, info);
                }
            }
        };

        herderPortrayal.setPortrayalForAll(individualHerderPortrayal);


        rainfallPortrayal.setField(world.dailyRainfallGrid);
        rainfallPortrayal.setMap(new SimpleColorMap(0.0, 25.0, Color.BLACK, Color.BLUE));

        vegetationDiffPortrayal.setField(world.vegetationChanges);

        Color [] noChange = new Color[1];
        noChange[0] = Color.BLACK;
        vegetationDiffPortrayal.setMap(new SimpleColorMap(noChange, -500, 500, Color.RED, Color.GREEN));

        herderTrailPortrayal.setField(world.getPopulation().getHerdingGrid());
        herderTrailPortrayal.setPortrayalForAll(new SnailTrailPortrayal(Color.lightGray, Color.darkGray, true));
        //true = draw intermediary points on the trail.
//        herdingFarmerHerderPortrayal.setPortrayalForAll(individualHerderPortrayal);
//
//        herdingFarmerTrailPortrayal.setField(world.herdingFarmers);
//        herdingFarmerTrailPortrayal.setPortrayalForAll(new SnailTrailPortrayal(Color.lightGray, Color.darkGray, true));

//        escalationPortrayal.setField(world.getMediator().getEscalationsGrid());
//        escalationPortrayal.setPortrayalForAll(new RectanglePortrayal2D(Color.red, world.getMediator().getEscalationDistance() * 2 + 1, false));

//        conflictPortrayal.setField(world.conflictsGrid);
        conflictPortrayal.setField(world.getMediator().getPrevConflictsGrid());

        // Render conflicts in different colors depending on the type.
        // 2*2+1: extend portrayal 2 squares away in each direction
        // false: no fill

        RectanglePortrayal2D individualConflictPortrayal = new RectanglePortrayal2D(Color.MAGENTA, 5, true)
        {
            @Override
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
            {
                if (object != null)
                {
                    double cType = ((Valuable) object).doubleValue();

                    if (cType == Conflict.CONFLICT_TYPE_HF)
                    {
                        paint = hfColor;
                    } else if (cType == Conflict.CONFLICT_TYPE_HH_TBD) //CONFLICT_TYPE_HH_WITHIN_CULT)
                    {
                        paint = hhInCultColor;//Color.CYAN;
                    } else
                    {
                        paint = hhInterCultColor;//Color.MAGENTA;
                    }
                    super.draw(object, graphics, info);
                } else
                {
                    super.draw(object, graphics, info);
                }
            }

        };

        conflictPortrayal.setPortrayalForAll(individualConflictPortrayal);

        ethnicFieldPortrayal.setField(world.getPopulation().getEthnicRegionsGrid());
        ethnicFieldPortrayal.setPortrayalForAll(new GeomPortrayal(Color.PINK, false));

        this.politicalBoundaryPortrayal.setField(world.getLand().getPoliticalBoundaries());
        this.politicalBoundaryPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED, false));
        

        Color[] populationColors = buildColorMap(0, ((IntGrid2D)world.getPopulation().getPopulationGrid().getGrid()).max());


        this.populationFieldPortrayal.setField(world.getPopulation().getPopulationGrid().getGrid());
        this.populationFieldPortrayal.setMap(new SimpleColorMap(populationColors));

        populationPortrayal.setField(world.getLand().getLandGrid());
        populationPortrayal.setMap(new SimpleColorMap(populationColors));
        
        populationChangePortrayal.setField(world.getLand().getLandGrid());
        populationChangePortrayal.setMap(new TriColorMap(-1, 0, 1, Color.red, Color.white, Color.blue));

        display.reset();
        display.setBackdrop(Color.WHITE);

        display.repaint();

        populationDisplay.reset();
        populationDisplay.setBackdrop(Color.WHITE);

        populationDisplay.repaint();
        
        attachPortrayals();
    }
    
    private void attachPortrayals()
    {
        display.attach(landPortrayal, "Land");
        display.attach(farmLandPortrayal, "Farm Land");  
        display.attach(waterHolePortrayal, "Water Holes");
        display.attach(conflictPortrayal, "Conflicts", false);
        display.attach(herderTrailPortrayal, "SnailTrails", false);
        display.attach(escalationPortrayal, "Escalations", false);
        display.attach(herderPortrayal, "Herding");
        display.attach(ethnicFieldPortrayal, "Ethnicities", false);
        display.attach(politicalBoundaryPortrayal, "Political Boundaries");

        populationDisplay.attach(this.populationFieldPortrayal, "Initial Population", false);
        populationDisplay.attach(this.politicalBoundaryPortrayal, "Political Boundaries", false);
        populationDisplay.attach(this.householdPortrayal, "Households", true);
        populationDisplay.attach(populationPortrayal, "Population", false);
        populationDisplay.attach(populationChangePortrayal, "Population Change", false);

        rainfallDisplay.attach(this.rainfallPortrayal, "Rainfall");

        vegetationDiffDisplay.attach(vegetationDiffPortrayal, "Daily Vegetation Difference");
        
        voronoiDisplay.attach(waterHoleVoronoiPortrayal, "Voronoi Diagram for WaterHoles");
    }
    
    private void setupCharts()
    {
        popChart.removeAllSeries();
        popChangeChart.removeAllSeries();
        conflictChart.removeAllSeries();

        // Population and weather
        weatherSeries = new YScalableXYSeries("Weather");  //, 1d/10);
        herderSeries = new YScalableXYSeries("Herders");
        farmerSeries = new YScalableXYSeries("Farmers");
        laborerSeries = new YScalableXYSeries("Laborers");
        displacedSeries = new YScalableXYSeries("Displaced");
        nonDisplacedSeries = new YScalableXYSeries("NonDisplaced");
        popChart.addSeries(weatherSeries, null).setPlotVisible(false);
        //popChart.addSeries(herderSeries, null);
        ((TimeSeriesAttributes)popChart.addSeries(herderSeries, null)).setStrokeColor(new Color(0,0,255));
        //popChart.addSeries(farmerSeries, null);
        ((TimeSeriesAttributes)popChart.addSeries(farmerSeries, null)).setStrokeColor(new Color(0,255,0));
        ((TimeSeriesAttributes)popChart.addSeries(laborerSeries, null)).setStrokeColor(new Color(255,0,0)); // (new Color(255, 219, 88));
        //popChart.addSeries(displacedSeries, null);
        ((TimeSeriesAttributes)popChart.addSeries(displacedSeries, null)).setStrokeColor(new Color(150,150,150));
        //popChart.addSeries(nonDisplacedSeries, null);
        ((TimeSeriesAttributes)popChart.addSeries(nonDisplacedSeries, null)).setStrokeColor(new Color(0,0,0));

        // Population change
        popChangeRateSeries = new YScalableXYSeries("PopChangeRate");
        popChangeChart.addSeries(popChangeRateSeries, null);    

        // Conflict
        conflictSeries = new YScalableXYSeries("ConflictTimeSeriesChart");
        herdingFarmerConflictSeries = new YScalableXYSeries("HerderFarmerConflictTimeSeriesChart");
        herderHerderConflictSeries = new YScalableXYSeries("HerderHerderConflictTimeSeriesChart");
        herderCooperationSeries = new YScalableXYSeries("HerderHerderNonConflictTimeSeriesChart");
        conflictChart.addSeries(conflictSeries, null);
        conflictChart.addSeries(herdingFarmerConflictSeries, null);
        conflictChart.addSeries(herderHerderConflictSeries, null);
        conflictChart.addSeries(herderCooperationSeries, null);

        this.scheduleRepeatingImmediatelyBefore(new Steppable()
        {
            @Override
            public void step(SimState state)
            {                
                // at this stage we're adding data to our chart.  We
                // need an X value and a Y value.  Typically the X
                // value is the schedule's timestamp.  The Y value
                // is whatever data you're extracting from your
                // simulation.

                World world = (World) state;
                double x = state.schedule.getTime();
                double hh = world.getMediator().getHHconflicts();
                double hn = world.getMediator().getHHnonconflicts();
                double hf = world.getMediator().getHFconflicts();

                double y_w = world.getWeather().getAvgMonthlyRainfall();
                double y_h = world.getPopulation().getCurrentNumHouseholds();
                double fpop = world.getPopulation().getCurrentPopulationFarmers();
                double hpop = world.getPopulation().getCurrentPopulationHerders();
                double labpop = world.getPopulation().getCurrentPopulationLaborers();
                double disppop = world.getPopulation().getCurrentPopulationDisplaced();
                double nonDisPop = world.getPopulation().getNonDisplacedPopulation();

                // monitor population growth
                double popChangeRate = world.getPopulation().calculateCurrentPopulationChangeRate();
                
                // now add the data
                if (x >= Schedule.EPOCH && x < Schedule.AFTER_SIMULATION)
                {
                    // Note: some conflicts may be ignored, so hf+hh is
                    //       now more accurate than y
                    // Also: If you add another plot here, you should probably
                    //       also write it to the csv file in WorldObserver.step()
                	final double ticksToYears = 1.0 / 365.25;
                    weatherSeries.add(x*ticksToYears, y_w, false);
                    farmerSeries.add(x*ticksToYears, fpop, false);
                    herderSeries.add(x*ticksToYears, hpop, false);
                    laborerSeries.add(x*ticksToYears, labpop, false);
                    displacedSeries.add(x*ticksToYears, disppop, false);
                    nonDisplacedSeries.add(x*ticksToYears, nonDisPop, false);

                    conflictSeries.add(x, hf + hh, false);
                    herdingFarmerConflictSeries.add(x, hf, false);
                    herderHerderConflictSeries.add(x, hh, false);
                    herderCooperationSeries.add(x, hn, false);
                    herdingFarmerConflictSeries.add(x, hn, false);
                   
                    // chart2 population change rate
                    popChangeRateSeries.add(x, popChangeRate, false);
                    
                    DataCuller dataCuller = popChart.getDataCuller();
                    if (dataCuller != null && dataCuller.tooManyPoints(farmerSeries.getItemCount()))
                    {
                        double[] xValues = getXValues(farmerSeries);
                        IntBag droppedDataPointIndices = dataCuller.cull(xValues, true);

                        deleteItems(droppedDataPointIndices, weatherSeries);
                        deleteItems(droppedDataPointIndices, farmerSeries);
                        deleteItems(droppedDataPointIndices, herderSeries);
                        deleteItems(droppedDataPointIndices, laborerSeries);
                        deleteItems(droppedDataPointIndices, displacedSeries);
                        deleteItems(droppedDataPointIndices, nonDisplacedSeries);
                    } else
                    {
                        weatherSeries.fireSeriesChanged();
                        farmerSeries.fireSeriesChanged();
                        herderSeries.fireSeriesChanged();
                        laborerSeries.fireSeriesChanged();
                        displacedSeries.fireSeriesChanged();
                        nonDisplacedSeries.fireSeriesChanged();
                    }

                    // for 2th chart, show resulting population change rate
                    dataCuller = popChangeChart.getDataCuller();
                    if(dataCuller != null && dataCuller.tooManyPoints(conflictSeries.getItemCount()))
                    {
                        double[] xValues = getXValues(conflictSeries);
                        IntBag droppedDataPointIndices = dataCuller.cull(xValues, true);

                        deleteItems(droppedDataPointIndices, popChangeRateSeries);
                    }
                    else
                    {
                        popChangeRateSeries.fireSeriesChanged();
                    }
                    
                    dataCuller = conflictChart.getDataCuller();
                    if (dataCuller != null && dataCuller.tooManyPoints(conflictSeries.getItemCount()))
                    {
                        double[] xValues = getXValues(conflictSeries);
                        IntBag droppedDataPointIndices = dataCuller.cull(xValues, true);

                        deleteItems(droppedDataPointIndices, conflictSeries);
                        deleteItems(droppedDataPointIndices, herdingFarmerConflictSeries);
                        deleteItems(droppedDataPointIndices, herderHerderConflictSeries);
                        deleteItems(droppedDataPointIndices, herderCooperationSeries);
                    } else
                    {
                        conflictSeries.fireSeriesChanged();
                        herdingFarmerConflictSeries.fireSeriesChanged();
                        herderHerderConflictSeries.fireSeriesChanged();
                        herderCooperationSeries.fireSeriesChanged();
                    }

                }
            }

        }); //, 3, 1.0); // schedule before mediator, which is at 2
    }

    /**
     * XXX What is this?  Parcel display color of herder-farmer conflicts //Bill
     */
    final Color hfColor = getTransparentColor(Color.red, 200);//Color.RED;

    /**
     * Parcel display color for herder-herder within Cult sharing //Bill
     */
    final Color hhInCultColor = getTransparentColor(Color.cyan, 200);//Color.CYAN;

    /**
     * XXX What is this? Parcel display color of herder-herder conflict w/ diff Cults  //Bill
     */
    final Color hhInterCultColor = getTransparentColor(Color.magenta, 200);//Color.MAGENTA;

    /** Color.java:  An alpha value of 1.0 or 255 means that the color is completely opaque 
     ** and an alpha value of 0 or 0.0 means that the color is completely transparent.
     */
    public static Color getTransparentColor(Color c, int transparency)
    {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), transparency);
    }

    @Override
    public Object getSimulationInspectedObject()
    {
        return state;
    }

    @Override
    public Inspector getInspector()
    {
        super.getInspector();

        TabbedInspector i = new TabbedInspector();

        i.setVolatile(true);
        i.addInspector(new SimpleInspector(((World)state).getParams().system, this), "System");
        i.addInspector(new SimpleInspector(((World)state).getParams().gui, this), "GUI");
        i.addInspector(new SimpleInspector(((World)state).getParams().world, this), "World");
        i.addInspector(new SimpleInspector(((World)state).getParams().households, this), "HouseHolds");
        i.addInspector(new SimpleInspector(((World)state).getParams().herding, this), "Herding");
        i.addInspector(new SimpleInspector(((World)state).getParams().farming, this), "Farming");
        i.addInspector(new SimpleInspector(((World)state).getParams().vegetation, this), "Vegetation");
        i.addInspector(new SimpleInspector(new ConflictProperties((World)state), this), "Conflict");
        i.addInspector(new SimpleInspector(new WeatherProperties((World)state), this), "Weather");
        i.addInspector(new SimpleInspector(new ObservationProperties((World)state), this), "Observe");
//        i.addInspector(new SimpleInspector(new TimeSeriesScales(), this), "SeriesScales");

        return i;
    }

    /**
     * @param args the command line arguments
     */
      public static void main(String[] args)
    {
        WorldGUI worldGUI = new WorldGUI(args);

        Console console = new Console(worldGUI);
        console.setVisible(true);
//        System.exit(0);
    }

    public static class ConflictProperties
    {
        World world_;

		private ConflictProperties(World world) {
			world_ = world;
		}

		public boolean getHerderHerderConflict() {
			return world_.getMediator().getHerderHerderConflictActive();
		}

		public void setHerderHerderConflict(boolean hh) {
			world_.getMediator().setHerderHerderConflictActive(hh);
		}

		public boolean getWithinCultConflict() {
			return world_.getMediator().getWithinCultConflictActive() && world_.getMediator().getHerderHerderConflictActive();
		}

		public void setWithinCultConflict(boolean wc) {
			world_.getMediator().setWithinCultConflictActive(wc);
		}

		public boolean getHerderFarmerConflict() {
			return world_.getMediator().getHerderFarmerConflictActive();
		}

		public void setHerderFarmerConflict(boolean hf) {
			world_.getMediator().setHerderFarmerConflictActive(hf);
		}

		public boolean getEscalateConflict() {
			return world_.getMediator().getEscalateConflictActive() && world_.getMediator().getHerderFarmerConflictActive();
		}

		public void setEscalateConflict(boolean ec) {
			world_.getMediator().setEscalateConflictActive(ec);
		}

		public int getNumStepsToEscalate() {
			return world_.getMediator().getNumStepsToEscalate();
		}

		public void setNumStepsToEscalate(int val) {
			world_.getMediator().setNumStepsToEscalate(val);
		}

		public int getEscalationDistance() {
			return world_.getMediator().getEscalationDistance();
		}

		public void setEscalationDistance(int val) {
			world_.getMediator().setEscalationDistance(val);
		}

		public double getDamageRatio() {
			return world_.getMediator().getDamageRatio();
		}

		public void setDamageRatio(double val) {
			world_.getMediator().setDamageRatio(val);
		}

	}

	public static class WeatherProperties
	{

		World world_;

		private WeatherProperties(World world) {
			world_ = world;
		}
	}

	public static class ObservationProperties
	{
		World world_;

		ObservationProperties(World world) {
			world_ = world;
		}

		public int getGrazableParcels() {
			return world_.getLand().getLandGrid().elements().size();
		}

		public double getFarmsPerGrazableParcel() {
			return (double) world_.getPopulation().getFarmingGrid().size() / 
					world_.getLand().getLandGrid().elements().size();
		}

		public double getCurrentFractionOpenParcels() {
			return (world_.getLand().getLandGrid().elements().size() - world_.getPopulation().getFarmingGrid().size()) / 
					world_.getLand().getLandGrid().elements().size();
		}

		/** @return the current number of herding activities in the simulation */
		public int getCurrentNumHerds() {
			return world_.getPopulation().getCurrentNumHerds();
		}

		/**
		 * @return the current number of herding activities divided by open
		 *         parcels
		 */
		public double getHerdsPerAvailableOpenParcel() {
			// open parcels
			double divby = world_.getLand().getLandGrid().elements().size() - world_.getPopulation().getFarmingGrid().size();
			double ans = 0.0;
			if (divby > 0) ans = (double) world_.getPopulation().getCurrentNumHerds() / divby;
			return ans;
		}

		public double getCurrentFractionOpenHectares() {
			return (100.0 * world_.getLand().getLandGrid().elements().size() - world_.getPopulation().getCurrentSizeOfFarmsInHectares()) / (100.0 * world_.getLand().getLandGrid().elements().size());
		}

		public int getCurrentNumberHouseholds() {
			return world_.getPopulation().getCurrentNumHouseholds();
		}
                
                public int getNumberNonDisplacedHouseholds() {
                    int n = 0;
                    Bag households = world_.getPopulation().getHouseholdsGrid().getAllObjects();
                    for (int i = 0; i < households.numObjs; i++)
                        if (!((Household)households.objs[i]).isDisplaced())
                            n++;
                    return n;
                }

		public int getCurrentPopulationFarmers() {
			return world_.getPopulation().getCurrentPopulationFarmers();
		}

		public int getCurrentPopulationHerders() {
			return world_.getPopulation().getCurrentPopulationHerders();
		}

		public int getCurrentPopulationLaborers() {
			return world_.getPopulation().getCurrentPopulationLaborers();
		}

		public int getCurrentPopulationDisplaced() {
			return world_.getPopulation().getCurrentPopulationDisplaced();
		}

		public int getCurrentPopulationNonDisplaced() {
			return world_.getPopulation().getNonDisplacedPopulation();
		}

		public double getCurrentPopulationChangeRate() {
			return world_.getPopulation().getCurrentPopulationChangeRate();
		}

                
//		public int getNumConflicts() {
//			return world_.getMediator().getNumConflicts();
//		}
//
//		public int getHFConflicts() {
//			return world_.getMediator().getHFconflicts();
//		}
//
//		public int getHHConflicts() {
//			return world_.getMediator().getHHconflicts();
//		}
//
//		public int getHHnonConflicts() {
//			return world_.getMediator().getHHnonconflicts();
//		}
                
                public int[] getFarmerCounts() {
                    int n = getNumberNonDisplacedHouseholds();
                    int[] counts = new int[n];
                    Bag households = world_.getPopulation().getHouseholdsGrid().getAllObjects();
                   
                    int index = 0;
                    for (int i = 0; i < households.numObjs; i++)
                        if (!((Household)households.objs[i]).isDisplaced())
                            counts[index++] = ((Household)households.objs[i]).getFarmingPopulation();
                    
                    return counts;
                }
                
                public int[] getHerderCounts() {
                    int n = getNumberNonDisplacedHouseholds();
                    int[] counts = new int[n];
                    Bag households = world_.getPopulation().getHouseholdsGrid().getAllObjects();
                   
                    int index = 0;
                    for (int i = 0; i < households.numObjs; i++)
                        if (!((Household)households.objs[i]).isDisplaced())
                            counts[index++] = ((Household)households.objs[i]).getHerdingPopulation();
                    
                    return counts;
                }
                
                public int[] getLaborerCounts() {
                    int n = getNumberNonDisplacedHouseholds();
                    int[] counts = new int[n];
                    Bag households = world_.getPopulation().getHouseholdsGrid().getAllObjects();
                   
                    int index = 0;
                    for (int i = 0; i < households.numObjs; i++)
                        if (!((Household)households.objs[i]).isDisplaced())
                            counts[index++] = ((Household)households.objs[i]).getLaboringPopulation();
                    
                    return counts;
                }
                
                private final double sqrt3 = Math.sqrt(3);
                public Double2D[] getActivitySimplex() {
                    int n = getNumberNonDisplacedHouseholds();
                    Double2D[] points = new Double2D[n];
                    Bag households = world_.getPopulation().getHouseholdsGrid().getAllObjects();
                    
                    int index = 0;
                    for (int i = 0; i < households.numObjs; i++)
                        if (!((Household)households.objs[i]).isDisplaced()) {
                            Household h = (Household)households.objs[index];
                            double total = (double)h.getPopulation();
                            double a = h.getFarmingPopulation() / total;
                            double b = h.getHerdingPopulation() / total;
                            double c = h.getLaboringPopulation() / total;
                            
                            double x = 0.5 * (2*b + c) / (a + b + c);
                            double y = sqrt3 * 0.5 * c / (a + b + c);
                            points[index++] = new Double2D(x,y);
                        }
                    
                    
                    return points;
                }
                
                private final Double2D[] simplexPad = new Double2D[] { new Double2D(-0.1, -0.1), new Double2D(0.5, 1.1), new Double2D(1.1, -0.1) };
                
                public Double2D[] getActivitySimplexPad() {
                    return simplexPad;
                }
                    

	}

	public class TimeSeriesScales
	{

		private double getScale(YScalableXYSeries series) {
			if (series == null) {
				return Double.NaN;
			}
			return series.getScale();
		}

		private void setScale(YScalableXYSeries series, double newScale) {
			if (series == null) {
				return;
			}
			series.setScale(newScale);
		}

		public double getWeatherTimeSeriesScale() {
			return getScale(weatherSeries);
		}

		public void setWeatherTimeSeriesScale(double newScale) {
			setScale(weatherSeries, newScale);
		}

		public double getFarmerTimeSeriesScale() {
			return getScale(farmerSeries);
		}

		public void setFarmerTimeSeriesScale(double newScale) {
			setScale(farmerSeries, newScale);
		}

		public double getConflictTimeSeriesScale() {
			return getScale(conflictSeries);
		}

		public void setConflictTimeSeriesScale(double newScale) {
			setScale(conflictSeries, newScale);
		}

		public double getHFTimeSeriesScale() {
			return getScale(herdingFarmerConflictSeries);
		}

		public void setHFTimeSeriesScale(double newScale) {
			setScale(herdingFarmerConflictSeries, newScale);
		}

		public double getHHTimeSeriesScale() {
			return getScale(herderHerderConflictSeries);
		}

		public void setHHTimeSeriesScale(double newScale) {
			setScale(herderHerderConflictSeries, newScale);
		}

		public double getHNTimeSeriesScale() {
			return getScale(herderCooperationSeries);
		}

		public void setHNTimeSeriesScale(double newScale) {
			setScale(herderCooperationSeries, newScale);
		}
    }

    @Override
    public void quit()
    {
        super.quit();        
        
        for (JFrame f : frames)
        	if (f != null) {
        		f.dispose();
        		f = null;
        	}

        for (Display2D d : displays) 
        	d = null;
    }

}
