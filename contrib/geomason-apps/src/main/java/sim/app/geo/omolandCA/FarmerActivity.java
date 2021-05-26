package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */

public class FarmerActivity {

	private final int weedingConstant = 30;// 30 days after plant
	private final double WEEDING_INDEX = 0.00001;

	// private int SAFE_HARVEST_DURATION = 7; // 10 days before and after lgp = safe

	int MAX_DAYS_AFTER_LANDPREPARATION = 4; // 4 month after land prep
	// indexes
	// holds selected crops for the current season
	// based on the adaptive option, agent put the id of crops

	// atleast one crop needed
	double MER = 4.5;

//    public void landPreparing(Parcel p, Landscape ls) {
//
//        p.setFarmPrepared(true);
//
//    }

	// public void setCanPlantNow(boolean plant){this.canPlantNow =plant;}
	// public boolean getCanPlantNow(){return canPlantNow;}
	public void planting(final int plantingDate, final int cropID, final double area, final double labor,
			final Parcel p, final Landscape ls) {
		final Crop cp = new Crop(cropID, p, ls);
		cp.assignValues(cropID);
		p.setCrop(cp);
		p.setFallowPeriodCounter(p.getFallowPeriodCounter() + 1);
		p.setTotalAreaPlanted(area);
		cp.setCropLevel(0.1);
		cp.setYield(0.0);
		cp.setAreaPlanted(area);
		cp.setLabor(labor);
		cp.setWeedingContribution(0.0);
		p.setCropHasPlanted(true);
		p.setFarmPrepared(true);
		p.setCropHasWeeded(false);
		p.setCropHasHarvested(false);

//        cp.setCropHasPlanted(true);
		cp.setPlantingDate(plantingDate);
		cp.setWeedingDate(plantingDate + (weedingConstant));
		cp.setHarvestingDate(plantingDate + cp.getLengthGrowthPeriod());

		ls.crops.setObjectLocation(cp, p.getX(), p.getY());
	}

	public void weeding(final Parcel p, final double laborAl) {

		// this will affect the crop growth equation

		p.setCropHasWeeded(true);
//        cp.setCropHasWeeded(true);
		p.getCrop().setWeedingContribution(WEEDING_INDEX * laborAl);
		// p.getCrop().setWeedingContribution(WEEDING_INDEX *
		// this.getFarmer().getLaborAllocatedFarming());

		// System.out.println("weeding");
	}

	public void harvesting(final Parcel p, final Crop cp, final Landscape ls) {
		cp.setYield(0);
		cp.setCropHasHarvested(true);
		p.setCropHasHarvested(true);
		p.setTotalAreaPlanted(0);
		ls.crops.remove(cp); // since on harvest we remove crop, we do not need to remember it was planted or
								// not
		p.setCrop(null);
		p.setFarmPrepared(false);
		p.setCropHasPlanted(false);

	}

	// planting date selection is conducted in two ways
	// 1. normal planting date - based on last year planting date +- 1 week
	// 2. adjusted planting date - if agent consideer this an adaptation option,
	// planting
	// date will be when there is enough rainfall
	//// land preparation-
	// change planting date - means changing the planting date based on assessing
	// reasonability well rainfall amount
	// otherwise farmers will plant on the same date as the sow last time
	// when tey see any mositure they will sow but growing is not grauranted as
	// rainfall may stop early
//
	public boolean plantingSeason(final int season, final int landPrep, final int currentTime, final Parcel farm,
			final Landscape ls) {
		// first determine potential date
		boolean isPlantingD = false;
		// int lastYearPlantingDate = 0;
		// if (climate.determinePlantingWeek(currentTime,farm.getX(), farm.getY()) ==
		// true) {
		// System.out.println("ct= "+ currentTime% 365+" lp= "+
		// this.getFarmer().getLandPrepDate());
		if (currentTime % 12 >= landPrep && currentTime % 12 < landPrep + MAX_DAYS_AFTER_LANDPREPARATION) {
			if (farm.getSoilMoisture() >= MER) {
				isPlantingD = true;
			} else {
				isPlantingD = false;
			}

		}

		return isPlantingD;
	}

	public boolean weedingSeason(final Parcel farm, final int currentTime) {

		if (farm.getCrop().getCropHasPlanted() == true && currentTime > farm.getCrop().getWeedingDate()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean harvestingSeason(final Parcel farm, final int currentTime) {

		// need to check if agent need to harvest now
		if (currentTime % 12 == farm.getCrop().getHarvestingDate()) { // this.currentSeason(currentTime)
			return true;
		} else {
			return false;
		}
	}

	// land prepare once in a season - it is a way of monitoring the start of
	// activity
	// when planting crop- follow the follwoing procedure
	// 1. identify the crop type
	// int plantingDate, Crop cp, final Climate climate, Landscape ls
	// Bag cropBag = act.implementFarmingActivity(farm, climate, ls);
	// check how many crop are there
	// means two crops
	// check crop 1
	public void doLandPreparation(final Household farmer, final int landprep, final int currentTime) {

		for (final Object o : farmer.getFarmLands()) {

			final Parcel farm = (Parcel) o;
			if (farmer.getLaborAllocatedFarming() == 0) {
				farm.setFarmPrepared(false);
				continue;
			}
			if (currentTime % 12 >= landprep && currentTime % 12 < landprep + MAX_DAYS_AFTER_LANDPREPARATION) {
				if (farm.getFarmPrepared() == false) {
					farm.setFarmPrepared(true);
				}

			}
		}

	}

	public void doPlanting(final Household farmer, final int season, final int currentTime, final int landPrep,
			final int crop1, final Landscape ls) {

		if (crop1 == CropParameters.NOCROP_ID) {
			return;
		}

		for (final Object o : farmer.getFarmLands()) {
			final Parcel farm = (Parcel) o;
			if (farm.getCropHasPlanted() == true) {
				continue;
			}
			if (plantingSeason(season, landPrep, currentTime, farm, ls) == true && farm.getFarmPrepared() == true) {
				planting(currentTime % 12, crop1, 1.0, this.laborCont(farmer, ls), farm, ls);

			}

		}

	}

	public void doWeeding(final Household farmer, final int currentTime) {

		for (final Object o : farmer.getFarmLands()) {
			final Parcel farm = (Parcel) o;
			if (farm.getCrop() == null) {
				continue;
			}
			if (weedingSeason(farm, currentTime) == true && farm.getCropHasWeeded() != true) {
				weeding(farm, farmer.getLaborAllocatedFarming());
			}
		}

	}

	public void doHarvesting(final Household farmer, final int currentTime, final Landscape ls) {
		double totalfarmIncome = 0;
		double farmInc = 0;
		farmer.setIncomeFarming(0);

		for (final Object o : farmer.getFarmLands()) {
			final Parcel farm = (Parcel) o;
			if (farm.getCrop() == null) {
				continue;
			}
			if (farm.getCropHasHarvested() == true) {
				continue;
			}

			if (harvestingSeason(farm, currentTime) == true) {

				farmInc = (farm.getCrop().getYield() * farm.getCrop().getPrice());
				if (farmInc < 0) {
					farmInc = 0;

				}

				harvesting(farm, farm.getCrop(), ls);

			}

			totalfarmIncome = totalfarmIncome + farmInc;

		}

		farmer.setIncomeFarming(totalfarmIncome);

	}

	public double laborCont(final Household farmer, final Landscape ls) {
		final double totalFarmLand = 1.0 * farmer.getFarmLands().numObjs;
		double laborFactor = 0;
		double adaptImpact = 0;
		if (farmer.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION) {
			adaptImpact = 0.2 * farmer.getClimateCAdaptationExperience();
		}

		if (totalFarmLand <= 0) {
			laborFactor = 0;
		} else {
			laborFactor = (farmer.getLaborAllocatedFarming() * ls.params.farmingParam.getProportionLaborToAreaHa()
					+ adaptImpact) / (totalFarmLand);
		}

		if (laborFactor < 0.1) {
			laborFactor = 0;
		}

		if (laborFactor > 1.2) {
			laborFactor = 1.2;
		}

		return laborFactor;

	}

	public void implementFarmingActivity(final Household farmer, final int landPrep, final int crop1,
			final int currentTime, final Landscape ls) {

		// System.out.println("c--"+act.getCropTypeOne());
		doLandPreparation(farmer, landPrep, currentTime);

		doPlanting(farmer, farmer.currentSeason(currentTime), currentTime, landPrep, crop1, ls);
		// doWeeding(currentTime);
		doHarvesting(farmer, currentTime, ls);

	}
}