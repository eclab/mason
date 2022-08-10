/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
        
package sim.field.partitioning;

import java.rmi.RemoteException;
import java.util.*;
import mpi.*;
import sim.util.*;
import sim.engine.*;
import java.util.function.Consumer;

/**
 * Quad tree partition divides the world into partitions and arranging them as a
 * quad tree.
 *
 * @param <P> Type of point
 */
public class Partition
    {
    QuadTree qt;
    QuadTreeNode myLeafNode; // the leaf node that this pid is mapped to
    Map<Integer, GroupComm> groups; // Map the level to its corresponding comm group
    int treeDepth;

    int pid;
    int numProcessors;
    int width;
    int height;
    boolean toroidal;
    protected Comm comm;
    protected int aoi;

    ArrayList<Consumer> preCallbacks, postCallbacks;

    public Partition(int width, int height, boolean toroidal, int aoi)
        {
        this.width = width;
        this.height = height;
        this.toroidal = toroidal;
        this.aoi = aoi;
        // TODO here check aoi < partition?

        try
            {
            pid = DSimState.getPID();
            numProcessors = MPI.COMM_WORLD.getSize();
            }
        catch (MPIException e)
            {
            e.printStackTrace();
            System.exit(-1);
            }

        preCallbacks = new ArrayList<Consumer>();
        postCallbacks = new ArrayList<Consumer>();

        qt = new QuadTree(new IntRect2D(width, height), numProcessors);
        }
        
        
    public int getPID()
        {
        return pid;
        }

    public int getNumProcessors()
        {
        return numProcessors;
        }

    public boolean isToroidal()
        {
        return toroidal;
        }

    public Comm getCommunicator()
        {
        return comm;
        }

    public int getWorldWidth()
        {
        return width;
        }

    public int getWorldHeight()
        {
        return height;
        }

    public IntRect2D getWorldBounds()
        {
        return new IntRect2D(width, height);
        }

    public int getAOI()
        {
        return aoi;
        }

    // TODO let other classes who depend on the partition scheme to register proper
    // actions when partiton changes
    /**
     * Register pre commit callbacks
     * 
     * @param r
     */
    public void registerPreCommit(final Consumer r)
        {
        preCallbacks.add(r);
        }

    /**
     * Register post commit callbacks
     * 
     * @param r
     */
    public void registerPostCommit(final Consumer r)
        {
        postCallbacks.add(r);
        }


    public IntRect2D getLocalBounds()
        {
        return myLeafNode.getShape();
        }

    public IntRect2D getLocalBounds(final int pid)
        {
        for (final QuadTreeNode node : qt.getAllLeaves())
            if (node.getProcessor() == pid)
                return node.getShape();

        throw new IllegalArgumentException("The partition for " + pid + " does not exist");
        }

    public IntRect2D getHaloBounds()
        {
        return myLeafNode.getShape().expand(aoi);
        }

    public ArrayList<IntRect2D> getAllBounds()
        {
        ArrayList<IntRect2D> allBounds = new ArrayList<>();

        // init with nulls
        for (int i = 0; i < numProcessors; i++)
            allBounds.add(null);

        for (final QuadTreeNode node : qt.getAllLeaves())
            allBounds.set(node.getProcessor(), node.getShape());
        return allBounds;
        }

    public QuadTree getQt()
        {
        return qt;
        }

    public int getNumNeighbors()
        {
        return getNeighborPIDs().length;
        }

    public int[] getNeighborPIDs()
        {
        return qt.getNeighborPids(myLeafNode, aoi, toroidal);
        }

    public int toPartitionPID(final Number2D p)
        {
        return qt.getLeafNode(p).getProcessor();
        }
    /**
     * Creates the MPI comm world by defining the MPI topology as this quad tree.
     */
    protected void createMPITopo()
        {
        final int[] ns = getNeighborPIDs();

        try
            {
            if(comm != null)
                {
                comm.free();
                //MPI.COMM_WORLD.barrier(); not sure if needed
                }
            // Create a unweighted & undirected graph for neighbor communication
            comm = MPI.COMM_WORLD.createDistGraphAdjacent(ns, ns, new Info(), false);

            // Create the group comms for nodes at the same level (intercomm) and for nodes
            // and its all leaves (intracomm)

            createGroups();
            }
        catch (final MPIException e)
            {
            e.printStackTrace();
            System.exit(-1);
            }
        }

    public void initialize()
        {
        initUniformly();
        }

    /**
     * This method is only used internally, for init use initialize instead
     */
    void initUniformly()
        {
        // Init into a full quad tree
        int numProcessorsOld = numProcessors;
        numProcessors = (int) Math.pow(4, (int) Math.floor(Math.log(numProcessorsOld) / Math.log(4)));

        int nz = 0;
        while ((numProcessors >> nz & 0x1) != 0x1)
            nz++;

        for (int level = 0; level < nz / 2; level++) // 2 == number of dimensions, width and height
            {
            final ArrayList<QuadTreeNode> leaves = qt.getAllLeaves();

            for (final QuadTreeNode leaf : leaves)
                {
                Double2D d = leaf.getShape().getCenter();
                qt.split(new Int2D((int) Math.floor(d.x), (int) Math.floor(d.y)));
                }

            }
        treeDepth = (nz / 2) - 1;

        // Refine needs to be done twice
        refineQuadtree(numProcessorsOld);
        refineQuadtree(numProcessorsOld);

        numProcessors = numProcessorsOld;
        mapNodeToProc();

        createMPITopo();
        }

    protected void refineQuadtree(int P)
        {
        QuadTreeNode root = qt.root;
        ArrayList<QuadTreeNode> leaves = qt.getAllLeaves();

        if (leaves.size() < P)
            {

            ArrayList<QuadTreeNode> splittableLeaves = new ArrayList<QuadTreeNode>();

            for (QuadTreeNode leaf : leaves)
                {
                if (isSpaceSplittable(leaf))
                    {
                    splittableLeaves.add(leaf);
                    }
                }

            int numberLeaves = leaves.size();
            // useful to handle the partitioning when the number of processers is lower than for
                        
            // if the quadtree has only the root node
            if (numberLeaves == 1 && leaves.get(0).isRoot())
                { 
                splittableLeaves.add(root);
                }

            int toSplitId = -1;
                        
            while (numberLeaves < P && ((toSplitId = splittableLeaves.remove(splittableLeaves.size() - 1).id) != -1))
                {
                Double2D center = qt.getNode(toSplitId).shape.getCenter();
                qt.split(new Int2D((int) Math.floor(center.x), (int) Math.floor(center.y)));
                numberLeaves += 3;
                }
                        
            if (numberLeaves < P)
                {
                return;
                }
            // now the number of leaves is at most p+2
                        
            }
        else
            {
            ArrayList<QuadTreeNode> parents = new ArrayList<QuadTreeNode>(findLeafParent(leaves));
            int numberLeaves = leaves.size();

            while (numberLeaves >= P + 3)
                {

                QuadTreeNode toMerge = parents.remove(0);

                qt.merge(toMerge);
                                
                numberLeaves -= 3;

                }

            while (numberLeaves != P)
                {
                refinePartition();
                numberLeaves--;
                }
            }
        }

    // checks if the heigh and the width is at least 2*AOI
    protected boolean isSpaceSplittable(QuadTreeNode node)
        {
        int x = node.getShape().getHeight();
        int y = node.getShape().getWidth();
        if ((x >= 2 * this.aoi) && (y >= 2 * this.aoi) && (node.level != 0))
            {
            return true;
            }
        else
            {
            return false;
            }

        }

    // find the parents of all leaves
    private static ArrayList<QuadTreeNode> findLeafParent(ArrayList<QuadTreeNode> leaves)
        {
        ArrayList<QuadTreeNode> parents = new ArrayList<QuadTreeNode>();
        for (QuadTreeNode leaf : leaves)
            {
            if (!parents.contains(leaf.parent))
                parents.add(leaf.parent);
            }
        return parents;
        }

    private void refinePartition()
        {
        ArrayList<QuadTreeNode> allNodes = new ArrayList<QuadTreeNode>(qt.getAllNodes());
        int maxChildren = -1;
        int maxChildrenId = -1;
        for (int i = 0; i < allNodes.size(); i++)
            {
            if (allNodes.get(i).getLevel() == (qt.getDepth() - 1))
                {
                if (maxChildren < allNodes.get(i).getChildren().size())
                    maxChildren = allNodes.get(i).getChildren().size();
                maxChildrenId = allNodes.get(i).id;
                }
            }
        ArrayList<QuadTreeNode> mergable = qt.getNode(maxChildrenId).getChildren();

                

        QuadTreeNode firstNode = mergable.get(0);
        QuadTreeNode secondNode = mergable.get(1);
 
        if(firstNode.getShape().getArea()>secondNode.getShape().getArea())
            {
            firstNode=secondNode;
            secondNode=mergable.get(2);
            }
                
        Int2D maxul = firstNode.shape.ul().min(secondNode.shape.ul());

        Int2D minbr = firstNode.shape.br().max(secondNode.shape.br());

        IntRect2D newShape = new IntRect2D(maxul, minbr);

        firstNode.reshape(newShape);
                
        qt.delLeaf(secondNode);
        }

    /**
     * Maps node to a processor
     * 
     */
    protected void mapNodeToProc()
        {
        final ArrayList<QuadTreeNode> leaves = qt.getAllLeaves();

        if (leaves.size() != numProcessors)
            throw new IllegalArgumentException("The number of leaves " + leaves.size()
                + " does not equal to the number of processors " + numProcessors);
        // Map the leaf nodes first
        for (int i = 0; i < numProcessors; i++)
            leaves.get(i).setProcessor(i);
        // if pid == 0 then myLeafNode is root node
        myLeafNode = leaves.get(pid);

        // Map non-leaf nodes - Use the first children node to hold itself
        while (leaves.size() > 0)
            {
            final QuadTreeNode curr = leaves.remove(0);
            final QuadTreeNode parent = curr.getParent();
            if (parent == null || parent.getChild(0) != curr)
                continue;
            parent.setProcessor(curr.getProcessor());
            leaves.add(parent);
            }

        // Set the proc id to the IntRect2D so it can be printed out when debugging
        // it is not used by the program itself (TODO double-check)

        // removed by Raj Patel
        // for (final QuadTreeNode leaf : qt.getAllLeaves())
        // leaf.getShape().setId(leaf.getProcessor());
        }

    /**
     * Create groups for MPI communication
     * 
     * @throws MPIException
     */
    protected void createGroups() throws MPIException
        {
        freeResources();
        int currDepth = 0;
        groups = new HashMap<Integer, GroupComm>();

        // Iterate level by level to create groups
        ArrayList<QuadTreeNode> currLevel = new ArrayList<>();
        currLevel.add(qt.getRoot());
        while (currLevel.size() > 0)
            {

            final ArrayList<QuadTreeNode> nextLevel = new ArrayList<>();

            for (final QuadTreeNode node : currLevel)
                {
                nextLevel.addAll(node.getChildren());

                // whether this pid should participate in this group
                if (node.isAncestorOf(myLeafNode))
                    {

                    groups.put(currDepth, new GroupComm(node));

                    }

                // Others will wait until the group is created
                MPI.COMM_WORLD.barrier();

                }

            final GroupComm gc = groups.get(currDepth);
            if (isGroupMaster(gc))
                gc.setInterComm(currLevel);

            MPI.COMM_WORLD.barrier();

            currLevel = nextLevel;
            currDepth++;
            }
        }

    public int[] getProcessorNeighborhood(int level) throws RemoteException
        {
        ArrayList<QuadTreeNode> leaves = groups.get(level).leaves;
        int[] pids = new int[leaves.size()];
        for (int i = 0; i < pids.length; i++)
            pids[i] = leaves.get(i).getProcessor();
        return pids;
        }

    /**
     * @param gc
     * @return true if the calling pid is the master node of the given GroupComm
     */
    public boolean isGroupMaster(final GroupComm gc)
        {
        return gc != null && gc.master.getProcessor() == pid;
        }

    /**
     * @param level
     * @return true if the calling pid is the master node of the GroupComm at the
     *         given level
     */
    public boolean isGroupMaster(final int level)
        {
        return isGroupMaster(getGroupComm(level));
        }

    public boolean isRootProcessor()
        {
        // The Global Master of Quad Tree is global root for MPI as well
        return isGroupMaster(0);
        }

    /**
     * @param level
     * @return the GroupComm instance if the calling pid should be involved in the
     *         group communication of the given level <br>
     *         null otherwise
     */
    public GroupComm getGroupComm(final int level)
        {
        return groups.get(level);
        }

    /**
     * @param level
     * @return the shape when the calling pid holds one of the master nodes of this
     *         level <br>
     *         null otherwise
     */
    public IntRect2D getNodeShapeAtLevel(final int level)
        {
        final GroupComm gc = getGroupComm(level);
        if (isGroupMaster(gc))
            return gc.master.getShape();
        return null;
        }

    /**
     * @return the treeDepth
     */
    public int getTreeDepth()
        {
        return treeDepth;
        }

    private void testIntraGroupComm(final int depth) throws MPIException
        {
        MPIUtil.MPITest.printOnlyIn(0, "Testing intra group comm at depth " + depth);

        if (groups.containsKey(depth))
            {
            final Comm gcomm = groups.get(depth).comm;
            final int[] buf = new int[16];

            buf[gcomm.getRank()] = pid;
            gcomm.allGather(buf, 1, MPI.INT);
            System.out.println(String.format("PID %2d %s", pid, Arrays.toString(buf)));
            }

        MPI.COMM_WORLD.barrier();
        }

    private void testInterGroupComm(final int depth) throws MPIException
        {
        MPIUtil.MPITest.printOnlyIn(0, "Testing inter group comm at depth " + depth);

        final GroupComm gc = getGroupComm(depth);
        if (isGroupMaster(gc))
            {
            final Comm gcomm = gc.interComm;
            final int[] buf = new int[16];

            buf[gcomm.getRank()] = pid;
            gcomm.allGather(buf, 1, MPI.INT);
            System.out.print(String.format("PID %2d %s\n", pid, Arrays.toString(buf)));
            }

        MPI.COMM_WORLD.barrier();
        }

    /**
     * Balance the partitions by moving the centroids for the given level.
     * 
     * @param myRuntime
     * @param level
     * 
     * @throws MPIException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void balance(final double myRuntime, final int level) throws MPIException
        {
        final GroupComm gc = groups.get(level);

        Object[] sendCentroids = new Object[] { null };

        if (gc != null)
            {
            Double2D d = myLeafNode.getShape().getCenter();
            final Int2D ctr = new Int2D((int) Math.floor(d.x), (int) Math.floor(d.y));
            final double[] sendData = new double[3], recvData = new double[3]; // 2 (num dimensions) + 1

            sendData[0] = myRuntime;
            sendData[1] = ctr.x * myRuntime;
            sendData[2] = ctr.y * myRuntime;

            gc.comm.reduce(sendData, recvData, recvData.length, MPI.DOUBLE, MPI.SUM, gc.groupRoot);

            if (isGroupMaster(gc))
                {
                int[] locVals = new int[recvData.length - 1];
                // skip first (i = 1)
                for (int i = 1; i < recvData.length; i++)
                    {
                    double x = recvData[i];
                    locVals[i - 1] = (int) (x / recvData[0]);
                    }
                sendCentroids = new Object[] { gc.master.getId(), new Int2D(locVals) };
                }
            }

        // broadcast to all nodes
        final ArrayList<Object[]> newCentroids = MPIUtil.<Object[]>allGather(MPI.COMM_WORLD, sendCentroids);

        // call precommit
        for (final Consumer r : (ArrayList<Consumer>) preCallbacks)
            r.accept(level);

        // apply changes
        for (final Object[] obj : newCentroids)
            if (obj[0] != null)
                qt.moveOrigin((int) obj[0], (Int2D) obj[1]);

        // Assigns new neighbors after balancing
        // Recreates MPI topology based on that
        createMPITopo();

        // call postcommit
        for (final Consumer r : (ArrayList<Consumer>) postCallbacks)
            r.accept(level);
        }

    /**
     * This method is only used internally, for init use initialize instead
     *
     * @param splitPoints
     */
    private void initQuadTree(final ArrayList<Int2D> splitPoints)
        {
        // Create the quad tree based on the given split points
        qt.split(splitPoints);

        // map all quad tree nodes to processors
        mapNodeToProc();
        createMPITopo();
        }

    private static void testBalance() throws MPIException
        {
        MPIUtil.MPITest.printOnlyIn(0, "Testing balance()......");

        final Partition p = new Partition(100, 100, false, 1);

        final Int2D[] splitPoints = new Int2D[] { new Int2D(50, 50), new Int2D(25, 25), new Int2D(75, 75),
                                                  new Int2D(60, 90), new Int2D(10, 10) };

        p.initQuadTree(new ArrayList<>(Arrays.asList(splitPoints)));

        final Random rand = new Random();
        final double myRt = rand.nextDouble() * 10;

        p.balance(myRt, 0);
        for (final QuadTreeNode node : p.qt.getAllLeaves())
            MPIUtil.MPITest.printOnlyIn(0, "Leaf " + node);

        MPIUtil.MPITest.printOnlyIn(0, p.qt.toString());
        }

    private static void testInitWithPoints() throws MPIException
        {
        MPIUtil.MPITest.printOnlyIn(0, "Testing init with points......");

        final Partition p = new Partition(100, 100, false, 1);

        final Int2D[] splitPoints = new Int2D[] { new Int2D(50, 50), new Int2D(25, 25), new Int2D(75, 75),
                                                  new Int2D(60, 90), new Int2D(10, 10) };

        p.initQuadTree(new ArrayList<>(Arrays.asList(splitPoints)));

        for (int i = 0; i < 3; i++)
            {
            p.testIntraGroupComm(i);
            p.testInterGroupComm(i);
            }
        }

    private static void testInitUniformly() throws MPIException
        {
        MPIUtil.MPITest.printOnlyIn(0, "Testing init uniformly......");

        final Partition p = new Partition(1000, 1000, false, 1);
        p.initUniformly();
        if(p.pid==0)
            System.out.println("-------availIds " + p.qt.availIds);
        // for (int i = 0; i < 3; i++) {
        // p.testIntraGroupComm(i);
        // p.testInterGroupComm(i);
        // }
        }
        
    //iterates through groups and calls free
    public void freeResources()
        {
                
        if (groups != null)
            {
                
            for (GroupComm gc : groups.values())
                {
                        
                try
                    {
                    if (gc != null)
                        {
                        if (gc.comm != null)
                            {
                            gc.comm.free();
                            }
                        else if (gc.interComm != null)
                            {
                            gc.interComm.free();

                            }
                        }
                                
                                
                    } 
                        
                catch (MPIException e)
                    {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    }
                        
                }
            }
                
        }

    public static void main(final String[] args) throws MPIException
        {
        MPI.Init(args);

        // testInitWithPoints();
        testInitUniformly();
        //testNeighbors();
        // testBalance();

        MPI.Finalize();
        }

    private static void testNeighbors()
        {
        final Partition p = new Partition(1000, 1000, false, 1);
        p.initUniformly();
        if(p.pid==0)
            {
            System.out.println(p.qt);
            List<QuadTreeNode> allNodes = p.qt.getAllNodes();
            for(int i= 0; i<allNodes.size(); i++)
                {
                HashSet<QuadTreeNode> neighbors = p.qt.getNeighbors(allNodes.get(i), p.aoi, p.toroidal);
                System.out.println("pid "+p.getPID()+" node "+allNodes.get(i)+ " neighbors "+neighbors);
                System.out.println("-------availIds " + p.qt.availIds);
                }
            }
        }

    public String toString()
        {
        return "DPartition [qt=" + qt + ", myLeafNode=" + myLeafNode + ", groups=" + groups + ", aoi=" + aoi + "]";
        }
    }
