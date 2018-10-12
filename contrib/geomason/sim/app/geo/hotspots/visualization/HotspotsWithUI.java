package sim.app.geo.hotspots.visualization;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import ec.util.MersenneTwisterFast;
import sim.app.geo.hotspots.objects.Agent;
import sim.app.geo.hotspots.sim.Hotspots;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.FieldPortrayal2D;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.TimeSeriesChartGenerator;
import swise.disasters.Wildfire;
import swise.visualization.AttributePolyPortrayal;
import swise.visualization.FilledPolyPortrayal;
import swise.visualization.GeomNetworkFieldPortrayal;

/**
 * A visualization of the Hotspots simulation.
 * 
 * @author swise
 */
public class HotspotsWithUI extends GUIState {

	Hotspots sim;
	public Display2D display;
	public JFrame displayFrame;
	
	// Map visualization objects
	private GeomVectorFieldPortrayal map = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal roads = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal agents = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal fire_valid = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal moreRoads = new GeomVectorFieldPortrayal();
	private GeomNetworkFieldPortrayal network = new GeomNetworkFieldPortrayal();
	private GeomNetworkFieldPortrayal high_roads = new GeomNetworkFieldPortrayal();
	
	private FastValueGridPortrayal2D wildfire = new FastValueGridPortrayal2D();	
	private FastValueGridPortrayal2D heatmap = new FastValueGridPortrayal2D();	
		
	///////////////////////////////////////////////////////////////////////////
	/////////////////////////// BEGIN functions ///////////////////////////////
	///////////////////////////////////////////////////////////////////////////	
	
	/** Default constructor */
	public HotspotsWithUI(SimState state) {
		super(state);
		sim = (Hotspots) state;
	}

	/** Begins the simulation */
	public void start() {
		super.start();
		
		// set up portrayals
		setupPortrayals();
	}

	/** Loads the simulation from a point */
	public void load(SimState state) {
		super.load(state);
		
		// we now have new grids. Set up the portrayals to reflect that
		setupPortrayals();
	}

	/**
	 * Sets up the portrayals of objects within the map visualization. This is called by both start() and by load()
	 */
	public void setupPortrayals() {
		
		Hotspots world = (Hotspots) state;
		map.setField(world.baseLayer);
		map.setPortrayalForAll(new AttributePolyPortrayal(
				new SimpleColorMap(0,13000, new Color(100,80,30), new Color(240,220,200)),
				"DP0010001", new Color(0,0,0,0), true));
		map.setImmutableField(true);
		
		roads.setField(world.roadLayer);
		roads.setPortrayalForAll(new GeomPortrayal(Color.BLACK, false));
		
		moreRoads.setField(world.hi_roadLayer);
		moreRoads.setPortrayalForAll(new GeomPortrayal(new Color(255,200,200), false));
		
		
		agents.setField(world.agentsLayer);
		agents.setPortrayalForAll( new AttributePolyPortrayal(
						new SimpleColorMap(0,1, new Color(255,0,0,100), new Color(255,255,0,100)),
						"Evacuating", new Color(0,0,0,0), true, 50));
		
		network.setField( world.agentsLayer, world.agentSocialNetwork );
		network.setImmutableField(false);
		network.setPortrayalForAll(new GeomPortrayal(new Color(200,200,50), false));

		
		high_roads.setField( world.hi_roadLayer, world.hiNetwork );
		high_roads.setImmutableField(false);
		high_roads.setPortrayalForAll(new GeomPortrayal(new Color(200,255,200), false));

		// optional visualization of the real wildfire
		fire_valid.setField( world.fireLayer );
		fire_valid.setImmutableField(false);
		fire_valid.setPortrayalForAll(new GeomPortrayal(new Color(153, 52, 4, 100),//new Color(255,0,0), 
				true));

		wildfire.setField(world.wildfire.getGrid().getGrid()); 
		wildfire.setMap(new SimpleColorMap(new Color[] {new Color(0,0,0,0), new Color(0,0,0,0),
				Color.RED, Color.YELLOW}));
	
		heatmap.setField(world.heatmap.getGrid()); 
		heatmap.setMap(new SimpleColorMap(0, 10, Color.black, Color.red));
		
		// reset stuff
		// reschedule the displayer
		display.reset();
		display.setBackdrop(Color.black);

		// redraw the display
		display.repaint();
	}

	/** Initializes the simulation visualization */
	public void init(Controller c) {
		super.init(c);

		// the map visualization
		display = new Display2D((int)(1.5 * sim.grid_width), (int)(1.5 * sim.grid_height), this);

		display.attach(heatmap, "Heatmap", false);
		display.attach(map, "Landscape");
		// optional visualization of the real wildfire
//		display.attach(fire_valid, "True Wildfire");
		display.attach(wildfire, "Wildfire");
		display.attach(roads, "Roads");
		display.attach(high_roads, "highpath");
		display.attach(network, "Network", false);
		display.attach(agents, "Agents");
		
		
		// ---TIMESTAMP---
		display.attach(new FieldPortrayal2D()
	    {
			private static final long serialVersionUID = 1L;
			
			Font font = new Font("SansSerif", 0, 24);  // keep it around for efficiency
		    SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd HH:mm zzz");
		    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
		        {
		        String s = "";
		        if (state !=null) // if simulation has not begun or has finished, indicate this
		            s = state.schedule.getTimestamp("Before Simulation", "Simulation Finished");
		        graphics.setColor(Color.white);
		        if (state != null){
		        	// specify the timestep here
		        	Date startDate;
					try {
						startDate = ft.parse("2012-06-21 00:00 MST");
				        Date time = new Date((int)state.schedule.getTime() * 300000 + startDate.getTime());
				        s = ft.format(time);	
					} catch (ParseException e) {
						e.printStackTrace();
					}
		        }

		        graphics.drawString(s, (int)info.clip.x + 10, 
		                (int)(info.clip.y + 10 + font.getStringBounds(s,graphics.getFontRenderContext()).getHeight()));

		        }
		    }, "Time");
		
		displayFrame = display.createFrame();
		c.registerFrame(displayFrame); // register the frame so it appears in the "Display" list
		displayFrame.setVisible(true);
	}

	/** Quits the simulation and cleans up.*/
	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null; // let gc
		display = null; // let gc
	}

	/** Runs the simulation */
	public static void main(String[] args) {
		HotspotsWithUI gui =  null;
		
		try {
			Hotspots lb = new Hotspots(12345);//System.currentTimeMillis());
			gui = new HotspotsWithUI(lb);
		} catch (Exception ex){
			System.out.println(ex.getStackTrace());
		}
		
		Console console = new Console(gui);
		console.setVisible(true);
	}

	/** Returns the name of the simulation */
	public static String getName() { return "Hotspots"; }

	/** Allows for users to modify the simulation using the model tab */
	public Object getSimulationInspectedObject() { return state; }

}
