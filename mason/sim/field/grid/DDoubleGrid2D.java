package sim.field.grid;

import java.util.ArrayList;
import java.nio.DoubleBuffer;

import sim.util.*;
import sim.field.DUniformPartition;

import mpi.*;
import static mpi.MPI.slice;

public class DDoubleGrid2D extends DoubleGrid2D {

	public double[] field;
	public int width, height, pw, ph, psize, aoi;
	public DUniformPartition partition;

	CartComm comm;
	Datatype wtype, htype, ctype, ptype, p2type;

	public DDoubleGrid2D(int width, int height, int aoi, double initialValue, DUniformPartition partition) {
		super(width, height);

		this.width = width;
		this.height = height;
		this.aoi = aoi;
		this.partition = partition;
		comm = partition.comm;

		// Init local storage
		// Assume divide evenly
		pw = width / partition.dims[0];
		ph = height / partition.dims[1];
		psize = (pw + 2 * aoi) * (ph + 2 * aoi);
		field = new double[psize];
		for (int i = 0; i < psize; i++)
			field[i] = initialValue;

		// Setup MPI types for Halo Exchange
		try {
			wtype = Datatype.createVector(aoi, ph, ph + 2 * aoi, MPI.DOUBLE); 	wtype.commit();
			htype = Datatype.createVector(pw, aoi, ph + 2 * aoi, MPI.DOUBLE); 	htype.commit();
			ctype = Datatype.createVector(aoi, aoi, ph + 2 * aoi, MPI.DOUBLE); 	ctype.commit();
			ptype = Datatype.createVector(pw, ph, ph + 2 * aoi, MPI.DOUBLE); 	ptype.commit();
			p2type = Datatype.createVector(pw, ph, height, MPI.DOUBLE); 		p2type.commit();
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public final double get(final int x, final int y) {
		int lx = x - partition.coords[0] * pw + aoi;
		int ly = y - partition.coords[1] * ph + aoi;

		// In global
		assert (x >= 0 && x < width && y >= 0 && y < height);

		// handle toridal
		if (lx < 0)
			lx += width;
		else if (lx >= pw + 2 * aoi)
			lx -= width;
		if (ly < 0)
			ly += height;
		else if (ly >= ph + 2 * aoi)
			ly -= height;

		// In this partition and its surrounding ghost cells
		assert (lx >= 0 && lx < pw + 2 * aoi && ly >= 0 && ly < ph + 2 * aoi);

		return field[lx * (ph + 2 * aoi) + ly];
	}

	public final void set(final int x, final int y, final double val) {
		int lx = x - partition.coords[0] * pw + aoi;
		int ly = y - partition.coords[1] * ph + aoi;

		// In global
		assert (x >= 0 && x < width && y >= 0 && y < height);

		// In this partition but not in ghost cells
		assert (lx >= aoi && lx < pw + aoi && ly >= aoi && ly < ph + aoi);

		field[lx * (ph + 2 * aoi) + ly] = val;
	}

	public void sync() throws MPIException {
		// Prepare data
		int sendsize = 2 * (pw + ph) * aoi;
		byte[] sendbuf = new byte[sendsize * 8];
		byte[] recvbuf = new byte[sendsize * 8];
		int[] pos = new int[4], count = new int[4];
		int lastPos = 0;

		// Pack data into byte array
		pos[0] = lastPos; lastPos = comm.pack(slice(field, idx(aoi, aoi)), 1, wtype, sendbuf, lastPos); count[0] = lastPos - pos[0]; // pack north - field[aoi][aoi]
		pos[1] = lastPos; lastPos = comm.pack(slice(field, idx(pw , aoi)), 1, wtype, sendbuf, lastPos); count[1] = lastPos - pos[1]; // pack south - field[pw][aoi]
		pos[2] = lastPos; lastPos = comm.pack(slice(field, idx(aoi, aoi)), 1, htype, sendbuf, lastPos); count[2] = lastPos - pos[2]; // pack west - field[aoi][aoi]
		pos[3] = lastPos; lastPos = comm.pack(slice(field, idx(aoi, ph)), 1, htype, sendbuf, lastPos); 	count[3] = lastPos - pos[3]; // pack east - field[aoi][ph]

		// Exchange
		comm.neighborAllToAllv(sendbuf, count, pos, MPI.BYTE, recvbuf, count, pos, MPI.BYTE);

		// Unpack
		comm.unpack(recvbuf, pos[1], slice(field, idx(0, aoi)), 1, wtype); // unpack north
		comm.unpack(recvbuf, pos[0], slice(field, idx(pw + aoi, aoi)), 1, wtype); // unpack south
		comm.unpack(recvbuf, pos[3], slice(field, idx(aoi, 0)) , 1, htype); // unpack west
		comm.unpack(recvbuf, pos[2], slice(field, idx(aoi, ph + aoi)), 1, htype); // unpack east

		// four corners (9-cell stencil)
		lastPos = 0;
		pos[0] = lastPos; lastPos = comm.pack(slice(field, idx(aoi, aoi + ph)), 1, ctype, sendbuf, lastPos); 	count[0] = lastPos - pos[0];
		pos[1] = lastPos; lastPos = comm.pack(slice(field, idx(pw, 0)), 1, ctype, sendbuf, lastPos); 			count[1] = lastPos - pos[1];
		pos[2] = lastPos; lastPos = comm.pack(slice(field, idx(0, aoi)), 1, ctype, sendbuf, lastPos); 			count[2] = lastPos - pos[2];
		pos[3] = lastPos; lastPos = comm.pack(slice(field, idx(pw + aoi, ph)), 1, ctype, sendbuf, lastPos); 	count[3] = lastPos - pos[3];

		comm.neighborAllToAllv(sendbuf, count, pos, MPI.BYTE, recvbuf, count, pos, MPI.BYTE);

		comm.unpack(recvbuf, pos[1], slice(field, idx(0, 0)), 1, ctype);
		comm.unpack(recvbuf, pos[0], slice(field, idx(pw + aoi, ph + aoi)), 1, ctype);
		comm.unpack(recvbuf, pos[3], slice(field, idx(pw + aoi, 0)) , 1, ctype);
		comm.unpack(recvbuf, pos[2], slice(field, idx(0, ph + aoi)), 1, ctype);
	}

	public double[] collect(int dst) throws MPIException {
		double [] ret = null;
		byte[] buf = null;

		if (partition.pid == dst) {
			ret = new double[width * height];
			buf = new byte[width * height * 8];
		}

		MPI.COMM_WORLD.gather(slice(field, idx(aoi, aoi)), 1, ptype, buf, pw * ph * 8, MPI.BYTE, dst);

		if (partition.pid == dst) {
			for (int i = 0; i < partition.np; i++) {
				int[] coords = comm.getCoords(i);
				MPI.COMM_WORLD.unpack(buf, pw * ph * 8 * i, slice(ret, coords[0] * pw * height + coords[1] * ph), 1, p2type);
			}
		}

		return ret;
	}

	public static void print2dArray(double[] a, int w, int h) {
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++)
				System.out.printf("%.1f\t", a[i * h + j]);
			System.out.printf("\n");
		}
	}

	private int idx(int x, int y) {
		return x * (ph + 2 * aoi) + y;
	}

	public static void main(String[] args) throws MPIException {
		MPI.Init(args);

		DUniformPartition p = new DUniformPartition(new int[] {8, 8});
		DDoubleGrid2D f = new DDoubleGrid2D(8, 8, 1, 0, p);

		if (p.pid == 0) {
			for (int i = 0; i < f.partition.np; i++)
				System.out.printf("PID %d (%d, %d)\n", i, f.comm.getCoords(i)[0], f.comm.getCoords(i)[1]);
			System.out.println();
		}

		f.field[f.idx(1, 1)] = p.pid + 1;
		f.field[f.idx(1, 4)] = p.pid + 1;
		f.field[f.idx(4, 1)] = p.pid + 1;
		f.field[f.idx(4, 4)] = p.pid + 1;

		f.sync();

		if (p.pid == 0) {
			print2dArray(f.field, 6, 6);
			System.out.println();
		}

		double[] ret = f.collect(0);

		if (p.pid == 0)
			print2dArray(ret, 8, 8);

		MPI.Finalize();
	}
}