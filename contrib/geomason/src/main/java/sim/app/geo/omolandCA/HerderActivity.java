package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
import sim.util.Bag;

public class HerderActivity {
	// season

	// private Herd herds;
	// Household herder;
	// private int ScoutRange = 200;
	public final static int MINIMUMVISIONFEED = 10;
	public final static int MINIMUMVISIONMOVE = 50;
	ParcelEvaluator parcelEvaluator = new ParcelEvaluator();
	private final double herdSizeMax = 200.0;

	// labor constraint - if enough labor not assigned - producvity decrease
	public void move(final Herd herd, final int scoutDist, final int currentTime, final Landscape ls) {

		// best parcel to move
		// if the current locations have enough food, stay
		// else move the herd to a new location, then herd will eat grass around this
		// location

		// best location depends on its proximity to wight-

		final Parcel p = this.nextMoveParcel(herd, scoutDist, currentTime, ls);
		if (p == null) {
			return;
		}
		// herd.eatGrass(ls);
		herd.move(p);

		ls.herdTLU.setObjectLocation(herd, p.getX(), p.getY());

	}

	public void implementHerdActivity(final Household herder, final int currentTime, final Landscape ls) {
		if (herder.getHerd() == null || herder.getHerd().getHerdSizeTLU() <= 0) {
			return;
		}

		int dist = 0;
		if (herder.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION) {
			dist = 2 * HerderActivity.MINIMUMVISIONMOVE + ls.getRandom().nextInt(10);
		} else {
			dist = HerderActivity.MINIMUMVISIONMOVE + ls.getRandom().nextInt(5);

		}
		updateVisionParcel(herder.getHerd(), ls);

		move(herder.getHerd(), currentTime, dist, ls);

	}

	// it is better to increase vision to move far- so that agent figures out the
	// best parcel along the way
	// rather than assigning some parcel
	// cost of travel - nclude any information -- from base area
	// if adaptive agent, invest on cost of ttravel - can go far to search better
	// forage
	// if not believe the cliamte around -
	// if adaptive agent but not have enough to invet for cost - split the folks
	// may need to know where they are and should be around some distance
	// which drection to move - based on labor
	// four direction
	public Parcel nextMoveParcel(final Herd herd, final int dist, final int currTime, final Landscape ls) {

		final int x = herd.getHerdLocation().getX();
		final int y = herd.getHerdLocation().getY();

		final Bag potLoc = new Bag();
		potLoc.clear();

		ls.getNearestNeighborGrazingAreas(x, y, HerderActivity.MINIMUMVISIONFEED, potLoc);

		if (potLoc.numObjs <= 1) {
			return ((Parcel) ls.allLand.field[x][y]);

		} else {

			Parcel newLocation = null;

			final Bag bestScoreParcels = new Bag();
			bestScoreParcels.clear();
			double bestScoreSoFar = Double.NEGATIVE_INFINITY;
			int count = 1;

			// compare random 5 parcel-no need to compare each for movement
			if (potLoc.size() > 5) {
				count = 5;
			} else {
				count = potLoc.size();
			}

			potLoc.shuffle(ls.getRandom());

			for (int i = 0; i < count; i++) {
				final Parcel p = (Parcel) potLoc.objs[i];

				double parcelScore = 0;
				parcelScore = parcelEvaluator.simpleEvaluateWithVision2(herd, p, currTime, ls);

				// assert(!Double.isInfinite(parcelScore));
//        	assert(!Double.isNaN(parcelScore));

				if (parcelScore < bestScoreSoFar) {
					continue;
				}
				// else the parcel's score is at least as good as the best score so far
				if (parcelScore > bestScoreSoFar) {
					bestScoreSoFar = parcelScore;
					bestScoreParcels.clear();
					// clear the tie-bag (the bag of parcels tied for best score so far)
				}
				bestScoreParcels.add(p);
			}

			// System.out.println("Moved");
			assert (bestScoreParcels.numObjs > 0);

			int winningIndex = 0;
			if (bestScoreParcels.numObjs > 1) {
				winningIndex = ls.getRandom().nextInt(bestScoreParcels.numObjs);
			}

			newLocation = (Parcel) bestScoreParcels.objs[winningIndex];
			if (newLocation == null) {
				return ((Parcel) ls.allLand.field[x][y]);
			} else {
				return newLocation;
			}

		}

	}

	public void updateVisionParcel(final Herd herd, final Landscape ls) {

		if (herd.getPotentialVisionParcel() == null) {
			herd.setPotentialVisionParcel(herd.getHerdLocation());

		} else {
			if (herd.getHerdLocation().distanceTo(herd.getPotentialVisionParcel()) < 5
					* HerderActivity.MINIMUMVISIONMOVE) {
				if (herd.getHerdOwner().getLocation().getGrass()
						/ ls.params.vegetationParam.getMaxVegetationPerHectare() > 0.5) {
					herd.setPotentialVisionParcel(herd.getHerdOwner().getLocation());
				} else {

					herd.setPotentialVisionParcel(getNextVisionParcelByWoredaNew(herd, ls));
				}
			}
		}

	}

	public Parcel getNextVisionParcelByWoredaNew(final Herd herd, final Landscape ls) {

		Parcel newLocation = null;

		if (ls.highGrazingparcel.numObjs > 0) {
			final Bag bestScoreParcels = new Bag();
			bestScoreParcels.clear();
			double bestScoreSoFar = Double.NEGATIVE_INFINITY;

			for (final Object obj : ls.highGrazingparcel) {

				final Parcel p = (Parcel) obj;

				double parcelScore = 0;

				parcelScore = p.getGrass() / herd.getHerdLocation().distanceTo(p);

				if (parcelScore < bestScoreSoFar) {
					continue;
				}
				// else the parcel's score is at least as good as the best score so far
				if (parcelScore > bestScoreSoFar) {
					bestScoreSoFar = parcelScore;
					bestScoreParcels.clear();
					// clear the tie-bag (the bag of parcels tied for best score so far)
				}
				bestScoreParcels.add(p);
			}

			int winningIndex = 0;
			if (bestScoreParcels.numObjs > 1) {
				winningIndex = ls.getRandom().nextInt(bestScoreParcels.numObjs);
			}

			newLocation = (Parcel) bestScoreParcels.objs[winningIndex];
		}

		if (newLocation == null) {
			return ((Parcel) ls.allLand.field[herd.getHerdOwner().getLocation().getX()][herd.getHerdOwner()
					.getLocation().getY()]);
		} else {
			return newLocation;
		}

	}

//
}
