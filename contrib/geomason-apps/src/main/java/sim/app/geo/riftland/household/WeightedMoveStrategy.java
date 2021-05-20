package sim.app.geo.riftland.household;

import ec.util.MersenneTwisterFast;
import java.util.List;
import java.util.logging.Level;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.World;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.app.geo.riftland.util.CachedDistance;
import sim.app.geo.riftland.util.Misc;
import sim.util.Bag;

/** An implementation of herding behavior that uses formula-based semantics.
 * 
 * @author Eric 'Siggy' Scott
 * @author Tim Gulden
 * @see HerdMover
 */
class WeightedMoveStrategy implements MoveStrategy
{
    private final Parameters params;
    
    WeightedMoveStrategy(Parameters params)
    {
        assert(params != null);
        this.params = params;
        assert(repOK());
    }

    /** Return a quality score for the given parcel
     *
     * The quality score is itself a weighted sum of five other scores.  There
     * is a score for available food, available water, how close we are to the
     * target parcel, whether we're returning home, and whether there are conflicts there.
     *
     * @param parcel that we wish to evaluate
     * @param herding Herd that is considering the parcel.
     * @param distanceToWater Distance from the herd to its current waterHole.
     *
     * @return score indicating parcel fitness
     */
    private double evaluateParcelScore(GrazableArea parcel, Herding herding, double distanceToWater)
    {
        assert(parcel != null);
        assert(herding != null);
        assert (herding.getLocation() != null) : "evaluateParcelScore called with null location...";
        assert (herding.getCurrentWaterHole() != null) : "evaluateParcelScore called with null current watering hole...";
        
        double score; // final quality score for 'parcel'

        double distanceToParcel = CachedDistance.distance(herding.getLocation().getCoordinate(), parcel.getCoordinate());
        double parcelDistanceToWater = CachedDistance.distance(parcel.getCoordinate(), herding.getCurrentWaterHole().getLocation());

        double foodScore;
        double waterScore;
        double distanceScore;

        // has value of 1 if not hungry, otherwise increases with hunger and available food
        foodScore = 1 + ((params.herding.getHerdingFoodWeight() * (herding.getScaledHunger()) * parcel.getNormalizedAvailableVeg()));
        //System.out.println( String.format("FoodScore: %f getHerdingFoodWeight: %f getScaledHunger: %f, getNormalizedAvailableVeg: %f", foodScore, params.herding.getHerdingFoodWeight(), herding.getScaledHunger(), parcel.getNormalizedAvailableVeg()));

        // has value of 1 if not thirsty, otherwise increases with thirst and being closer to a waterhole
        waterScore = 1 + ((params.herding.getHerdingWaterWeight() * (herding.getScaledThirst()) *
                ((distanceToWater - parcelDistanceToWater) / params.herding.getVisionAndMovementRange() ))); // gets normalized by vision radius

//        double myDistanceToHome = CachedDistance.distance(herding.getLocation().getCoordinate(), herding.getHousehold().getLocation().getCoordinate());
//        double parcelDistanceToHome = CachedDistance.distance(parcel.getCoordinate(), herding.getHousehold().getLocation().getCoordinate());
//        myDistanceToHome = myDistanceToHome == 0 ? 1 : myDistanceToHome; // inifinity sucks
//        homeScore = params.herding.getHerdingHomeWeight() * ((myDistanceToHome - parcelDistanceToHome) / params.herding.getVisionAndMovementRange() ); // this gets normalized by vision radius
        
        // This may need elaboration to differentiate different types of conflict
        // currently assumes that any sharing of a parcel results in conflict

//        double conflictScore = 0;
//
//        if (parcel.isOccupied())
//        {
//            conflictScore = params.herding.getHerdingConflictWeight();
//        }

        // This will not permit evaluation of the parcel you are on
        // because of div(0).  I say make them computeTarget every day.
//        distanceScore = 1 - (params.herding.getHerdingDistanceWeight() * (distanceToParcel / params.herding.getVisionAndMovementRange())); // normalied by vision
        // value decreases as you move away from the current position -- reaching 0 at the maximum movement range
        distanceScore = 1 - params.herding.getHerdingDistanceWeight() * (distanceToParcel / params.herding.getVisionAndMovementRange()); // normalied by vision

        score = foodScore * waterScore * distanceScore;
        //System.out.println( String.format("EvaluateParcel: food: %f water: %f, distance: %f, score: %f", foodScore, waterScore, distanceScore, score));
//        score = foodScore + waterScore + distanceScore; //

        return score;
    }

    /** Move the herd based on Tim's weighted decision making approach
     *
     * The agent movement operates in one of two states.  Either the agent is
     * moving around a viable watering hole, or is migrating to a new watering
     * hole.  In the former case the agent will oscillate between the current
     * watering hole and viable grazing parcels.  The agent will begin to
     * migrate if under stress, which is defined as either starving or dying
     * of thirst.
     * 
     */
    @Override
    public GrazableArea computeTarget(World world, MersenneTwisterFast random, Herding herding)
    {
        // Incrementally check adjacent parcels for best score up to
        // max herder vision. Break loop when a good parcel is found.
        // or herder vision is reached.

        // <GCB>:
        // The only reason this is here is to avoid processing duplicates (when
        // asking for parcels X away, you also get parcels X-1 away, which you
        // got last iteration).  However, it takes O(n^2) pointer comparisons
        // to prune out duplicates.
        //
        // One could avoid this by recomputing the distance [for a total of
        // O(n) arithmethic operations] Except sometimes parcels are not added
        // in currentParcels when they're range away, so they're revisited on
        // subsequent iterations (for larger range values).  I can't tell if
        // this is the intended behavior, but I'm holding back the change.
        //
        // Note that getNeighborsMaxDistance returns all parcels with x and y
        // within +/-range of current x,y.  (so you get some of the parcels
        // with euclidian distance of 2 for range=1).  so maybe I should do
        // max(abs(x-x),abs(y-y)) instead of (x-x)*(x-x)+(y-y)(y-y).

        // JKB: One factor that goes into scoring a parcel is "how much closer
        // a parcel is to a water hole than the herder's current location".
        // How do scores changes once a herder has moved?  If we combine
        // previous parcel scores with new ones, can things get out of whack?
        // Just curious.

        Bag bestScoreParcels = new Bag();

        List<GrazableArea> parcelsToSearch = Misc.sampleNearbyGrazableAreas(herding.getLocation(), params, random);//herding.getLocation().getNearestGrazableAreas();

        double bestScoreSoFar = Double.NEGATIVE_INFINITY;
        double distanceToWater = CachedDistance.distance(herding.getLocation().getCoordinate(), herding.getCurrentWaterHole().getLocation());

        for (GrazableArea p : parcelsToSearch)
        {
            double parcelScore = evaluateParcelScore(p, herding, distanceToWater);

//          assert (!Double.isInfinite(parcelScore)) : parcelScore;
            assert (!Double.isNaN(parcelScore)) : parcelScore;

            // if this is a new best score
            if (parcelScore > bestScoreSoFar)
            {
                bestScoreSoFar = parcelScore;
                bestScoreParcels.clear(); //clear the tie-bag (the bag of parcels tied for best score so far)
                bestScoreParcels.add(p);
            }
            else if (parcelScore == bestScoreSoFar)
                bestScoreParcels.add(p);
          
        }

        assert bestScoreParcels.numObjs > 0;

        int winningIndex = 0;
        if (bestScoreParcels.numObjs > 1)
        {
            winningIndex = random.nextInt(bestScoreParcels.numObjs);
        }

        GrazableArea newLocation = (GrazableArea) bestScoreParcels.objs[winningIndex];

        return newLocation;
    }
    
    public final boolean repOK()
    {
        return params != null;
    }
    
    @Override
    public String toString()
    {
        return "[WeightedMoveStrategy]";
    }
}
