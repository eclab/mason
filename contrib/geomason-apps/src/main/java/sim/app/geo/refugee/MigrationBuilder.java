package sim.app.geo.refugee;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

import net.sf.csv4j.CSVReader;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.grid.SparseGrid2D;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.geo.MasonGeometry;

class MigrationBuilder {
	public static Migration migrationSim;
	private static NormalDistribution nd = new NormalDistribution(Parameters.AVG_FAMILY_SIZE,
			Parameters.FAMILY_SIZE_SD);
	private static HashMap<Integer, ArrayList<Double>> age_dist;
	private static HashMap<Integer, Double> pop_dist;
	private static HashMap<Integer, NormalDistribution> fin_dist;

	// initialize world
	public static void initializeWorld(final Migration sim) throws java.net.URISyntaxException {

		MigrationBuilder.migrationSim = sim;

		MigrationBuilder.age_dist = new HashMap<Integer, ArrayList<Double>>();
		MigrationBuilder.pop_dist = new HashMap<Integer, Double>();
		MigrationBuilder.fin_dist = new HashMap<Integer, NormalDistribution>();

		final String[] regionAttributes = { "REGION" };
		final String[] countryAttributes = { "COUNTRY" };
		final String[] roadAttributes = { "NAME1", "TYPE" };
		final String[] cityAttributes = { "ID", "NAME_1", "ORIG", "POP", "SPOP_1", "QUOTA_1", "VIOL_1", "ECON_1",
				"FAMILY_1" };
		final String[] roadLinksAttributes = { "ID", "FR", "TO", "SPEED_1", "SPOP", "COST", "TLEVEL_1", "DEATH_1",
				"LENGTH_1" };

		MigrationBuilder.migrationSim.world_height = 500; // TODO - set correct size
		MigrationBuilder.migrationSim.world_width = 500; // TODO - set correct size

		MigrationBuilder.migrationSim.regions = new GeomVectorField(sim.world_width, sim.world_height);
		final Bag regionAtt = new Bag(regionAttributes);

		MigrationBuilder.migrationSim.countries = new GeomVectorField(sim.world_width, sim.world_height);
		final Bag countryAtt = new Bag(countryAttributes);

		MigrationBuilder.migrationSim.roads = new GeomVectorField(sim.world_width, sim.world_height);
		final Bag roadAtt = new Bag(roadAttributes);

		MigrationBuilder.migrationSim.cityPoints = new GeomVectorField(sim.world_width, sim.world_height);
		final Bag cityAtt = new Bag(cityAttributes);
		MigrationBuilder.migrationSim.cityGrid = new SparseGrid2D(sim.world_width, sim.world_height);

		MigrationBuilder.migrationSim.roadNetwork = new Network();
		MigrationBuilder.migrationSim.allRoadNodes = new SparseGrid2D(sim.world_width, sim.world_height);
		MigrationBuilder.migrationSim.roadLinks = new GeomVectorField(sim.world_width, sim.world_height);
		final Bag roadLinksAtt = new Bag(roadLinksAttributes);

		final URL[] files = { Parameters.REGION_SHP, Parameters.REGION_DBF,
				Parameters.COUNTRY_SHP, Parameters.COUNTRY_DBF,
				Parameters.ROAD_SHP, Parameters.ROAD_DBF,
				Parameters.CITY_SHP, Parameters.CITY_DBF,
				Parameters.ROADLINK_SHP, Parameters.ROADLINK_DBF };// shapefiles
		final Bag[] attfiles = { regionAtt, countryAtt, roadAtt, cityAtt, roadLinksAtt };
		final GeomVectorField[] vectorFields = { MigrationBuilder.migrationSim.regions,
				MigrationBuilder.migrationSim.countries, MigrationBuilder.migrationSim.roads,
				MigrationBuilder.migrationSim.cityPoints, MigrationBuilder.migrationSim.roadLinks };
		readInShapefile(files, attfiles, vectorFields);// read in attributes

		// expand the extent to include all features
		final Envelope MBR = MigrationBuilder.migrationSim.regions.getMBR();
		MBR.expandToInclude(MigrationBuilder.migrationSim.roadLinks.getMBR());
		MBR.expandToInclude(MigrationBuilder.migrationSim.cityPoints.getMBR());

		MigrationBuilder.migrationSim.regions.setMBR(MBR);
		MigrationBuilder.migrationSim.countries.setMBR(MBR);
		MigrationBuilder.migrationSim.roads.setMBR(MBR);
		MigrationBuilder.migrationSim.roadLinks.setMBR(MBR);
		MigrationBuilder.migrationSim.cityPoints.setMBR(MBR);

		makeCities(MigrationBuilder.migrationSim.cityPoints, MigrationBuilder.migrationSim.cityGrid,
				MigrationBuilder.migrationSim.cities, MigrationBuilder.migrationSim.cityList);
		extractFromRoadLinks(MigrationBuilder.migrationSim.roadLinks, MigrationBuilder.migrationSim);
		setUpAgeDist(Parameters.AGE_DIST);
		setUpPopDist(Parameters.POP_DIST);
		setUpFinDist(Parameters.FIN_DIST);

		// add refugees
		addRefugees();

	}

	private static void printCities() {
		for (final Object city : MigrationBuilder.migrationSim.cities) {
			final City c = (City) city;
			System.out.format("Name: " + c.getName() + " Ref Pop: " + c.getRefugeePopulation());
			System.out.println("\n");
		}
	}

	/*
	 * public static class Node { public Int2D location;
	 *
	 * ArrayList<Edge> links; double weightOnLineString;// measures the weight on
	 * the line string from // 0 public HashSet<LineString> lineStrings = new
	 * HashSet<LineString>(); public int index;
	 *
	 * public Node(Int2D l) { location = l; links = new ArrayList<Edge>(); }
	 *
	 * public ArrayList<Edge> getLinks() { return links; }
	 *
	 * @Override public String toString() { return "(" + location.getX() + ", " +
	 * location.getY() + ")"; } // }
	 */

	static void makeCities(final GeomVectorField cities_vector, final SparseGrid2D grid, final Bag addTo,
			final Map<Integer, City> cityList) {
		final Bag cities = cities_vector.getGeometries();
		final Envelope e = cities_vector.getMBR();
		final double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e.getMaxY();
		final int xcols = MigrationBuilder.migrationSim.world_width - 1,
				ycols = MigrationBuilder.migrationSim.world_height - 1;
		System.out.println("Reading in Cities");
		for (int i = 0; i < cities.size(); i++) {
			final MasonGeometry cityinfo = (MasonGeometry) cities.objs[i];
			final Point point = cities_vector.getGeometryLocation(cityinfo);
			final double x = point.getX(), y = point.getY();
			final int xint = (int) Math.floor(xcols * (x - xmin) / (xmax - xmin)),
					yint = (int) (ycols - Math.floor(ycols * (y - ymin) / (ymax - ymin)));
			final String name = cityinfo.getStringAttribute("NAME_1");
			final int ID = cityinfo.getIntegerAttribute("ID");
			final int origin = cityinfo.getIntegerAttribute("ORIG");
			final double scaledPop = cityinfo.getDoubleAttribute("SPOP_1");
			final int pop = cityinfo.getIntegerAttribute("POP");
			final int quota = cityinfo.getIntegerAttribute("QUOTA_1");
			final double violence = cityinfo.getDoubleAttribute("VIOL_1");
			final double economy = cityinfo.getDoubleAttribute("ECON_1");
			final double familyPresence = cityinfo.getDoubleAttribute("FAMILY_1");
			final Int2D location = new Int2D(xint, yint);

			final City city = new City(location, ID, name, origin, scaledPop, pop, quota, violence, economy,
					familyPresence);
			addTo.add(city);
			cityList.put(ID, city);
			grid.setObjectLocation(city, location);
		}
	}

	static void readInShapefile(final URL[] files, final Bag[] attfiles, final GeomVectorField[] vectorFields) {
		try {
			for (int i = 0; i < files.length; i += 2) {
				final Bag attributes = attfiles[i / 2];

				final URL shapeURI = files[i];
				final URL shapeDBF = files[i + 1];
				ShapeFileImporter.read(shapeURI, shapeDBF, vectorFields[i / 2], attributes);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

//	private static URL getUrl(final String nodesFilename) throws IOException {
//		final InputStream nodeStream = MigrationBuilder.class.getResourceAsStream(nodesFilename);
//		try {
//			if (!new File("./shapeFiles/").exists()) {
//				new File("./shapeFiles/").mkdir();
//			}
//			final File targetFile = new File(
//					"./shapeFiles/" + nodesFilename.split("/")[nodesFilename.split("/").length - 1]);
//			final OutputStream outStream = new FileOutputStream(targetFile);
//			// outStream.write(buffer);
//			int read = 0;
//			final byte[] bytes = new byte[1024];
//			while ((read = nodeStream.read(bytes)) != -1) {
//				outStream.write(bytes, 0, read);
//			}
//			outStream.close();
//			nodeStream.close();
//			if (nodesFilename.endsWith(".shp")) {
//				getUrl(nodesFilename.replace("shp", "dbf"));
//				getUrl(nodesFilename.replace("shp", "prj"));
//				getUrl(nodesFilename.replace("shp", "sbx"));
//				getUrl(nodesFilename.replace("shp", "sbn"));
//				getUrl(nodesFilename.replace("shp", "shx"));
//			}
//			return targetFile.toURI().toURL();
//		} catch (final Exception e) {
//			if (nodesFilename.endsWith("shp")) {
//				e.printStackTrace();
//				return null;
//			} else {
//				// e.printStackTrace();
//				return null;
//			}
//		}
//	}

	private static void addRefugees() {
		System.out.println("Adding Refugees ");
		MigrationBuilder.migrationSim.world = new Continuous2D(Parameters.WORLD_DISCRETIZTION,
				MigrationBuilder.migrationSim.world_width,
				MigrationBuilder.migrationSim.world_height); // TODO set this correctly
		for (final Object c : MigrationBuilder.migrationSim.cities) {

			final City city = (City) c;
			if (city.getOrigin() == 1) {
				int currentPop = 0;// 1,4,5,10,3,14,24
				final int citypop = (int) Math
						.round(MigrationBuilder.pop_dist.get(city.getID()) * Parameters.TOTAL_POP);
				System.out.println(city.getName() + ": " + citypop);
				while (currentPop <= citypop) {
					final RefugeeFamily r = createRefugeeFamily(city);
					System.out.println(r.getFamily().size());
					MigrationBuilder.migrationSim.refugeeFamilies.add(r);
					for (final Object o : r.getFamily()) {
						final Refugee refugee = (Refugee) o;
						currentPop++;
						city.addMember(refugee);
						MigrationBuilder.migrationSim.refugees.add(refugee);
						final Int2D loc = city.getLocation();
						final double y_coord = (loc.y * Parameters.WORLD_TO_POP_SCALE)
								+ (int) (MigrationBuilder.migrationSim.random.nextDouble()
										* Parameters.WORLD_TO_POP_SCALE);
						final double x_coord = (loc.x * Parameters.WORLD_TO_POP_SCALE)
								+ (int) (MigrationBuilder.migrationSim.random.nextDouble()
										* Parameters.WORLD_TO_POP_SCALE);
						MigrationBuilder.migrationSim.world.setObjectLocation(refugee, new Double2D(x_coord, y_coord));
						final int y_coordint = loc.y
								+ (int) ((MigrationBuilder.migrationSim.random.nextDouble() - 0.5) * 3);
						final int x_coordint = loc.x
								+ (int) ((MigrationBuilder.migrationSim.random.nextDouble() - 0.5) * 3);
						MigrationBuilder.migrationSim.total_pop++;
					}
					MigrationBuilder.migrationSim.schedule.scheduleRepeating(r);

				}

			}

		}
	}

	private static RefugeeFamily createRefugeeFamily(final City city) {

		// generate family
		final int familySize = pickFamilySize();
		final double finStatus = pick_fin_status(MigrationBuilder.fin_dist, city.getID()) * familySize;
		// System.out.println(finStatus);
		final RefugeeFamily refugeeFamily = new RefugeeFamily(city.getLocation(), familySize, city, finStatus);
		for (int i = 0; i < familySize; i++) {

			// first pick sex
			int sex;
			if (MigrationBuilder.migrationSim.random.nextBoolean())
				sex = Constants.MALE;
			else
				sex = Constants.FEMALE;

			// now get age
			final int age = pick_age(MigrationBuilder.age_dist, city.getID());
			System.out.println(age);

			final Refugee refugee = new Refugee(sex, age, refugeeFamily);
			refugeeFamily.getFamily().add(refugee);
		}
		return refugeeFamily;

	}

	private static int pick_age(final HashMap<Integer, ArrayList<Double>> age_dist, final int cityid) {
		int category = 0;
		final double rand = MigrationBuilder.migrationSim.random.nextDouble();
		final ArrayList<Double> dist = age_dist.get(cityid);
		for (int i = 1; i < 4; i++) {
			if (rand >= dist.get(i - 1) && rand <= dist.get(i)) {
				category = i;
				System.out.println("" + category);
				break; // TODO DOES THIS ACTUALLY BREAK
			}
		}

		switch (category) {
		case 0:
			return MigrationBuilder.migrationSim.random.nextInt(5); // 0-4
		case 1:
			return MigrationBuilder.migrationSim.random.nextInt(13) + 5; // 5-17
		case 2:
			return MigrationBuilder.migrationSim.random.nextInt(42) + 18; // 18-59
		case 3:
			return MigrationBuilder.migrationSim.random.nextInt(41) + 60; // 60+
		default:
			return 0;
		}
		// return 5;

	}

	private static void setUpPopDist(final URL pop_dist_file) throws java.net.URISyntaxException {
		try {
			// buffer reader for age distribution data
			final CSVReader csvReader = new CSVReader(
					new BufferedReader(new InputStreamReader(pop_dist_file.openStream())));
			// csvReader.readLine();// skip the headers
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				final int city_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				final double percentage = Double.parseDouble(line.get(1));
				MigrationBuilder.pop_dist.put(city_id, percentage);
				line = csvReader.readLine();
			}
			System.out.println(MigrationBuilder.pop_dist);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final java.text.ParseException e) {
			e.printStackTrace();
		}
	}

	private static void setUpFinDist(final URL fin_dist_file) throws java.net.URISyntaxException {
		try {
			// buffer reader for age distribution data
			final CSVReader csvReader = new CSVReader(
					new BufferedReader(new InputStreamReader(fin_dist_file.openStream())));
			// csvReader.readLine();// skip the headers
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				final int city_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				final double avgfin = Double.parseDouble(line.get(2));
				final double sd = Double.parseDouble(line.get(3));
				MigrationBuilder.fin_dist.put(city_id, new NormalDistribution(avgfin, sd));
				line = csvReader.readLine();
			}
			System.out.println("fin");
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final java.text.ParseException e) {
			e.printStackTrace();
		}
	}

	private static void setUpAgeDist(final URL age_dist_file) throws java.net.URISyntaxException {
		try {
			// buffer reader for age distribution data
			final CSVReader csvReader = new CSVReader(
					new BufferedReader(new InputStreamReader(age_dist_file.openStream())));
			csvReader.readLine();
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				final int city_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				final ArrayList<Double> list = new ArrayList<Double>();
				double sum = 0;
				for (int i = 1; i <= 4; i++) {
					final double percentage = Double.parseDouble(line.get(i));
					sum += percentage;
					list.add(sum);
				}
				// System.out.println("sum = " + sum);
				// System.out.println();

				// now add it to the hashmap
				MigrationBuilder.age_dist.put(city_id, list);

				line = csvReader.readLine();
			}
			System.out.println(MigrationBuilder.age_dist);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final java.text.ParseException e) {
			e.printStackTrace();
		}
	}

	private static double pick_fin_status(final HashMap<Integer, NormalDistribution> fin_dist, final int cityid) {
		// TODO Auto-generated method stub
		final NormalDistribution nd = fin_dist.get(cityid);
		return nd.sample();

	}

	private static int pickFamilySize() {
		final int familySize = (int) Math.round(MigrationBuilder.nd.sample());
		return familySize;
	}

	static void extractFromRoadLinks(final GeomVectorField roadLinks, final Migration migrationSim) {
		final Bag geoms = roadLinks.getGeometries();
		final Envelope e = roadLinks.getMBR();
		final double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e.getMaxY();
		final int xcols = migrationSim.world_width - 1, ycols = migrationSim.world_height - 1;
		final int count = 0;

		for (final Object o : geoms) {
			final MasonGeometry gm = (MasonGeometry) o;
			final int from = gm.getIntegerAttribute("FR");
			final int to = gm.getIntegerAttribute("TO");
			final double speed = gm.getDoubleAttribute("SPEED_1");
			final double distance = gm.getDoubleAttribute("LENGTH_1");
			final double spop = gm.getDoubleAttribute("SPOP");
			final double cost = gm.getDoubleAttribute("COST");
			final double transportlevel = gm.getDoubleAttribute("TLEVEL_1");
			final double deaths = gm.getDoubleAttribute("DEATH_1");
			System.out.println("pop weight: " + spop);
			final RoadInfo edgeinfo = new RoadInfo(gm.geometry, from, to, speed, spop, distance, cost, transportlevel,
					deaths);

			// build road network
			migrationSim.roadNetwork.addEdge(migrationSim.cityList.get(from), migrationSim.cityList.get(to), edgeinfo);
			migrationSim.roadNetwork.addEdge(migrationSim.cityList.get(to), migrationSim.cityList.get(from), edgeinfo);
		}

		// addRedirects();
	}

	/*
	 * static void addRedirects(){ for (Object c: migrationSim.cities){ City city =
	 * (City) c; if (migrationSim.roadNetwork.getEdgesOut(city) == null){ for
	 * (Object e: migrationSim.roadNetwork.getEdgesIn(city)){ Edge edge = (Edge) e;
	 * City reverseFrom = city; City reverseTo = (City)edge.getFrom(); RoadInfo info
	 * = (RoadInfo) edge.getInfo(); migrationSim.roadNetwork.addEdge(reverseFrom,
	 * reverseTo, info);
	 *
	 * } }
	 *
	 * } }
	 */

}
