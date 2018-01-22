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
	SimState sim;

	List<Object> ghosts;

	public DContinuous2D(final double discretization, double width, double height, double aoi, DUniformPartition p, SimState sim) {
		super(discretization, width, height);
		this.aoi = aoi;
		this.p = p;
		this.f = new HaloFieldContinuous(p, aoi);
		try {
			this.m = new DObjectMigrator(p);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		this.sim = sim;

		ghosts = new ArrayList<Object>();
	}

	@Override
	public boolean setObjectLocation(Object obj, final Double2D loc) {
		double[] loc_arr = new double[] {loc.x, loc.y};

		if (!f.inLocalAndHalo(loc_arr))
			throw new IllegalArgumentException(String.format("New location outside local partition and its halo area"));

		if (f.inPrivate(loc_arr)) {
			super.setObjectLocation(obj, loc);
		} else {
			DContinuous2DObject a = new DContinuous2DObject(obj, loc);
			try {
				if (f.inShared(loc_arr)) {
					super.setObjectLocation(obj, loc);
					for (int dst : f.toNeighbors(loc_arr))
						m.migrate(a, dst);
				} else if (f.inHalo(loc_arr)) {
					super.remove(obj);
					a.migrate = true;
					m.migrate(a, p.toPartitionId(loc_arr));
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		return true;
	}

	public void sync() {
		ghosts.forEach(super::remove);
		try {
			m.sync();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		ghosts = new ArrayList<Object>();
		for (Object o : m.objects) {
			DContinuous2DObject a = (DContinuous2DObject)o;
			super.setObjectLocation(a.obj, a.loc);
			if (a.migrate)
				sim.schedule.scheduleOnce((Steppable)a.obj, 1);
			else
				ghosts.add(a.obj);
		}
	}
}