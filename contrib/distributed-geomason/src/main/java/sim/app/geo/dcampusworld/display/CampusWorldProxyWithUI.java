package sim.app.geo.dcampusworld.display;

import java.awt.Color;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.vividsolutions.jts.geom.Envelope;

import sim.app.geo.campusworld.BuildingLabelPortrayal;
import sim.app.geo.dcampusworld.DCampusWorld;
import sim.app.geo.dcampusworld.data.DCampusWorldData;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.simple.MovablePortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;

public class CampusWorldProxyWithUI extends GUIState {
	public Display2D display;
	public JFrame displayFrame;
	
	
	public GeomVectorField walkways = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
	public GeomVectorField roads = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
	public GeomVectorField buildings = new GeomVectorField(DCampusWorld.width, DCampusWorld.height);
	

	public Envelope MBR;
    GeomVectorFieldPortrayal walkwaysPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal buildingPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal();

	public GeomPlanarGraph network = new GeomPlanarGraph();
	public GeomVectorField junctions = new GeomVectorField(DCampusWorld.width, DCampusWorld.height); // nodes for intersections

	

	//ContinuousPortrayal2D agentPortrayal = new ContinuousPortrayal2D();

    GeomVectorFieldPortrayal agentPortrayal = new GeomVectorFieldPortrayal();

	
	public static void main(String[] args) { new CampusWorldProxyWithUI().createController(); }

	public CampusWorldProxyWithUI() { super(new CampusWorldProxy(System.currentTimeMillis())); 	loadStatic();
}

	public CampusWorldProxyWithUI(SimState state) { super(state); 	loadStatic();
 }

	public static String getName() { return "CampusWorld Proxy"; }

	// TODO What is this?
	public Object getSimulationInspectedObject() { return state; } // non-volatile

	// TODO?
	// public Controller createController() {//...}

	public void start() {
		super.start();
		setupPortrayals();
		// TODO: How to update display bounds
		// What happens when balancing changes the bounds?

//		try {
//			System.out.println("Hi");
//			bounds = ((CampusWorldProxy) state).bounds();
//			display.setSize(bounds.getWidth(), bounds.getHeight());
//		} catch (RemoteException | NotBoundException e) {
//			throw new RuntimeException(e);
//		}

	}

	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}

	public void setupPortrayals() {
		
		
        walkwaysPortrayal.setField(walkways);
        walkwaysPortrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN,true));

        buildingPortrayal.setField(buildings);
        BuildingLabelPortrayal b = new BuildingLabelPortrayal(new GeomPortrayal(Color.DARK_GRAY,true), Color.BLUE);
        buildingPortrayal.setPortrayalForAll(b);
        
        roadsPortrayal.setField(roads);
        roadsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.GRAY,true));

		agentPortrayal.setField(((CampusWorldProxy) state).agents);
//        agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED,10.0,true));
//        agentPortrayal.setPortrayalForAll(new AdjustablePortrayal2D(new MovablePortrayal2D(basic)));

//        agentPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.RED,6.0));
		agentPortrayal.setPortrayalForAll(new MovablePortrayal2D(new OvalPortrayal2D(Color.RED, 6.0)));

		// reschedule the displayer
		display.reset();
		display.setBackdrop(Color.WHITE);

		// redraw the display
		display.repaint();
	}

	public void init(final Controller c) {
		super.init(c);

		// Make the Display2D
		display = new Display2D(DCampusWorld.width, DCampusWorld.height, this);

		// attach the portrayals
        display.attach(walkwaysPortrayal, "Walkways", true);
        display.attach(buildingPortrayal, "Buildings", true);
        display.attach(roadsPortrayal, "Roads", true);
		display.attach(agentPortrayal, "Agents", true);

		displayFrame = display.createFrame();
		displayFrame.setTitle(displayFrame.getTitle());
		c.registerFrame(displayFrame); // register the frame so it appears in the "Display" list
		displayFrame.setVisible(true);

		// specify the backdrop color -- what gets painted behind the displays
		display.setBackdrop(Color.WHITE);
	}

	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null; // let gc
		display = null; // let gc
	}
	
	void loadStatic() {
		try {
			System.out.println("reading buildings layer ...");

			// this Bag lets us only display certain fields in the Inspector, the non-masked
			// fields
			// are not associated with the object at all
			final Bag masked = new Bag();
			masked.add("NAME");
			masked.add("FLOORS");
			masked.add("ADDR_NUM");

			// read in the buildings GIS file
			final URL bldgGeometry = DCampusWorldData.class.getResource("bldg.shp");
			final URL bldgDB = DCampusWorldData.class.getResource("bldg.dbf");
			ShapeFileImporter.read(bldgGeometry, bldgDB, buildings, masked);

			// We want to save the MBR so that we can ensure that all GeomFields
			// cover identical area.
			MBR = buildings.getMBR();

			System.out.println("reading roads layer");

			final URL roadGeometry = DCampusWorldData.class.getResource("roads.shp");
			final URL roadDB = DCampusWorldData.class.getResource("roads.dbf");
			ShapeFileImporter.read(roadGeometry, roadDB, roads);

			MBR.expandToInclude(roads.getMBR());

			System.out.println("reading walkways layer");

			final URL walkWayGeometry = DCampusWorldData.class.getResource("walk_ways.shp");
			final URL walkWayDB = DCampusWorldData.class.getResource("walk_ways.dbf");
			ShapeFileImporter.read(walkWayGeometry, walkWayDB, walkways);

			MBR.expandToInclude(walkways.getMBR());

			System.out.println("Done reading data");

			// Now synchronize the MBR for all GeomFields to ensure they cover the same area
			buildings.setMBR(MBR);
			roads.setMBR(MBR);
			walkways.setMBR(MBR);

			network.createFromGeomField(walkways);

			//addIntersectionNodes(network.nodeIterator(), junctions);

		} catch (final Exception ex) {
			Logger.getLogger(DCampusWorld.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
