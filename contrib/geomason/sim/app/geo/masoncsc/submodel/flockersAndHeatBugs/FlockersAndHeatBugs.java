package sim.app.geo.masoncsc.submodel.flockersAndHeatBugs;
import sim.app.geo.masoncsc.submodel.MetaSimState;
import sim.app.geo.masoncsc.submodel.flockersAndHeatBugs.flockers.Flockers;
import sim.app.geo.masoncsc.submodel.flockersAndHeatBugs.heatBugs.HeatBugs;
import sim.engine.SimState;


public class FlockersAndHeatBugs extends MetaSimState
{
	private static final long serialVersionUID = 1L;

	public Flockers flockers;
	public HeatBugs heatBugs;
	
	public FlockersAndHeatBugs(long seed) {
		super(seed);
		flockers = new Flockers(seed);
		heatBugs = new HeatBugs(seed);

		setSimStates(new SimState[] { flockers, heatBugs });
	}

	@Override
	public void start() {
		super.start();
	}

	public static void main(String[] args) {
		doLoop(FlockersAndHeatBugs.class, args);
		System.exit(0);
	}

}
