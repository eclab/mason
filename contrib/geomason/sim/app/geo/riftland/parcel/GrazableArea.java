/*
 * GrazableArea.java
 *
 * $Id: GrazableArea.java 1967 2013-08-01 16:31:41Z tim $
 * 
 */
package sim.app.geo.riftland.parcel;

import java.util.ArrayList;
import java.util.List;
import sim.app.geo.riftland.Land;
import sim.app.geo.riftland.household.Farming;
import sim.app.geo.riftland.household.Herding;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.ThreadedGardener;
import sim.util.Bag;

/** Represents a 1km^2 area of grazable land */
public class GrazableArea extends Parcel
{
    // <editor-fold defaultstate="collapsed" desc="Fields">
    private final Parameters params;
    private final Land land;
    
    /** NDVI residual value;
     *
     * Represents the "quality" of this parcel -- ex soil quality,
     * incline, etc.  May be positive or negative.
     */
    final double ndviResidualValue;

    /** NDVI value as measured from data */
    final double ndviValue;

    private static final long serialVersionUID = 1L;

    /** There may be one or more herding activities in this parcel. */
    private volatile List<Herding> herdingActivitiesAtThisLocation = new ArrayList<Herding>();


    /** A grazable parcel can have more than one farming activity */
    private volatile List<Farming> farmingActivitiesAtThisLocation = new ArrayList<Farming>();

    /** The amount of land allocated to farmers in the parcel -- should only be
     * accessed via adjustLandClaim() */
    private double farmedLandinHectares;
    
    /** Cached container of nearby GrazableAreas
     * <p>
     * Computing the nearest grazing areas happens often and is slow.  Therefore
     * we cache the nearest grazing areas after the first time they're requested.
     */
    private Bag nearbyGrazableAreas;

    /* Do we have a Farm located at this Grazable area? */
    private boolean isaFarmland = false;
    
    /** Amount of vegetation per square kilometer in the
     * available (unfarmed) area. */
    private volatile double grazableVegetationDensity; // XXX Review: Does this need to be volatile? -- Siggy
    
    /** Difference in vegetation from previous day */
    private volatile double vegetationChange;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Accessors">

    public double getGrazableVegetationDensity()
    {
        assert(grazableVegetationDensity >= params.vegetation.getMinVegetationKgPerKm2());
        assert(grazableVegetationDensity <= params.vegetation.getMaxVegetationKgPerKm2());
        return grazableVegetationDensity;
    }

    public void setGrazableVegetationDensity(double grazableVegetationDensity)
    {
        assert(grazableVegetationDensity >= params.vegetation.getMinVegetationKgPerKm2()) : String.format("grazableVegetationDensity: %f is too low", grazableVegetationDensity);
        assert(grazableVegetationDensity <= params.vegetation.getMaxVegetationKgPerKm2()) : String.format("grazableVegetationDensity: %f is too high", grazableVegetationDensity);
        this.grazableVegetationDensity = grazableVegetationDensity;
        updateAvailableVeg();
    }
    
    private double normalizedAvailableVeg = 0;
    /** Get the amount of vegation available for grazing.  */
    public double getNormalizedAvailableVeg() {
    	return normalizedAvailableVeg;
    }
    
    private void updateAvailableVeg() {

        double farmedLand = getFarmedLandInHectares(); // hectares of the 100 hectares in the parcel
        double grazableLand = 100 - farmedLand; // Each parcel is 100 hectares.
        double availableGrazableVegetation = (getGrazableVegetationDensity() - params.vegetation.getMinVegetationKgPerKm2()) * grazableLand * 0.01;
        
        double min = params.vegetation.getMinVegetationKgPerKm2();
        double max = params.vegetation.getTheoreticalMaxVegetationKgPerKm2();

        normalizedAvailableVeg = availableGrazableVegetation / (max - min);
    }
    
    /**
     * XXX Should we also consider household locations?
     * 
     * @return true if there is no farming and herding in this parcel, else return false
     */
    public boolean isOccupied()
    {
        if (getHerds().isEmpty() && getFarms().isEmpty())
        {
            return false;
        } else
        {
            return true;
        }
    }
    
    /** @return true since agents can move through GrazableAreas */
    @Override
    public boolean isNavigable()
    {
        return true;
    }
    
    /** @return the amount of vegetation that has changed from the previous day */
    @Override
    public double getVegetationChange()
    {
        return vegetationChange;
    }

    public void setVegetationChange(double change)
    {
        vegetationChange = change;
    }

    public boolean getIsaFarmland(){
        return isaFarmland;
    }

    public void setIsaFarmland(){
        isaFarmland = true;
    }
    
    public double getNdviValue()
    {
        return ndviValue;
    }
    
    public double getResidualNDVI()
    {
        return this.ndviResidualValue;
    }

    public double getNormalizedVeg()
    {
        return (this.getGrazableVegetationDensity() - params.vegetation.getMinVegetationKgPerKm2()) / params.vegetation.getTheoreticalMaxVegetationKgPerKm2();
    }

    public List<Herding> getHerds()
    {
        return herdingActivitiesAtThisLocation;
    }

    public void addHerd(Herding herder)
    {
        this.herdingActivitiesAtThisLocation.add(herder);
    }

    public void removeHerd(Herding herder)
    {
        this.herdingActivitiesAtThisLocation.remove(herder);
    }
    
    public List<Farming> getFarms()
    {
        return farmingActivitiesAtThisLocation;
    }

    public void addFarm(Farming farmer)
    {
        this.farmingActivitiesAtThisLocation.add(farmer);
    }

    public void removeFarm(Farming farmer)
    {
        this.farmingActivitiesAtThisLocation.remove(farmer);
    }
    
    @Override
    public double doubleValue()
    {
        return getGrazableVegetationDensity();
    }
    
    /** Calculates and returns the total number of herders and farmers on this parcel. */
    public int getPopulation() {
    	int total = 0;
    	for (Farming f : farmingActivitiesAtThisLocation) {
    		total += f.getPopulation();
    	}
    	
    	for (Herding h : herdingActivitiesAtThisLocation) {
    		total += h.getPopulation();
    	}
    	
    	return total;
    }
    
    /** Returns all the GrazableAreas within params.herding.visionAndMovementRange
     *  parcels of this.  Memoized.
     */
    public final Bag getNearestGrazableAreas()
    {
        if (this.nearbyGrazableAreas == null)
        {
            int distance = params.herding.getVisionAndMovementRange();
            // pre-allocate the bag as an optimization
            this.nearbyGrazableAreas = new Bag((distance + 1) * (distance + 1));
            land.getNearestGrazableAreas(getX(), getY(), distance, this.nearbyGrazableAreas);
        }

        return nearbyGrazableAreas;
    }
    
    /**
     * Get the ratio of this parcel that is used for farming.
     * @return Real value between 0 (no farming) and 1 (all farming). 
     */
    public double getFarmedLandInHectares()
    {
        assert(!(params.system.isRunExpensiveAsserts() && Math.abs(farmedLandinHectares - calculateFarmedLandInHectares()) > 1e-6)) :
        	System.out.format("farmedLandInHectares: %f != %f\n", farmedLandinHectares, calculateFarmedLandInHectares());
        return farmedLandinHectares;
    }
    
    private double calculateFarmedLandInHectares()
    {
        double total = 0.0;
        for (Farming f : getFarms())
            total += f.getHousehold().getFarmAreaInHectares();
        return total;
    }
    
    public double getAvailableAreaInHectares()
    {
        return 100.0 - getFarmedLandInHectares();
    }
    // </editor-fold>
    
    /** Claim 'demand' amount of land from this parcel. Units are hectares
     *
     * Available land is 100 hectares minus the land that is already being farmed by everyone on the parcel.  
     * If the demand is
     * less than the available land, reduce the available land
     * by that demand amount; else, reduce the vegetation to the zero.  In either case, return the actual
     * amount.
         * This can also take a negative value and be used to give up claim to land.
     *
     * @param demand is amount of land that the caller wants to add. 
         * (NOT the new farm size -- just the amount to add -- farmer has to track this!)
     *
     * @return actual amount of the farmer can have.
     */
    public double adjustLandClaim(double demand)
    {
        double availableLand = 100 - getFarmedLandInHectares();
        if (demand > availableLand)
        {
            double granted = availableLand;
            farmedLandinHectares = 100;
            return granted;
        }
        else
        {
            farmedLandinHectares = farmedLandinHectares + demand;
            return demand;
        }
    }
    
    public void debugFarmedLandArea() {

        double total = 0.0;
        for (Farming f : getFarms())
            total += f.getHousehold().getFarmAreaInHectares();
        		
        assert(Math.abs(total - farmedLandinHectares) < 1e-6);
    }
    
    public GrazableArea(Parameters params, Land land, ThreadedGardener gardener, int x, int y, int country, double ndviValue, double ndviResidualValue)
    {
        super(x,y,country);
        assert(params != null);
        assert(land != null);
        assert(gardener != null);
        assert(ndviValue >= 0);
        this.params = params;
        this.land = land;
        this.ndviValue = ndviValue;
        this.ndviResidualValue = ndviResidualValue;
        init();
        
        // Notify the Gardener of the new GrazableArea such that it
        // can manage its vegetation and dailyRainfall.
        gardener.addGrazableArea(this);
    }

    public GrazableArea(Parameters params, Land land, int x, int y)
    {
        this.land = land;
        this.params = params;
        this.ndviValue = 0;
        this.ndviResidualValue = 0;
        this.setCoordinate(x, y);
    }

    /* (Re)Set up initial state. */
    protected final void init()
    {
        // TODO There is **A LOT** of other state that needs to be reset, but
        // am not sure what those values should be.

        // Initialize vegetation based on its ndviValue, or if it's too low set it to the minimum.
        double grazableVeg = params.vegetation.getTheoreticalMaxVegetationKgPerKm2() * ndviValue;
        grazableVeg = Math.max(grazableVeg, params.vegetation.getMinVegetationKgPerKm2());
        grazableVeg = Math.min(grazableVeg, params.vegetation.getMaxVegetationKgPerKm2());
        
        setGrazableVegetationDensity(grazableVeg);
    }

    /** Reset this parcel to its pristine initial state */
    public void reset()
    {

        // Clear out any people/households/artifacts
        farmingActivitiesAtThisLocation.clear(); // XXX maybe deprecated?
        herdingActivitiesAtThisLocation.clear();

        // Re-initialize the values to initial state.
        init();
    }
    

    /** Consume 'demand' amount of vegetation from this parcel
     *
     * Available vegetation is the current vegetation less than the
     * minimum vegetation supported by this parcel.  If the demand is
     * less than the available vegetation, reduce the available vegetation
     * by that demand amount; else, reduce the vegetation to the minimum
     * supportable amount for this parcel.  In either case, return the actual
     * consumed amount.
     *
     * @param demand is amount of vegetation that caller wants to consume
     *
     * @return actual amount of vegetation consumed
     */
    public double eatVegetation(double demand)
    {
        assert demand > 0 : "demand: " + demand;
        double amountEaten;
        double minVeg = params.vegetation.getMinVegetationKgPerKm2();
        double maxVeg = params.vegetation.getMaxVegetationKgPerKm2();
        double nowVeg = getGrazableVegetationDensity();
        double vegRange = maxVeg - minVeg;
        
        double farmedLand = getFarmedLandInHectares(); // hectares of the 100 hectares in the parcel
        double grazableLand = 100 - farmedLand; // Each parcel is 100 hectares.
        
        if (grazableLand == 0)
            return 0;
        double availableVeg = (nowVeg - minVeg) * grazableLand * 0.01;

        // as the vegetation density falls below 10% of its range, reduce the amount that the cows can eat
        // when at 10%, they get what they want, when at minimum, they get nothing
        if (nowVeg < minVeg + vegRange * 0.1){
            demand = demand * (10 * (nowVeg - minVeg))/minVeg; 
        }
        
        if (demand > availableVeg)
        {
        	// Eat all the vegetation available, leaving only the mininum
            setGrazableVegetationDensity(params.vegetation.getMinVegetationKgPerKm2()); 
//            System.out.println("VegetationParams: not enough available: " + availableVeg + ", demand: " + demand);

            amountEaten = availableVeg;
        }
        else
        {
            double densityConsumed = demand * 100/grazableLand;		// multiply by the inverse of grazableLand/100 to get from kg to kg/km^2
            setGrazableVegetationDensity(getGrazableVegetationDensity() - densityConsumed);
//            System.out.println("VegetationParams: enough available: " + availableVeg + ", demand: " + demand);
            
            amountEaten = demand;
        }
        
        assert(getGrazableVegetationDensity() >= params.vegetation.getMinVegetationKgPerKm2());
        assert(getGrazableVegetationDensity() <= params.vegetation.getMaxVegetationKgPerKm2());
        return amountEaten;
    }

    @Override
    public String toString()
    {
        return super.toString() + ": (" + getX() + "," + getY() + ") " + getGrazableVegetationDensity();
    }

}
