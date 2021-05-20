package sim.app.geo.omolandCA;

import java.util.Arrays;

/**
 *
 * @author gmu
 */
import sim.util.Bag;

//import java.lang.Math.*;

public class HouseholdData {

	private int ethinicID; // ethnic id
	// total income ( Herd+ farm+ off-farm + remittance)

	protected Bag farmLands; // farm lands - ha
	protected Bag familyMembers;
	private Parcel location;

	protected Herd herd;
	protected double laborAllocatedFarming;
	protected double laborAllocatedHerding;

	protected double wealth;
	protected double storedCapital; // money stored in bank or at hand
	protected double learningrate;

	protected double incomeFarming;
	protected double incomeHerding;
	protected double incomeLabor;

	protected double incomeOtherSource;
	protected double expenditure; // all costs of the household (food, transpor, pleasure etc)

	protected double climateCAdaptationExperience;

	protected int perAmountRainfall;
	protected int perOnsetRainfall;

	protected int nextPredictionDate = 10;

	final int SEASONPERYEAR = 2;
	final int YEARSTOREMEMBER = 4;

	protected int[][] rainfallAmountMemory;
	protected int[][] rainfallOnsetMemory;

	protected int currenAdaptationMechanism; // should hold one - if more than one - diversification

	public HouseholdData() {
		farmLands = new Bag();
		familyMembers = new Bag();
		rainfallAmountMemory = new int[YEARSTOREMEMBER][SEASONPERYEAR];
		rainfallOnsetMemory = new int[YEARSTOREMEMBER][SEASONPERYEAR];

	}

	public void setEthinicID(final int id) {
		ethinicID = id;
	}

	public int getEthinicID() {
		return ethinicID;
	}

	public void setFamilyMembers(final Bag families) {
		familyMembers = families;
	}

	public Bag getFamilyMembers() {
		return familyMembers;
	}

	public void addFamilyMembers(final Person p) {
		familyMembers.add(p);
	}

	public void setNextPredictionDate(final int id) {
		nextPredictionDate = id;
	}

	public int getNextPredictionDate() {
		return nextPredictionDate;
	}

	// farm techn knowldge

	public void setPercievedAmountRainfall(final int w) {
		perAmountRainfall = w;
	}

	public int getPercievedAmountRainfall() {
		return perAmountRainfall;
	}

	public void setPercievedOnsetRainfall(final int w) {
		perOnsetRainfall = w;
	}

	public int getPercievedOnsetRainfall() {
		return perOnsetRainfall;
	}

	public void setLaborAllocatedFarming(final double laborDemand) {
		laborAllocatedFarming = laborDemand;
	}

	public double getLaborAllocatedFarming() {
		return laborAllocatedFarming;
	}

	public void setLaborAllocatedHerding(final double laborDemand) {
		laborAllocatedHerding = laborDemand;
	}

	public double getLaborAllocatedHerding() {
		return laborAllocatedHerding;
	}

	public void setIncomeFarming(final double income) {
		incomeFarming = income;
	}

	public double getIncomeFarming() {
		return incomeFarming;
	}

	public void setIncomeHerding(final double income) {
		incomeHerding = income;
	}

	public double getIncomeHerding() {
		return incomeHerding;
	}

	public void setIncomeLabor(final double income) {
		incomeLabor = income;
	}

	public double getIncomeLabor() {
		return incomeLabor;
	}

	public void setLearningRate(final double learning) {
		learningrate = learning;
	}

	public double getLearningRate() {
		return learningrate;
	}

	// household total income
	public void setWealth(final double w) {
		wealth = w;
	}

	public double getWealth() {
		return wealth;
	}

	public void setStoredCapital(final double w) {
		storedCapital = w;
	}

	public double getStoredCapital() {
		return storedCapital;
	}

	public void setHerd(final Herd herd) {
		this.herd = herd;
	}

	public Herd getHerd() {
		return herd;
	}

	public void setExpenditure(final double cost) {
		expenditure = cost;
	}

	public double getExpenditure() {
		return expenditure;

	}

	public void setFarmLands(final Bag farms) {
		farmLands = farms;
	}

	public Bag getFarmLands() {
		return farmLands;
	}

	public void addFarmLands(final Parcel p) {
		farmLands.add(p);
	}

	public void removeFarmLands(final Parcel p) {
		farmLands.remove(p);
	}

	public void setLocation(final Parcel p) {
		location = p;
	}

	public Parcel getLocation() {
		return location;
	}

	public void setClimateCAdaptationExperience(final double exp) {
		climateCAdaptationExperience = exp;
	}

	public double getClimateCAdaptationExperience() {
		return climateCAdaptationExperience;
	}

	public void setCurrenAdaptationMechanism(final int cAd) {
		currenAdaptationMechanism = cAd;
	}

	public int getCurrenAdaptationMechanism() {
		return currenAdaptationMechanism;
	}

	// http://stackoverflow.com/questions/6665150/getters-and-setters-for-arrays

	public void setRainfallAmountMemory(final int[][] amount) {
		rainfallAmountMemory = Arrays.copyOf(amount, amount.length);
	}

	public int[][] getRainfallAmountMemory() {
		return Arrays.copyOf(rainfallAmountMemory, rainfallAmountMemory.length);
	}

	public void setRainfallOnsetMemory(final int[][] onset) {
		rainfallOnsetMemory = Arrays.copyOf(onset, onset.length);
	}

	public int[][] getRainfallOnsetMemory() {
		return Arrays.copyOf(rainfallOnsetMemory, rainfallOnsetMemory.length);
	}

}
