package masoncsc.submodel;

import sim.engine.Schedule;
import sim.engine.SimState;

public class MetaSimState extends SimState
{
	private static final long serialVersionUID = 1L;
	
	SimState[] simStates;
	
	/**
	 * MetaSimState manually starts all its SimStates in its start() function.
	 * However, those SimStates also get started from their GUIState's 
	 * start() function. This flag prevents them from being started twice.
	 */
	boolean startSimStatesManually = true;

	public MetaSimState(long seed) {
		super(seed);
	}
	
	/**
	 * This function must be called by the subclass's constructor.
	 * @param simStates Array of SimStates, already initialized.
	 */
	public void setSimStates(SimState[] simStates) {
		this.simStates = simStates;
		schedule = new MetaSchedule(simStates);
	}

	@Override
	public void start() {
		super.start();
		
		if (simStates == null)
			throw new NullPointerException("Subclasses of MetaSimState must call setSimStates(new SimState[] { ... }) from their constructor.");

		if (startSimStatesManually)
			for (int i = 0; i < simStates.length; i++) {
				simStates[i].schedule = new Schedule();
				simStates[i].start();
			}
	}

}
