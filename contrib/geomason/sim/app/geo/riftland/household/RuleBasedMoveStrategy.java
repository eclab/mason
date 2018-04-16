package riftland.household;

import ec.util.MersenneTwisterFast;
import java.awt.geom.Point2D;
import riftland.Land;
import riftland.Parameters;
import riftland.World;
import riftland.conflict.Conflict;
import riftland.parcel.GrazableArea;
import riftland.parcel.Parcel;
import riftland.parcel.WaterHole;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.IntBag;

/** An implementation of herding behavior that uses "Fast and Frugal" semantics.
 *
 * @author Eric 'Siggy' Scott
 * @author William G. Kennedy
 * @see HerdMover
 */
class RuleBasedMoveStrategy implements MoveStrategy
{ 
    private Parameters params;
    
    RuleBasedMoveStrategy(Parameters params)
    {
        assert(params != null);
        this.params = params;
        assert(repOK());
    }
    
    /** Move the herd using a "fast and frugal" approach.
     *  This method ought to be thread-safe in the sense that it does not
     *  modify any world state and does not use world's PRNG, only the one
     *  that is passed in (random).*/
    @Override
    public GrazableArea computeTarget(World world, MersenneTwisterFast rnd, Herding herding)
    {
        // XXX Should be using the model parameters here!!!!
        final int maxMove = 10;
        final int maxVision = 10;
        double thirstThreshold = 0.3;  // Above this, herder seeks water

        // Get some info about the herder
        final int maxDistance = Math.min(maxMove, maxVision);

        GrazableArea currLocation = herding.getLocation();

        GrazableArea newLocation = currLocation;     // starting with no move

        double thirst = herding.getScaledThirst();

        // Have we figured out where we're going?
        boolean found[] = new boolean[] { true };  // defined this way so I can pass by reference

//        Point2D.Double avgConflicts = findNearbyConflicts(currLocation, world.getMediator().getPrevConflictsGrid(), maxDistance);

        // First question.  Is the situation dire (herd dying of thirst or hunger)?
           // this is handled in the super/herding class 
                
        // Second question.  Am I near a conflict?
//        if (avgConflicts != null)
//        {
//            // Calculate a position in the opposite direction of the average
//            // of the conflicts
//            newLocation = findRunawayLocation(currLocation, avgConflicts,
//                                                      maxDistance, found, world.getLand(), random, herding);
//        }
//        else
        // Third question.  Do I need water?
        if (herding.getScaledThirst() >= thirstThreshold)  // Need water?
        {
//System.out.println("RuleBasedMoveStrategy>computeTarget> moving toward WH. Thirst is: " 
//                    + herding.getScaledThirst() + " hunger: " + herding.getScaledHunger()); 
            newLocation = findStepTowardWaterhole(herding.getCurrentWaterHole(), currLocation,
                                                  maxDistance, found, world, herding, rnd);
//System.out.println("RuleBasedMoveStrategy>computeTarget> from " + herding.getLocation()
//                    + " to: " + newLocation); 
        } else  // Move to best grazable land nearby, closer is better
        {
            newLocation = findBestAcceptableNearbyGrazing(currLocation, maxDistance,
                                                found, true, true, world.getLand(), herding, rnd);
        }

        // If we haven't decided to move, then don't move.
        if (!found[0])
        {
            return newLocation;
        }

       return newLocation;
    }  // _move

    /**
     * Returns a point specifying the average of all the nearby conflicts.
     * If there are no conflicts, NULL is returned.
     */
    Point2D.Double findNearbyConflicts(Parcel currLocation, SparseGrid2D conflictGrid, int range)
    {
       return null;
    }

    /** Returns a unit vector in the direction of point a to point b. */
    Point2D.Double getDirection(Point2D.Double a, Point2D.Double b)
    {
        double dist = a.distance(b);
        Point2D.Double unit = new Point2D.Double((b.x - a.x)/dist, (b.y - a.y)/dist);
        //System.out.println("unit = ("+ unit.x +","+ unit.y +")");
        //System.out.println("length(unit) = "+ unit.distance(0.0, 0.0));
        return unit;
    }

    /** Returns a nearby Parcel that provides the best food source. */
    GrazableArea findBestGrazingArea(GrazableArea center, double radius,
                                       boolean found[], boolean discountDist,
                                       boolean avoidOccupied, Land land, 
                                       Herding herding, MersenneTwisterFast rnd)
    {
        return findBestAcceptableNearbyGrazing(center, radius, found, discountDist,
                                       avoidOccupied, land, herding, rnd);
    }


    
    /** Returns a bag of parcels at square ring distance from center x,y */
    Bag getRingN (Land land, int n, int x, int y)
    {
//           staticBagOfParcels.clear();
       Bag ringParcels = new Bag();  // was = staticBagOfParcels; but that won't work with threading(?)

       if (n == 0)
       {
           if (land.getParcel(x, y) == null)
               System.out.println("getRingN>null current location (" + x + ", " + y + ")");
           else
               ringParcels.add(land.getParcel(x,y));
           return ringParcels;
       }

       // set ring limits in range
       // defaults
       // note x > to right but y > to downward!
       int xmin = x - n;
       int xmax = x + n;
       int ymin = y - n;
       int ymax = y + n;
       // limit by subArea
       if (xmin < (int) land.getSubAreaUpperLeft().getX())
           xmin = (int) land.getSubAreaUpperLeft().getX();
       if (xmax > (int) land.getSubAreaLowerRight().getX() - 1)
           xmax = (int) land.getSubAreaLowerRight().getX() - 1;
       if (ymin < (int) land.getSubAreaUpperLeft().getY())
           ymin = (int) land.getSubAreaUpperLeft().getY();
       if (ymax > (int) land.getSubAreaLowerRight().getY() - 1)
           ymax = (int) land.getSubAreaLowerRight().getY() - 1;


       //top side
       for (int i = xmin; i <= xmax; i++)
       {
           ringParcels.add(land.getParcel(i, ymax));
//               System.out.println("ring: top parcel (" + i + ", " + ymax + ")");
       }
       //right side
       for (int j = ymin + 1; j <= ymax - 1; j++)
       {
           ringParcels.add(land.getParcel(xmax, j));
//               System.out.println("ring: RHS parcel (" + xmax + ", " + j + ")");
       }
       //bottom side
       for (int i = xmin; i <= xmax; i++)
       {
           ringParcels.add(land.getParcel(i, ymin));
//               System.out.println("ring: bot parcel (" + i + ", " + ymin + ")");
       }
       //left side
       for (int j = ymin + 1; j <= ymax - 1; j++)
       {
           ringParcels.add(land.getParcel(xmin, j));
//               System.out.println("ring: LHS parcel (" + xmin + ", " + j + ")");
       }
//           System.out.println("ring: center=(" + x + ", " + y + "), ring = " + n
//                              + " found " + ringParcels.numObjs + " parcels");
       assert (!ringParcels.isEmpty()) : ringParcels;
       return ringParcels;
    }

    /**
     * Returns a nearby Parcel that provides the best food source
     * by searching closest outward.
     */
    GrazableArea findBestAcceptableNearbyGrazing(GrazableArea center, double radius,
            boolean found[], boolean discountDist,
            boolean avoidOccupied, Land land, Herding herding, MersenneTwisterFast rnd)
    {
        // search nearest outward
        int x = (int) center.getX(); //Math.max(0, Math.min((int)center.x, world.getLand().getWidth()-1));
        int y = (int) center.getY(); //Math.max(0, Math.min((int)center.y, world.getLand().getHeight()-1));

        Bag ringParcels;   // was: = staticBagOfParcels;
        double bestScoreSoFar = Double.NEGATIVE_INFINITY;
        GrazableArea bestParcelSoFar = null;
        int satisfyingCount = 0;
        // note one best and collect satisfying in this ring
        // search from center outward
        for (int n = 0; n < this.params.herding.getVisionAndMovementRange(); n++) // vision limit default 10km
        {
            // if we don't yet have any satisfying parcels, keep looking for best parcel
            // (if we found satisfying in previous ring, we'll return that list
            if (satisfyingCount == 0) 
            {
                ringParcels = getRingN(land, n, x, y);   // get bag of this ring's parcels
                assert (ringParcels.size() > 0);          // at least 1, the best or many satisfying

                if (ringParcels.size() == 1) 
                {
                    bestParcelSoFar = (GrazableArea) ((Parcel) ringParcels.get(0));
                } else // select from multiple satisfying parcels
                {
                    ringParcels.shuffle(rnd);   // shuffle to randomize final selection
                    int bestIndex = 0;
                    // System.out.println("herdingRuleBased>checking rings " + ringParcels.size() + " parcels");
                    for (int i = 0; i < ringParcels.size(); i++) 
                    {
                        // ensure have parcels to consider
                        if (ringParcels.get(i) != null) 
                        {
                            // get score for this parcel
                            Parcel p = (Parcel) ringParcels.get(i);
                            if (p.isNavigable()) // test for grazable land
                            {
                                GrazableArea g = (GrazableArea) p;
                                double parcelScore = g.getNormalizedAvailableVeg();
                                assert (!Double.isInfinite(parcelScore)) : parcelScore;
                                assert (!Double.isNaN(parcelScore)) : parcelScore;

                                // update best so far for this ring
                                if (parcelScore > bestScoreSoFar) 
                                {
                                    bestScoreSoFar = parcelScore;
                                    bestIndex = i;
                                    bestParcelSoFar = g;
                                }

                                // note if parcel is satisfying
                                if (g.getGrazableVegetationDensity()
                                        > ((params.herding.getTLUFoodMax() * herding.getHerdSize()) - herding.getHerdFood())) {
                                    satisfyingCount++;
                                }
                            } // grazable test
                        } // ring results 1 or multiple satisfying to choose from
                    } // end ring
                } // end loop testing one ring
            }  // endsatisfying count ==0
        } // end loop testing all rings

        // have the best of rings out until parcel(s) satify herd's hunger
        // if have multiple satisfying parcels, select randomly from these

        found[0] = true;
        return bestParcelSoFar;
    }

    /**
     * Returns a GrazableArea that is a step closer to (or is at) the
     * waterhole.
     */
    GrazableArea findStepTowardWaterhole(WaterHole waterhole, GrazableArea center, 
                                         double radius, boolean found[], World world, 
                                         Herding herding, MersenneTwisterFast rnd)
   {      
        Point2D.Double whPoint = new Point2D.Double(waterhole.getX(),
                                                    waterhole.getY());
        GrazableArea whParcel = waterhole.getGrazableArea();
        GrazableArea targetParcel = null; 
        Point2D.Double centerPt = new Point2D.Double(center.getX(),center.getY());  
//System.out.println("RuleBasedMoveStrategy>findStepTowardWH> current WH: " + whPoint + " radius: " + radius); 
        // if close enough, move to water hole
        if (centerPt.distance(whPoint) <= radius)
        {
            targetParcel = waterhole.getGrazableArea();
        }
        else
        {
            // we'll search for a grazable area around a point closer to the water hole
            Point2D.Double targetDir = getDirection(centerPt, whPoint);
            Point2D.Double centerOffset = new Point2D.Double(
                    targetDir.x * radius * 2.0/3.0,
                    targetDir.y * radius * 2.0/3.0);
            Point2D.Double searchCenter = new Point2D.Double(
                    centerPt.x + centerOffset.x,
                    centerPt.y + centerOffset.y);
            Parcel centerP = world.getLand().getParcel(
                    (int)(centerPt.x + centerOffset.x), 
                    (int)(centerPt.y + centerOffset.y) );
            if (centerP != null && centerP.isNavigable())
            {
               targetParcel = findBestGrazingArea((GrazableArea) centerP, radius/3.0, found, true, true, world.getLand(), herding, rnd); 
            }
            else  // oops: wanted to travel to a way point that's not grazable - punt and send directly to waterhole
                targetParcel = (GrazableArea) world.getLand().getParcel((int) whPoint.x, (int) whPoint.y);
        }

        return targetParcel;
    }



    /** Returns the Parcel within some range that has the best land. */
    GrazableArea findRunawayLocation(GrazableArea currParcel,
                                       Point2D.Double avgConflicts,
                                       double maxDistance,
                                       boolean found[], Land land, 
                                       MersenneTwisterFast rnd, Herding herding)
    {
        Point2D.Double startingPt = new Point2D.Double(currParcel.getX(), currParcel.getY());
        Point2D.Double awayDir = getDirection(avgConflicts, startingPt );
        if (Double.isNaN(awayDir.x))  // We were part of the conflict
        {
            double angle = rnd.nextDouble() * 2*Math.PI;
            awayDir.x = Math.cos(angle);
            awayDir.y = Math.sin(angle);
        }
        
        Point2D.Double searchCenter = new Point2D.Double(
                    currParcel.getX() + awayDir.x * maxDistance * 2.0/3.0,
                    currParcel.getY() + awayDir.y * maxDistance * 2.0/3.0);
        GrazableArea centerParcel = (GrazableArea) land.getParcel(
                                                   (int)searchCenter.getX(),
                                                   (int) searchCenter.getY());
        GrazableArea target = findBestGrazingArea(centerParcel, maxDistance/3.0,
                                                    found, true, true, land, herding, rnd);

        return target;
    }


    public final boolean repOK()
    {
        return params != null;
    }
}