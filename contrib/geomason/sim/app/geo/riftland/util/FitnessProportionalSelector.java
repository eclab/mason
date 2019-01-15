package sim.app.geo.riftland.util;

import ec.util.MersenneTwisterFast;

import java.util.List;

/**
 * Fitness Proportional Selection class
 *
 * @author Habib Karbasian
 */
public class FitnessProportionalSelector<T> {

    private final MersenneTwisterFast random;
    
    public FitnessProportionalSelector(MersenneTwisterFast random)
    {
        this.random = random;
    }
    
    /**
     * Returns the type T of a given Pair from objects List based on proportion value
     * which is stored in the type Double of the Pair.
     *
     * @param objects is a List of Pairs having types T and Double
     */
    public T select(List<Pair<T, Double>> objects){
        int size = objects.size();
        Double totalFitness = 0.0;
        Double rand;

        assert(size > 0);
        for (Pair<T, Double> object : objects) {
            totalFitness += object.getSecond();
        }

        rand = random.nextDouble() * totalFitness;

        for (int i = 0; i < size - 1; i++)
        {
            if (rand < objects.get(i).getSecond())
                return objects.get(i).getFirst();
            rand = rand - objects.get(i).getSecond();
        }

        return objects.get(size - 1).getFirst();
    }
}
