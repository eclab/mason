package sim.app.dflockers.dregistry;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import mpi.MPI;
import mpi.MPIException;
import sim.engine.DSteppable;
import sim.engine.DSimState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Timing;
import sim.util.*;

public class DFlockersWithDRegistry extends DSimState
{
	private static final long serialVersionUID = 1;

	public final static int width = 400;
	public final static int height = 400;
	public final static int numFlockers = 100;
	public final static double cohesion = 0.0;
	public final static double avoidance = 1.0;
	public final static double randomness = 1.0;
	public final static double consistency = 1.0;
	public final static double momentum = 1.0;
	public final static double deadFlockerProbability = 0;
	public final static double neighborhood = 6; // aoi
	public final static double jump = 0.7; // how far do we move in a time step?

	public final DContinuous2D<DFlockerWithDRegistry> flockers;

	/** Creates a Flockers simulation with the given random number seed. */
	public DFlockersWithDRegistry(final long seed)
	{
		super(seed, DFlockersWithDRegistry.width, DFlockersWithDRegistry.height, (int) DFlockersWithDRegistry.neighborhood);
		enableRegistry(); // used to enable the object registry
		// final double[] discretizations = new double[] {
		// DFlockersWithDRegistry.neighborhood / 1.5,
		// DFlockersWithDRegistry.neighborhood / 1.5 };
		flockers = new DContinuous2D<>((int) (neighborhood / 1.5), this);
	}

	public void start()
	{
		super.start();
		final int width = getPartition().getLocalBounds().getWidth();
		final int height = getPartition().getLocalBounds().getHeight();

		for (int x = 0; x < DFlockersWithDRegistry.numFlockers / getPartition().getNumProcessors(); x++)
		{
			final double px = random.nextDouble() * width + getPartition().getLocalBounds().ul().toArray()[0];
			final double py = random.nextDouble() * height + getPartition().getLocalBounds().ul().toArray()[1];
			final Double2D location = new Double2D(px, py);
//			final DFlockerWithDRegistry flocker = new DFlockerWithDRegistry(location, 
//					(getPartitioning().pid * (DFlockersWithDRegistry.numFlockers / getPartitioning().numProcessors)) + x);

			final DFlockerWithDRegistry flocker = new DFlockerWithDRegistry(location);

			flockers.addAgent(location, flocker, 0, 0);

			try
			{
				if (x == 0 && MPI.COMM_WORLD.getRank() == 0)
					try
					{
						flocker.amItheBoss = true;
						this.getDRegistry().registerObject("cafebabe", flocker);
					}
					catch (AccessException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (RemoteException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			catch (MPIException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		schedule.scheduleRepeating(Schedule.EPOCH, 2, new DSteppable()
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void step(SimState state)
			{
				try
				{

					MPI.COMM_WORLD.barrier();
					if (MPI.COMM_WORLD.getRank() == 0)
					{
						DFlockersWithDRegistry flockers = (DFlockersWithDRegistry) state;
						DFlockerDummyRemote myfriend = flockers.getDRegistry().getObjectT("cafebabe");
						int fval = myfriend.getVal();
						int update_val = (int) (DFlockersWithDRegistry.numFlockers *
								(flockers.schedule.getSteps() + 1));
						if (fval != update_val)
						{
							System.err.println("Error in friend value for processor : " + MPI.COMM_WORLD.getRank()
									+ " at step " + flockers.schedule.getSteps());
							System.err.println(flockers.schedule.getSteps() + " " + fval + " != " + update_val);
							System.exit(-1);
						}
					}

				}
				catch (AccessException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (RemoteException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (NotBoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (MPIException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public static void main(final String[] args) throws MPIException
	{
		Timing.setWindow(20);
		doLoopDistributed(DFlockersWithDRegistry.class, args);
		System.exit(0);
	}
}