/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Timing;
import sim.util.*;

public class DFlockers extends DSimState
{
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
	public DFlockers(final long seed)
	{
		super(seed, DFlockers.width, DFlockers.height, DFlockers.neighborhood);

		flockers = new DContinuous2D<>((int) (DFlockers.neighborhood / 1.5), this);

	}

	@Override
	protected void startRoot()
	{
		ArrayList<DFlocker> agents = new ArrayList<DFlocker>();
		for (int x = 0; x < DFlockers.numFlockers; x++)
		{
			final Double2D loc = new Double2D(random.nextDouble() * width, random.nextDouble() * height);
			DFlocker flocker = new DFlocker(loc);
			if (random.nextBoolean(deadFlockerProbability))
				flocker.dead = true;
			agents.add(flocker);
		}

		sendRootInfoToAll("agents", agents);
	}

	@Override
	public void start()
	{
		// TODO Auto-generated method stub
		super.start(); // do not forget this line

		// ArrayList<DFlocker> agents = (ArrayList<DFlocker>) rootInfo.get("agents");
		ArrayList<DFlocker> agents = (ArrayList<DFlocker>) getRootInfo("agents");

		for (Object p : agents)
		{
			DFlocker a = (DFlocker) p;
			if (partition.getLocalBounds().contains(a.loc))
				flockers.addAgent(a.loc, a, 0, 0, 1);
		}

	}

	public static void main(final String[] args)
	{
		Timing.setWindow(20);
		doLoopDistributed(DFlockers.class, args);
		System.exit(0);
	}
}
