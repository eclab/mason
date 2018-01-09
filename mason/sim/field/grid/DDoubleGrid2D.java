package sim.field.grid;
import sim.util.*;
import mpi.*;
import java.util.ArrayList;
import java.nio.DoubleBuffer;
import static mpi.MPI.slice;

public class DDoubleGrid2D extends DoubleGrid2D {

	public double[] field;
	//DoubleBuffer buf;
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

		// Init MPI Topology
		dims = new int[2];
		coords = new int[2];
		boolean periods[] = {true, true};
		dims[0] = 0;
		dims[1] = 0;

		try {
			np = MPI.COMM_WORLD.getSize();
			CartComm.createDims(np, dims);
			comm = ((Intracomm)MPI.COMM_WORLD).createCart(dims, periods, false);
			topoParams = comm.getTopo();
			coords[0] = topoParams.getCoord(0);
			coords[1] = topoParams.getCoord(1);
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// Init local storage
		pw = width / dims[0];
		ph = height / dims[1];
		psize = (pw + 2 * aoi) * (ph + 2 * aoi);
		field = new double[psize];
		for (int i = 0; i < psize; i++)
			field[i] = initialValue;

		//buf = MPI.newDoubleBuffer(psize);

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

	public void sync() throws MPIException{
		// Prepare data
		int sendsize = 2 * (pw + ph) * aoi;
		byte[] sendbuf = new byte[sendsize * 8];
		byte[] recvbuf = new byte[sendsize * 8];
		int[] pos = new int[4], count = new int[4];
		int lastPos = 0;

		// pack north - field[aoi][aoi]
		pos[0] = lastPos;
		lastPos = comm.pack(slice(field, aoi * (ph + 2 * aoi) + aoi), 1, wtype, sendbuf, lastPos);
		count[0] = lastPos - pos[0];

		// pack south - field[pw][aoi]
		pos[1] = lastPos;
		lastPos = comm.pack(slice(field, pw * (ph + 2 * aoi) + aoi), 1, wtype, sendbuf, lastPos);
		count[1] = lastPos - pos[1];

		// pack west - field[aoi][aoi]
		pos[2] = lastPos;
		lastPos = comm.pack(slice(field, aoi * (ph + 2 * aoi) + aoi), 1, htype, sendbuf, lastPos);
		count[2] = lastPos - pos[2];

		// pack east - field[aoi][ph]
		pos[3] = lastPos;
		lastPos = comm.pack(slice(field, aoi * (ph + 2 * aoi) + ph), 1, htype, sendbuf, lastPos);
		count[3] = lastPos - pos[3];

		// Exchange
		comm.neighborAllToAllv(sendbuf, count, pos, MPI.BYTE, recvbuf, count, pos, MPI.BYTE);

		// unpack north
		comm.unpack(recvbuf, pos[0], slice(field, aoi), 1, wtype);
		// unpack south
		comm.unpack(recvbuf, pos[1], slice(field, (pw + aoi) * (ph + 2 * aoi) + aoi), 1, wtype);
		// unpack west
		comm.unpack(recvbuf, pos[2], slice(field, aoi * (ph + 2 * aoi)), 1, htype);
		// unpack east
		comm.unpack(recvbuf, pos[3], slice(field, aoi * (ph + 2 * aoi) + ph + aoi), 1, htype);
	}
}