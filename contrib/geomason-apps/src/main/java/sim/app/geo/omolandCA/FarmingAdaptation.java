package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */

public class FarmingAdaptation {

	public void farmAdaptation(final int onset, final int amount, final Household household, final Parcel farm,
			final Climate climate, final Landscape ls) {
		household.setCropTypeOne(CropParameters.MAIZE_ID);
	}

	public double midValue(final double min, final double max, final double invRatio) {
		return (min + (invRatio * (max - min)));

	}

	private double moistureFactorSimple(final double current, final double actual) {

		double m = current / actual;
		if (m > 1.5) {
			m = 1.5;
		}
		return m;

	}

	private double lgpFactorSimple(final double current, final double actual) {

		double m = current / actual;
		if (m > 1.5) {
			m = 1.5;
		}
		return m;

	}

	// no need to use soil to compare which crop is good
	// since all crops use the same soil -
	// the comparison will be based on moisture + length of growing period + price
	// and yeild per hectare)
	public double MaxcropOutputIrrigation(final int cropID, final Parcel farm, final Landscape ls) {
		double expectdYield = 0;
		if (cropID == CropParameters.MAIZE_ID) {
			final double seedQ = 0.8;// this.getSeedQuality();
			// double ferInte = this.getFertilizerIntesity();

			// System.out.println("moisture: " + moisture + "lgp: "+ lgpF );

			final double minYield = CropParameters.MAIZE_MAXYIELD;
			final double maxYield = CropParameters.MAIZE_MAXYIELDHYV;
			final double yield = this.midValue(minYield, maxYield, seedQ);
			final double price = CropParameters.MAIZE_PRICE;
			final double waterReq = CropParameters.MAIZE_WATERREQ;
			double moistRatio = this.moistureFactorSimple(waterReq, waterReq);
			final double lgpRatio = this.lgpFactorSimple(CropParameters.MAIZE_LGP, CropParameters.MAIZE_LGP);

			// can not grow if it is lower than the min
			if (farm.getIsIrrigatedFarm() == true || moistRatio > 1) {
				moistRatio = 1;
			}

			expectdYield = price * yield * moistRatio * lgpRatio;// sQuality *
		}

		return expectdYield;
	}

	public double MaxcropOutputRainfall(final int cropID, final int onset, final int amount, final Household household,
			final Parcel farm, final Climate climate, final Landscape ls) {
		// if there is a need to add access to market, or other crop charactersitics, it
		// is possible to add here
		double expectdYield = 0;
		if (cropID == CropParameters.MAIZE_ID) {
			final double seedQ = 0.8;// this.getSeedQuality();
			// double ferInte = this.getFertilizerIntesity();
			final int season = household.currentSeason((int) ls.schedule.getTime());

			final double moisture = climate.potentialMoisture(amount, season, farm.getX(), farm.getY());

			double lgpF = 0;
			if (onset == ls.LATE) {
				lgpF = 0.3 + 0.4 * ls.getRandom().nextDouble();
			} else if (onset == ls.ONTIME) {
				lgpF = 0.8 + 0.4 * ls.getRandom().nextDouble();
			} else {
				lgpF = 1.0 + 0.4 * ls.getRandom().nextDouble();
			}

			// System.out.println("moisture: " + moisture + "lgp: "+ lgpF );

			final double lgpRatio = lgpF;

			final double minYield = CropParameters.MAIZE_MAXYIELD;
			final double maxYield = CropParameters.MAIZE_MAXYIELDHYV;
			final double yield = this.midValue(minYield, maxYield, seedQ);
			final double price = CropParameters.MAIZE_PRICE;
			final double waterReq = CropParameters.MAIZE_WATERREQ;
			double moistRatio = this.moistureFactorSimple(moisture, waterReq);

			// can not grow if it is lower than the min
			if (farm.getIsIrrigatedFarm() == true || moistRatio > 1) {
				moistRatio = 1;
			}

			expectdYield = price * yield * moistRatio * lgpRatio;// sQuality *
		}

		return expectdYield;

	}

}
