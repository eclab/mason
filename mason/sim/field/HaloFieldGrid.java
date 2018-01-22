package sim.field;

import java.util.Arrays;

import mpi.*;

public class HaloFieldGrid {

	DUniformPartition p;
	int aoi;

	int[] lb, ub, lsize;

	public HaloFieldGrid(DUniformPartition p, int aoi) {
		this.p = p;
		this.aoi = aoi;

		lsize = new int[p.dims.length];
		lb = new int[p.dims.length];
		ub = new int[p.dims.length];
		for (int i = 0; i < p.dims.length; i++) {
			lsize[i] = p.size[i] / p.dims[i];
			lb[i] = lsize[i] * p.coords[i];
			ub[i] = lb[i] + lsize[i];
		}
	}

	public boolean inGlobal(int[] c) {
		boolean ret = true;
		for (int i = 0; i < c.length; i++)
			ret &= c[i] >= 0 && c[i] < p.size[i];
		return ret;
	}

	public boolean inLocal(int[] c) {
		boolean ret = true;
		for (int i = 0; i < c.length; i++)
			ret &= c[i] >= lb[i] && c[i] < ub[i];
		return ret;
	}

	public boolean inPrivate(int[] c) {
		boolean ret = true;
		for (int i = 0; i < c.length; i++)
			ret &= c[i] >= lb[i] + aoi && c[i] < ub[i] - aoi;
		return ret;
	}

	public boolean inShared(int[] c) {
		return inLocal(c) && !inPrivate(c);
	}

	public boolean inHalo(int[] c) {
		return inLocalAndHalo(c) && !inLocal(c);
	}

	public boolean inLocalAndHalo(int[] c) {
		boolean ret = true;
		for (int i = 0; i < c.length; i++) {
			int lc = toLocalCoord(c[i], i);
			ret &= lc >= 0 && lc < lsize[i] + 2 * aoi;
		}
		return ret;
	}

	public int[] toToroidal(int[] c) {
		for (int i = 0; i < c.length; i++) {
			if (c[i] < 0)
				c[i] += p.size[i];
			else if (c[i] >= p.size[i])
				c[i] -= p.size[i];
		}
		return c;
	}

	public int[] toLocalCoords(int[] c) {
		for (int i = 0; i < c.length; i++)
			c[i] = toLocalCoord(c[i], i);
		return c;
	}

	private int toLocalCoord(int c, int i) {
		int lc = c - lb[i] + aoi;
		if (lc < 0)
			lc += p.size[i];
		else if (lc >= lsize[i] + 2 * aoi)
			lc -= p.size[i];
		return lc;
	}

	public static void main(String args[]) throws MPIException {
		int[] size = new int[] {100, 200};
		int aoi = 10;
		int[] want, got;

		MPI.Init(args);

		DUniformPartition p = new DUniformPartition(size);
		HaloFieldGrid hf = new HaloFieldGrid(p, aoi);

		assert p.np == 4;

		assert hf.inGlobal(new int[] {59, 82});
		assert !hf.inGlobal(new int[] { -3, 40});
		assert !hf.inGlobal(new int[] {35, 240});

		want = new int[] {70, 50};
		got = hf.toToroidal(new int[] { -30, 250});
		assert Arrays.equals(want, got);

		want = new int[] {20, 180};
		got = hf.toToroidal(new int[] {120, -20});
		assert Arrays.equals(want, got);

		if (p.pid == 0) {
			assert hf.inLocal(new int[] {0, 99});
			assert !hf.inLocal(new int[] {50, 0});

			assert hf.inPrivate(new int[] {aoi, aoi});
			assert !hf.inPrivate(new int[] {50, 0});
			assert !hf.inPrivate(new int[] {0, 99});

			assert hf.inShared(new int[] {49, 99});
			assert !hf.inShared(new int[] {50, 100});
			assert !hf.inShared(new int[] {25, 50});

			assert hf.inHalo(new int[] {0, 100});
			assert hf.inHalo(new int[] {0, 199});
			assert hf.inHalo(new int[] {95, 100});
			assert !hf.inHalo(new int[] {25, 50});
			assert !hf.inHalo(new int[] {75, 50});

			want = new int[] {30, 60};
			got = hf.toLocalCoords(new int[] {20, 50});
			assert Arrays.equals(want, got);

			want = new int[] {5, 5};
			got = hf.toLocalCoords(new int[] {95, 195});
			assert Arrays.equals(want, got);
		}

		MPI.Finalize();
	}
}