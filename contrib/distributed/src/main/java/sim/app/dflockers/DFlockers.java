/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import mpi.MPI;
import mpi.MPIException;
import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.field.partitioning.DoublePoint;
import sim.util.Timing;

public class DFlockers extends DSimState {
	private static final long serialVersionUID = 1;

	public final static int width = 600;
	public final static int height = 600;
	public final static int numFlockers = 1000;
	public final static double cohesion = 1.0;
	public final static double avoidance = 1.0;
	public final static double randomness = 1.0;
	public final static double consistency = 1.0;
	public final static double momentum = 1.0;
	public final static double deadFlockerProbability = 0;
	public final static int neighborhood = 6; // aoi
	public final static double jump = 0.7; // how far do we move in a time step?

	public final DContinuous2D<DFlocker> flockers;

	final SimpleDateFormat format = new SimpleDateFormat("ss-mm-HH-yyyy-MM-dd");
	String dateString = format.format(new Date());
	String dirname = System.getProperty("user.dir") + File.separator + dateString;

	/** Creates a Flockers simulation with the given random number seed. */
	public DFlockers(final long seed) {
		super(seed, DFlockers.width, DFlockers.height, DFlockers.neighborhood);

		final double[] discretizations = new double[] { DFlockers.neighborhood / 1.5, DFlockers.neighborhood / 1.5 };
		flockers = new DContinuous2D<DFlocker>(getPartitioning(), aoi, discretizations, this);
	}

	@Override
	public void preSchedule() {
		super.preSchedule();

		if (schedule.getSteps() == 1 || schedule.getSteps() == 100) {
			String filename = dirname + File.separator +
					getPartitioning().pid + "." + (schedule.getSteps());

			File testdir = new File(dirname);
			testdir.mkdir();

			File myfileagent = new File(filename);
			System.out.println("Create file " + filename);

			PrintWriter out = null;
			try {
				myfileagent.createNewFile();
				out = new PrintWriter(new FileOutputStream(myfileagent, false));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (DFlocker f : flockers.getAllObjects()) {
				out.println(f.id);
			}

			out.close();
		}

	}

	public void start() {
		super.start();
		final int[] size = getPartitioning().getPartition().getSize();
		for (int x = 0; x < DFlockers.numFlockers / getPartitioning().numProcessors; x++) {
			final double px = random.nextDouble() * size[0] + getPartitioning().getPartition().ul().getArray()[0];
			final double py = random.nextDouble() * size[1] + getPartitioning().getPartition().ul().getArray()[1];
			final DoublePoint location = new DoublePoint(px, py);
			final DFlocker flocker = new DFlocker(location,
					(getPartitioning().pid * (DFlockers.numFlockers / getPartitioning().numProcessors)) + x);

			if (random.nextBoolean(DFlockers.deadFlockerProbability))
				flocker.dead = true;

			flockers.addAgent(location, flocker);
			
		
//			schedule.scheduleOnce(flocker, 1);
		}

		dateString = format.format(new Date());
		dirname = System.getProperty("user.dir") + File.separator + dateString;


//		SimpleDateFormat format = new SimpleDateFormat("ss-mm-HH-yyyy-MM-dd");
//		String dateString = format.format( new Date() );
//		String dirname = System.getProperty("user.dir")+File.separator+dateString;
//		
//		schedule.scheduleRepeating(Schedule.EPOCH - Schedule.BEFORE_SIMULATION, new AbstractStopping() {
//			
//			@Override
//			public void step(SimState state) {
//				final DFlockers dFlockers = (DFlockers) state;
//				
//				if(dFlockers.schedule.getSteps()== 1 || dFlockers.schedule.getSteps() == 100 || dFlockers.schedule.getSteps() == 101)
//				{
//					String filename = dirname+File.separator+
//							dFlockers.getPartitioning().pid+"."+(dFlockers.schedule.getSteps());
//					
//					File testdir = new File(dirname);
//					testdir.mkdir();
//					
//					File myfileagent = new File(filename);
//					System.out.println("Create file "+filename);
//					
//					PrintWriter out = null;
//					try {
//						myfileagent.createNewFile();
//						out = new PrintWriter(new FileOutputStream(myfileagent, false)); 
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					
//					for(DFlocker f : dFlockers.flockers.getAllObjects())
//					{
//						out.println(f.id);
//					}
//					
//					out.close();
//				}
//				
//				
//			}
//		});
//		

	}

	public static void main(final String[] args) throws MPIException {
		Timing.setWindow(20);
		doLoopMPI(DFlockers.class, args);
		System.exit(0);
	}
}
