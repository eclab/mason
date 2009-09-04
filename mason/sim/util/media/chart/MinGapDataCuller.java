package sim.util.media.chart;

import sim.util.*;



/**
 * This is meant as an on-line algorithm for keeping a constant number of data points
 * from an on-going time series. It only looks at the X values of the time series data points.
 * 
 * <p>Specifically, this algorithm eliminates the point the leaves the smallest gap
 * (i.e. has the closest neighbors).  The first and last data point are never touched. 
 * 
 * <p>In case of ties, it chooses the first.  This is meant to make the older data sparse while keeping
 * more/most of the fresh data.
 * 
 * 
 * Few gaps change between removes (2 old are merged, 1 new is introduced;
 * 1. I could cache the gap sums
 * 2. I could use a heap (although the tie breaking might be off)
 * 
 * 
 * <p> For efficiency reasons, multiple points are dropped in one culling.
 * The suggested (default) amount of points dropped in one culling is 50%.
 * 
 * 
 * @author Gabriel Balan
 */
public class MinGapDataCuller implements DataCuller
    {
    int maxPointCount;
    int pointCountAfterCulling;
    IntBag reusableIntBag;
        
    public MinGapDataCuller(int maxPointCount, int pointCountAfterCulling)
        {
        setMaxAndMinCounts(maxPointCount, pointCountAfterCulling);
        this.reusableIntBag = new IntBag(maxPointCount-pointCountAfterCulling+1);
        //+1 cause you need 1 over maxPointCount to trigger the culling
        }
        
    public MinGapDataCuller(int maxPointCount)
        {
        this(maxPointCount, maxPointCount/2+1);
        }
    public void setMaxAndMinCounts(int maxPointCount, int pointCountAfterCulling)
        {
        this.maxPointCount = maxPointCount;
        this.pointCountAfterCulling = pointCountAfterCulling;
        }
        
        
    public boolean tooManyPoints(int currentPointCount)
        {
        return currentPointCount >maxPointCount;
        }
        
    // O(maxPoints)
    public static void sort(IntBag indices, int maxPoints)
        {
        boolean[] map = new boolean[maxPoints];
        for(int i=0;i<indices.numObjs;i++)
            map[indices.objs[i]]=true;
        indices.clear();
        for(int i=0;i<maxPoints;i++)
            if(map[i])
                indices.add(i);
        }
        
    public IntBag cull(double[] xValues, boolean sortedOutput)
        {
        return cull(xValues, reusableIntBag, sortedOutput);
        }
        
    public IntBag cull(double[] xValues, IntBag droppedIndices, boolean sortOutput)
        {
        return cull(xValues, pointCountAfterCulling, droppedIndices, sortOutput);
        }
    //this ignores size!!!
    public IntBag cull(double[] xValues, int size, IntBag droppedIndices, boolean sortOutput)
        {
        IntBag bag =  cullToSize(xValues, size, droppedIndices);
        if(sortOutput)
            sort(bag, xValues.length);
        return bag;
        }
        
    /* I expect the xValues to be sorted! 
     * I don't sort the output, but I offer <code>sort</code> to do it.
     * 
     * This works by finding the element that would leave the 
     * smallest gap (if dropped), then repeat.  This means I won't touch
     * the first and last element.
     **/
    static public IntBag cullToSize(double[] xValues, int size, IntBag droppedIndices)
        {
        droppedIndices.clear();
        int pointsToDrop = xValues.length-size;
        if(pointsToDrop<=0)
            return droppedIndices;
        if(xValues.length<=2)
            {
            System.err.println("Your plot can't hold more than 2 points? Really?");
            //I shouldn't be in this situation. I'll just drop something and get out.
            for(int i=0;i<pointsToDrop;i++)
                droppedIndices.add(i);
            return droppedIndices;
            }
        if(pointsToDrop==1)
            {
            //no need for a heap, just the best point to drop
            //i.e. the points with the neighbors closest together.
            double bestGapSumSoFar = Double.MAX_VALUE;
            int index = -1;
            double lastX = xValues[1];
            double lastGap = xValues[1]-xValues[0];
            for(int i=2;i<xValues.length;i++)
                {
                double xi = xValues[i];
                double gap = xi-lastX;
                lastX = xi;
                double gapSum = gap+lastGap;
                lastGap = gap;
                if(gapSum<bestGapSumSoFar)
                    {
                    index = i-1;
                    bestGapSumSoFar = gapSum;
                    }
                }
            droppedIndices.add(index);
            return droppedIndices;
            }
        //now for the main case: let's make the heap
        Heap h = new Heap(xValues);
        for(int i=0;i<pointsToDrop;i++)
            {
//                      h.printHeap();
            droppedIndices.add(h.extractMin().xValueIndex);
//                      System.out.println("Delete "+droppedIndices.objs[i]);
            }
                
//              System.out.print("Xvalues:");
//              for(int i=0;i<xValues.length;i++)
//                      System.out.print("\t"+xValues[i]);
//              System.out.print("\nDropIndices:");
//              for(int i=0;i<droppedIndices.numObjs;i++)
//                      System.out.print("\t"+droppedIndices.objs[i]);
        return droppedIndices;
        }
        
    static class Record implements Comparable
        {
        int xValueIndex;
        double leftGap, rightGap;
        double key;//leftGap+rightGap.
        Record leftRecord, rightRecord;
        int heapPosition;//actually, it's the index in 1based counting.
                
//              public String toString()
//              {
//                      return "R{xi="+xValueIndex+" hp="+heapPosition+" lg="+leftGap+" rg="+rightGap+
//                      " lr="+(leftRecord==null?leftRecord:"R"+leftRecord.xValueIndex)+
//                      " rr="+(rightRecord==null?rightRecord:"R"+rightRecord.xValueIndex)+
//                      "}";
//              }
        public Record(int xValueIndex, double leftGap, double rightGap, int heapPosition)
            {
            this.xValueIndex = xValueIndex;
            this.leftGap = leftGap;
            this.rightGap = rightGap;
            this.key = leftGap+rightGap;
            this.heapPosition = heapPosition;
            }
        //I prefer to drop the point the leaves behind the smallest gap (key=leftGap+rightGap)
        //In case of a tie, I prefer to dop the first point (so I keep more of the fresh data on)
        public int      compareTo(Object o) 
            {
            Record r = (Record)o;
            double keydiff = key-r.key;
            if(keydiff==0)
                return xValueIndex - r.xValueIndex;
            else
                return keydiff<=0?-1:1;
            }
        public void setLeftGap(double lg)
            {
            leftGap = lg;
            key = leftGap+rightGap;
            }
        public void setRightGap(double rg)
            {
            rightGap = rg;
            key = leftGap+rightGap;
            }
        }
        
    static class Heap
        {
        int heapsize;
        Record[] heap;
        public Heap(double[] xValues)
            {
            this.heapsize = xValues.length-2;
            //of all the data points I can delete, the first and last are taboo.
            heap = new Record[heapsize];

            double currentX = xValues[1];
            double lastGap = currentX-xValues[0];
            if(lastGap<=0)
                throw new RuntimeException("I expect xValues in strictly increasing order.");
            Record lastRecord = null;
            for(int i=1;i<xValues.length-1;i++)
                {
                double nextX = xValues[i+1];
                double nextGap = nextX - currentX;
                if(nextGap<=0)
                    throw new RuntimeException("I expect xValues in strictly increasing order.");
                                
                Record ri = new Record(i,lastGap,nextGap,i);
                ri.leftRecord = lastRecord;
                if(lastRecord!=null)
                    lastRecord.rightRecord = ri;
                                
                lastRecord = ri;
                currentX = nextX;
                lastGap = nextGap;
                                
                heap[i-1]=ri;
                }
                        
            for( int i = heapsize/2 ; i >= 1 ; i-- )
                heapify(i);
            }
                
        public Record extractMin()
            {
            if( heapsize == 0 )
                return null;
            // remove the info
            Record result = heap[1-1];
            heap[1-1] = heap[heapsize-1];
            heap[1-1].heapPosition=1;
            heap[heapsize-1] = null;
            heapsize--;
            // rebuild heap
            if (heapsize > 1) 
                {// no need to heapify if there's only zero or one element!
                heapify(1);    
                        
                //lets update the previous and next record.
                Record leftRecord = result.leftRecord; 
                Record rightRecord = result.rightRecord;
                if(rightRecord!=null)
                    {
                    if(rightRecord.leftGap!=result.rightGap)
                        throw new RuntimeException("BUG");//TODO delete these checks
                    }
                if(leftRecord!=null)
                    {
                    if(leftRecord.rightGap!=result.leftGap)
                        throw new RuntimeException("BUG");//TODO delete these checks
                    //leftRecord.rightGap+=result.rightGap;
                    leftRecord.setRightGap(result.key);
                    leftRecord.rightRecord = result.rightRecord;
                    heapify(leftRecord.heapPosition);
                    }
                if(rightRecord!=null)
                    {
                    rightRecord.setLeftGap(result.key);
                    rightRecord.leftRecord = result.leftRecord;
                    heapify(rightRecord.heapPosition);
                    }
                        
                }
            return result;
            }
                
        void printHeap()
            {
            System.out.println("------------------------------------");
            for(int i=0;i<heapsize;i++)
                System.out.println(heap[i]);
            }
            
        void heapify(int i)
            {
            while(true)
                {
                int l = 2*i;
                int r = 2*i+1;
                int smallest;
                if( l <= heapsize && heap[l-1].compareTo(heap[i-1])<0)
                    smallest = l;
                else
                    smallest = i;
                if( r <= heapsize && heap[r-1].compareTo(heap[smallest-1]) < 0)
                    smallest = r;
                if( smallest != i )
                    {
                    // swap records
                    Record tmp = heap[i-1];
                    heap[i-1] = heap[smallest-1];
                    heap[smallest-1] = tmp;
                        
                    heap[i-1].heapPosition=i;
                    heap[smallest-1].heapPosition = smallest;
                        
                    // recursive call.... :)
                    i = smallest;
                    }
                else
                    return;
                }
            }
        }
        
    }
