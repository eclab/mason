package sim.field;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;

import mpi.*;

import sim.util.MPIUtil;
import sim.field.storage.TestObj;

public class DObjectMap<T extends Serializable> {

	HashMap<Integer, T> local;
	HashMap<Integer, ArrayList<T>> global;

	boolean dirty;

	public DObjectMap() {
		local = new HashMap();
		global = new HashMap();
	}

	public void set(int id, T obj) {
		local.put(id, obj);
		dirty = true;
	}

	public ArrayList<T> get(int id) {
		return global.get(id);
	}

	public void sync() throws MPIException {
		Comm comm = MPI.COMM_WORLD;
		int np = comm.getSize();

		HashMap sendObj = dirty ? local : null;
		ArrayList<HashMap> recvObjs = MPIUtil.<HashMap>allGather(comm, sendObj);

		for (int i = 0; i < np; i++) {
			if (recvObjs.get(i) == null)
				continue;
			HashMap<Integer, T> m = (HashMap<Integer, T>)recvObjs.get(i);
			for (Integer id : m.keySet()) {
				if (!global.containsKey(id))
					global.put(id, new ArrayList(Collections.nCopies(np, null)));
				global.get(id).set(i, m.get(id));
			}
		}

		dirty = false;
	}

	// Remove any entries in the global that are not present in local
	public void trim() {
		global.keySet().retainAll(local.keySet());
	}

	public static void main(String[] args) throws MPIException {
		MPI.Init(args);

		int pid = MPI.COMM_WORLD.getRank();
		int np = MPI.COMM_WORLD.getSize();
		DObjectMap<TestObj> dom = new DObjectMap<TestObj>();

		assert np == 4;

		// Everyone contributes to the same id 0
		dom.set(0, new TestObj(pid));
		dom.sync();
		assert dom.get(0).stream().mapToInt(x -> x.id).sum() == 6;
		assert dom.get(0).stream().mapToInt(x -> x.id).average().getAsDouble() == 1.5;

		// Even pid node adds id 2 while odd pid node adds id 1
		if (pid % 2 == 0)
			dom.set(2, new TestObj(pid));
		else
			dom.set(1, new TestObj(pid));
		dom.sync();
		assert dom.get(2).stream().filter(x -> x != null).mapToInt(x -> x.id).sum() == 2;
		assert dom.get(2).stream().filter(x -> x != null).mapToInt(x -> x.id).average().getAsDouble() == 1.0;
		assert dom.get(1).stream().filter(x -> x != null).mapToInt(x -> x.id).sum() == 4;
		assert dom.get(1).stream().filter(x -> x != null).mapToInt(x -> x.id).average().getAsDouble() == 2.0;

		// Trim global map
		dom.trim();
		if (pid % 2 == 0)
			assert dom.get(1) == null;
		else
			assert dom.get(2) == null;

		// Sync with dirty bit not set - nothing should change
		if (pid == 0) {
			dom.set(0, new TestObj(999));
			dom.dirty = false; // overwrite the flag
		}
		dom.sync();
		assert dom.get(0).stream().mapToInt(x -> x.id).sum() == 6;
		assert dom.get(0).stream().mapToInt(x -> x.id).average().getAsDouble() == 1.5;
		if (pid % 2 == 0) {
			assert dom.get(2).stream().filter(x -> x != null).mapToInt(x -> x.id).sum() == 2;
			assert dom.get(2).stream().filter(x -> x != null).mapToInt(x -> x.id).average().getAsDouble() == 1.0;
		} else {
			assert dom.get(1).stream().filter(x -> x != null).mapToInt(x -> x.id).sum() == 4;
			assert dom.get(1).stream().filter(x -> x != null).mapToInt(x -> x.id).average().getAsDouble() == 2.0;
		}

		sim.util.MPITest.printOnlyIn(0, "All tests passed!");

		MPI.Finalize();
	}
}