/*
    ThreadedGardener.java

    $Id$
 */
package sim.app.geo.riftland;

import sim.app.geo.riftland.parcel.GrazableArea;
import sim.engine.ParallelSequence;
import sim.engine.SimState;
import sim.engine.Steppable;



/** Spins off as many separate Gardener threads as there are processors
 */
public class ThreadedGardener implements Steppable
{
    private static final long serialVersionUID = 1L;

    /** Number of available cores */
    private final int NUMCORES;

    /** The index of the current Gardener to which we'll be adding the next
     * GrazableArea.
     * <p>
     *
     * @see #addGrazableArea(GrazableArea)
     */
    private int currentGrazableArea = 0;


    /** The Gardeners that will be allocated to their own core */
    private final Gardener [] gardeners;
    

    /** The bundle of Gardener threads */
    private ParallelSequence gardenerSequence;
    

    ThreadedGardener(Parameters params)
    {
        NUMCORES = params.system.getNumthreads();
        gardeners = new Gardener[NUMCORES];
        for (int i = 0; i < gardeners.length; i++)
        {
            gardeners[i] = new Gardener();
        }

        gardenerSequence = new ParallelSequence(gardeners, NUMCORES);
    }

    @Override
    public void step(SimState state)
    {
        gardenerSequence.step(state);
    }

    public void setWaterHoles(WaterHoles waterHoles)
    {
        for (int i = 0; i < gardeners.length; i++)
            gardeners[i].setWaterHoles(waterHoles);
    }

    /**
     *  @see World#finish()
     */
    protected void finish()
    {
        gardenerSequence.cleanup();
    }



    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();

        gardenerSequence.cleanup();
    }


    public void reset(World world)
    {
        for (int i = 0; i < gardeners.length; i++)
        {
            gardeners[i].reset();
        }
    }



    /** Add a new GrazableArea that the Gardener "tends"
     *
     * @param ga
     */
    synchronized public void addGrazableArea(GrazableArea ga)
    {
        gardeners[currentGrazableArea % NUMCORES].addGrazableArea(ga);
        this.currentGrazableArea++;
    }


}
