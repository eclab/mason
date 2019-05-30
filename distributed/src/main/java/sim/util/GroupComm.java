package sim.util;

import java.util.List;

import mpi.*;

public class GroupComm {
    public QTNode master;
    public List<QTNode> leaves;

    public Comm comm, interComm;
    public int groupRoot;

    public GroupComm(QTNode master) throws MPIException {
        this.master = master;
        this.leaves = master.getLeaves();

        Group world = MPI.COMM_WORLD.getGroup();
        Group group = world.incl(leaves.stream()
                                 .mapToInt(leaf -> leaf.getProc())
                                 .toArray());

        comm = MPI.COMM_WORLD.createGroup(group, 0);
        groupRoot = Group.translateRanks(world, new int[] {master.getProc()}, group)[0];
    }

    public void setInterComm(List<QTNode> nodes) throws MPIException {
        Group world = MPI.COMM_WORLD.getGroup();
        Group group = world.incl(nodes.stream()
                                 .filter(node -> !node.isLeaf())
                                 .mapToInt(node -> node.getProc())
                                 .toArray());

        interComm = MPI.COMM_WORLD.createGroup(group, 0);
    }
}
