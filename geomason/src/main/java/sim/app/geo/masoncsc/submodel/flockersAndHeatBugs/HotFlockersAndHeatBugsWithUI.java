package sim.app.geo.masoncsc.submodel.flockersAndHeatBugs;

import java.awt.Color;

import javax.swing.JFrame;

import sim.app.geo.masoncsc.submodel.MetaGUIState;
import sim.app.geo.masoncsc.submodel.flockersAndHeatBugs.flockers.FlockersWithUI;
import sim.app.geo.masoncsc.submodel.flockersAndHeatBugs.heatBugs.HeatBugsWithUI;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.inspector.TabbedInspector;

public class HotFlockersAndHeatBugsWithUI extends MetaGUIState
{
	FlockersWithUI flockersWithUI;
	HeatBugsWithUI heatBugsWithUI;

    public Display2D display;
    public JFrame displayFrame;

	public HotFlockersAndHeatBugsWithUI() {
		super(new HotFlockersAndHeatBugs(System.currentTimeMillis()));
		flockersWithUI = new FlockersWithUI(((HotFlockersAndHeatBugs)state).flockers);
		heatBugsWithUI = new HeatBugsWithUI(((HotFlockersAndHeatBugs)state).heatBugs);
		setGUIStates(new GUIState[] { flockersWithUI, heatBugsWithUI });
	}

	public Object getSimulationInspectedObject() {
		return state;
	} // non-volatile
	
    @Override
    public Inspector getInspector() {
		TabbedInspector i = new TabbedInspector();
		i.addInspector(new SimpleInspector((HotFlockersAndHeatBugs)state, this), "Hybrid");
		i.addInspector(new SimpleInspector(((HotFlockersAndHeatBugs)state).flockers, this), "Flockers");
		i.addInspector(new SimpleInspector(((HotFlockersAndHeatBugs)state).heatBugs, this), "Heat Bugs");
		i.setVolatile(false);
		return i;
	}

	public void init(final Controller c) {
		super.init(c);
		
		// ----- Flockers
        // make the displayer
        display = new Display2D(750,750,this);
        display.setBackdrop(Color.black);

        displayFrame = display.createFrame();
        displayFrame.setTitle("HotFlockers and HeatBugs");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals
        display.attach(heatBugsWithUI.heatPortrayal,"Heat");
        display.attach(heatBugsWithUI.bugPortrayal,"Bugs");
        display.attach( flockersWithUI.flockersPortrayal, "Behold the Flock!" );

        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(Color.black);

		((Console)controller).setSize(400, 600);

	}

	public static void main(String[] args) {
		new HotFlockersAndHeatBugsWithUI().createController();
	}

}

