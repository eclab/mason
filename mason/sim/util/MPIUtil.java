package sim.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import mpi.*;

import sim.field.DPartition;

//TODO Reuse ByteBuffers to minimize allocate/de-allocate overhead
//TODO Use ByteBuffer-back output/input streams - need to dynamically adjust the size of backing buffer.

// class ByteBufferOutputStream extends OutpuStream {
// 	ByteBuffer buf;

// 	public ByteBufferOutputStream(ByteBuffer buf) {
// 		this.buf = buf;
// 	}

// 	public void write(int b) throws IOException {
// 		buf.put((byte) b);
// 	}

// 	public void write(byte[] bytes, int off, int len) throws IOException {
// 		buf.put(bytes, off, len);
// 	}
// }

// class ByteBufferInputStream extends InputStream {
// 	ByteBuffer buf;

// 	public ByteBufferInputStream(ByteBuffer buf) {
// 		this.buf = buf;
// 	}

// 	public int read() throws IOException {
// 		if (!buf.hasRemaining()) {
// 			return -1;
// 		}
// 		return buf.get() & 0xFF;
// 	}

// 	public int read(byte[] bytes, int off, int len) throws IOException {
// 		if (!buf.hasRemaining()) {
// 			return -1;
// 		}

// 		len = Math.min(len, buf.remaining());
// 		buf.get(bytes, off, len);
// 		return len;
// 	}
// }

// Utility class that serialize/exchange/deserialize objects using MPI
public class MPIUtil {

	// Serialize a Serializable using Java's builtin serialization and return the ByteBuffer
	private static ByteBuffer serialize(Serializable obj) {
		ByteBuffer buf = null;

		try (
			    ByteArrayOutputStream out = new ByteArrayOutputStream();
			    ObjectOutputStream os = new ObjectOutputStream(out)
			) {
			os.writeObject(obj);
			os.flush();
			byte[] data = out.toByteArray();

			buf = ByteBuffer.allocateDirect(data.length);
			buf.put(data).flip();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return buf;
	}

	// Serialize each Serializable in objs using Java's builtin serialization
	// Concatenate their individual ByteBuffer together and return the final ByteBuffer
	// The length of each byte array will be writtern into the count array
	private static ByteBuffer serialize(Serializable[] objs, int[] count) {
		int total = 0;
		ByteBuffer buf = null;

		ByteBuffer[] objBufs = new ByteBuffer[objs.length];
		for(int i = 0; i < objs.length; i++) {
			objBufs[i] = serialize(objs[i]);
			count[i] = objBufs[i].capacity();
			total += count[i];
		}

		buf = ByteBuffer.allocateDirect(total);
		for (ByteBuffer b : objBufs)
			buf.put(b);

		buf.flip();

		return buf;
	}

	// Deserialize the object of given type T that is stored in [pos, pos + len) in buf
	private static <T extends Serializable> T deserialize(ByteBuffer buf, int pos, int len) {
		T obj = null;

		// Get the data in buf[pos : pos + len] into the byte array
		byte[] data = new byte[len];
		buf.position(pos);
		buf.get(data);

		try (
			    ByteArrayInputStream in = new ByteArrayInputStream(data);
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

	// LP root broadcasts a Serializable object to all other LPs
	public static <T extends Serializable> T bcast(DPartition p, T obj, int root) throws MPIException {
		ByteBuffer buf = null;

		if (p.getPid() == root)
			buf = serialize(obj);

		int[] count = new int[] {buf == null ? 0 : buf.capacity()};

		p.getCommunicator().bcast(count, 1, MPI.INT, root);

		if (p.getPid() != root)
			buf = ByteBuffer.allocateDirect(count[0]);

		p.getCommunicator().bcast(buf, count[0], MPI.BYTE, root);

		return MPIUtil.<T>deserialize(buf, 0, count[0]);
	}

	// TODO refactor this or the one above
	// Currently used by init() in RemoteProxy() to broadcast the host ip address
	public static <T extends Serializable> T bcast(T obj, int root) throws MPIException {
		ByteBuffer buf = null;

		if (MPI.COMM_WORLD.getRank() == root)
			buf = serialize(obj);

		int[] count = new int[] {buf == null ? 0 : buf.capacity()};

		MPI.COMM_WORLD.bcast(count, 1, MPI.INT, root);

		if (MPI.COMM_WORLD.getRank() != root)
			buf = ByteBuffer.allocateDirect(count[0]);

		MPI.COMM_WORLD.bcast(buf, count[0], MPI.BYTE, root);

		return MPIUtil.<T>deserialize(buf, 0, count[0]);
	}

	// Reverse of gather
	public static <T extends Serializable> T scatter(DPartition p, T[] sendObjs, int root) throws MPIException {
		int pid = p.getPid(), np = p.getNumProc(), dstCount;
		int[] srcDispl = null, srcCount = new int[np];
		ByteBuffer dstBuf, srcBuf = null;

		if (pid == root) {
			srcBuf = serialize(sendObjs, srcCount);
			srcDispl = getDispl(srcCount);
		}

		p.getCommunicator().scatter(srcCount, 1, MPI.INT, root);

		dstCount = srcCount[0];
		dstBuf = ByteBuffer.allocateDirect(dstCount);

		p.getCommunicator().scatterv(srcBuf, srcCount, srcDispl, MPI.BYTE, dstBuf, dstCount, MPI.BYTE, root);

		return MPIUtil.<T>deserialize(dstBuf, 0, dstCount);
	}

	// TODO refactor this or the one above
	// Currently used by collectGroup() and distributedGroup() in HaloField
	public static <T extends Serializable> T scatter(Comm comm, T[] sendObjs, int root) throws MPIException {
		int pid =  comm.getRank(), np =  comm.getSize(), dstCount;
		int[] srcDispl = null, srcCount = new int[np];
		ByteBuffer dstBuf, srcBuf = null;

		if (pid == root) {
			srcBuf = serialize(sendObjs, srcCount);
			srcDispl = getDispl(srcCount);
		}

		comm.scatter(srcCount, 1, MPI.INT, root);

		dstCount = srcCount[0];
		dstBuf = ByteBuffer.allocateDirect(dstCount);

		comm.scatterv(srcBuf, srcCount, srcDispl, MPI.BYTE, dstBuf, dstCount, MPI.BYTE, root);

		return MPIUtil.<T>deserialize(dstBuf, 0, dstCount);
	}

	// Each LP sends the sendObj to dst
	// dst will return an ArrayList of np objects of type T
	// others will return an empty ArrayList
	public static <T extends Serializable> ArrayList<T> gather(DPartition p, T sendObj, int dst) throws MPIException {
		int np = p.getNumProc();
		int pid = p.getPid();
		int[] dstDispl = null, dstCount = new int[np];
		ByteBuffer srcBuf, dstBuf;
		ArrayList<T> recvObjs = new ArrayList();;

		srcBuf = serialize(sendObj);

		p.getCommunicator().gather(new int[] {srcBuf.capacity()}, 1, MPI.INT, dstCount, 1, MPI.INT, dst);

		dstBuf = ByteBuffer.allocateDirect(Arrays.stream(dstCount).sum());
		dstDispl = getDispl(dstCount);

		p.getCommunicator().gatherv(srcBuf, srcBuf.capacity(), MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE, dst);

		if (pid == dst) {
			for (int i = 0; i < np; i++)
				if (i == pid)
					recvObjs.add(sendObj);
				else
					recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));
		}

		return recvObjs;
	}

	// TODO refactor this or the one above
	// Currently used by collectGroup() and distributedGroup() in HaloField
	public static <T extends Serializable> ArrayList<T> gather(Comm comm, T sendObj, int dst) throws MPIException {
		int np = comm.getSize();
		int pid = comm.getRank();
		int[] dstDispl, dstCount = new int[np];
		ByteBuffer dstBuf, srcBuf;
		ArrayList<T> recvObjs = new ArrayList();

		srcBuf = serialize(sendObj);

		comm.gather(new int[] {srcBuf.capacity()}, 1, MPI.INT, dstCount, 1, MPI.INT, dst);

		dstBuf = ByteBuffer.allocateDirect(Arrays.stream(dstCount).sum());
		dstDispl = getDispl(dstCount);

		comm.gatherv(srcBuf, srcBuf.capacity(), MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE, dst);

		if (pid == dst)
			for (int i = 0; i < np; i++)
				if (i == pid)
					recvObjs.add(sendObj);
				else
					recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));

		return recvObjs;
	}

	// Each LP contributes the sendObj
	// All the LPs will receive all the sendObjs from all the LPs returned in the ArrayList
	public static <T extends Serializable> ArrayList<T> allGather(DPartition p, T sendObj) throws MPIException {
		int np = p.getNumProc();
		int pid = p.getPid();
		int[] dstDispl, dstCount = new int[np];
		ByteBuffer dstBuf, srcBuf;
		ArrayList<T> recvObjs = new ArrayList();

		srcBuf = serialize(sendObj);
		dstCount[pid] = srcBuf.capacity();

		p.getCommunicator().allGather(dstCount, 1, MPI.INT);

		dstBuf = ByteBuffer.allocateDirect(Arrays.stream(dstCount).sum());
		dstDispl = getDispl(dstCount);

		p.getCommunicator().allGatherv(srcBuf, srcBuf.capacity(), MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE);
		for (int i = 0; i < np; i++)
			if (i == pid)
				recvObjs.add(sendObj);
			else
				recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));

		return recvObjs;
	}

	public static <T extends Serializable> ArrayList<T> allGather(Comm comm, T sendObj) throws MPIException {
		int np = comm.getSize();
		int pid = comm.getRank();
		int[] dstDispl, dstCount = new int[np];
		ByteBuffer dstBuf, srcBuf;
		ArrayList<T> recvObjs = new ArrayList();

		srcBuf = serialize(sendObj);
		dstCount[pid] = srcBuf.capacity();

		comm.allGather(dstCount, 1, MPI.INT);

		dstBuf = ByteBuffer.allocateDirect(Arrays.stream(dstCount).sum());
		dstDispl = getDispl(dstCount);

		comm.allGatherv(srcBuf, srcBuf.capacity(), MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE);
		for (int i = 0; i < np; i++)
			if (i == pid)
				recvObjs.add(sendObj);
			else
				recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));

		return recvObjs;
	}

	// Each LP sends and receives one object to/from each of its neighbors
	// in the order that is defined in partition scheme
	public static <T extends Serializable> ArrayList<T> neighborAllToAll(DPartition p, T[] sendObjs) throws MPIException {
		int nc = sendObjs.length;
		int[] srcDispl, srcCount = new int[nc];
		int[] dstDispl, dstCount = new int[nc];
		ByteBuffer srcBuf, dstBuf;
		ArrayList<T> recvObjs = new ArrayList();

		srcBuf = serialize(sendObjs, srcCount);
		srcDispl = getDispl(srcCount);

		p.getCommunicator().neighborAllToAll(srcCount, 1, MPI.INT, dstCount, 1, MPI.INT);

		dstBuf = ByteBuffer.allocateDirect(Arrays.stream(dstCount).sum());
		dstDispl = getDispl(dstCount);

		p.getCommunicator().neighborAllToAllv(srcBuf, srcCount, srcDispl, MPI.BYTE, dstBuf, dstCount, dstDispl, MPI.BYTE);

		for (int i = 0; i < nc; i++)
			recvObjs.add(MPIUtil.<T>deserialize(dstBuf, dstDispl[i], dstCount[i]));

		return recvObjs;
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

		int size = 100 * 1024 * 1024; // 100 x 4 MB (int array)
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