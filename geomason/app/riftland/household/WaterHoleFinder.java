package sim.app.geo.riftland.household;

import ec.util.MersenneTwisterFast;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.World;
import sim.app.geo.riftland.parcel.WaterHole;
import sim.app.geo.riftland.util.CachedDistance;
import sim.app.geo.riftland.util.FitnessProportionalSelector;
import sim.app.geo.riftland.util.Pair;
import sim.util.Int2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Static algorithm for selecting a new WaterHole for a Herding Activity.
 * 
 * @author Eric 'Siggy' Scott
 */
class WaterHoleFinder
{         
    /** Choose a new WaterHole to use as a base of operations.  Returns null if
     * the currentWaterHole has no nearbyWaterHoles to choose from. */
    public static WaterHole findNewWaterHole(WaterHole currentWaterHole, Int2D homeLocation, int today, int daysTillReturn, Parameters params, MersenneTwisterFast random)
    {
        assert(currentWaterHole != null);
        //if (waterHole == null)
        //    setWaterHoleToNearest();
        
        final List<WaterHole> nearbyWaterHoles = currentWaterHole.getNearbyWaterHoles();

        // If there are no nearby watering holes, then give up.
        if (nearbyWaterHoles.isEmpty())
        {
            World.getLogger().warning("Unable to find nearby watering hole, so not setting new one.");
            return null;
        }

//        System.out.println("===========================");

        List<Pair<WaterHole, Double>> waterHolePairs = evaluateAll(nearbyWaterHoles, currentWaterHole, homeLocation, today, daysTillReturn, params, random);
        waterHolePairs = selectWaterHolePairs(waterHolePairs, 4);

        FitnessProportionalSelector<WaterHole> selector = new FitnessProportionalSelector<WaterHole>(random);

        WaterHole w = selector.select(waterHolePairs);

        // ******** This block is just for testing
        double pickedScore = -1;
        for (Pair<WaterHole, Double> p : waterHolePairs){
            if (p.getFirst() == w)
                pickedScore = p.getSecond();
        }
        double distanceScore = distanceScore(currentWaterHole, w, params);
         //if (distanceScore == 0) {
        // if (w == currentWaterHole) {
//        System.out.format("Picked: %s, %f\n", w.toString(), pickedScore);
        // }
//        //**************** 
        return w;
    }

    private static List<Pair<WaterHole, Double>> selectWaterHolePairs(List<Pair<WaterHole, Double>> waterHolePairs, int size)
    {
        int waterHolePairsSize = waterHolePairs.size();
        List<Pair<WaterHole, Double>> selectedWaterHolePairs = new ArrayList<Pair<WaterHole, Double>>();

        Collections.sort(waterHolePairs, new CompareWaterHoles());

        if (waterHolePairsSize <= size) return waterHolePairs;

        for (int i = 0; i < size; i++)  selectedWaterHolePairs.add(waterHolePairs.get(i));

//        System.out.format("input waterHoles");
//        for (Pair i : waterHolePairs) System.out.println((Double)i.getSecond());
//
//        System.out.format("selected waterHoles");
//        for (Pair i : selectedWaterHolePairs) System.out.println((Double)i.getSecond());

        return selectedWaterHolePairs;
    }

    // <editor-fold defaultstate="collapsed" desc="Evaluation">
    private static List<Pair<WaterHole, Double>> evaluateAll(List<WaterHole> waterHoles, WaterHole currentWaterHole, Int2D homeLocation, int today,
                                                             int daysTillReturn, Parameters params, MersenneTwisterFast random)
    {
        List<Pair<WaterHole, Double>> all = new ArrayList<Pair<WaterHole, Double>>();

        for (WaterHole wh : waterHoles)
            all.add(new Pair<WaterHole, Double>(wh, evaluateWaterHole(currentWaterHole, wh, homeLocation, today, daysTillReturn, params, random)));
        return all;
    }
    /** Computes a score for how attractive a WaterHole is. */
    private static double evaluateWaterHole(WaterHole currentWaterHole, WaterHole targetWaterHole, Int2D homeLocation, int today, int daysTillReturn, Parameters params, MersenneTwisterFast random)
    {
        final double foodScore = foodScore(targetWaterHole, today, random);
        final double waterScore =  waterScore(targetWaterHole, params);
        final double distanceScore = distanceScore(currentWaterHole, targetWaterHole, params);
        final double homeScore =  homeScore(homeLocation, currentWaterHole, targetWaterHole, daysTillReturn, params);
        
        double totalScore = foodScore * waterScore * distanceScore * homeScore;
        //totalScore = Math.pow(totalScore,10);
        //System.out.format("f: %9.4f, w: %9.4f, d: %9.4f, h: %9.4f, total: %9.4f\n", foodScore, waterScore, distanceScore, homeScore, totalScore);
        return totalScore;
    }
    
    /** Compute a score for ranking WaterHole's by the amount of grazable food
     *  in their vicinity. */
    private static double foodScore(WaterHole wh, int today, MersenneTwisterFast random)
    {
        return wh.getGrazingQuality(today, random);
    }
    
    private static double waterScore(WaterHole wh, Parameters params)
    {
        assert(wh.getWater() >= 0);
        assert(wh.getWater() <= wh.getMaxWater());
        double score = Math.min(1.0, wh.getWater()/params.herding.getPlentyOfWater());
        return score;
    }
    
    private static double distanceScore(WaterHole currentWaterHole, WaterHole targetWaterHole, Parameters params)
    {
        double maxMoveDistance = params.herding.getMigrationRange();
        double distance = CachedDistance.distance(currentWaterHole.getLocation(), targetWaterHole.getLocation());
        double score = 1 - distance/maxMoveDistance;
        //score = Math.pow(score, 5);
//        System.out.println(score);
        assert(score >= 1 - Math.sqrt(2)); // sqrt(2) because the set of waterholes is retrived with Manhatten distance, but here we're using Euclidean
        assert(score <= 1.0);
        return score;
    }
    
    private static double homeScore(Int2D homeLocation, WaterHole currentWaterHole, WaterHole targetWaterHole, int daysTillReturn, Parameters params)
    {
        double distanceToHome = currentWaterHole.getLocation().distance(homeLocation);
        double targetDistanceToHome = targetWaterHole.getLocation().distance(homeLocation);
        
        // This value weights how important it is to move closer to home at this time.
        double homeNeed = Math.max(1, (distanceToHome - daysTillReturn));
        double unweightedHomeScore = 1 + (distanceToHome - targetDistanceToHome)/params.herding.getMigrationRange();
//        assert(homeNeed >= 1);
//        assert(unweightedHomeScore >= -Math.sqrt(2)); // sqrt(2) because the set of waterholes is retrived with Manhatten distance, but here we're using Euclidean
//        assert(unweightedHomeScore <= Math.sqrt(2));
        return homeNeed*unweightedHomeScore;
    }


    public static class CompareWaterHoles implements Comparator<Pair>
    {
        @Override
        public int compare(Pair o1, Pair o2) {
            Double f1 = (Double) o1.getSecond();
            Double f2 = (Double) o2.getSecond();
            return f2.compareTo(f1); //Descending order
        }
    }
    
    // </editor-fold>
}
