package sim.app.geo.masoncsc.submodel;

import sim.display.Console;
import sim.display.Controller;
import sim.display.GUIState;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.inspector.TabbedInspector;

public class MetaGUIState extends GUIState
{
	GUIState[] guiStates;

	public MetaGUIState(MetaSimState state) {
		super(state);

		// MetaSimState manually starts all its SimStates in its start() function.
		// However, those SimStates also get started from their GUIState's 
		// start() function. This flag prevents them from being started twice.
		state.startSimStatesManually = false;
	}
	
	public void setGUIStates(GUIState[] guiStates) {
		this.guiStates = guiStates;
	}
	
    @Override
    public Inspector getInspector() {
		TabbedInspector ti = new TabbedInspector();
		for (int i = 0; i < guiStates.length; i++)
			ti.addInspector(new SimpleInspector(guiStates[i].state, guiStates[i]), GUIState.getName(guiStates[i].getClass()));
		ti.setVolatile(false);
		return ti;
	}
    
    @Override
	public void start() {
		super.start();
		for (int i = 0; i < guiStates.length; i++)
			guiStates[i].start();
	}

	@Override
	public void init(final Controller c) {
		super.init(c);
		for (int i = 0; i < guiStates.length; i++)
			guiStates[i].init(c);

		((Console)controller).setSize(400, 600);
	}

	@Override
	public boolean step() {
		boolean result = super.step();
		for (int i = 0; i < guiStates.length; i++)
			guiStates[i].step();

		return result;
	}

	@Override
	public void quit() {
		super.quit();
		for (int i = 0; i < guiStates.length; i++)
			guiStates[i].quit();
	}
}
