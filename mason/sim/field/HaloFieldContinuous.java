package sim.field;

import java.util.*;

import mpi.*;

public class HaloFieldContinuous {

	DUniformPartition p;
	double aoi;

	public double[] lb, ub, lsize;

	int[] ns;

	public HaloFieldContinuous(DUniformPartition p, double aoi) {
		this.p = p;
		this.aoi = aoi;

		//2d order = [W, E, N, NW, NE, S, SW, SE]
		try {
			ns = p.getExtendedNeighborIds(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		lsize = new double[p.dims.length];
		lb = new double[p.dims.length];
		ub = new double[p.dims.length];
		for (int i = 0; i < p.dims.length; i++) {
			lsize[i] = p.size[i] / p.dims[i];
			lb[i] = lsize[i] * p.coords[i];
			ub[i] = lb[i] + lsize[i];
		}
	}

	public boolean inGlobal(final double[] c) {
		boolean ret = true;
		for (int i = 0; i < c.length; i++)
			ret &= c[i] >= 0 && c[i] < p.size[i];
		return ret;
	}

	public boolean inLocal(final double[] c) {
		boolean ret = true;
		for (int i = 0; i < c.length; i++)
			ret &= c[i] >= lb[i] && c[i] < ub[i];
		return ret;
	}

	public boolean inPrivate(final double[] c) {
		boolean ret = true;
		for (int i = 0; i < c.length; i++)
			ret &= c[i] >= lb[i] + aoi && c[i] < ub[i] - aoi;
		return ret;
	}

	public boolean inShared(final double[] c) {
		return inLocal(c) && !inPrivate(c);
	}

	public boolean inHalo(final double[] c) {
		return inLocalAndHalo(c) && !inLocal(c);
	}

	public boolean inLocalAndHalo(final double[] c) {
		boolean ret = true;
		for (int i = 0; i < c.length; i++) {
			double lc = toLocalCoord(c[i], i);
			ret &= lc >= 0 && lc < lsize[i] + 2 * aoi;
		}
		return ret;
	}

	public double[] toToroidal(double[] c) {
		for (int i = 0; i < c.length; i++) {
			if (c[i] < 0)
				c[i] += p.size[i];
			else if (c[i] >= p.size[i])
				c[i] -= p.size[i];
		}
		return c;
	}

	public int[] toNeighbors(double[] c) {
		assert inShared(c);
		double[] lc = toLocalCoords(c);

		// Currently only works for 2d
		assert c.length == 2;

		ArrayList<Integer> dest = new ArrayList<Integer>();
		if (lc[1] < aoi)
			dest.add(ns[0]);
		else if (lc[1] >= lsize[1] - aoi)
			dest.add(ns[1]);
		if (lc[0] < aoi) {
			dest.add(ns[2]);
			if (lc[1] < aoi)
				dest.add(ns[3]);
			else if (lc[1] >= lsize[1] - aoi)
				dest.add(ns[4]);
		} else if (lc[0] >= lsize[0] - aoi) {
			dest.add(ns[5]);
			if (lc[1] < aoi)
				dest.add(ns[6]);
			else if (lc[1] >= lsize[1] - aoi)
				dest.add(ns[7]);
		}

		return dest.stream().mapToInt(i->i).toArray();
	}

	public double[] toLocalCoords(double[] c) {
		for (int i = 0; i < c.length; i++)
			c[i] = toLocalCoord(c[i], i);
		return c;
	}

	private double toLocalCoord(final double c, final int i) {
		double lc = c - lb[i] + aoi;
		if (lc < 0)
			lc += p.size[i];
		else if (lc >= lsize[i] + 2 * aoi)
			lc -= p.size[i];
		return lc;
	}


	public static void main(String args[]) throws MPIException {
		int[] size = new int[] {100, 200};
		double aoi = 10;
		double[] want, got;

		MPI.Init(args);

		DUniformPartition p = new DUniformPartition(size);
		HaloFieldContinuous hf = new HaloFieldContinuous(p, aoi);

		assert p.np == 4;

		assert hf.inGlobal(new double[] {59, 82});
		assert !hf.inGlobal(new double[] { -3, 40});
		assert !hf.inGlobal(new double[] {35, 240});

		want = new double[] {70, 50};
		got = hf.toToroidal(new double[] { -30, 250});
		assert Arrays.equals(want, got);

		want = new double[] {20, 180};
		got = hf.toToroidal(new double[] {120, -20});
		assert Arrays.equals(want, got);

		if (p.pid == 0) {
			assert hf.inLocal(new double[] {0, 99});
			assert !hf.inLocal(new double[] {50, 0});

			assert hf.inPrivate(new double[] {aoi, aoi});
			assert !hf.inPrivate(new double[] {50, 0});
			assert !hf.inPrivate(new double[] {0, 99});

			assert hf.inShared(new double[] {49, 99});
			assert !hf.inShared(new double[] {50, 100});
			assert !hf.inShared(new double[] {25, 50});

			assert hf.inHalo(new double[] {0, 100});
			assert hf.inHalo(new double[] {0, 199});
			assert hf.inHalo(new double[] {95, 100});
			assert !hf.inHalo(new double[] {25, 50});
			assert !hf.inHalo(new double[] {75, 50});

			want = new double[] {30, 60};
			got = hf.toLocalCoords(new double[] {20, 50});
			assert Arrays.equals(want, got);

			want = new double[] {5, 5};
			got = hf.toLocalCoords(new double[] {95, 195});
			assert Arrays.equals(want, got);
		}

		MPI.Finalize();
	}
}