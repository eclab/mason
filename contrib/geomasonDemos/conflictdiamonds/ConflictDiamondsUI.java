package conflictdiamonds;

import java.awt.*;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import sim.display.*;
import sim.engine.*;
import sim.portrayal.*;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.*;
import sim.portrayal.inspector.TabbedInspector;
import sim.portrayal.simple.*;
import sim.util.gui.ColorMap;

/**
 * Conflict Diamonds UI
 * Used to build graphical user interface and display model, charts, and legend
 * 
 * @author bpint
 */
public class ConflictDiamondsUI extends GUIState {
	
    ConflictDiamonds conflictDiamonds;

    public Display2D display;
    public JFrame displayFrame;

    // portrayal data
    ObjectGridPortrayal2D landPortrayal = new ObjectGridPortrayal2D();
    SparseGridPortrayal2D populationPortrayal = new SparseGridPortrayal2D();
    ObjectGridPortrayal2D diamondDistancePortrayal = new ObjectGridPortrayal2D();
    ObjectGridPortrayal2D cityDistancePortrayal = new ObjectGridPortrayal2D();
    GeomVectorFieldPortrayal boundariesPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal diamondsPortrayal = new GeomVectorFieldPortrayal();

    // This must be included to have model tab, which allows mid-simulation
    // modification of the coefficients
    public Object getSimulationInspectedObject() { return state; }  // non-volatile
    
    // chart information
    sim.util.media.chart.TimeSeriesChartGenerator diamondChart;
    public XYSeries rebelSeries = new XYSeries("Number of Rebels");
    public XYSeries residentSeries = new XYSeries("Number of Residents");

    sim.util.media.chart.TimeSeriesChartGenerator employChart;
    public XYSeries informalSeries = new XYSeries("Number of Diamond Miners");
    public XYSeries formalSeries = new XYSeries("Number of Other Employees");

    protected ConflictDiamondsUI(SimState state) {
        super(state);
        conflictDiamonds = (ConflictDiamonds)state;
    }

    public static void main(String[] args) {
        ConflictDiamondsUI conflictGUI = new ConflictDiamondsUI(new ConflictDiamonds(System.currentTimeMillis(), args));
        //ConflictDiamondsUI conflictGUI = new ConflictDiamondsUI(args);
        
        Console c = new Console(conflictGUI);
        c.setVisible(true);
    }

    public static String getName() {
        return "ConflictDiamonds";
    }

    public void start() {
        super.start();			

        Steppable chartUpdater = new Steppable() {
            public void step(SimState state) {
                int numRebel = conflictDiamonds.allRebels.size();
                int numResident = conflictDiamonds.allResidents.size();
                int numInformal = conflictDiamonds.diamondMinerEmployer.getEmployees().size();
                int numFormal = conflictDiamonds.otherEmployer.getEmployees().size();

                rebelSeries.add((double) (state.schedule.time()), numRebel);
                residentSeries.add((double) (state.schedule.time()), numResident);
                informalSeries.add((double) (state.schedule.time()), numInformal);
                formalSeries.add((double) (state.schedule.time()), numFormal);
            }
        };
		
        conflictDiamonds.schedule.scheduleRepeating(chartUpdater);

        // set up the portrayals
        setupPortrayals();
    }
    
    public Inspector getInspector() {
        super.getInspector();
          TabbedInspector i = new TabbedInspector();


        i.addInspector(new SimpleInspector(
                ((ConflictDiamonds) state).params.global, this), "Paramters");
        return i;
    }

    public void setupPortrayals() {
        landPortrayal.setField(conflictDiamonds.allLand);	
        landPortrayal.setPortrayalForAll(new LandscapePortrayal());
        //display.attach(landPortrayal, "land");

        populationPortrayal.setField(conflictDiamonds.allPopulation);
        //setupPopulationPortrayal();
        populationPortrayal.setPortrayalForAll(new PopulationPortrayal());
        display.attach(populationPortrayal, "population");

        diamondDistancePortrayal.setField(conflictDiamonds.allLand);
        diamondDistancePortrayal.setPortrayalForAll( new DiamondDistancePortrayal() );
        //display.attach(diamondDistancePortrayal, "diamonds");

        cityDistancePortrayal.setField(conflictDiamonds.allLand);
        cityDistancePortrayal.setPortrayalForAll( new CityDistancePortrayal() );
        //display.attach(cityDistancePortrayal, "city");

        boundariesPortrayal.setField(conflictDiamonds.allBoundaries);
        boundariesPortrayal.setPortrayalForAll(new GeomPortrayal(Color.black, false));
        display.attach(boundariesPortrayal, "boundaries");
	    
        diamondsPortrayal.setField(conflictDiamonds.allDiamonds);
        diamondsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.cyan, false));
        //display.attach(diamondsPortrayal, "diamond");

        display.reset();
        display.setBackdrop(Color.white);
        // redraw the display
        display.repaint();
    }
    
    //portray the landscape
    class LandscapePortrayal extends RectanglePortrayal2D {

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {			
            if(object == null) {
                System.out.println("null");
                return;
            }		

            Parcel p = (Parcel) object;
            if ( p.getRegion() != null ) {					
                if( p.getRegion().getRegionID() == 1 ) //Kailahun
                    graphics.setColor( Color.orange );
                else if( p.getRegion().getRegionID() == 2) //Kenema
                    graphics.setColor( Color.yellow );
                else if( p.getRegion().getRegionID() == 3) //Kono
                    graphics.setColor( Color.blue );
                else if( p.getRegion().getRegionID() == 4) //Bombali
                    graphics.setColor( Color.green);
                else if ( p.getRegion().getRegionID() == 5) //Kambia
                    graphics.setColor( Color.WHITE );
                else if ( p.getRegion().getRegionID() == 6) //Koinadugu
                    graphics.setColor( Color.black );
                else if ( p.getRegion().getRegionID() == 7) //Port Loko
                    graphics.setColor( Color.gray );
                else if ( p.getRegion().getRegionID() == 8) //Tonkolini
                    graphics.setColor( Color.pink );
                else if ( p.getRegion().getRegionID() == 9) //Bo
                    graphics.setColor( Color.cyan );
                else if ( p.getRegion().getRegionID() == 10) //Bonthe
                    graphics.setColor( Color.MAGENTA );
                else if ( p.getRegion().getRegionID() == 11) //Moyamba
                    graphics.setColor( Color.lightGray );
                else if ( p.getRegion().getRegionID() == 12) //Pujehun
                    graphics.setColor( Color.DARK_GRAY );
                else if ( p.getRegion().getRegionID() == 13) //Western Area Rural
                    graphics.setColor( Color.blue );
                else if (p.getRegion().getRegionID() == 14) //Western Area Urban
                    graphics.setColor(Color.red);
                
                else return; // landuse is not provided for this parcel

                paint = graphics.getColor();
                super.draw( p, graphics, info);
				
            }
        }
    }
    
    //portray population of agents
    class PopulationPortrayal extends OvalPortrayal2D {

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
                Person p = (Person) object;

                if ( object != null && object instanceof Rebel && !p.isInitialRebel()) {
                    
                    paint = Color.red;		
                    scale =1.5;
                    super.draw(object, graphics, info);
                    
                }
                
                else if ( object != null && p.getDiamondMiner() != null) {
                    paint = Color.blue;
                    scale = 1.5;
                    super.draw(object, graphics, info);
                }
                
                else {
                    //paint = Color.green;
                    //super.draw(object, graphics, info);
                }
        }
    }

    // colormap for distance from mine, with alpha value 150 giving semi-transparency
    public static ColorMap diamondDistanceColor = new sim.util.gui.SimpleColorMap(
                    0, .5, new Color(0, 0, 255, 150), new Color(0, 0, 0, 150));

        class DiamondDistancePortrayal extends RectanglePortrayal2D {
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {			
                if(object == null) {
                    System.out.println("null");
                    return;
                }			

                Parcel p = (Parcel) object;
                Color color = diamondDistanceColor.getColor( 
                        p.getDiamondMineDistance() );
                graphics.setColor( color );
                paint = color;
                super.draw( p, graphics, info);
            }	
        }
    
    // colormap for distance from city, with alpha value 150 giving semi-transparency
    public static ColorMap cityDistanceColor = new sim.util.gui.SimpleColorMap(
                    0, .5, new Color(255, 0, 0, 150), new Color(0, 0, 0, 150));

	class CityDistancePortrayal extends RectanglePortrayal2D {
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {			
                if(object == null) {
                    System.out.println("null");
                    return;
                }			

                Parcel p = (Parcel) object;
                Color color = cityDistanceColor.getColor( 
                    p.getRemoteness() );
                graphics.setColor( color );
                paint = color;
                super.draw( p, graphics, info);				
            }
	}
        
    /**
     * Called when first beginning a ConflictsDiamondUI. Sets up the display windows,
     * the JFrames, and the chart structure.
     */
    public void init(Controller c) {
        super.init(c);
        
        JFreeChart barchart = ChartFactory.createBarChart("Agent's Activity", "Activity", "Percentage", ((ConflictDiamonds) this.state).dataset, PlotOrientation.VERTICAL, false, false, false);
        barchart.setBackgroundPaint(Color.WHITE);
        barchart.getTitle().setPaint(Color.BLACK);
   
        CategoryPlot p = barchart.getCategoryPlot();
        p.setBackgroundPaint(Color.WHITE);
        p.setRangeGridlinePaint(Color.red);
        
        // set the range axis to display integers only...  
        NumberAxis rangeAxis = (NumberAxis) p.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        int max = 100;
        rangeAxis.setRange(0, max);
        
        ChartFrame frame = new ChartFrame("Activity Chart", barchart);
        frame.setVisible(true);
        frame.setSize(400, 350);
                
          //portray legend
        Dimension dl = new Dimension(300,425);
        Legend legend = new Legend();
        legend.setSize(dl);

        JFrame legendframe = new JFrame();
        legendframe.setVisible(true);
        legendframe.setPreferredSize(dl);
        legendframe.setSize(300, 500);

        legendframe.setBackground(Color.white);
        legendframe.setTitle("Legend");
        legendframe.getContentPane().add(legend);   
        legendframe.pack();
        c.registerFrame(legendframe);
    
        frame.pack();
        c.registerFrame(frame);
    
        display = new Display2D(800, 800, this, 1);

        //display.attach(landPortrayal, "Land");
        display.attach(populationPortrayal, "Population");
        //display.attach(diamondDistancePortrayal, "Distance to Diamond Mine");
        //display.attach(cityDistancePortrayal, "Distance to Cities");
        display.attach(boundariesPortrayal, "Boundaries");
        //display.attach(diamondsPortrayal, "Diamonds");

        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);

        diamondChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        employChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        // set up the chart info
        //numRebel = new XYSeries("Number of Rebels");
        //diamondChart.removeAllSeries();
        //diamondChart.addSeries(numRebel, null);
		
        diamondChart.setTitle("Number of Rebels");
        diamondChart.setRangeAxisLabel("Number of Rebels");
        diamondChart.setDomainAxisLabel("Months");
        diamondChart.addSeries(rebelSeries  , null);
        diamondChart.addSeries(residentSeries  , null);
        //JFrame frame = diamondChart.createFrame(this);
        frame.pack();
        c.registerFrame(frame);
                      
        employChart.setTitle("Number in Formal/Informal Market");
        employChart.setRangeAxisLabel("Number of Agents");
        employChart.setDomainAxisLabel("Months");
        employChart.addSeries(formalSeries  , null);
        employChart.addSeries(informalSeries  , null);
        JFrame frame2 = employChart.createFrame(this);
        frame2.pack();
        c.registerFrame(frame2);						
    }

    public void quit() {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
    }

}
