
package refugee;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.dial.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.field.geo.GeomVectorField;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.FieldPortrayal2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.simple.CircledPortrayal2D;
import sim.portrayal.simple.HexagonalPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.geo.MasonGeometry;
import sim.util.media.chart.ChartGenerator;
import sim.util.media.chart.TimeSeriesChartGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

public class MigrationWithUI extends GUIState {
	Display2D display; // displaying the model
	JFrame displayFrame; // frame containing all the displays
	FieldPortrayal2D cityPortrayal = new SparseGridPortrayal2D();
	GeomVectorFieldPortrayal regionPortrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal countryPortrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal countryBndPortrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal roadPortrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal roadLinkPortrayal = new GeomVectorFieldPortrayal();
	ContinuousPortrayal2D refugeePortrayal = new ContinuousPortrayal2D();

	public MigrationWithUI(Migration sim) {
		super(sim);
	}

	@Override
	public void init(Controller c) {
		super.init(c);

		// set dimen and position of controller
		((Console) c).setSize(350, 80);
		((Console) c).setLocation(0, 520);

		display = new Display2D(600, 520, this); // creates the display
		// display.setRefresRate(32);
		display.setScale(1.5);

		display.attach(regionPortrayal, "Regions");
		display.attach(countryPortrayal, "Counties (area)");
		display.attach(countryBndPortrayal, "Countries (boundary)");
		//display.attach(roadPortrayal, "Roads");
		display.attach(cityPortrayal, "Cities");
		display.attach(roadLinkPortrayal, "Routes");
		display.attach(refugeePortrayal, "Refugees");

		displayFrame = display.createFrame();
		c.registerFrame(displayFrame);
		displayFrame.setVisible(true);
		displayFrame.setSize(800, 520);
		display.setBackdrop(new Color(179,236,255));
		
		//deaths chart
	    Dimension dm = new Dimension(360,120);
	    Dimension dmn = new Dimension(120,100);
	    
		TimeSeriesChartGenerator healthStatus;
        healthStatus = new TimeSeriesChartGenerator();
        JFrame frameSeries = healthStatus.createFrame();
        healthStatus.setSize(dm);
        
        healthStatus.setTitle("Health Status");
        healthStatus.setRangeAxisLabel("Percentage of Agents");
        healthStatus.setDomainAxisLabel("Hours");
        healthStatus.setMaximumSize(dm);
        healthStatus.setMinimumSize(dmn);
//        chartSeriesCholera.setMinimumChartDrawSize(400, 300); // makes it scale at small sizes
//        chartSeriesCholera.setPreferredChartSize(400, 300); // lets it be small

        healthStatus.addSeries(((Migration) this.state).totalDeadSeries, null);
        healthStatus.setProportion(2.2);
       // JFrame frameSeries = healthStatus.createFrame(this);
        frameSeries.setSize(480,260);
        
        frameSeries.setLocation(800, 0);
      //  frameSeries.pack();
        c.registerFrame(frameSeries);
        frameSeries.setVisible(true);
     
      //Financial chart
		TimeSeriesChartGenerator finStatus;
		finStatus = new TimeSeriesChartGenerator();
		finStatus.createFrame();
		finStatus.setSize(dm);
		finStatus.setTitle("Average Financial Status");
		finStatus.setRangeAxisLabel("Money Left (USD)");
		finStatus.setDomainAxisLabel("Hours");
		finStatus.setMaximumSize(dm);
		finStatus.setMinimumSize(dmn);
		finStatus.addSeries(((Migration) this.state).finSeries, null);
		finStatus.setProportion(2.2);
     
     JFrame frameSeries2 = finStatus.createFrame(this);
     
     frameSeries2.setSize(480,260);
     frameSeries2.setLocation(800, 260);
     //frameSeries2.pack();
     c.registerFrame(frameSeries2);
     frameSeries2.setVisible(true);

     StandardDialFrame dialFrame = new StandardDialFrame();
     DialBackground ddb = new DialBackground(Color.white);
     dialFrame.setBackgroundPaint(Color.lightGray);
     dialFrame.setForegroundPaint(Color.darkGray);

     DialPlot plot = new DialPlot();
     plot.setView(0.0, 0.0, 1.0, 1.0);
     plot.setBackground(ddb);
     plot.setDialFrame(dialFrame);

     plot.setDataset(0, ((Migration) this.state).hourDialer);
     plot.setDataset(1,((Migration) this.state).dayDialer);


     DialTextAnnotation annotation1 = new DialTextAnnotation("Hour");
     annotation1.setFont(new Font("Dialog", Font.BOLD, 14));
     annotation1.setRadius(0.1);
     plot.addLayer(annotation1);


//     DialValueIndicator dvi = new DialValueIndicator(0);
//     dvi.setFont(new Font("Dialog", Font.PLAIN, 10));
//     dvi.setOutlinePaint(Color.black);
//     plot.addLayer(dvi);
//

     DialValueIndicator dvi2 = new DialValueIndicator(1);
     dvi2.setFont(new Font("Dialog", Font.PLAIN, 22));
     dvi2.setOutlinePaint(Color.red);
     dvi2.setRadius(0.3);
     plot.addLayer(dvi2);

     DialTextAnnotation annotation2 = new DialTextAnnotation("Day");
     annotation2.setFont(new Font("Dialog", Font.BOLD, 18));
     annotation2.setRadius(0.4);
     plot.addLayer(annotation2);

     StandardDialScale scale = new StandardDialScale(0.0, 23.99, 90, -360, 1.0,59);
     scale.setTickRadius(0.9);
     scale.setTickLabelOffset(0.15);
     scale.setTickLabelFont(new Font("Dialog", Font.PLAIN, 12));
     plot.addScale(0, scale);
     scale.setMajorTickPaint(Color.black);
     scale.setMinorTickPaint(Color.lightGray);




//     StandardDialScale scale2 = new StandardDialScale(1, 7, -150, -240, 1,1);
//     scale2.setTickRadius(0.50);
//     scale2.setTickLabelOffset(0.15);
//     scale2.setTickLabelPaint(Color.RED);
//     scale2.setTickLabelFont(new Font("Dialog", Font.PLAIN, 12));
//     plot.addScale(1, scale2);
//
//     DialPointer needle2 = new DialPointer.Pin(1);
//     plot.addPointer(needle2);
//     needle2.setRadius(0.40);
     // plot.mapDatasetToScale(1, 1);

     DialPointer needle = new DialPointer.Pointer(0);
     plot.addPointer(needle);


     DialCap cap = new DialCap();
     cap.setRadius(0.10);
     plot.setCap(cap);

     JFreeChart chart1 = new JFreeChart(plot);
     ChartFrame timeframe = new ChartFrame("Time Chart", chart1);
     timeframe.setVisible(false);
     timeframe.setSize(200, 100);
     //timeframe.pack();
     c.registerFrame(timeframe);

     

		// ((Console) c).pressPlay();
	}

	@Override
	public void start() {
		super.start();

		setupFixedPortrayals();
		setupMovingPortrayals();
	}

	public void setupFixedPortrayals() {

		// Adding the city portrayal
		cityPortrayal.setField(((Migration) state).cityGrid);
		//cityPortrayal.setPortrayalForAll(new OvalPortrayal2D(new Color(255, 154, 146), 5.0, true));
		cityPortrayal.setPortrayalForAll(new OvalPortrayal2D(new Color(255, 137, 95), 5.0, true));

		// Adding the roadlinks portrayal
		roadLinkPortrayal.setField(((Migration) state).roadLinks);
		roadLinkPortrayal.setPortrayalForAll(new GeomPortrayal(new Color(255,77,166), 1, true));
		
		// Adding the road portrayal
		roadPortrayal.setField(((Migration) state).roads);
		roadPortrayal.setPortrayalForAll(new GeomPortrayal(new Color(255, 199, 199), 1, false));
		
		// Adding the region portrayal
		regionPortrayal.setField(((Migration) state).regions);
		regionPortrayal.setPortrayalForAll(new GeomPortrayal(new Color(128, 128, 128), 2, false));
		
		// Adding the country portrayal
		countryPortrayal.setField(((Migration) state).countries);
		countryPortrayal.setPortrayalForAll(new GeomPortrayal(new Color(226,198,141), 1, true));
		countryBndPortrayal.setField(((Migration) state).countries);
		countryBndPortrayal.setPortrayalForAll(new GeomPortrayal(new Color(64,64,64), 1, false));		

	}

	// display refresh each step
	public void setupMovingPortrayals() {
		
//		cityPortrayal.setField(((Migration) state).cityGrid);
//		cityPortrayal.setPortrayalForAll(new OvalPortrayal2D(){
//			@Override
//			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
//			City city = (City) object;
//			paint = new Color(255, 154, 146);
//			//System.out.println(city.getName() + " Population: "  + city.getRefugeePopulation());
//			double scale = city.getScale()*200;
//			System.out.println(scale);
//			super.scale = scale;
//			super.filled = true;
//			super.draw(object, graphics, info);
//			}
//		});
		cityPortrayal.setPortrayalForAll(new OvalPortrayal2D() {

			private static final long serialVersionUID = 546102092597315413L;

				@Override
	            public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	            {
	                City city = (City)object;

	                Rectangle2D.Double draw = info.draw;
	                int refugee_pop = city.getRefugeePopulation();
	              //  System.out.println("refugee_pop = " + refugee_pop);
	                paint = new Color(122, 56, 255);
	                Double scale = 1.0;
	                if(refugee_pop == 0) {
	                	scale = 5.0;
	              //  	paint = new Color(51, 102, 255);
	                }
	                else if(refugee_pop > 0 && refugee_pop <= Parameters.TOTAL_POP * 0.3) {
	                	scale = 15.0;
	                	//paint = new Color(112, 77, 255);
	              //  	paint = new Color(163, 177, 255);
	                }
	                else if(refugee_pop > Parameters.TOTAL_POP * 0.3 && refugee_pop <= Parameters.TOTAL_POP*0.6){
	                	scale = 25.0;
	                	//paint = new Color(133, 102, 255);
	             //   	paint = new Color(177, 138, 255);
	                }
	                else if(refugee_pop > Parameters.TOTAL_POP*0.6){
	                	scale = 40.0;
	                	//paint = new Color(235, 138, 255);
	              //  	paint = new Color(177, 138, 255);
	                }
	                
	                //paint = new Color(0, 128, 255);
	                //paint = new Color(255, 154, 146);
	                //paint = new Color(255, 137, 95);
	                final double width = draw.width*scale + offset;
	                final double height = draw.height*scale + offset;

	                graphics.setPaint(paint);
	                final int x = (int)(draw.x - width / 2.0);
	                final int y = (int)(draw.y - height / 2.0);
	                int w = (int)(width);
	                int h = (int)(height);
	                        
	                // draw centered on the origin
	                if (filled)
	                    graphics.fillOval(x,y,w,h);
	                else
	                    graphics.drawOval(x,y,w,h);

	            }
	        });
	  
		
		refugeePortrayal.setField(((Migration) this.state).world);
		refugeePortrayal.setPortrayalForAll(new OvalPortrayal2D() {
			@Override
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {

				Refugee refugee = (Refugee) object;
				if (refugee.getHealthStatus() == Constants.DEAD)
					paint = Color.RED;
				// System.out.println(refugee);
				else
					paint = Color.GREEN;
				//super.draw(object, graphics, info);
				super.filled = true;
				super.scale = 5;
				super.draw(object, graphics, info);
			}
		});

		display.reset();
		display.setBackdrop(new Color(179,236,255));
		display.repaint();
	}

	@Override
	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;

	}

	public static void main(String[] args) {
		MigrationWithUI ebUI = new MigrationWithUI(new Migration(System.currentTimeMillis()));
		Console c = new Console(ebUI);
		c.setVisible(true);
	}
}
