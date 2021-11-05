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
		wanderers = new DContinuous2D<>((int) (neighborhood), this);
	}
		
	protected void startRoot() {
		
		ArrayList<Wanderer> agents = new ArrayList<Wanderer>();

		Wanderer agentA = new Wanderer(new Double2D(50.0, 50.0));
		Wanderer agentB = new Wanderer(new Double2D(150.0, 150.0));
		agentA.name = "A";
		agentB.name = "B";
	
		agents.add(agentA);
		agents.add(agentB);

		sendRootInfoToAll("agents", agents);
	}

	public void start() {
		super.start();

		ArrayList<Wanderer> agents = (ArrayList<Wanderer>) getRootInfo("agents");

		for (Object obj : agents) {
			Wanderer agent = (Wanderer) obj;
			if (getPartition().getLocalBounds().contains(agent.loc)){
				try {
					this.getDRegistry().registerObject(agent.name, agent);
				} catch (Exception e) {
					e.printStackTrace();
				} 
				wanderers.addAgent(agent.loc, agent, 0, 0, 1);
			}
		}
	}

	public static void main(final String[] args) throws MPIException {
		Timing.setWindow(20);
		doLoopDistributed(Wanderers.class, args);
		System.exit(0);
	}
}