import org.jfree.data.xy.XYSeries;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.util.media.chart.TimeSeriesChartGenerator;

import javax.swing.*;
import java.awt.*;

/** <b>AcequiaWorldWithUI</b> the visualization of AcequiaWorld.
 * 
 * @author Sarah Wise and Andrew Crooks
 */
public class AcequiaWorldWithUI extends GUIState {

	AcequiaWorld aw;
	
	public Display2D display;
	public JFrame displayFrame;

	ObjectGridPortrayal2D acequias = new ObjectGridPortrayal2D();
	ObjectGridPortrayal2D acequiaTracts = new ObjectGridPortrayal2D();
	ObjectGridPortrayal2D counties = new ObjectGridPortrayal2D();
	ObjectGridPortrayal2D elevation = new ObjectGridPortrayal2D();
	ObjectGridPortrayal2D hydrologicalFeatures = new ObjectGridPortrayal2D();
	ObjectGridPortrayal2D landUse = new ObjectGridPortrayal2D();
	ObjectGridPortrayal2D roads = new ObjectGridPortrayal2D();
		GridNetworkPortrayal waterNetwork = new GridNetworkPortrayal();
	ObjectGridPortrayal2D hydrationPortrayal = new ObjectGridPortrayal2D();
	
	TimeSeriesChartGenerator urbanVSAgChart;
	XYSeries numUrban;
	XYSeries numAg;

	TimeSeriesChartGenerator parciantesChart;
	XYSeries numParciantes;
	
	/** Constructor */
	protected AcequiaWorldWithUI(SimState state) {
		super(state);
		aw = (AcequiaWorld) state;
	}

	// This must be included to have model tab, which allows mid-simulation
	// modification of the coefficients
    public Object getSimulationInspectedObject() { return state; }  // non-volatile

	/**
	 * Main function
	 * @param args
	 */
	public static void main(String[] args) {
		AcequiaWorldWithUI simple = new AcequiaWorldWithUI(new AcequiaWorld(System
				.currentTimeMillis()));
		Console c = new Console(simple);
		c.setVisible(true);
	}
	
	/**
	 * @return name of the simulation
	 */
	public static String getName() { return "AcequiaWorld"; }

	/**
	 * Called when starting a new run of the simulation. Sets up the portrayals
	 * and chart data.
	 */
	public void start() {
		super.start();

		// set up the chart info
				numUrban = new XYSeries("Number of Urban Tiles");
				numAg = new XYSeries("Number of Agricultural Tiles");
				urbanVSAgChart.removeAllSeries();
				urbanVSAgChart.addSeries(numUrban, null);
				urbanVSAgChart.addSeries(numAg, null);
				
				numParciantes = new XYSeries("Number of parciantes");
				parciantesChart.removeAllSeries();
				parciantesChart.addSeries( numParciantes, null);
				
				// schedule the chart to take data
				state.schedule.scheduleRepeating( new Steppable(){
					public void step(SimState state){
						AcequiaWorld aw = (AcequiaWorld) state;
						numUrban.add( state.schedule.time(), aw.numUrban / 
								(double)( aw.numUrban + aw.numAg));
						numAg.add( state.schedule.time(), aw.numAg / 
								(double) ( aw.numUrban + aw.numAg));
						numParciantes.add( state.schedule.time(), aw.parciantes.size() );
					}
				});
		
		acequias.setField( aw.tiles );
		acequias.setPortrayalForAll( new AcequiaPortrayal() );
		
		acequiaTracts.setField( aw.tiles );
		acequiaTracts.setPortrayalForAll( new AcequiaTractPortrayal() );
		
		counties.setField( aw.tiles );
		counties.setPortrayalForAll( new CountiesPortrayal() );
		
		elevation.setField( aw.tiles );
		elevation.setPortrayalForAll( new ElevationPortrayal() );

		hydrologicalFeatures.setField( aw.tiles );
		hydrologicalFeatures.setPortrayalForAll( new HydrologicalNetworkPortrayal() );
		
		landUse.setField( aw.tiles );
		landUse.setPortrayalForAll( new LandUsePortrayal() );
		
		roads.setField( aw.tiles );
		roads.setPortrayalForAll( new RoadsPortrayal() );
		
		hydrationPortrayal.setField( aw.tiles );
		hydrationPortrayal.setPortrayalForAll( new HydrationPortrayal() );
		
		waterNetwork.setField( aw.waterflow, aw.tiles.getWidth(), aw.tiles.getHeight());
		waterNetwork.setPortrayalForAll( new LinkPortrayal() );
		
		// reschedule the displayer
		display.reset();
		display.setBackdrop( Color.white );

		// redraw the display
		display.repaint();
	}

	/**
	 * Called when first beginning a AcequiaWorldWithUI. Sets up the display window,
	 * the JFrames, and the chart structure.
	 */
	public void init(Controller c) {
		super.init(c);

		// make the displayer
		display = new Display2D(600, 600, this, 1);
		// turn off clipping
		display.setClipping(false);

		displayFrame = display.createFrame();
		displayFrame.setTitle("AcequiaWorld Display");
		c.registerFrame(displayFrame); // register the frame so it appears in
										// the "Display" list
		displayFrame.setVisible(true);

		display.attach( elevation, "Elevation", false);
		display.attach( landUse, "Land Use", false);
		display.attach( counties, "Counties", false);
		display.attach( acequiaTracts, "Acequia Tracts", false);
		display.attach( hydrologicalFeatures, "Hydrological Features", false);
		display.attach( roads, "Roads", false);
		display.attach( acequias, "Acequias", false);
		display.attach( hydrationPortrayal, "Hydration", false);
		display.attach( waterNetwork, "Functioning Water Network", false);
		
		urbanVSAgChart = new TimeSeriesChartGenerator();
		urbanVSAgChart.setTitle("Percent of Urban vs Agricultural Tiles in Simulation");
		urbanVSAgChart.setRangeAxisLabel("Percent of Tiles");
		urbanVSAgChart.setDomainAxisLabel("Time");
		JFrame chartFrame = urbanVSAgChart.createFrame(this);
		chartFrame.setVisible(false);
		chartFrame.pack();
		c.registerFrame(chartFrame);

		parciantesChart = new TimeSeriesChartGenerator();
		parciantesChart.setTitle("Number of Parciantes in Simulation");
		parciantesChart.setRangeAxisLabel("Number of Parciantes");
		parciantesChart.setDomainAxisLabel("Time");
		JFrame chartFrame2 = parciantesChart.createFrame(this);
		chartFrame2.setVisible(false);
		chartFrame2.pack();
		c.registerFrame(chartFrame2);

	}
	
	/** called when quitting a simulation. Does appropriate garbage collection. */
	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null; // let gc
		display = null; // let gc
	}

}