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

	/**
	 * Creates and contains the comm world (for the communication topology)
	 * 
	 * @param master
	 * @throws MPIException
	 */
	public GroupComm(QuadTreeNode master) throws MPIException {
		this.master = master;
		this.leaves = master.getLeaves();

		Group world = MPI.COMM_WORLD.getGroup();
		Group group = world.incl(leaves.stream()
				.mapToInt(leaf -> leaf.getProcessor())
				.toArray());

		comm = MPI.COMM_WORLD.createGroup(group, 0);
		groupRoot = Group.translateRanks(world, new int[] { master.getProcessor() }, group)[0];
	}

	/**
	 * Sets up interComm between a given list of nodes (nodes on a level)
	 * 
	 * @param nodes
	 * @throws MPIException
	 */
	public void setInterComm(List<QuadTreeNode> nodes) throws MPIException {
		Group world = MPI.COMM_WORLD.getGroup();
		Group group = world.incl(nodes.stream()
				.filter(node -> !node.isLeaf())
				.mapToInt(node -> node.getProcessor())
				.toArray());

		interComm = MPI.COMM_WORLD.createGroup(group, 0);
	}
}
