package sim.engine.mpi;

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
import sim.field.partitioning.Partition;
import sim.util.*;
import sim.engine.*;

/**
 * This class contains the methods for moving objects and agents between
 * processors.
 * 
 * In Distributed Mason moving objects from one processor to another is called
 * transportation and transporting agents is called migration.
 */
public class Transporter
{
	int numNeighbors; // number of direct neighbors
	int[] srcCount, srcDispl, dstCount, distDispl;

	HashMap<Integer, RemoteOutputStream> dstMap; // map of all neighboring partitions

	Partition partition;
	int[] neighbors;

	public ArrayList<PayloadWrapper> objectQueue; // things being moved are put here, and integrated into local storage in DSimState

	// protected boolean withRegistry;

	public Transporter(Partition partition)
	{
		this.partition = partition;
		// this.withRegistry = false;
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

		srcCount = new int[numNeighbors];
		srcDispl = new int[numNeighbors];
		dstCount = new int[numNeighbors];
		distDispl = new int[numNeighbors];

		// outputStreams for direct neighbors
		dstMap = new HashMap<Integer, RemoteOutputStream>();
		try
		{
			for (int i : neighbors)
				dstMap.putIfAbsent(i, new RemoteOutputStream()); //new streams
		}
		catch (IOException e)
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
			srcCount[i] = outputStream.size();
			srcDispl[i] = total;
			total += srcCount[i];
		}

		// Concat neighbor streams into one
		ByteArrayOutputStream objstream = new ByteArrayOutputStream();
		for (int i : neighbors)
			objstream.write(dstMap.get(i).toByteArray());

		ByteBuffer sendbuf = ByteBuffer.allocateDirect(objstream.size());
		sendbuf.put(objstream.toByteArray()).flip();

		
		// TODO: we should not be calling MPI methods directly, this should be in MPIUtil 
		//Problem: How do we convert objstream to T[] sendObjs to call MPIUtil neighborAllToAll
		
		// First exchange count[] of the send byte buffers with neighbors so that we can
		// setup recvbuf
		partition.getCommunicator().neighborAllToAll(srcCount, 1, MPI.INT, dstCount, 1, MPI.INT);

		for (int i = 0, total = 0; i < numNeighbors; i++)
		{
			distDispl[i] = total;
			total += dstCount[i];
		}
		ByteBuffer recvbuf = ByteBuffer.allocateDirect(distDispl[numNeighbors - 1] + dstCount[numNeighbors - 1]);

		// exchange the actual object bytes
		partition.getCommunicator().neighborAllToAllv(sendbuf, srcCount, srcDispl, MPI.BYTE, recvbuf, dstCount, distDispl, MPI.BYTE);
		
		// read and handle incoming objects
		for (int i = 0; i < numNeighbors; i++)
		{
			byte[] data = new byte[dstCount[i]];
			recvbuf.position(distDispl[i]);
			recvbuf.get(data);
			ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(data));

			while (true)
			{
				try
				{
					PayloadWrapper wrapper = (PayloadWrapper) inputStream.readObject();
					if (partition.getPID() != wrapper.destination)
					{
						throw new RuntimeException("This is not the correct processor");
					}
					else
						objectQueue.add(wrapper);
				}
				catch (EOFException e)
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
	 * Transports the Object but doesn't schedule it. Does not stop() the repeating
	 * object. Thus, call stop on iterativeRepeat after calling this function
	 */
	public void transport(Serializable a, int dst, Number2D loc, int fieldIndex)
	{
		transport(a, dst, loc, fieldIndex, PayloadWrapper.NON_AGENT_ORDERING, PayloadWrapper.NON_AGENT_TIME, PayloadWrapper.NON_REPEATING_INTERVAL);
	}

	/**
	 * Transports the (non-repeating) Agent as well as migrates it.
	 */
	public void transport(Serializable obj, int dst, Number2D loc, int fieldIndex, int ordering, double time)
	{
		transport(obj, dst, loc, fieldIndex, ordering, time, PayloadWrapper.NON_REPEATING_INTERVAL);
	}
	
	/**
	 * Transports the repeating Agent as well as migrates it
	 */
	public void transport(Serializable obj, int dst, Number2D loc, int fieldIndex, int ordering, double time, double interval)
	{
		//shouldn't be calling this if local move
		if (partition.getPID() == dst)
			throw new IllegalArgumentException("Destination cannot be local, must be remote");

		// Wrap the agent, this is important because we want to keep track of
		// dst, which could be the diagonal processor
		PayloadWrapper wrapper = new PayloadWrapper(obj, dst, loc, fieldIndex, ordering, time, interval);

		// if (withRegistry)
		// {
			// String name = 
			// check if the agent is exported, if so add it to the migrated group
			// therefore the DSimState can unregister it
			if (obj instanceof Distinguished){
					DistinguishedRegistry.getInstance().
						ifExportedThenAddMigratedName((Distinguished) obj);
			}
	
			// if (name != null)
			// obj.distinguishedName(name);
		// }

		assert dstMap.containsKey(dst);
		try
		{
			dstMap.get(dst).write(wrapper);
		}
		catch (Exception e)
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
	public boolean isNeighbor(int loc)
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

		public void write(Object obj) throws IOException
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
