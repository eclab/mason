package sim.util;

import java.util.List;

import mpi.*;
import sim.field.partitioning.QuadTreeNode;

/**
 * Creates and contains the comm world (for the communication topology)
 *
 */
public class GroupComm {
	public QuadTreeNode master;
	public List<QuadTreeNode> leaves;

	public Comm comm, interComm;
	public int groupRoot;

	public GroupComm(QuadTreeNode master) throws MPIException {
		this.master = master;
		this.leaves = master.getLeaves();

		Group world = MPI.COMM_WORLD.getGroup();
		Group group = world.incl(leaves.stream()
				.mapToInt(leaf -> leaf.getProc())
				.toArray());

		comm = MPI.COMM_WORLD.createGroup(group, 0);
		groupRoot = Group.translateRanks(world, new int[] { master.getProc() }, group)[0];
	}

	public void setInterComm(List<QuadTreeNode> nodes) throws MPIException {
		Group world = MPI.COMM_WORLD.getGroup();
		Group group = world.incl(nodes.stream()
				.filter(node -> !node.isLeaf())
				.mapToInt(node -> node.getProc())
				.toArray());

		interComm = MPI.COMM_WORLD.createGroup(group, 0);
	}
}
