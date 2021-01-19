package sim.app.geo.omolandCA;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
//import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import ec.util.MersenneTwisterFast;
import sim.app.geo.omolandCA.data.OmolandCAData;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.field.geo.GeomVectorField;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.ObjectGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.io.geo.ArcInfoASCGridImporter;
import sim.io.geo.ShapeFileImporter;
/**
 *
 * @author gmu
 */
import sim.util.Bag;

public class LandBuilder {

	static int totalPopDensSoFar = 0; //

	static public void create(final String landusefile, final Landscape ls) {
		try {
			// land use file
			// based on this, define dimension
			final BufferedReader landuse = new BufferedReader(
					new InputStreamReader(OmolandCAData.class.getResourceAsStream(landusefile)));
			String line;

			// first read the dimensions
			line = landuse.readLine(); // read line for width
			String[] tokens = line.split("\\s+");
			final int width = Integer.parseInt(tokens[1]); // 2nd token contains #
			line = landuse.readLine();

			tokens = line.split("\\s+");

			final int height = Integer.parseInt(tokens[1]); // 2nd token contains #

			createGrids(width, height, ls);

			// skip the next four lines as they contain irrelevant metadata
			for (int i = 0; i < 4; ++i) {
				line = landuse.readLine(); // assign to line so that we can
				// peep at it in the debugger
			}

			for (int curr_row = 0; curr_row < height; ++curr_row) {
				line = landuse.readLine();

				tokens = line.split("\\s+");

				for (int curr_col = 0; curr_col < width; ++curr_col) {

					int nv = Integer.parseInt(tokens[curr_col]);

					Parcel parcel = null;
					parcel = new Parcel();

					parcel.setX(curr_col);
					parcel.setY(curr_row);
					ls.allLand.field[curr_col][curr_row] = parcel;

					if (nv > 0 && nv < 6) {
						if (nv == 4 || nv == 5) {
							nv = 3;
						}
						parcel.setLanduseType(nv);
						ls.allParcels.add(parcel);
					}
				}
			}
			readGridAsciiFiles(ls); // all grid ascii files
			readShapeFiles(ls); // all shape files
			landuse.close();
		} catch (final Exception ex) {
			Logger.getLogger(LandBuilder.class.getName()).log(Level.SEVERE, null, ex);
			System.exit(-1);
		}
		populate(ls);
	}
	// reading ascii files using geomason

	private static void readGridAsciiFiles(final Landscape ls) {

		final InputStream ndvi = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_ndvi_river_n.txt"));
//            InputStream soil = new BufferedInputStream(OmolandCAData.class.getResourceAsStream("rasters/so_soil_fertility_n.txt"));
		final InputStream river = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_riv.txt"));
		final InputStream access = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_access_n.txt"));
		final InputStream popDens = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/pop_den_scan.txt"));
		final InputStream elevation = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_elev_f.txt"));
		final InputStream woredaid = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_woreda_rast.txt"));
		final InputStream elevation2 = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_elev_f.txt"));
		final InputStream elevation3 = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_elev_f.txt"));
		final InputStream elevation4 = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_elev_f.txt"));
		final InputStream elevation5 = new BufferedInputStream(
				OmolandCAData.class.getResourceAsStream("rasters/so_elev_f.txt"));

		// GeomGridField soilGrid = new GeomGridField();
		final GeomGridField riverGrid = new GeomGridField();
		final GeomGridField accessGrid = new GeomGridField();
		final GeomGridField popGrid = new GeomGridField();
		final GeomGridField elevationGrid = new GeomGridField();
		final GeomGridField woredaIDGrid = new GeomGridField();

		ArcInfoASCGridImporter.read(ndvi, GridDataType.DOUBLE, ls.ndviGrid); // used for shapefile analysis
		ArcInfoASCGridImporter.read(river, GridDataType.INTEGER, riverGrid);
		ArcInfoASCGridImporter.read(access, GridDataType.DOUBLE, accessGrid);
		ArcInfoASCGridImporter.read(popDens, GridDataType.DOUBLE, popGrid);
		ArcInfoASCGridImporter.read(elevation, GridDataType.DOUBLE, elevationGrid);
		ArcInfoASCGridImporter.read(woredaid, GridDataType.INTEGER, woredaIDGrid);
		ArcInfoASCGridImporter.read(elevation2, GridDataType.DOUBLE, ls.outputGrid);
		ArcInfoASCGridImporter.read(elevation3, GridDataType.DOUBLE, ls.outputGridAdpExp);
		ArcInfoASCGridImporter.read(elevation4, GridDataType.DOUBLE, ls.outputGridFarming);
		ArcInfoASCGridImporter.read(elevation5, GridDataType.DOUBLE, ls.outputGridHerding);

		ls.southAriBag.clear();
		ls.hamerBag.clear();
		ls.dasenechBag.clear();
		ls.selamagoBag.clear();
		ls.benaBag.clear();
		ls.northAriBag.clear();
		ls.gnyagnatomBag.clear();
		ls.malieBag.clear();
		// ls.highGrazingparcel.clear();

		for (final Object obj : ls.allParcels) {
			final Parcel p = (Parcel) obj;
			if (p.getLanduseType() <= 0) {
				continue;
			}
			if (p.getLanduseType() > 6) {
				continue;
			}

			final int x = p.getX();
			final int y = p.getY();
//            }
//
//            for (int x = 0; x < ls.allLand.getWidth(); x++) {
//                for (int y = 0; y < ls.allLand.getHeight(); y++) {
//                    Parcel p = (Parcel) ls.allLand.field[x][y];

			final double ndviP = ((DoubleGrid2D) ls.ndviGrid.getGrid()).get(x, y);
			// double soilP = ((DoubleGrid2D) soilGrid.getGrid()).get(x, y);
			final int riverP = ((IntGrid2D) riverGrid.getGrid()).get(x, y);
			final double accessP = ((DoubleGrid2D) accessGrid.getGrid()).get(x, y);
			double popD = ((DoubleGrid2D) popGrid.getGrid()).get(x, y);
			final int elevP = (int) ((DoubleGrid2D) elevationGrid.getGrid()).get(x, y);
			final int woreda_id = ((IntGrid2D) woredaIDGrid.getGrid()).get(x, y);

			double f = 0;
			if (ndviP >= 0) {

				final double potGrass = 0.5 * (ndviP + 0.5) * ls.params.vegetationParam.getMaxVegetationPerHectare()
						* (0.009 / (0.009 + Math.pow(((elevP - 300.0) / 3100.0), 3)));
				f = (0.009 / (0.009 + Math.pow(((elevP - 300.0) / 3100.0), 3)));
				Landscape.elevTreeFactor.field[x][y] = f;

				p.setGrass(potGrass);
				p.setSoilQuality(1.0);

				if (p.getLanduseType() == Landscape.GRASSLAND) {
					if (riverP < 3000 && riverP > 0) { // 3 km from water source - avoid water
						p.setIsIrrigatedFarm(true);
						ls.potentialIrrigableLand.add(p);
					}

				}

			}

			// technology - access to information
			if (accessP > 0) {
				p.setAccessToTechno(accessP);

			}
			// density / ha
			if (popD >= 0 && woreda_id > 0) {

				if (popD > 200) {
					popD = 0;
				}
				final int pop = (int) Math.floor(popD);
				p.setPopulationDensity(popD);
				LandBuilder.totalPopDensSoFar += 1.0 * (ls.getRandom().nextInt(pop + 1));
				ls.roulette[x][y] = LandBuilder.totalPopDensSoFar;
			}

			// elelvation
			// p.setElevation(elevP);

			p.setWoredaID(woreda_id);
			if (woreda_id == Landscape.MAGO) {

				p.setWoredaID(Landscape.SELAMAGO);
				ls.selamagoBag.add(p);
			}

			if (woreda_id > 8 || p.getLanduseType() != Landscape.GRASSLAND) {
				continue;
			}

			if (woreda_id == Landscape.SOUTH_ARI) {
				ls.southAriBag.add(p);
			} else if (woreda_id == Landscape.HAMER) {
				ls.hamerBag.add(p);
			} else if (woreda_id == Landscape.DASENECH) {
				ls.dasenechBag.add(p);
			} else if (woreda_id == Landscape.SELAMAGO) {
				ls.selamagoBag.add(p);
			} else if (woreda_id == Landscape.BENA_TSEMAY) {
				ls.benaBag.add(p);
			} else if (woreda_id == Landscape.NORTH_ARI) {
				ls.northAriBag.add(p);
			} else if (woreda_id == Landscape.GYNANGATOM) {
				ls.gnyagnatomBag.add(p);
			} else if (woreda_id == Landscape.MALIE) {
				ls.malieBag.add(p);
			} else {
				continue;
			}

			// ndvi - initial grass cover

//                }
		}
		// System.out.println("total P" + ls.highGrazingparcel.numObjs);
	}

	private static void readShapeFiles(final Landscape ls) throws Exception {
		try {

			final URL woredaSHP = OmolandCAData.class.getResource("shapefiles/so_woreda_final_att_n.shp");
			final URL woredaDB = OmolandCAData.class.getResource("shapefiles/so_woreda_final_att_n.dbf");

			final Bag woredaAttribute = new Bag();
			woredaAttribute.add("WOREDA_ID"); // id- woreda
			woredaAttribute.add("SUM_Househ"); // number of total househol per woreda
			woredaAttribute.add("SUM_CropLa"); // total farmland
			woredaAttribute.add("SUM_LandOw"); // number of household owned farmland
			woredaAttribute.add("SUM_Livest"); // total livestock - tlu

			ShapeFileImporter.read(woredaSHP, woredaDB, ls.woredaShape, woredaAttribute);

			// road
			final URL roadSHP = OmolandCAData.class.getResource("shapefiles/so_road_new.shp");
			final URL roadDB = OmolandCAData.class.getResource("shapefiles/so_road_new.dbf");

			ShapeFileImporter.read(roadSHP, roadDB, ls.roadShape);

		} catch (final IOException ex) {
			Logger.getLogger(LandBuilder.class.getName()).log(Level.SEVERE, null, ex);
			throw ex;
		}
	}

	private static void createGrids(final int width, final int height, final Landscape land) {
		land.allLand = new ObjectGrid2D(width, height);
		land.households = new SparseGrid2D(width, height);

		land.herdTLU = new SparseGrid2D(width, height);
		land.crops = new SparseGrid2D(width, height);
		land.dailyRainfall = new DoubleGrid2D(width, height);
		land.vegetationPortrial = new DoubleGrid2D(width, height);
		Landscape.elevTreeFactor = new DoubleGrid2D(width, height);
		land.woredaShape = new GeomVectorField(width, height);
		land.roadShape = new GeomVectorField(width, height);
//        land.elevationGrid = new GeomGridField();
		land.ndviGrid = new GeomGridField();
		land.outputGrid = new GeomGridField();
		land.outputGridFarming = new GeomGridField();
		land.outputGridHerding = new GeomGridField();
		land.outputGridAdpExp = new GeomGridField();
		land.roulette = new double[width][height];

	}

	public static Parcel getNextAvailParcelEnterprise(final Landscape ls) {

		Parcel nextAvailableParcel = null;

		int z = ls.getRandom().nextInt(ls.potentialIrrigableLand.numObjs);
		while (((Parcel) ls.potentialIrrigableLand.objs[z]).getLanduseType() != Landscape.GRASSLAND
				|| (((Parcel) ls.potentialIrrigableLand.objs[z])).getIsOccupied() != false) {
			// try another spot
			z = ls.getRandom().nextInt(ls.potentialIrrigableLand.numObjs);
		}

		nextAvailableParcel = (Parcel) ls.potentialIrrigableLand.objs[z];

		return nextAvailableParcel;
	}

	public static Parcel nextAvailParcelByWoreda(final int woredaID, final MersenneTwisterFast randomN,
			final Landscape ls) {
		Parcel nextAvailableParcel = null;
		int z = 0;
		switch (woredaID) {

		case 1:
			z = randomN.nextInt(ls.southAriBag.numObjs);
			while (((Parcel) ls.southAriBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					&& (((Parcel) ls.southAriBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = randomN.nextInt(ls.southAriBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.southAriBag.objs[z];
			break;
		case 2:
			z = randomN.nextInt(ls.hamerBag.numObjs);
			while (((Parcel) ls.hamerBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					&& (((Parcel) ls.hamerBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = randomN.nextInt(ls.hamerBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.hamerBag.objs[z];
			break;
		case 3:
			z = randomN.nextInt(ls.dasenechBag.numObjs);
			while (((Parcel) ls.dasenechBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					&& (((Parcel) ls.dasenechBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = randomN.nextInt(ls.dasenechBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.dasenechBag.objs[z];

			break;
		case 4:
			z = randomN.nextInt(ls.selamagoBag.numObjs);
			while (((Parcel) ls.selamagoBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					&& (((Parcel) ls.selamagoBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = randomN.nextInt(ls.selamagoBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.selamagoBag.objs[z];
			break;
		case 5:
			z = randomN.nextInt(ls.benaBag.numObjs);
			while (((Parcel) ls.benaBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					&& (((Parcel) ls.benaBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = randomN.nextInt(ls.benaBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.benaBag.objs[z];
			break;
		case 6:
			z = randomN.nextInt(ls.northAriBag.numObjs);
			while (((Parcel) ls.northAriBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					&& (((Parcel) ls.northAriBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = randomN.nextInt(ls.northAriBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.northAriBag.objs[z];
			break;
		case 7:
			z = randomN.nextInt(ls.gnyagnatomBag.numObjs);
			while (((Parcel) ls.gnyagnatomBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					&& (((Parcel) ls.gnyagnatomBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = randomN.nextInt(ls.gnyagnatomBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.gnyagnatomBag.objs[z];
			break;
		case 8:
			z = randomN.nextInt(ls.malieBag.numObjs);
			while (((Parcel) ls.malieBag.objs[z]).getLanduseType() != Landscape.GRASSLAND
					&& (((Parcel) ls.malieBag.objs[z])).getIsOccupied() != false) {
				// try another spot
				z = randomN.nextInt(ls.malieBag.numObjs);
			}
			nextAvailableParcel = (Parcel) ls.malieBag.objs[z];
			break;

		default:
			nextAvailableParcel = null;
			break;

		}

		return nextAvailableParcel;

	}

	private static void addHouseholdsByWoredAtInitail(final Landscape ls) {

		final int totalInital = ls.params.globalParam.getInitialNumberOfHouseholds();
		final HouseholdGenerator hhGenerator = new HouseholdGenerator();
		hhGenerator.addNewHouseholdAtRandomStep(totalInital, totalInital, ls);

	}

	public void addNewHouseholdAtRandomStep(final int total, final Landscape ls) {

	}

	//
	private static void populate(final Landscape land) {

		// addHouseholdsSample(land);
		addHouseholdsByWoredAtInitail(land);
	}
}
