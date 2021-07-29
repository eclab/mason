package sim.app.dwanderer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import mpi.MPIException;
import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Timing;
import sim.util.*;

public class Wanderers extends DSimState {
	private static final long serialVersionUID = 1;

	public final static int width = 200;
	public final static int height = 200;
	public final static int neighborhood = 1; // aoi

	public final DContinuous2D<Wanderer> wanderers;

	final SimpleDateFormat format = new SimpleDateFormat("ss-mm-HH-yyyy-MM-dd");
	String dateString = format.format(new Date());
	String dirname = System.getProperty("user.dir") + File.separator + dateString;

	/** Creates a Wanderers simulation with the given random number seed. */
	public Wanderers(final long seed) {
		super(seed, Wanderers.width, Wanderers.height, Wanderers.neighborhood);
		// enabling the RMI Registry
		this.enableRegistry();
		wanderers = new DContinuous2D<>((int) (neighborhood), this);
	}
		
	protected void startRoot() {
		
		ArrayList<Wanderer> agents = new ArrayList<Wanderer>();

		// agent A starts in the center of the NW quadrant
		Double2D locationA = new Double2D(50.0, 50.0);

		// agent B starts in the center of the SW quadrant
		Double2D locationB = new Double2D(150.0, 150.0);

		// name used to register the agents on the DRegistry
		String agentAName = "agentA";
		String agentBName = "agentB";

		Wanderer agentA = null;
		Wanderer agentB = null;

		try {
			agentA = new Wanderer(locationA);
			agentB = new Wanderer(locationB);

			agentA.setExportedName(agentAName);
			boolean test = this.getDRegistry().registerObject(agentAName, agentA);
			if(!test) System.exit(-1);
			System.out.println("Agent A registered with ID: " + agentAName + " with position " + locationA);

			agentB.setExportedName(agentBName);
			if(!test) System.exit(-1);
			this.getDRegistry().registerObject(agentBName, agentB);
			System.out.println("Agent B registered with ID: " + agentBName + " with position " + locationB);

		} catch (Exception e) {
			System.err.println("Failed to register agents.");
			e.printStackTrace();
		}

		agents.add(agentA);
		agents.add(agentB);

		sendRootInfoToAll("agents", agents);
	}

	public void start() {
		super.start();

		ArrayList<Wanderer> agents = (ArrayList<Wanderer>) getRootInfo("agents");

		// System.out.println("I am " + this.getPartition().getPID() + " and I have " + partition.getLocalBounds());

		// adding the agent
		for (Object obj : agents) {
			Wanderer agent = (Wanderer) obj;
			if (partition.getLocalBounds().contains(agent.loc)){
				wanderers.addAgent(agent.loc, agent, 0, 0, 1);
				// System.out.println("I am " + DSimState.getPID() + " and I take " + agent.getRemoteId());
			}
		}
	}

	public static void main(final String[] args) throws MPIException {
		Timing.setWindow(20);
		doLoopDistributed(Wanderers.class, args);
		System.exit(0);
	}
}