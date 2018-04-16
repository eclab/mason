package riftland.parcel;

import ec.util.MersenneTwisterFast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import riftland.Parameters;
import riftland.WaterHoles;
import riftland.util.Misc;
import sim.field.grid.Grid2D;
import sim.util.Bag;
import sim.util.Int2D;

/**
 *
 * @author Eric 'Siggy' Scott
 */
public class WaterHole
{
    // <editor-fold defaultstate="collapsed" desc="Fields">
    final private Parameters params;
    final private WaterHoles waterHoles;
    final private GrazableArea location;
    private List<WaterHole> nearbyWaterHoles;
    private Map<WaterHole, Double> distanceToWaterHole = new HashMap<WaterHole, Double>();
    
    /** Maximum amount of water that this parcel can have.
     * This is only nonzero if this parcel is a water hole.
     * 
     * Livestock require 20 – 30 litres per tropical livestock unit (TLU2) per day
     * (EWRA, 1976; Hofkes, 1983).
     * http://www.ilri.org/InfoServ/Webpub/Fulldocs/A_Manual/Assessment.htm
     * Assuming that a good watersource can water 100 TLU for 100 days without rain (arbitrary).
     * maxWater in the best places should be 3,000,000 liters
     */
    final private double maxWater;
    final private double flow;

    /** Current amount of water in this parcel (in liters).
     *
     * Volatile because we don't want threads caching their own copy; and under
     * Java 1.5 there's an implicit synchronize on this variable.
     */
    private volatile double water;
    
    /** Time that the grazingQuality of this WaterHole was last assessed. */
    private double lastGrazingAssessment = -1;
    /** A score between 0 and 1, representing the quality of the grazable land around the WaterHole. */
    private double grazingQuality;
    
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Accessors">
    public Int2D getLocation()
    {
        return location.getCoordinate();
    }
    
    public GrazableArea getGrazableArea()
    {
        return location;
    }

    public double getMaxWater()
    {
        return maxWater;
    }

    public double getWater()
    {
        return water;
    }
    
    /** Only stores distances to waterHoles in the List returned by
     * getNearbyWaterHoles. Any other query will result in a NPE. */
    public double getDistanceToWaterHole(WaterHole wh) throws NullPointerException
    {
        return distanceToWaterHole.get(wh);
    }

    public void setWater(double water)
    {
        this.water = water;
    }
    public double getFlow()
    {
        return flow;
    }
    
    public int getX()
    {
        assert(location.getX() >= 0);
        return location.getX();
    }
    
    public int getY()
    {
        assert(location.getY() >= 0);
        return location.getY();
    }
    //</editor-fold>
    
    public WaterHole(GrazableArea parcel, double flow, int maxWater, WaterHoles waterHoles, Parameters params)
    {
        assert(parcel.getX() >= 0);
        assert(parcel.getY() >= 0);
        assert(flow >= 0);
        assert(maxWater > 0);
        assert(waterHoles != null);
        assert(params != null);
        location = parcel;
        this.maxWater = maxWater;
        this.flow = flow;
        this.water = Math.min((flow * 20), maxWater); // initial water is 100 days of flow or the maximum whichever is less
        this.waterHoles = waterHoles;
        this.params = params;
        assert(repOK());
    }

    /** Consume 'demand' amount of water from this parcel
     *
     * Reduce the amount of water by the demand, or to zero if the demand
     * exceeds available water.
     *
     * @param demand amount of water to be consumed from parcel
     *
     * @return actual amount of water consumed
     */
    public double drinkWater(double demand)
    {
        if (demand > this.getWater())
        {
            double consumed = getWater();
            water = 0;
            return consumed;
        } else
        {
            water -= demand;
            return demand;
        }
    }
    
    /** Get a list of the nearest n waterHoles within migration range, where n
     * is given by params.maxNearbyWaterHoles.  This is done by lazy evaluation
     * -- i.e. it is cached after the first time it's called. */
    public synchronized List<WaterHole> getNearbyWaterHoles()
    {
        if (nearbyWaterHoles != null)
            return nearbyWaterHoles;
        
        // Get all WaterHoles within migrationRange.
        final Bag waterHolesBag = new Bag();
        int migrationRange = params.herding.getMigrationRange();
//        waterHoles.getWaterHolesGrid().getNeighborsMaxDistance(location.getX(), location.getY(), migrationRange, false, waterHolesBag, null, null);
        waterHoles.getWaterHolesGrid().getRadialNeighbors(location.getX(), location.getY(), migrationRange, Grid2D.BOUNDED, true, waterHolesBag, null, null);
        
        // Copy them to a list and sort in ascending order by distance from this.
        final List<WaterHole> allWaterHolesInRange = new ArrayList<WaterHole>(waterHolesBag.size());
        
        for (Object o : waterHolesBag) {
            WaterHole wh = (WaterHole)o;
            
            if (Misc.getEuclideanDistance(location.getCoordinate(), wh.getLocation()) <= migrationRange)
                allWaterHolesInRange.add((WaterHole)o);
        }
            
        Collections.sort(allWaterHolesInRange, new WaterHoleDistanceComparator()); // Running this sort also populates the distanceToWaterHole hash.
        
        // Copy the first maxNearbyWaterHoles WaterHoles into the cache.
        nearbyWaterHoles = new ArrayList<WaterHole>(params.herding.getMaxNearbyWaterHoles()) {{
            for (int i = 0; i < allWaterHolesInRange.size(); i++)
                add(allWaterHolesInRange.get(i));
        }};
        
        assert(repOK());
        return nearbyWaterHoles;
    }
    
    /** A score between 0 and 1, representing the quality of the grazable land around the WaterHole. */
    public synchronized double getGrazingQuality(int time, MersenneTwisterFast random)
    {
        assert(time >= 0);
        if (time > lastGrazingAssessment)
            assessGrazingQuality(time, random);
        assert(grazingQuality >= 0);
        assert(grazingQuality <= 1.0);
        return grazingQuality;
    }
    
    private void assessGrazingQuality(int time, MersenneTwisterFast random)
    {
        lastGrazingAssessment = time;
        List<GrazableArea> sampleNearbyLand = Misc.sampleNearbyGrazableAreas(this.getGrazableArea(), params, random);
        
        double vegetationSum = 0.0;
        for (GrazableArea g : sampleNearbyLand)
            vegetationSum += g.getGrazableVegetationDensity();
        
        double normalizedVegetationSum = Math.min(1.0, vegetationSum / (sampleNearbyLand.size() * params.vegetation.getMaxVegetationKgPerKm2()));
        assert(normalizedVegetationSum >= 0);
        assert(normalizedVegetationSum <= 1.0);
        grazingQuality = normalizedVegetationSum;
    }
    /** Ranks the WaterHole that is *closer* to this *higher* than the one that
     * is *farther*.  i.e. sorts according to distance from this in ascending
     * order. */
    private class WaterHoleDistanceComparator implements Comparator<WaterHole>
    {   
        @Override
        public int compare(WaterHole w1, WaterHole w2)
        {
            double w1Distance = lazyDistance(w1);
            double w2Distance = lazyDistance(w2);
            return w1Distance > w2Distance ? -1 : (w1Distance < w2Distance ? 1 : 0);
        }
     
        private double lazyDistance(WaterHole target)
        {
            if (!distanceToWaterHole.containsKey(target))
                distanceToWaterHole.put(target, Misc.getEuclideanDistance(location.getCoordinate(), target.getLocation()));
            return distanceToWaterHole.get(target);
        }
    }
    
    final public boolean repOK()
    {
        return params != null
                && waterHoles != null
                && location != null
                && (nearbyWaterHoles == null
                    || !(params.system.isRunExpensiveAsserts() && !Misc.containsOnlyType(nearbyWaterHoles, WaterHole.class)));
                
    }
}
