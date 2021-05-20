package sim.app.geo.omolandCA;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import sim.app.geo.omolandCA.data.OmolandCAData;
/**
 *
 * @author gmu
 */
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.util.Int2D;

/**
 *
 *
 *
 * NB. Rainfall original is stored as double. here it is convereted to int. To
 * keep the precision the original value is multiplied by 1000. however in grass
 * and crop growth, the rainfall will be divided by 1000
 */
public final class Climate implements Steppable {

	private Landscape ls;

	BufferedReader reader;
	StringTokenizer tokenizer;
	private int currentMonth;
//	private final double CESS_THRESHOLD = 10.0; //
	public static final int START_OF_SEASONONE = 0; // January first month
	public static final int START_OF_SEASONTWO = 7; // August
	// private double MinimimEffectiveRainfall =20.5;
//   // private int MER = 20; // ( 20 mm in a week??) minimim effect rain - ati et.al 2002. Int. J. Climatol. 22: 731–742 (2002
	// 20 -based on FAO crop water requirement. we know the K value (9) and intial
	// water requirement
	public double[][][] weatherData;// This array has mean and SD of monthly dailyRainfall for all patches for each
									// of 12 months
	public double[][][] weatherMeanData;// This array has mean monthly
	public double[][][] weatherSTDData;// This array has SD monthly
	// public double[][] monthlyRainfall;
//    private int avgMonthlyRainfall;
	public double[][] curMonthRainfall;
	public double[][] curMonthRainfallNeighb;
	public double[][] prevMonthRainfall;
	public double[][] totalMonthRainfallAmount;
	private int actualYear;
	private int actualMonth;
	private int droughtYear;
	private int wetYear;
	private final int startYear;
	public DoubleGrid2D vegetationPL;
	public IntGrid2D rfZoner;

	// onset of rain Omotosho, J. B., A. A. Balogun, and K. Ogunjobi (2000),
	// Predicting monthly and seasonal rainfall, onset and
	// cessation of the rainy season in the West Africa using only surface data,
	// Int. J. Climatol., 20, 865–880
	// "the beginning of the first two rains totalling 20 mm or more, within 7 days,
	// followed by 2–3 weeks each with at least 50% of the weekly crop–water
	// requirement"
	// since this model tempral resoulution is 1 week ,
	// onset - flag - if current week rainfall is > 20 mm and next week > 8 ,
	// public int[][][] OnsetMeanWeekData;//This array holds long term average weeks
	// of onset - helps to dermien timing
	// calculated based on mean rainfall to detrmine onset prediction
	// early-ontime-late-
	public int[][] prevOnsetWeek;// This array holds long term average weeks of previous year onset - helps to
									// dermien timing
	public int[][] curOnsetWeek; // current year
	private final int[][] curCessWeek; // current year - cessassion
	private final int[][] prevCessWeek;
	public int[][] meanOnsetSeasonOne;
	public int[][] meanOnsetSeasonTwo;
	private final int[][] meanCessationSeasonOne;
	private final int[][] meanCessationSeasonTwo;
	private final double[][][] meanRainfallSeasonOne;
	private final double[][][] meanRainfallSeasonTwo;
	private final double[][][] prevTotalRainfallMonth; // three month
//    public int amountNormal[][][]; // holds total amount of rainfal for each season - normal
//    public int amountBelow[][][]; // holds total amount of rainfal for each season - below normal ( normal -1 s.d)
//    public int amountAbove[][][]; // holds total amount of rainfal for each season - above normal ) normal + 1 s.d)
//    private int onsetMonth;
	// season - main or second
	// amount - normal above or below
	private final int[] juleanCalanderRegularMonth = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	final int ONSETDAYMARGIN = 1; // +- 5 from normal - late or early
	static final int MAINRAINSEASON_ID = 0;
	static final int SECONDRAINSEASON_ID = 1;
	int startClimate = 469;
	int MONTH_RAINFALL_DATA = 1308 - startClimate - 1; // 70years - the last 77 years rainfall data
	int[] randomizeD = new int[MONTH_RAINFALL_DATA];

	public static final int ORDERING = 1;
	int parcelWidth;
	int parcelHeight;
	int climateWidth = 4;
	int climateHeight = 5;

	int randomMonthStarter = 60;

	// MersenneTwisterFast random;
	// check week 5, 14,18,22, 26,31,35,40,44
	/**
	 * @param widthInParcels  width of simulation in parcels
	 * @param heightInParcels height of simulation in parcels
	 */
	public Climate(final int widthInParcels, final int heightInParcels, final String filePath) {

		ls = null;

		currentMonth = 0;
//        currentYear = 0;
		droughtYear = 0;// (ls.params.climate.getFirstDroughtYear()* 12);;
		wetYear = 0;
		actualMonth = 0;
		parcelWidth = widthInParcels;
		parcelHeight = heightInParcels;
		vegetationPL = new DoubleGrid2D(climateWidth, climateHeight);
		rfZoner = new IntGrid2D(parcelWidth, parcelHeight);

		curOnsetWeek = new int[parcelWidth][parcelHeight]; // mainseaon =0; second season =1
		prevOnsetWeek = new int[parcelWidth][parcelHeight];

		curCessWeek = new int[parcelWidth][parcelHeight];
		prevCessWeek = new int[parcelWidth][parcelHeight];
		meanOnsetSeasonOne = new int[parcelWidth][parcelHeight];
		meanOnsetSeasonTwo = new int[parcelWidth][parcelHeight];
		meanCessationSeasonOne = new int[parcelWidth][parcelHeight];
		meanCessationSeasonTwo = new int[parcelWidth][parcelHeight];

		weatherData = new double[MONTH_RAINFALL_DATA][climateWidth][climateHeight];// This array has mean and SD of
																					// monthly dailyRainfall for all
																					// patches for each of 12 months
		weatherMeanData = new double[12][climateWidth][climateHeight];
		weatherSTDData = new double[12][climateWidth][climateHeight];
		meanRainfallSeasonOne = new double[3][climateWidth][climateHeight];// holds below normal, normal and aboven
		meanRainfallSeasonTwo = new double[3][climateWidth][climateHeight];

		prevTotalRainfallMonth = new double[4][climateWidth][climateHeight];

		// monthlyRainfall = new double[this.climateWidth][this.climateHeight];
		curMonthRainfall = new double[climateWidth][climateHeight];
		prevMonthRainfall = new double[climateWidth][climateHeight];
		totalMonthRainfallAmount = new double[climateWidth][climateHeight];

		curMonthRainfallNeighb = new double[parcelWidth][parcelHeight];
		actualYear = 1990; // This is needed for handling leap years
		startYear = 1990;

		readAllRainfallData(filePath);
		readAllMeanRainfallData(filePath);
		readRFZoner();

	}

	private void updateCalanderDays() {
		actualMonth += 1;
		if (actualMonth % 12 == 0) {
			currentMonth = 0;
			actualYear += 1;
		} else {
			currentMonth += 1;
		}

	}

	// this method determien first onset week . onset week is a week after a dry
	// month followed by two consecutive rainy weeks
	// deteremine which rainy season it it? - based on
	// will hold wich rainyseaon it is. 0 = not rain season 1 =main , 2= second
	public int determineRainySeason() {

		if (currentMonth >= Climate.START_OF_SEASONONE && currentMonth < Climate.START_OF_SEASONTWO) {
			return Climate.MAINRAINSEASON_ID;
		} else {
			return Climate.SECONDRAINSEASON_ID;
		}

	}

	private void updateMonthlyParcelRainfall() {

		for (int x = 0; x < parcelWidth; x++) {
			for (int y = 0; y < parcelHeight; y++) {

				final double todayRainfall = dailyrain(x, y);
				final double previous = ((Parcel) ls.allLand.field[x][y]).getSoilMoisture();

				((Parcel) ls.allLand.field[x][y]).setPrevSoilMositure(previous);
				((Parcel) ls.allLand.field[x][y]).setSoilMositure((todayRainfall));
				ls.dailyRainfall.field[x][y] = todayRainfall;

			}
		}
	}

	// return based on amount classification- below, normal or above
	// agent wil update their prediction based on the real situation
	// uses to check if the parcel is suitable fr crop // return the amount
	private void updateMonthlyRainfall() {
		final int m = actualMonth % MONTH_RAINFALL_DATA;
		final int n = actualMonth % 12;
		for (int i = 0; i < climateWidth; i++) {
			for (int j = 0; j < climateHeight; j++) {

				curMonthRainfall[i][j] = weatherData[m][i][j];

				if (ls.params.climateParam.getisSteadyState() == true) {
					curMonthRainfall[i][j] = weatherMeanData[n][i][j];

				}

				if (ls.params.climateParam.getIsWetYearsOn() == true) {

					if (actualMonth >= wetYear
							&& actualMonth < wetYear + (ls.params.climateParam.getNumberOfWetYears() * 12)) {
						curMonthRainfall[i][j] = weatherMeanData[n][i][j]
								+ weatherMeanData[n][i][j] * ls.params.climateParam.getSeverityOfWet();

					} else {
						curMonthRainfall[i][j] = weatherData[m][i][j];
					}
				}

				if (ls.params.climateParam.getIsDroughtYearsOn() == true) {
					if (actualMonth >= droughtYear
							&& actualMonth < droughtYear + (ls.params.climateParam.getNumberOfDroughtYears() * 12)) {
						curMonthRainfall[i][j] = weatherMeanData[n][i][j]
								* (1.0 - ls.params.climateParam.getSeverityOfDrought());

					} else {
						curMonthRainfall[i][j] = weatherData[m][i][j];
					}

				}

			}

		}

	}

	private void resetOnset() {
		if (currentMonth == 5 || currentMonth == 11) {
			for (int x = 0; x < climateWidth; x++) {
				for (int y = 0; y < climateHeight; y++) {
					prevOnsetWeek[x][y] = curOnsetWeek[x][y]; // first save
					curOnsetWeek[x][y] = 0; // change the current
				}
			}
		}
	}

	private void resetCessation() {
		if (currentMonth == Climate.START_OF_SEASONONE || currentMonth == Climate.START_OF_SEASONTWO) {
			for (int x = 0; x < climateWidth; x++) {
				for (int y = 0; y < climateHeight; y++) {
					prevCessWeek[x][y] = curCessWeek[x][y]; // first save
					curCessWeek[x][y] = 0; // change the current
				}
			}
		}
	}

//    // randomize the rainfall data - 144
//    private void randomRainfallData() {
//        int[] randomizeD = new int[MONTH_RAINFALL_DATA];
//        int m = 12; // month in a year
//        int y = (int) Math.floor(MONTH_RAINFALL_DATA / 12); // years in a
//
//        for (int i = 0; i < MONTH_RAINFALL_DATA; i++) {
//            int j = i % 12;
//            int cMth = j + 12 * ls.getRandom().nextInt(y);
//            randomizeD[i] = cMth;
//
//        }
//
//    }

	private void updateOnset() {
		// every start of the season - current onset will
		for (int x = 0; x < climateWidth; x++) {
			for (int y = 0; y < climateHeight; y++) {
				// && (int)ls.schedule.getTime() %52 > prevOnsetWeek[x][y] + this.MINIMUMLGP
				// if onset is not yet set
				if (curOnsetWeek[x][y] > 0) {
					continue;
				}

				if (this.isOnsetDay(x, y) == true) {

					curOnsetWeek[x][y] = currentMonth;

				}

			}
		}

	}

	private void updateCessation() {
		resetCessation();

		for (int x = 0; x < climateWidth; x++) {
			for (int y = 0; y < climateHeight; y++) {

				if (this.isCessationDate(x, y) == true && curCessWeek[x][y] == 0) {
					curCessWeek[x][y] = currentMonth;
				}

			}
		}
	}

	// if season one )
	// if ( time > seaso s

	private void calcMeanOnsetSeasonOne() {

		for (int x = 0; x < climateWidth; x++) {
			for (int y = 0; y < climateHeight; y++) {

				meanOnsetSeasonOne[x][y] = 0;

				for (int i = 0; i < 4; i++) {
					if (meanOnsetSeasonOne[x][y] == 0) {
						// season one jan to agust -onset - before may -- since mean is calcualted based
						// on montly - days does not consdred
						if ((weatherMeanData[i][x][y] / (1.0 * juleanCalanderRegularMonth[i])) >= 0.7
								* ls.params.climateParam.getMinimumOnsetRainThreshold()) {
							// onset should be on the season and before 45 days of next season
							meanOnsetSeasonOne[x][y] = i;
						}

					}
				}

			}
		}

	}

	private void calcMeanOnsetSeasonTwo() {

		for (int x = 0; x < climateWidth; x++) {
			for (int y = 0; y < climateHeight; y++) {

				meanOnsetSeasonTwo[x][y] = 0; // at inital all cell have 0 vlaue

				for (int i = 7; i < 11; i++) {
					if (meanOnsetSeasonTwo[x][y] == 0) { // if not set yet
						if ((weatherMeanData[i][x][y] / (1.0 * juleanCalanderRegularMonth[i])) >= ls.params.climateParam
								.getMinimumOnsetRainThreshold()) {
							meanOnsetSeasonTwo[x][y] = i;
						}
					}

				}
			}
		}

	}

	private void calcMeanCessationSeasonOne() {
		for (int x = 0; x < climateWidth; x++) {
			for (int y = 0; y < climateHeight; y++) {

				int cess = meanOnsetSeasonOne[x][y] + 4;
				if (cess > 5) {
					cess = 5;
				}
				meanCessationSeasonOne[x][y] = cess;

			}
		}

	}

	private void calcMeanCessationSeasonTwo() {

		for (int x = 0; x < climateWidth; x++) {
			for (int y = 0; y < climateHeight; y++) {

				int cess = meanOnsetSeasonTwo[x][y] + 4;
				if (cess > 11) {
					cess = 11;
				}
				meanCessationSeasonTwo[x][y] = cess;

			}
		}

	}

	private boolean isOnsetDay(final int x, final int y) {
		if (curMonthRainfall[x][y] / 30.0 >= 0.7 * ls.params.climateParam.getMinimumOnsetRainThreshold()) {
			return true;
		} else {
			return false;
		}

	}
	// average of 7 datys

	private boolean isCessationDate(final int x, final int y) {

		final int zone = rfZoner.field[x][y];
		final int xConverted = getrainZone(zone).x;
		final int yConverted = getrainZone(zone).y;

		// all date are from current month
		if (currentMonth == meanCessationSeasonOne[xConverted][yConverted]
				|| currentMonth == meanCessationSeasonTwo[xConverted][yConverted]) {
			return true;
		} else {
			return false;
		}

	}

	private void calcMeanTotalSeasonRainfal() {

		double meanBelow = 0;
		double meanAbove = 0;
		double meanNormal = 0;
		double meanBelow2 = 0;
		double meanAbove2 = 0;
		double meanNormal2 = 0;

		for (int x = 0; x < climateWidth; x++) {
			for (int y = 0; y < climateHeight; y++) {

				meanRainfallSeasonOne[0][x][y] = 0;
				meanRainfallSeasonOne[1][x][y] = 0;
				meanRainfallSeasonOne[2][x][y] = 0;
				meanRainfallSeasonTwo[0][x][y] = 0;
				meanRainfallSeasonTwo[1][x][y] = 0;
				meanRainfallSeasonTwo[2][x][y] = 0;

				meanNormal = weatherMeanData[0][x][y] + weatherMeanData[1][x][y] + weatherMeanData[2][x][y]
						+ weatherMeanData[3][x][y] + 0.5 * weatherMeanData[4][x][y];
				meanBelow = 0.5 * meanNormal;
				meanAbove = 1.5 * meanNormal;

				meanNormal2 = weatherMeanData[8][x][y] + weatherMeanData[9][x][y] + weatherMeanData[10][x][y]
						+ 0.5 * weatherMeanData[11][x][y];
				meanBelow2 = 0.5 * meanNormal2;
				meanAbove2 = 1.5 * meanNormal2;

				meanRainfallSeasonOne[0][x][y] = meanBelow;
				meanRainfallSeasonOne[1][x][y] = meanNormal;
				meanRainfallSeasonOne[2][x][y] = meanAbove;
				meanRainfallSeasonTwo[0][x][y] = meanBelow2;
				meanRainfallSeasonTwo[1][x][y] = meanNormal2;
				meanRainfallSeasonTwo[2][x][y] = meanAbove2;

			}

		}

	}
	// take the first day- multiply by the month=

	private void calcTotalRainfallSeason() {

		if (rainySeasonAmount() == true) {

			for (int x = 0; x < climateWidth; x++) {
				for (int y = 0; y < climateHeight; y++) {
					final double firstM = prevTotalRainfallMonth[0][x][y];
					final double secondM = prevTotalRainfallMonth[1][x][y];
					final double thirdM = prevTotalRainfallMonth[2][x][y];

					prevTotalRainfallMonth[0][x][y] = curMonthRainfall[x][y];
					prevTotalRainfallMonth[1][x][y] = firstM;
					prevTotalRainfallMonth[2][x][y] = secondM;
					prevTotalRainfallMonth[3][x][y] = thirdM;
					totalMonthRainfallAmount[x][y] = (0.5 * curMonthRainfall[x][y] + firstM + secondM);

				}
			}

		}

	}

	public boolean rainySeasonAmount() {
		boolean israinySeason = false;
		if (currentMonth >= 0 && currentMonth <= 5) {
			israinySeason = true;
		} else if (currentMonth >= 7 && currentMonth <= 10) {
			israinySeason = true;
		} else {
			israinySeason = false;
		}

		return israinySeason;

	}

	// N.B it is good to separate onset and planting
	// because planting matters only for farmers
//   public boolean determinePlantingWeek(int currentTime, int x, int y){
//        // if onset of rain is garanteed and current month holds enough moisture
//        // it possible to plant
//
//        if(currentTime %52 >= (curOnsetWeek[x][y]) &&  (curWeekRainfall[x][y]) > this.MER){
//            return true;
//        }
//        return false;
//    }
//   public int determinePlantingWeekValue(int x, int y){
//        // if onset of rain is garanteed and current month holds enough moisture
//        // it possible to plant
//
//        if(curOnsetWeek[determineRainySeason()][x][y]> 0 && 30 * prevWeekRainfall[x][y] > MER){
//            return numWeek;
//        }
//        else {
//            return 0;
//        }
//
//    }
	public void readAllRainfallData(final String filePath) {

		for (int m = 0; m < MONTH_RAINFALL_DATA; m++) {
			final int month = m + startClimate;

			final String rainfallfile = filePath + "rainfall/" + "p" + (month + 1) + ".txt";

			try {

				final BufferedReader rainfall = new BufferedReader(
						new InputStreamReader(OmolandCAData.class.getResourceAsStream(rainfallfile)));

				String line;
				String[] tokens;

//                for (int i = 0; i < 6; ++i) {
//                    line = rainfall.readLine();  // assign to line so that we can
//                    // peep at it in the debugger
//                }

				for (int curr_row = 0; curr_row < climateHeight; ++curr_row) {
					line = rainfall.readLine();

					tokens = line.split("\\s+");

					for (int curr_col = 0; curr_col < climateWidth; ++curr_col) {
						final Double patchRain = Double.parseDouble(tokens[curr_col]);

						weatherData[m][curr_col][curr_row] = patchRain / 10.0;

						curOnsetWeek[curr_col][curr_row] = 0;
						prevOnsetWeek[curr_col][curr_row] = Climate.START_OF_SEASONTWO;

					}
				}

			} catch (final IOException ex) {

				Logger.getLogger(Climate.class.getName()).log(Level.SEVERE, null, ex);

			}

		}

	}

	public void readAllMeanRainfallData(final String filePath) {

		try {

			for (int month = 0; month < 12; month++) {

				final String rainfallfile = filePath + "rainfall/" + "md" + (month + 1) + ".txt";

				final BufferedReader rainfall = new BufferedReader(
						new InputStreamReader(OmolandCAData.class.getResourceAsStream(rainfallfile)));

				String line;
				String[] tokens;

//                for (int i = 0; i < 6; ++i) {
//                    line = rainfall.readLine();  // assign to line so that we can
//                    // peep at it in the debugger
//                }

				for (int curr_row = 0; curr_row < climateHeight; ++curr_row) {
					line = rainfall.readLine();

					tokens = line.split("\\s+");

					for (int curr_col = 0; curr_col < climateWidth; ++curr_col) {
						final Double patchRain = Double.parseDouble(tokens[curr_col]);

						weatherMeanData[month][curr_col][curr_row] = patchRain / 10.0; // cgiar data has 10 scaling
																						// factor

						if (month == 0) {
							prevTotalRainfallMonth[0][curr_col][curr_row] = 0;
							prevTotalRainfallMonth[1][curr_col][curr_row] = 0;
							prevTotalRainfallMonth[2][curr_col][curr_row] = 0;
							prevTotalRainfallMonth[3][curr_col][curr_row] = 0;
						}

					}
				}

			}
			// assign mean onset

		} catch (final IOException ex) {

			Logger.getLogger(Climate.class.getName()).log(Level.SEVERE, null, ex);

		}

	}

	public void readAllSDRainfallData(final String filePath) {

		try {

			for (int month = 0; month < 12; month++) {

				final String rainfallfile = filePath + "rainfall/" + "sd" + (month + 1) + ".txt";

				final BufferedReader rainfall = new BufferedReader(
						new InputStreamReader(OmolandCAData.class.getResourceAsStream(rainfallfile)));

				String line;
				String[] tokens;

//                for (int i = 0; i < 6; ++i) {
//                    line = rainfall.readLine();  // assign to line so that we can
//                    // peep at it in the debugger
//                }

				for (int curr_row = 0; curr_row < climateHeight; ++curr_row) {
					line = rainfall.readLine();

					tokens = line.split("\\s+");

					for (int curr_col = 0; curr_col < climateWidth; ++curr_col) {
						final Double patchRain = Double.parseDouble(tokens[curr_col]);

						weatherSTDData[month][curr_col][curr_row] = patchRain / 10.0;

					}
				}

			}
			// assign mean onset

		} catch (final IOException ex) {

			Logger.getLogger(Climate.class.getName()).log(Level.SEVERE, null, ex);

		}

	}

	public void readRFZoner() {
		try {

			final InputStream ist = OmolandCAData.class.getResourceAsStream("rasters/so_rfZone.asc");

			reader = new BufferedReader(new InputStreamReader(ist));

			for (int i = 0; i < 6; i++) {
				reader.readLine();
			}
			for (int i = 0; i < rfZoner.getHeight(); i++) {
				tokenizer = new StringTokenizer(reader.readLine());
				for (int j = 0; j < rfZoner.getWidth(); j++) {
					final double value = Double.parseDouble(tokenizer.nextToken());
					rfZoner.set(j, i, (int) value);
				}
			}
		} catch (final Exception e) {
		}
	}

	// provide three month average rainfall based on agent expected mositure content
	// agent dtermination of onset week to next average growing season month
	// based on last year
	public double potentialMoistureAsLastYear(final int season, final int xLoc, final int yLoc) {
		double averageMoisture = 0;
		final int zone = rfZoner.field[xLoc][yLoc];
		final int xConverted = getrainZone(zone).x;
		final int yConverted = getrainZone(zone).y;

		if (season == Climate.MAINRAINSEASON_ID) {
			averageMoisture = totalMonthRainfallAmount[xConverted][yConverted];
		} else {
			averageMoisture = totalMonthRainfallAmount[xConverted][yConverted];
		}

		return averageMoisture; // some variation
	}

	public double potentialMoisture(final int amount, final int season, final int xLoc, final int yLoc) {
		double averageMoisture = 0;
		double temp = 0;
		final int zone = rfZoner.field[xLoc][yLoc];
		final int xConverted = getrainZone(zone).x;
		final int yConverted = getrainZone(zone).y;
		if (season == Climate.MAINRAINSEASON_ID) {
			if (amount == ls.BELOW_NORMAL) {
				averageMoisture = meanRainfallSeasonOne[0][xConverted][yConverted];
			} else if (amount == ls.NORMAL) {
				averageMoisture = meanRainfallSeasonOne[1][xConverted][yConverted];
			} else {
				averageMoisture = meanRainfallSeasonOne[2][xConverted][yConverted];
			}
		} else {
			if (amount == ls.BELOW_NORMAL) {
				averageMoisture = meanRainfallSeasonTwo[0][xConverted][yConverted];
			} else if (amount == ls.NORMAL) {
				averageMoisture = meanRainfallSeasonTwo[1][xConverted][yConverted];
			} else {
				averageMoisture = meanRainfallSeasonTwo[2][xConverted][yConverted];
			}
		}
		temp = averageMoisture;

		if (temp < 0) {
			temp = 0;
		}
		return temp;

	}

	public Int2D getrainZone(final int zone) {
		int x = 0;
		int y = 0;
		if (zone == 1 || zone == 5 || zone == 9 || zone == 13 || zone == 17) {
			x = 0;
		}
		if (zone == 2 || zone == 6 || zone == 10 || zone == 14 || zone == 18) {
			x = 1;
		}
		if (zone == 3 || zone == 7 || zone == 11 || zone == 15 || zone == 19) {
			x = 2;
		}
		if (zone == 4 || zone == 8 || zone == 12 || zone == 16 || zone == 20) {
			x = 3;
		}

		if (zone == 1 || zone == 2 || zone == 3 || zone == 4) {
			y = 0;
		}
		if (zone == 5 || zone == 6 || zone == 7 || zone == 8) {
			y = 1;
		}
		if (zone == 9 || zone == 10 || zone == 11 || zone == 12) {
			y = 2;
		}
		if (zone == 13 || zone == 14 || zone == 15 || zone == 16) {
			y = 3;
		}
		if (zone == 17 || zone == 18 || zone == 19 || zone == 20) {
			y = 4;
		}

		return new Int2D(x, y);
	}

	// agent only get based on previous season // this is not prediction but agent
	// updae their memory based on the past season
	public int determineSeasonRainfallPattern(final int season, final int x, final int y) {

		int currentAmount = 0;
		final int zone = rfZoner.field[x][y];
		final int xConverted = getrainZone(zone).x;
		final int yConverted = getrainZone(zone).y;

		if (actualYear == startYear) {
			currentAmount = ls.NORMAL;
		}

		if (season == Climate.MAINRAINSEASON_ID) {
			// first year - no way to access previous season

			if (totalMonthRainfallAmount[xConverted][yConverted] < meanRainfallSeasonOne[0][xConverted][yConverted]) {
				currentAmount = ls.BELOW_NORMAL;
			} else if (totalMonthRainfallAmount[xConverted][yConverted] > meanRainfallSeasonOne[2][xConverted][yConverted]) {
				currentAmount = ls.ABOVE_NORMAL;
			} else {
				currentAmount = ls.NORMAL;
			}

		}

		if (season == Climate.SECONDRAINSEASON_ID) {

			if (totalMonthRainfallAmount[xConverted][yConverted] < meanRainfallSeasonTwo[0][xConverted][yConverted]) {
				currentAmount = ls.BELOW_NORMAL;
			} else if (totalMonthRainfallAmount[xConverted][yConverted] > meanRainfallSeasonTwo[2][xConverted][yConverted]) {
				currentAmount = ls.ABOVE_NORMAL;
			} else {
				currentAmount = ls.NORMAL;
			}

		}

		((Parcel) ls.allLand.field[x][y]).setCurrentAmount(currentAmount);

		return currentAmount;

	}

	private int yRainScale(final int y) {

		final double dimentionDivisor = Math.ceil(parcelHeight / (climateHeight * 1.0));
		final double rem = y % dimentionDivisor;
		double scaler = 0;
		final double scalerDimension = Math.floor(y * 1.0 / dimentionDivisor);
		if (rem / dimentionDivisor < 0.4) {
			scaler = -1;
		} else if (rem / dimentionDivisor > 0.6) {
			scaler = 1;
		} else {
			scaler = 0;
		}

		if (scalerDimension + scaler >= 0 && scalerDimension + scaler < climateHeight) {
			return (int) scaler;
		}

		else {
			return 0;
		}
	}

	private double dailyrain(final int xLoc, final int yLoc) {

		final int zone = randomizeRF(rfZoner.field[xLoc][yLoc]);
		final int xConverted = getrainZone(zone).x;
		final int yConverted = getrainZone(zone).y;
		double todayRainfall = 0.0;
		todayRainfall = curMonthRainfall[xConverted][yConverted] / 30.0;
		return (todayRainfall);

	}

	private int randomizeRF(final int z) {
		int n = 1;
		if (z >= 1 && z < 5) {
			n = 1 + ls.getRandom().nextInt(4);
		}
		if (z >= 5 && z < 9) {
			n = 5 + ls.getRandom().nextInt(4);
		}
		if (z >= 9 && z < 13) {
			n = 9 + ls.getRandom().nextInt(4);
		}
		if (z >= 13 && z < 17) {
			n = 13 + ls.getRandom().nextInt(4);
		}
		if (z >= 17 && z < 21) {
			n = 17 + ls.getRandom().nextInt(4);
		}
		return n;
	}

//    // accessing rainfall from parcel -- differnt scale
//    private int xCoordScalerToLarge(int x) {
//
//        double dimentionDivisor = Math.ceil(this.parcelWidth / (this.climateWidth * 1.0));
//        double scalerDimension = Math.floor(x * 1.0 / dimentionDivisor);
//
//        assert scalerDimension >= 0 && scalerDimension < climateWidth : "patchROW: " + scalerDimension;
//
//        return (int) scalerDimension;
//    }
//
//    private int yCoordScalerToLarge(int y) {
//        double dimentionDivisor = Math.ceil(this.parcelHeight / (this.climateHeight * 1.0));
//        double scalerDimension = Math.floor(y * 1.0 / dimentionDivisor);
//        assert scalerDimension >= 0 && scalerDimension < climateHeight : "patchROW: " + scalerDimension;
//
//        return (int) scalerDimension;
//    }

	public int determineSeasonLandPreparation(final int season, final int onsetPre, final int x, final int y) {
		// land prep before onset
		int onset = 0;
		final int zone = rfZoner.field[x][y];
		final int xConverted = getrainZone(zone).x;
		final int yConverted = getrainZone(zone).y;
		int landPrep = 0;

		if (season == Climate.MAINRAINSEASON_ID) {
			onset = meanOnsetSeasonOne[xConverted][yConverted];// normal onset
		} else {
			onset = meanOnsetSeasonTwo[xConverted][yConverted];// normal onset
		}

		if (onsetPre == ls.EARLY) { // early is one month earlier than normal
			landPrep = onset - 2; // so prepration start one month earlier than early onset ..> two ,month before
									// normal
		} else if (onsetPre == ls.NORMAL) {
			landPrep = -1;
		} else if (onsetPre == ls.LATE) {
			landPrep = onset;
		} else {
			landPrep = onset;
		}

		if (landPrep < 0) {
			landPrep = 0;
		}
		if (landPrep > 11) {
			landPrep = 11;
		}

		return landPrep;

	}

	// agent update the previous season onset
	public int determineSeasonOnset(final int season, final int x, final int y) {

		int onset = 0;
		final int zone = rfZoner.field[x][y];
		final int xConverted = getrainZone(zone).x;
		final int yConverted = getrainZone(zone).y;

		// if agent is in mean season - get the previous season
		if (actualYear == startYear) {
			onset = ls.NORMAL; // normal
		}
		if (season == Climate.MAINRAINSEASON_ID) {

			if (curOnsetWeek[xConverted][yConverted] == meanOnsetSeasonOne[xConverted][yConverted]) {
				onset = ls.NORMAL; // normal
			} else if (curOnsetWeek[xConverted][yConverted] < meanOnsetSeasonOne[xConverted][yConverted]) {
				onset = ls.EARLY; // early
			} else if (curOnsetWeek[xConverted][yConverted] > meanOnsetSeasonOne[xConverted][yConverted]) {
				onset = ls.LATE; // late
			} else {
				onset = 1 + ls.getRandom().nextInt(3);
			}

		}
		if (season == Climate.SECONDRAINSEASON_ID) {
			if (curOnsetWeek[xConverted][yConverted] == meanOnsetSeasonTwo[xConverted][yConverted]) {
				onset = ls.NORMAL; // normal
			} else if (curOnsetWeek[xConverted][yConverted] < meanOnsetSeasonTwo[xConverted][yConverted]) {
				onset = ls.EARLY; // early
			} else if (curOnsetWeek[xConverted][yConverted] > meanOnsetSeasonTwo[xConverted][yConverted]) {
				onset = ls.LATE; // late
			} else {
				onset = 1 + ls.getRandom().nextInt(3);
			}
		}

		((Parcel) ls.allLand.field[x][y]).setCurrentOnset(onset);

		return onset;

	}

	// scenario analysis
	// drought
	private void updateDroughtYear() {
		// drought year//ls.params.climate.getNumberOfDroughtYears()

		if (actualMonth == droughtYear + (ls.params.climateParam.getNumberOfDroughtYears() * 12)) {
			final int temp = droughtYear;
			droughtYear = temp + (ls.params.climateParam.getFrequencyOfDroughtYears() * 12);

		} // 2000

	}

	private void updateWetYear() {

		if (actualMonth == wetYear + (ls.params.climateParam.getNumberOfWetYears() * 12)) {
			final int temp = wetYear;
			wetYear = temp + (ls.params.climateParam.getFrequencyOfWetYears() * 12);

		} // 2000

	}

	public void step(final SimState state) {

		ls = (Landscape) state;

		if (ls.schedule.getTime() == 0) {
			calcMeanTotalSeasonRainfal();
			calcMeanOnsetSeasonOne();
			calcMeanOnsetSeasonTwo();
			calcMeanCessationSeasonOne();
			calcMeanCessationSeasonTwo();
			droughtYear = ls.params.climateParam.getFirstDroughtYear() * 12;
			wetYear = ls.params.climateParam.getFirstWetYear() * 12;

		}

		updateCalanderDays();// update the date and week

		updateDroughtYear();
		updateWetYear();
		updateMonthlyRainfall();
		updateMonthlyParcelRainfall();
		resetOnset();
		updateOnset();
		updateCessation();
		calcTotalRainfallSeason();

	}
}