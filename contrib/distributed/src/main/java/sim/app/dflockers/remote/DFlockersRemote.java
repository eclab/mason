package sim.app.dflockers.remote;

import java.io.File;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import mpi.MPIException;
import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Timing;
import sim.util.*;

public class DFlockersRemote extends DSimState {
	private static final long serialVersionUID = 1;

	public final static int width = 60;
	public final static int height = 60;
	public final static int numFlockers = 10;
	public final static double cohesion = 1.0;
	public final static double avoidance = 1.0;
	public final static double randomness = 1.0;
	public final static double consistency = 1.0;
	public final static double momentum = 1.0;
	public final static double deadFlockerProbability = 0;
	public final static int neighborhood = 6; // aoi
	public final static double jump = 0.7; // how far do we move in a time step?

	public final DContinuous2D<DFlockerRemote> flockers;

	final SimpleDateFormat format = new SimpleDateFormat("ss-mm-HH-yyyy-MM-dd");
	String dateString = format.format(new Date());
	String dirname = System.getProperty("user.dir") + File.separator + dateString;

	/** Creates a Flockers simulation with the given random number seed. */
	public DFlockersRemote(final long seed) {
		super(seed, DFlockersRemote.width, DFlockersRemote.height, DFlockersRemote.neighborhood);
		enableRegistry(); // used to enable the object registry

		flockers = new DContinuous2D<>((int) (neighborhood / 1.5), this);
	}

	/* Initialization of agent */
	protected void startRoot() {
		ArrayList<DFlockerRemote> agents = new ArrayList<DFlockerRemote>();
		for (int x = 0; x < DFlockersRemote.numFlockers; x++) {
			final Double2D loc = new Double2D(random.nextDouble() * width, random.nextDouble() * height);
			DFlockerRemote flocker = null;
			try {
				flocker = new DFlockerRemote(loc);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (random.nextBoolean(deadFlockerProbability))
				flocker.dead = true;
			agents.add(flocker);
		}
		sendRootInfoToAll("agents", agents);
	}

	public void start() {
		super.start();

		ArrayList<DFlockerRemote> agents = (ArrayList<DFlockerRemote>) getRootInfo("agents");

//		for (int x = 0; x < agents.size(); x++) {
//			DFlockerRemote a = (DFlockerRemote) agents.get(x);
//			if (partition.getLocalBounds().contains(a.loc)) {
//				flockers.addAgent(a.loc, a, 0, 0);
//				String flockName = "";
//				try {
//					flockName = "flock" + x;
//					this.getDRegistry().registerObject(flockName, a);
////					System.out.println("$$$$ Flocker with name " + flockName + " registered.");
//				} catch (Exception e) {
//					System.err.println("Error in registering agent " + a + " with name " + flockName);
//					e.printStackTrace();
//				}
//			}
//		}

		for (int x = 0; x < agents.size(); x++) {
			DFlockerRemote a = (DFlockerRemote) agents.get(x);
			if (partition.getLocalBounds().contains(a.loc)) {
				flockers.addAgent(a.loc, a, 0, 0, 1);
				if (x == 0) {
					try {
						this.getDRegistry().registerObject("cafebabe", a);
//					System.out.println("$$$$ Flocker with name " + flockName + " registered.");
						System.out.println("Cafebabe registered!");
					} catch (Exception e) {
						System.err.println("Error in registering agent cafebabe" + a);
						e.printStackTrace();
					}
				}

			}
		}
	}

	public static void main(final String[] args) throws MPIException {
		Timing.setWindow(20);
		doLoopDistributed(DFlockersRemote.class, args);
		System.exit(0);
	}
}