package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
public class ParcelEvaluator {

	double foodWeight = 0.5;
	double distanceCampWeight = 0.2;
	double distanceVisionWeight = 0.3;
	double distPower = 1.4;
	double hungerPower = 2.5;
	double distThreshold = 1000;

	// evaluate heading direction
	// depends on the size of
	// heading direction -
//
	public double simpleEvaluateScout(final Herd herd, final Parcel parcel, final int currentT, final Landscape ls) {
		double score = 0;

		double foodScore = 0;

		double otherLandUseFactor = 0;
		double distanceScore = 0;

		final double hunger = herd.getHerdFood();
		double hungerFactor = herd.getHerdFood()
				/ (1.0 + (herd.getHerdSizeTLU() * ls.params.herdingParam.getHerdMaxFoodStored()));

		// System.out.println("hung: "+ hungerFactor);
		if (hungerFactor > 1) {
			hungerFactor = 1;
		}

		if (hungerFactor < 0) {
			hungerFactor = 0;
		}

		final double herdNormalizedHunger = 1.0 - Math.pow(Math.exp(-1.0 * hungerFactor), hungerPower);

		if (parcel.getLanduseType() != Landscape.GRASSLAND) {
			otherLandUseFactor = 0;

//            if (parcel.getLanduseType() == Landscape.MAGO_PARK || parcel.getLanduseType() == Landscape.OMO_PARK) {
//                otherLandUseFactor = 0.4 * ls.random.nextDouble();
//            } else {
//                otherLandUseFactor = 0;
//            }
		} else {
			otherLandUseFactor = 1.0;
		}

		final double normalizeSeasonFactor = 1.0 - seasonDistanceBaise(currentT);

		final double normalizeDistanceToCamp = this.absoluteDistance(herd.getHerdOwner().getLocation(), parcel,
				herd.getHerdLocation());

		final double grassA = herdNormalizedHunger
				* (parcel.getGrass() / ls.params.vegetationParam.getMaxVegetationPerHectare());

		foodScore = foodWeight * otherLandUseFactor * grassA;
		distanceScore = distanceCampWeight * normalizeDistanceToCamp * normalizeSeasonFactor;

		score = foodScore + distanceScore + 0.001 * ls.random.nextDouble();

		return score;
	}

	public double simpleEvaluateWithVision(final Herd herd, final Parcel parcel, final int currentT,
			final Landscape ls) {
		double score = 0;
		final double distHome = distanceCampWeight
				* this.calDistanceFactor(herd.getHerdOwner().getLocation(), parcel, ls)
				* this.mainSeaonFactor(currentT, ls);
		final double distVision = distanceVisionWeight
				* this.calDistanceFactor(herd.getPotentialVisionParcel(), parcel, ls);
		final double vegetation = foodWeight
				* (0.4 + (parcel.getGrass() / ls.params.vegetationParam.getMaxVegetationPerHectare()));

		score = distHome + distVision + vegetation;
		// score = foodScore + distanceScore + normalizeDistanceToVision;

		return score;
	}

	public double simpleEvaluateWithVision2(final Herd herd, final Parcel parcel, final int currentT,
			final Landscape ls) {
		double score = 0;
		final double distHomeP = herd.getHerdOwner().getLocation().distanceTo(parcel);
		double distHome = herd.getHerdOwner().getLocation().distanceTo(herd.getHerdLocation());
		double distLoc = 0;
		double disth = 0;
		final double vegetation = parcel.getGrass() / ls.params.vegetationParam.getMaxVegetationPerHectare();

		double dist = 0;
		double veg = 0;

		double distVision = 0;
		if (herd.getPotentialVisionParcel() == null) {
			distVision = 0;
			distLoc = 1.0;
		} else {
			distVision = herd.getPotentialVisionParcel().distanceTo(parcel);
			distLoc = herd.getPotentialVisionParcel().distanceTo(herd.getHerdLocation());
		}
		if (distLoc <= 0) {
			distLoc = 1.0;
		}

		if (distHome <= 0) {
			distHome = 1.0;
		}

		disth = dist = 0.12 / (0.12 + ((distHomeP / distHome) * (distHomeP / distHome) * (distHomeP / distHome)));
		dist = 0.12 / (0.12 + ((distVision / distLoc) * (distVision / distLoc) * (distVision / distLoc)));
		veg = 1.0 - 0.12 / (0.12 + vegetation * vegetation * vegetation);

		score = foodWeight * veg + distanceCampWeight * disth * veg + distanceVisionWeight * dist;
		// score = foodScore + distanceScore + normalizeDistanceToVision;

		return score;
	}

	private double seasonDistanceBaise(final int currenT) {

		final int currenTime = currenT % 365; // incase if not divided
		// if current time between season 1 and season 2
		final double mid = 190; // mid of season
		double seasonFactor = 0;

		if (currenTime >= Climate.START_OF_SEASONONE && currenTime <= 365) {

			seasonFactor = Math.abs(mid - currenTime) / mid;

			// <= Math.abs(currenTime - Climate.SEASONTWOID)
		} else {
			seasonFactor = 1;
		}

		if (seasonFactor < 0) {
			seasonFactor = 0;
		}

//(1.0 -(Math.pow(((current-actualMean)),2)/(1.0*Math.pow(actualMean,3))))
		return (seasonFactor); /// always > 0

	}

	private double mainSeaonFactor(final int curTime, final Landscape ls) {
		final int currenTime = curTime % 365; // incase if not divided
		// if current time between season 1 and season 2
		final double mid = Math.abs(45 - currenTime); // mid feb
		double seasonFactor = 0;

		if (mid < 15) {

			seasonFactor = 0.8 + (0.4 * ls.random.nextDouble());

			// <= Math.abs(currenTime - Climate.SEASONTWOID)
		} else {
			seasonFactor = 0.3 * ls.random.nextDouble();
		}

		return seasonFactor;

	}

	public double absoluteDistance(final Parcel visionParcel, final Parcel currentParcel, final Parcel loc) {

		final double locdis = visionParcel.distanceTo(loc) + ((2 * HerderActivity.MINIMUMVISIONMOVE) * Math.sqrt(2));
		final double distCurrent = visionParcel.distanceTo(currentParcel);
		final double dif = locdis - distCurrent;

		double distRatio = 0;
		if (dif < 0) {
			distRatio = 0;
		}

		if (dif > 1) {
			distRatio = 1;
		}

		distRatio = (dif * 1.0) / ((HerderActivity.MINIMUMVISIONMOVE * 4.0) * Math.sqrt(2.0));

		return (1.0 - distRatio);
	}

	public double calDistanceFactor(final Parcel visionParcel, final Parcel currentParcel, final Landscape ls) {
		double distCurrent = 0;
		if (visionParcel == null || currentParcel == null) {
			distCurrent = 0;
		} else {
			final double locdis = (visionParcel.distanceTo(currentParcel) * 1.0) / distThreshold;

			if (locdis > 1) {
				distCurrent = 2.0 - (1.0 + ls.random.nextDouble());
			} else {
				distCurrent = 1.0 - locdis;
			}
		}

		return distCurrent;
	}

}
