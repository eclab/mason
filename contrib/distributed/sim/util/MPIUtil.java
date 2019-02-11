package sim.util;

import java.io.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import mpi.*;

import sim.field.DPartition;

//TODO Reuse ByteBuffers to minimize allocate/de-allocate overhead
//TODO Use ByteBuffer-back output/input streams - need to dynamically adjust the size of backing buffer.

// Utility class that serialize/exchange/deserialize objects using MPI
public class MPIUtil {

	private static final int MAX_SIZE = 1 << 30; // 1024MBytes

	// Persistent send and recv direct buffers
	// Those two bufferes will be shared across multiple MPI calls 
	// so that each call will not need to alloc/dealloc buffers individually
	// TODO
	// Such shared is not protected with locks or anything
	// so Calls to those MPI functions in a multi-threaded scenario will likely to have race condition
	// Since currently there is no concurrent call implemented, this issue is left as a TODO
	private static ByteBuffer pSendBuf, pRecvBuf;

	private static ByteBuffer initSendBuf() {
		if (pSendBuf == null)
			pSendBuf = ByteBuffer.allocateDirect(MAX_SIZE);
		else
			pSendBuf.clear();

		return pSendBuf;
	}

	private static ByteBuffer initRecvBuf() {
		if (pRecvBuf == null)
			pRecvBuf = ByteBuffer.allocateDirect(MAX_SIZE);
		else
			pRecvBuf.clear();

		return pRecvBuf;
	}

	// Serialize a Serializable using Java's builtin serialization and return the ByteBuffer
	private static void serialize(Serializable obj, ByteBuffer buf) {
		try (
			    ByteBufferOutputStream out = new ByteBufferOutputStream(buf);
			    ObjectOutputStream os = new ObjectOutputStream(out)
			) {
			os.writeObject(obj);
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// Serialize each Serializable in objs using Java's builtin serialization
	// Concatenate their individual ByteBuffer together and return the final ByteBuffer
	// The length of each byte array will be writtern into the count array
	private static void serialize(Serializable[] objs, ByteBuffer buf, int[] count) {
		for (int i = 0, prevPos = buf.position(); i < objs.length; i++) {
			serialize(objs[i], buf);
			count[i] = buf.position() - prevPos;
			prevPos = buf.position();
		}
	}

	// Deserialize the object of given type T that is stored in [pos, pos + len) in buf
	private static <T extends Serializable> T deserialize(ByteBuffer buf, int pos, int len) {
		T obj = null;

		buf.position(pos);
		try (
			    ByteBufferInputStream in = new ByteBufferInputStream(buf);
			    ObjectInputStream is = new ObjectInputStream(in);
			) {
			obj = (T)is.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return obj;
	}

	// Compute the displacement according to the count
	private static int[] getDispl(int[] count) {
		return IntStream.range(0, count.length)
		       .map(x -> Arrays.stream(count).limit(x).sum())
		       .toArray();
	}

	public static <T extends Serializable> T bcast(Comm comm, T obj, int root) throws MPIException {
		int pid = comm.getRank();
		ByteBuffer buf = initSendBuf();

		if (pid == root)
			serialize(obj, buf);

		int[] count = new int[] {buf.position()};
		comm.bcast(count, 1, MPI.INT, root);

		comm.bcast(buf, count[0], MPI.BYTE, root);

		return MPIUtil.<T>deserialize(buf, 0, count[0]);
	}

	public static <T extends Serializable> T bcast(DPartition p, T obj, int root) throws MPIException {
		return MPIUtil.<T>bcast(p.getCommunicator(), obj, root);
	}

	// Used by init() in RemoteProxy() to broadcast the host ip address
	public static <T extends Serializable> T bcast(T obj, int root) throws MPIException {
		return MPIUtil.<T>bcast(MPI.COMM_WORLD, obj, root);
	}

	// Reverse of gather
	// Currently used by collectGroup() and distributedGroup() in HaloField
	public static <T extends Serializable> T scatter(Comm comm, T[] sendObjs, int root) throws MPIException {
		int pid =  comm.getRank(), np =  comm.getSize(), dstCount;
		int[] srcDispl = null, srcCount = new int[np];
		ByteBuffer srcBuf = initSendBuf(), dstBuf = initRecvBuf();

		if (pid == root) {
			serialize(sendObjs, srcBuf, srcCount);
			srcDispl = getDispl(srcCount);
		}

		comm.scatter(srcCount, 1, MPI.INT, root);
		dstCount = srcCount[0];

		comm.scatterv(srcBuf, srcCount, srcDispl, MPI.BYTE, dstBuf, dstCount, MPI.BYTE, root);

		return MPIUtil.<T>deserialize(dstBuf, 0, dstCount);
	}

	public static <T extends Serializable> T scatter(DPartition p, T[] sendObjs, int root) throws MPIException {
		return MPIUtil.<T>scatter(p.getCommunicator(), sendObjs, root);
	}

	// Each LP sends the sendObj to dst
	// dst will return an ArrayList of np objects of type T
	// others will return an empty ArrayList
	// Currently used by collectGroup() and distributedGroup() in HaloField
	public static <T extends Serializable> ArrayList<T> gather(Comm comm, T sendObj, int dst) throws MPIException {
		int np = comm.getSize();
		int pid = comm.getRank();
		int[] dstDispl, dstCount = new int[np];
		ArrayList<T> recvObjs = new ArrayList();
		ByteBuffer srcBuf = initSendBuf(), dstBuf = initRecvBuf();

		serialize(sendObj, srcBuf);

		comm.gather(new int[] {srcBuf.position()}, 1, MPI.INT, dstCount, 1, MPI.INT, dst);
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

	public static <T extends Serializable> ArrayList<T> gather(DPartition p, T sendObj, int dst) throws MPIException {
		return MPIUtil.<T>gather(p.getCommunicator(), sendObj, dst);
	}

	// Each LP contributes the sendObj
	// All the LPs will receive all the sendObjs from all the LPs returned in the ArrayList
	public static <T extends Serializable> ArrayList<T> allGather(Comm comm, T sendObj) throws MPIException {
		int np = comm.getSize();
		int pid = comm.getRank();
		int[] dstDispl, dstCount = new int[np];
		ArrayList<T> recvObjs = new ArrayList();
		ByteBuffer srcBuf = initSendBuf(), dstBuf = initRecvBuf();

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

	public static <T extends Serializable> ArrayList<T> allGather(DPartition p, T sendObj) throws MPIException {
		return MPIUtil.<T>allGather(p.getCommunicator(), sendObj);
	}

	// Each LP sends and receives one object to/from each of its neighbors
	// in the order that is defined in partition scheme
	public static <T extends Serializable> ArrayList<T> neighborAllToAll(Comm comm, T[] sendObjs) throws MPIException {
		int nc = sendObjs.length;
		int[] srcDispl, srcCount = new int[nc];
		int[] dstDispl, dstCount = new int[nc];
		ArrayList<T> recvObjs = new ArrayList();
		ByteBuffer srcBuf = initSendBuf(), dstBuf = initRecvBuf();

		serialize(sendObjs, srcBuf, srcCount);
		srcDispl = getDispl(srcCount);

		comm.neighborAllToAll(srcCount, 1, MPI.INT, dstCount, 1, MPI.INT);
		dstDispl = getDispl(dstCount);

		comm.neighborAllToAllv(srcBuf, srcCount, srcDispl, MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE);

		for (int i = 0; i < nc; i++)
			recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));

		return recvObjs;
	}

	public static <T extends Serializable> ArrayList<T> neighborAllToAll(DPartition p, T[] sendObjs) throws MPIException {
		return MPIUtil.<T>neighborAllToAll(p.getCommunicator(), sendObjs);
	}

	// neighborAllGather for primitive type data (fixed length)
	public static Object neighborAllGather(DPartition p, Object val, Datatype type) throws MPIException {
		int nc = p.getNumNeighbors();
		Object sendBuf, recvBuf;

		// Use if-else since switch-case only accepts int
		if (type == MPI.BYTE) {
			sendBuf = new byte[] {(byte)val};
			recvBuf = new byte[nc];
		} else if (type == MPI.DOUBLE) {
			sendBuf = new double[] {(double)val};
			recvBuf = new double[nc];
		} else if (type == MPI.INT) {
			sendBuf = new int[] {(int)val};
			recvBuf = new int[nc];
		} else if (type == MPI.FLOAT) {
			sendBuf = new float[] {(float)val};
			recvBuf = new float[nc];
		} else if (type == MPI.LONG) {
			sendBuf = new long[] {(long)val};
			recvBuf = new long[nc];
		} else
			throw new UnsupportedOperationException("The given MPI Datatype " + type + " is invalid / not implemented yet");

		p.getCommunicator().neighborAllGather(sendBuf, 1, type, recvBuf, 1, type);

		return recvBuf;
	}

	public static void main(String[] args) throws MPIException, IOException {
		MPI.Init(args);

		sim.field.DNonUniformPartition p = sim.field.DNonUniformPartition.getPartitionScheme(new int[] {10, 10}, true, new int[] {1, 1});
		p.initUniformly(null);
		p.commit();

		int[] nids = p.getNeighborIds();
		Integer[] t = new Integer[nids.length];
		for (int i = 0; i < nids.length; i++)
			t[i] = p.getPid() * 10 + nids[i];
		final int dst = 0;

		ArrayList<Integer[]> res = MPIUtil.<Integer[]>gather(p, t, dst);

		MPITest.execInOrder(x -> {
			System.out.println("gather to dst " + dst + " PID " + x);
			for (Integer[] r : res)
				for (Integer i : r)
					System.out.println(i);
		}, 100);

		Integer[] scattered = MPIUtil.<Integer[]>scatter(p, res.toArray(new Integer[0][]), dst);

		MPITest.execInOrder(x -> {
			System.out.println("scattered from src " + dst + " PID " + x);
			for (Integer i : scattered)
				System.out.println(i);
		}, 100);

		ArrayList<Integer[]> res2 = MPIUtil.<Integer[]>allGather(p, t);

		MPITest.execInOrder(x -> {
			System.out.println("allGather PID " + x);
			for (Integer[] r : res2)
				for (Integer i : r)
					System.out.println(i);
		}, 100);

		ArrayList<Integer> res3 = MPIUtil.<Integer>neighborAllToAll(p, t);

		MPITest.execInOrder(x -> {
			System.out.println("neighborAllToAll PID " + x);
			for (Integer i : res3)
				System.out.println(i);
		}, 100);

		int size = 50 * 1024 * 1024; // 50 x 4 MB (int array)
		MPITest.execOnlyIn(0, x -> {
			System.out.println("Large Dataset Test for scatterv");
		});

		int[][] sendData = new int[p.getNumProc()][];
		if (p.getPid() == dst) {
			for (int i = 0; i < p.getNumProc(); i++) {
				sendData[i] = new int[size];
				Arrays.fill(sendData[i], i + 1);
			}
		}

		int[] recvData = MPIUtil.<int[]>scatter(p, sendData, dst);
		for (int i = 0; i < size; i++)
			if (recvData[i] != p.getPid() + 1) {
				System.out.printf("PID %d VerifyError index %d want %d got %d\n", p.getPid(), i, p.getPid() + 1, recvData[i]);
				System.exit(-1);
			}

		MPITest.execOnlyIn(0, x -> {
			System.out.println("Pass");
		});

		MPITest.execOnlyIn(0, x -> {
			System.out.println("Large Dataset Test for gatherv");
		});

		int[] sendData2 = new int[size];
		Arrays.fill(sendData2, p.getPid() + 1);

		ArrayList<int[]> recvData2 = MPIUtil.<int[]>gather(p, sendData2, dst);
		if (dst == p.getPid())
			for (int i = 0; i < p.getNumProc(); i++)
				for (int j = 0; j < size; j++)
					if (recvData2.get(i)[j] != i + 1) {
						System.out.printf("VerifyError index [%d][%d] want %d got %d\n", i, j, i + 1, recvData2.get(i)[j]);
						System.exit(-1);
					}

		MPITest.execOnlyIn(0, x -> {
			System.out.println("Pass");
		});

		MPI.Finalize();
	}
}

class ByteBufferOutputStream extends OutputStream implements AutoCloseable {
	ByteBuffer buf;

	public ByteBufferOutputStream(ByteBuffer buf) {
		if (buf == null)
			throw new IllegalArgumentException("The buffer provided is null!");

		this.buf = buf;
	}

	public void write(int b) throws IOException {
		buf.put((byte) b);
	}

	public void write(byte[] bytes, int off, int len) throws IOException {
		buf.put(bytes, off, len);
	}

	public void close() {
		try {
			super.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

class ByteBufferInputStream extends InputStream implements AutoCloseable {
	ByteBuffer buf;

	public ByteBufferInputStream(ByteBuffer buf) {
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

	public int read(byte[] bytes, int off, int len) throws IOException {
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
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
