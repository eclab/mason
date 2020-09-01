package sim.engine.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import mpi.MPI;
import mpi.MPIException;
import sim.engine.DistributedIterativeRepeat;
import sim.engine.Stopping;
import sim.engine.registry.DRegistry;
import sim.field.partitioning.PartitionInterface;
import sim.util.*;

/**
 * This class contains the methods for moving objects and agents between
 * processors.
 * 
 * In Distributed Mason moving objects from one processor to another is called
 * transportation and transporting agents is called migration.
 */
public class TransporterMPI {

	int numNeighbors; // number of direct neighbors
	int[] src_count, src_displ, dst_count, dst_displ;

	HashMap<Integer, RemoteOutputStream> dstMap;

	PartitionInterface<?> partition;
	int[] neighbors;

	public ArrayList<PayloadWrapper> objectQueue;

	protected boolean withRegistry;

	public TransporterMPI(final PartitionInterface<?> partition) {
		this.partition = partition;
		this.withRegistry = false;
		reload();

		partition.registerPreCommit(arg -> {
			try {
				sync();
			} catch (MPIException | IOException | ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		});

		partition.registerPostCommit(arg -> {
			reload();
			try {
				sync();
			} catch (MPIException | IOException | ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		});
	}

	public void reload() {
		// TODO cannot work with one node?
		neighbors = partition.getNeighborIds();
		numNeighbors = neighbors.length;

		objectQueue = new ArrayList<>();

		src_count = new int[numNeighbors];
		src_displ = new int[numNeighbors];
		dst_count = new int[numNeighbors];
		dst_displ = new int[numNeighbors];

		// outputStreams for direct neighbors
		dstMap = new HashMap<Integer, RemoteOutputStream>();
		try {
			for (int i : neighbors)
				dstMap.putIfAbsent(i, new RemoteOutputStream());
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public int size() {
		return objectQueue.size();
	}

	public void clear() {
		objectQueue.clear();
	}

	/**
	 * Send/receive all objects and agents. All objects are added to their
	 * respective fields and agents are also scheduled on top of that.
	 * 
	 * @throws MPIException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void sync() throws MPIException, IOException, ClassNotFoundException {
		// Prepare data
		for (int i = 0, total = 0; i < numNeighbors; i++) {
			RemoteOutputStream outputStream = dstMap.get(neighbors[i]);
			outputStream.flush();
			src_count[i] = outputStream.size();
			src_displ[i] = total;
			total += src_count[i];
		}

		// Concat neighbor streams into one
		final ByteArrayOutputStream objstream = new ByteArrayOutputStream();
		for (int i : neighbors)
			objstream.write(dstMap.get(i).toByteArray());

		final ByteBuffer sendbuf = ByteBuffer.allocateDirect(objstream.size());
		sendbuf.put(objstream.toByteArray()).flip();

		// First exchange count[] of the send byte buffers with neighbors so that we can
		// setup recvbuf
		partition.getCommunicator().neighborAllToAll(src_count, 1, MPI.INT, dst_count, 1, MPI.INT);

		for (int i = 0, total = 0; i < numNeighbors; i++) {
			dst_displ[i] = total;
			total += dst_count[i];
		}
		final ByteBuffer recvbuf = ByteBuffer.allocateDirect(dst_displ[numNeighbors - 1] + dst_count[numNeighbors - 1]);

		// exchange the actual object bytes
		partition.getCommunicator().neighborAllToAllv(sendbuf, src_count, src_displ, MPI.BYTE, recvbuf, dst_count,
				dst_displ, MPI.BYTE);

		// read and handle incoming objects
//		final ArrayList<PayloadWrapper> bufferList = new ArrayList<>();
		for (int i = 0; i < numNeighbors; i++) {
			final byte[] data = new byte[dst_count[i]];
			recvbuf.position(dst_displ[i]);
			recvbuf.get(data);
			final ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(data));

			while (true) {
				try {
					final PayloadWrapper wrapper = (PayloadWrapper) inputStream.readObject();
					if (partition.pid != wrapper.destination) {
						System.err.println("This is not the correct processor");
						throw new RuntimeException("This is not the correct processor");
//						assert dstMap.containsKey(wrapper.destination);
//						bufferList.add(wrapper);
					} else
						objectQueue.add(wrapper);
				} catch (final EOFException e) {
					break;
				}
			}
//			while (true) {
//			try {
//				Transportee<? extends Object> wrapper = null;
//				Object object = is.readObject();
//				if (object instanceof String) {
//					String className = (String) object;
//					// return the wrapper with header information filled in
//					wrapper = readHeader(is, className);
//					((SelfStreamedAgent) wrapper.wrappedObject).readStream(is);
//				} else {
//				wrapper = (Transportee) object;
//				}
		}

		// System.out.println("PID "+MPI.COMM_WORLD.getRank()+" objectQueue
		// "+objectQueue);

		// Clear previous queues
		for (int i : neighbors)
			dstMap.get(i).reset();

		// Handling the agent in bufferList
//		for (final PayloadWrapper wrapper : bufferList)
//			dstMap.get(wrapper.destination).write(wrapper);
//		bufferList.clear();

//		for (int i = 0; i < bufferList.size(); ++i) {
//			Transportee<? extends Object> wrapper = bufferList.get(i);
//			int dst = wrapper.destination;
//			if (wrapper.wrappedObject instanceof SelfStreamedAgent) {
//				// write header information, all agent has this info
//				writeHeader(dstMap.get(dst), wrapper);
//				// write agent
//				((SelfStreamedAgent) wrapper.wrappedObject).writeStream(dstMap.get(dst));
//				// have to flush the data, in case user forget this step
//				dstMap.get(dst).os.flush();
//			} else {
//			dstMap.get(dst).write(wrapper);
//			}
//		}
//		bufferList.clear();
	}

	/**
	 * Does not transport the Object, only migrates it
	 *
	 * @param agent
	 * @param dst   destination pId
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateAgent(final Stopping agent, final int dst) {

		AgentWrapper wrapper = new AgentWrapper(agent);

		if (withRegistry) {
			if (DRegistry.getInstance().isExported(agent)) {
				wrapper.setExportedName(DRegistry.getInstance().getLocalExportedName(agent));
				DRegistry.getInstance().addMigratedName(agent);
			}
		}

		migrateAgent(wrapper, dst);
	}

	/**
	 * Does not transport the Object, only migrates it
	 *
	 * @param ordering
	 * @param agent
	 * @param dst      destination pId
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateAgent(final int ordering, final Stopping agent, final int dst) {
		AgentWrapper wrapper = new AgentWrapper(ordering, agent);

		if (withRegistry) {
			if (DRegistry.getInstance().isExported(agent)) {
				wrapper.setExportedName(DRegistry.getInstance().getLocalExportedName(agent));
				DRegistry.getInstance().addMigratedName(agent);
			}
		}

		migrateAgent(wrapper, dst);
	}

	/**
	 * Does not transport the Object, only migrates it
	 *
	 * @param ordering
	 * @param time
	 * @param agent
	 * @param dst      destination pId
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateAgent(final int ordering, final double time, final Stopping agent, final int dst) {
		AgentWrapper wrapper = new AgentWrapper(ordering, time, agent);

		if (withRegistry) {
			if (DRegistry.getInstance().isExported(agent)) {
				wrapper.setExportedName(DRegistry.getInstance().getLocalExportedName(agent));
				DRegistry.getInstance().addMigratedName(agent);
			}
		}

		migrateAgent(wrapper, dst);
	}

	/**
	 * Internal method. Don't use AgentWrapper, use Stopping instead <br>
	 * Does not transport the Object, only migrates it
	 *
	 * @param agentWrapper
	 * @param dst          destination pId
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateAgent(final AgentWrapper agentWrapper, final int dst) {
		// If fieldIndex < 0 then the payload does not need to be transported
		migrateAgent(agentWrapper, dst, null, -1);
	}

	/**
	 * Transports the Object as well as migrates it
	 *
	 * @param agent
	 * @param dst        destination pId
	 * @param loc
	 * @param fieldIndex
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateAgent(final Stopping agent, final int dst, final NumberND loc,
			final int fieldIndex) {
		AgentWrapper wrapper = new AgentWrapper(agent);

		if (withRegistry) {
			if (DRegistry.getInstance().isExported(agent)) {
				wrapper.setExportedName(DRegistry.getInstance().getLocalExportedName(agent));
				DRegistry.getInstance().addMigratedName(agent);
			}
		}

		migrateAgent(wrapper, dst, loc, fieldIndex);
	}

	/**
	 * Transports the Object as well as migrates it
	 *
	 * @param ordering
	 * @param agent
	 * @param dst        destination pId
	 * @param loc
	 * @param fieldIndex
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateAgent(final int ordering, final Stopping agent, final int dst, final NumberND loc,
			final int fieldIndex) {
		AgentWrapper wrapper = new AgentWrapper(ordering, agent);

		if (withRegistry) {
			if (DRegistry.getInstance().isExported(agent)) {
				wrapper.setExportedName(DRegistry.getInstance().getLocalExportedName(agent));
				DRegistry.getInstance().addMigratedName(agent);
			}
		}

		migrateAgent(wrapper, dst, loc, fieldIndex);
	}

	/**
	 * Transports the Object as well as migrates it
	 *
	 * @param ordering
	 * @param time
	 * @param agent
	 * @param dst        destination pId
	 * @param loc
	 * @param fieldIndex
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateAgent(final int ordering, final double time, final Stopping agent, final int dst,
			final NumberND loc, final int fieldIndex) {
		AgentWrapper wrapper = new AgentWrapper(ordering, time, agent);

		if (withRegistry) {
			if (DRegistry.getInstance().isExported(agent)) {
				wrapper.setExportedName(DRegistry.getInstance().getLocalExportedName(agent));
				DRegistry.getInstance().addMigratedName(agent);
			}
		}

		migrateAgent(wrapper, dst, loc, fieldIndex);
	}

	/**
	 * Transports the Object as well as migrates it
	 *
	 * @param agentWrapper
	 * @param dst          destination pId
	 * @param loc
	 * @param fieldIndex
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateAgent(final AgentWrapper agentWrapper, final int dst, final NumberND loc,
			final int fieldIndex) {
		// These methods differ in just the datatype of the WrappedObject
		transportObject(agentWrapper, dst, loc, fieldIndex);
	}

	/**
	 * Does not transport the Object, only migrates it
	 *
	 * @param iterativeRepeat
	 * @param dst             destination pId
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateRepeatingAgent(final DistributedIterativeRepeat iterativeRepeat, final int dst) {
		// If fieldIndex < 0 then the payload does not need to be transported
		migrateRepeatingAgent(iterativeRepeat, dst, null, -1);
	}

	/**
	 * Transports the Object as well as migrates it. Does not stop() the repeating
	 * object. Thus, call stop on iterativeRepeat after calling this function
	 *
	 * @param iterativeRepeat
	 * @param dst             destination pId
	 * @param loc
	 * @param fieldIndex
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void migrateRepeatingAgent(final DistributedIterativeRepeat iterativeRepeat, final int dst,
			final NumberND loc,
			final int fieldIndex) {

		// TODO: do we need to synchronize something to ensure that the stoppable is
		// stopped before we transport?

		// These methods differ in just the datatype of the WrappedObject
		transportObject(iterativeRepeat, dst, loc, fieldIndex);
	}

	/**
	 * Transports the Object but doesn't schedule it. Does not stop() the repeating
	 * object. Thus, call stop on iterativeRepeat after calling this function
	 *
	 * @param obj        Object to be transported
	 * @param dst        destination pId
	 * @param loc
	 * @param fieldIndex
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public void transportObject(final Serializable obj, final int dst, final NumberND loc,
			final int fieldIndex) {
		if (partition.pid == dst)
			throw new IllegalArgumentException("Destination cannot be local, must be remote");

		System.out.println("transporting: " + obj); //added by Raj Patel (see email from rlather 7/31/2020)
		
		
		// Wrap the agent, this is important because we want to keep track of
		// dst, which could be the diagonal processor
		final PayloadWrapper wrapper = new PayloadWrapper(dst, obj, loc, fieldIndex);

		if (withRegistry) {
			if (DRegistry.getInstance().isExported(obj)) {
				wrapper.setExportedName(DRegistry.getInstance().getLocalExportedName(obj));
				DRegistry.getInstance().addMigratedName(obj);
			}
		}

		assert dstMap.containsKey(dst);
		try {
			dstMap.get(dst).write(wrapper);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static class RemoteOutputStream {
		public ByteArrayOutputStream out;
		public ObjectOutputStream os;
		public ArrayList<Object> obj = new ArrayList<Object>();

		public RemoteOutputStream() throws IOException {
			out = new ByteArrayOutputStream();
			os = new ObjectOutputStream(out);
		}

		public void write(final Object obj) throws IOException {
			// os.writeObject(obj);
			// virtual write really write only at the end of the simulation steps
			this.obj.add(obj);
		}

		public byte[] toByteArray() throws IOException {

			return out.toByteArray();
		}

		public int size() {
			return out.size();
		}

		public void flush() throws IOException {
			// write all objects
			for (Object o : obj)
				os.writeObject(o);
			os.flush();
		}

		public void reset() throws IOException {
			os.close();
			out.close();
			out = new ByteArrayOutputStream();
			os = new ObjectOutputStream(out);
			obj.clear();
		}
	}

//	public void writeHeader(RemoteOutputStream aos, Transportee wrapper) throws IOException {
//		String className = wrapper.wrappedObject.getClass().getName();
//		aos.os.writeObject(className);
//		aos.os.writeInt(wrapper.destination);
//		aos.os.writeBoolean(wrapper.migrate);
//		aos.os.writeDouble(wrapper.loc.c[0]);
//		aos.os.writeDouble(wrapper.loc.c[1]);
//		aos.os.flush();
//	}
//
//	public Transportee readHeader(ObjectInputStream is, String className) throws IOException {
//		// read destination
//		int dst = is.readInt();
//		// read Wrapper data
//		boolean migrate = is.readBoolean();
//		double x = is.readDouble();
//		double y = is.readDouble();
//		// create the new agent
//		SelfStreamedAgent newAgent = null;
//		try {
//			newAgent = (SelfStreamedAgent) Class.forName(className).newInstance();
//		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//			e.printStackTrace();
//		}
//		// read in the data
//		Transportee wrapper = new Transportee(dst, newAgent, new Double2D(x, y), migrate);
//		return wrapper;
//	}

}
