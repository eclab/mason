package sim.field.grid;

import sim.util.*;
import mpi.*;
import java.util.ArrayList;
import java.nio.DoubleBuffer;

public class DDoubleGrid2DRMA extends DoubleGrid2D {

	public int pid, aoi, p_w, p_h, px, py, cell_width, cell_height;
	public BorderObject m[];

	int N = 1, S = 2, E = 4, W = 8;
	int NE = N | E, NW = N | W, SE = S | E, SW = S | W;
	int[] all_directions = new int[]{N, S, E, W, NE, NW, SE, SW};

	public DDoubleGrid2DRMA(int width, int height, int p_w, int p_h, int pid, int aoi, double initialValue) {
		super(width / p_w, height / p_h);
		
		cell_width = width / p_w;
		cell_height = height / p_h;
		px = (int)(pid / p_h);
		py = pid % p_h;
		this.pid = pid;
		this.aoi = aoi;
		this.p_w = p_w;
		this.p_h = p_h;
		this.width = width;
		this.height = height;

		m = new BorderObject[11];

		try {
			m[N] = new BorderObject(aoi, height, initialValue, getNeighborRank(N));
			m[S] = new BorderObject(aoi, height, initialValue, getNeighborRank(S));
			m[E] = new BorderObject(width, aoi, initialValue, getNeighborRank(E));
			m[W] = new BorderObject(width, aoi, initialValue, getNeighborRank(W));
			m[NE] = new BorderObject(aoi, aoi, initialValue, getNeighborRank(NE));
			m[NW] = new BorderObject(aoi, aoi, initialValue, getNeighborRank(NW));
			m[SE] = new BorderObject(aoi, aoi, initialValue, getNeighborRank(SE));
			m[SW] = new BorderObject(aoi, aoi, initialValue, getNeighborRank(SW));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	protected boolean isDistributed() {
		return true;
	}

	@Override
	public final double get(final int x, final int y) {
		int neighbor = 0;
		int lx = x - px * cell_width;
		int ly = y - py * cell_height;

		if (lx >= 0 && lx < cell_width && ly >= 0 && ly < cell_height)
			return this.field[lx][ly];

		if (lx < 0 && lx + aoi >= 0) {
			neighbor |= N;
			lx += aoi;
		}
		else if (lx >= cell_width && lx - aoi < cell_width) {
			neighbor |= S;
			lx -= cell_width;
		}

		if (ly < 0 && ly + aoi >= 0) {
			neighbor |= W;
			ly += aoi;
		}
		else if (ly >= cell_height && ly - aoi < cell_height) {
			neighbor |= E;
			ly -= cell_height;
		}

		if (neighbor == 0) {
			System.out.printf("pid %d tries to access [%d, %d] ([%d, %d]) which is outside its aoi \n", pid, x, y, lx, ly);
			Thread.dumpStack();
			System.exit(-1);
		}

		//System.out.printf("x %d y %d lx %d ly %d neighbor %s\n", x, y, lx, ly, Integer.toString(neighbor, 2));
		return m[neighbor].get(lx, ly);
	}

	@Override
	public final void set(final int x, final int y, final double val) {
		int lx = x - px * cell_width;
		int ly = y - py * cell_height;

		if (!(lx >= 0 && ly >= 0 && lx < cell_width && ly < cell_height)) {
			System.out.printf("pid %d tries to modify [%d, %d] ([%d, %d]) which is outside its own field\n", pid, x, y, lx, ly);
			Thread.dumpStack();
			System.exit(-1);
		}

		// Update local
		field[lx][ly] = val;

		// notify others if in shared region;
		ArrayList<Integer> dest = new ArrayList<Integer>();
		if (lx < aoi) {
			dest.add(N);
			if (ly < aoi)
				dest.add(NW);
			else if (ly >= cell_height - aoi)
				dest.add(NE);
		} else if (lx >= cell_width - aoi) {
			dest.add(S);
			if (ly < aoi) 
				dest.add(SW);
			 else if (ly >= cell_height - aoi) 
				dest.add(SE);
		}
		if (ly < aoi) 
			dest.add(W);
		else if (ly >= cell_height - aoi) 
			dest.add(E);

		// System.out.println(dest);
		// System.out.printf("x %d y %d lx %d ly %d\n", x, y, lx, ly);

		// Notify remote
		for (int n : dest)
			m[n].setRemote(getRemoteIdx(n, lx, ly), val);
	}

	public void sync() throws MPIException {
		for (int n : all_directions)
			m[n].mpiWin.fence(0);
	}

	public final int getRemoteIdx(final int n, final int lx, final int ly) {
		int rx = lx, ry = ly, len = cell_height;

		if ((n & N) != 0)
			rx += cell_width - aoi;
		else if ((n & S) != 0)
			rx += aoi - cell_width;

		if ((n & E) != 0) {
			ry += aoi - cell_height;
			len = aoi;
		} else if ((n & W) != 0){
			ry += cell_height - aoi;
			len = aoi;
		}

		return rx * len + ry;
	}

	public final int getNeighborRank(final int d) {
		int rank = pid;

		if ((d & N) != 0)
			rank = rank - p_h >= 0 ? rank - p_h : rank + (p_w - 1) * p_h;
		else if ((d & S) != 0)
			rank = (rank + 1) % p_h == 0 ? rank + 1 - p_h : rank + 1;
		if ((d & E) != 0)
			rank = rank >= (p_w - 1) * p_h ? rank - (p_w - 1) * p_h : rank + p_h;
		else if ((d & W) != 0)
			rank = rank % p_h == 0 ? rank - 1 + p_h : rank - 1;

		return rank;
	}

	public class BorderObject {
		public DoubleBuffer buf;
		public Win mpiWin;
		public int width, height;
		public int neighbor_rank;

		public BorderObject(int width, int height, double initialValue, int neighbor_rank) throws MPIException {
			this.width = width;
			this.height = height;
			this.neighbor_rank = neighbor_rank;
			buf = MPI.newDoubleBuffer(width * height);
			for (int i = 0; i < width * height; i++)
				buf.put(initialValue);
			mpiWin = new Win(buf, width * height, 1, new Info(), MPI.COMM_WORLD);
		}

		public double get(int x, int y) {
			try {
				return buf.get(x * height + y);
			} catch (Exception e) {
				System.out.printf("x %d y %d width %d height %d\n", x, y, width, height);
				e.printStackTrace();
				System.exit(-1);
			}
			return 0;
		}

		public double[] getRow(int x) {
			double[] ret = new double[height];
			for (int i = x * height; i < (x + 1) * height; i++)
				ret[i] = buf.get(i);
			return ret;
		}

		public double[] getCol(int y) {
			double[] ret = new double[width];
			for (int i = 0; i < width; i++)
				ret[i] = buf.get(i * height + y);
			return ret;
		}

		public void setRemote(int idx, double val) {
			if (neighbor_rank == pid)
				return;
			try {
				mpiWin.put(MPI.newDoubleBuffer(1).put(val), 1, MPI.DOUBLE, neighbor_rank, idx, 1, MPI.DOUBLE);
			} catch (MPIException e) {
				System.out.printf("val %g idx %d neighbor %d\n", val, idx, neighbor_rank);
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}