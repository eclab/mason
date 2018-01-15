package sim.engine;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import sim.util.*;
import ec.util.*;
import sim.field.DUniformPartition;

import mpi.*;

public class DistributedAgentQueueTest extends SimState {

	ArrayList<Integer> agentids;
	DistributedAgentQueue queue;
	DUniformPartition partition;

	private class Inspector implements Steppable {
		public void step( final SimState state ) {
			DistributedAgentQueueTest sim = (DistributedAgentQueueTest)state;
			String s = String.format("PID %d Step %d Agent list: ", sim.partition.pid, sim.schedule.getSteps());
			for (Integer i : sim.agentids)
				s += i.toString() + " ";
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

	public void addId(Object a) {
		DistributedAgentQueueTestAgent agent = (DistributedAgentQueueTestAgent)a;
		agentids.add(Integer.valueOf(agent.id));
	}

	public void removeId(Object a) {
		DistributedAgentQueueTestAgent agent = (DistributedAgentQueueTestAgent)a;
		agentids.remove(Integer.valueOf(agent.id));
	}

	public DistributedAgentQueueTest(long seed) {
		super(seed);
	}

	public void start() {
		super.start();

		int width = 10, height = 10;
		agentids = new ArrayList<Integer>();
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
				agentids.add(Integer.valueOf(1));
				agentids.add(Integer.valueOf(2));
				agentids.add(Integer.valueOf(3));
				schedule.scheduleOnce(new DistributedAgentQueueTestAgent(1, 0, 0, 0, 1, width, height), 1);
				schedule.scheduleOnce(new DistributedAgentQueueTestAgent(2, 0, 0, 1, 0, width, height), 1);
				schedule.scheduleOnce(new DistributedAgentQueueTestAgent(3, 0, 0, 1, 1, width, height), 1);
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