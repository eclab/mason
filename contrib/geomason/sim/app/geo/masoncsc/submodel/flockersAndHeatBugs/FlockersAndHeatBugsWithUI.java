package masoncsc.submodel.flockersAndHeatBugs;

import masoncsc.submodel.MetaGUIState;
import masoncsc.submodel.flockersAndHeatBugs.flockers.FlockersWithUI;
import masoncsc.submodel.flockersAndHeatBugs.heatBugs.HeatBugsWithUI;
import sim.display.GUIState;

public class FlockersAndHeatBugsWithUI extends MetaGUIState
{
	FlockersWithUI flockersWithUI;
	HeatBugsWithUI heatBugsWithUI;

	public FlockersAndHeatBugsWithUI() {
		super(new FlockersAndHeatBugs(System.currentTimeMillis()));
		flockersWithUI = new FlockersWithUI(((FlockersAndHeatBugs)state).flockers);
		heatBugsWithUI = new HeatBugsWithUI(((FlockersAndHeatBugs)state).heatBugs);
		setGUIStates(new GUIState[] { flockersWithUI, heatBugsWithUI });
	}

	public Object getSimulationInspectedObject() {
		return state;
	} // non-volatile

	public static void main(String[] args) {
		new FlockersAndHeatBugsWithUI().createController();
	}

}
