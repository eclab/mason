package sim.engine;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import sim.util.*;
import ec.util.*;
import sim.field.DUniformPartition;

import mpi.*;

public class DistributedAgentQueueTest extends SimState {

	DistributedAgentQueue queue;
	DUniformPartition partition;

	private class Inspector implements Steppable {
		public void step( final SimState state ) {
			DistributedAgentQueueTest sim = (DistributedAgentQueueTest)state;
			String s = String.format("PID %d Step %d Agent list: ", sim.partition.pid, sim.schedule.getSteps());
			for (Steppable i : sim.queue) {
				DistributedAgentQueueTestAgent a = (DistributedAgentQueueTestAgent)i;
				s += a.toString() + " ";
			}
			System.out.println(s);
		}
	}

	private class Synchronizer implements Steppable {
		public void step( final SimState state ) {
			DistributedAgentQueueTest sim = (DistributedAgentQueueTest)state;
			try {
				sim.queue.sync();
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (Exception e) {
				e.printStackTrace(System.out);
				System.exit(-1);
			}
		}
	}

	public DistributedAgentQueueTest(long seed) {
		super(seed);
	}

	public void start() {
		super.start();

		int width = 10, height = 10;
		partition = new DUniformPartition(new int[] {width, height});
		try {
			queue = new DistributedAgentQueue(partition, this);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		
		schedule.scheduleRepeating(Schedule.EPOCH, 0, new Inspector(), 1);
		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Synchronizer(), 1);

		try {
			if (partition.pid == 0) {
				queue.add(new DistributedAgentQueueTestAgent(1, 0, 0, 0, 1, width, height), 0, 0);
				queue.add(new DistributedAgentQueueTestAgent(2, 0, 0, 1, 0, width, height), 0, 0);
				queue.add(new DistributedAgentQueueTestAgent(3, 0, 0, 1, 1, width, height), 0, 0);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}
	}

	public static void main(String[] args) throws MPIException {
		MPI.Init(args);
		SimState.doLoop(DistributedAgentQueueTest.class, args);
		MPI.Finalize();
	}
}