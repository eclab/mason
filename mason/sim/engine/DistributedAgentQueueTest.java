package sim.engine;

import java.io.*;
import java.util.*;

import sim.util.*;
import ec.util.*;

import sim.field.grid.DDoubleGrid2D;
import sim.engine.*;

import mpi.*;

import sim.engine.DistributedAgentQueue.*;

import sim.engine.DistributedAgentQueueTestAgent;

import java.util.concurrent.TimeUnit;

public class DistributedAgentQueueTest extends SimState {

	DDoubleGrid2D grid;
	HashSet<Integer> agentids;
	DistributedAgentQueue queue;

	private class Inspector implements Steppable {
		private static final long serialVersionUID = 1;

		public void step( final SimState state ) {
			DistributedAgentQueueTest sim = (DistributedAgentQueueTest)state;
			String s = String.format("PID %d Step %d Agent list: ", sim.grid.pid, sim.schedule.getSteps());
			for (Integer i : sim.agentids)
				s += i.toString() + " ";
			System.out.println(s);
		}
	}

	private class Synchronizer implements Steppable {
		private static final long serialVersionUID = 1;

		public void step( final SimState state ) {
			DistributedAgentQueueTest sim = (DistributedAgentQueueTest)state;
			try {
				sim.queue.sync();
				TimeUnit.SECONDS.sleep(1);
			} catch (Exception e) {
				e.printStackTrace(System.out);
				System.exit(-1);
			}
		}
	}

	public void addId(Object a) {
		DistributedAgentQueueTestAgent agent = (DistributedAgentQueueTestAgent)a;
		agentids.add(agent.id);
	}

	public void removeId(Object a) {
		DistributedAgentQueueTestAgent agent = (DistributedAgentQueueTestAgent)a;
		agentids.remove(agent.id);
	}

	public DistributedAgentQueueTest(long seed) {
		super(seed);
	}

	public void start() {
		super.start();
		int num_agent = 16, width = 10, height = 10;
		grid = new DDoubleGrid2D(width, height, 1, 0);
		try {
			queue = new DistributedAgentQueue(grid, this);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		agentids = new HashSet<Integer>();

		schedule.scheduleRepeating(Schedule.EPOCH, 0, new Inspector(), 1);

		try {
			if (grid.pid == 0) {
				agentids.add(1);
				agentids.add(2);
				agentids.add(3);
				schedule.scheduleOnce(new DistributedAgentQueueTestAgent(1, 0, 0, 1, 0, width, height), 1);
				schedule.scheduleOnce(new DistributedAgentQueueTestAgent(2, 0, 0, 0, 1, width, height), 1);
				schedule.scheduleOnce(new DistributedAgentQueueTestAgent(3, 0, 0, 1, 1, width, height), 1);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}

		schedule.scheduleRepeating(Schedule.EPOCH, 2, new Synchronizer(), 1);
	}

	public void stop() {
		return;
	}

	public static void main(String[] args) throws MPIException {
		MPI.Init(args);
		SimState.doLoop(DistributedAgentQueueTest.class, args);
		MPI.Finalize();
		System.exit(0);
	}
}