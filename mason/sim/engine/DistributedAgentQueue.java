package sim.engine;

import java.io.*;
import java.util.*;

import sim.util.*;
import ec.util.*;

import sim.field.grid.DDoubleGrid2D;

//import sim.engine.DistributedAgentQueueTest.*;

import mpi.*;

public class DistributedAgentQueue {

	// Neighbor / Remote migrate

	CartComm comm;
	int src_count[], dst_count[], neighbors[];

	HashMap<Integer, AgentOutputStream> serializeq;

	DDoubleGrid2D grid;
	SimState state;

	// for debug
	DistributedAgentQueueTest sim;

	// Neighbor count in topology
	static final int nc = 4;

	private class AgentOutputStream {
		ByteArrayOutputStream out;
		ObjectOutputStream os;

		public AgentOutputStream() throws IOException {
			reset();
		}

		public void write(Object obj) throws IOException {
			os.writeObject(obj);
		}

		public byte[] toByteArray() {
			return out.toByteArray();
		}

		public int size() {
			return out.size();
		}

		public void reset() throws IOException {
			if (os != null)
				os.close();
			if (out != null)
				out.close();
			out = new ByteArrayOutputStream();
			os = new ObjectOutputStream(out);
		}
	}

	public DistributedAgentQueue(DDoubleGrid2D grid, SimState state) throws MPIException, IOException {
		this.grid = grid;
		this.state = state;

		serializeq = new HashMap<Integer, AgentOutputStream>();
		neighbors = grid.getNeighborIds();
		for (int i : neighbors)
			serializeq.putIfAbsent(i, new AgentOutputStream());

		sim = (DistributedAgentQueueTest)state;

		// Corner neighbors
		int[] corners = grid.getCornerIds();
		for (int i = 0 ; i < nc; i++)
			serializeq.putIfAbsent(corners[i], serializeq.get(neighbors[i]));
	}

	public void setPos(final Steppable a, int x, int y) throws MPIException, IOException {
		// For debug
		sim.removeId(a);

		int dst = grid.toPartitionId(x, y);
		AgentWithPosition agent = new AgentWithPosition(a, x, y);

		// Local
		if (dst == grid.pid) {
			onRecv(agent);
			return;
		}

		// Remote
		assert serializeq.containsKey(dst);
		serializeq.get(dst).write(agent);
	}

	public void sync() throws MPIException, IOException, ClassNotFoundException {
		// Migrate twice since it need two steps for an agent to migrate to a diagnol neighbor.
		migrate();
		migrate();
	}

	public void migrate() throws MPIException, IOException, ClassNotFoundException {
		// Prepare data
		int[] src_count = new int[nc];
		int[] src_displ = new int[nc];
		int[] dst_count = new int[nc];
		int[] dst_displ = new int[nc];

		int total = 0;
		for (int i = 0; i < nc; i++) {
			src_count[i] = serializeq.get(neighbors[i]).size();
			src_displ[i] = total;
			total += src_count[i];
		}

		ByteArrayOutputStream objstream = new ByteArrayOutputStream();
		for (int i : neighbors)
			objstream.write(serializeq.get(i).toByteArray());
		byte[] sendbuf = objstream.toByteArray();

		// Two neighbor all-to-all call
		// First exchange count[] & displ[] of the agent object byte buffers
		grid.comm.neighborAllToAll(src_count, 1, MPI.INT, dst_count, 1, MPI.INT);

		total = 0;
		for (int i = 0; i < nc; i++) {
			dst_displ[i] = total;
			total += dst_count[i];
		}
		byte[] recvbuf = new byte[total];

		// Next is the to exchange the actual object bytes
		grid.comm.neighborAllToAllv(sendbuf, src_count, src_displ, MPI.BYTE, recvbuf, dst_count, dst_displ, MPI.BYTE);

		ArrayList<AgentWithPosition> migrated = new ArrayList<AgentWithPosition>();

		for (int i = 0; i < nc; i++) {
			ByteArrayInputStream in = new ByteArrayInputStream(Arrays.copyOfRange(recvbuf, dst_displ[i], dst_displ[i] + dst_count[i]));
			ObjectInputStream is = new ObjectInputStream(in);
			boolean more = true;
			while (more) {
				try {
					migrated.add((AgentWithPosition)is.readObject());
				} catch (EOFException e) {
					more = false;
				} 
			}
		}

		// Clear previous queues
		for (int i : neighbors)
			serializeq.get(i).reset();

		// handle incoming agents
		for (AgentWithPosition agent : migrated)
			onRecv(agent);
	}

	public void onRecv(AgentWithPosition a) throws MPIException, IOException {
		// Check dst != self
		// put back to migrate queue

		int dst = grid.toPartitionId(a.x, a.y);

		// if still remote
		if (dst != grid.pid) {
			assert serializeq.containsKey(dst);
			serializeq.get(dst).write(a);
			//setPos(a.obj, a.x, a.y);
			return;
		}

		// local
		// Added to local schedule
		state.schedule.scheduleOnce(a.obj, 1);

		// For debug
		sim.addId(a.obj);
	}
}