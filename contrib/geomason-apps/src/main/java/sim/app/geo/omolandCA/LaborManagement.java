package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
import sim.util.Bag;

public class LaborManagement {

	private static Bag staticBagOfAdjacentParcels = new Bag();

	public void findExtraLabor(final HouseholdData h, final Landscape ls) {
		final Bag potentialLabor = new Bag();
		final Bag adjPlots = new Bag();
		final int dist = 20;

		// make sure bag is empty
		adjPlots.clear();
		potentialLabor.clear();

		// search inviduals around you

		// add neighbors
		ls.getNearestNeighborAreas(h.getLocation().getX(), h.getLocation().getY(), dist, adjPlots);
		// adjPlots = ls.allLand.getNeighborsMaxDistance(h.getLocation().xLoc,
		// h.getLocation().getY(), dist,false,staticBagOfAdjacentParcels , null, null);

		// add neighbor hh
		for (int j = 0; j < adjPlots.numObjs; j++) {
			// if not occupied && suitable for farming

			if (((Parcel) adjPlots.objs[j]).getIsOccupied() != false) {

				potentialLabor.add(((Parcel) adjPlots.objs[j]).getOwner());

			}
		}

		// remember household and amount of labor aquaired
		// use hashmap

	}

	// age code
	int minAgeFarming = 15;
	int minAgeHerding = 10;
	int minAgeOffFarm = 15;

	// activity code
	int HERDING = 1;
	int FARMING = 2;
	int OFFFARM = 3; // labor

}
