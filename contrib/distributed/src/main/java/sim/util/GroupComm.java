package sim.util;

import java.util.ArrayList;

import mpi.Comm;
import mpi.Group;
import mpi.MPI;
import mpi.MPIException;
import sim.field.partitioning.QuadTreeNode;

/**
 * Creates and contains the comm world (for the communication topology)
 *
 */
public class GroupComm
{
	private static final long serialVersionUID = 1L;

	public QuadTreeNode master;
	public ArrayList<QuadTreeNode> leaves;
	public Comm comm;
	public Comm interComm;
	public int groupRoot;

	/**
	 * Creates and contains the comm world (for the communication topology)
	 * 
	 * @param master
	 * @throws MPIException
	 */
	public GroupComm(QuadTreeNode master) throws MPIException
	{
		this.master = master;
		this.leaves = master.getLeaves();

		Group world = MPI.COMM_WORLD.getGroup();
		Group group = world.incl(leaves.stream()
				.mapToInt(leaf -> leaf.getProcessor())
				.toArray());

		/*
		//added by Raj
        if(comm != null) {
        	comm.free();
			//MPI.COMM_WORLD.barrier(); not sure if needed
        }
        */
		
		comm = MPI.COMM_WORLD.createGroup(group, 0);
		groupRoot = Group.translateRanks(world, new int[] { master.getProcessor() }, group)[0];
	}

	/**
	 * Sets up interComm between a given list of nodes (nodes on a level)
	 * 
	 * @param nodes
	 * @throws MPIException
	 */
	public void setInterComm(ArrayList<QuadTreeNode> nodes) throws MPIException
	{
		Group world = MPI.COMM_WORLD.getGroup();
		Group group = world.incl(nodes.stream()
				.filter(node -> !node.isLeaf())
				.mapToInt(node -> node.getProcessor())
				.toArray());
		/*
		//added by Raj
        if(interComm != null) {
        	interComm.free();
			//MPI.COMM_WORLD.barrier(); not sure if needed
        }
        */
        
		interComm = MPI.COMM_WORLD.createGroup(group, 0);
	}
}
