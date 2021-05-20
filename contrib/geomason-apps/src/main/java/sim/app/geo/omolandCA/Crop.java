package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
import sim.util.Valuable;

public class Crop implements Valuable {

	private int cropID;
	private int lengthGrowthPeriod;
	private int waterRequirement;
	private double cropLevel; // biomass - plant growth
	private double maxYield;
	private double maxYieldHYV;
	private double yield;
	private double price;
	private double soilFertilityPenalty;
	private double inputCost;
	private double cropGrowthRate;
	private double areaPlanted;
	Parcel cropLand;
	private int plantingDate;
	private int weedingDate;
	private int harvestingDate;
	private double weedingContibution;
	// private double areaPlanted;
	private boolean fertilizerApplied = false;
	public double INITIALCROP_PARAMETER = 0.01; // fixed for all crop
	public double MAXIMUM_GROWTH = 1.0; // fixed for all crop
	public double GROWTHFACTORMULTIPLIER = 1.01; //
	private boolean planted;
	private boolean harvested;
	private boolean weeded;
	private int weedingFrequency;
	double ETO = 13.0;
	public final double moist_SD = 2.0; // mm/day
	private final int SAFE_HARVEST_DURATION = 1; //
	private double laborApplied = 0;
	double alpha = 0.6;
	private final int[] juleanCalanderRegular = { 1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335 };
//
//    public Crop(){
//
//    }

	public Crop(final int type, final Parcel farm, final Landscape ls) {
		this.setCropID(type);
		// assignValues(this.getCropID(), ls);
		weedingFrequency = 0;
		areaPlanted = 0;
		this.setCropLand(farm);

	}

	public void setCropID(final int id) {
		cropID = id;
	}

	public int getCropID() {
		return cropID;
	}

	public void setPlantingDate(final int d) {
		plantingDate = d;
	}

	public int getPlantingDate() {
		return plantingDate;
	}

	public void setWeedingDate(final int d) {
		weedingDate = d;
	}

	public int getWeedingDate() {
		return weedingDate;
	}

	public void setHarvestingDate(final int d) {
		harvestingDate = d;
	}

	public int getHarvestingDate() {
		return harvestingDate;
	}

	public void setWaterRequirement(final int d) {
		waterRequirement = d;
	}

	public int getWaterRequirement() {
		return waterRequirement;
	}

	public void setLengthGrowthPeriod(final int growthPeriod) {
		lengthGrowthPeriod = growthPeriod;
	}

	public int getLengthGrowthPeriod() {
		return lengthGrowthPeriod;
	}

	public void setAreaPlanted(final double area) {
		areaPlanted = area;
	}

	public double getAreaPlanted() {
		return areaPlanted;
	}

	public void setYield(final double yield) {
		this.yield = yield;
	}

	public double getYield() {
		return yield;
	}

	public void setMaxYield(final double maxProfit) {
		maxYield = maxProfit;
	}

	public double getMaxYield() {
		return maxYield;
	}

	public void setMaxYieldHYV(final double maxProfitHYV) {
		maxYieldHYV = maxProfitHYV;
	}

	public double getMaxYieldHYV() {
		return maxYieldHYV;
	}

	public void setPrice(final double price) {
		this.price = price;
	}

	public double getPrice() {
		return price;
	}

	public void setCropLevel(final double growth) {
		cropLevel = growth;
	}

	public double getCropLevel() {
		return cropLevel;
	}

	public void setCropGrowthRate(final double rate) {
		cropGrowthRate = rate;
	}

	public double getCropGrowthRate() {
		return cropGrowthRate;
	}

	public void setSoilFertilityPenalty(final double soilFertilityPenalty) {
		this.soilFertilityPenalty = soilFertilityPenalty;
	}

	public double getSoilFertilityPenalty() {
		return soilFertilityPenalty;
	}

	public void setFerilizerApplied(final boolean f) {
		fertilizerApplied = f;
	}

	public boolean getFerilizerApplied() {
		return fertilizerApplied;
	}

	public void setCropLand(final Parcel farm) {
		cropLand = farm;
	}

	public Parcel getCropLand() {
		return cropLand;
	}

	public void setInputCost(final double inputCost) {
		this.inputCost = inputCost;
	}

	public double getInputCost() {
		return inputCost;
	}

	public void setWeedingContribution(final double weeding) {
		weedingContibution = weeding;
	}

	public double getWeedingContribution() {
		return weedingContibution;
	}

	public void setLabor(final double price) {
		laborApplied = price;
	}

	public double getLabor() {
		return laborApplied;
	}

	// used to color parcel - green when planted
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

	public boolean cropHasHarvested() {
		return harvested;
	}

	private double moistureFactor(final double alpha, final Landscape ls) {
		int dateLGP = juleanCalanderRegular[(int) ls.schedule.getTime() % 12]
				- juleanCalanderRegular[(this.getPlantingDate())];

		if (dateLGP <= 0) {
			dateLGP = 1;
		}
		final double current = this.getCropLand().getSoilMoisture();
		final double actual = this.moistureRequirement(this.getCropID(), dateLGP, ETO);
		final double moist_SD_nve = this.moistureRequirement(this.getCropID(), dateLGP, ETO * alpha); // margin -ve
		final double moist_SD_pve = this.moistureRequirement(this.getCropID(), dateLGP, ETO * (1.0 + alpha)); // margin
																												// +ve

		if (actual <= 0) {
			return 0.001;
		} else if ((current >= (actual - moist_SD_nve)) && (current <= (actual + moist_SD_pve))) {
			return 1.0;

		} else if (current < (moist_SD_nve)) { // dry
			return (1.0 - (current - moist_SD_nve) / (actual));
		}
		// wet
		else {
			return (1.0 - (Math.pow(((current - moist_SD_pve)), 2) / (1.0 * Math.pow(actual, 3))));
		}
	}

	public void grow(final Landscape ls) {

		double rainfallRatio = moistureFactor(alpha, ls);
		if (rainfallRatio < 0) {
			rainfallRatio = 0;
		}

		final double growthR = this.getCropGrowthRate() * rainfallRatio;
		double tempCrop = this.getCropLevel() + (growthR);
		// laplse return 0 to 0.5 value
		if (tempCrop > MAXIMUM_GROWTH) {
			tempCrop = MAXIMUM_GROWTH;
		}
		if (tempCrop <= 0) {
			tempCrop = INITIALCROP_PARAMETER;
		}

		assert rainfallRatio >= 0 : "crop level: " + rainfallRatio;

		this.setCropLevel(tempCrop); // to make it 0-1 range

	}

	public void yield(final Landscape ls) {

		final int dateLGPC = this.getHarvestingDate();

		double lgpFactor = 0;

		if ((int) ls.schedule.getTime() % 12 >= (dateLGPC - SAFE_HARVEST_DURATION)
				&& (int) ls.schedule.getTime() % 12 <= (dateLGPC + SAFE_HARVEST_DURATION)) {
			lgpFactor = 1;
		}

		else {
			// lgpFactor = Math.abs(((int)ls.schedule.getTime() -
			// dateLGPC)/Math.pow((1.0*dateLGPC),2));
			lgpFactor = 0;
		}

		if (lgpFactor > 1) {
			lgpFactor = 1;
		}

		double yieldFactor = 0;

		if (this.getFerilizerApplied() == true) {
			yieldFactor = (0.7 * this.getCropLevel() + 0.3) * lgpFactor * this.getMaxYieldHYV() * this.getLabor();

		} else {
			yieldFactor = (0.7 * this.getCropLevel() + 0.3) * lgpFactor * this.getMaxYield() * this.getLabor();

		}

		this.setYield(yieldFactor);

		// this.setYield(tempYield * this.getAreaPlanted());

	}

	public double moistureRequirement(final int type, final int dateLGP, final double ETO) {
		double mc = 0.0;

		// double ETo = 13.0; // 9.0
		if (type == CropParameters.MAIZE_ID) {
			// -0.0996x3 + 0.5293x2 - 0.4471x + 0.4
			mc = ((Math.pow(dateLGP, 2) * -0.00009) + (0.0173 * dateLGP) + 0.02779) * ETO;
		}
		return mc;

	}
	// on initalization

	public void assignValues(final int cropID) {
		if (cropID == CropParameters.MAIZE_ID) {
			this.setLengthGrowthPeriod(CropParameters.MAIZE_LGP); // weeks
			this.setWaterRequirement(CropParameters.MAIZE_WATERREQ);
			this.setMaxYield(CropParameters.MAIZE_MAXYIELD);
			this.setMaxYieldHYV(CropParameters.MAIZE_MAXYIELDHYV);
			this.setPrice(CropParameters.MAIZE_PRICE);
			this.setSoilFertilityPenalty(CropParameters.MAIZE_SOILFERPENALITY);

			this.setInputCost(CropParameters.MAIZE_INPUTCOST);// for now
			this.setCropGrowthRate(GROWTHFACTORMULTIPLIER * 1.0 / CropParameters.MAIZE_LGP);
		} else {
			this.setLengthGrowthPeriod(0);
			this.setWaterRequirement(0);
			this.setMaxYield(0);
			this.setMaxYieldHYV(0);
			this.setPrice(0);
			this.setSoilFertilityPenalty(0);
			this.setCropGrowthRate(0);
			this.setInputCost(0);// for now
		}

	}

	public double doubleValue() {

		return this.getCropID() * 1.0;

		// return this.getCrop();
	}
}
