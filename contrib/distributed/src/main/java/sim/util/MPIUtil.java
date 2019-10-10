package sim.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import mpi.Comm;
import mpi.Datatype;
import mpi.MPI;
import mpi.MPIException;
import sim.field.DPartition;

// TODO: Reuse ByteBuffers to minimize allocate/de-allocate overhead
// TODO: Use ByteBuffer-back output/input streams - need to dynamically adjust the size of backing buffer.

// TODO: Point to point comm

// Utility class that serialize/exchange/deserialize objects using MPI
public class MPIUtil {

	// static final int MAX_SIZE = 1 << 30; // 1024MBytes
	static final int MAX_SIZE = 134217728; // 128 MB

	// Persistent send and recv direct buffers
	// Those two bufferes will be shared across multiple MPI calls
	// so that each call will not need to alloc/dealloc buffers individually
	// TODO
	// Such shared is not protected with locks or anything
	// so Calls to those MPI functions in a multi-threaded scenario will likely to
	// have race condition
	// Since currently there is no concurrent call implemented, this issue is left
	// as a TODO
	static ByteBuffer pSendBuf, pRecvBuf;

	/**
	 * Makes a ginormous and inappropriate buffer for sending. This needs to be
	 * fixed.
	 */
	static ByteBuffer initSendBuf() {
		if (MPIUtil.pSendBuf == null)
			MPIUtil.pSendBuf = ByteBuffer.allocateDirect(MPIUtil.MAX_SIZE);
		else
			MPIUtil.pSendBuf.clear();

		return MPIUtil.pSendBuf;
	}

	/**
	 * Makes a ginormous and inappropriate buffer for receiving. This needs to be
	 * fixed.
	 */

	static ByteBuffer initRecvBuf() {
		if (MPIUtil.pRecvBuf == null)
			MPIUtil.pRecvBuf = ByteBuffer.allocateDirect(MPIUtil.MAX_SIZE);
		else
			MPIUtil.pRecvBuf.clear();

		return MPIUtil.pRecvBuf;
	}

	// Serialize a Serializable using Java's builtin serialization and return the
	// ByteBuffer
	static void serialize(final Serializable obj, final ByteBuffer buf) {
		try (
				ByteBufferOutputStream out = new ByteBufferOutputStream(buf);
				ObjectOutputStream os = new ObjectOutputStream(out)) {
			os.writeObject(obj);
			os.flush(); /// SEAN QUESTION: If the try ALREADY closes the stream, why do we need to flush
						/// it?
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/// SEAN QUESTION: why are we reallocating ByteBufferOutputStream and
	/// ObjectOutputStream every single time?
	/// Why aren't we just writing all of the objects with the same ones and then
	/// flushing?

	// Serialize each Serializable in objs into buf.
	// The length of each byte array will be written into the count array
	static void serialize(final Serializable[] objs, final ByteBuffer buf, final int[] count) {
		for (int i = 0, prevPos = buf.position(); i < objs.length; i++) {
			serialize(objs[i], buf);
			count[i] = buf.position() - prevPos;
			prevPos = buf.position();
		}
	}

	// Deserialize the object of given type T that is stored in [pos, pos + len) in
	// buf
	static <T extends Serializable> T deserialize(final ByteBuffer buf, final int pos, final int len) {
		T obj = null;

		buf.position(pos);
		try (
				ByteBufferInputStream in = new ByteBufferInputStream(buf);
				ObjectInputStream is = new ObjectInputStream(in);) {
			obj = (T) is.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return obj;
	}

	static <T extends Serializable> T deserialize(final ByteBuffer buf, final int pos) {
		T obj = null;

		buf.position(pos);
		try (
				ByteBufferInputStream in = new ByteBufferInputStream(buf);
				ObjectInputStream is = new ObjectInputStream(in);) {
			obj = (T) is.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return obj;
	}

	//// SEAN QUESTION: Why are we using a lambda here? This is nuts, we should not
	//// be using lambdas in this context.
	//// Also, this looks like it's *terribly* inefficient.

	// Compute the displacement according to the count
	static int[] getDispl(final int[] count) {
		return IntStream.range(0, count.length)
				.map(x -> Arrays.stream(count).limit(x).sum())
				.toArray();
	}

	//// Broadcasts to everyone. In order to do this, all nodes must call bcast even
	//// if they're not root.
	public static <T extends Serializable> T bcast(final Comm comm, final T obj, final int root) throws MPIException {
		final int pid = comm.getRank();
		final ByteBuffer buf = initSendBuf();

		if (pid == root)
			serialize(obj, buf);

		final int[] numBytes = new int[] { buf.position() };
		comm.bcast(numBytes, 1, MPI.INT, root);
		comm.bcast(buf, numBytes[0], MPI.BYTE, root);

		return MPIUtil.<T>deserialize(buf, 0, numBytes[0]);
	}

	//// Broadcasts to everyone. In order to do this, all nodes must call bcast even
	//// if they're not root.
	public static <T extends Serializable> T bcast(final DPartition p, final T obj, final int root)
			throws MPIException {
		return MPIUtil.<T>bcast(p.getCommunicator(), obj, root);
	}

	// Used by init() in RemoteProxy() to broadcast the host ip address
	public static <T extends Serializable> T bcast(final T obj, final int root) throws MPIException {
		return MPIUtil.<T>bcast(MPI.COMM_WORLD, obj, root);
	}

	// Allows root to send UNIQUE messages to EACH node. Both root and the nodes
	// call this method.

	// Reverse of gather
	// Currently used by collectGroup() and distributedGroup() in HaloField
	public static <T extends Serializable> T scatter(final Comm comm, final T[] sendObjs, final int root)
			throws MPIException {
		final int pid = comm.getRank();
		final int np = comm.getSize();
		int dstCount;
		int[] srcDispl = null;
		final int[] srcCount = new int[np];

		final ByteBuffer srcBuf = initSendBuf(), dstBuf = initRecvBuf();

		if (pid == root) {
			serialize(sendObjs, srcBuf, srcCount);
			srcDispl = getDispl(srcCount);
		}

		comm.scatter(srcCount, 1, MPI.INT, root);
		dstCount = srcCount[0];

		comm.scatterv(srcBuf, srcCount, srcDispl, MPI.BYTE, dstBuf, dstCount, MPI.BYTE, root);

		return MPIUtil.<T>deserialize(dstBuf, 0, dstCount);
	}

	// Allows root to send UNIQUE messages to EACH node. Both root and the nodes
	// call this method.

	public static <T extends Serializable> T scatter(final DPartition p, final T[] sendObjs, final int root)
			throws MPIException {
		return MPIUtil.<T>scatter(p.getCommunicator(), sendObjs, root);
	}

	// TODO: can we use the same buffers for all operations?
	public static void bSend(final Comm comm, final Serializable sendObj, final int dst) throws MPIException {
		final ByteBuffer srcBuf = initSendBuf();

		serialize(sendObj, srcBuf);
		comm.bSend(srcBuf, 1, MPI.BYTE, dst, 0);
	}

	public static Serializable recv(final Comm comm, final int src) throws MPIException {
		final ByteBuffer dstBuf = initRecvBuf();

		// TODO: how to figure out the count?
		comm.recv(dstBuf, 1, MPI.BYTE, src, 0);
		return deserialize(dstBuf, 0);
	}

	// Allows each node to send one message each to the the root. These messages are
	// returned as an arraylist to the root.

	// Each LP sends the sendObj to dst
	// dst will return an ArrayList of np objects of type T
	// others will return an empty ArrayList
	// Currently used by collectGroup() and distributedGroup() in HaloField
	public static <T extends Serializable> ArrayList<T> gather(final Comm comm, final T sendObj, final int dst)
			throws MPIException {
		final int np = comm.getSize();
		final int pid = comm.getRank();
		int[] dstDispl;
		final int[] dstCount = new int[np];
		final ArrayList<T> recvObjs = new ArrayList<>();
		final ByteBuffer srcBuf = initSendBuf();
		final ByteBuffer dstBuf = initRecvBuf();

		serialize(sendObj, srcBuf);

		comm.gather(new int[] { srcBuf.position() }, 1, MPI.INT, dstCount, 1, MPI.INT, dst);
		dstDispl = getDispl(dstCount);

		comm.gatherv(srcBuf, srcBuf.position(), MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE, dst);

		if (pid == dst)
			for (int i = 0; i < np; i++)
				if (i == pid)
					recvObjs.add(sendObj);
				else
					recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));

		return recvObjs;
	}

	// Allows each node to send one message each to the the root. These messages are
	// returned as an arraylist to the root.

	public static <T extends Serializable> ArrayList<T> gather(final DPartition p, final T sendObj, final int dst)
			throws MPIException {
		return MPIUtil.<T>gather(p.getCommunicator(), sendObj, dst);
	}

	// Allows all nodes to broadcast to all nodes at once.

	// Each LP contributes the sendObj
	// All the LPs will receive all the sendObjs from all the LPs returned in the
	// ArrayList
	public static <T extends Serializable> ArrayList<T> allGather(final Comm comm, final T sendObj)
			throws MPIException {
		final int np = comm.getSize();
		final int pid = comm.getRank();
		int[] dstDispl;
		final int[] dstCount = new int[np];
		final ArrayList<T> recvObjs = new ArrayList<>();
		final ByteBuffer srcBuf = initSendBuf(), dstBuf = initRecvBuf();

		serialize(sendObj, srcBuf);
		dstCount[pid] = srcBuf.position();

		comm.allGather(dstCount, 1, MPI.INT);
		dstDispl = getDispl(dstCount);

		comm.allGatherv(srcBuf, srcBuf.position(), MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE);
		for (int i = 0; i < np; i++)
			if (i == pid)
				recvObjs.add(sendObj);
			else
				recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));

		return recvObjs;
	}

	// Allows all nodes to broadcast to all nodes at once.

	public static <T extends Serializable> ArrayList<T> allGather(final DPartition p, final T sendObj)
			throws MPIException {
		return MPIUtil.<T>allGather(p.getCommunicator(), sendObj);
	}

	//// SEAN QUESTION: Where is the topology specified?

	// Allows each node to send data to its neighbors as specified by a topology
	// simultaneously

	// Each LP sends and receives one object to/from each of its neighbors
	// in the order that is defined in partition scheme
	public static <T extends Serializable> ArrayList<T> neighborAllToAll(final Comm comm, final T[] sendObjs)
			throws MPIException {
		final int nc = sendObjs.length;
		int[] srcDispl;
		final int[] srcCount = new int[nc];
		int[] dstDispl;
		final int[] dstCount = new int[nc];
		final ArrayList<T> recvObjs = new ArrayList<>();
		final ByteBuffer srcBuf = initSendBuf(), dstBuf = initRecvBuf();

		serialize(sendObjs, srcBuf, srcCount);
		srcDispl = getDispl(srcCount);

		comm.neighborAllToAll(srcCount, 1, MPI.INT, dstCount, 1, MPI.INT);
		dstDispl = getDispl(dstCount);

		comm.neighborAllToAllv(srcBuf, srcCount, srcDispl, MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE);

		for (int i = 0; i < nc; i++)
			recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));

		return recvObjs;
	}

	// Allows each node to send data to its neighbors as specified by a topology
	// simultaneously

	public static <T extends Serializable> ArrayList<T> neighborAllToAll(final DPartition p, final T[] sendObjs)
			throws MPIException {
		return MPIUtil.<T>neighborAllToAll(p.getCommunicator(), sendObjs);
	}

	//// SEAN QUESTION: Why is this different from the others? Why do we not have
	//// serialization?
	//// Is this because we'd never use it?
	//// SEAN CONCERN: This is horribly inefficient, we're boxing and unboxing vals.
	//// Why not pass in
	//// The value as a byte, or int, or float, etc. BTW, we don't need float and
	//// long probably, just double and int.

	// Allows all nodes to broadcast to all nodes at once, but only neighbors will
	// receive a given node's message.

	// neighborAllGather for primitive type data (fixed length)
	public static Object neighborAllGather(final DPartition p, final Object val, final Datatype type)
			throws MPIException {
		final int nc = p.getNumNeighbors();
		Object sendBuf, recvBuf;

		// Use if-else since switch-case only accepts int
		if (type == MPI.BYTE) {
			sendBuf = new byte[] { (byte) val };
			recvBuf = new byte[nc];
		} else if (type == MPI.DOUBLE) {
			sendBuf = new double[] { (double) val };
			recvBuf = new double[nc];
		} else if (type == MPI.INT) {
			sendBuf = new int[] { (int) val };
			recvBuf = new int[nc];
		} else if (type == MPI.FLOAT) {
			sendBuf = new float[] { (float) val };
			recvBuf = new float[nc];
		} else if (type == MPI.LONG) {
			sendBuf = new long[] { (long) val };
			recvBuf = new long[nc];
		} else
			throw new UnsupportedOperationException(
					"The given MPI Datatype " + type + " is invalid / not implemented yet");

		p.getCommunicator().neighborAllGather(sendBuf, 1, type, recvBuf, 1, type);

		return recvBuf;
	}

	/*
	 * public static void main(String[] args) throws MPIException, IOException {
	 * MPI.Init(args);
	 *
	 * sim.field.DNonUniformPartition p =
	 * sim.field.DNonUniformPartition.getPartitionScheme(new int[] {10, 10}, true,
	 * new int[] {1, 1}); p.initUniformly(null); p.commit();
	 *
	 * int[] nids = p.getNeighborIds(); Integer[] t = new Integer[nids.length]; for
	 * (int i = 0; i < nids.length; i++) t[i] = p.getPid() * 10 + nids[i]; final int
	 * dst = 0;
	 *
	 * ArrayList<Integer[]> res = MPIUtil.<Integer[]>gather(p, t, dst);
	 *
	 * MPITest.execInOrder(x -> { System.out.println("gather to dst " + dst +
	 * " PID " + x); for (Integer[] r : res) for (Integer i : r)
	 * System.out.println(i); }, 100);
	 *
	 * Integer[] scattered = MPIUtil.<Integer[]>scatter(p, res.toArray(new
	 * Integer[0][]), dst);
	 *
	 * MPITest.execInOrder(x -> { System.out.println("scattered from src " + dst +
	 * " PID " + x); for (Integer i : scattered) System.out.println(i); }, 100);
	 *
	 * ArrayList<Integer[]> res2 = MPIUtil.<Integer[]>allGather(p, t);
	 *
	 * MPITest.execInOrder(x -> { System.out.println("allGather PID " + x); for
	 * (Integer[] r : res2) for (Integer i : r) System.out.println(i); }, 100);
	 *
	 * ArrayList<Integer> res3 = MPIUtil.<Integer>neighborAllToAll(p, t);
	 *
	 * MPITest.execInOrder(x -> { System.out.println("neighborAllToAll PID " + x);
	 * for (Integer i : res3) System.out.println(i); }, 100);
	 *
	 * int size = 50 * 1024 * 1024; // 50 x 4 MB (int array) MPITest.execOnlyIn(0, x
	 * -> { System.out.println("Large Dataset Test for scatterv"); });
	 *
	 * int[][] sendData = new int[p.getNumProc()][]; if (p.getPid() == dst) { for
	 * (int i = 0; i < p.getNumProc(); i++) { sendData[i] = new int[size];
	 * Arrays.fill(sendData[i], i + 1); } }
	 *
	 * int[] recvData = MPIUtil.<int[]>scatter(p, sendData, dst); for (int i = 0; i
	 * < size; i++) if (recvData[i] != p.getPid() + 1) {
	 * System.out.printf("PID %d VerifyError index %d want %d got %d\n", p.getPid(),
	 * i, p.getPid() + 1, recvData[i]); System.exit(-1); }
	 *
	 * MPITest.execOnlyIn(0, x -> { System.out.println("Pass"); });
	 *
	 * MPITest.execOnlyIn(0, x -> {
	 * System.out.println("Large Dataset Test for gatherv"); });
	 *
	 * int[] sendData2 = new int[size]; Arrays.fill(sendData2, p.getPid() + 1);
	 *
	 * ArrayList<int[]> recvData2 = MPIUtil.<int[]>gather(p, sendData2, dst); if
	 * (dst == p.getPid()) for (int i = 0; i < p.getNumProc(); i++) for (int j = 0;
	 * j < size; j++) if (recvData2.get(i)[j] != i + 1) {
	 * System.out.printf("VerifyError index [%d][%d] want %d got %d\n", i, j, i + 1,
	 * recvData2.get(i)[j]); System.exit(-1); }
	 *
	 * MPITest.execOnlyIn(0, x -> { System.out.println("Pass"); });
	 *
	 * MPI.Finalize(); }
	 */
}

class ByteBufferOutputStream extends OutputStream implements AutoCloseable {
	ByteBuffer buf;

	public ByteBufferOutputStream(final ByteBuffer buf) {
		if (buf == null)
			throw new IllegalArgumentException("The buffer provided is null!");

		this.buf = buf;
	}

	public void write(final int b) throws IOException {
		buf.put((byte) b);
	}

	public void write(final byte[] bytes, final int off, final int len) throws IOException {
		buf.put(bytes, off, len);
	}

	public void close() {
		try {
			super.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

class ByteBufferInputStream extends InputStream implements AutoCloseable {
	ByteBuffer buf;

	public ByteBufferInputStream(final ByteBuffer buf) {
		if (buf == null)
			throw new IllegalArgumentException("The buffer provided is null!");

		this.buf = buf;
	}

	public int read() throws IOException {
		if (!buf.hasRemaining()) {
			return -1;
		}
		return buf.get() & 0xFF;
	}

	public int read(final byte[] bytes, final int off, int len) throws IOException {
		if (!buf.hasRemaining()) {
			return -1;
		}

		len = Math.min(len, buf.remaining());
		buf.get(bytes, off, len);
		return len;
	}

	public void close() {
		try {
			super.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
