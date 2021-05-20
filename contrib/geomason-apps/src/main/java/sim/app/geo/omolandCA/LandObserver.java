package sim.app.geo.omolandCA;

import java.io.BufferedWriter;
import java.io.FileWriter;
/**
 *
 * @author gmu
 */
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.csv4j.CSVWriter;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DoubleGrid2D;
import sim.io.geo.ArcInfoASCGridExporter;
import sim.util.Bag;

public class LandObserver implements Steppable {

	private BufferedWriter dataFileBufferHH; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVFileHH; // CSV file that contains run data
	private BufferedWriter dataFileBufferClimateAdpt; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVFileClimateAdpt; // CSV file that contains run data
	private BufferedWriter dataFileBufferCrops; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVFileCrops; // CSV file that contains run data
	// woreda based
	private BufferedWriter dataFileBufferHHWoreda; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVHHWoreda; // CSV file that contains run data
	private BufferedWriter dataFileBufferPOPWoreda; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVPOPWoreda; // CSV file that contains run data

	private BufferedWriter dataFileBufferWLTHWoreda; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVWLTHWoreda; // CSV file that contains run data
	private BufferedWriter dataFileBufferYIELDWoreda; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVYIELDWoreda; // CSV file that contains run data
	private BufferedWriter dataFileBufferTLUWoreda; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVTLUWoreda; // CSV file that contains run data

	private BufferedWriter dataFileBufferBPLWoreda; // output file buffer for dataCSVFile_
	private CSVWriter dataCSVBPLWoreda; // CSV file that contains run data

	Bag omoLGridBag = new Bag();
	Landscape ls;
	public final static int ORDERING = 7;
	private int step = 0;
	private boolean writeGrid = false;
	private int totalHH;
	private int totalPerson;
	private String expName = "exp";

	private double belowPovertyLine;
	private double totalWealthHH;
	private double totalStoredCaptitalHH;
	private double totalLivestockCapitalHH;
	private int[] ageCatagoryTotal = { 0, 0, 0, 0, 0 }; // adding agent all agents whose age falls in a given age-class
	private int[] familySizeCatagoryTotal = { 0, 0, 0, 0, 0, 0, 0 };
	private int[] popByworedaCatagoryTotal = { 0, 0, 0, 0, 0, 0, 0, 0 }; // woreda total pop

	private double totalLivestockTLU;
	private double totalMaizeHectares;
	private double totalSorghumHectares;
	private double totalTeffHectares;
	private double totalWheatHectares;
	private double totalMaizeYield;
	private double totalSorghumYield;
	private double totalTeffYield;
	private double totalWheatYield;
	private double dailyRainfallNorthern;
	private double dailyRainfallCentral;
	private double dailyRainfallSouthern;
	private double totalAdapt;
	private double totalNonAdapt;
	private double totalAdaptationExperience; // no relaton with cliamte- labor
	private double earlyOnsetRatio;
	private double normalOnsetRatio;
	private double lateOnsetRatio;
	private double belowNormalAmountratio;
	private double normalAmountRatio;
	private double aboveNormalAmountRatio; // no relaton with cliamte- labor

	private double earlyOnsetRatioC;
	private double normalOnsetRatioC;
	private double lateOnsetRatioC;
	private double belowNormalAmountratioC;
	private double normalAmountRatioC;
	private double aboveNormalAmountRatioC; // no relaton with cliamte- labor

	private final int[] totEmployeeWoreda = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	LandObserver(final Landscape land) {
//    	setup(world);
		// <GCB>: you may want to adjust the number of columns based on these flags.
		// both in createLogFile, and step
		ls = null;

		this.setExperimentName(land.params.globalParam.getExpFileName());
		startLogFile();

	}
//
//    LandObserver() {
//
//        startLogFile();
//    }

	public void setExperimentName(final String exp) {
		expName = exp;
	}

	public String getExperimentName() {
		return expName;
	}

	public void setearlyOnsetRatio(final double total) {
		earlyOnsetRatio = total;
	}

	public double getearlyOnsetRatio() {
		return earlyOnsetRatio;
	}

	public void setnormalOnsetRatio(final double total) {
		normalOnsetRatio = total;
	}

	public double getnormalOnsetRatio() {
		return normalOnsetRatio;
	}

	public void setlateOnsetRatio(final double total) {
		lateOnsetRatio = total;
	}

	public double getlateOnsetRatio() {
		return lateOnsetRatio;
	}

	public void setbelowNormalAmountratio(final double total) {
		belowNormalAmountratio = total;
	}

	public double getbelowNormalAmountratio() {
		return belowNormalAmountratio;
	}

	public void setnormalAmountRatio(final double total) {
		normalAmountRatio = total;
	}

	public double getnormalAmountRatio() {
		return normalAmountRatio;
	}

	public void setaboveNormalAmountRatio(final double total) {
		aboveNormalAmountRatio = total;
	}

	public double getaboveNormalAmountRatio() {
		return aboveNormalAmountRatio;
	}

	public void setearlyOnsetRatioC(final double total) {
		earlyOnsetRatioC = total;
	}

	public double getearlyOnsetRatioC() {
		return earlyOnsetRatioC;
	}

	public void setnormalOnsetRatioC(final double total) {
		normalOnsetRatioC = total;
	}

	public double getnormalOnsetRatioC() {
		return normalOnsetRatioC;
	}

	public void setlateOnsetRatioC(final double total) {
		lateOnsetRatioC = total;
	}

	public double getlateOnsetRatioC() {
		return lateOnsetRatioC;
	}

	public void setbelowNormalAmountratioC(final double total) {
		belowNormalAmountratioC = total;
	}

	public double getbelowNormalAmountratioC() {
		return belowNormalAmountratioC;
	}

	public void setnormalAmountRatioC(final double total) {
		normalAmountRatioC = total;
	}

	public double getnormalAmountRatioC() {
		return normalAmountRatioC;
	}

	public void setaboveNormalAmountRatioC(final double total) {
		aboveNormalAmountRatioC = total;
	}

	public double getaboveNormalAmountRatioC() {
		return aboveNormalAmountRatioC;
	}

	public void setTotalHH(final int total) {
		totalHH = total;
	}

	public int getTotalHH() {
		return totalHH;
	}

	public void setBelowPovertyLine(final double total) {
		belowPovertyLine = total;
	}

	public double getBelowPovertyLine() {
		return belowPovertyLine;
	}

	public void settotalPerson(final int total) {
		totalPerson = total;
	}

	public int gettotalPerson() {
		return totalPerson;
	}

	public void settotalAdapt(final double total) {
		totalAdapt = total;
	}

	public double gettotalAdapt() {
		return totalAdapt;
	}

	public void settotalNonAdapt(final double total) {
		totalNonAdapt = total;
	}

	public double gettotalNonAdapt() {
		return totalNonAdapt;
	}

	public void settotalAdaptationExperience(final double total) {
		totalAdaptationExperience = total;
	}

	public double gettotalAdaptationExperience() {
		return totalAdaptationExperience;
	}

	public void settotalWealthHH(final double total) {
		totalWealthHH = total;
	}

	public double gettotalWealthHH() {
		return totalWealthHH;
	}

	public void settotalStoredCaptitalHH(final double total) {
		totalStoredCaptitalHH = total;
	}

	public double gettotalStoredCaptitalHH() {
		return totalStoredCaptitalHH;
	}

	public void settotalLivestockCapitalHH(final double total) {
		totalLivestockCapitalHH = total;
	}

	public double gettotalLivestockCapitalHH() {
		return totalLivestockCapitalHH;
	}

	public void setageCatagoryTotal(final int[] total) {
		ageCatagoryTotal = total;
	}

	public int[] getageCatagoryTotal() {
		return ageCatagoryTotal;
	}

	public void setfamilySizeCatagoryTotal(final int[] total) {
		familySizeCatagoryTotal = total;
	}

	public int[] getfamilySizeCatagoryTotal() {
		return familySizeCatagoryTotal;
	}

	public void setpopByworedaCatagoryTotal(final int[] total) {
		popByworedaCatagoryTotal = total;
	}

	public int[] getpopByworedaCatagoryTotal() {
		return popByworedaCatagoryTotal;
	}

	public void settotalLivestockTLU(final double total) {
		totalLivestockTLU = total;
	}

	public double gettotalLivestockTLU() {
		return totalLivestockTLU;
	}

	public void settotalMaizeHectares(final double total) {
		totalMaizeHectares = total;
	}

	public double gettotalMaizeHectares() {
		return totalMaizeHectares;
	}

	public void settotalMaizeYield(final double total) {
		totalMaizeYield = total;
	}

	public double gettotalMaizeYield() {
		return totalMaizeYield;
	}

	public void setdailyRainfallNorthern(final double total) {
		dailyRainfallNorthern = total;
	}

	public double getdailyRainfallNorthern() {
		return dailyRainfallNorthern;
	}

	public void setdailyRainfallCentral(final double total) {
		dailyRainfallCentral = total;
	}

	public double getdailyRainfallCentral() {
		return dailyRainfallCentral;
	}

	public void setdailyRainfallSouthern(final double total) {
		dailyRainfallSouthern = total;
	}

	public double getdailyRainfallSouthern() {
		return dailyRainfallSouthern;
	}

	public void householdStat() {

		final int[] sumfamSiz = { 0, 0, 0, 0, 0, 0, 0 }; // adding all agent families based o their family size
		final int[] sumPopWpreda = { 0, 0, 0, 0, 0, 0, 0, 0 };

		// household data
		double totWealth = 0;
		double stored = 0;
		double tluC = 0;
		int belowpv = 0;

		int totadpt = 0;
		int totNotAdpt = 0;

		double totalAdaptationExperience = 0;

		double earlyOnset = 0;
		double normalOnset = 0;
		double lateOnset = 0;
		double belowNormal = 0;
		double normalAmount = 0;
		double aboveNormal = 0;

		double earlyOnsetC = 0;
		double normalOnsetC = 0;
		double lateOnsetC = 0;
		double belowNormalC = 0;
		double normalAmountC = 0;
		double aboveNormalC = 0;

		final int totalPerson = 0;
		for (final Object hh : ls.households.allObjects) {
			final Household household = (Household) hh;

			if (household.getWealth() >= 0) {
				totWealth = totWealth + household.getWealth();

				if (household.getWealth() < household.getFamilyMembers().numObjs
						* ls.params.householdParam.getPovertyLineThreshold()) {
					belowpv = belowpv + 1;
				}

			}

			if (household.getHerd() != null && household.getHerd().getHerdSizeTLU() > 0) {
				tluC = tluC + household.getHerd().getHerdSizeTLU() * ls.params.herdingParam.getAverageHerdPrice();
			}

			stored = stored + household.getStoredCapital();

			if (household.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION) {
				totadpt = totadpt + 1;
			}
			if (household.getCurrenAdaptationMechanism() == ls.NO_ADAPTATION) {
				totNotAdpt = totNotAdpt + 1;
			}
			if (household.getClimateCAdaptationExperience() >= 0) {
				totalAdaptationExperience = totalAdaptationExperience + household.getClimateCAdaptationExperience();
			}

			if (household.getCurrentOnset() == ls.EARLY) {
				earlyOnset = earlyOnset + 1;
			}

			if (household.getCurrentOnset() == ls.NORMAL) {
				normalOnset = normalOnset + 1;
			}
			if (household.getCurrentOnset() == ls.LATE) {
				lateOnset = lateOnset + 1;
			}

			if (household.getCurrentAmount() == ls.BELOW_NORMAL) {
				belowNormal = belowNormal + 1;
			}

			if (household.getCurrentAmount() == ls.NORMAL) {
				normalAmount = normalAmount + 1;
			}

			if (household.getCurrentAmount() == ls.ABOVE_NORMAL) {
				aboveNormal = aboveNormal + 1;
			}

			// real
			if (household.getLocation().getCurrentOnset() == ls.EARLY && household.getCurrentOnset() == ls.EARLY) {
				earlyOnsetC = earlyOnsetC + 1;
			}

			if (household.getLocation().getCurrentOnset() == ls.NORMAL && household.getCurrentOnset() == ls.NORMAL) {
				normalOnsetC = normalOnsetC + 1;
			}
			if (household.getLocation().getCurrentOnset() == ls.LATE && household.getCurrentOnset() == ls.LATE) {
				lateOnsetC = lateOnsetC + 1;
			}

			if (household.getLocation().getCurrentAmount() == ls.BELOW_NORMAL
					&& household.getCurrentAmount() == ls.BELOW_NORMAL) {
				belowNormalC = belowNormalC + 1;
			}

			if (household.getLocation().getCurrentAmount() == ls.NORMAL && household.getCurrentAmount() == ls.NORMAL) {
				normalAmountC = normalAmountC + 1;
			}

			if (household.getLocation().getCurrentAmount() == ls.ABOVE_NORMAL
					&& household.getCurrentAmount() == ls.ABOVE_NORMAL) {

				aboveNormalC = aboveNormalC + 1;
			}

			int siz = 0;
			int pop = 0;
			if (household.getFamilyMembers().numObjs == 0) {
				continue;
			}
			if (household.getFamilyMembers().numObjs > 6) { // aggregate all families of >6 family size
				siz = 6;
			} else {
				siz = household.getFamilyMembers().numObjs - 1;

			}
			pop = (household.getLocation().getWoredaID() - 1);
			if (pop < 0 || pop > 7) {
				continue;
			}
			sumPopWpreda[pop] += 1;
			sumfamSiz[siz] += 1;

		}

		this.setBelowPovertyLine(belowpv * 1.0 / ls.households.allObjects.numObjs);

		this.setTotalHH(ls.households.allObjects.numObjs);
		this.settotalAdapt(totadpt * 1.0 / ls.households.allObjects.numObjs);
		this.settotalNonAdapt(totNotAdpt * 1.0 / ls.households.allObjects.numObjs);
		this.settotalAdaptationExperience(totalAdaptationExperience * 1.0 / ls.households.allObjects.numObjs);
		this.settotalWealthHH(totWealth);
		this.settotalStoredCaptitalHH(stored);
		this.settotalLivestockCapitalHH(tluC);

		this.setfamilySizeCatagoryTotal(sumfamSiz);
		this.setpopByworedaCatagoryTotal(sumPopWpreda);

		this.setearlyOnsetRatio(earlyOnset * 1.0 / ls.households.allObjects.numObjs);
		this.setnormalOnsetRatio(normalOnset * 1.0 / ls.households.allObjects.numObjs);
		this.setlateOnsetRatio(lateOnset * 1.0 / ls.households.allObjects.numObjs);

		this.setbelowNormalAmountratio(belowNormal * 1.0 / ls.households.allObjects.numObjs);
		this.setnormalAmountRatio(normalAmount * 1.0 / ls.households.allObjects.numObjs);
		this.setaboveNormalAmountRatio(aboveNormal * 1.0 / ls.households.allObjects.numObjs);

		this.setearlyOnsetRatioC(earlyOnsetC * 1.0 / ls.households.allObjects.numObjs);
		this.setnormalOnsetRatioC(normalOnsetC * 1.0 / ls.households.allObjects.numObjs);
		this.setlateOnsetRatioC(lateOnsetC * 1.0 / ls.households.allObjects.numObjs);

		this.setbelowNormalAmountratioC(belowNormalC * 1.0 / ls.households.allObjects.numObjs);
		this.setnormalAmountRatioC(normalAmountC * 1.0 / ls.households.allObjects.numObjs);
		this.setaboveNormalAmountRatioC(aboveNormalC * 1.0 / ls.households.allObjects.numObjs);

	}

	public void personStat() {
		final int[] sumAge = { 0, 0, 0, 0, 0 }; // adding agent all agents whose age falls in a given age-class

		int totalPerson = 0;
		final int totalOff = 0;

		for (final Object hh : ls.households.allObjects) {
			final Household household = (Household) hh;

			if (household.getFamilyMembers().numObjs == 0) {
				continue;
			}
			for (final Object m : household.getFamilyMembers()) {
				final Person member = (Person) m;
				final int age = ageClass(member.getAge()); // age class of agent i
				// int siz = 0;
				sumAge[age] += 1;
				totalPerson = totalPerson + 1;

			}

		}
		this.settotalPerson(totalPerson);
		this.setageCatagoryTotal(sumAge);

	}

	private int ageClass(final int age) {
		int a = 0;

		if (age < 5) {
			a = 0;
		} else if (age >= 5 && age < 12) {
			a = 1;
		} else if (age >= 12 && age < 18) {
			a = 2;
		} else if (age >= 18 && age < 60) {
			a = 3;
		} else {
			a = 4;
		}

		return a;
	}

	public void livestockStat() {
		double tlu = 0;

		for (final Object h : ls.herdTLU.allObjects) {
			final Herd herd = (Herd) h;

			if (herd.getHerdSizeTLU() >= 0) {
				tlu = tlu + herd.getHerdSizeTLU();
			}

		}

		this.settotalLivestockTLU(tlu);

	}

	public void cropStat() {
		double totalMaize = 0;
		final double totalSorghum = 0;
		final double totalWheat = 0;
		final double totalTEFF = 0;

		double totalMaizeYield = 0;
		final double totalSorghumYield = 0;
		final double totalWheatYield = 0;
		final double totalTEFFYield = 0;

		for (final Object c : ls.crops.allObjects) {
			final Crop crop = (Crop) c;

			if (crop.getCropLand().getIsOccupied() != true) {
				continue;
			}

			if (crop.getCropID() == CropParameters.MAIZE_ID) {
				totalMaize = totalMaize + 1;
				totalMaizeYield = totalMaizeYield + yieldCrop(crop);
			}

		}
		this.settotalMaizeHectares(totalMaize);
		this.settotalMaizeYield(totalMaizeYield);

	}

	public void rainfallStat() {
		this.setdailyRainfallNorthern(ls.dailyRainfall.field[590][467]);
		this.setdailyRainfallCentral(ls.dailyRainfall.field[638][1130]);
		this.setdailyRainfallSouthern(ls.dailyRainfall.field[535][1924]);
	}

	public void addCharts(final double tSteps) {

		ls.totalHousehold.add(tSteps, this.getTotalHH());
		ls.totalPopulation.add(tSteps, this.gettotalPerson());
		ls.totalWealth.add(tSteps, this.gettotalWealthHH());
		ls.totalStoredCapital.add(tSteps, this.gettotalStoredCaptitalHH());
		ls.totalLivestockCapital.add(tSteps, this.gettotalLivestockCapitalHH());

		ls.totalAdaptationSeries.add(tSteps, this.gettotalAdapt());
		ls.totalNonAdaptationSeries.add(tSteps, this.gettotalNonAdapt());
		ls.totalAdaptationExperienceSeries.add(tSteps, this.gettotalAdaptationExperience());

		ls.totalEarlyOnsetSeries.add(tSteps, this.getearlyOnsetRatio());
		ls.totalNormalOnsetSeries.add(tSteps, this.getnormalOnsetRatio());
		ls.totalLateOnsetExperienceSeries.add(tSteps, this.getlateOnsetRatio());

		ls.totalBelowNormalAmountSeries.add(tSteps, this.getbelowNormalAmountratio());
		ls.totalNormalAmountSeries.add(tSteps, this.getnormalAmountRatio());
		ls.totalAboveNormalSeries.add(tSteps, this.getaboveNormalAmountRatio());

		ls.totalLivestockSeries.add(tSteps, this.gettotalLivestockTLU());

		ls.totalMaizeSeriesHA.add(tSteps, this.gettotalMaizeHectares());
		ls.totalMaizeSeriesYield.add(tSteps, this.gettotalMaizeYield());

		ls.rainfallSeriesNorth.add(tSteps, this.getdailyRainfallNorthern());
		ls.rainfallSeriesCentral.add(tSteps, this.getdailyRainfallCentral());
		ls.rainfallSeriesSouth.add(tSteps, this.getdailyRainfallSouthern());
//

		// ageset
		final String ageTitle = "Age Group";
		final String[] ageC = new String[] { "1-4", "5-11", "12-17", "18-60", "60 +" };
		for (int i = 0; i < this.getageCatagoryTotal().length; i++) {
			if (totalPerson <= 0) {
				ls.agedataset.setValue(0, ageTitle, ageC[i]);
			} else {
				ls.agedataset.setValue(this.getageCatagoryTotal()[i] * 100 / totalPerson, ageTitle, ageC[i]);
			}
		}

		final String famTitle = "Household Size";
		final String[] famC = new String[] { "1", "2", "3", "4", "5", "6", "6+" };

		// family size
		for (int i = 0; i < this.getfamilySizeCatagoryTotal().length; i++) {
			ls.familydataset.setValue(this.getfamilySizeCatagoryTotal()[i], famTitle, famC[i]);
		}

		final String popWoredaTitle = "Household Size";
		final String[] popWC = new String[] { "SOUTH ARI", "HAMER", "DASENECH", "SELAMAGO", "BENATSEMAY", "NORTH ARI",
				"GYNANGATOM", "MALIE" };

		// family size
		for (int i = 0; i < this.getpopByworedaCatagoryTotal().length; i++) {
			ls.popWoredadataset.setValue(
					this.getpopByworedaCatagoryTotal()[i] * 100 / ls.households.getAllObjects().numObjs, popWoredaTitle,
					popWC[i]);
		}

	}

	private void startLogFile() {
		// Create a CSV file to capture data for this run.
		try {
			createLogFile();

			// Household data

			final String[] headerHH = new String[] { "Job", "Step", "TotalHH", "TotalPop", "TotalWealth",
					"TotalCapital", "TotalLivestock Cap", "TotalLivestockTLU", "TotalBelowPovLine", "TotalMigrant",
					"TotalMograntPop" };
			dataCSVFileHH.writeLine(headerHH);

			// Crop Data
			final String[] headercrop = new String[] { "Job", "Step", "Maize", "Sorghum", "Teff", "Wheat", "MaizeY",
					"SorghumY", "TeffY", "WheatY" };
			dataCSVFileCrops.writeLine(headercrop);

			final String[] headerAdpt = new String[] { "Job", "Step", "TotalHH", "Adapt", "Not-Adapt", "Adp-Exp",
					"Early Onset", "Normal Onset", "Late Onset",
					"Below Normal Rain", "Normal Rain", "Above Normal Rain", "Early Onset Current",
					"Normal Onset Current", "Late Onset Current",
					"Below Normal Current", "Normal Rain Current", "Above Normal Current" };
			dataCSVFileClimateAdpt.writeLine(headerAdpt);

			final String[] headerTLUWoreda = new String[] { "Job", "Step", "South Ari", "Hamer", "Dasenech", "Selamago",
					"Bena Tsemay", "North Ari", "Gynangatom", "Malie" };
			final String[] headerHHWoreda = new String[] { "Job", "Step", "South Ari", "Hamer", "Dasenech", "Selamago",
					"Bena Tsemay", "North Ari", "Gynangatom", "Malie", "South Ari Mig", "Hamer Mig", "Dasenech Mig",
					"Selamago Mig", "Bena Tsemay Mig", "North Ari Mig", "Gynangatom Mig", "Malie Mig" };
			final String[] headerPopWoreda = new String[] { "Job", "Step", "South Ari", "Hamer", "Dasenech", "Selamago",
					"Bena Tsemay", "North Ari", "Gynangatom", "Malie", "South Ari Mig", "Hamer Mig", "Dasenech Mig",
					"Selamago Mig", "Bena Tsemay Mig", "North Ari Mig", "Gynangatom Mig", "Malie Mig",
					"South Ari EMP", "Hamer EMP", "Dasenech EMP", "Selamago EMP", "Bena Tsemay EMP", "North Ari EMP",
					"Gynangatom EMP", "Malie EMP" };

			// woreda based

			dataCSVHHWoreda.writeLine(headerHHWoreda);
			dataCSVPOPWoreda.writeLine(headerPopWoreda);
			dataCSVWLTHWoreda.writeLine(headerTLUWoreda);
			dataCSVYIELDWoreda.writeLine(headerTLUWoreda);
			dataCSVTLUWoreda.writeLine(headerTLUWoreda);
			dataCSVBPLWoreda.writeLine(headerTLUWoreda);

		} catch (final IOException ex) {
			Logger.getLogger(Landscape.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	int count = 0;

	public void writeAllCSV(final String job) {
		// String job = Long.toString(state.job());

		final String totalH = Integer.toString(getTotalHH());
		final String totalPop = Integer.toString(gettotalPerson());
		final String totalWealth = Double.toString(gettotalWealthHH());
		final String totalStored = Double.toString(gettotalStoredCaptitalHH());
		final String totalLivestockCap = Double.toString(gettotalLivestockCapitalHH());
		final String totalTLU = Double.toString(gettotalLivestockTLU());
		final String totalBPVL = Double.toString(getBelowPovertyLine());
		final String totalMigr = Integer.toString(ls.getTotalMigrantHH());
		final String totalMigrPop = Integer.toString(ls.getTotalMigrantPOP());

		final String totaladpt = Double.toString(gettotalAdapt());
		final String totalNonadpt = Double.toString(gettotalNonAdapt());
		final String totalAdapExp = Double.toString(gettotalAdaptationExperience());

		final String early = Double.toString(getearlyOnsetRatio());
		final String normalO = Double.toString(getnormalOnsetRatio());
		final String late = Double.toString(getlateOnsetRatio());
		final String belowN = Double.toString(getbelowNormalAmountratio());
		final String normalA = Double.toString(getnormalAmountRatio());
		final String aboveN = Double.toString(getaboveNormalAmountRatio());

		final String earlyC = Double.toString(getearlyOnsetRatioC());
		final String normalOC = Double.toString(getnormalOnsetRatioC());
		final String lateC = Double.toString(getlateOnsetRatioC());
		final String belowNC = Double.toString(getbelowNormalAmountratioC());
		final String normalAC = Double.toString(getnormalAmountRatioC());
		final String aboveNC = Double.toString(getaboveNormalAmountRatioC());

		final String totalMaize = Double.toString(gettotalMaizeHectares());
		final String totalMaizeYield = Double.toString(gettotalMaizeYield());

		final String[] dataHH = new String[] { job, Integer.toString(step), totalH, totalPop, totalWealth, totalStored,
				totalLivestockCap, totalTLU, totalBPVL, totalMigr, totalMigrPop };// totalLivestockCap, totalTLU
		final String[] datacrops = new String[] { job, Integer.toString(step), totalMaize, totalMaizeYield };
		final String[] dataAdpt = new String[] { job, Integer.toString(step), totalH, totaladpt, totalNonadpt,
				totalAdapExp, early, normalO, late, belowN, normalA, aboveN, earlyC, normalOC, lateC, belowNC, normalAC,
				aboveNC };

		try {

			dataCSVFileHH.writeLine(dataHH);
			dataCSVFileCrops.writeLine(datacrops);
			dataCSVFileClimateAdpt.writeLine(dataAdpt);

		} catch (final IOException ex) {
			Logger.getLogger(LandObserver.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	public void writeAllCSVByWoreda(final String job) {

		// households
		int hh_southAri = 0;
		int hh_hamer = 0;
		int hh_dasenech = 0;
		int hh_selama = 0;
		int hh_bena = 0;
		int hh_northAri = 0;
		int hh_gyna = 0;
		int hh_malie = 0;
		// poeple
		int pop_southAri = 0;
		int pop_hamer = 0;
		int pop_dasenech = 0;
		int pop_selama = 0;
		int pop_bena = 0;
		int pop_northAri = 0;
		int pop_gyna = 0;
		int pop_malie = 0;
		// total livestock
		double tlu_southAri = 0;
		double tlu_hamer = 0;
		double tlu_dasenech = 0;
		double tlu_selama = 0;
		double tlu_bena = 0;
		double tlu_northAri = 0;
		double tlu_gyna = 0;
		double tlu_malie = 0;

		// total welath
		double wlth_southAri = 0;
		double wlth_hamer = 0;
		double wlth_dasenech = 0;
		double wlth_selama = 0;
		double wlth_bena = 0;
		double wlth_northAri = 0;
		double wlth_gyna = 0;
		double wlth_malie = 0;

		// below poverty line
		int bpvl_southAri = 0;
		int bpvl_hamer = 0;
		int bpvl_dasenech = 0;
		int bpvl_selama = 0;
		int bpvl_bena = 0;
		int bpvl_northAri = 0;
		int bpvl_gyna = 0;
		int bpvl_malie = 0;

		double noneV = 0;

		for (final Object hh : ls.households.allObjects) {
			final Household household = (Household) hh;

			if (household.getLocation().getWoredaID() == ls.SOUTH_ARI) {
				hh_southAri = hh_southAri + 1;
				ls.totHHWoreda[ls.SOUTH_ARI] = hh_southAri;

				if (household.getFamilyMembers().numObjs > 0) {
					pop_southAri = pop_southAri + household.getFamilyMembers().numObjs;
				}
				if (household.getWealth() > 0) {
					wlth_southAri = wlth_southAri + household.getWealth();
					if (household.getWealth() < household.getFamilyMembers().numObjs
							* ls.params.householdParam.getPovertyLineThreshold()) {
						bpvl_southAri = bpvl_southAri + 1;
					}
				}

				tlu_southAri = tlu_southAri + tluTotal(household);

			} else if (household.getLocation().getWoredaID() == ls.HAMER) {
				hh_hamer = hh_hamer + 1;
				ls.totHHWoreda[ls.HAMER] = hh_hamer;

				if (household.getFamilyMembers().numObjs > 0) {
					pop_hamer = pop_hamer + household.getFamilyMembers().numObjs;
				}
				if (household.getWealth() > 0) {
					wlth_hamer = wlth_hamer + household.getWealth();
					if (household.getWealth() < household.getFamilyMembers().numObjs
							* ls.params.householdParam.getPovertyLineThreshold()) {
						bpvl_hamer = bpvl_hamer + 1;
					}
				}

				tlu_hamer = tlu_hamer + tluTotal(household);

			} else if (household.getLocation().getWoredaID() == ls.DASENECH) {
				hh_dasenech = hh_dasenech + 1;
				ls.totHHWoreda[ls.DASENECH] = hh_dasenech;

				if (household.getFamilyMembers().numObjs > 0) {
					pop_dasenech = pop_dasenech + household.getFamilyMembers().numObjs;
				}
				if (household.getWealth() > 0) {
					wlth_dasenech = wlth_dasenech + household.getWealth();
					if (household.getWealth() < household.getFamilyMembers().numObjs
							* ls.params.householdParam.getPovertyLineThreshold()) {
						bpvl_dasenech = bpvl_dasenech + 1;
					}
				}

				tlu_dasenech = tlu_dasenech + tluTotal(household);

			} else if (household.getLocation().getWoredaID() == ls.SELAMAGO) {
				hh_selama = hh_selama + 1;
				ls.totHHWoreda[ls.SELAMAGO] = hh_selama;

				if (household.getFamilyMembers().numObjs > 0) {
					pop_selama = pop_selama + household.getFamilyMembers().numObjs;
				}
				if (household.getWealth() > 0) {
					wlth_selama = wlth_selama + household.getWealth();
					if (household.getWealth() < household.getFamilyMembers().numObjs
							* ls.params.householdParam.getPovertyLineThreshold()) {
						bpvl_selama = bpvl_selama + 1;
					}
				}

				tlu_selama = tlu_selama + tluTotal(household);

			} else if (household.getLocation().getWoredaID() == ls.BENA_TSEMAY) {
				hh_bena = hh_bena + 1;
				ls.totHHWoreda[ls.BENA_TSEMAY] = hh_bena;

				if (household.getFamilyMembers().numObjs > 0) {
					pop_bena = pop_bena + household.getFamilyMembers().numObjs;
				}
				if (household.getWealth() > 0) {
					wlth_bena = wlth_bena + household.getWealth();
					if (household.getWealth() < household.getFamilyMembers().numObjs
							* ls.params.householdParam.getPovertyLineThreshold()) {
						bpvl_bena = bpvl_bena + 1;
					}
				}

				tlu_bena = tlu_bena + tluTotal(household);

			} else if (household.getLocation().getWoredaID() == ls.NORTH_ARI) {
				hh_northAri = hh_northAri + 1;
				ls.totHHWoreda[ls.NORTH_ARI] = hh_northAri;

				if (household.getFamilyMembers().numObjs > 0) {
					pop_northAri = pop_northAri + household.getFamilyMembers().numObjs;
				}
				if (household.getWealth() > 0) {
					wlth_northAri = wlth_northAri + household.getWealth();
					if (household.getWealth() < household.getFamilyMembers().numObjs
							* ls.params.householdParam.getPovertyLineThreshold()) {
						bpvl_northAri = bpvl_northAri + 1;
					}
				}

				tlu_northAri = tlu_northAri + tluTotal(household);

			} else if (household.getLocation().getWoredaID() == ls.GYNANGATOM) {
				hh_gyna = hh_gyna + 1;
				ls.totHHWoreda[ls.GYNANGATOM] = hh_gyna;

				if (household.getFamilyMembers().numObjs > 0) {
					pop_gyna = pop_gyna + household.getFamilyMembers().numObjs;
				}
				if (household.getWealth() > 0) {
					wlth_gyna = wlth_gyna + household.getWealth();
					if (household.getWealth() < household.getFamilyMembers().numObjs
							* ls.params.householdParam.getPovertyLineThreshold()) {
						bpvl_gyna = bpvl_gyna + 1;
					}
				}

				tlu_gyna = tlu_gyna + tluTotal(household);

			} else if (household.getLocation().getWoredaID() == ls.MALIE) {
				hh_malie = hh_malie + 1;
				ls.totHHWoreda[ls.MALIE] = hh_malie;

				if (household.getFamilyMembers().numObjs > 0) {
					pop_malie = pop_malie + household.getFamilyMembers().numObjs;
				}
				if (household.getWealth() > 0) {
					wlth_malie = wlth_malie + household.getWealth();
					if (household.getWealth() < household.getFamilyMembers().numObjs
							* ls.params.householdParam.getPovertyLineThreshold()) {
						bpvl_malie = bpvl_malie + 1;
					}
				}

				tlu_malie = tlu_malie + tluTotal(household);

			} else {
				noneV = 0;
			}

		}

		// crop yield - income ( since it is based currency -same unit
		double yld_southAri = 0;
		double yld_hamer = 0;
		double yld_dasenech = 0;
		double yld_selama = 0;
		double yld_bena = 0;
		double yld_northAri = 0;
		double yld_gyna = 0;
		double yld_malie = 0;
		for (final Object c : ls.crops.allObjects) {
			final Crop crop = (Crop) c;
			if (crop.getCropLand().getIsOccupied() != true) {
				continue;
			}

			if (crop.getCropLand().getWoredaID() == ls.SOUTH_ARI) {
				yld_southAri = yld_southAri + yieldCrop(crop);

			} else if (crop.getCropLand().getWoredaID() == ls.HAMER) {
				yld_hamer = yld_hamer + yieldCrop(crop);
			} else if (crop.getCropLand().getWoredaID() == ls.DASENECH) {
				yld_dasenech = yld_dasenech + yieldCrop(crop);
			} else if (crop.getCropLand().getWoredaID() == ls.SELAMAGO) {
				yld_selama = yld_selama + yieldCrop(crop);
			} else if (crop.getCropLand().getWoredaID() == ls.BENA_TSEMAY) {
				yld_bena = yld_bena + yieldCrop(crop);
			} else if (crop.getCropLand().getWoredaID() == ls.NORTH_ARI) {
				yld_northAri = yld_northAri + yieldCrop(crop);
			} else if (crop.getCropLand().getWoredaID() == ls.GYNANGATOM) {
				yld_gyna = yld_gyna + yieldCrop(crop);
			} else if (crop.getCropLand().getWoredaID() == ls.MALIE) {
				yld_malie = yld_malie + yieldCrop(crop);
			} else {
				noneV = 0;
			}
		}

		// hh
		final String SouthAriHH = Integer.toString(hh_southAri);
		final String HamerHH = Integer.toString(hh_hamer);
		final String DasenechHH = Integer.toString(hh_dasenech);
		final String SelamagoHH = Integer.toString(hh_selama);
		final String BenaTsemayHH = Integer.toString(hh_bena);
		final String NorthAriHH = Integer.toString(hh_northAri);
		final String GynangatomHH = Integer.toString(hh_gyna);
		final String malieHH = Integer.toString(hh_malie);

		// migrantHH
		final String SouthAriMigHH = Integer.toString(ls.getTotalMigrantHHWoreda()[ls.SOUTH_ARI]);
		final String HamerMigHH = Integer.toString(ls.getTotalMigrantHHWoreda()[ls.HAMER]);
		final String DasenechMigHH = Integer.toString(ls.getTotalMigrantHHWoreda()[ls.DASENECH]);
		final String SelamagoMigHH = Integer.toString(ls.getTotalMigrantHHWoreda()[ls.SELAMAGO]);
		final String BenaTsemayMigHH = Integer.toString(ls.getTotalMigrantHHWoreda()[ls.BENA_TSEMAY]);
		final String NorthAriMigHH = Integer.toString(ls.getTotalMigrantHHWoreda()[ls.NORTH_ARI]);
		final String GynangatomMigHH = Integer.toString(ls.getTotalMigrantHHWoreda()[ls.GYNANGATOM]);
		final String malieMigHH = Integer.toString(ls.getTotalMigrantHHWoreda()[ls.MALIE]);

		// pop
		final String SouthAriPOP = Integer.toString(pop_southAri);
		final String HamerPOP = Integer.toString(pop_hamer);
		final String DasenechPOP = Integer.toString(pop_dasenech);
		final String SelamagoPOP = Integer.toString(pop_selama);
		final String BenaTsemayPOP = Integer.toString(pop_bena);
		final String NorthAriPOP = Integer.toString(pop_northAri);
		final String GynangatomPOP = Integer.toString(pop_gyna);
		final String maliePOP = Integer.toString(pop_malie);

		// POP MIGRANT

		final String SouthAriMigPOP = Integer.toString(ls.getTotalMigrantPOPWoreda()[ls.SOUTH_ARI]);
		final String HamerMigPOP = Integer.toString(ls.getTotalMigrantPOPWoreda()[ls.HAMER]);
		final String DasenechMigPOP = Integer.toString(ls.getTotalMigrantPOPWoreda()[ls.DASENECH]);
		final String SelamagoMigPOP = Integer.toString(ls.getTotalMigrantPOPWoreda()[ls.SELAMAGO]);
		final String BenaTsemayMigPOP = Integer.toString(ls.getTotalMigrantPOPWoreda()[ls.BENA_TSEMAY]);
		final String NorthAriMigPOP = Integer.toString(ls.getTotalMigrantPOPWoreda()[ls.NORTH_ARI]);
		final String GynangatomMigPOP = Integer.toString(ls.getTotalMigrantPOPWoreda()[ls.GYNANGATOM]);
		final String malieMigPOP = Integer.toString(ls.getTotalMigrantPOPWoreda()[ls.MALIE]);

		// employees

		final String SouthAriEMP = Integer.toString(totEmployeeWoreda[ls.SOUTH_ARI]);
		final String HamerEMP = Integer.toString(totEmployeeWoreda[ls.HAMER]);
		final String DasenechEMP = Integer.toString(totEmployeeWoreda[ls.DASENECH]);
		final String SelamagoEMP = Integer.toString(totEmployeeWoreda[ls.SELAMAGO]);
		final String BenaTsemayEMP = Integer.toString(totEmployeeWoreda[ls.BENA_TSEMAY]);
		final String NorthAriEMP = Integer.toString(totEmployeeWoreda[ls.NORTH_ARI]);
		final String GynangatomEMP = Integer.toString(totEmployeeWoreda[ls.GYNANGATOM]);
		final String malieEMP = Integer.toString(totEmployeeWoreda[ls.MALIE]);

		// wealth

		final String SouthAriWLTH = Double.toString(wlth_southAri);
		final String HamerWLTH = Double.toString(wlth_hamer);
		final String DasenechWLTH = Double.toString(wlth_dasenech);
		final String SelamagoWLTH = Double.toString(wlth_selama);
		final String BenaTsemayWLTH = Double.toString(wlth_bena);
		final String NorthAriWLTH = Double.toString(wlth_northAri);
		final String GynangatomWLTH = Double.toString(wlth_gyna);
		final String malieWLTH = Double.toString(wlth_malie);

		// yield

		final String SouthAriYLD = Double.toString(yld_southAri);
		final String HamerYLD = Double.toString(yld_hamer);
		final String DasenechYLD = Double.toString(yld_dasenech);
		final String SelamagoYLD = Double.toString(yld_selama);
		final String BenaTsemayYLD = Double.toString(yld_bena);
		final String NorthAriYLD = Double.toString(yld_northAri);
		final String GynangatomYLD = Double.toString(yld_gyna);
		final String malieYLD = Double.toString(yld_malie);

		// tlu

		final String SouthAri = Double.toString(tlu_southAri);
		final String Hamer = Double.toString(tlu_hamer);
		final String Dasenech = Double.toString(tlu_dasenech);
		final String Selamago = Double.toString(tlu_selama);
		final String BenaTsemay = Double.toString(tlu_bena);
		final String NorthAri = Double.toString(tlu_northAri);
		final String Gynangatom = Double.toString(tlu_gyna);
		final String malie = Double.toString(tlu_malie);

		// below poverty line

		// hh
		final String SouthAriBPL = Double.toString(bpvl_southAri / (1.0 * hh_southAri));
		final String HamerBPL = Double.toString(bpvl_hamer / (1.0 * hh_hamer));
		final String DasenechBPL = Double.toString(bpvl_dasenech / (1.0 * hh_dasenech));
		final String SelamagoBPL = Double.toString(bpvl_selama / (1.0 * hh_selama));
		final String BenaTsemayBPL = Double.toString(bpvl_bena / (1.0 * hh_bena));
		final String NorthAriBPL = Double.toString(bpvl_northAri / (1.0 * hh_northAri));
		final String GynangatomBPL = Double.toString(bpvl_gyna / (1.0 * hh_gyna));
		final String malieBPL = Double.toString(bpvl_malie / (1.0 * hh_malie));

		final String[] dataWoredaHH = new String[] { job, Integer.toString(step), SouthAriHH, HamerHH, DasenechHH,
				SelamagoHH, BenaTsemayHH, NorthAriHH, GynangatomHH, malieHH, SouthAriMigHH, HamerMigHH, DasenechMigHH,
				SelamagoMigHH, BenaTsemayMigHH, NorthAriMigHH, GynangatomMigHH, malieMigHH };
		final String[] dataWoredaPOP = new String[] { job, Integer.toString(step), SouthAriPOP, HamerPOP, DasenechPOP,
				SelamagoPOP, BenaTsemayPOP, NorthAriPOP, GynangatomPOP, maliePOP, SouthAriMigPOP, HamerMigPOP,
				DasenechMigPOP, SelamagoMigPOP, BenaTsemayMigPOP, NorthAriMigPOP, GynangatomMigPOP, malieMigPOP,
				SouthAriEMP, HamerEMP, DasenechEMP, SelamagoEMP, BenaTsemayEMP, NorthAriEMP, GynangatomEMP, malieEMP };
		final String[] dataWoredaWLTH = new String[] { job, Integer.toString(step), SouthAriWLTH, HamerWLTH,
				DasenechWLTH, SelamagoWLTH, BenaTsemayWLTH, NorthAriWLTH, GynangatomWLTH, malieWLTH };
		final String[] dataWoredaYLD = new String[] { job, Integer.toString(step), SouthAriYLD, HamerYLD, DasenechYLD,
				SelamagoYLD, BenaTsemayYLD, NorthAriYLD, GynangatomYLD, malieYLD };
		final String[] dataTLUWoreda = new String[] { job, Integer.toString(step), SouthAri, Hamer, Dasenech, Selamago,
				BenaTsemay, NorthAri, Gynangatom, malie };
		final String[] dataWoredaBPL = new String[] { job, Integer.toString(step), SouthAriBPL, HamerBPL, DasenechBPL,
				SelamagoBPL, BenaTsemayBPL, NorthAriBPL, GynangatomBPL, malieBPL };
		// when to export raster;- everyday at midnight

		try {

			dataCSVHHWoreda.writeLine(dataWoredaHH);
			dataCSVPOPWoreda.writeLine(dataWoredaPOP);
			dataCSVWLTHWoreda.writeLine(dataWoredaWLTH);
			dataCSVYIELDWoreda.writeLine(dataWoredaYLD);
			dataCSVTLUWoreda.writeLine(dataTLUWoreda);
			dataCSVBPLWoreda.writeLine(dataWoredaBPL);

		} catch (final IOException ex) {
			Logger.getLogger(LandObserver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public double tluTotal(final Household household) {
		double tlu = 0;
		if (household.getHerd() != null && household.getHerd().getHerdSizeTLU() > 0) {
			tlu = household.getHerd().getHerdSizeTLU();
		}

		return tlu;
	}

	public double yieldCrop(final Crop crop) {

		double yield = 0;

		if (crop.getCropID() == CropParameters.MAIZE_ID) {
			yield = crop.getYield() * CropParameters.MAIZE_PRICE;
		}

		return yield;
	}

	public void writeAllGrids(final String job) {

		try {

			if (writeGrid == true) {
				count = count + 1;
				final long now = System.nanoTime();

				final String filenameAsc = Long.toString(now)
						+ "_" + getExperimentName() + "_" + count + "_householdASC.asc";
				final String filenameExpAsc = Long.toString(now)
						+ "_" + getExperimentName() + "_" + count + "_householdADPExpASC.asc";

				final String filenameFarming = Long.toString(now)
						+ "_" + getExperimentName() + "_" + count + "_householdFarmingASC.asc";
				final String filenameHerding = Long.toString(now)
						+ "_" + getExperimentName() + "_" + count + "_householdHerdingASC.asc";

				final BufferedWriter dataASCHH = new BufferedWriter(new FileWriter(filenameAsc));
				final BufferedWriter dataExpHH = new BufferedWriter(new FileWriter(filenameExpAsc));
				final BufferedWriter dataFarming = new BufferedWriter(new FileWriter(filenameFarming));
				final BufferedWriter dataHerding = new BufferedWriter(new FileWriter(filenameHerding));

				writeGridFiles(ls);

				ArcInfoASCGridExporter.write(ls.outputGrid, dataASCHH);
				ArcInfoASCGridExporter.write(ls.outputGridAdpExp, dataExpHH);
				ArcInfoASCGridExporter.write(ls.outputGridFarming, dataFarming);
				ArcInfoASCGridExporter.write(ls.outputGridHerding, dataHerding);
				omoLGridBag.add(dataASCHH);
				omoLGridBag.add(dataExpHH);
				omoLGridBag.add(dataFarming);
				omoLGridBag.add(dataHerding);

			}
		} catch (final IOException ex) {
			Logger.getLogger(LandObserver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void step(final SimState state) {
		ls = (Landscape) state;

		householdStat();
		personStat();

		livestockStat();
		cropStat();
		//

		rainfallStat();

		addCharts((ls.schedule.time()));

		final String job = Long.toString(state.job());

		if (ls.schedule.getSteps() % ls.params.observerParam.getwriteGridTimerFrequency() == 0) { // every month ??
			writeGrid = true;
		} else {
			writeGrid = false;
		}

		if (ls.params.observerParam.getIsWritingStats() == true) {
			writeAllCSV(job);
			writeAllCSVByWoreda(job);
			writeAllGrids(job);

		}

		step++;
	}

	void finish() {
		try {
			dataFileBufferHH.close();
			dataFileBufferCrops.close();
			dataFileBufferClimateAdpt.close();
			dataFileBufferTLUWoreda.close();

			dataFileBufferHHWoreda.close();
			dataFileBufferPOPWoreda.close();
			dataFileBufferWLTHWoreda.close();
			dataFileBufferYIELDWoreda.close();
			dataFileBufferBPLWoreda.close();

			for (final Object o : omoLGridBag) {
				final BufferedWriter bw = (BufferedWriter) o;
				bw.close();
			}

		} catch (final IOException ex) {
			Logger.getLogger(LandObserver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void createLogFile() throws IOException {
		final long now = System.nanoTime();

		final String filename = Long.toString(now)
				+ "_" + getExperimentName() + "_Household.csv";
//
		final String filename_crop = Long.toString(now)
				+ "_" + getExperimentName() + "_crop.csv";
		final String filename_adpt = Long.toString(now)
				+ "_" + getExperimentName() + "_adpt.csv";
		final String filename_tlu = Long.toString(now)
				+ "_" + getExperimentName() + "_TLUWoreda.csv";

		final String filename_hh = Long.toString(now)
				+ "_" + getExperimentName() + "_HHWoreda.csv";

		final String filename_pop = Long.toString(now)
				+ "_" + getExperimentName() + "_POPWoreda.csv";

		final String filename_wlth = Long.toString(now)
				+ "_" + getExperimentName() + "_WLTHWoreda.csv";

		final String filename_yld = Long.toString(now)
				+ "_" + getExperimentName() + "_YIELDWoreda.csv";

		final String filename_bpl = Long.toString(now)
				+ "_" + getExperimentName() + "_BPLineWoreda.csv";

		// household
		dataFileBufferHH = new BufferedWriter(new FileWriter(filename));
		dataCSVFileHH = new CSVWriter(dataFileBufferHH);

		// crops
		dataFileBufferCrops = new BufferedWriter(new FileWriter(filename_crop));
		dataCSVFileCrops = new CSVWriter(dataFileBufferCrops);

		dataFileBufferClimateAdpt = new BufferedWriter(new FileWriter(filename_adpt));
		dataCSVFileClimateAdpt = new CSVWriter(dataFileBufferClimateAdpt);

		dataFileBufferTLUWoreda = new BufferedWriter(new FileWriter(filename_tlu));
		dataCSVTLUWoreda = new CSVWriter(dataFileBufferTLUWoreda);

		dataFileBufferHHWoreda = new BufferedWriter(new FileWriter(filename_hh));
		dataCSVHHWoreda = new CSVWriter(dataFileBufferHHWoreda);

		dataFileBufferPOPWoreda = new BufferedWriter(new FileWriter(filename_pop));
		dataCSVPOPWoreda = new CSVWriter(dataFileBufferPOPWoreda);

		dataFileBufferWLTHWoreda = new BufferedWriter(new FileWriter(filename_wlth));
		dataCSVWLTHWoreda = new CSVWriter(dataFileBufferWLTHWoreda);

		dataFileBufferYIELDWoreda = new BufferedWriter(new FileWriter(filename_yld));
		dataCSVYIELDWoreda = new CSVWriter(dataFileBufferYIELDWoreda);

		dataFileBufferBPLWoreda = new BufferedWriter(new FileWriter(filename_bpl));
		dataCSVBPLWoreda = new CSVWriter(dataFileBufferBPLWoreda);

	}

	public void writeGridFiles(final Landscape ls) {

		for (int i = 0; i < ls.allLand.getWidth(); i++) {
			for (int j = 0; j < ls.allLand.getHeight(); j++) {
				((DoubleGrid2D) ls.outputGrid.getGrid()).set(i, j, 0);
				((DoubleGrid2D) ls.outputGridAdpExp.getGrid()).set(i, j, 0);
				((DoubleGrid2D) ls.outputGridFarming.getGrid()).set(i, j, 0);
				((DoubleGrid2D) ls.outputGridHerding.getGrid()).set(i, j, 0);

				((DoubleGrid2D) ls.outputGridFarming.getGrid()).set(i, j,
						((Parcel) ls.allLand.field[i][j]).getFallowPeriodCounter());
				// after accounting the faming years, set the parcel to 0

				((Parcel) ls.allLand.field[i][j]).setFallowPeriodCounter(0);
			}
		}

		// wealth
		for (final Object o : ls.households.allObjects) {
			final Household h = (Household) o;

			if (h.getWealth() > 0 && h.getFamilyMembers().numObjs > 0) {
				((DoubleGrid2D) ls.outputGrid.getGrid()).set(h.getLocation().getX(), h.getLocation().getY(),
						h.getWealth() / (h.getFamilyMembers().numObjs * 1.0));
				((DoubleGrid2D) ls.outputGridAdpExp.getGrid()).set(h.getLocation().getX(), h.getLocation().getY(),
						h.getCurrenAdaptationMechanism());
				if (h.getHerd() != null && h.getHerd().getHerdSizeTLU() > 0) {
					((DoubleGrid2D) ls.outputGridHerding.getGrid()).set(h.getLocation().getX(), h.getLocation().getY(),
							h.getHerd().getHerdSizeTLU() / (h.getFamilyMembers().numObjs * 1.0));

				} else {
					((DoubleGrid2D) ls.outputGridHerding.getGrid()).set(h.getLocation().getX(), h.getLocation().getY(),
							0);

				}

			}
		}

	}

	private void writeObject(final java.io.ObjectOutputStream out)
			throws IOException {
		out.writeInt(step);

	}

	private void readObject(final java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		step = in.readInt();

		startLogFile();
	}
}
