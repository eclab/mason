package sim.field.grid;
import sim.util.*;
import mpi.*;
import java.util.ArrayList;
import java.nio.DoubleBuffer;
import static mpi.MPI.slice;

public class DDoubleGrid2D extends DoubleGrid2D {

	public double[] field;
	public int width, height, np, pw, ph, psize, aoi;
	public int dims[], coords[];
	CartParms topoParams;
	CartComm comm;
	Datatype wtype, htype, ctype;

	public DDoubleGrid2D(int width, int height, int aoi, double initialValue) {
		super(width, height);

		this.width = width;
		this.height = height;
		this.aoi = aoi;

		// Init MPI Cartesian Topology (4 neighbors)
		dims = new int[] {0, 0};
		coords = new int[2];
		boolean periods[] = {true, true};

		try {
			np = MPI.COMM_WORLD.getSize();
			CartComm.createDims(np, dims);
			comm = ((Intracomm)MPI.COMM_WORLD).createCart(dims, periods, false);
			topoParams = comm.getTopo();
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		coords[0] = topoParams.getCoord(0);
		coords[1] = topoParams.getCoord(1);

		// Init local storage
		pw = width / dims[0];
		ph = height / dims[1];
		psize = (pw + 2 * aoi) * (ph + 2 * aoi);
		field = new double[psize];
		for (int i = 0; i < psize; i++)
			field[i] = initialValue;

		// Setup MPI types for Halo Exchange
		try {
			wtype = Datatype.createVector(aoi, ph, ph + 2 * aoi, MPI.DOUBLE);
			wtype.commit();
			htype = Datatype.createVector(pw, aoi, ph + 2 * aoi, MPI.DOUBLE);
			htype.commit();
			ctype = Datatype.createVector(aoi, aoi, ph + 2 * aoi, MPI.DOUBLE);
			ctype.commit();
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public final double get(final int x, final int y) {
		int lx = x - coords[0] * pw + aoi;
		int ly = y - coords[1] * ph + aoi;

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

		// In this partition and ghost cells
		assert (lx >= 0 && lx < pw + 2 * aoi && ly >= 0 && ly < ph + 2 * aoi);

		return field[lx * (ph + 2 * aoi) + ly];
	}

	public final void set(final int x, final int y, final double val) {
		int lx = x - coords[0] * pw + aoi;
		int ly = y - coords[1] * ph + aoi;

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
		pos[3] = lastPos; lastPos = comm.pack(slice(field, idx(aoi, ph)), 1, htype, sendbuf, lastPos); count[3] = lastPos - pos[3]; // pack east - field[aoi][ph]

		// Exchange
		comm.neighborAllToAllv(sendbuf, count, pos, MPI.BYTE, recvbuf, count, pos, MPI.BYTE);

		// Unpack
		comm.unpack(recvbuf, pos[0], slice(field, idx(0, aoi)), 1, wtype); // unpack north
		comm.unpack(recvbuf, pos[1], slice(field, idx(pw + aoi, aoi)), 1, wtype); // unpack south
		comm.unpack(recvbuf, pos[2], slice(field, idx(aoi, 0)) , 1, htype); // unpack west
		comm.unpack(recvbuf, pos[3], slice(field, idx(aoi, ph + aoi)), 1, htype); // unpack east

		// four corners (9-cell stencil)
		lastPos = 0;
		pos[0] = lastPos; lastPos = comm.pack(slice(field, idx(aoi, aoi + ph)), 1, ctype, sendbuf, lastPos); count[0] = lastPos - pos[0];
		pos[1] = lastPos; lastPos = comm.pack(slice(field, idx(pw, 0)), 1, ctype, sendbuf, lastPos); count[1] = lastPos - pos[1];
		pos[2] = lastPos; lastPos = comm.pack(slice(field, idx(0, aoi)), 1, ctype, sendbuf, lastPos); count[2] = lastPos - pos[2];
		pos[3] = lastPos; lastPos = comm.pack(slice(field, idx(pw + aoi, ph)), 1, ctype, sendbuf, lastPos); count[3] = lastPos - pos[3];

		comm.neighborAllToAllv(sendbuf, count, pos, MPI.BYTE, recvbuf, count, pos, MPI.BYTE);

		comm.unpack(recvbuf, pos[0], slice(field, idx(0, 0)), 1, ctype);
		comm.unpack(recvbuf, pos[1], slice(field, idx(pw + aoi, ph + aoi)), 1, ctype);
		comm.unpack(recvbuf, pos[2], slice(field, idx(pw + aoi, 0)) , 1, ctype);
		comm.unpack(recvbuf, pos[3], slice(field, idx(0, ph + aoi)), 1, ctype);
	}

	private int idx(int x, int y) {
		return x * (ph + 2 * aoi) + y;
	}
}