package sim.app.geo.riftland.household;

import ec.util.MersenneTwister;
import ec.util.MersenneTwisterFast;
import sim.app.geo.riftland.Land;
import sim.app.geo.riftland.Population;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.app.geo.riftland.parcel.Parcel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Static methods for finding a new location to set up a farm.  This is used,
 * for instance, if the current parcel is too crowded and we need to restart
 * farming or split the farm we have.
 * 
 * @author Eric 'Siggy' Scott
 */
public class FarmSearch
{
    /**
     * @return True if g has at least half as much land available as this owns.
     * This makes it so that, when Household's split, the child Household can
     * have a farm as or less dense than the parent.
     */
    public static boolean enoughLand(double minimumAcceptableHectares, GrazableArea g)
    {
        return (g.getAvailableAreaInHectares()>= minimumAcceptableHectares);
    }
    
    /**
     * @param startParcel
     * @return grazableArea
     * Author: CRC
     * getNewFarmLoc() searches the neighborhood of a given parcel to find a suitable parcel to locate a newly created farming unit.
     * IF the current parcel has space to accommodate a new farm then it returns the current parcel. 
     * Otherwise, it searches the Moore neighborhood at different radiuses to find a suitable parcel to locate the newly created Farm
     */
    public static GrazableArea findNewFarmLoc(Land land, Population population, Household household, double desiredHectares,
                                              final GrazableArea startParcel, MersenneTwisterFast random, int maxDepth)
    {
        GrazableArea targetParcel = null;

        if (!enoughLand(desiredHectares, startParcel))
        {
            for (int depth = 1; depth < maxDepth; depth++)
            {
                GrazableArea newLoc = findNewFarmLoc(land, population, household, desiredHectares, startParcel, depth, random); // we will iteratively explore Moore neighborhoods of different radii to find a sutiable parcel.
                if (newLoc != null)
                {
                    targetParcel = newLoc;
                    break;
                }
            }


        } else
        {
            // Set up a farm adjacent to the parent without reducing the parent's land.
            targetParcel = startParcel;
        }

        return targetParcel;
    }

    /**
     * @param grazableArea
     * @param depth
     * @return Author: CRC 
     */
    private static GrazableArea findNewFarmLoc(Land land, Population population, Household household, double desiredHectares, GrazableArea grazableArea, int depth, MersenneTwisterFast random)
    {
        GrazableArea targetParcel = null;

        // This nested class is used to contain the x and y coordinates of candidate parcels.
        class parcelCor
        {
            public int xCor = 0;
            public int yCor = 0;

            public parcelCor(int x, int y)
            {
                xCor = x;
                yCor = y;
            }
        }
        
        // The following list will contain the coordinates of all the parcels in the Moore neighborhood with the radius equal to "depth" parameter of a given parcel.
        List<parcelCor> perimeter = new ArrayList<parcelCor>();

        int parcelX = grazableArea.getX();
        int parcelY = grazableArea.getY();

        // Add all parcels on the perimeter
        for (int i = -depth; i <= depth; i++)
        {
            perimeter.add(new parcelCor(parcelX + i, parcelY + depth));
            perimeter.add(new parcelCor(parcelX + i, parcelY - depth));
            perimeter.add(new parcelCor(parcelX + depth, parcelY + i));
            perimeter.add(new parcelCor(parcelX - depth, parcelY + i));

        }

        // Shuffling the list so that we minimize direction bias in placing the new farm.
        Collections.shuffle(perimeter, new MersenneTwister(random.nextLong()));
        
        // Now we loop though the parcels in the list to find a parcel that qualifies to be the home of newly created Farm.
        // Once we find it we break from the loop and stop searching further.
        for (ListIterator<parcelCor> it = perimeter.listIterator(); it.hasNext();)
        {
            parcelCor currentParcel = it.next();
            int x = currentParcel.xCor;
            int y = currentParcel.yCor;

            // Checking that we are conisdering only parcels within the Riftland area udner consideration. Tha last condition sees if the parcel under consideration belongs to different country than the original one.
            // We are are not considering the situation where the new farm can be placed in a different coutnry. Coutnry borders are strictly observed.
            if (y < 0 || y >= land.getHeight() || x < 0 || x >= land.getWidth() || (land.getCountry(x, y) != land.getCountry(parcelX, parcelY)))
            {
                continue;
            }

            Parcel p = (Parcel) land.getParcel(x, y);

            if (p instanceof GrazableArea) // Make sure that the parcel is of GrazableArea type.
            {

                //XXX If the farm is placed in the new parcel of different ethnicity then that should ideally lead to conflict. So this should be given further thought.
                boolean conflictFlag = false; // This flag will be used to trigger conflict
// QUICK HACK TO IGNORE ETHNICITY OF NEIGHBORS WHEN CHOOSING NEW FARM
                int totalNeighbors = 1; //0
                int sameNeighbors = 1; // 0 This keeps track of the number of parcels with the same ethnicity as that of parcel where the parent Farm is loated.
                // We see if the parcels in Moore neighborhood of the candidate parcel to determine 
                // if more than half of the parcels in the neighborhood have same ethnicity as that of the parcel where the parent Farming unit is located.
//                for (int m = x - 1; m <= x + 1; m++)
//                {
//                    for (int n = y - 1; n <= y + 1; n++)
//                    {
//                        if (n >= 0 && n < land.getHeight() && m >= 0 && m < land.getWidth())
//                        {
//                            totalNeighbors++;
//                            if (household.getCulture() == population.determineCulture(m, n))
//                            {
//                                sameNeighbors++;
//                            }
//                        }
//                    }
//                }

                if (sameNeighbors >= Math.round(totalNeighbors / 2.0)) // The criteria is met
                {
                    GrazableArea q = (GrazableArea) p;
                    if (enoughLand(desiredHectares, q))
                    {
                        targetParcel = q; // This will the parcel where the newly created Farm will be placed. 
                        break;
                    }
                }

            }

        }

        return targetParcel;
    }
}
