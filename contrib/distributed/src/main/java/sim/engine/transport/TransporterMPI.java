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

import mpi.Comm;
import mpi.MPI;
import mpi.MPIException;
import sim.engine.DSimState;
import sim.engine.DistributedIterativeRepeat;
import sim.engine.Stopping;
import sim.field.partitioning.Partition;
import sim.util.*;

/**
 * This class contains the methods for moving objects and agents between
 * processors.
 * 
 * In Distributed Mason moving objects from one processor to another is called
 * transportation and transporting agents is called migration.
 */
public class TransporterMPI
{
	int numNeighbors; // number of direct neighbors
	int[] src_count, src_displ, dst_count, dst_displ;

	HashMap<Integer, RemoteOutputStream> dstMap; // map of all neighboring partitions

	Partition partition;
	int[] neighbors;

	public ArrayList<PayloadWrapper> objectQueue; //things being moved are put here, and integrated into local storage in DSimState

	public TransporterMPI(final Partition partition)
	{
		this.partition = partition;
		reload();

		//unclear on exactly how this works, I assume it is just syncing before initializing?
		partition.registerPreCommit(arg ->
		{
			try
			{
				sync();
			}
			catch (MPIException | IOException | ClassNotFoundException e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
		});

		//unclear on exactly how this works, I assume it is just syncing before initializing?
		partition.registerPostCommit(arg ->
		{
			reload();
			try
			{
				sync();
			}
			catch (MPIException | IOException | ClassNotFoundException e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
		});
	}

	/**
	 * Initializes the objectQueue, sets variables like number of neighbors, source
	 * counts, destination counts etc. and creates the destination map for the
	 * neighbors.
	 * 
	 */
	public void reload()
	{
		// TODO cannot work with one node?
		neighbors = partition.getNeighborPIDs();
		numNeighbors = neighbors.length;

		objectQueue = new ArrayList<>(); //reset this when reloading

		src_count = new int[numNeighbors];
		src_displ = new int[numNeighbors];
		dst_count = new int[numNeighbors];
		dst_displ = new int[numNeighbors];

		// outputStreams for direct neighbors
		dstMap = new HashMap<Integer, RemoteOutputStream>();
		try
		{
			for (int i : neighbors)
				dstMap.putIfAbsent(i, new RemoteOutputStream()); //new streams
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public int size()
	{
		return objectQueue.size();
	}

	public void clear()
	{
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
	
	//do we need to do byte stuff here?  Or can we use MPIUtil neighbor to neighbor
	public void sync() throws MPIException, IOException, ClassNotFoundException
	{
		// Prepare data
		for (int i = 0, total = 0; i < numNeighbors; i++)
		{
			RemoteOutputStream outputStream = dstMap.get(neighbors[i]);
			outputStream.flush(); //writes to ObjectOutputStream and removes from this stream
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

		
		// TODO: we should not be calling MPI methods directly, this should be in MPIUtil 
		//Problem: How do we convert objstream to T[] sendObjs to call MPIUtil neighborAllToAll
		
		// First exchange count[] of the send byte buffers with neighbors so that we can
		// setup recvbuf
		partition.getCommunicator().neighborAllToAll(src_count, 1, MPI.INT, dst_count, 1, MPI.INT);

		for (int i = 0, total = 0; i < numNeighbors; i++)
		{
			dst_displ[i] = total;
			total += dst_count[i];
		}
		final ByteBuffer recvbuf = ByteBuffer.allocateDirect(dst_displ[numNeighbors - 1] + dst_count[numNeighbors - 1]);

		// exchange the actual object bytes
		partition.getCommunicator().neighborAllToAllv(sendbuf, src_count, src_displ, MPI.BYTE, recvbuf, dst_count,
				dst_displ, MPI.BYTE);

		
		// read and handle incoming objects

		for (int i = 0; i < numNeighbors; i++)
		{
			final byte[] data = new byte[dst_count[i]];
			recvbuf.position(dst_displ[i]);
			recvbuf.get(data);
			final ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(data));

			while (true)
			{
				try
				{
					final PayloadWrapper wrapper = (PayloadWrapper) inputStream.readObject();
					if (partition.getPID() != wrapper.destination)
					{
						System.err.println("This is not the correct processor");
						throw new RuntimeException("This is not the correct processor");

					}
					else
						objectQueue.add(wrapper);
				}
				catch (final EOFException e)
				{
					break;
				}
			}
		}

		// Clear previous queues
		for (int i : neighbors)
			dstMap.get(i).reset();
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
			final int fieldIndex)
	{
		AgentWrapper wrapper = new AgentWrapper(agent);
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
			final int fieldIndex)
	{
		AgentWrapper wrapper = new AgentWrapper(ordering, agent);
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
			final NumberND loc, final int fieldIndex)
	{
		AgentWrapper wrapper = new AgentWrapper(ordering, time, agent);
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
			final int fieldIndex)
	{
		// These methods differ in just the datatype of the WrappedObject
		transportObject(agentWrapper, dst, loc, fieldIndex);
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
			final NumberND loc, final int fieldIndex)
	{
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
			final int fieldIndex)
	{
		//shouldn't be calling this if local move
		if (partition.getPID() == dst)
			throw new IllegalArgumentException("Destination cannot be local, must be remote");


		// Wrap the agent, this is important because we want to keep track of
		// dst, which could be the diagonal processor
		final PayloadWrapper wrapper = new PayloadWrapper(dst, obj, loc, fieldIndex);

		if (DSimState.withRegistry)		
		DRegistry.getInstance().ifExportedThenAddMigratedName(obj);

		assert dstMap.containsKey(dst);
		try
		{
			dstMap.get(dst).write(wrapper);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Determines if the given partition is local to our partition
	 *
	 * @param loc        partition pid
	 *
	 * @throws IllegalArgumentException if destination (pid) is local
	 */
	public boolean isNeighbor(final int loc)
	{
		return dstMap.containsKey(loc);
	}

	public static class RemoteOutputStream
	{
		public ByteArrayOutputStream out;
		public ObjectOutputStream os;
		public ArrayList<Object> obj = new ArrayList<Object>();

		public RemoteOutputStream() throws IOException
		{
			out = new ByteArrayOutputStream();
			os = new ObjectOutputStream(out);
		}

		public void write(final Object obj) throws IOException
		{
			// virtual write really write only at the end of the simulation steps
			this.obj.add(obj);
		}

		public byte[] toByteArray() throws IOException
		{
			return out.toByteArray();
		}

		public int size()
		{
			return out.size();
		}

		public void flush() throws IOException
		{
			// write all objects
			for (Object o : obj)
				os.writeObject(o);
			os.flush();
		}

		public void reset() throws IOException
		{
			os.close();
			out.close();
			out = new ByteArrayOutputStream();
			os = new ObjectOutputStream(out);
			obj.clear();
		}
	}
}
