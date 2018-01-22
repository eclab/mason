package sim.field.continuous;

import java.util.*;

import sim.field.*;
import sim.util.*;
import sim.engine.*;

import mpi.*;

public /*strictfp*/ class DContinuous2D extends Continuous2D {
	double aoi;
	public DUniformPartition p;
	public HaloFieldContinuous f;
	DObjectMigrator m;
	Schedule sched;

	public List<Object> ghosts;
	List<Object> futureGhosts;

	public DContinuous2D(final double discretization, double width, double height, double aoi, DUniformPartition p, Schedule sched) {
		super(discretization, width, height);
		this.aoi = aoi;
		this.p = p;
		this.sched = sched;
		this.f = new HaloFieldContinuous(p, aoi);

		try {
			this.m = new DObjectMigrator(p);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		ghosts = new ArrayList<Object>();
		futureGhosts = new ArrayList<Object>();
	}

	@Override
	public boolean setObjectLocation(Object obj, final Double2D loc) {
		double[] loc_arr = new double[] {loc.x, loc.y};
		DContinuous2DObject a = new DContinuous2DObject(obj, loc);

		// if (!f.inLocalAndHalo(loc_arr))
		// 	throw new IllegalArgumentException(String.format("New location outside local partition and its halo area"));

		super.setObjectLocation(obj, loc);

		if (f.inPrivate(loc_arr)) {
			sched.scheduleOnce((Steppable)obj, 1);
		} else if (f.inShared(loc_arr)) {
			sched.scheduleOnce((Steppable)obj, 1);
			for (int dst : f.toNeighbors(loc_arr))
				m.migrate(a, dst);
		} else if (f.inHalo(loc_arr)) {
			futureGhosts.add(obj);
			a.migrate = true;
			try {
				m.migrate(a, p.toPartitionId(loc_arr));
			} catch (MPIException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		} else 
			return false;

		return true;
	}

	public void sync() {
		ghosts.forEach(super::remove);
		ghosts.clear();

		try {
			m.sync();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (Object o : m.objects) {
			DContinuous2DObject a = (DContinuous2DObject)o;
			super.setObjectLocation(a.obj, a.loc);
			if (a.migrate)
				sched.scheduleOnce((Steppable)a.obj, 1);
			else
				ghosts.add(a.obj);
		}
		ghosts.addAll(futureGhosts);

		m.objects.clear();
		futureGhosts.clear();
	}

	public static void main(String args[]) throws MPIException {
		MPI.Init(args);

		double width = 1000;
		double height = 1000;
		double neighborhood = 10;

		DUniformPartition p = new DUniformPartition(new int[] {(int)width, (int)height});
		fakeSchedule sch = new fakeSchedule(p.pid);
		DContinuous2D f = new DContinuous2D(neighborhood / 1.5, width, height, neighborhood, p, sch);
		DContinuous2DTestObject obj = null;
		Double2D loc = new Double2D(250, 250);
		String s = null;

		assert p.np == 4;

		// step 1 ---------------------------------
		if (p.pid == 0) {
			obj = new DContinuous2DTestObject(0, loc);
			f.setObjectLocation(obj, loc);
		}

		f.sync();

		s = String.format("PID %d Step %d Total objects %d\n", p.pid, sch.steps, f.size());
		System.out.print(s);

		sch.steps++;

		// step 2 ---------------------------------
		if (p.pid == 0) {
			loc = new Double2D(495, 250);
			obj.loc = loc;
			f.setObjectLocation(obj, loc);
		}

		f.sync();

		s = String.format("PID %d Step %d Total objects %d\n", p.pid, sch.steps, f.size());
		System.out.print(s);

		sch.steps++;

		// step 3 ---------------------------------
		if (p.pid == 0) {
			loc = new Double2D(500, 250);
			obj.loc = loc;
			f.setObjectLocation(obj, loc);
		}

		f.sync();

		s = String.format("PID %d Step %d Total objects %d\n", p.pid, sch.steps, f.size());
		System.out.print(s);

		sch.steps++;

		// step 4 ---------------------------------
		if (p.pid == 2) {
			loc = new Double2D(500, 250);
			Bag bag = f.getObjectsAtLocation(loc);
			//f.getAllObjects();
			assert bag.size() == 1;
			obj = (DContinuous2DTestObject)bag.pop();

			loc = new Double2D(750, 250);
			obj.loc = loc;
			f.setObjectLocation(obj, loc);
		}

		f.sync();

		s = String.format("PID %d Step %d Total objects %d\n", p.pid, sch.steps, f.size());
		System.out.print(s);

		sch.steps++;

		MPI.Finalize();
	}

	private static class fakeSchedule extends Schedule {

		public int steps = 0;
		public int pid;

		public fakeSchedule(int pid) {
			this.pid = pid;
		}

		@Override
		public boolean scheduleOnce(final Steppable event, final int ordering) {
			String s = String.format("PID %d Step %d scheduled object %s\n", pid, steps, event.toString());
			System.out.print(s);
			return true;
		}
	}
}