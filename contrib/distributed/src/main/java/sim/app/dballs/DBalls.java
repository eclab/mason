package sim.app.dballs;

import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.field.continuous.DContinuous2D;
import sim.field.network.*;
import sim.util.Double2D;

/*
 * Simulation based on MASON tutorial 5
 */
public class DBalls extends DSimState {
	private static final long serialVersionUID = 1;

	public final DContinuous2D<DBall> balls;
	public Network bands;


	public final static int numBalls = 50;
	public final static int numBands = 100;
	public final static int maxMass = 10;
	public final static int minMass = 1;
	public final static int minLaxBandDistance = 10;
	public final static int maxLaxBandDistance = 50;
	public final static int minBandStrength = 5;
	public final static int maxBandStrength = 10;
	public final static int collisionDistance = 5;

	public DBalls(final long seed) {
		super(seed, 100, 100, 5);
		enableRegistry(); // used to enable the object registry

		balls = new DContinuous2D<>(collisionDistance, this);
		bands = new Network();
	}

	// initialization of agents
	public void startRoot() {
		ArrayList<DBall> agents = new ArrayList<DBall>();
		for (int i = 0; i < numBalls; i++) {
			final Double2D loc = new Double2D(random.nextDouble() * 100, random.nextDouble() * 100);
			DBall ball = null;
			
			try {
				ball = new DBall(Integer.toString(i), loc, 0, 0, random.nextDouble() * (maxMass - minMass) + minMass);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			agents.add(ball);
			try {
				this.getDRegistry().registerObject(Integer.toString(i), ball);
			}catch (Exception e1) {
				System.out.println("Exception while registering agent " + ball);
				e1.printStackTrace();
			}
		}
		sendRootInfoToAll("agents", agents);

		// create the undirected graph where balls are the node
		Network bands = new Network(false);
		
		// make the bands connecting the balls, i.e. connect the nodes of the graph
		for (int i = 0; i < numBands; i++) {
			DBand band = new DBand(random.nextDouble() * (maxLaxBandDistance - minLaxBandDistance) + minLaxBandDistance,
					random.nextDouble() * (maxBandStrength - minBandStrength) + minBandStrength);
			
			DBall from = (DBall) agents.get(random.nextInt(agents.size()));

			DBall to = from;

			while (to == from)
				to = (DBall) agents.get(random.nextInt(agents.size()));

			bands.addEdge(from, to, band);
		}

		sendRootInfoToAll("bands", bands);
		
	}

	public void start() {
		super.start();
		ArrayList<DBall> agents = (ArrayList<DBall>) getRootInfo("agents");
		
		for(int i = 0; i<agents.size(); i++) {
			DBall a = (DBall) agents.get(i);
			if (partition.getLocalBounds().contains(a.loc)) {
				balls.addAgent(a.loc, a, 0, 0, 1);
				
				schedule.scheduleRepeating(Schedule.EPOCH, 1, new DSteppable() {
					private static final long serialVersionUID = 1;

					@Override
					public void step(SimState state) {
						a.computeForce(state);
//						System.out.println("&&&&&&&&&&&&&&&&&&&&&&& COMPUTING FORCE");
					}
				});
			}
		}
		

	}

	public static void main(String[] args) {
		doLoopDistributed(DBalls.class, args);
		System.exit(0);
	}

}
