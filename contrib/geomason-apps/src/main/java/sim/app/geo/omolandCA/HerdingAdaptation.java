package sim.app.geo.omolandCA;

/**
 *
 * @author gmu
 */
//import java.util.*;
import sim.util.Bag;

public class HerdingAdaptation {

	// with no adaptation
	public void herdNoAdaptation(final Household h, final Landscape ls) {

		if (h.getHerd() == null || h.getHerd().getHerdSizeTLU() == 0) {
			return;
		} else {
			if (ls.getRandom().nextDouble() < 0.1) {
				doDestocking(h, ls);
				doRestocking(h, ls);
			}
		}

	}

	public void herdAdaptation(final Household h, final int onset, final int amount, final Landscape ls) {

		if (h.getHerd() == null || h.getHerd().getHerdSizeTLU() == 0) {
			return;
		} else {

			doDestocking(h, ls);
			doRestocking(h, ls);
		}

	}

	private void doDestocking(final Household herder, final Landscape ls) {
		if (herder.getCurrentOnset() == ls.LATE && herder.getCurrentAmount() == ls.BELOW_NORMAL) {
			final double hunderH = herder.getHerd().getHerdFood()
					/ (1.0 + (herder.getHerd().getHerdSizeTLU() * ls.params.herdingParam.getHerdMaxFoodStored()));
			if (herder.getClimateCAdaptationExperience() > 0.3
					&& herder.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION && hunderH < 0.3) {
				final double newSize = herder.getHerd().getHerdSizeTLU()
						* ls.params.herdingParam.getHerdDestockingProportion();
				destock(herder, herder.getHerd(), newSize, ls);
			}

		}

	}

	private void doRestocking(final Household herder, final Landscape ls) {
		if (herder.getCurrentOnset() == ls.EARLY && herder.getCurrentAmount() == ls.ABOVE_NORMAL) {
			final double hunderH = herder.getStoredCapital();
			if (herder.getClimateCAdaptationExperience() > 0.3
					&& herder.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION && hunderH > 500) {
				final double newSize = herder.getHerd().getHerdSizeTLU()
						* ls.params.herdingParam.getHerdRestockingProportion();
				this.restock(herder, herder.getHerd(), newSize, ls);
			}

		}
	}

	// public void manageHerd
	// clear if you donot have any cattle left
	// herd income increase or decrease based on their food(hunger) situation -
	// hunder level

	public void restock(final Household herder, final Herd restockHerd, final double newStock, final Landscape ls) {
		// proportion - severity dependant
		// on herd activity - can restock up to max restock - random - based on labor
		// also

		restockHerd.setHerdSizeTLU(restockHerd.getHerdSizeTLU() + newStock);
		final double capital = herder.getStoredCapital();
		herder.setStoredCapital(capital - newStock * ls.params.herdingParam.getAverageHerdPrice());

	}

	public void destock(final Household herder, final Herd destockHerd, final double destockNum, final Landscape ls) {

		double newStock = 0;
		if (destockNum > destockHerd.getHerdSizeTLU()) {

			newStock = destockHerd.getHerdSizeTLU();
		} else {
			newStock = destockHerd.getHerdSizeTLU() - destockNum;
		}

		destockHerd.setHerdSizeTLU(newStock);

		final double capital = herder.getStoredCapital();
		herder.setStoredCapital(capital + newStock * ls.params.herdingParam.getAverageHerdPrice());

	}

	public void startHerding(final Household h, final Person herder, final boolean scouting, final double newStock,
			final Landscape ls) {

		final Parcel loc = h.getLocation();
		final Herd newHerd = new Herd(h);

		newHerd.setHerdSizeTLU(newStock);
		newHerd.setHerdFood(100 + 50 * ls.getRandom().nextDouble());

		loc.addHerds(newHerd);
		ls.herdTLU.setObjectLocation(newHerd, loc.getX(), loc.getY());

	}

	public Parcel scoutRoute(final int x, final int y, final int dist, final Landscape ls) {
		final int EAST = 1;
		final int WEST = 2;
		final int NORTH = 3;
		final int SOUTH = 4;

		final int xEast = ((x + dist <= ls.allLand.getWidth() - 1) ? x + dist : ls.allLand.getWidth() - 1);
		final int yEast = y;

		final int xWest = ((x - dist >= 0) ? x - dist : 0);
		final int yWest = y;

		final int xNorth = x;
		final int yNorth = ((y + dist <= ls.allLand.getHeight() - 1) ? y + dist : ls.allLand.getHeight() - 1);

		final int xSouth = x;

		final int ySouth = ((y - dist >= 0) ? y - dist : 0);

		double totalEast = 0;
		double totalWest = 0;
		double totalNorth = 0;
		double totalSouth = 0;

		final Bag landE = new Bag();
		final Bag landW = new Bag();
		final Bag landN = new Bag();
		final Bag landS = new Bag();

		landE.clear();
		landW.clear();
		landN.clear();
		landS.clear();

		// towards east
		for (int x0 = x; x0 <= xEast; x0++) {
			if (((Parcel) ls.allLand.field[x0][yEast]).getLanduseType() == ls.GRASSLAND) {
				landE.add((ls.allLand.field[x0][yEast]));
				totalEast = totalEast + ((Parcel) ls.allLand.field[x0][yEast]).getGrass();
			}

		}
		// toward west
		for (int x0 = x; x0 <= xWest; x0++) {

			if (((Parcel) ls.allLand.field[x0][yWest]).getLanduseType() == ls.GRASSLAND) {
				((Parcel) ls.allLand.field[x0][yWest]).getLanduseType();
				landW.add((ls.allLand.field[x0][yWest]));
				totalWest = totalWest + ((Parcel) ls.allLand.field[x0][yWest]).getGrass();
			}

		}
		// System.out.println("east: -" + xEast + "south: " + ySouth);
		// parcelEvaluator.evaluate(herd, (Parcel)ls.allLand.get(xNorth, y0));
		for (int y0 = y; y0 <= yNorth; y0++) {

			if (((Parcel) ls.allLand.field[xNorth][y0]).getLanduseType() == ls.GRASSLAND) {
				landN.add((ls.allLand.field[xNorth][y0]));
				totalNorth = totalNorth + ((Parcel) ls.allLand.field[xNorth][y0]).getGrass();
			}

		}

		// toward south
		for (int y0 = y; y0 <= ySouth; y0++) {

			if (((Parcel) ls.allLand.field[xSouth][y0]).getLanduseType() == ls.GRASSLAND) {
				landS.add((ls.allLand.field[xSouth][y0]));
				totalSouth = totalSouth + ((Parcel) ls.allLand.field[xSouth][y0]).getGrass();
			}

		}
		final double[] totalBiom = { 0, 0, 0, 0, 0 };
		totalBiom[0] = 0;
		totalBiom[EAST] = totalEast;
		totalBiom[WEST] = totalWest;
		totalBiom[NORTH] = totalNorth;
		totalBiom[SOUTH] = totalSouth;

		// check the max
		double max = totalBiom[0];
		int bestScoutRout = 0;
		for (int i = 1; i < totalBiom.length; i++) {
			if (max > totalBiom[i]) {
				max = totalBiom[i];
				bestScoutRout = i;
			}
		}

		if (bestScoutRout == EAST) {

			if (landE.isEmpty() == true) {
				return ((Parcel) ls.allLand.field[x][y]);
			} else if (landE.contains((ls.allLand.field[xEast][yEast]))) {
				return (Parcel) ls.allLand.get(xEast, yEast);
			} else {
				return (Parcel) landE.objs[ls.random.nextInt(landE.numObjs)];
			}
		}

		if (bestScoutRout == WEST) {
			if (landW.isEmpty() == true) {
				return (Parcel) ls.allLand.get(x, y);
			} else if (landW.contains(ls.allLand.field[xWest][yWest])) {
				return (Parcel) ls.allLand.get(xWest, yWest);
			} else {
				return (Parcel) landW.objs[ls.random.nextInt(landW.numObjs)];
			}

		}

		if (bestScoutRout == NORTH) {
			if (landN.isEmpty() == true) {
				return (Parcel) ls.allLand.get(x, y);
			} else if (landN.contains(ls.allLand.field[xNorth][yNorth])) {
				return (Parcel) ls.allLand.get(xNorth, yNorth);
			} else {
				return (Parcel) landN.objs[ls.random.nextInt(landN.numObjs)];
			}

		} else {
			if (landS.isEmpty() == true) {

				return (Parcel) ls.allLand.get(x, y);
			} else if (landS.contains(ls.allLand.field[xSouth][ySouth])) {
				return (Parcel) ls.allLand.get(xSouth, ySouth);
			} else {
				return (Parcel) landS.objs[ls.random.nextInt(landS.numObjs)];
			}

		}

	}

}
