/*
    Gardener.java

    $Id$
 */
package riftland;

import riftland.parcel.GrazableArea;
import java.util.Collection;
import java.util.LinkedList;
import riftland.parcel.WaterHole;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;

/** Updates all the GrazableAreas with each step
 * @author chenna
 */
public class Gardener implements Steppable
{
    private static final long serialVersionUID = 1L;

    private WaterHoles waterHoles;
    /** Contains all the simulation GrazableAreas */
    private Collection<GrazableArea> grazableAreas = new LinkedList<GrazableArea>();
    

    
    public Gardener()
    {
        super();
    }

    public void setWaterHoles(WaterHoles waterHoles)
    {
        this.waterHoles = waterHoles;
    }

    /** Manage all the rain and vegetation grown for all GrazableAreas
     *
     * @param ss
     */
    @Override
    public void step(SimState ss)
    {
        World world = (World) ss;

        for (GrazableArea grazableArea : grazableAreas)
        {
            final int x = grazableArea.getX();
            final int y = grazableArea.getY();
            final int patchX = grazableArea.getRainfallPatch().x;
            final int patchY = grazableArea.getRainfallPatch().y;

            final double rainfall = world.getWeather().getDailyRainfall(patchX, patchY);
            final double currentVegetation = grazableArea.getGrazableVegetationDensity();
            rainOnWaterHole(x, y, rainfall);

            grazableArea.setGrazableVegetationDensity(getUpdatedVegetation(rainfall, grazableArea, currentVegetation, world.getParams()));
            grazableArea.setVegetationChange(currentVegetation - grazableArea.getGrazableVegetationDensity());
            world.vegetationChanges.set(x, y, grazableArea.getVegetationChange());
        }
    }

    private void rainOnWaterHole(final int x, final int y, final double rainfall)
    {
        assert(waterHoles != null);
        Bag waterHolesAtXY;
        synchronized(waterHoles.getWaterHolesGrid()) // getObjectsAtLocation() is not threadsafe!
        {
            waterHolesAtXY = waterHoles.getWaterHolesGrid().getObjectsAtLocation(x, y);
        }
        if (waterHolesAtXY != null)
        {
            assert(waterHolesAtXY.size() < 2);
            runHydrology(rainfall, (WaterHole)waterHolesAtXY.get(0));
        }
    }

    /** Add a new GrazableArea that the Gardener "tends"
     *
     * @param ga
     */
    public void addGrazableArea(GrazableArea ga)
    {
        grazableAreas.add(ga);
    }

    /** The gardener is no longer responsible for maintaining this parcel
     *
     * @param ga to be removed; presumes that it actually exists.
     */
    public void removeGrazableArea(GrazableArea ga)
    {
        if ( ! grazableAreas.remove(ga) )
        {
            World.getLogger().severe("Unable to find grazable area");
        }
    }

    /** Update the amount of water in the parcel
     *
     * Reduce the amount of water due to evaporation and add in water from
     * precipitation.
     *
     * @param rainfall for this parcel
     *
     * @see #step(SimState)
     */
    private void runHydrology(double rainfall, WaterHole waterHole)
    {
        double EVAPORATION_RATE = 0.005;//This evoporation rate concerns with the Hydrology not vegetation growth. CRC
        // experimentally turned up to 1 from .1 to see about supporting more cows
        double effectiveRainfall = rainfall * 0.5; // 10% of rain shows up as available water


        /* 150mm of rain per month is 5,000,000 liters of rain per day per km^2
        // this means a conversion factor of 33333 to go from mm/rain to liters/water
         * assume 1/3 of dailyRainfall is available for drinking
         */

        /* This also means 1mm of dailyRainfall a day means 1,000,000 litres of rain water per day per km^2
         * The conversion factor thus becomes 1,000,000 to go from mm of rain/day to litres of water/day when we consider daily rainfall
         * when we assume 1/3 of dailyRainfall is available for drinking as assumed before the conversion factor is 333333
         * -CRC */

        double water = waterHole.getWater() * (1-EVAPORATION_RATE);

        water += effectiveRainfall * waterHole.getFlow(); 
        if (waterHole.getFlow() == 50000){
            water = waterHole.getMaxWater();
        }

        // Clamp water to sane range
        water = Math.min(water, waterHole.getMaxWater());
        water = Math.max(water, 0.0);
        waterHole.setWater(water);
    }

    /**
     * Computes the updated vegetation density for a parcel (without side
     * effects).
     * 
     * @param rainfall The current amount of rainfall for this parcel
     * (i.e. from world.dailyrainfall)
     * @param grazableArea The piece of land we're growing things on.
     * @param currentVegetationDensity The current density of what we're
     * growing.  ex. Could be the grazableDensity of grazableArea, or the
     * cropDensity of its associated FarmingActivity.
     * 
     * @return The new vegetation density.
     */
    public static double getUpdatedVegetation(double rainfall, GrazableArea grazableArea, double currentVegetationDensity, Parameters params)
    {
        double res = grazableArea.getResidualNDVI();
        
        //XXX growth rate is modeled as a funtion of daily dailyRainfall here. CRC
        double growthRate = params.vegetation.getBaseGrowthRate() *
            (rainfall - ((currentVegetationDensity / params.vegetation.getTheoreticalMaxVegetationKgPerKm2()) *
             params.vegetation.getGrowthThreshold()) + (params.vegetation.getQualityMultiplier() * res));
        
        double newVegetation = currentVegetationDensity + (growthRate * currentVegetationDensity * (1 - currentVegetationDensity / params.vegetation.getTheoreticalMaxVegetationKgPerKm2()));
        
        // Boundary constraints
        newVegetation = Math.max(newVegetation, params.vegetation.getMinVegetationKgPerKm2());
        newVegetation = Math.min(newVegetation, params.vegetation.getMaxVegetationKgPerKm2());
        
        assert newVegetation >= params.vegetation.getMinVegetationKgPerKm2();
        assert newVegetation <= params.vegetation.getMaxVegetationKgPerKm2();
        assert !Double.isNaN(newVegetation) : "newVegetation: " + newVegetation;
        assert !Double.isInfinite(newVegetation) : "newVegetation: " + newVegetation;
        
        return newVegetation;
    }

    /** Reset the GrazableAreas that this Gardener manages */
    public void reset()
    {
        for (GrazableArea grazableArea : grazableAreas)
        {
            grazableArea.reset();
        }
    }
}
