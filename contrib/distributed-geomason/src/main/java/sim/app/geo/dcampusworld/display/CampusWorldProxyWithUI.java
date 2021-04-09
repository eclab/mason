package sim.app.geo.dcampusworld.display;

import java.awt.Color;

import javax.swing.JFrame;

import sim.app.geo.campusworld.BuildingLabelPortrayal;
import sim.app.geo.campusworld.CampusWorld;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;

public class CampusWorldProxyWithUI extends GUIState {
    public Display2D display;
    public JFrame displayFrame;
	
    GeomVectorFieldPortrayal walkwaysPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal buildingPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal();
//    RectanglePortrayal2D walkwaysPortrayal = new RectanglePortrayal2D();
//    RectanglePortrayal2D buildingPortrayal = new RectanglePortrayal2D();
//    RectanglePortrayal2D roadsPortrayal = new RectanglePortrayal2D();

    ContinuousPortrayal2D agentPortrayal = new ContinuousPortrayal2D();

	public static void main(String[] args) {
		new CampusWorldProxyWithUI().createController();
	}
		
	public CampusWorldProxyWithUI() {
		super(new CampusWorldProxy(System.currentTimeMillis()));
	}
	
	public CampusWorldProxyWithUI(SimState state) {
		super(state);
	}
	
	public static String getName() {
		return "CampusWorld Proxy";
	}
	
	// TODO What is this?
    public Object getSimulationInspectedObject() { return state; }  // non-volatile

    // TODO?
    //    public Controller createController() {//...}

	public void start() {
		super.start();
		setupPortrayals();
	}
	
	public void load(SimState state) {
		super.load(state);;
        setupPortrayals();
    }
	
	public void setupPortrayals() {
		CampusWorldProxy world = (CampusWorldProxy) state;

        walkwaysPortrayal.setField(world.walkways);
        walkwaysPortrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN,true));

        buildingPortrayal.setField(world.buildings);
        BuildingLabelPortrayal b = new BuildingLabelPortrayal(new GeomPortrayal(Color.DARK_GRAY,true), Color.BLUE);
        buildingPortrayal.setPortrayalForAll(b);
        
        roadsPortrayal.setField(world.roads);
        roadsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.GRAY,true));

        agentPortrayal.setField(world.agents);
        agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED,10.0,true));
//        agentPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.RED,6.0));

        // reschedule the displayer
        display.reset();
        display.setBackdrop(Color.WHITE);

        // redraw the display
        display.repaint();
	}
	
	public void init(final Controller c) {
		super.init(c);
		
		// Make the Display2D
        display = new Display2D(CampusWorld.WIDTH, CampusWorld.HEIGHT, this);
		
		// attach the portrayals
        display.attach(walkwaysPortrayal, "Walkways", true);
        display.attach(buildingPortrayal, "Buildings", true);
        display.attach(roadsPortrayal, "Roads", true);
        display.attach(agentPortrayal, "Agents", true);

		displayFrame = display.createFrame();
		displayFrame.setTitle(displayFrame.getTitle());
		c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
		displayFrame.setVisible(true);
        
        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(Color.WHITE);
	}

	
    public void quit() {
	    super.quit();
	    
	    if (displayFrame!=null) displayFrame.dispose();
	    displayFrame = null;  // let gc
	    display = null;       // let gc
    }
}
