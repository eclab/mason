package sim.engine;

import java.io.*;
import java.util.*;

import sim.util.*;
import ec.util.*;
import sim.field.DUniformPartition;

import mpi.*;

public class DistributedAgentQueue {

	int nc; // number of direct neighbors
	int[] src_count, src_displ, dst_count, dst_displ;

	CartComm comm;

	HashMap<Integer, AgentOutputStream> agentOutput;
	AgentOutputStream[] outputStreams;

	DUniformPartition partition;
	SimState state;

	// for debug
	DistributedAgentQueueTest sim;

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

	public DistributedAgentQueue(DUniformPartition partition, SimState state) throws MPIException, IOException {
		this.partition = partition;
		this.state = state;
		nc = partition.dims.length * 2; // number of direct neighbors = |dims| * 2;

		src_count = new int[nc];
		src_displ = new int[nc];
		dst_count = new int[nc];
		dst_displ = new int[nc];

		// outputStreams for direct neighbors
		outputStreams = new AgentOutputStream[nc];
		for (int i = 0; i < nc; i++)
			outputStreams[i] = new AgentOutputStream();

		// direct neighbors
		agentOutput = new HashMap<Integer, AgentOutputStream>();
		int[] neighbors = partition.getNeighborIds();
		for (int i = 0; i < nc; i++)
			agentOutput.putIfAbsent(neighbors[i], outputStreams[i]);

		// extended neighbors
		// assuming the order of the result is like [-1, -1], [-1, 1], [1, -1], [1, 1]
		// first half send to neighbors[0] - [-1, 0]
		// second half send to neighbors[1] - [1, 0]
		int[] extended = partition.getExtendedNeighborIds(false);
		for (int i = 0 ; i < extended.length / 2; i++) {
			agentOutput.putIfAbsent(extended[i], agentOutput.get(neighbors[0]));
			agentOutput.putIfAbsent(extended[i + extended.length / 2], agentOutput.get(neighbors[1]));
		}

		// For debug
		sim = (DistributedAgentQueueTest)state;
	}

	public void setPos(final Steppable a, int x, int y) throws MPIException, IOException {
		// For debug
		sim.removeId(a);

		int dst = partition.toPartitionId(new int[]{x, y});
		AgentWithPosition agent = new AgentWithPosition(a, x, y);

		if (dst == partition.pid) { // Local
			onRecv(agent);
			return;
		}

		assert agentOutput.containsKey(dst); // Remote
		agentOutput.get(dst).write(agent);
	}

	public void sync() throws MPIException, IOException, ClassNotFoundException {
		// Migrate |dims| times since it need |dims| steps for an agent to migrate to a diagnol neighbor.
		for (int i = 0; i < partition.dims.length; i++)
			migrate();
	}

	private void migrate() throws MPIException, IOException, ClassNotFoundException {
		// Prepare data
		for (int i = 0, total = 0; i < nc; i++) {
			src_count[i] = outputStreams[i].size();
			src_displ[i] = total;
			total += src_count[i];
		}

		// Concat neighbor streams into one
		ByteArrayOutputStream objstream = new ByteArrayOutputStream();
		for (int i = 0; i < nc; i++) 
			objstream.write(outputStreams[i].toByteArray());
		byte[] sendbuf = objstream.toByteArray();

		// First exchange count[] of the send byte buffers with neighbors so that we can setup recvbuf
		partition.comm.neighborAllToAll(src_count, 1, MPI.INT, dst_count, 1, MPI.INT);
		for (int i = 0, total = 0; i < nc; i++) {
			dst_displ[i] = total;
			total += dst_count[i];
		}
		byte[] recvbuf = new byte[dst_displ[nc - 1] + dst_count[nc - 1]];

		// exchange the actual object bytes
		partition.comm.neighborAllToAllv(sendbuf, src_count, src_displ, MPI.BYTE, recvbuf, dst_count, dst_displ, MPI.BYTE);

		// read and handle incoming agents
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
		for (int i = 0; i < nc; i++) 
			outputStreams[i].reset();

		// handle incoming agents
		for (AgentWithPosition agent : migrated)
			onRecv(agent);
	}

	private void onRecv(AgentWithPosition a) throws MPIException, IOException {
		int dst = partition.toPartitionId(new int[]{a.x, a.y});

		// if still remote
		if (dst != partition.pid) {
			setPos(a.obj, a.x, a.y);
			return;
		}

		// local - Added to local schedule
		state.schedule.scheduleOnce(a.obj, 1);

		// For debug
		sim.addId(a.obj);
	}
}