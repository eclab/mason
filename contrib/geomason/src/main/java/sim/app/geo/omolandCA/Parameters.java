package sim.app.geo.omolandCA;

import java.io.File;
import java.io.IOException;
//import java.util.logging.Level;

import ec.util.Parameter;
import ec.util.ParameterDatabase;

/**
 *
 * @author gmu
 */
public class Parameters {

	// private static final long serialVersionUID = 1L;
	private static final long serialVersionUID = System.nanoTime();
	ObserverParams observerParam = new ObserverParams();
	GlobalParamters globalParam = new GlobalParamters();
	ClimateParamters climateParam = new ClimateParamters();
	HouseholdParamters householdParam = new HouseholdParamters();
	HerdingParamters herdingParam = new HerdingParamters();
	FarmingParameters farmingParam = new FarmingParameters();
	Vegetation vegetationParam = new Vegetation();

//    ParcelParamters parcel = new ParcelParamters();
//    CropParamters crop = new CropParamters();
//
	private final static String A_FILE = "-file";

	public Parameters(final String[] args) {
		if (args != null) {
			loadParameters(openParameterDatabase(args));
		}
	}

	// <editor-fold defaultstate="collapsed" desc="ECJ ParameterDatabase methods">
	/**
	 * Initialize parameter database from file
	 *
	 * If there exists an command line argument '-file', create a parameter database
	 * from the file specified. Otherwise create an empty parameter database.
	 *
	 * @param args contains command line arguments
	 * @return newly created parameter data base
	 *
	 * @see loadParameters()
	 */
	private static ParameterDatabase openParameterDatabase(final String[] args) {
		ParameterDatabase parameters = null;
		for (int x = 0; x < args.length - 1; x++) {
			if (args[x].equals(Parameters.A_FILE)) {
				try {
					final File parameterDatabaseFile = new File(args[x + 1]);
					parameters = new ParameterDatabase(parameterDatabaseFile.getAbsoluteFile());
				} catch (final IOException ex) {
					ex.printStackTrace();
				}
				break;
			}
		}
		if (parameters == null) {
			System.out.println("\nNo parameter file was specified");// ("\nNo parameter file was specified");
			parameters = new ParameterDatabase();
		}
		return parameters;
	}

	private void loadParameters(final ParameterDatabase parameterDB) {
		// write statistics
		observerParam.setIsWritingStats(returnBooleanParameter(parameterDB, "writeStats",
				observerParam.getIsWritingStats()));
		observerParam.setwriteGridTimerFrequency(returnIntParameter(parameterDB, "writeGridTimerFrequency",
				observerParam.getwriteGridTimerFrequency()));
		// global
		globalParam.setInitialNumberOfHouseholds(returnIntParameter(parameterDB, "InitialNumberOfHouseholds",
				globalParam.getInitialNumberOfHouseholds()));

		globalParam.setExpFileName(returnStringParameter(parameterDB, "experimentName",
				globalParam.getExpFileName()));

		// household
		householdParam
				.setAdaptationIntentionThreshold(returnDoubleParameter(parameterDB, "AdaptationIntentionThreshold",
						householdParam.getAdaptationIntentionThreshold()));
		householdParam.setAdaptiveAgentLearningRate(returnDoubleParameter(parameterDB, "AdaptiveAgentLearningRate",
				householdParam.getAdaptiveAgentLearningRate()));
		householdParam.setCognitiveBias(returnDoubleParameter(parameterDB, "cognitiveBiasAlpha",
				householdParam.getCognitiveBias()));
		householdParam.setConsumptionExp(returnDoubleParameter(parameterDB, "ConsumptionExp",
				householdParam.getConsumptionExp()));
		householdParam.setCostAdptElasticity(returnDoubleParameter(parameterDB, "adptationElasticity",
				householdParam.getCostAdptElasticity()));
		householdParam.setCostOfCredit(returnDoubleParameter(parameterDB, "CostOfCredit",
				householdParam.getCostOfCredit()));
		householdParam.setMalAdptHouseholdRatio(returnDoubleParameter(parameterDB, "MalAdptHouseholdRatio",
				householdParam.getMalAdptHouseholdRatio()));
		householdParam.setMimimumExpenditurePerPerson(returnDoubleParameter(parameterDB, "MimimumExpenditurePerPerson",
				householdParam.getMimimumExpenditurePerPerson()));
		householdParam.setMinLaborManDay(returnDoubleParameter(parameterDB, "MinLaborManDay",
				householdParam.getMinLaborManDay()));
		householdParam.setMinimumLaborAge(returnIntParameter(parameterDB, "MinimumLaborAge",
				householdParam.getMinimumLaborAge()));
		householdParam.setPercentageInhertanceWedding(returnDoubleParameter(parameterDB, "PercentageInhertanceWedding",
				householdParam.getPercentageInhertanceWedding()));
		householdParam.setPersonBirthRate(returnDoubleParameter(parameterDB, "PersonBirthRate",
				householdParam.getPersonBirthRate()));
		householdParam.setPersonDeathRate(returnDoubleParameter(parameterDB, "PersonDeathRate",
				householdParam.getPersonDeathRate()));
		householdParam.setPovertyLineThreshold(returnDoubleParameter(parameterDB, "PovertyLineThreshold",
				householdParam.getPovertyLineThreshold()));
		householdParam.setProRiskDeduction(returnDoubleParameter(parameterDB, "ProRiskDeduction",
				householdParam.getProRiskDeduction()));
		householdParam.setRiskElasticity(returnDoubleParameter(parameterDB, "RiskElasticity",
				householdParam.getRiskElasticity()));

		// Herding

		herdingParam.setAverageHerdPrice(returnDoubleParameter(parameterDB, "AverageHerdPrice",
				herdingParam.getAverageHerdPrice()));

		herdingParam.setHerdDailyConsumptionRate(returnDoubleParameter(parameterDB, "HerdDailyConsumptionRate",
				herdingParam.getHerdDailyConsumptionRate()));
		herdingParam.setHerdDestockingProportion(returnDoubleParameter(parameterDB, "HerdDestockingProportion",
				herdingParam.getHerdDestockingProportion()));
		herdingParam.setHerdGrowthRate(returnDoubleParameter(parameterDB, "HerdGrowthRate",
				herdingParam.getHerdGrowthRate()));
		herdingParam.setHerdHealthConditionFactor(returnDoubleParameter(parameterDB, "HerdHealthConditionFactor",
				herdingParam.getHerdHealthConditionFactor()));
		herdingParam.setHerdLaborEfficiencyFactor(returnDoubleParameter(parameterDB, "HerdLaborEfficiencyFactor",
				herdingParam.getHerdLaborEfficiencyFactor()));
		herdingParam.setHerdMaxDailyDMIntake(returnDoubleParameter(parameterDB, "HerdMaxDailyDMIntake",
				herdingParam.getHerdMaxDailyDMIntake()));
		herdingParam.setHerdMaxFoodStored(returnDoubleParameter(parameterDB, "HerdMaxFoodStored",
				herdingParam.getHerdMaxFoodStored()));
		herdingParam.setHerdRestockingProportion(returnDoubleParameter(parameterDB, "HerdRestockingProportion",
				herdingParam.getHerdRestockingProportion()));
		herdingParam.setHerdSplitingThreshold(returnDoubleParameter(parameterDB, "HerdSplitingThreshold",
				herdingParam.getHerdSplitingThreshold()));
		herdingParam.setHerderMinVisionRange(returnIntParameter(parameterDB, "HerderMinVisionRange",
				herdingParam.getHerderMinVisionRange()));
		herdingParam.setHerderScoutingRange(returnIntParameter(parameterDB, "HerderScoutingRange",
				herdingParam.getHerderScoutingRange()));
		herdingParam.setProportionLaborToTLU(returnDoubleParameter(parameterDB, "ProportionLaborToTLU",
				herdingParam.getProportionLaborToTLU()));

		// farming
		farmingParam.setAverageInitialFarmingCost(returnDoubleParameter(parameterDB, "AverageInitialFarmingCost",
				farmingParam.getAverageInitialFarmingCost()));
		farmingParam.setFarmCobbDaglasCoeff(returnDoubleParameter(parameterDB, "FarmCobbDaglasCoeff",
				farmingParam.getFarmCobbDaglasCoeff()));
		farmingParam.setFarmInputCost(returnDoubleParameter(parameterDB, "FarmInputCost",
				farmingParam.getFarmInputCost()));
		farmingParam.setFarmLaborEfficiencyFactor(returnDoubleParameter(parameterDB, "FarmLaborEfficiencyFactor",
				farmingParam.getFarmLaborEfficiencyFactor()));
		farmingParam.setIrrigationFarmCost(returnDoubleParameter(parameterDB, "IrrigationFarmCost",
				farmingParam.getIrrigationFarmCost()));
		farmingParam.setIsIntesificationPossible(returnBooleanParameter(parameterDB, "IsIntesificationPossible",
				farmingParam.getIsIntesificationPossible()));
		farmingParam.setMaxDaysPlantingAfterLandPrep(returnIntParameter(parameterDB, "MaxDaysPlantingAfterLandPrep",
				farmingParam.getMaxDaysPlantingAfterLandPrep()));
		farmingParam.setMinimumAnnualRainfall(returnDoubleParameter(parameterDB, "MinimumAnnualRainfall",
				farmingParam.getMinimumAnnualRainfall()));
		farmingParam.setProportionLaborToAreaHa(returnDoubleParameter(parameterDB, "ProportionLaborToAreaHa",
				farmingParam.getProportionLaborToAreaHa()));
		farmingParam.setWeedingMeanDay(returnIntParameter(parameterDB, "WeedingMeanDay",
				farmingParam.getWeedingMeanDay()));

		// vegetation
		vegetationParam.setBaseGrowthRateContorler(returnDoubleParameter(parameterDB, "BaseGrowthRateContorler",
				vegetationParam.getBaseGrowthRateContorler()));
		vegetationParam.setAverageRainfallGrowthCutOFF(returnDoubleParameter(parameterDB, "averageRainfallGrowthCutOFF",
				vegetationParam.getAverageRainfallGrowthCutOFF()));
//        vegetation.setGrowthThreshold(returnDoubleParameter(parameterDB, "GrowthThreshold",
//                vegetation.getGrowthThreshold()));
		vegetationParam.setMaxVegetationPerHectare(returnDoubleParameter(parameterDB, "MaxVegetationPerHectare",
				vegetationParam.getMaxVegetationPerHectare()));
		vegetationParam.setMinVegetation(returnDoubleParameter(parameterDB, "MinVegetation",
				vegetationParam.getMinVegetation()));

		// climate

		climateParam.setFirstDroughtYear(returnIntParameter(parameterDB, "FirstDroughtYear",
				climateParam.getFirstDroughtYear()));
		climateParam.setFirstWetYear(returnIntParameter(parameterDB, "FirstWetYear",
				climateParam.getFirstWetYear()));
		climateParam.setFrequencyOfDroughtYears(returnIntParameter(parameterDB, "FrequencyOfDroughtYears",
				climateParam.getFrequencyOfDroughtYears()));
		climateParam.setFrequencyOfWetYears(returnIntParameter(parameterDB, "FrequencyOfWetYears",
				climateParam.getFrequencyOfWetYears()));
		climateParam.setIsDroughtYearsOn(returnBooleanParameter(parameterDB, "IsDroughtYearsOn",
				climateParam.getIsDroughtYearsOn()));

		climateParam.setisSteadyState(returnBooleanParameter(parameterDB, "isSteadyState",
				climateParam.getisSteadyState()));

		climateParam.setIsRandomizedRainYears(returnBooleanParameter(parameterDB, "IsRandomizedRainYears",
				climateParam.getIsRandomizedRainYears()));
		climateParam.setIsWetYearsOn(returnBooleanParameter(parameterDB, "IsWetYearsOn",
				climateParam.getIsWetYearsOn()));

		climateParam.setMinDaysOfMinimumrainfall(returnIntParameter(parameterDB, "MinDaysOfMinimumrainfall",
				climateParam.getMinDaysOfMinimumrainfall()));
		climateParam.setMinDaysofMERainPercetage(returnIntParameter(parameterDB, "MinDaysofMERainPercetage",
				climateParam.getMinDaysofMERainPercetage()));
		climateParam.setMinimimCessRainThreshold(returnDoubleParameter(parameterDB, "MinimimCessRainThreshold",
				climateParam.getMinimimCessRainThreshold()));
		climateParam.setMinimimEffectiveRainfall(returnDoubleParameter(parameterDB, "MinimimEffectiveRainfall",
				climateParam.getMinimimEffectiveRainfall()));
		climateParam.setMinimumOnsetRainThreshold(returnDoubleParameter(parameterDB, "MinimumOnsetRainThreshold",
				climateParam.getMinimumOnsetRainThreshold()));
		climateParam.setNumberOfDroughtYears(returnIntParameter(parameterDB, "NumberOfDroughtYears",
				climateParam.getNumberOfDroughtYears()));
		climateParam.setNumberOfWetYears(returnIntParameter(parameterDB, "NumberOfWetYears",
				climateParam.getNumberOfWetYears()));
		climateParam.setSeverityOfDrought(returnDoubleParameter(parameterDB, "SeverityOfDrought",
				climateParam.getSeverityOfDrought()));
		climateParam.setSeverityOfWet(returnDoubleParameter(parameterDB, "SeverityOfWet",
				climateParam.getSeverityOfWet()));

	}

	public int returnIntParameter(final ParameterDatabase paramDB, final String parameterName, final int defaultValue) {
		return paramDB.getIntWithDefault(new Parameter(parameterName), null, defaultValue);
	}

	public boolean returnBooleanParameter(final ParameterDatabase paramDB, final String parameterName,
			final boolean defaultValue) {
		return paramDB.getBoolean(new Parameter(parameterName), null, defaultValue);
	}

	public double returnDoubleParameter(final ParameterDatabase paramDB, final String parameterName,
			final double defaultValue) {
		return paramDB.getDoubleWithDefault(new Parameter(parameterName), null, defaultValue);
	}

	public String returnStringParameter(final ParameterDatabase paramDB, final String parameterName,
			final String defaultValue) {
		return paramDB.getStringWithDefault(new Parameter(parameterName), null, defaultValue);
	}

	public class ObserverParams {

		private static final long serialVersionUID = 1L;
//        private Level loggerLevel = Level.INFO;
		/**
		 * true if we should write simulation statistics to file
		 *
		 * The file is a comma separate value (CSV) file with each row containing
		 * statistics for each simulation tick. worldObserver is responsible for writing
		 * to this file.
		 *
		 * XXX What is the output file name? What kinds of things are written to the
		 * file?
		 *
		 * @see worldObserver
		 */
		private boolean writeStats = true;
		private int writeGridTimerFrequency = 3650;

		// <editor-fold defaultstate="collapsed" desc="Accessors">
		public void setIsWritingStats(final boolean doWriteStats) {
			writeStats = doWriteStats;
		}

		public boolean getIsWritingStats() {
			return writeStats;
		}

		public void setwriteGridTimerFrequency(final int hh) {
			writeGridTimerFrequency = hh;
		}

		public int getwriteGridTimerFrequency() {
			return writeGridTimerFrequency;
		}
		// </editor-fold>
	}

	public class GlobalParamters {

		private int initialNumberOfHouseholds = 50000;

		private final double adaptationTrainingCoverage = 0.2;
		private final double reliefSupportPerPerson = 100; //
		private final double probReliefSupportCoverage = 0.01;

		private String experimentName = "Default";
		private final boolean isEvictionPolicy = false;

		public void setInitialNumberOfHouseholds(final int hh) {
			initialNumberOfHouseholds = hh;
		}

		public int getInitialNumberOfHouseholds() {
			return initialNumberOfHouseholds;
		}

		public void setExpFileName(final String f) {
			experimentName = f;
		}

		public String getExpFileName() {
			return experimentName;
		}

	}

	public class HouseholdParamters {

		private double povertyLineThreshold = 547.0; // 1.5 birr per person per day
		private double personBirthRate = 0.025; // ethiopia - birth=43 per 1000 - death= 11 per 1000
		private double personDeathRate = 0.007; // all age per day -
		private double ADPTATION_THRESHOLD = 0.4; // adaptation intetion < 0.3 = no adaptation
		private double MINIMUMEXPENDITUREPERPERSON = 60;// 3.50;// ETB ( about 1 kg of maize- unhcr 15kg/person ( wheat)
														// 1.5 kg pulse 0.45 kg oil -
		private double adaptiveAgentLearningRate = 0.05;
		private double cognitiveBiasAlpha = 0.1;// k
		private double costAdptationElasticity = 0.1;// b
		private double costOfCredit = 0.1;
		private double proRiskDeduction = 0.1;
		private double malAdptHouseholdRatio = 0.2;
		private double riskElasticity = 0.1;
		private double consumptionExp = 0.8; // if household can not cover 30 % of consumption, migrate
		private int minLaborAge = 11;
		private double percentageInhertanceWedding = 0.2; // percentage og
//        private double tluPerPersonPerYear =1.8; // tlu /person/per year serenegti
//        private double kgPerPersonPerYear =540; // kg/prson/per year // assumption
		private double minLaborManDay = 0.5; // a person can enage in two activities

		public void setPovertyLineThreshold(final double r) {
			povertyLineThreshold = r;
		}

		public double getPovertyLineThreshold() {
			return povertyLineThreshold;
		}

		public void setPersonBirthRate(final double r) {
			personBirthRate = r;
		}

		public double getPersonBirthRate() {
			return personBirthRate;
		}

		public void setPersonDeathRate(final double r) {
			personDeathRate = r;
		}

		public double getPersonDeathRate() {
			return personDeathRate;
		}

		public void setMinimumLaborAge(final int age) {
			minLaborAge = age;
		}

		public int getMinimumLaborAge() {
			return minLaborAge;
		}

		public void setMimimumExpenditurePerPerson(final double exp) {
			MINIMUMEXPENDITUREPERPERSON = exp;
		}

		public double getMimimumExpenditurePerPerson() {
			return MINIMUMEXPENDITUREPERPERSON;
		}

		public void setAdaptationIntentionThreshold(final double adpt) {
			ADPTATION_THRESHOLD = adpt;
		}

		public double getAdaptationIntentionThreshold() {
			return ADPTATION_THRESHOLD;
		}

		public void setAdaptiveAgentLearningRate(final double learn) {
			adaptiveAgentLearningRate = learn;
		}

		public double getAdaptiveAgentLearningRate() {
			return adaptiveAgentLearningRate;
		}

		public void setCognitiveBias(final double learn) {
			cognitiveBiasAlpha = learn;
		}

		public double getCognitiveBias() {
			return cognitiveBiasAlpha;
		}

		public void setCostOfCredit(final double credit) {
			costOfCredit = credit;
		}

		public double getCostOfCredit() {
			return costOfCredit;
		}

		public void setCostAdptElasticity(final double el) {
			costAdptationElasticity = el;
		}

		public double getCostAdptElasticity() {
			return costAdptationElasticity;
		}

		public void setProRiskDeduction(final double risk) {
			proRiskDeduction = risk;
		}

		public double getProRiskDeduction() {
			return proRiskDeduction;
		}

		public void setMalAdptHouseholdRatio(final double mal) {
			malAdptHouseholdRatio = mal;
		}

		public double getMalAdptHouseholdRatio() {
			return malAdptHouseholdRatio;
		}

		public void setRiskElasticity(final double mal) {
			riskElasticity = mal;
		}

		public double getRiskElasticity() {
			return riskElasticity;
		}

		public void setConsumptionExp(final double mal) {
			consumptionExp = mal;
		}

		public double getConsumptionExp() {
			return consumptionExp;
		}

		public void setPercentageInhertanceWedding(final double mal) {
			percentageInhertanceWedding = mal;
		}

		public double getPercentageInhertanceWedding() {
			return percentageInhertanceWedding;
		}

		public void setMinLaborManDay(final double mal) {
			minLaborManDay = mal;
		}

		public double getMinLaborManDay() {
			return minLaborManDay;
		}
	}

	public class HerdingParamters {
		// http://www.fao.org/DOCREP/005/Y4176E/y4176e04.htm
		// 10 cattle - .7*10=7// FAO The TLU conversion factors used are as follows:
		// cattle = 0.70, sheep and goats = 0.10, pigs = 0.20 and chicken = 0.01

		private double herdMaxDailyDMIntake = 7.0;// // daily tlu forage requirement 7 kg/tlu/day
													// http://books.google.com/books?id=ClS9y_dwVuYC&pg=PA130&lpg=PA130&dq=grazing+land+dairy+forage+per+tlu&source=bl&ots=UwXlHV7Aq7&sig=OlSzscDjjO8jUyyWhm9daWaBG5U&hl=en&sa=X&ei=WEENUcOgLI680AG71YCgDA&ved=0CDcQ6AEwAg#v=onepage&q=grazing%20land%20dairy%20forage%20per%20tlu&f=false
		public double herdMaxiumFoodStored = 350; // 5Kg/day * 60 days (without food)
		private double herdGrowthRate = 0.01;// see descption below // generated from //
												// http://www.csa.gov.et/images/general/news/livs_2014_2015
		private double herdConsumptionRate = 3.0; // schmidt and Verweij 1992 minimum - max - energy for livestock for
													// gowth and development
		private double proportionLaborToTLU = 15; // 1 herder can look 15TLU
		private double herdLaborEfficiencyFactor = 0.95;
		private double AVERAGEHERDTLUPRICE = 3340;// 3340; // ETB/livestock (CSA2013) ( cow = 2850 ox = 6700 mean of the
													// two * 0.7 ( to tlu) == 3342.0) almost 3340
		private double DESTOCKPROPORTION = 0.04; // csa - sell
		private double RESTOCKPROPORTION = 0.01; // csa - purchase
		private double HERDSPLITTHRESHOLD = 0.3;
		private int ScoutRange = 200; // 20km/day
		private int MINIMUMVISION = 50; // 5 km /day
		private double herdHealthConditionFactor = 0.5; // about 1tlu per yearper 10 tlu
		// http://www.lrrd.org/lrrd21/9/muli21151.htm ( mean daily growth 10 kg)
		// if only rainy season - may reach 20 kgDM/ha

		// http://www.csa.gov.et/images/general/news/livs_2014_2015
		// CSA agri census Livestock and Livestock characteristics survey report for the
		// year 2007 E.C (2014-2015)
		// south Omo - totla cattle = 1,673,434
		// cattle birth = 291,747 1381687
		// cattle purchase = 28,047
		// cattle sale = 76,119
		// cattle slaughter =6,108
		// cattle death = 141,962
		// cattle offering = 18,320
		// diseased = 268,783 and
		// died from disease = 120,108 ( about 7 % of total livestock died by disease
		// every year

		// calculateing grwoth rate - total gain (birth_purchase = 319,794
		// total loss = 224189
		// net gain = 95,600 --> growth rate = (1,673,434/(1,673,434 - 95,600) =
		// 0.060592751
		// but only considering the death (since sell or slughtered are consider in the
		// model)
		// Growth rate per year = 0.12 --- > 0.00032 per day
		// Geographic Area Holdings with
		// no cattle 1-2Head 3-4Head 5-9Head 10-1Head 20-49Head 50-99Head 100-199Head
		// >=200 Head
		// South Omo 58,286 46,239 37,482 50,001 21,445 14,126 4,282 1,339 0
		public void setHerdMaxDailyDMIntake(final double intake) {
			herdMaxDailyDMIntake = intake;
		}

		public double getHerdMaxDailyDMIntake() {
			return herdMaxDailyDMIntake;
		}

		public void setHerdMaxFoodStored(final double stored) {
			herdMaxiumFoodStored = stored;
		}

		public double getHerdMaxFoodStored() {
			return herdMaxiumFoodStored;
		}

		public void setHerdGrowthRate(final double rate) {
			herdGrowthRate = rate;
		}

		public double getHerdGrowthRate() {
			return herdGrowthRate;
		}

		public void setHerdDailyConsumptionRate(final double consump) {
			herdConsumptionRate = consump;
		}

		public double getHerdDailyConsumptionRate() {
			return herdConsumptionRate;
		}

		public void setProportionLaborToTLU(final double tlu) {
			proportionLaborToTLU = tlu;
		}

		public double getProportionLaborToTLU() {
			return proportionLaborToTLU;
		}

		public void setHerdLaborEfficiencyFactor(final double tlu) {
			herdLaborEfficiencyFactor = tlu;
		}

		public double getHerdLaborEfficiencyFactor() {
			return herdLaborEfficiencyFactor;
		}

		public void setAverageHerdPrice(final double price) {
			AVERAGEHERDTLUPRICE = price;
		}

		public double getAverageHerdPrice() {
			return AVERAGEHERDTLUPRICE;
		}

		public void setHerdDestockingProportion(final double destock) {
			DESTOCKPROPORTION = destock;
		}

		public double getHerdDestockingProportion() {
			return DESTOCKPROPORTION;
		}

		public void setHerdRestockingProportion(final double restock) {
			RESTOCKPROPORTION = restock;
		}

		public double getHerdRestockingProportion() {
			return RESTOCKPROPORTION;
		}

		public void setHerdSplitingThreshold(final double split) {
			HERDSPLITTHRESHOLD = split;
		}

		public double getHerdSplitingThreshold() {
			return HERDSPLITTHRESHOLD;
		}

		public void setHerderScoutingRange(final int range) {
			ScoutRange = range;
		}

		public int getHerderScoutingRange() {
			return ScoutRange;
		}

		public void setHerderMinVisionRange(final int vision) {
			MINIMUMVISION = vision;
		}

		public int getHerderMinVisionRange() {
			return MINIMUMVISION;
		}

		public void setHerdHealthConditionFactor(final double vision) {
			herdHealthConditionFactor = vision;
		}

		public double getHerdHealthConditionFactor() {
			return herdHealthConditionFactor;
		}
	}

	public class FarmingParameters {

		private double minimumAnnualRainfall = 500; // less than this farmin is not possible
		private int weedingMeanDay = 30;// 30 days after plant
		private int MAX_DAYS_AFTER_LANDPREPARATION = 30; // 30 days after land prep
		private double farmInputCost = 200; // fertlizer and hyv ; 100 kg.ha Morris et al 2007- fertilizer use in africa
											// ( WorldBank)
		private boolean isIrrigationPossible = false;
		private boolean isIntesificationPossible = true;
		private double averageInitialFarmingCost = 100;// cost of implementing new farm - arbitrary
		private double irrigationFarmCost = 200;//// may depend on distance from current location- if implementing far-
												//// distance cost
		private double farmCobbDaglasCoeff = 0.5;
		private double proportionLaborToAreaHa = 0.75; // hectare a person can farm
		private double farmLaborEfficiencyFactor = 1.1; // exponent

		//
		public void setMinimumAnnualRainfall(final double rain) {
			minimumAnnualRainfall = rain;
		}

		public double getMinimumAnnualRainfall() {
			return minimumAnnualRainfall;
		}

		public void setAverageInitialFarmingCost(final double cost) {
			averageInitialFarmingCost = cost;
		}

		public double getAverageInitialFarmingCost() {
			return averageInitialFarmingCost;
		}

		public void setWeedingMeanDay(final int day) {
			weedingMeanDay = day;
		}

		public int getWeedingMeanDay() {
			return weedingMeanDay;
		}

		public void setMaxDaysPlantingAfterLandPrep(final int d) {
			MAX_DAYS_AFTER_LANDPREPARATION = d;
		}

		public int getMaxDaysPlantingAfterLandPrep() {
			return MAX_DAYS_AFTER_LANDPREPARATION;
		}

		public void setIsIrrigationPossible(final boolean irr) {
			isIrrigationPossible = irr;
		}

		public boolean getIsIrrigationPossible() {
			return isIrrigationPossible;
		}

		public void setIsIntesificationPossible(final boolean intes) {
			isIntesificationPossible = intes;
		}

		public boolean getIsIntesificationPossible() {
			return isIntesificationPossible;
		}

		public void setIrrigationFarmCost(final double c) {
			irrigationFarmCost = c;
		}

		public double getIrrigationFarmCost() {
			return irrigationFarmCost;
		}

		public void setFarmInputCost(final double c) {
			farmInputCost = c;
		}

		public double getFarmInputCost() {
			return farmInputCost;
		}

		public void setFarmCobbDaglasCoeff(final double c) {
			farmCobbDaglasCoeff = c;
		}

		public double getFarmCobbDaglasCoeff() {
			return farmCobbDaglasCoeff;
		}

		public void setProportionLaborToAreaHa(final double c) {
			proportionLaborToAreaHa = c;
		}

		public double getProportionLaborToAreaHa() {
			return proportionLaborToAreaHa;
		}

		public void setFarmLaborEfficiencyFactor(final double f) {
			farmLaborEfficiencyFactor = f;
		}

		public double getFarmLaborEfficiencyFactor() {
			return farmLaborEfficiencyFactor;
		}
	}

	public class ClimateParamters {

		private double minimumOnsetRainThreshold = 10; /// 10 minimum rainfall for onset //
		private double MinimimEffectiveRainfall = 40; // MER/day// 2 –3 weeks each with at least 50% of the weekly
														// crop–water requirement --e.g50% requirement (4 mm for Kano)
		private int MINDAYS_MERPERENTAGE = 15; // 15 days sum >50 % MER // 4 * 15 days = 60 == start with mininum 50
		private double CESS_THRESHOLD = 10.0; //
		// private int MinimumDaysOnset = 2; // 1 week
		private int MinDaysOfMinimumrainfall = 3; // two days
		// drought parameters
		private int firstDroughtYear = 3;
		private int numberOfDroughtYears = 1;
		private double severityOfDrought = 0.7;//
		private int frequencyOfDroughtYears = 2;
		private boolean isDroughtYearsOn = false;
		// wet season parameters
		private int firstWetYear = 5;
		private int numberOfWetYears = 5;
		private double severityOfWet = 0.5;
		private int frequencyOfWetYears = 5;
		private boolean isWetYearsOn = true;
		private boolean isSteadyState = true; // rainfall take the long term mean rainfal
//        private boolean isDroughtInMainSeaon =false;
//        private boolean isDroughtInSecondSeason =false;
//
		private boolean isRandomizedRainYears = false; // if it is true, the next 12 year monthly rainfall
		// will be assignd by randomize the 12 rainy season year

//        private boolean isWetInMainSeaon =false;
//        private boolean isWetInSecondSeason =false;
//
		public void setMinimumOnsetRainThreshold(final double rain) {
			minimumOnsetRainThreshold = rain;
		}

		public double getMinimumOnsetRainThreshold() {
			return minimumOnsetRainThreshold;
		}

		public void setMinimimEffectiveRainfall(final double mer) {
			MinimimEffectiveRainfall = mer;
		}

		public double getMinimimEffectiveRainfall() {
			return MinimimEffectiveRainfall;
		}

		public void setMinimimCessRainThreshold(final double rain) {
			CESS_THRESHOLD = rain;
		}

		public double getMinimimCessRainThreshold() {
			return CESS_THRESHOLD;
		}

		public void setMinDaysOfMinimumrainfall(final int days) {
			MinDaysOfMinimumrainfall = days;
		}

		public int getMinDaysOfMinimumrainfall() {
			return MinDaysOfMinimumrainfall;
		}

		public void setMinDaysofMERainPercetage(final int days) {
			MINDAYS_MERPERENTAGE = days;
		}

		public int getMinDaysofMERainPercetage() {
			return MINDAYS_MERPERENTAGE;
		}

		public void setFirstDroughtYear(final int years) {
			firstDroughtYear = years;
		}

		public int getFirstDroughtYear() {
			return firstDroughtYear;
		}

		public void setNumberOfDroughtYears(final int years) {
			numberOfDroughtYears = years;
		}

		public int getNumberOfDroughtYears() {
			return numberOfDroughtYears;
		}

		public void setSeverityOfDrought(final double sev) {
			severityOfDrought = sev;
		}

		public double getSeverityOfDrought() {
			return severityOfDrought;
		}

		public void setFrequencyOfDroughtYears(final int freq) {
			frequencyOfDroughtYears = freq;
		}

		public int getFrequencyOfDroughtYears() {
			return frequencyOfDroughtYears;
		}

		public void setIsDroughtYearsOn(final boolean main) {
			isDroughtYearsOn = main;
		}

		public boolean getIsDroughtYearsOn() {
			return isDroughtYearsOn;
		}

		public void setisSteadyState(final boolean main) {
			isSteadyState = main;
		}

		public boolean getisSteadyState() {
			return isSteadyState;
		}

//        public void setIsDroughtInMainSeaon(boolean main){this.isDroughtInMainSeaon= main;}
//        public boolean getIsDroughtInMainSeaon(){return this.isDroughtInMainSeaon;}
//
//        public void setIsDroughtInSecondSeason(boolean sec){this.isDroughtInSecondSeason= sec;}
//        public boolean getIsDroughtInSecondSeason(){return this.isDroughtInSecondSeason;}
//
		public void setIsRandomizedRainYears(final boolean random) {
			isRandomizedRainYears = random;
		}

		public boolean getIsRandomizedRainYears() {
			return isRandomizedRainYears;
		}

		public void setFirstWetYear(final int years) {
			firstWetYear = years;
		}

		public int getFirstWetYear() {
			return firstWetYear;
		}

		public void setNumberOfWetYears(final int years) {
			numberOfWetYears = years;
		}

		public int getNumberOfWetYears() {
			return numberOfWetYears;
		}

		public void setSeverityOfWet(final double sev) {
			severityOfWet = sev;
		}

		public double getSeverityOfWet() {
			return severityOfWet;
		}

		public void setFrequencyOfWetYears(final int freq) {
			frequencyOfWetYears = freq;
		}

		public int getFrequencyOfWetYears() {
			return frequencyOfWetYears;
		}

		public void setIsWetYearsOn(final boolean main) {
			isWetYearsOn = main;
		}

		public boolean getIsWetYearsOn() {
			return isWetYearsOn;
		}
//        public void setIsWetInMainSeaon(boolean main){this.isWetInMainSeaon= main;}
//        public boolean getIsWetInMainSeaon(){return this.isWetInMainSeaon;}
//
//        public void setIIsWetInSecondSeason(boolean sec){this.isWetInSecondSeason= sec;}
//        public boolean getIsWetInSecondSeason(){return this.isWetInSecondSeason;}
//
	}

	public class Vegetation {

		public double maxVegetationPerHectare = 6000; // 4000// http://www.lrrd.org/9/muli21151.htm kg/DM/year 9 manly
														// count ofrainy season
		public double minVegetation = 100.0;
		public double baseGrowthRateControler = 1.390;/// growth rate // kgDM/ha/day= sergeti III page 285 ( mcNaughtin
														/// 1985)
		public double averageRainfallGrowthCutOFF = 3.125;// mm/day mximum rainfall

		// public double baseGrowthRatePerDay =4.50; //3-5 kg kg/ha/day
		// http://agtr.ilri.cgiar.org/documents/library/docs/x5552e/x5552e06.htm
		// if only rainy season - may reach 20 kgDM/ha
		public void setMaxVegetationPerHectare(final double max) {
			maxVegetationPerHectare = max;
		}

		public double getMaxVegetationPerHectare() {
			return maxVegetationPerHectare;
		}

		public void setMinVegetation(final double min) {
			minVegetation = min;
		}

		public double getMinVegetation() {
			return minVegetation;
		}

//        public void setGrowthThreshold(double g){this.growthThreshold =g;}
//        public double getGrowthThreshold(){return this.growthThreshold;}
//
//        public void setQualityMultiplier(double m){this.qualityMultiplier =m;}
//        public double getQualityMultiplier(){return this.qualityMultiplier;}
//
		public void setBaseGrowthRateContorler(final double b) {
			baseGrowthRateControler = b;
		}

		public double getBaseGrowthRateContorler() {
			return baseGrowthRateControler;
		}

//
		public void setAverageRainfallGrowthCutOFF(final double c) {
			averageRainfallGrowthCutOFF = c;
		}

		public double getAverageRainfallGrowthCutOFF() {
			return averageRainfallGrowthCutOFF;
		}
//        public void setGrassTreeComptition(double c){this.treeComptition =c;}
//        public double getGrassTreeComptition(){return this.treeComptition;}
//
//
	}
//
//    public class ParcelParamters{
//
//    }
//
//    public class CropParamters{
//
//    }
}
