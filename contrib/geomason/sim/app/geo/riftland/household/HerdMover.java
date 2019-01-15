package sim.app.geo.riftland.household;

import ec.util.MersenneTwisterFast;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.World;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.app.geo.riftland.util.Misc;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;

/**
 * An agent that iterates through all active Herding objects and executes their
 * computeTarget strategy.  This has been separated from the other behavior in Herding so
 * that it can be parallelized.
 * 
 * @author Eric 'Siggy' Scott
 * @see Herding, MoveStrategy
 */
public class HerdMover implements Steppable
{
    private final MoveStrategy moveStrategy;
    private final Parameters params;
    private final SparseGrid2D herdingGrid;
    
    public HerdMover(Parameters params, SparseGrid2D herdingGrid)
    {
        assert(params != null);
        this.params = params;
        this.herdingGrid = herdingGrid;
        moveStrategy = (params.herding.isUseHerdingRuleBased()) ? new RuleBasedMoveStrategy(params) : new WeightedMoveStrategy(params);
        assert(repOK());
    }

    @Override
    public void step(SimState ss)
    {
        assert(ss instanceof World);
        final World world = (World)ss;
        class HerdGroup implements Runnable
        {
            private final MersenneTwisterFast localRandom;
            private final Bag herds;
            HerdGroup(Bag herds, long seed)
            {
                this.herds = herds;
                localRandom = new MersenneTwisterFast(seed); // MersenneTwisterFast is not threadsafe, so we chain PRNG's.
            }
            
            @Override
            public void run()
            {
                for (Object o : herds)
                {
                    Herding h = (Herding) o;
                    if (h.getHerdSize() > 0)
                        h.setNextLocation(moveStrategy.computeTarget(world, localRandom, h));
                }
            }
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(params.system.getNumthreads());
        List<Bag> partition = Misc.partition(herdingGrid.getAllObjects(), params.system.getNumthreads()*2);
        for (Bag herdGroup : partition)
        {
            executor.execute(new HerdGroup(herdGroup, world.random.nextLong()));
        }
        executor.shutdown();
        while(!executor.isTerminated()) { }
        assert(repOK());
    }
    
    public final boolean repOK()
    {
        return moveStrategy != null
                && params != null
                && herdingGrid != null
                && !(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(herdingGrid.getAllObjects(), Herding.class));
    }
    
    @Override
    public String toString()
    {
        return String.format("[HerdMover: MoveStrategy=%s, Params=%s, Herders=%s]", moveStrategy.toString(), params.toString(), herdingGrid.toString());
    }
}
