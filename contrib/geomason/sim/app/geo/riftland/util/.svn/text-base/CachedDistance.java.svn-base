package riftland.util;

import riftland.parcel.Parcel;
import sim.util.Int2D;

/**
 * Cached distance calculation. <p> Distance calculations can
 * be expensive, so we pre-calculate the distances and then just retrieve it
 * when an agent asks for it.
 * <p>
 * This is static because it exhaustively computes the cache by enumerating all
 * (delta-x, delta-y) pairs, so the data will be identical for all caches.
 * 
 * @author Eric 'Siggy' Scott
 */
public class CachedDistance
{
    private static double[][] cachedDistance = { { Math.sqrt(2.0) } };
    private static int maxDistance = 1;
    
    private CachedDistance() throws AssertionError
    { // Prevent instantiation of static class.
        throw new AssertionError();
    }
    
    /**
     * Build the distance look up table, or expand it if we've increased the 
     * max cached distance. <p> Computes and stores distances in
     * cachedDistance.  Throws an IllegalArgumentException if max is less than 1.
     *
     * @param max is the maximum distance for which we need to calculate
     */
    public static void buildLUT(int max) throws IllegalArgumentException
    {
        if (max < 1)
            throw new IllegalArgumentException("Max parameter of CachedDistance.buildLUT must be greater than zero.");
        if (max + 1 <= maxDistance) 
            return; // The table has already been computed for this max.
        
        // max + 1 because we include the current location
        maxDistance = max + 1;
        cachedDistance = new double[maxDistance][maxDistance];

        for (int x = 0; x <= max; x++)
            for (int y = 0; y <= max; y++)
                cachedDistance[x][y] = java.lang.Math.sqrt((double) (x * x + y * y));
        assert(repOK());
    }
    
    /**
     * Returns the distance between the two parcels. <p> The distance between
     * the parcels must be smaller than the maximum distance, or this will
     * return an IndexOutOfBoundsException.
     *
     * @param from
     * @param to
     * @return the Cartesian distance between the two parcels
     * @throws ArrayIndexOutOfBoundsException if the given parcels are further
     * than VisionAndMovementRange apart
     */
    public static double distance(final Int2D from, final Int2D to) throws IndexOutOfBoundsException
    {
        assert(from != null);
        assert(to != null);
        final int x = java.lang.Math.abs(from.getX() - to.getX());
        final int y = java.lang.Math.abs(from.getY() - to.getY());

        return cachedDistance[x][y];
    }
    
    /**
     * Clears the cache and returns maxDistance to 1.
     */
    public static void reset()
    {
        cachedDistance = new double[][] { { Math.sqrt(2.0) } };
        maxDistance = 1;
    }

    /** Representation invariant.  If this is false, the class is in an inconsistent state. */
    public static boolean repOK()
    {
        return  maxDistance > 0 &&
                cachedDistance != null &&
                cachedDistance.length == maxDistance &&
                cachedDistance[0].length == maxDistance &&
                tableOkay();
                
    }
    
    /** Make sure the cached data has the correct distances. */
    private static boolean tableOkay()
    {
        for (int x = 0; x < maxDistance; x++)
            for (int y = 0; y < maxDistance; y++)
                if (cachedDistance[x][y] != java.lang.Math.sqrt((double) (x * x + y * y)))
                    return false;
        return true;
    }
}
