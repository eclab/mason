package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */

//import java.util.*;
import sim.util.Bag;
import sim.util.Valuable;

public class Parcel implements Valuable {

	// biophysical quality
	// quality
	// ownership
	// type of land use
	// land use process
	// select potential land for investment
	// check if there is any household in the land
	// allocate them on some land with potential for agriculture
	// instantiate enterprises
	int xLoc;
	int yLoc;
	private double soilQuality;
	Household owner;
	private double popDensity;
	private double accessTechno;

	// private double distToRoad;
	private int landuseType;

	private double soilMoisture;

	private double grass;
	private Crop crop;
	// private Bag crops;

	private boolean prepared;
	private boolean planted;
	private boolean harvested;
	private boolean weeded;

	private int currentAmount;
	private int currenOnset;

	private int fallowPeriod;
	// private Bag farmHouseholds;
	private Bag herds;
	private boolean isOccupied;

	// private int countPlanted =0;
	private int woreda_id;
	private int weedingFrequency;
	private boolean isIrrigatedFarm;
	private boolean isFallow;
	private boolean isRented;
	public final static double minVegetation = 10;
	private int distFromRiver; // it is converted to int
	private double areaPlanted;
	private double prevMoisture;
	private double locFactor;
	private double meanAnnualRainfall;
	private int rfZone;

	// private double veg; // holds vegetation -eiter crop or grass for
	// visualization

	/*****************************
	 * Grass growth Parameters from RiftLand vegetation growth function
	 *
	 */

	/*******************************
	 * parcel characteristics 1. soil quality 2. soil fertility 3. slope 4. moisture
	 * = rain 5. land use type 6. grass growth -
	 *
	 * @param sqty
	 */

	Parcel() {
		super();
//        farmHouseholds = new Bag();
		herds = new Bag();
		prepared = false;
		weedingFrequency = 0;
		fallowPeriod = 0;
		isIrrigatedFarm = false;
		isFallow = false;
		isRented = false;
		isOccupied = false;

//        countHarvested =0;
//        countWeeded =0;
//        crops = new Bag();
	}

	public void setSoilQuality(final double sqty) {
		soilQuality = sqty;
	}

	public double getSoilQuality() {
		return soilQuality;
	}

	public void setPopulationDensity(final double density) {
		popDensity = density;
	}

	public double getPopulationDensity() {
		return popDensity;
	}

	// rain or irigation
	public void setSoilMositure(final double sm) {
		soilMoisture = sm;
	}

	public double getSoilMoisture() {
		return soilMoisture;
	}

	public void setPrevSoilMositure(final double sm) {
		prevMoisture = sm;
	}

	public double getPrevSoilMoisture() {
		return prevMoisture;
	}

	public void setMeanAnnualRainfall(final double rf) {
		meanAnnualRainfall = rf;
	}

	public double getMeanAnnualRainfall() {
		return meanAnnualRainfall;
	}

	public void setLanduseType(final int type) {
		landuseType = type;
	}

	public int getLanduseType() {
		return landuseType;
	}

	public void setCurrentAmount(final int type) {
		currentAmount = type;
	}

	public int getCurrentAmount() {
		return currentAmount;
	}

	public void setCurrentOnset(final int type) {
		currenOnset = type;
	}

	public int getCurrentOnset() {
		return currenOnset;
	}

	public void setWoredaID(final int type) {
		woreda_id = type;
	}

	public int getWoredaID() {
		return woreda_id;
	}

//    public void setElevation(int type) {
//        this.elev = type;
//    }
//
//    public int getElevation() {
//        return elev;
//    }

	public void setDistanceFromRiver(final int d) {
		distFromRiver = d;
	}

	public int getDistanceFromRiver() {
		return distFromRiver;
	}

	public void setAccessToTechno(final double sm) {
		accessTechno = sm;
	}

	public double getAccessToTechno() {
		return accessTechno;
	}

	/*******************************
	 * grass grow --
	 */
	public void setGrass(final double g) {
		grass = g;
	}

	public double getGrass() {
		return grass;
	}

	public double eatGrass(final double herdDMNeed, final Landscape ls) {

		assert herdDMNeed >= 0 : "DM need: " + herdDMNeed;
		// herd eat 40-100 % of the demand if available
		double grass = this.getGrass();
		if (this.getLanduseType() == ls.FARMLAND) {
			grass = 0;
			return 0;
		}
		final double potGrassToEat = (grass) - (1.2 * ls.params.vegetationParam.getMinVegetation());

		if (herdDMNeed == 0) {
			return 0;
		} else if (potGrassToEat <= 0) {
			return 0;
		}

		final double availableVeg = 0.8 * potGrassToEat; // max grazing 80% of grass

		if (herdDMNeed >= availableVeg) {
			if ((grass - availableVeg) <= ls.params.vegetationParam.getMinVegetation()) {
				this.setGrass(ls.params.vegetationParam.getMinVegetation());
			} else {
				this.setGrass(grass - availableVeg);
			}

			return availableVeg;

		} else {

			if ((grass - herdDMNeed) <= ls.params.vegetationParam.getMinVegetation()) {
				this.setGrass(ls.params.vegetationParam.getMinVegetation());
			} else {
				this.setGrass(grass - herdDMNeed);
			}

			return herdDMNeed;
		}

	}

	/*******************************
	 * crop grow --
	 */
	public void setCrop(final Crop c) {
		crop = c;
	}

	public Crop getCrop() {
		return crop;
	}

//    public void setCrops(Bag c){
//        this.crops =c;
//    }
//
//    public Bag getCrops(){
//        return crops;
//    }
//
//    public void addCrops(Crop c){
//        crops.add(c);
//    }
//    public void remove(Crop c){
//        crops.remove(c);
//    }

	public void setFarmPrepared(final boolean farmIsPrepared) {

		prepared = farmIsPrepared;

	}
	// farmland is prepared

	public boolean getFarmPrepared() {
		return prepared;
	}

	public void setTotalAreaPlanted(final double area) {
		areaPlanted = area;
	}

	public double getTotalAreaPlanted() {
		return areaPlanted;
	}

	public void setCropHasPlanted(final boolean cropIsPlanted) {

		planted = cropIsPlanted;

	}
	// crop is plannted

	public boolean getCropHasPlanted() {
		return planted;
	}

	public void setCropHasWeeded(final boolean weeding) {

		weeded = weeding;

	}
	// crop is weeding

	public boolean getCropHasWeeded() {
		return weeded;
	}

	// weeding and any other crop management
	public void setWeedingFrequency(final int f) {
		weedingFrequency = f;

	}

	public int getWeedingFrequency() {
		return weedingFrequency;
	}

	public void setCropHasHarvested(final boolean crop) {
		harvested = crop;
	}
	// crop is harvested

	public boolean getCropHasHarvested() {
		return harvested;
	}

	public void setIsFarmRented(final boolean t) {
		isRented = t;
	}
	// crop is plannted

	public boolean getIsFarmRented() {
		return isRented;
	}

	public void setOwner(final Household h) {
		owner = h;
	}

	public Household getOwner() {
		return owner;
	}

	public void setLocFactor(final double learning) {
		locFactor = learning;
	}

	public double getLocFactor() {
		return locFactor;
	}

//
//    public void setHouseholds(Bag households) {
//        this.farmHouseholds = households;
//    }
//
//    public Bag getHouseholds() {
//        return farmHouseholds;
//    }
//
//    public void addHousehold(Household hh) {
//        // limit the number when u add
//
//        farmHouseholds.add(hh);
//    }
//
//    // remove household from parcel
//    public void removeHousehold(Household hh) {
//        farmHouseholds.remove(hh);
//    }

	public boolean isOccupied() {
		if (this.getOwner() != null) {
			return true;
		} else {
			return false;
		}
	}

	public void setIsOccupied(final boolean b) {
		isOccupied = b;
	}

	public boolean getIsOccupied() {
		return isOccupied;
	}

	public void setIsIrrigatedFarm(final boolean b) {
		isIrrigatedFarm = b;
	}

	public boolean getIsIrrigatedFarm() {
		return isIrrigatedFarm;
	}

	// farm land leaves as fallow
	public void setIsFallowFarmLand(final boolean t) {
		isFallow = t;
	}

	public boolean getIsFallowFarmLand() {

		return isFallow;
	}

	// number of years season as fallow not month???
	public void setFallowPeriodCounter(final int currentStep) {
		fallowPeriod = currentStep;
	}

	public int getFallowPeriodCounter() {
		return fallowPeriod;

	}

	public void setRFfZone(final int f) {
		rfZone = f;

	}

	public int getRFZone() {
		return rfZone;
	}

	public void setHerds(final Bag herds) {
		this.herds = herds;
	}

	public Bag getHerds() {
		return herds;
	}

	public void addHerds(final Herd herd) {
		herds.add(herd);
	}

	public void removeHerds(final Herd herd) {
		herds.remove(herd);
	}

	public void setX(final int x) {
		xLoc = x;
	}

	public int getX() {
		return xLoc;
	}

	public void setY(final int y) {
		yLoc = y;
	}

	public int getY() {
		return yLoc;
	}

	public boolean equals(final Parcel b) {
		if (b.getX() == this.getX() && b.getY() == this.getY()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean equals(final int x, final int y) {
		if (x == this.getX() && y == this.getY()) {
			return true;
		}
		return false;
	}

	// calaculate distance
	public double distanceTo(final Parcel b) {
		return Math.sqrt(Math.pow(b.getX() - this.getX(), 2) + Math.pow(b.getY() - this.getY(), 2));
	}

	public double distanceTo(final int xCoord, final int yCoord) {
		return Math.sqrt(Math.pow(xCoord - this.getX(), 2) + Math.pow(yCoord - this.getY(), 2));
	}
	// it is a mason method - which is directly refereced by GUI

	public double doubleValue() {

		return this.getLanduseType();

	}

}
