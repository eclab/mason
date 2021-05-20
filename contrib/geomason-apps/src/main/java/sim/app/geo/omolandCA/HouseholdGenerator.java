package sim.app.geo.omolandCA;

import sim.util.Bag;

/**
 *
 * @author ahailegiorgis
 */
public class HouseholdGenerator {

	public void addNewHouseholdAtRandomStep(final int total, final int currentTotalPop, final Landscape ls) {
		final double ticksize = LandBuilder.totalPopDensSoFar / currentTotalPop;

		double r = ls.getRandom().nextDouble() * ticksize;

		int xx = 0;
		int yy = 0;

		// locate household by pop density - marc C.- developed this method for
		// waterhole location in riftland
		for (int i = 0; i < total; i++) {
			final int hhSize = householdSize(ls);

			// If we randomly happen to pick a location that already has a
			// water hole, try again until we pick a novel location.
			do {
				while (ls.roulette[xx][yy] < r) {
					// Increment to next coordinate
					xx++;

					if (xx >= ls.allLand.getWidth()) {
						// Time to scan the next row.
						yy++;

						// If we're out of parcels, reset to the whole thing,
						// rejiggle the offset, and start over.
						if (yy >= ls.allLand.getHeight()) {
//
							r = ls.getRandom().nextDouble() * ticksize;

							yy = 0;
						}

						xx = 0;
					}
				}

				if (((Parcel) ls.allLand.field[xx][yy]).getLanduseType() != Landscape.GRASSLAND
						|| ((Parcel) ls.allLand.field[xx][yy]).getIsOccupied() != false) {
					r += ticksize;
				}

			} while (((Parcel) ls.allLand.field[xx][yy]).getLanduseType() != Landscape.GRASSLAND
					|| ((Parcel) ls.allLand.field[xx][yy]).getIsOccupied() != false);

			// (x,y) contains a viable location, so place one
			final Parcel nextAvailableParcel = (Parcel) ls.allLand.get(xx, yy);

			final double totalTLU = herdSizeBYWoreda(nextAvailableParcel.getWoredaID(), ls);

			// double totalTLU = herdSizeBYWoredaEqual(nextAvailableParcel.getWoredaID(),
			// ls);
			assert hhSize > 0 : "Total hhSize: " + hhSize;

			addHousehold(hhSize, nextAvailableParcel, totalTLU, ls);

			r += ticksize;

		}
	}

	public void addNewHouseholdAtRandomStepW(final int woredaID, final int total, final Landscape ls) {

		// locate household by pop density
		for (int i = 0; i < total; i++) {
			final int hhSize = householdSize(ls);

			final Parcel nextAvailableParcel = ls.getNextAvailParcelByWoreda(woredaID, ls);

			if (nextAvailableParcel == null) {
				continue;
			}
			final double totalTLU = herdSizeBYWoreda(nextAvailableParcel.getWoredaID(), ls);

			// double totalTLU = herdSizeBYWoredaEqual(nextAvailableParcel.getWoredaID(),
			// ls);
			assert hhSize > 0 : "Total hhSize: " + hhSize;
			addHousehold(hhSize, nextAvailableParcel, totalTLU, ls);

		}
	}

	public static void addHousehold(final int size, final Parcel p, final double tlu, final Landscape land) {

		final Household newHousehold = new Household();
		p.setOwner(newHousehold);
		p.setIsOccupied(true);
		newHousehold.setLocation(p);
		newHousehold.setEthinicID(p.getWoredaID());

		// int totalFarm = farmSizeBYWoredaRandom(p.getWoredaID(),land);
		final int totalFarm = farmSizeBYWoreda(p.getWoredaID(), land);

		if (totalFarm > 0) {
			if (totalFarm == 1) {
				newHousehold.addFarmLands(p);
				p.setLanduseType(Landscape.FARMLAND);

			} else {
				for (final Object o : farmLands(totalFarm, p, land)) {
					final Parcel farmLand = (Parcel) o;
					newHousehold.addFarmLands(farmLand);
					farmLand.setLanduseType(Landscape.FARMLAND);
				}
			}

		}

		final int[][] amount = new int[newHousehold.YEARSTOREMEMBER][newHousehold.SEASONPERYEAR];
		final int[][] onset = new int[newHousehold.YEARSTOREMEMBER][newHousehold.SEASONPERYEAR];
		// double [][] cropCN = new
		// double[newHousehold.CROPTYPE][newHousehold.SEASONPERYEAR];
		// double [][] cropC = new
		// double[newHousehold.CROPTYPE][newHousehold.SEASONPERYEAR];
		final double[][] damage = new double[newHousehold.YEARSTOREMEMBER][newHousehold.SEASONPERYEAR];

		for (int i = 0; i < newHousehold.YEARSTOREMEMBER; i++) {
			for (int j = 0; j < newHousehold.SEASONPERYEAR; j++) {
				amount[i][j] = 1 + land.getRandom().nextInt(3);
				onset[i][j] = 1 + land.getRandom().nextInt(3);
				damage[i][j] = 0;

//                amount[i][j] = land.NORMAL;
//                onset[i][j] = land.ONTIME;
//                damage[i][j] = 0;
			}

		}

		newHousehold.setLearningRate(0.1 + 0.9 * land.getRandom().nextDouble());
		newHousehold.setRainfallAmountMemory(amount);
		newHousehold.setRainfallOnsetMemory(onset);

		newHousehold.setLaborAllocatedFarming(size * 1.0);
		newHousehold.setLaborAllocatedHerding(size * 1.0);

		newHousehold.setClimateCAdaptationExperience(0.01 + 0.1 * land.getRandom().nextDouble());
		newHousehold.setCurrenAdaptationMechanism(Landscape.NO_ADAPTATION);
		newHousehold.setNextPredictionDate(2 + land.getRandom().nextInt(25));

		land.schedule.scheduleRepeating(newHousehold, Household.ORDERING, 1.0);
//        newHousehold.setStoppable(land.schedule.scheduleRepeating(
//                newHousehold, Household.ORDERING, 1.0));
//

		land.households.setObjectLocation(newHousehold, p.getX(), p.getY());
		final double stored = storedIncomeByYWoreda(p.getWoredaID(), size, land);

		newHousehold.setStoredCapital(stored); // 6-12 month of consumption
		newHousehold.setIncomeLabor(0);
		newHousehold.setIncomeFarming(0);
		populateHousehold(newHousehold, size, land);

		allocateHerd(newHousehold, tlu, land);

		newHousehold.setWealth(stored + tlu * land.params.herdingParam.getAverageHerdPrice());

	}

	public static void populateHousehold(final Household newHousehold, final int size, final Landscape land) {

		int age = 0;
		int sex = 0; // sex 50-50 chance

		// atleast one member of the household shold be > 17 years
		if (size == 1) {
			age = 17 + land.getRandom().nextInt(60);
			sex = land.getRandom().nextDouble() < 0.80 ? 1 : 2;

			final Person villager = new Person(sex, age, newHousehold);
			villager.setDateOfBirth(2 + land.getRandom().nextInt(360));
			villager.setMyFamily(newHousehold);
			newHousehold.addFamilyMembers(villager);

		} else if (size == 2) {
			age = 17 + land.getRandom().nextInt(60);
			sex = land.getRandom().nextDouble() < 0.80 ? 1 : 2;

			final Person villager = new Person(sex, age, newHousehold);
			villager.setDateOfBirth(2 + land.getRandom().nextInt(360));
			newHousehold.addFamilyMembers(villager);
			villager.setMyFamily(newHousehold);

			age = 15 + land.getRandom().nextInt(60);
			sex = land.getRandom().nextDouble() < 0.80 ? 2 : 1;

			final Person villager2 = new Person(sex, age, newHousehold);
			villager.setDateOfBirth(2 + land.getRandom().nextInt(360));
			newHousehold.addFamilyMembers(villager2);
			villager2.setMyFamily(newHousehold);

		} else {

			for (int i = 0; i < size; i++) {

				if (i < 2) {
					age = 17 + land.getRandom().nextInt(60);
					sex = land.getRandom().nextDouble() < 0.60 ? 1 : 2;

				} else {
					final double rn = land.getRandom().nextDouble();
					if (rn <= 0.17) {
						age = 1 + land.getRandom().nextInt(4); // 1-4 age
						sex = land.getRandom().nextDouble() < 0.49 ? 1 : 2;

					} else if (rn > 0.17 && rn <= 0.30) {
						age = 5 + land.getRandom().nextInt(5); // 5-9
						sex = land.getRandom().nextDouble() < 0.50 ? 1 : 2;
					} else if (rn > 0.30 && rn <= 0.50) {
						age = 10 + land.getRandom().nextInt(5); // 10=14
						sex = land.getRandom().nextDouble() < 0.54 ? 1 : 2;
					} else if (rn > 0.50 && rn <= 0.60) {
						age = 15 + land.getRandom().nextInt(5); // 15=19
						sex = land.getRandom().nextDouble() < 0.50 ? 1 : 2;
					} else if (rn > 0.60 && rn <= 0.68) {
						age = 20 + land.getRandom().nextInt(5); // 20=24
						sex = land.getRandom().nextDouble() < 0.43 ? 1 : 2;
					} else if (rn > 0.68 && rn <= 0.76) {
						age = 25 + land.getRandom().nextInt(5); // 25=30
						sex = land.getRandom().nextDouble() < 0.45 ? 1 : 2;
					} else if (rn > 0.76 && rn <= 0.82) {
						age = 30 + land.getRandom().nextInt(5); // 30=34
						sex = land.getRandom().nextDouble() < 0.47 ? 1 : 2;
					} else if (rn > 0.82 && rn <= 0.88) {
						age = 35 + land.getRandom().nextInt(5); // 35=39
						sex = land.getRandom().nextDouble() < 0.48 ? 1 : 2;
					} else if (rn > 0.88 && rn <= 0.91) {
						age = 40 + land.getRandom().nextInt(5); // 40=44
						sex = land.getRandom().nextDouble() < 0.53 ? 1 : 2;
					} else if (rn > 0.91 && rn <= 0.94) {
						age = 45 + land.getRandom().nextInt(5); // 45-49
						sex = land.getRandom().nextDouble() < 0.51 ? 1 : 2;
					} else if (rn > 0.94 && rn <= 0.96) {
						age = 50 + land.getRandom().nextInt(5); // 50-54
						sex = land.getRandom().nextDouble() < 0.50 ? 1 : 2;
					} else if (rn > 0.96 && rn <= 0.98) {
						age = 55 + land.getRandom().nextInt(5); // 55-59
						sex = land.getRandom().nextDouble() < 0.51 ? 1 : 2;
					} else if (rn > 0.98 && rn <= 0.99) {
						age = 60 + land.getRandom().nextInt(5); // 60-64
						sex = land.getRandom().nextDouble() < 0.53 ? 1 : 2;
					} else if (rn > 0.99 && rn <= 0.995) {
						age = 65 + land.getRandom().nextInt(70); // 65-69
						sex = land.getRandom().nextDouble() < 0.55 ? 1 : 2;
					} else if (rn > 0.995 && rn <= 0.997) {
						age = 70 + land.getRandom().nextInt(5); // 70-74
						sex = land.getRandom().nextDouble() < 0.58 ? 1 : 2;
					} else {
						age = 60 + land.getRandom().nextInt(20); // >90
						sex = land.getRandom().nextDouble() < 0.58 ? 1 : 2;
					}

				}

				final Person villager = new Person(sex, age, newHousehold);
				villager.setDateOfBirth(2 + land.getRandom().nextInt(360));
				villager.setMyFamily(newHousehold);
				newHousehold.addFamilyMembers(villager);

			}

		}
	}

	private static int householdSize(final Landscape ls) {
		// based on CSA 1994
		// household size
		// size 1 to 10
		// 0.102170052,0.152173913
		// ,0.176307051,0.170141838,0.142881046,0.104293768,0.066388653,0.039616959,
		// 0.021687646, 0.024339074

		// 0.10, 0.15, 0.18, 0.17, 0.14, 0.10, 0.066, 0.04, 0.02, 0.02
		int hhSize = 0;
		if (ls.getRandom().nextDouble() <= 0.10) {
			hhSize = 1;
		} else if (ls.getRandom().nextDouble() > 0.10 && ls.getRandom().nextDouble() <= 0.25) {
			hhSize = 2;
		} else if (ls.getRandom().nextDouble() > 0.25 && ls.getRandom().nextDouble() <= 0.43) {
			hhSize = 3;
		} else if (ls.getRandom().nextDouble() > 0.43 && ls.getRandom().nextDouble() <= 0.60) {
			hhSize = 4;
		} else if (ls.getRandom().nextDouble() > 0.60 && ls.getRandom().nextDouble() <= 0.74) {
			hhSize = 5;
		} else if (ls.getRandom().nextDouble() > 0.74 && ls.getRandom().nextDouble() <= 0.84) {
			hhSize = 6;
		} else {
			hhSize = 4 + ls.getRandom().nextInt(4);
		}

		return hhSize;
	}

	public static Bag farmLands(int total, final Parcel loc, final Landscape ls) {
		final Bag potBag = new Bag();
		potBag.clear();

		final Bag adjacentParcels = new Bag();
		adjacentParcels.clear();

		potBag.add(loc);
		ls.getNearestNeighborAreas(loc.getX(), loc.getY(), 1, adjacentParcels);
		for (final Object o : adjacentParcels) {

			final Parcel grassParcel = (Parcel) o;

			if (grassParcel.getLanduseType() == Landscape.GRASSLAND && grassParcel.getIsOccupied() == false) {
				potBag.add(grassParcel);
			}

		}

		final Bag farmBag = new Bag();
		farmBag.clear();
		if (total > farmBag.numObjs) {
			total = farmBag.numObjs;
		}

		for (int i = 0; i < total; i++) {
			final Parcel grassParcel = (Parcel) potBag.objs[i];
			farmBag.add(grassParcel);
		}

		return potBag;

	}

	private static double storedIncomeByYWoreda(final int woredaID, final int size, final Landscape ls) {
		double minIncome = 0;

		int min = 150;//
		int max = 400;

		int days = 0;
		switch (woredaID) {
		case 1:
			min = 350;
			max = 730;

			// tlu = (min + (max * randomN.nextDouble()));
			break;
		case 2:
			min = 150;
			max = 450;

			break;
		case 3:
			min = 150;
			max = 450;

			break;
		case 4:
			min = 150;
			max = 450;

			break;
		case 5:
			min = 350;
			max = 500;

			break;
		case 6:
			min = 350;
			max = 550;

			break;
		case 7:
			min = 150;
			max = 450;

			break;
		case 8:
			min = 350;
			max = 500;

			break;
		default:

			min = 250;
			break;
		}

		days = min + ls.getRandom().nextInt(max);

		minIncome = 2 * ls.params.householdParam.getMimimumExpenditurePerPerson() * size * days;

		if (minIncome <= 0) {
			minIncome = ls.params.householdParam.getMimimumExpenditurePerPerson() * size * 150;
		}

		return minIncome;
	}

	private static int farmSizeBYWoreda(final int woredaID, final Landscape ls) {

		int farmSize = 0;

		switch (woredaID) {
		case 1:
			if (ls.getRandom().nextDouble() <= 0.97) {
				farmSize = 1;
			} else if (ls.getRandom().nextDouble() > 0.970 && ls.getRandom().nextDouble() <= 0.999) {
				farmSize = 2;
			} else {
				farmSize = 3;
			}

			break;
		case 2:
			farmSize = 1;
			break;
		case 3:
			farmSize = 1;

			break;
		case 4:
			farmSize = 1;

			break;
		case 5:
			farmSize = 1;
			break;
		case 6:

			if (ls.getRandom().nextDouble() <= 0.980) {
				farmSize = 1;
			} else if (ls.getRandom().nextDouble() > 0.980 && ls.getRandom().nextDouble() <= 0.9999) {
				farmSize = 2;
			} else {
				farmSize = 3;
			}
			break;
		case 7:
			farmSize = 1;

			break;
		case 8:
			farmSize = 1;

			break;
		default:

			farmSize = 0;
			break;
		}

		return farmSize;
	}

	public static void allocateHerd(final Household newHousehold, final double size, final Landscape land) {

		if (size == 0) {

			newHousehold.setIncomeHerding(0);
			newHousehold.setHerd(null);

		} else {
			final Herd herds = new Herd(newHousehold);

			land.schedule.scheduleRepeating(herds, Herd.ORDERING, 1.0);

			herds.setHerdOwner(newHousehold);
			herds.setHerdSizeTLU(size);

			herds.setHerdLocation(newHousehold.getLocation());
			final double food = size * land.params.herdingParam.getHerdMaxFoodStored()
					* (0.8 + 0.2 * land.getRandom().nextDouble());

			herds.setHerdFood(food);
			herds.setPotentialVisionParcel(newHousehold.getLocation());

			newHousehold.setHerd(herds);
			newHousehold.setHerdingVision(land.params.herdingParam.getHerderMinVisionRange());
			newHousehold.setIncomeHerding(0);
			land.herdTLU.setObjectLocation(herds, newHousehold.getLocation().getX(), newHousehold.getLocation().getY());

		}

	}

	private static double herdSizeBYWoreda(final int woredaID, final Landscape land) {
		int min = 0;// livestock holding boundary
		int max = 1;

		double tlu = 0;
		switch (woredaID) {
		case 1:
			min = 3;
			max = 5;

			break;
		case 2:
			min = 15;
			max = 25;

			break;
		case 3:
			min = 15;
			max = 25;

			break;
		case 4:
			min = 10;
			max = 20;

			break;
		case 5:
			min = 10;
			max = 25;

			break;
		case 6:
			min = 5;
			max = 15;

			break;
		case 7:
			min = 15;
			max = 40;

			break;
		case 8:
			min = 3;
			max = 5;

			break;
		default:

			tlu = 0;
			break;
		}

		tlu = min + max * land.getRandom().nextDouble();

		if (tlu < 0) {
			tlu = 0;
		}

		return tlu;
	}

	public Parcel getNextAvailParcelByWoreda(final int woredaID, final Landscape ls) {
		Parcel nextAvailableParcel = null;
		int z = 0;
		switch (woredaID) {

		case 1:
			z = ls.getRandom().nextInt(ls.southAriBag.numObjs);
			while (((Parcel) ls.southAriBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.southAriBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = ls.getRandom().nextInt(ls.southAriBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.southAriBag.objs[z];
			break;
		case 2:
			z = ls.getRandom().nextInt(ls.hamerBag.numObjs);
			while (((Parcel) ls.hamerBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.hamerBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = ls.getRandom().nextInt(ls.hamerBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.hamerBag.objs[z];
			break;
		case 3:
			z = ls.getRandom().nextInt(ls.dasenechBag.numObjs);
			while (((Parcel) ls.dasenechBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.dasenechBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = ls.getRandom().nextInt(ls.dasenechBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.dasenechBag.objs[z];

			break;
		case 4:
			z = ls.getRandom().nextInt(ls.selamagoBag.numObjs);
			while (((Parcel) ls.selamagoBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.selamagoBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = ls.getRandom().nextInt(ls.selamagoBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.selamagoBag.objs[z];
			break;
		case 5:
			z = ls.getRandom().nextInt(ls.benaBag.numObjs);
			while (((Parcel) ls.benaBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.benaBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = ls.getRandom().nextInt(ls.benaBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.benaBag.objs[z];
			break;
		case 6:
			z = ls.getRandom().nextInt(ls.northAriBag.numObjs);
			while (((Parcel) ls.northAriBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.northAriBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = ls.getRandom().nextInt(ls.northAriBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.northAriBag.objs[z];
			break;
		case 7:
			z = ls.getRandom().nextInt(ls.gnyagnatomBag.numObjs);
			while (((Parcel) ls.gnyagnatomBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.gnyagnatomBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = ls.getRandom().nextInt(ls.gnyagnatomBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.gnyagnatomBag.objs[z];
			break;
		case 8:
			z = ls.getRandom().nextInt(ls.malieBag.numObjs);
			while (((Parcel) ls.malieBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.malieBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = ls.getRandom().nextInt(ls.malieBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.malieBag.objs[z];
			break;

		default:
			nextAvailableParcel = null;
			break;

		}

		return nextAvailableParcel;

	}
//
}
