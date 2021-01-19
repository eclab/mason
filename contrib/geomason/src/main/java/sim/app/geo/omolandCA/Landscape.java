package sim.app.geo.omolandCA;

import java.lang.reflect.Constructor;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;

import com.vividsolutions.jts.geom.Point;

/**
 *
 * @author gmu
 */
import ec.util.MersenneTwisterFast;
import sim.engine.MakesSimState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.ObjectGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

public class Landscape extends SimState {
	// composed of parcels
	// report to insitution over all condition of parcels

	public ObjectGrid2D allLand;
	public SparseGrid2D households;
	public SparseGrid2D herdTLU;
	public SparseGrid2D crops;
	public DoubleGrid2D dailyRainfall;
	public DoubleGrid2D vegetationPortrial;

	public static DoubleGrid2D elevTreeFactor;

	public static IntGrid2D rfZoner;
	// public GeomGridField elevationGrid ;
	public GeomGridField ndviGrid;
	public GeomGridField outputGrid;
	public GeomGridField outputGridAdpExp;
	public GeomGridField outputGridFarming;
	public GeomGridField outputGridHerding;
	public double[][] roulette;
	public GeomVectorField woredaShape;
	public GeomVectorField roadShape;
	public final Parameters params;
	public Climate climate;
	public Vegetation vegetationParcel;
	public CropHarverster cropHarverster;
	public LandObserver landObserver;
	public final static int NO_ADAPTATION = 1;
	public final static int WITH_ADAPTATION = 2;
	public final int ONSETMARGIN = 4; // one month from normal // since the rainfall amount is monthly

	// public Herd herd;
	public final int BELOW_NORMAL = 1;
	public final int NORMAL = 2;
	public final int ABOVE_NORMAL = 3;
	public final int EARLY = 1;
	public final int ONTIME = 2;
	public final int LATE = 3;
	public static final int LAKE = 1;
	public static final int RIVER = 2;
	public static final int GRASSLAND = 3;
	public static final int FARMLAND = 6;
	public static final int MAGO_PARK = 4;
	public static final int OMO_PARK = 5;

	// Woreda id- Name
	public static final int TOTAL_WOREDA_NUMBER = 9; // 8 woredas + mago park
	public static final int MAGO = 0;
	public static final int SOUTH_ARI = 1;
	public static final int HAMER = 2;
	public static final int DASENECH = 3;
	public static final int SELAMAGO = 4;
	public static final int BENA_TSEMAY = 5;
	public static final int NORTH_ARI = 6;
	public static final int GYNANGATOM = 7;
	public static final int MALIE = 8;
	public static final double[] POP_PROPORTION_WOREDA = { 0, 0.33, 0.11, 0.09, 0.05, 0.12, 0.09, 0.05, 0.16 };
	public double[] vegPerWoreda;
	// crop type
	private static final long serialVersionUID = 1L;
	public XYSeries totalHousehold = new XYSeries("Total Household");
	public XYSeries totalPopulation = new XYSeries("Total Population");
	public XYSeries totalWealth = new XYSeries("Total Wealth");
	public XYSeries totalStoredCapital = new XYSeries("Total Stored Capital");
	public XYSeries totalLivestockCapital = new XYSeries("Total Livestock Capital");
	public XYSeries totalOffFarmWorker = new XYSeries("Total OffFarm Workers");
	public XYSeries totalAdaptationSeries = new XYSeries("Adopter");
	public XYSeries totalNonAdaptationSeries = new XYSeries(" Non-Adopter");
	public XYSeries totalAdaptationExperienceSeries = new XYSeries("Adpt Experience");
	public XYSeries totalEarlyOnsetSeries = new XYSeries("Easly Onset");
	public XYSeries totalNormalOnsetSeries = new XYSeries("Normal Onset");
	public XYSeries totalLateOnsetExperienceSeries = new XYSeries("Late Onset");
	public XYSeries totalBelowNormalAmountSeries = new XYSeries("Below Normal Amount");
	public XYSeries totalNormalAmountSeries = new XYSeries("Normal Amount");
	public XYSeries totalAboveNormalSeries = new XYSeries("Above Normal Amount");
	public XYSeries totalLivestockSeries = new XYSeries(" Livestock");
	public XYSeries totalMaizeSeriesHA = new XYSeries("Maize");
	public XYSeries totalMaizeSeriesYield = new XYSeries("Maize Yield");

	// rainfall per parcel
	public XYSeries rainfallSeriesNorth = new XYSeries(" parcel rainfall - Northern");
	public XYSeries rainfallSeriesCentral = new XYSeries(" parcel rainfall - Central");
	public XYSeries rainfallSeriesSouth = new XYSeries(" parcel rainfall - Southern");
	DefaultCategoryDataset agedataset = new DefaultCategoryDataset();// shows age structure of agents
	DefaultCategoryDataset familydataset = new DefaultCategoryDataset(); // shows family size
	DefaultCategoryDataset popWoredadataset = new DefaultCategoryDataset(); // shows family size
//   public int totalgridWidth =  10;
//   public int totalgridHeight = 10;
	Bag allParcels;
	Bag potentialFarms = new Bag();
	Bag potentialIrrigableLand;
	Bag southAriBag = new Bag();
	Bag hamerBag = new Bag();
	Bag dasenechBag = new Bag();
	Bag selamagoBag = new Bag();
	Bag benaBag = new Bag();
	Bag northAriBag = new Bag();
	Bag gnyagnatomBag = new Bag();
	Bag malieBag = new Bag();
	Bag highGrazingparcel = new Bag(); // collect potential grazzing land - agent will have access based on their
										// vision
//   public final double FARMINPUTINVESTMENT = 0.2;  // percentage of inversment household can put
//   public final double HERDINPUTINVESTMENT = 0.2; // if both herding and farming - propotional based on income
//

	// amount parameter
	public final double wAmountYear1 = 0.4;
	public final double wAmountYear2 = 0.3;
	public final double wAmountYear3 = 0.2;
	public final double wAmountYear4 = 0.1;

	public final double wOnsetYear1 = 0.4;
	public final double wOnsetYear2 = 0.3;
	public final double wOnsetYear3 = 0.2;
	public final double wOnsetYear4 = 0.1;

	public double totalPopDensSoFar = 0.0; //
	public final double PER_SEVERITY_INDEX[][] = new double[][] { { 0.5, 0.1, 0.4 },
			{ 0.7, 0.2, 0.3 },
			{ 1.0, 0.6, 0.5 } };
	public double vegetationdrawer = 0.80;
	public int totalMigrantHH = 0;
	public int totalMigrantPOP = 0;
	public int[] totMigrantHHWoreda = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	public int[] totMigrantPopWoreda = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	public int[] totHHWoreda = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	public int numberHighGrazingParcel = 20;

	public Landscape(final long seed, final String[] args) {
		this(seed, args, false);
	}

	public Landscape(final long seed, final String[] args, final boolean writer) {
		super(seed);
		params = new Parameters(args);
		allParcels = new Bag();
		potentialFarms = new Bag();
		potentialIrrigableLand = new Bag();
		vegPerWoreda = new double[Landscape.TOTAL_WOREDA_NUMBER];

		if (writer == true) {
			System.out.println("OmoLand");
		}

	}

	public void start() {

		super.start();
		LandBuilder.create("rasters/so_lus_n.txt", this);
		climate = new Climate(allLand.getWidth(), allLand.getHeight(), "");
		vegetationParcel = new Vegetation();
		cropHarverster = new CropHarverster();

		schedule.scheduleRepeating(climate, Climate.ORDERING, 1.0);
		schedule.scheduleRepeating(vegetationParcel, Climate.ORDERING + 1, 1.0);
		schedule.scheduleRepeating(cropHarverster, CropHarverster.ORDERING, 1.0);

		landObserver = new LandObserver(this);
		schedule.scheduleRepeating(landObserver, LandObserver.ORDERING, 1.0);

		// System.out.println("tot"+ households.allObjects.numObjs);

		final double rate = params.householdParam.getPersonBirthRate() / 12.0;
		final Steppable removeObj = new Steppable() {

			public void step(final SimState state) {
				removeHerds();
				removeHouseholdLandscape();

				addNewHousehold(rate);

				if (schedule.getTime() % 30 == 0) {

					averageVegPerWoredaNew();
				}

			}
		};

		schedule.scheduleRepeating(removeObj);

	}

	public MersenneTwisterFast getRandom() {

		return random;
	}

//  // taken from riftland and modified
	// for any other

	public final void getNearestNeighborAreas(final int x, final int y, final int dist, final Bag closestParcel) {
		// Cribbed from SparseGrid2D.getNeighborsMaxDistance()
		int xmin, xmax, ymin, ymax;
		// compute xmin and xmax for the neighborhood such that they are within
		// boundaries
		xmin = ((x - dist >= 0) ? x - dist : 0);
		xmax = ((x + dist <= allLand.getWidth() - 1) ? x + dist : allLand.getWidth() - 1);

		// compute ymin and ymax for the neighborhood such that they are within
		// boundaries
		ymin = ((y - dist >= 0) ? y - dist : 0);
		ymax = ((y + dist <= allLand.getHeight() - 1) ? y + dist : allLand.getHeight() - 1);

		for (int x0 = xmin; x0 <= xmax; x0++) {
			for (int y0 = ymin; y0 <= ymax; y0++) {
				if (((Parcel) allLand.get(x0, y0)).getLanduseType() > 0) {
					closestParcel.add(allLand.field[x0][y0]);
				}
			}
		}
	}

	// for herders
	public final void getNearestNeighborGrazingAreas(final int x, final int y, final int dist,
			final Bag closestParcel) {

		int xmin, xmax, ymin, ymax;
		// compute xmin and xmax for the neighborhood such that they are within
		// boundaries
		xmin = ((x - dist >= 0) ? x - dist : 0);
		xmax = ((x + dist <= allLand.getWidth() - 1) ? x + dist : allLand.getWidth() - 1);

		// compute ymin and ymax for the neighborhood such that they are within
		// boundaries
		ymin = ((y - dist >= 0) ? y - dist : 0);
		ymax = ((y + dist <= allLand.getHeight() - 1) ? y + dist : allLand.getHeight() - 1);

		for (int x0 = xmin; x0 <= xmax; x0++) {
			for (int y0 = ymin; y0 <= ymax; y0++) {
				if (((Parcel) allLand.get(x0, y0)).getLanduseType() == Landscape.GRASSLAND
						&& ((Parcel) allLand.get(x0, y0)).getWoredaID() > 0) {
					closestParcel.add(allLand.field[x0][y0]);
				}
			}
		}
	}

	// read attributes from woreda shape file - geomason
	public int getWoredaID(final int x, final int y) {
		final Point p = ndviGrid.toPoint(x, y);

		final Bag coveringObjects = woredaShape.getCoveringObjects(p);

		// If the coordinate falls outside all the woreda boundaries, then
		// coveringObjects will be empty.
		if (coveringObjects.isEmpty()) {
			return 0;
		}

		final MasonGeometry masonGeometry = (MasonGeometry) coveringObjects.objs[0];

		final int woredaId = masonGeometry.getIntegerAttribute("WOREDA_ID");

		return woredaId;
	}

	public Parcel getNextAvailParcelByWoreda(final int woredaID, final Landscape ls) {
		Parcel nextAvailableParcel = null;
		int z = 0;
		switch (woredaID) {

		case 1:
			z = getRandom().nextInt(ls.southAriBag.numObjs);
			while (((Parcel) ls.southAriBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.southAriBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = getRandom().nextInt(ls.southAriBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.southAriBag.objs[z];
			break;
		case 2:
			z = getRandom().nextInt(ls.hamerBag.numObjs);
			while (((Parcel) ls.hamerBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.hamerBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = getRandom().nextInt(ls.hamerBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.hamerBag.objs[z];
			break;
		case 3:
			z = getRandom().nextInt(ls.dasenechBag.numObjs);
			while (((Parcel) ls.dasenechBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.dasenechBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = getRandom().nextInt(ls.dasenechBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.dasenechBag.objs[z];

			break;
		case 4:
			z = getRandom().nextInt(ls.selamagoBag.numObjs);
			while (((Parcel) ls.selamagoBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.selamagoBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = getRandom().nextInt(ls.selamagoBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.selamagoBag.objs[z];
			break;
		case 5:
			z = getRandom().nextInt(ls.benaBag.numObjs);
			while (((Parcel) ls.benaBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.benaBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = getRandom().nextInt(ls.benaBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.benaBag.objs[z];
			break;
		case 6:
			z = getRandom().nextInt(ls.northAriBag.numObjs);
			while (((Parcel) ls.northAriBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.northAriBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = getRandom().nextInt(ls.northAriBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.northAriBag.objs[z];
			break;
		case 7:
			z = getRandom().nextInt(ls.gnyagnatomBag.numObjs);
			while (((Parcel) ls.gnyagnatomBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.gnyagnatomBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = getRandom().nextInt(ls.gnyagnatomBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.gnyagnatomBag.objs[z];
			break;
		case 8:
			z = getRandom().nextInt(ls.malieBag.numObjs);
			while (((Parcel) ls.malieBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					|| (((Parcel) ls.malieBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = getRandom().nextInt(ls.malieBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.malieBag.objs[z];
			break;

		default:
			nextAvailableParcel = null;
			break;

		}

		return nextAvailableParcel;

	}

	public void addNewHousehold(final double rate) {

		for (int i = 0; i < 9; i++) {
			final double potential = (getTotalHHWoreda()[i] * (1.0 + rate)) - getTotalHHWoreda()[i];

			if (potential > 1.0) {

				final int newHHnumber = (int) Math.ceil(potential);

				final int currentHH = 1 + this.getRandom().nextInt(newHHnumber);
				final HouseholdGenerator hhGenerator = new HouseholdGenerator();
				hhGenerator.addNewHouseholdAtRandomStepW(i, currentHH, this);

			} else {
				if (this.getRandom().nextDouble() < potential) {
					final int currentHH = 1;
					final HouseholdGenerator hhGenerator = new HouseholdGenerator();
					hhGenerator.addNewHouseholdAtRandomStepW(i, currentHH, this);
				}

			}
		}

	}

	public void removeHerd(final Herd herd) {

		herdTLU.remove(herd);
	}

	public void removeHerds() {
		for (final Object obj : herdTLU.allObjects) {
			final Herd herd = (Herd) obj;
			if (herd.getHerdSizeTLU() <= 0) {
				herd.getHerdOwner().setHerd(null);
				removeHerd(herd);
			}

		}

	}

	public void removeHouseholdLandscape() {
		int total = 0;
		int totPop = 0;
		for (final Object hh : households.allObjects) {
			final Household household = (Household) hh;

			if (household.getWealth() > 0) {
				continue;
			}
			if (household.getFamilyMembers().numObjs <= 0) {
				removeHousehold(household);
				continue;
			}

			if (household.getWealth() <= 0 && household.getFamilyMembers().numObjs > 0) {
				total = total + 1;
				final int wid = household.getLocation().getWoredaID();
				if (wid >= 0 && wid < 9) {
					final int t = totMigrantHHWoreda[wid];
					totMigrantHHWoreda[wid] = t + 1;
					totPop = totPop + household.getFamilyMembers().numObjs;
					final int p = totMigrantPopWoreda[wid];
					totMigrantPopWoreda[wid] = p + household.getFamilyMembers().numObjs;
				}

				removeHousehold(household);
			}

		}

		setTotalMigrantHH(getTotalMigrantHH() + total);
		setTotalMigrantPOP(getTotalMigrantPOP() + totPop);

	}

	public void removeHousehold(final Household household) {
		if (household.getHerd() != null && household.getHerd().getHerdSizeTLU() >= 0) {
			removeHerd(household.getHerd());
			household.setHerd(null);
		}
		if (household.getFarmLands().numObjs > 0) {
			for (final Object fm : household.getFarmLands()) {
				final Parcel parcel = (Parcel) fm;
				if (parcel.getCrop() != null) {
					parcel.setCropHasHarvested(true);
					crops.remove(parcel.getCrop());
					parcel.setCrop(null);
					parcel.setFarmPrepared(false);
					parcel.setCropHasPlanted(false);
				}

				parcel.setOwner(null);
				parcel.setLanduseType(Landscape.GRASSLAND); // from farm land to grazingland- abandon

			}
		}

		if (household.getFamilyMembers().numObjs > 0) {
			for (final Object p : household.getFamilyMembers()) {
				final Person person = (Person) p;
				household.removeFamilyMembers(person);
			}
		}

		households.remove(household);

	}

//    public void averageVegPerWoreda(){
//
//        double noting=0;
//       for ( int i=0; i<vegPerWoreda.length;i++){
//           vegPerWoreda[i] =0;
//       }
//
//
//
//        for(Object p: this.allParcels){
//            Parcel parcel =(Parcel)p;
//            if(parcel.getGrass()/params.vegetationParam.getMaxVegetationPerHectare() < 0.6){
//                continue;
//            }
//            switch(parcel.getWoredaID()){
//                case MAGO:
//                    vegPerWoreda[MAGO] =0;
//                    break;
//                case SOUTH_ARI:
//
//                    vegPerWoreda[SOUTH_ARI] +=parcel.getGrass();
//                    break;
//                case HAMER:
//                    vegPerWoreda[HAMER] +=parcel.getGrass();
//                    break;
//                case DASENECH:
//                    vegPerWoreda[DASENECH] +=parcel.getGrass();
//                    break;
//                case SELAMAGO:
//                    vegPerWoreda[SELAMAGO] +=parcel.getGrass();
//                    break;
//                case BENA_TSEMAY:
//                    vegPerWoreda[BENA_TSEMAY] +=parcel.getGrass();
//                    break;
//                case NORTH_ARI:
//                    vegPerWoreda[NORTH_ARI] +=parcel.getGrass();
//                    break;
//                case GYNANGATOM:
//                    vegPerWoreda[GYNANGATOM] +=parcel.getGrass();
//                    break;
//                case MALIE:
//                    vegPerWoreda[MALIE] +=parcel.getGrass();
//                    break;
//
//                default:
//                    noting=0;
//                    break;
//
//            }
//        }
//    }

	public void averageVegPerWoredaNew() {

//        double noting=0;

		highGrazingparcel.clear();

		int c1 = 0;
		int c2 = 0;
		int c3 = 0;
		int c4 = 0;
		int c5 = 0;
		int c6 = 0;
		int c7 = 0;
		int c8 = 0;

		for (final Object p : allParcels) {
			final Parcel parcel = (Parcel) p;

			final double vegLimit = parcel.getGrass() / params.vegetationParam.getMaxVegetationPerHectare();
			if (vegLimit < 0.6 || vegLimit > 0.8) {
				continue;
			}
			if (parcel.getLanduseType() == Landscape.FARMLAND) {
				continue;
			}
			switch (parcel.getWoredaID()) {

			case SOUTH_ARI:
				if (c1 < numberHighGrazingParcel) {
					highGrazingparcel.add(parcel);
					c1 = c1 + 1;
				}

				break;
			case HAMER:
				if (c2 < numberHighGrazingParcel) {
					highGrazingparcel.add(parcel);
					c2 = c2 + 1;
				}

				break;
			case DASENECH:
				if (c3 < numberHighGrazingParcel) {
					highGrazingparcel.add(parcel);
					c3 = c3 + 1;
				}

				break;
			case SELAMAGO:
				if (c4 < numberHighGrazingParcel) {
					highGrazingparcel.add(parcel);
					c4 = c4 + 1;
				}

				break;
			case BENA_TSEMAY:
				if (c5 < numberHighGrazingParcel) {
					highGrazingparcel.add(parcel);
					c5 = c5 + 1;
				}

				break;
			case NORTH_ARI:
				if (c6 < numberHighGrazingParcel) {
					highGrazingparcel.add(parcel);
					c6 = c6 + 1;
				}

				break;
			case GYNANGATOM:
				if (c7 < numberHighGrazingParcel) {
					highGrazingparcel.add(parcel);
					c7 = c7 + 1;
				}

				break;
			case MALIE:

				if (c8 < numberHighGrazingParcel) {
					highGrazingparcel.add(parcel);
					c8 = c8 + 1;
				}

				break;

			default:
//                    noting=0;
				break;

			}
		}
	}

	public void setVegDrawerRange(final double d) {
		vegetationdrawer = d;
	}

	public double getVegDrawerRange() {
		return vegetationdrawer;
	}

	public void setTotalMigrantHH(final int d) {
		totalMigrantHH = d;
	}

	public int getTotalMigrantHH() {
		return totalMigrantHH;
	}

	public void setTotalMigrantPOP(final int d) {
		totalMigrantPOP = d;
	}

	public int getTotalMigrantPOP() {
		return totalMigrantPOP;
	}

	public int[] getTotalMigrantHHWoreda() {
		return totMigrantHHWoreda;
	}

	public int[] getTotalMigrantPOPWoreda() {
		return totMigrantPopWoreda;
	}

	public int[] getTotalHHWoreda() {
		return totHHWoreda;
	}

	public static void main(final String[] args) {

		// doLoop(Landscape.class, args);
		doLoop(new MakesSimState() {

			@Override
			public SimState newInstance(final long seed, final String[] args) {

				return new Landscape(seed, args);
			}

			@Override
			public Class<Landscape> simulationClass() {
				return Landscape.class;
			}

			@Override
			public Constructor[] getConstructors() {
				return Landscape.class.getConstructors();
			}
		}, args);

		System.exit(0);
	}

	public void finish() {

		super.finish();

		if (landObserver != null) {
			landObserver.finish();
		}

	}

}
