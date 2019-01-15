package sim.field;

import java.io.*;
import java.util.*;
import sim.util.*;
import ec.util.*;

import mpi.*;

public class DObjectMigrator implements Iterable<Object> {

	int nc; // number of direct neighbors
	int[] src_count, src_displ, dst_count, dst_displ;

	HashMap<Object, Integer> sendingCatelog;
	HashMap<Integer, Object> receivingCatelog;
	HashMap<Integer, AgentOutputStream> dstMap;
	AgentOutputStream[] outputStreams;

	DUniformPartition partition;

	public ArrayList<Object> objects;

	public static class AgentOutputStream {
		public ByteArrayOutputStream out;
		public ObjectOutputStream os;
 
		public AgentOutputStream() throws IOException {
			out = new ByteArrayOutputStream();
			os = new ObjectOutputStream(out);
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
		
		public void flush() throws IOException
		{
			os.flush();
		}

		public void reset() throws IOException {
			os.close();
			out.close();
			out = new ByteArrayOutputStream();
			os = new ObjectOutputStream(out);
		}
	}

	public DObjectMigrator(DUniformPartition partition) throws MPIException, IOException {
		this.partition = partition;
		nc = partition.dims.length * 2; // number of direct neighbors = |dims| * 2;

		objects = new ArrayList<Object>();

		src_count = new int[nc];
		src_displ = new int[nc];
		dst_count = new int[nc];
		dst_displ = new int[nc];

		// outputStreams for direct neighbors
		outputStreams = new AgentOutputStream[nc];
		for (int i = 0; i < nc; i++)
			outputStreams[i] = new AgentOutputStream();

		// direct neighbors
		dstMap = new HashMap<Integer, AgentOutputStream>();
		int[] neighbors = partition.getNeighborIds();
		for (int i = 0; i < nc; i++)
			dstMap.putIfAbsent(neighbors[i], outputStreams[i]);

		// extended neighbors
		// assuming the order of the result is like [-1, -1], [-1, 1], [1, -1], [1, 1]
		// first half send to neighbors[0] - [-1, 0]
		// second half send to neighbors[1] - [1, 0]
		int[] extended = partition.getExtendedNeighborIds(false);
		for (int i = 0 ; i < extended.length / 2; i++) {
			dstMap.putIfAbsent(extended[i], dstMap.get(neighbors[0]));
			dstMap.putIfAbsent(extended[i + extended.length / 2], dstMap.get(neighbors[1]));
		}
	}

	@Override
	public Iterator<Object> iterator() {
		return objects.iterator();
	}

	public int size() {
		return objects.size();
	}
	
	public void writeHeader(AgentOutputStream aos, MigratingAgent wrapper) throws IOException
	{
		String className = wrapper.wrappedAgent.getClass().getName();
		aos.os.writeObject(className);
		aos.os.writeInt(wrapper.destination);
		aos.os.writeBoolean(wrapper.migrate);
		aos.os.writeDouble(wrapper.loc.c[0]);
		aos.os.writeDouble(wrapper.loc.c[1]);
		aos.os.flush();
	}
	
	public MigratingAgent readHeader(ObjectInputStream is, String className) throws IOException
	{
		// read destination
		int dst = is.readInt();
		// read Wrapper data
		boolean migrate = is.readBoolean();
		double x = is.readDouble();
		double y = is.readDouble();
		// create the new agent
		SelfStreamedAgent newAgent = null;
		try
		{
			newAgent = (SelfStreamedAgent) Class.forName(className).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		// read in the data
		MigratingAgent wrapper = new MigratingAgent(dst, newAgent, new DoublePoint(x, y), migrate);
		return wrapper;
	}
	
	public void migrate(final Object obj, final int dst) {
		MigratingAgent wrapper = (MigratingAgent) obj;
		assert dstMap.containsKey(dst);
		try {
			if(wrapper.wrappedAgent instanceof SelfStreamedAgent)
			{
				// write header information, all agent has this info
				writeHeader(dstMap.get(dst), wrapper);
				// write agent
				((SelfStreamedAgent) wrapper.wrappedAgent).writeStream(dstMap.get(dst));
				// have to flush the data, in case user forget this step
				dstMap.get(dst).os.flush();
			}
			else {
				dstMap.get(dst).write(wrapper);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void sync() throws MPIException, IOException, ClassNotFoundException {
		
		// Migrate |dims| times since it need |dims| steps for an agent to migrate to a diagnol neighbor.
		for (int i = 0; i < partition.dims.length; i++)
			sync_step();
	}

	private void sync_step() throws MPIException, IOException, ClassNotFoundException {
		// Prepare data
		for (int i = 0, total = 0; i < nc; i++) {
			outputStreams[i].flush();
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

		// read and handle incoming objects
		ArrayList<MigratingAgent> bufferList = new ArrayList<MigratingAgent>();
		for (int i = 0; i < nc; i++) {
			ByteArrayInputStream in = new ByteArrayInputStream(Arrays.copyOfRange(recvbuf, dst_displ[i], dst_displ[i] + dst_count[i]));
			ObjectInputStream is = new ObjectInputStream(in);
			boolean more = true;
			while (more) {
				try {
					MigratingAgent wrapper = null;
					Object object = is.readObject();
					if (object instanceof String)
					{
						String className = (String)object;
						// return the wrapper with header information filled in
						wrapper = readHeader(is, className);
						((SelfStreamedAgent)wrapper.wrappedAgent).readStream(is);						
					}
					else {
						wrapper = (MigratingAgent)object;
					}
					if (partition.pid != wrapper.destination) {
						assert dstMap.containsKey(wrapper.destination);
						bufferList.add(wrapper);
					} else
						objects.add(wrapper);
				} catch (EOFException e) {
					more = false;
				}
			}
		}

		// Clear previous queues
		for (int i = 0; i < nc; i++)
			outputStreams[i].reset();

		// Handling the agent in bufferList
		for (int i = 0; i < bufferList.size(); ++i)
		{
			MigratingAgent wrapper = (MigratingAgent) bufferList.get(i);
			int dst = wrapper.destination;
			if(wrapper.wrappedAgent instanceof SelfStreamedAgent)
			{
				// write header information, all agent has this info
				writeHeader(dstMap.get(dst), wrapper);
				// write agent
				((SelfStreamedAgent) wrapper.wrappedAgent).writeStream(dstMap.get(dst));
				// have to flush the data, in case user forget this step
				dstMap.get(dst).os.flush();
			}
			else {
				dstMap.get(dst).write(wrapper);
			}
		}
		bufferList.clear();
	}
}