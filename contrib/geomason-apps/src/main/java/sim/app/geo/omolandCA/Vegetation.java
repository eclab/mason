package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
import sim.engine.SimState;
import sim.engine.Steppable;

public class Vegetation implements Steppable {

	// hold grass and other vegetation
	// go through parcel and make change
	// http://ilri.org/InfoServ/Webpub/fulldocs/X5548E/X5548E03.HTM

	// regression analysi of NDVI with Rainfall

//    nDvi = aRain2 + bRain + c

	private void growGrass(final Parcel parcel, final Landscape ls) {
		// double rainc = ls.weeklyRainfall.field[parcel.getX()][parcel.getY()] ;
		final double treeComp = ls.elevTreeFactor.field[parcel.getX()][parcel.getY()];
		double rainc = (parcel.getSoilMoisture() + 0.7 * (treeComp * parcel.getSoilMoisture()))
				- ls.params.vegetationParam.getAverageRainfallGrowthCutOFF();

		double grwothF = 1.0;
		// double ndviP = ls.ndviresidual.field[parcel.getX()][parcel.getY()];//
		// residual

		if (rainc <= 0) {
			rainc = 0;
			grwothF = 0.5;
		}

		//
		final double rf = ((rainc * rainc * rainc * 0.0012) + (rainc * rainc * -0.054) + (0.58 * rainc) - 0.83);
		final double g = parcel.getGrass() / ls.params.vegetationParam.getMaxVegetationPerHectare();
		final double vegGrowthRate = grwothF * treeComp * ls.params.vegetationParam.getBaseGrowthRateContorler() * rf
				* (0.009 / (0.009 + g * g * g));

		final double newGrass = parcel.getGrass() * vegGrowthRate;

		if (treeComp > 1) {
			System.out.println("tc:  " + treeComp);

		}

		double tempVegetation = parcel.getGrass() + newGrass; // grassBIOMASS *

		// river impact
		if (parcel.getIsIrrigatedFarm() == true && tempVegetation < ls.params.vegetationParam.getMinVegetation() * 3) {
			tempVegetation = 5 * ls.params.vegetationParam.getMinVegetation();
		}

		// keep the low to the minumim
		if (tempVegetation < ls.params.vegetationParam.getMinVegetation()) {// minimum influnced by soil quality
			tempVegetation = ls.params.vegetationParam.getMinVegetation()
					+ 0.1 * ls.params.vegetationParam.getMinVegetation();

		}
//        // keep the high to the max
		if (tempVegetation > ls.params.vegetationParam.getMaxVegetationPerHectare()) {

			tempVegetation = ls.params.vegetationParam.getMaxVegetationPerHectare();
		}

//     assert tempVegetation >= ls.params.vegetationParam.getMaxVegetationPerHectare() : "Veg: " + tempVegetation;

		parcel.setGrass(tempVegetation);
		ls.vegetationPortrial.field[parcel.getX()][parcel.getY()] = tempVegetation
				/ ls.params.vegetationParam.getMaxVegetationPerHectare();

	}

	public void growGrassAllParcel(final Landscape ls) {

		for (final Object o : ls.allParcels) {
			final Parcel parcel = (Parcel) o;

			if (parcel.getLanduseType() <= ls.RIVER)
				continue;
			if (parcel.getLanduseType() == ls.FARMLAND)
				continue;

			growGrass(parcel, ls);

		}

	}

	@Override
	public void step(final SimState state) {
		final Landscape ls = (Landscape) state;
		growGrassAllParcel(ls);

	}

}
