package sim.field.continuous;
import sim.field.*;
import sim.util.*;
import java.util.*;
import mpi.*;
import java.nio.*;

public /*strictfp*/ class DContinuous2DNBLK extends Continuous2D {
	public int px, py, pw, ph, pid;
	public double cell_width, cell_height;
	public double aoi;

	public List<Request> req_list = new ArrayList<Request>();

	public DContinuous2DNBLK(final double discretization, double width, double height, int pw, int ph, double aoi) {
		super(discretization, width, height);

		try {
			pid = MPI.COMM_WORLD.getRank();
		} catch (MPIException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		px = pid / ph;
		py = pid % ph;
		this.aoi = aoi;
		cell_height = height / ph;
		cell_width = width / pw;
		this.ph = ph;
		this.pw = pw;

		//System.out.printf("pid %d px %d py %d cw %g ch %g\n", pid, px, py, cell_width, cell_height);
	}

	@Override
	public synchronized final boolean setObjectLocation(Object obj, final Double2D location) {
		if (isPrivate(location)) {
			super.setObjectLocation(obj, location);
			return false;
		} else {
			super.remove(obj);
			int dst = toNeighborRank(location);
			DoubleBuffer data = MPI.newDoubleBuffer(3);
			data.put(location.x);
			data.put(location.y);
			data.put((double)pid);
			try {
				Request req = MPI.COMM_WORLD.iSend(data, 3, MPI.DOUBLE, dst, 0);
				req_list.add(req);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
			return true;
		}
	}

	public boolean isPrivate(final Double2D location) {
		return px * cell_width <= location.x && location.x < (px + 1) * cell_width && py * cell_height <= location.y && location.y < (py + 1) * cell_height;
	}

	public int toNeighborRank(final Double2D location) {
		int nx = (int)(location.x / (double)cell_width);
		int ny = (int)(location.y / (double)cell_height);

		return nx * ph + ny;
	}

	public void sync() {
		Request[] rs = new Request[req_list.size()];
		try {
			Request.waitAll(req_list.toArray(rs));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// private class MFlocker implements java.io.Serializable {
	// 	public Double2D location;
	// 	public boolean end = false;

	// 	public MFlocker(Double2D location) {
	// 		this.location = location;
	// 	}

	// 	public MFlocker() {
	// 		this.end = true;
	// 	}
	// }
}