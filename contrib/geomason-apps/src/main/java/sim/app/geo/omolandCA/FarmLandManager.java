package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
import java.util.Iterator;

import sim.util.Bag;

public class FarmLandManager {
	// farming gradient
	// from simple (hoe farming) to intesification
	// Five ways of getting farmland

	// encroach - boolean isEncroached =false;
//        boolean isExpansion =false;
//        boolean isInherited = false;
//        boolean isRented = false;
//        boolean isPurchased=false;
	public final double TRAVEL_COST = 0.3; //
	public final double MAX_DIST_RENTER = 3.0;
	public final double RENT_PRICE = 3.0;

	public void implementNewFarm(final Household farmer, final Parcel newFarm) {

		// this.plots = new Bag ();
		farmer.addFarmLands(newFarm);
		newFarm.setOwner(farmer);

	}

	public Parcel encroachRandomNeighbor(final Parcel currentLoc, final int dist, final Landscape ls) {
		Parcel newF = null;
		final Bag potential = new Bag();
		potential.clear();

		final Bag adjacent = new Bag();
		adjacent.clear();
		ls.getNearestNeighborAreas(currentLoc.getX(), currentLoc.getY(), dist, adjacent);

		for (final Object fm : adjacent) {
			final Parcel farm = (Parcel) fm;

			if (farm.getWoredaID() > 0 && farm.isOccupied() != true && farm.getSoilQuality() > 0.5) {
				potential.add(farm);
			}

		}

		// pick one of the potential farm randomly
		if (potential.numObjs > 1) {
			newF = (Parcel) potential.objs[ls.getRandom().nextInt(potential.numObjs)];
		}
		return newF;

	}

	public Parcel encroachRandom(final Landscape ls) {

		Parcel newF = null;

		int x = ls.getRandom().nextInt(ls.allParcels.numObjs);

		while (((Parcel) ls.allParcels.objs[x]).isOccupied() != true
				&& ((Parcel) ls.allParcels.objs[x]).getSoilQuality() > 0.5) {

			x = ls.getRandom().nextInt(ls.allParcels.numObjs);

		}

		newF = (Parcel) ls.allParcels.objs[x];

		return newF;
	}

	// nearest and fertile
	public Parcel encroachNearbyRandom(final Parcel currentFarm, final double dist, final Landscape ls) {
		// randomly pick potential land that can be used for farming

		Parcel newF = null;
		int x = ls.getRandom().nextInt(ls.allParcels.numObjs);

		final double distance = calcDistance(currentFarm, (Parcel) ls.allParcels.objs[x]);

		while (((Parcel) ls.allParcels.objs[x]).isOccupied() != true
				&& ((Parcel) ls.allParcels.objs[x]).getSoilQuality() > 0.5 && distance < dist) {

			x = ls.getRandom().nextInt(ls.allParcels.numObjs);
		}

		newF = (Parcel) ls.allParcels.objs[x];

		return newF;

	}

	// expand next to any of the farm parcel you have
	// expansion is checked when planting season arrives
	public Parcel potentialIrrigableLand(final Household farmer, final Landscape ls) {

		final Bag allNeighPlot = new Bag();
		final Bag bestNewFarm = new Bag();
		final int dist = 1;

		final Bag adjPlots = new Bag();

		adjPlots.clear();

		for (final Object obj : ls.potentialIrrigableLand) {
			final Parcel potIrr = (Parcel) obj;
			if (farmer.getWealth() > TRAVEL_COST * calcDistance(farmer.getLocation(), potIrr)
					&& potIrr.isOccupied() == false) {
				allNeighPlot.add(potIrr);

			}
		}

		// best score - from HerderLand Project - Thanks Jef and Mark

		double bestScoreSoFar = Double.NEGATIVE_INFINITY;
		Parcel newFarm = null;

		for (int i = 0; i < allNeighPlot.numObjs; i++) {

			final Parcel PotentialFarm = (Parcel) allNeighPlot.objs[i];

			final double fScore = PotentialFarm.getSoilQuality();

			if (fScore < bestScoreSoFar) {
				continue;
			}

			if (fScore > bestScoreSoFar) {
				bestScoreSoFar = fScore;
				bestNewFarm.clear();
			}

			bestNewFarm.add(PotentialFarm);

		}

		if (bestNewFarm != null) {
			int winningIndex = 0;
			if (bestNewFarm.numObjs >= 1) {
				winningIndex = ls.getRandom().nextInt(bestNewFarm.numObjs);
			}

			newFarm = (Parcel) bestNewFarm.objs[winningIndex];

		}

		return newFarm;
	}

	public void shrinkFarmLand(final HouseholdData farmer, final Parcel farm) {
		farm.setOwner(null);
		farmer.removeFarmLands(farm);
	}

//
//    public void transferLand(HouseholdData father, HouseholdData child){
//        // how mcuh land to transfer?? if only one parcel??
//        Parcel oldFarm = father.getFarmLands().objs[random.r father.getFarmLands().numObjs];
//        father.setFarmPlot(null);
//        child.setFarmPlot(oldFarm);
//
//        // if bag of farmland ?? serach for
//
//    }
	// rent thelowest quality land
	private Parcel findFarmToRent(final HouseholdData farmer) {

		Parcel result = null;
		double lowest = Double.POSITIVE_INFINITY;

		for (int i = 0; i < farmer.getFarmLands().numObjs; i++) {
			final Parcel farm = (Parcel) farmer.getFarmLands().objs[i];
			final double q = farm.getSoilQuality();
			if (q < lowest) {
				result = farm;
				lowest = q;
			}
		}

		return result;
	}

	private HouseholdData findRenter(final Bag potentialRenters, final double x, final double y, final Landscape ls) {

		HouseholdData result = null;

		double lowestDistance = MAX_DIST_RENTER;
		// double sellingPrice = Global.params.getValue
		// (Global.params.plotSellingPrice);

		final Iterator iter = potentialRenters.iterator();
		while (iter.hasNext()) {
			final HouseholdData other = (HouseholdData) iter.next();
			final double otherWealth = other.getWealth();
			final double dx = x - other.getLocation().xLoc;
			final double dy = y - other.getLocation().yLoc;
			final double distance = Math.sqrt(dx * dx + dy * dy);
			if (distance < lowestDistance && otherWealth >= RENT_PRICE) {
				result = other;
				lowestDistance = distance;
			}
		}
		return result;
	}

	public void rentFarmPlot(final HouseholdData owner, final Bag potentialBuyers, final Landscape ls) {

		final Parcel farmToRent = findFarmToRent(owner);

		if (farmToRent == null) {
			throw new java.lang.IllegalStateException("no farm to rent");
		}
		final double x = farmToRent.getX();
		final double y = farmToRent.getY();

		final HouseholdData renter = findRenter(potentialBuyers, x, y, ls);
		// this.removePlot(plotToRent); - change id to rent
		if (renter == null) {
			// farmToRent.setLandUse (Global.params.landUseDiscarded);
		} else {
//          double sellingPrice = Global.params.getValue (Global.params.plotSellingPrice);
//          double buyerWealth = buyer.getWealth ();
//          this.wealth = this.wealth + sellingPrice;
//          buyerWealth = buyerWealth - sellingPrice;
//          buyer.setWealth (buyerWealth);
//          buyer.addPlot (plotToSell);
		}
	}

	public void purchaseFarmLand(final HouseholdData buyer) {
		// seller will be insitution
	}

	public double calcDistance(final Parcel fromParcel, final Parcel toParcel) {
		return Math.sqrt(Math.pow((toParcel.getX() - fromParcel.getX()), 2)
				+ Math.pow((toParcel.getY() - fromParcel.getY()), 2));
	}
}
